package edu.macalester.acs.server;

import org.json.simple.JSONValue;

import java.util.Map;
import java.util.HashMap;
import org.json.simple.parser.ParseException;

/**
 */
public class AbstractEntity extends HashMap<String, Object> {
    private String id;
    private String name;
    private double score;

    public AbstractEntity(Map<String, Object> map) throws IllegalArgumentException {
        if (!map.containsKey("id")) {
            throw new IllegalArgumentException("map must contain key named 'id'");
        } else if (!(map.get("id") instanceof String)) {
            throw new IllegalArgumentException("id must be a string");
        }
        id = (String)map.get("id");
        
        if (!map.containsKey("name")) {
            throw new IllegalArgumentException("map must contain key named 'name'");
        } else if (!(map.get("name") instanceof String)) {
            throw new IllegalArgumentException("name must be a string");
        }
        name = (String)map.get("name");

        score = 0.0;
        if (!map.containsKey("score")) {
            throw new IllegalArgumentException("map must contain key named 'score'");
        } else if (map.get("score") instanceof Double) {
            score = (Double)map.get("score");
        } else if (map.get("score") instanceof Integer) {
            score = (Integer)map.get("score");
        } else if (map.get("score") instanceof Long) {
            score = (Long)map.get("score");
        } else {
            throw new IllegalArgumentException("score must be an int or double");
        }
        putAll(map);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getScore() {
        return score;
    }

    public String toString() {
        return name;
    }

    public String serialize() {
        String json = JSONValue.toJSONString(this);
        json = json.replaceAll("\n", "");
        json = json.replaceAll("\r", "");
        return json;
    }

    static public AbstractEntity deserialize(String json) throws IllegalArgumentException {
        try {
            Object o = JSONValue.parseWithException(json);
            return new AbstractEntity((Map<String, Object>)o);
        } catch (ParseException e) {
            throw new IllegalArgumentException("illegal json object");
        }
    }
}
