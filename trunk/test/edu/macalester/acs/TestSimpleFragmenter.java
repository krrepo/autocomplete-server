package edu.macalester.acs;

import junit.framework.TestCase;

/**
 * Author: Shilad Sen
 * Date: Dec 20, 2009
 */
public class TestSimpleFragmenter extends TestCase {
    private SimpleFragmenter frag = new SimpleFragmenter();

    public void testNormalize() {
        assertEquals(frag.normalize("asdf"), "asdf");
        assertEquals(frag.normalize("Asdf"), "asdf");
        assertEquals(frag.normalize("Asdf a"), "asdf a");
        assertEquals(frag.normalize("Asdf  a"), "asdf a");
        assertEquals(frag.normalize("Asdf  a'f"), "asdf af");
        assertEquals(frag.normalize("Asdf  a.!f"), "asdf a f");
        assertEquals(frag.normalize(" Asdf  a.!f!"), "asdf a f");
    }
}
