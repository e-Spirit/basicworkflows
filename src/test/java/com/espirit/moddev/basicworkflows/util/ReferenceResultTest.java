package com.espirit.moddev.basicworkflows.util;

import org.junit.Before;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(Theories.class)
public class ReferenceResultTest {

    private ReferenceResult testling;

    @Before
    public void setUp() throws Exception {
        testling = new ReferenceResult();
    }

    @DataPoint
    public static boolean value_1 = true;

    @DataPoint
    public static boolean value_2 = false;

    @Theory
    public void testHasReleaseIssues(boolean allObjectsReleased, boolean notMediaReleased, boolean onlyMedia, boolean noBrokenReferences,
                                     boolean releaseWithMedia)
        throws Exception {

        testling.setAllObjectsReleased(allObjectsReleased);
        testling.setNoBrokenReferences(noBrokenReferences);
        testling.setNotMediaReleased(notMediaReleased);
        testling.setOnlyMedia(onlyMedia);

        final boolean hasReleaseIssues = testling.hasReleaseIssues(releaseWithMedia);

        final boolean releasedStuff = (allObjectsReleased && !releaseWithMedia) || (notMediaReleased && releaseWithMedia) || (onlyMedia
                                                                                                                              && releaseWithMedia);
        final boolean expected = !(releasedStuff && noBrokenReferences);

        assertEquals("all released: " + releasedStuff + " - broken references: " + noBrokenReferences, expected,
                     hasReleaseIssues);
    }
}
