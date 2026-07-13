package com.circuitsokoban.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.circuitsokoban.model.Board;
import com.circuitsokoban.model.Direction;
import com.circuitsokoban.model.Piece;
import com.circuitsokoban.model.PieceType;
import com.circuitsokoban.model.Pos;
import com.circuitsokoban.model.Terminal;

/**
 * Draws a {@link Board} isometrically with flat procedural shapes, reading all
 * animation state from a {@link BoardView} (offsets, smooth rotation, energize
 * sweep, idle pulse, player position, particles).
 *
 * <p>The caller owns the {@link ShapeRenderer} and must have set its projection
 * matrix; this class runs its own begin/end batches.
 */
public final class BoardRenderer {

    private final IsoProjector iso;
    private final Vector2 off = new Vector2();
    private final Vector2 end = new Vector2();
    private final Color wire = new Color();

    public BoardRenderer(IsoProjector iso) {
        this.iso = iso;
    }

    public void render(ShapeRenderer sr, Board board, BoardView view) {
        drawTileFills(sr, board);
        drawTileBorders(sr, board);
        drawConnectors(sr, board, view);
        drawPlayer(sr, view);
        drawParticles(sr, view);
    }

    private void drawTileFills(ShapeRenderer sr, Board board) {
        sr.begin(ShapeType.Filled);
        for (int y = 0; y < board.height(); y++) {
            for (int x = 0; x < board.width(); x++) {
                Pos p = new Pos(x, y);
                Color c = (x + y) % 2 == 0 ? Palette.TILE_DARK : Palette.TILE_LIGHT;
                if (board.isIce(p)) {
                    c = Palette.ICE;
                }
                if (board.source().pos().equals(p)) {
                    c = Palette.SOURCE;
                } else if (board.receiver().pos().equals(p)) {
                    c = Palette.RECEIVER;
                } else if (board.hasSecondary() && board.source2().pos().equals(p)) {
                    c = Palette.SOURCE2;
                } else if (board.hasSecondary() && board.receiver2().pos().equals(p)) {
                    c = Palette.RECEIVER2;
                }
                fillDiamond(sr, iso.worldX(x, y), iso.worldY(x, y), c);
                if (board.isIce(p)) {
                    drawIceSheen(sr, iso.worldX(x, y), iso.worldY(x, y));
                }
            }
        }
        sr.end();
    }

    private void drawTileBorders(ShapeRenderer sr, Board board) {
        sr.begin(ShapeType.Line);
        sr.setColor(Palette.TILE_BORDER);
        float hw = iso.halfW();
        float hh = iso.halfH();
        for (int y = 0; y < board.height(); y++) {
            for (int x = 0; x < board.width(); x++) {
                float cx = iso.worldX(x, y);
                float cy = iso.worldY(x, y);
                sr.line(cx, cy + hh, cx + hw, cy);
                sr.line(cx + hw, cy, cx, cy - hh);
                sr.line(cx, cy - hh, cx - hw, cy);
                sr.line(cx - hw, cy, cx, cy + hh);
            }
        }
        sr.end();
    }

    private void drawConnectors(ShapeRenderer sr, Board board, BoardView view) {
        float armWidth = iso.halfH() * 0.34f;
        float jointR = iso.halfH() * 0.42f;

        sr.begin(ShapeType.Filled);
        drawTerminalStub(sr, board.source(), Palette.SOURCE, armWidth, jointR);
        drawTerminalStub(sr, board.receiver(), Palette.RECEIVER, armWidth, jointR);
        if (board.hasSecondary()) {
            drawTerminalStub(sr, board.source2(), Palette.SOURCE2, armWidth, jointR);
            drawTerminalStub(sr, board.receiver2(), Palette.RECEIVER2, armWidth, jointR);
        }

        for (int y = 0; y < board.height(); y++) {
            for (int x = 0; x < board.width(); x++) {
                Pos p = new Pos(x, y);
                Piece piece = board.pieceAt(p);
                if (piece == null) {
                    continue;
                }
                view.offsetFor(p, off);
                float cx = iso.worldX(x, y) + off.x;
                float cy = iso.worldY(x, y) + off.y;

                if (piece.type() == PieceType.FUSE) {
                    wire.set(Palette.FUSE); // one-use fuse always reads warm, so it stands out
                } else if (view.isLit(p)) {
                    float f = view.pulse();
                    wire.set(Palette.WIRE_ENERGIZED.r * f, Palette.WIRE_ENERGIZED.g * f,
                            Palette.WIRE_ENERGIZED.b * f, 1f);
                } else {
                    wire.set(Palette.WIRE_IDLE);
                }
                sr.setColor(wire);

                float rot = view.rotationProgress(p);
                for (Direction d : Direction.values()) {
                    if (!piece.hasOpening(d)) {
                        continue;
                    }
                    if (rot >= 0f) {
                        // Sweep each arm from where it was (one step CCW) to its new direction.
                        armEndpoint(p, d.rotateCCW(), end);
                        float ex = end.x;
                        float ey = end.y;
                        armEndpoint(p, d, end);
                        sr.rectLine(cx, cy, MathUtils.lerp(ex, end.x, rot),
                                MathUtils.lerp(ey, end.y, rot), armWidth);
                    } else {
                        armEndpoint(p, d, end);
                        sr.rectLine(cx, cy, end.x + off.x, end.y + off.y, armWidth);
                    }
                }
                sr.setColor(Palette.JOINT);
                sr.circle(cx, cy, jointR, 20);

                if (piece.type() == PieceType.DIODE) {
                    drawDiodeArrow(sr, p, piece.flowDirection(), cx, cy);
                } else if (piece.type() == PieceType.GATE) {
                    drawGateBar(sr, p, piece, cx, cy, view.gatesUnlocked(), armWidth);
                } else if (piece.type() == PieceType.FUSE) {
                    drawFuseGlyph(sr, cx, cy);
                }
            }
        }
        sr.end();
    }

    /** A coloured bar across the gate's axis: red while locked, green once the secondary opens it. */
    private void drawGateBar(ShapeRenderer sr, Pos cell, Piece piece, float cx, float cy,
                             boolean unlocked, float armWidth) {
        Direction axis = firstOpening(piece);
        Direction perp = axis.rotateCW();
        armEndpoint(cell, perp, end);
        float vx = end.x - iso.worldX(cell.x(), cell.y());
        float vy = end.y - iso.worldY(cell.x(), cell.y());
        sr.setColor(unlocked ? Palette.GATE_OPEN : Palette.GATE_LOCKED);
        sr.rectLine(cx + vx * 0.85f, cy + vy * 0.85f, cx - vx * 0.85f, cy - vy * 0.85f, armWidth * 1.3f);
    }

    /** A small bright "crack" (X) at the fuse's centre, signalling it's fragile / one-use. */
    private void drawFuseGlyph(ShapeRenderer sr, float cx, float cy) {
        float s = iso.halfH() * 0.28f;
        sr.setColor(Palette.FUSE_GLYPH);
        sr.rectLine(cx - s, cy - s * 0.6f, cx + s, cy + s * 0.6f, 2.5f);
        sr.rectLine(cx - s, cy + s * 0.6f, cx + s, cy - s * 0.6f, 2.5f);
    }

    private static Direction firstOpening(Piece piece) {
        for (Direction d : Direction.values()) {
            if (piece.hasOpening(d)) {
                return d;
            }
        }
        return Direction.NORTH;
    }

    /** A dark arrowhead on the diode's flow arm, showing the one-way direction. */
    private void drawDiodeArrow(ShapeRenderer sr, Pos cell, Direction flow, float cx, float cy) {
        armEndpoint(cell, flow, end);
        float vx = end.x - iso.worldX(cell.x(), cell.y());
        float vy = end.y - iso.worldY(cell.x(), cell.y());
        float len = (float) Math.sqrt(vx * vx + vy * vy);
        if (len < 1e-3f) {
            return;
        }
        vx /= len;
        vy /= len;
        float apexX = cx + vx * len * 0.92f;
        float apexY = cy + vy * len * 0.92f;
        float baseX = cx + vx * len * 0.48f;
        float baseY = cy + vy * len * 0.48f;
        float px = -vy;
        float py = vx;
        float wingR = iso.halfH() * 0.3f;
        sr.setColor(Palette.BACKGROUND);
        sr.triangle(apexX, apexY, baseX + px * wingR, baseY + py * wingR,
                baseX - px * wingR, baseY - py * wingR);
    }

    private void drawTerminalStub(ShapeRenderer sr, Terminal t, Color color, float armWidth, float jointR) {
        sr.setColor(color);
        float cx = iso.worldX(t.pos().x(), t.pos().y());
        float cy = iso.worldY(t.pos().x(), t.pos().y());
        armEndpoint(t.pos(), t.opening(), end);
        sr.rectLine(cx, cy, end.x, end.y, armWidth);
        sr.circle(cx, cy, jointR * 0.9f, 20);
    }

    private void drawPlayer(ShapeRenderer sr, BoardView view) {
        sr.begin(ShapeType.Filled);
        sr.setColor(Palette.PLAYER);
        view.playerWorld(off);
        sr.circle(off.x, off.y + iso.halfH() * 0.4f, iso.halfH() * 0.55f, 24);
        sr.end();
    }

    private void drawParticles(ShapeRenderer sr, BoardView view) {
        if (view.particles().isEmpty()) {
            return;
        }
        sr.begin(ShapeType.Filled);
        for (Particle pt : view.particles()) {
            Color c = pt.color;
            sr.setColor(c.r, c.g, c.b, pt.alpha());
            if (pt.triangle) {
                triangle(sr, pt.x, pt.y, pt.size, pt.angle);
            } else {
                square(sr, pt.x, pt.y, pt.size, pt.angle);
            }
        }
        sr.end();
    }

    /** Midpoint between cell {@code p}'s centre and its neighbour in direction {@code d}. */
    private void armEndpoint(Pos p, Direction d, Vector2 out) {
        float cx = iso.worldX(p.x(), p.y());
        float cy = iso.worldY(p.x(), p.y());
        float nx = iso.worldX(p.x() + d.dx, p.y() + d.dy);
        float ny = iso.worldY(p.x() + d.dx, p.y() + d.dy);
        out.set((cx + nx) / 2f, (cy + ny) / 2f);
    }

    /** Two faint parallel streaks to read the tile as slippery. */
    private void drawIceSheen(ShapeRenderer sr, float cx, float cy) {
        float hw = iso.halfW();
        float hh = iso.halfH();
        sr.setColor(Palette.ICE_SHEEN);
        sr.rectLine(cx - hw * 0.32f, cy + hh * 0.06f, cx + hw * 0.06f, cy + hh * 0.26f, 2.5f);
        sr.rectLine(cx - hw * 0.06f, cy - hh * 0.26f, cx + hw * 0.32f, cy - hh * 0.06f, 2.5f);
    }

    private void fillDiamond(ShapeRenderer sr, float cx, float cy, Color c) {
        float hw = iso.halfW();
        float hh = iso.halfH();
        sr.setColor(c);
        sr.triangle(cx, cy + hh, cx + hw, cy, cx, cy - hh);
        sr.triangle(cx, cy + hh, cx, cy - hh, cx - hw, cy);
    }

    private void square(ShapeRenderer sr, float x, float y, float s, float angleDeg) {
        float h = s / 2f;
        float c = MathUtils.cosDeg(angleDeg);
        float sn = MathUtils.sinDeg(angleDeg);
        float x1 = -h * c - h * sn;
        float y1 = -h * sn + h * c;
        float x2 = h * c - h * sn;
        float y2 = h * sn + h * c;
        sr.triangle(x + x1, y + y1, x + x2, y + y2, x - x1, y - y1);
        sr.triangle(x + x2, y + y2, x - x1, y - y1, x - x2, y - y2);
    }

    private void triangle(ShapeRenderer sr, float x, float y, float s, float angleDeg) {
        float r = s * 0.6f;
        float a0 = angleDeg * MathUtils.degreesToRadians;
        sr.triangle(
                x + r * MathUtils.cos(a0), y + r * MathUtils.sin(a0),
                x + r * MathUtils.cos(a0 + MathUtils.PI2 / 3f), y + r * MathUtils.sin(a0 + MathUtils.PI2 / 3f),
                x + r * MathUtils.cos(a0 + 2f * MathUtils.PI2 / 3f), y + r * MathUtils.sin(a0 + 2f * MathUtils.PI2 / 3f));
    }
}
