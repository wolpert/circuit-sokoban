package com.circuitsokoban.render;

import com.badlogic.gdx.graphics.Color;

/**
 * Flat, abstract colour scheme. Kept in one place so the whole look can be
 * retuned without touching rendering code.
 */
public final class Palette {

    private Palette() {}

    public static final Color BACKGROUND = new Color(0x10131aff);

    // Tiles (subtle checker for depth readability).
    public static final Color TILE_LIGHT = new Color(0x2a3142ff);
    public static final Color TILE_DARK = new Color(0x232a39ff);
    public static final Color TILE_BORDER = new Color(0x3a465bff);
    public static final Color ICE = new Color(0x5f8aa6ff);      // slick blue-grey slide tile
    public static final Color ICE_SHEEN = new Color(0x9fc4d8ff);

    // Terminals.
    public static final Color SOURCE = new Color(0xffb24dff);   // warm
    public static final Color RECEIVER = new Color(0x4dd0ffff); // cool

    // Connectors.
    public static final Color WIRE_IDLE = new Color(0x8792a8ff);      // aligned but not powered
    public static final Color WIRE_ENERGIZED = new Color(0x7CF6b0ff); // carrying power
    public static final Color JOINT = new Color(0xc7d0e0ff);

    // Player token.
    public static final Color PLAYER = new Color(0xff5d73ff);
}
