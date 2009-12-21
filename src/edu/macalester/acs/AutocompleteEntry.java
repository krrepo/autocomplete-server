package edu.macalester.acs;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Stores autocomplete information about an entity.
 *
 * User: shilad
 * Date: Dec 19, 2009
 */
public class AutocompleteEntry<K extends Comparable, V> {
    private K key;
    private V value;

    /**
     * Fragments are populated by the AutocompleteTree when add() is called.
     */
    List<AutocompleteFragment<K, V>> fragments = new ArrayList<AutocompleteFragment<K, V>>();

    private int frequency;

    /**
     * Creates a new autocomplete entry.
     * @param key
     * @param value
     */
    public AutocompleteEntry(K key, V value) {
        this.key = key;
        this.value = value;
        this.frequency = 0;
    }

    /**
     * Creates a new autocomplete entry.
     * @param key
     * @param value
     * @param frequency
     */
    public AutocompleteEntry(K key, V value, int frequency) {
        this.key = key;
        this.value = value;
        this.frequency = frequency;
    }

    protected void clearFragments() {
        this.fragments = new ArrayList<AutocompleteFragment<K, V>>();
    }

    protected void addFragment(AutocompleteFragment<K, V> fragment) {
        fragments.add(fragment);
    }

    protected void freezeFragments() {
        this.fragments = Collections.unmodifiableList(fragments);
    }

    public K getKey() {
        return key;
    }
    
    public V getValue() {
        return value;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    public List<AutocompleteFragment<K,V>> getFragments() {
        return fragments;
    }

    public String toString() {
        return "entry (" + getKey() + ", " + getValue() + ") with freq " + frequency; 
    }
}
