package com.circuitsokoban.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector2;
import com.circuitsokoban.model.Board;
import com.circuitsokoban.model.Circuit;
import com.circuitsokoban.model.Direction;
import com.circuitsokoban.model.Piece;
import com.circuitsokoban.model.Pos;
import com.circuitsokoban.model.Terminal;

/**
 * Draws a {@link Board} isometrically with flat procedural shapes (no texture
 * assets). Static rendering only for now &mdash; movement/energize animation is
 * the later juice layer; here everything snaps to its grid cell.
 *
 * <p>The caller owns the {@link ShapeRenderer} and must have set its projection
 * matrix to the camera before calling {@link #render}. This method runs its own
 * begin/end batches.
 */
public final class BoardRenderer {

    private final IsoProjector iso;
    private final Vector2 b = new Vector2();

    public BoardRenderer(IsoProjector iso) {
        this.iso = iso;
    }

    public void render(ShapeRenderer sr, Board board, Circuit.Result circuit) {
        drawTileFills(sr, board);
        drawTileBorders(sr, board);
        drawConnectors(sr, board, circuit);
        drawPlayer(sr, board);
    }

    private void drawTileFills(ShapeRenderer sr, Board board) {
        sr.begin(ShapeType.Filled);
        for (int y = 0; y < board.height(); y++) {
            for (int x = 0; x < board.width(); x++) {
                Pos p = new Pos(x, y);
                Color c = Palette.TILE_LIGHT;
                if ((x + y) % 2 == 0) {
                    c = Palette.TILE_DARK;
                }
                if (board.source().pos().equals(p)) {
                    c = Palette.SOURCE;
                } else if (board.receiver().pos().equals(p)) {
                    c = Palette.RECEIVER;
                }
                fillDiamond(sr, iso.worldX(x, y), iso.worldY(x, y), c);
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
                sr.line(cx, cy + hh, cx + hw, cy); // top -> right
                sr.line(cx + hw, cy, cx, cy - hh);  // right -> bottom
                sr.line(cx, cy - hh, cx - hw, cy);  // bottom -> left
                sr.line(cx - hw, cy, cx, cy + hh);  // left -> top
            }
        }
        sr.end();
    }

    private void drawConnectors(ShapeRenderer sr, Board board, Circuit.Result circuit) {
        float armWidth = iso.halfH() * 0.34f;
        float jointR = iso.halfH() * 0.42f;

        sr.begin(ShapeType.Filled);
        // Terminal stubs: a short arm from each terminal toward its opening.
        drawTerminalStub(sr, board.source(), Palette.SOURCE, armWidth, jointR);
        drawTerminalStub(sr, board.receiver(), Palette.RECEIVER, armWidth, jointR);

        for (int y = 0; y < board.height(); y++) {
            for (int x = 0; x < board.width(); x++) {
                Pos p = new Pos(x, y);
                Piece piece = board.pieceAt(p);
                if (piece == null) {
                    continue;
                }
                Color wire = circuit.energized().contains(p)
                        ? Palette.WIRE_ENERGIZED : Palette.WIRE_IDLE;
                sr.setColor(wire);
                float cx = iso.worldX(x, y);
                float cy = iso.worldY(x, y);
                for (Direction d : Direction.values()) {
                    if (piece.hasOpening(d)) {
                        halfwayToNeighbor(x, y, d, b);
                        sr.rectLine(cx, cy, b.x, b.y, armWidth);
                    }
                }
                sr.setColor(Palette.JOINT);
                sr.circle(cx, cy, jointR, 20);
            }
        }
        sr.end();
    }

    private void drawTerminalStub(ShapeRenderer sr, Terminal t, Color color, float armWidth, float jointR) {
        sr.setColor(color);
        float cx = iso.worldX(t.pos().x(), t.pos().y());
        float cy = iso.worldY(t.pos().x(), t.pos().y());
        halfwayToNeighbor(t.pos().x(), t.pos().y(), t.opening(), b);
        sr.rectLine(cx, cy, b.x, b.y, armWidth);
        sr.circle(cx, cy, jointR * 0.9f, 20);
    }

    private void drawPlayer(ShapeRenderer sr, Board board) {
        sr.begin(ShapeType.Filled);
        sr.setColor(Palette.PLAYER);
        Pos p = board.player();
        float cx = iso.worldX(p.x(), p.y());
        float cy = iso.worldY(p.x(), p.y()) + iso.halfH() * 0.4f; // lift a touch so it "stands"
        sr.circle(cx, cy, iso.halfH() * 0.55f, 24);
        sr.end();
    }

    /** Point halfway between cell (x,y)'s centre and its neighbour in direction d. */
    private void halfwayToNeighbor(int x, int y, Direction d, Vector2 out) {
        float cx = iso.worldX(x, y);
        float cy = iso.worldY(x, y);
        float nx = iso.worldX(x + d.dx, y + d.dy);
        float ny = iso.worldY(x + d.dx, y + d.dy);
        out.set((cx + nx) / 2f, (cy + ny) / 2f);
    }

    private void fillDiamond(ShapeRenderer sr, float cx, float cy, Color c) {
        float hw = iso.halfW();
        float hh = iso.halfH();
        sr.setColor(c);
        // top, right, bottom, left
        sr.triangle(cx, cy + hh, cx + hw, cy, cx, cy - hh);
        sr.triangle(cx, cy + hh, cx, cy - hh, cx - hw, cy);
    }
}
