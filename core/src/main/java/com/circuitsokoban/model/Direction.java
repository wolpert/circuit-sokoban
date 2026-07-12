package com.circuitsokoban.model;

/**
 * The four orthogonal directions on the logical grid.
 *
 * <p>The game logic is entirely orthogonal (a plain 2D grid); the isometric
 * look is purely a render-time transform (see {@code render.IsoProjector}). So
 * nothing in this package knows or cares about isometry.
 *
 * <p>Ordinals are ordered clockwise (N, E, S, W) so that {@link #rotateCW()} is
 * a simple {@code (ordinal + 1) % 4}. Each direction owns a single bit
 * ({@code 1 << ordinal}) used to build connector opening masks.
 */
public enum Direction {
    NORTH(0, 1),
    EAST(1, 0),
    SOUTH(0, -1),
    WEST(-1, 0);

    /** Grid delta. y increases toward NORTH; the renderer maps this to iso space. */
    public final int dx;
    public final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    /** Bit for this direction within a connector opening mask. */
    public int bit() {
        return 1 << ordinal();
    }

    public Direction opposite() {
        return values()[(ordinal() + 2) % 4];
    }

    public Direction rotateCW() {
        return values()[(ordinal() + 1) % 4];
    }

    public Direction rotateCCW() {
        return values()[(ordinal() + 3) % 4];
    }

    /**
     * Rotates an opening mask clockwise by the given number of 90&deg; steps.
     * Rotating a piece is exactly this operation on its openings.
     */
    public static int rotateMask(int mask, int steps) {
        steps = ((steps % 4) + 4) % 4;
        int result = 0;
        for (Direction d : values()) {
            if ((mask & d.bit()) != 0) {
                Direction rotated = values()[(d.ordinal() + steps) % 4];
                result |= rotated.bit();
            }
        }
        return result;
    }
}
