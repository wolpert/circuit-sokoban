package com.circuitsokoban.input;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.circuitsokoban.game.PlayController;
import com.circuitsokoban.model.Board;
import com.circuitsokoban.model.Direction;
import com.circuitsokoban.model.Pos;
import com.circuitsokoban.render.IsoProjector;

/**
 * Single {@link com.badlogic.gdx.InputProcessor} serving both platforms, so
 * touch and mouse/keyboard share one code path (no per-platform duplication):
 *
 * <ul>
 *   <li><b>Desktop:</b> arrow keys / WASD walk-and-push; click an adjacent piece
 *       to rotate it; R rotates the piece the avatar faces; Z / Y undo / redo.</li>
 *   <li><b>Touch:</b> swipe to walk-and-push; tap an adjacent piece to rotate it
 *       (tapping an adjacent empty tile also steps there, which is handy on
 *       desktop with the mouse).</li>
 * </ul>
 *
 * <p>Movement keys are screen-relative and map to the four isometric diagonals
 * in a consistent rotation: RIGHT&rarr;EAST, UP&rarr;SOUTH, LEFT&rarr;WEST,
 * DOWN&rarr;NORTH.
 */
public final class GameInput extends InputAdapter {

    private static final float SWIPE_MIN_PX = 24f;

    private final PlayController controller;
    private final Viewport viewport;
    private final IsoProjector iso;

    private final Vector2 tmp = new Vector2();
    private float downX;
    private float downY;
    private Direction lastDir = Direction.EAST;

    public GameInput(PlayController controller, Viewport viewport, IsoProjector iso) {
        this.controller = controller;
        this.viewport = viewport;
        this.iso = iso;
    }

    @Override
    public boolean keyDown(int keycode) {
        switch (keycode) {
            case Input.Keys.RIGHT, Input.Keys.D -> move(Direction.EAST);
            case Input.Keys.UP, Input.Keys.W -> move(Direction.SOUTH);
            case Input.Keys.LEFT, Input.Keys.A -> move(Direction.WEST);
            case Input.Keys.DOWN, Input.Keys.S -> move(Direction.NORTH);
            case Input.Keys.R, Input.Keys.E -> rotateFacing();
            case Input.Keys.Z -> controller.undo();
            case Input.Keys.Y -> controller.redo();
            default -> { return false; }
        }
        return true;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        downX = screenX;
        downY = screenY;
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        float dx = screenX - downX;
        float dy = screenY - downY;
        if (Vector2.len(dx, dy) >= SWIPE_MIN_PX) {
            move(iso.directionForSwipe(dx, dy));
        } else {
            handleTap(screenX, screenY);
        }
        return true;
    }

    private void handleTap(int screenX, int screenY) {
        viewport.unproject(tmp.set(screenX, screenY));
        Pos cell = iso.unproject(tmp.x, tmp.y);
        Board board = controller.board();
        if (!board.inBounds(cell)) {
            return;
        }
        Direction toCell = adjacentDirection(board.player(), cell);
        if (toCell == null) {
            return; // not adjacent to the avatar; ignore
        }
        if (board.pieceAt(cell) != null) {
            controller.rotate(cell);
        } else {
            move(toCell);
        }
    }

    private void move(Direction d) {
        if (controller.step(d).isLegal()) {
            lastDir = d;
        }
    }

    /** Rotate the piece the avatar faces; failing that, any adjacent piece. */
    private void rotateFacing() {
        Board board = controller.board();
        Pos player = board.player();
        Pos facing = player.step(lastDir);
        if (board.pieceAt(facing) != null) {
            controller.rotate(facing);
            return;
        }
        for (Direction d : Direction.values()) {
            Pos p = player.step(d);
            if (board.pieceAt(p) != null) {
                controller.rotate(p);
                return;
            }
        }
    }

    private static Direction adjacentDirection(Pos from, Pos to) {
        for (Direction d : Direction.values()) {
            if (from.step(d).equals(to)) {
                return d;
            }
        }
        return null;
    }
}
