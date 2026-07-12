package com.circuitsokoban.render;

import com.badlogic.gdx.math.Vector2;
import com.circuitsokoban.model.Direction;
import com.circuitsokoban.model.Pos;

/**
 * Converts logical grid coordinates to on-screen isometric (2:1 diamond) world
 * coordinates. This is the <em>only</em> place the game becomes isometric &mdash;
 * the model is a plain orthogonal grid.
 *
 * <p>World space is y-up (libGDX convention). Increasing {@code gx+gy} moves a
 * tile down the screen; increasing {@code gx-gy} moves it right.
 */
public final class IsoProjector {

    private final float halfW; // half a tile's screen width
    private final float halfH; // half a tile's screen height (== halfW/2 for a 2:1 look)
    private float originX;
    private float originY;

    public IsoProjector(float tileWidth) {
        this.halfW = tileWidth / 2f;
        this.halfH = tileWidth / 4f;
    }

    public float halfW() { return halfW; }
    public float halfH() { return halfH; }

    /** Centres a {@code cols x rows} board inside a {@code worldW x worldH} viewport. */
    public void centerBoard(int cols, int rows, float worldW, float worldH) {
        setBoardCenter(cols, rows, worldW / 2f, worldH / 2f);
    }

    /** Places the visual centre of a {@code cols x rows} board at ({@code centerX}, {@code centerY}). */
    public void setBoardCenter(int cols, int rows, float centerX, float centerY) {
        originX = centerX;
        originY = centerY + ((cols - 1 + rows - 1) / 2f) * halfH;
    }

    public float worldX(int gx, int gy) {
        return originX + (gx - gy) * halfW;
    }

    public float worldY(int gx, int gy) {
        return originY - (gx + gy) * halfH;
    }

    public Vector2 center(Pos p, Vector2 out) {
        return out.set(worldX(p.x(), p.y()), worldY(p.x(), p.y()));
    }

    /** Inverse projection: the grid cell whose centre is nearest world point (wx, wy). */
    public Pos unproject(float wx, float wy) {
        float diff = (wx - originX) / halfW;   // == gx - gy
        float sum = (originY - wy) / halfH;     // == gx + gy
        int gx = Math.round((sum + diff) / 2f);
        int gy = Math.round((sum - diff) / 2f);
        return new Pos(gx, gy);
    }

    /**
     * The grid direction whose on-screen projection best matches a swipe of
     * ({@code screenDx}, {@code screenDy}) in screen pixels (y grows downward).
     * Uses the actual projected axes, so it always agrees with what's drawn.
     */
    public Direction directionForSwipe(float screenDx, float screenDy) {
        float worldDy = -screenDy; // screen y is down, world y is up
        Direction best = Direction.EAST;
        float bestDot = Float.NEGATIVE_INFINITY;
        for (Direction d : Direction.values()) {
            float vx = (d.dx - d.dy) * halfW;
            float vy = -(d.dx + d.dy) * halfH;
            float dot = vx * screenDx + vy * worldDy;
            if (dot > bestDot) {
                bestDot = dot;
                best = d;
            }
        }
        return best;
    }
}
