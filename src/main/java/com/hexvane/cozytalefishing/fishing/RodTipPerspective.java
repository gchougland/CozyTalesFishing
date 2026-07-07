package com.hexvane.cozytalefishing.fishing;

/** Which held-rod view model rod-tip offsets apply to. */
public enum RodTipPerspective {
    /** Eye position + FPS shortbow-style attach offsets (line owner view). */
    FIRST_PERSON,
    /** Body/hand anchor + third-person hold offsets (observers). */
    THIRD_PERSON
}
