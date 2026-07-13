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
    // Desaturated steel-blue so it reads as a frosted floor, not the vivid cyan
    // receiver terminal; the bright frost crystal on top is the real "ice" cue.
    public static final Color ICE = new Color(0x566d7dff);
    public static final Color ICE_SHEEN = new Color(0xc3dae6ff);

    // Terminals.
    public static final Color SOURCE = new Color(0xffb24dff);   // warm
    public static final Color RECEIVER = new Color(0x4dd0ffff); // cool
    public static final Color SOURCE2 = new Color(0x9b7cf0ff);   // secondary circuit: violet
    public static final Color RECEIVER2 = new Color(0x3fc7b0ff); // secondary circuit: teal

    // Gate bar: red when locked, green when the secondary circuit has opened it.
    public static final Color GATE_LOCKED = new Color(0xe05c6aff);
    public static final Color GATE_OPEN = new Color(0x7cf6b0ff);

    // Fuse: a one-use connector; warm coral wire with a bright "crack" marker.
    public static final Color FUSE = new Color(0xff7a4dff);
    public static final Color FUSE_GLYPH = new Color(0xffd7b0ff);

    // Connectors.
    public static final Color WIRE_IDLE = new Color(0x8792a8ff);      // aligned but not powered
    public static final Color WIRE_ENERGIZED = new Color(0x7CF6b0ff); // carrying power
    public static final Color JOINT = new Color(0xc7d0e0ff);

    // Player token.
    public static final Color PLAYER = new Color(0xff5d73ff);
}
