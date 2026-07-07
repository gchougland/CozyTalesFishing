package com.hexvane.cozytalefishing.fishing;

/** Which viewers receive a fishing-line segment entity chain. */
public enum LineSegmentAudience {
    /** Visible only to the line owner (first-person rod tip anchor). */
    FIRST_PERSON_OWNER,
    /** Visible to everyone except the line owner (third-person rod tip anchor). */
    THIRD_PERSON_WORLD
}
