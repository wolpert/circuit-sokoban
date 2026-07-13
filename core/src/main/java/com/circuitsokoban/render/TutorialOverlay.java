package com.circuitsokoban.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.MathUtils;
import com.circuitsokoban.game.Lesson;
import com.circuitsokoban.model.Board;
import com.circuitsokoban.model.Direction;
import com.circuitsokoban.model.Piece;
import com.circuitsokoban.model.PieceType;
import com.circuitsokoban.model.Pos;

/**
 * Draws a wholly text-free tutorial: it dims the board, rings the cell the player
 * should act on, and animates the gesture (a rotating arc for rotate, a marching
 * arrow for move/slide, a pulsing link for the gate's dependency).
 *
 * <p>Add a {@code case} in {@link #draw} to teach a new piece (see
 * {@link Lesson}). Assumes blending is enabled and the projection is the board
 * camera; runs its own begin/end batches.
 */
public final class TutorialOverlay {

    private static final Color GUIDE = new Color(0xffe066ff);   // rotate / point
    private static final Color MOVE = new Color(0x8fd6ffff);    // move / slide
    private static final Color LINK = new Color(0xff8fceff);    // dependency link

    private final IsoProjector iso;
    private final float worldW;
    private final float worldH;

    public TutorialOverlay(IsoProjector iso, float worldW, float worldH) {
        this.iso = iso;
        this.worldW = worldW;
        this.worldH = worldH;
    }

    public void draw(ShapeRenderer sr, Board board, Lesson lesson, float time) {
        sr.begin(ShapeType.Filled);
        sr.setColor(0f, 0f, 0f, 0.5f); // scrim to focus attention
        sr.rect(0f, 0f, worldW, worldH);

        switch (lesson) {
            case BASICS -> drawBasics(sr, board, time);
            case DIODE -> ringAndRotate(sr, firstType(board, PieceType.DIODE), time);
            case ICE -> drawIce(sr, board, time);
            case GATE -> drawGate(sr, board, time);
        }
        sr.end();
    }

    private void drawBasics(ShapeRenderer sr, Board board, float time) {
        // Rotate: ring + arc on a piece next to the player.
        Pos player = board.player();
        for (Direction d : Direction.values()) {
            Pos n = player.step(d);
            if (board.pieceAt(n) != null) {
                ringAndRotate(sr, n, time);
                break;
            }
        }
        // Move: a marching arrow from the player toward an open tile.
        for (Direction d : Direction.values()) {
            if (board.isStandable(player.step(d))) {
                arrow(sr, player, d, time);
                break;
            }
        }
    }

    private void drawIce(ShapeRenderer sr, Board board, float time) {
        Pos ice = firstIce(board);
        if (ice == null) {
            return;
        }
        ring(sr, ice, time, GUIDE);
        // Show the slide with a marching arrow across the tile toward open space.
        for (Direction d : Direction.values()) {
            if (board.isStandable(ice.step(d))) {
                arrow(sr, ice, d, time);
                return;
            }
        }
    }

    private void drawGate(ShapeRenderer sr, Board board, float time) {
        Pos gate = firstType(board, PieceType.GATE);
        Pos key = board.source2().pos().step(board.source2().opening()); // the secondary's middle piece
        if (gate != null) {
            ring(sr, gate, time, LINK);
        }
        // The action is completing the secondary: ring + rotate its key, linked to the gate.
        ringAndRotate(sr, key, time);
        if (gate != null) {
            link(sr, key, gate, time);
        }
    }

    // ---- glyphs ----

    private void ringAndRotate(ShapeRenderer sr, Pos cell, float time) {
        if (cell == null) {
            return;
        }
        ring(sr, cell, time, GUIDE);
        rotateArc(sr, cell, time);
    }

    private void ring(ShapeRenderer sr, Pos cell, float time, Color color) {
        float cx = iso.worldX(cell.x(), cell.y());
        float cy = iso.worldY(cell.x(), cell.y());
        float radius = iso.halfW() * 0.9f * (1f + 0.05f * MathUtils.sin(time * 5f));
        sr.setColor(color.r, color.g, color.b, 0.95f);
        int n = 18;
        float rot = time * 1.2f;
        for (int i = 0; i < n; i++) {
            float a = rot + i * MathUtils.PI2 / n;
            sr.circle(cx + MathUtils.cos(a) * radius, cy + MathUtils.sin(a) * radius, 3.5f, 8);
        }
    }

    private void rotateArc(ShapeRenderer sr, Pos cell, float time) {
        float cx = iso.worldX(cell.x(), cell.y());
        float cy = iso.worldY(cell.x(), cell.y());
        float radius = iso.halfW() * 0.55f;
        float start = time * 2.2f;
        float sweep = MathUtils.PI2 * 0.78f;
        sr.setColor(GUIDE.r, GUIDE.g, GUIDE.b, 1f);
        int n = 14;
        for (int i = 0; i < n; i++) {
            float a = start + sweep * i / (n - 1);
            sr.circle(cx + MathUtils.cos(a) * radius, cy + MathUtils.sin(a) * radius, 3.2f, 8);
        }
        float ae = start + sweep;
        arrowHead(sr, cx + MathUtils.cos(ae) * radius, cy + MathUtils.sin(ae) * radius,
                ae + MathUtils.HALF_PI, 9f); // tangent to the arc
    }

    /** A marching dotted arrow from cell centre one tile toward {@code d}. */
    private void arrow(ShapeRenderer sr, Pos cell, Direction d, float time) {
        float sx = iso.worldX(cell.x(), cell.y());
        float sy = iso.worldY(cell.x(), cell.y());
        float ex = iso.worldX(cell.x() + d.dx, cell.y() + d.dy);
        float ey = iso.worldY(cell.x() + d.dx, cell.y() + d.dy);
        int n = 7;
        for (int i = 0; i < n; i++) {
            float t = i / (n - 1f);
            float bright = 0.35f + 0.65f * (0.5f + 0.5f * MathUtils.sin(time * 6f - i * 0.7f));
            sr.setColor(MOVE.r, MOVE.g, MOVE.b, bright);
            sr.circle(MathUtils.lerp(sx, ex, t), MathUtils.lerp(sy, ey, t), 4.5f, 8);
        }
        arrowHead(sr, ex, ey, MathUtils.atan2(ey - sy, ex - sx), 11f);
    }

    /** A pulsing dotted link between two cells (gate <- key dependency). */
    private void link(ShapeRenderer sr, Pos a, Pos b, float time) {
        float ax = iso.worldX(a.x(), a.y());
        float ay = iso.worldY(a.x(), a.y());
        float bx = iso.worldX(b.x(), b.y());
        float by = iso.worldY(b.x(), b.y());
        int n = 10;
        for (int i = 0; i < n; i++) {
            float t = i / (n - 1f);
            float bright = 0.3f + 0.7f * (0.5f + 0.5f * MathUtils.sin(time * 5f - i * 0.6f));
            sr.setColor(LINK.r, LINK.g, LINK.b, bright);
            sr.circle(MathUtils.lerp(ax, bx, t), MathUtils.lerp(ay, by, t), 3f, 8);
        }
    }

    private void arrowHead(ShapeRenderer sr, float x, float y, float angle, float size) {
        float back = angle + MathUtils.PI;
        float spread = 0.5f;
        sr.triangle(x, y,
                x + MathUtils.cos(back + spread) * size, y + MathUtils.sin(back + spread) * size,
                x + MathUtils.cos(back - spread) * size, y + MathUtils.sin(back - spread) * size);
    }

    // ---- board scanning ----

    private Pos firstType(Board board, PieceType type) {
        for (int y = 0; y < board.height(); y++) {
            for (int x = 0; x < board.width(); x++) {
                Piece p = board.pieceAt(new Pos(x, y));
                if (p != null && p.type() == type) {
                    return new Pos(x, y);
                }
            }
        }
        return null;
    }

    private Pos firstIce(Board board) {
        for (int y = 0; y < board.height(); y++) {
            for (int x = 0; x < board.width(); x++) {
                if (board.isIce(new Pos(x, y))) {
                    return new Pos(x, y);
                }
            }
        }
        return null;
    }
}
