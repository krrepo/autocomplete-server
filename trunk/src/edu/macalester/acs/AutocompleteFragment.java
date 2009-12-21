package edu.macalester.acs;

/**
 * A fragment that a user's query is compared against.
 * A single entry may have multiple fragments.  For example,
 * "Barack Hussain Obama" may have three fragments:
 *      1. "Barack Hussain Obama"
 *      2. "Barack Obama"
 *      3. "Obama"
 */
public class AutocompleteFragment<K extends Comparable, V> implements Comparable<AutocompleteFragment<K, V>> {
    private AutocompleteEntry<K, V> entry;
    private String fragment;

    public AutocompleteFragment(AutocompleteEntry<K, V> entry, String fragment) {
        this.entry = entry;
        this.fragment = fragment;
    }

    public AutocompleteEntry<K, V> getEntry() {
        return entry;
    }

    public String getFragment() {
        return fragment;
    }

    public int compareTo(AutocompleteFragment<K, V> other) {
        int r = fragment.compareTo(other.fragment);
        if (r == 0 && other.entry != null && entry != null) {
            r = other.entry.getFrequency() - entry.getFrequency();
        }
        if (r == 0 && other.entry != null && entry != null) {
            r = entry.getKey().compareTo(other.entry.getKey());
        }
//        System.out.println("comparing " + this + " to " + other + " returns " + r);
        return r;
    }

    public boolean equals(Object o) {
        if (o instanceof AutocompleteFragment) {
            AutocompleteFragment<K, V> other = (AutocompleteFragment<K, V>)o;
            return (this.compareTo(other) == 0);
        } else {
            return false;
        }
    }

    public String toString() {
        return "fragment: '" + fragment + "' for entry " + entry;
    }
}
