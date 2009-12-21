package edu.macalester.acs;

import java.util.*;

/**
 * @author Shilad Sen
 * Supports autocomplete queries on some entities.
 *
 * The key should be a unique identifier for an entity such as a database id.
 * The value should contain data that your application needs to provide about
 * the entity in the external autocomplete interface, such as a name or
 * description.
 *
 * The keys must implement reasonable hashCode, compareTo, and equals methods.
 */
public class AutocompleteTree<K extends Comparable, V> {
    /** Cleans and tokenizes entity names */
    private Fragmenter<K, V> fragmenter;

    /** Compares Autocomplete entires by frequency.  Ties broken by key */
    private Comparator<AutocompleteEntry<K, V>> FREQ_COMPARATOR =
        new Comparator<AutocompleteEntry<K, V>>() {
            public int compare(AutocompleteEntry<K, V> e1, AutocompleteEntry<K, V> e2) {
                int r = e2.getFrequency() - e1.getFrequency();
                if (r == 0) {
                    return e1.getKey().compareTo(e2.getKey());
                }
                return r;
            }
    };

    /** Mapping from keys to ac entries */
    private final Map<K, AutocompleteEntry<K, V>> map = new HashMap<K, AutocompleteEntry<K, V>>();

    /** Alpha sorted autocomplete tree */
    private final TreeSet<AutocompleteFragment<K, V>> tree = new TreeSet<AutocompleteFragment<K, V>>();

    /** Cache for common queries */
    private final Map<String, TreeSet<AutocompleteEntry<K, V>>> cache = new HashMap<String, TreeSet<AutocompleteEntry<K, V>>>();

    /** Maximum query length for entries in the cache. */
    private int maxCacheQueryLength = 2;

    /** Number of results to cache for each query */
    private int numCacheResults = 20;

    public AutocompleteTree() {
        fragmenter = new SimpleFragmenter<K, V>();
    }

    public AutocompleteTree(Fragmenter<K, V> fragmenter) {
        this.fragmenter = fragmenter;
    }

    public int getMaxCacheQueryLength() {
        return maxCacheQueryLength;
    }

    /**
     * Set the maximumum length for queries that are cached.
     * Also clears the cache.
     * @param maxCacheQueryLength
     */
    public void setMaxCacheQueryLength(int maxCacheQueryLength) {
        synchronized (cache) {
            this.maxCacheQueryLength = maxCacheQueryLength;
            cache.clear();
        }
    }

    public int getNumCacheResults() {
        return numCacheResults;
    }

    /**
     * Sets the maximum number of results that are cached for each query.
     * @param numCacheResults
     */
    public void setNumCacheResults(int numCacheResults) {
        synchronized (cache) {
            this.numCacheResults = numCacheResults;
            cache.clear();
        }
    }
    
    public void add(AutocompleteEntry<K, V> entry) {
        synchronized (map) {
            if (map.containsKey(entry.getKey())) {
        //            throw new IllegalArgumentException("entry for " + entry.getKey() + " already exists");
                return;
            }
            map.put(entry.getKey(), entry);
            // Populate fragments
            entry.clearFragments();
            for (String fragment : fragmenter.getFragments(entry)) {
                entry.addFragment(new AutocompleteFragment<K, V>(entry, fragment));
            }
            entry.freezeFragments();
            synchronized (tree) {
                // Add the data structures.
                tree.addAll(entry.getFragments());
            }
            adjustCacheForIncreasedFrequency(entry);
        }
    }

    public void remove(K key) {
        synchronized (map) {
            AutocompleteEntry entry = map.get(key);
            if (entry == null) {
    //            throw new IllegalArgumentException("unknown key: " + key);
                return;
            }
            map.remove(entry.getKey());
            synchronized (tree) {
                tree.removeAll(entry.getFragments());
            }
            adjustCacheForDecreasedFrequency(entry);
        }
    }

    private void adjustCacheForDecreasedFrequency(AutocompleteEntry<K, V> entry) {
        synchronized(cache) {
            for (Object o : entry.getFragments()) {
                // hack for unknown reasons
                AutocompleteFragment<K, V> fragment = (AutocompleteFragment<K, V>)o;
                for (int n = 1; n <= maxCacheQueryLength; n++) {
                    if (fragment.getFragment().length() >= n) {
                        String prefix = fragment.getFragment().substring(0, n);
                        SortedSet<AutocompleteEntry<K, V>> results = cache.get(prefix);
                        if (results != null && results.contains(entry)) {
                            cache.remove(prefix);
                        }
                    }
                }
            }
        }
    }

    private void adjustCacheForIncreasedFrequency(AutocompleteEntry<K, V> entry) {
        // Check to see if the new entry will make the cut in the cache.
        synchronized(cache) {
            for (AutocompleteFragment<K, V> fragment : entry.getFragments()) {
                for (int n = 1; n <= maxCacheQueryLength; n++) {
                    if (fragment.getFragment().length() >= n) {
                        String prefix = fragment.getFragment().substring(0, n);
                        SortedSet<AutocompleteEntry<K, V>> results = cache.get(prefix);
                        if (results != null) {
                            if (results.size() < numCacheResults ||
                                    results.last().getFrequency() <= entry.getFrequency()) {
                                cache.remove(prefix);
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean contains(K key) {
        synchronized (map) {
            return map.containsKey(key);
        }
    }
    
    public AutocompleteEntry<K, V> get(K key) {
        synchronized (map) {
            return map.get(key);
        }
    }

    public void increment(K key) {
        setFrequency(key, get(key).getFrequency() + 1);
    }

    public void decrement(K key) {
        setFrequency(key, get(key).getFrequency() - 1);
    }


    public void setFrequency(K key, int frequency) {
        synchronized (map) {
            AutocompleteEntry entry = map.get(key);
            if (entry == null) {
                throw new IllegalArgumentException("unknown key: " + key);
            }

            synchronized (tree) {
                // Not sure why this has to loop over objects, but I couldn't get it
                // to work properly with generics.
                for (Object o : entry.getFragments()) {
                    AutocompleteFragment<K, V> fragment = (AutocompleteFragment<K, V>)o;
                    tree.remove(fragment);
                }
                int df = frequency - entry.getFrequency();
                if (df < 0) {
                    adjustCacheForDecreasedFrequency(entry);
                } else if (df > 0) {
                    adjustCacheForIncreasedFrequency(entry);
                }
                entry.setFrequency(frequency);
                for (Object o : entry.getFragments()) {
                    AutocompleteFragment<K, V> fragment = (AutocompleteFragment<K, V>)o;
                    tree.add(fragment);
                }
            }
        }

    }


    
    public SortedSet<AutocompleteEntry<K, V>> autocomplete(String query, int maxResults) {
        String start = fragmenter.normalize(query);
        TreeSet<AutocompleteEntry<K, V>> results = null;

        // Check the cache
        if (start.length() <= maxCacheQueryLength) {
            synchronized (cache) {
                results = cache.get(start);
            }
        }
        
        if (results == null) {
            int n = maxResults;
            if (start.length() <= maxCacheQueryLength) {
                n = Math.max(n, numCacheResults);
            }
            AutocompleteFragment<K, V> startWrapper = new AutocompleteFragment(null, start);
            String end = start.substring(0, start.length()-1);
            end += (char)(start.charAt(start.length()-1) + 1);
            AutocompleteFragment<K, V> endWrapper = new AutocompleteFragment(null, end);
            results = new TreeSet<AutocompleteEntry<K, V>>(FREQ_COMPARATOR);

            synchronized (tree) {
                for (AutocompleteFragment<K, V> fragment : tree.subSet(startWrapper, endWrapper)) {
                    if (results.size() < n) {
                        results.add(fragment.getEntry());
                    } else if (FREQ_COMPARATOR.compare(fragment.getEntry(), results.last()) < 0) {
                        results.remove(results.last());
                        results.add(fragment.getEntry());
                    }
                }
            }
        }

        if (start.length() <= maxCacheQueryLength) {
            synchronized (cache) {
                cache.put(start, results);
            }
        }

        // Truncate if necessary (may be because of cache fills)
        if (results.size() > maxResults) {
            TreeSet<AutocompleteEntry<K, V>> truncated = new TreeSet<AutocompleteEntry<K, V>>(FREQ_COMPARATOR);
            Iterator<AutocompleteEntry<K, V>> iterator = results.iterator();
            for (int i = 0; i < maxResults; i++) {
                truncated.add(iterator.next());
            }
            results = truncated;
        }

        return results;
    }
}
