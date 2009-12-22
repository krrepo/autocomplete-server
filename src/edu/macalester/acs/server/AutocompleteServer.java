package edu.macalester.acs.server;

import net.freeutils.httpserver.HTTPServer;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

import org.json.simple.parser.JSONParser;
import org.json.simple.JSONValue;
import edu.macalester.acs.AutocompleteTree;
import edu.macalester.acs.AutocompleteEntry;

/**
 * @author Shilad Sen
 */
public class AutocompleteServer {
    private Logger log = Logger.getLogger(getClass().getName());
    
    private static final String PATH_UPDATE = "/update";
    private static final String PATH_DUMP = "/dump";
    private static final String PATH_AUTOCOMPLETE = "/autocomplete";

    BufferedWriter writer;
    final File dataPath;
    JSONParser parser;

    HTTPServer server;
    AutocompleteTree<String, AbstractEntity> tree;

    public AutocompleteServer(AutocompleteTree<String, AbstractEntity> tree, File dataPath, int port) throws IOException {
        this.dataPath = dataPath;
        this.tree = tree;
        server = new HTTPServer(port);
        parser = new JSONParser();
        writer = new BufferedWriter(new FileWriter(dataPath, true));
        HTTPServer.VirtualHost host = server.getVirtualHost(null);
        
        host.addContext("/", new HTTPServer.ContextHandler() {
            public int serve(HTTPServer.Request request, HTTPServer.Response response) throws IOException {
                return handle(request, response);
            }
        });

        reloadData();

    }

    public void reloadData() throws IOException {
        log.info("loading entries from " + dataPath);
        int i = 0;
        synchronized (dataPath) {
            tree.clear();
            BufferedReader reader = new BufferedReader(new FileReader(dataPath));
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                try {
                    AbstractEntity entity = AbstractEntity.deserialize(line.trim());
                    AutocompleteEntry<String, AbstractEntity> entry =
                            new AutocompleteEntry<String, AbstractEntity>(entity.getId(), entity, entity.getScore());
                    tree.add(entry);
                    i++;
                } catch (IllegalArgumentException e) {
                    System.err.println("invalid line: '" + line.trim() + "': " + e.getMessage());
                }
            }
        }
        log.info("loaded " + i + " entries from " + dataPath);
    }

    
    public void start() throws IOException {
        server.start();
    }

    private int sendError(int code, HTTPServer.Request request, HTTPServer.Response response, String message) throws IOException {
        log.warning("error occured processing " + request.getPath() + ": " + message);
        response.sendError(code, message);
        return code;
    }

    private int sendError(HTTPServer.Request request, HTTPServer.Response response, String message) throws IOException {
        return sendError(500, request, response, message);
    }

    private int sendOkay(HTTPServer.Request request, HTTPServer.Response response, String message) throws IOException {
        response.send(200, message);
        return 200;
    }


    public int handle(HTTPServer.Request request, HTTPServer.Response response) throws IOException {
        log.info("Received request " + request.getPath());
        
        int code = 200;

        if (request.getPath().startsWith(PATH_DUMP)) {
            code = handleDump(request, response);
        } else if (request.getPath().startsWith(PATH_UPDATE)) {
            code = handleUpdate(request, response);
        } else if (request.getPath().startsWith(PATH_AUTOCOMPLETE)) {
            code = handleAutocomplete(request, response);
        } else {
            code = sendError(400, request, response, "unknown path: " + request.getPath());
        }
        
        return code;
    }
    
    public int handleDump(HTTPServer.Request request, HTTPServer.Response response) throws IOException {
        dump();
        return sendOkay(request, response, "okay");
    }

    public void dump() throws IOException {
        synchronized (dataPath) {
            File newFile = new File(dataPath.toString() + ".new");
            BufferedWriter newWriter = new BufferedWriter(new FileWriter(newFile));
            for (AutocompleteEntry<String, AbstractEntity> entry : tree.getEntries()) {
                newWriter.write(entry.getValue().serialize() + "\n");
            }
            newWriter.flush();
            newWriter.close();
            writer.close();
            dataPath.delete();
            newFile.renameTo(dataPath);
            writer = new BufferedWriter(new FileWriter(dataPath, true));
        }
    }

    public int handleAutocomplete(HTTPServer.Request request, HTTPServer.Response response) throws IOException {
        Map<String, String> params = request.getParams();
        if (!params.containsKey("query")) {
            return sendError(request, response, "query parameter not specified");
        }
        String query = params.get("query");
        int maxResults = 10;
        if (params.containsKey("max")) {
            maxResults = Integer.parseInt(params.get("max"));
        }
        SortedSet<AutocompleteEntry<String, AbstractEntity>> results = tree.autocomplete(query, maxResults);
        String body = "";
        for (AutocompleteEntry<String, AbstractEntity> entry : results) {
            if (body.length() > 0) {
                body += ",";
            }
            body += entry.getValue().serialize();
        }
        body = "[" + body + "]";
        response.getHeaders().add("Content-type", "application/json");
        return sendOkay(request, response, body);
    }

    public int handleUpdate(HTTPServer.Request request, HTTPServer.Response response) throws IOException {
        if (!request.getMethod().equals("POST")) {
            return sendError(request, response, "only POST method allowed for " + PATH_UPDATE);
        }
        String body = readBody(request);
        Object object = JSONValue.parse(body);
        if (object == null || !(object instanceof Map)) {
            return sendError(request, response, "body must contain a JSON map");
        }
        Map map = (Map)object;
        AbstractEntity entity = null;
        try {
            entity = new AbstractEntity(map);
        } catch (IllegalArgumentException e) {
            return sendError(request, response, e.getMessage());
        }

        if (tree.contains(entity.getId())) {
            tree.remove(entity.getId());
        }
        AutocompleteEntry<String, AbstractEntity> entry =
                new AutocompleteEntry<String, AbstractEntity>(entity.getId(), entity, entity.getScore());
        tree.add(entry);

        String json = entry.getValue().serialize();
        synchronized (dataPath) {
            // Should we flush, or not?
            writer.write(json + "\n");
            writer.flush();
        }
        
        return sendOkay(request, response, "okay");
    }

    private String readBody(HTTPServer.Request request) throws IOException {
        String body = "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(request.getBody()));
        while (true) {
            String line = reader.readLine();
            if (line == null)
                break;
            body += line;
        }
        return body;
    }

    private void loadCities() throws IOException {
        File path = new File("data/cities.txt");
        BufferedReader reader = new BufferedReader(new FileReader(path));
        Random random = new Random();
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            String [] tokens = line.trim().split(",", 3);
            if (tokens.length != 3 || tokens[0].length() == 0 || tokens[2].length() == 0) {
                System.err.println("bad line in '" + path + "': " + line.trim());
                continue;
            }
            String id = tokens[0];
            String state = tokens[1];
            String name = tokens[2];
            double score = random.nextDouble() * 100;
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("id", id);
            map.put("name", name);
            map.put("state", state);
            map.put("score", new Double(score));
            AbstractEntity entity = new AbstractEntity(map);
            AutocompleteEntry<String, AbstractEntity> entry =
                    new AutocompleteEntry<String, AbstractEntity>(entity.getId(), entity, entity.getScore());
            tree.add(entry);
        }
        dump();
    }

    public static void main(String args[]) throws IOException, InterruptedException {
        if (args.length == 0) {
            System.err.printf("Usage: %s data_file port%n",
                AutocompleteServer.class.getName());
            return;
        }
        File dataPath = new File(args[0]);
        int port = Integer.parseInt(args[1]);
        AutocompleteTree<String, AbstractEntity> tree
                = new AutocompleteTree<String, AbstractEntity>();
        AutocompleteServer server = new AutocompleteServer(tree, dataPath, port);
        server.start();
//        server.loadCities();
        System.out.println("server listening on port " + port);

        // Hack: what is correct?
        while (true) {
            Thread.sleep(100000);
        }
    }

}
