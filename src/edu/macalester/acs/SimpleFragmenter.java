package edu.macalester.acs;

import java.util.List;
import java.util.ArrayList;

/**
 * Fragments strings by splitting on whitespace, and normalizing them.
 * User: shilad
 * Date: Dec 20, 2009
 */
public class SimpleFragmenter<K extends Comparable, V> implements Fragmenter<K, V>{
    
    public List<String> getFragments(AutocompleteEntry<K, V> entry) {
        List<String> fragments = new ArrayList<String>();
        for (String token : normalize(entry.getValue().toString()).split("\\s+")) {
            fragments.add(token);
        }
        return fragments;
    }

    public String normalize(String dirty) {
        return dirty.toLowerCase()
                .replaceAll("'", "")
                .replaceAll("[^0-9a-zA-Z]", " ")
                .replaceAll(" \\s+", " ")
                .trim();
    }
}
