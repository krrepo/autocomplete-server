package edu.macalester.acs;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * @author Shilad Sen
 * @see AutocompleteTree
 */
public class AutocompleteEntry<K extends Comparable, V> {
    private K key;
    private V value;

    /**
     * Fragments are populated by the AutocompleteTree when add() is called.
     */
    List<AutocompleteFragment<K, V>> fragments = new ArrayList<AutocompleteFragment<K, V>>();

    private double score;

    /**
     * Creates a new autocomplete entry.
     * @param key
     * @param value
     */
    public AutocompleteEntry(K key, V value) {
        this.key = key;
        this.value = value;
        this.score = 0;
    }

    /**
     * Creates a new autocomplete entry.
     * @param key
     * @param value
     * @param score
     */
    public AutocompleteEntry(K key, V value, int score) {
        this.key = key;
        this.value = value;
        this.score = score;
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

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    /**
     * This method is useful if the score is actually a
     * frequency (as it commonly is).
     * @return
     */
    public int getScoreAsInt() {
        return (int) Math.round(score);
    }

    public List<AutocompleteFragment<K,V>> getFragments() {
        return fragments;
    }

    public String toString() {
        return "entry (" + getKey() + ", " + getValue() + ") with freq " + score;
    }
}
