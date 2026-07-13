package com.circuitsokoban.game;

/**
 * A one-time, text-free tutorial shown the first time a mechanic appears. Each
 * lesson points the player at the cell to act on and animates the gesture.
 *
 * <p>To teach a NEW piece: add a value here, a detection rule in
 * {@link Tutorials#firstUnseen}, and a glyph case in
 * {@code render.TutorialOverlay}. Those three spots are the whole extension seam.
 */
public enum Lesson {
    BASICS, // move + rotate
    DIODE,
    ICE,
    GATE
}
