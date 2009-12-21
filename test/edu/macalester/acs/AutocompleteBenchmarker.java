package edu.macalester.acs;

import java.util.*;
import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

/**
 * @author Shilad Sen
 * 
 * Benchmarks the performance of the autocomplete library.
 */
public class AutocompleteBenchmarker {

    /** The number of threads to use for the simulation */
    private static final int NUM_THREADS = 10;

    /** The number of iterations per thread to use for the simulation */
    private static final int NUM_ITERATIONS = 10000;

    /** Frequencies are randomly distributed between 0 and this number. */
    private static final int MAX_FREQUENCY = 100;

    /** The maximum length of an autocomplete query. */
    private static final int MAX_QUERY_LENGTH = 10;

    /** The maximum number of autocomplete results */
    private static final int MAX_RESULTS = 6;

    /** The maximum size of the cache query */
    private static final int MAX_CACHE_QUERY_LENGTH = 2;

    /**
     * Probability of updating the frequency of an entity.
     * When not writing, a simple autocomplete query is invoked.
     */
    private static double WRITE_LIKELIHOOD = 0.01;

    /**
     * Path to the city data file.
     */
    private static File PATH_CITIES = new File("autocomplete-server/data/cities.txt");
    

    private Random rand = new Random();
    private List<City> cities;
    private AutocompleteTree<Integer, City> tree;
    private static final double ONE_BILLION = 1000000000.0;

    public AutocompleteBenchmarker() {
    }

    /**
     * Reads a collection of cities from a file.
     * The format is line-separated, "id,state,city" such as:
     *
     * "3,Alabama,Addison"
     * @param path
     */
    public void readCities(File path) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(path));
        cities = new ArrayList<City>();
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
            cities.add(new City(Integer.parseInt(tokens[0]), tokens[2], tokens[1]));
        }
    }

    /**
     * Builds the tree from the cities.
     */
    public void buildTree() {
        double begin = getCpuSeconds();
        tree = new AutocompleteTree<Integer, City>();
        tree.setNumCacheResults(MAX_RESULTS);
        tree.setMaxCacheQueryLength(MAX_CACHE_QUERY_LENGTH);
        for (City city : cities) {
            int freq = rand.nextInt(MAX_FREQUENCY);
            tree.add(new AutocompleteEntry<Integer, City>(city.getId(), city, freq));
        }
        double end = getCpuSeconds();
        double elapsed = end - begin;
        System.out.println("populated tree with " + cities.size() + " cities in " + elapsed + " seconds");
    }

    public void benchmark(int numThreads, final int numIterationsPerThread) {
        // Create all the threads
        Thread [] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread() {
                public void run() {
                    benchmarkThread(numIterationsPerThread);
                }
            };
        }
        
        // Start all the threads
        for (Thread t : threads) {
            t.start();
        }
    }

    /**
     * Runs a single benchmarking thread for some iterations and outputs
     * statistics about the thread.
     * @param numIterations
     */
    public void benchmarkThread(int numIterations) {
        double beginCpu = getCpuSeconds();
        double beginWall = getWallSeconds();
        long totalResults = 0;

        for (int i = 0; i < numIterations; i++) {
            // Select a city randomly
            City city = cities.get(rand.nextInt(cities.size()));
            if (rand.nextDouble() < WRITE_LIKELIHOOD) {
                // update the frequency of one of the cities.
                tree.increment(city.getId());
            } else {
                int prefixLength = Math.min(1+rand.nextInt(MAX_QUERY_LENGTH-1), city.getName().length());
                String prefix = city.getName().substring(0, prefixLength);
                SortedSet<AutocompleteEntry<Integer, City>> results = tree.autocomplete(prefix, MAX_RESULTS);
                totalResults += results.size();
            }
        }
        double endCpu = getCpuSeconds();
        double endWall = getWallSeconds();
        double elapsedWall = endWall - beginWall;
        System.out.println("thread took " + (endCpu - beginCpu) + " cpu seconds");
        System.out.println("thread took " + elapsedWall + " wall seconds");
        System.out.println("num calls per iteration is " + (numIterations / elapsedWall));
        System.out.println("mean num results is " + (1.0 * totalResults / numIterations));
    }

    private static final double getCpuSeconds() {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        long threadId = java.lang.Thread.currentThread().getId();
        long nanos = bean.getThreadCpuTime(threadId);
        return nanos / ONE_BILLION;
    }

    private static final double getWallSeconds() {
        return System.nanoTime() / ONE_BILLION;
    }

    public static void main(String args[]) throws Exception {
        AutocompleteBenchmarker benchmarker = new AutocompleteBenchmarker();
        benchmarker.readCities(PATH_CITIES);
        benchmarker.buildTree();
        benchmarker.benchmark(NUM_THREADS, NUM_ITERATIONS);
    }
}

