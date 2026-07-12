package com.circuitsokoban.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.circuitsokoban.game.PlaySession;
import com.circuitsokoban.model.Board;
import com.circuitsokoban.model.Direction;
import com.circuitsokoban.model.Piece;
import com.circuitsokoban.model.PieceType;
import com.circuitsokoban.model.Pos;
import com.circuitsokoban.model.Terminal;
import com.circuitsokoban.render.IsoProjector;
import com.circuitsokoban.solver.Level;
import org.junit.jupiter.api.Test;

/**
 * Drives the input handler with no GL context (viewport/camera are pure math),
 * verifying the desktop key path end-to-end into the PlaySession.
 */
class GameInputTest {

    private GameInput inputFor(PlaySession session) {
        OrthographicCamera cam = new OrthographicCamera();
        // Note: not calling vp.update() — that needs a GL context and the keyboard
        // path under test never touches the viewport (only tap handling does).
        FitViewport vp = new FitViewport(540f, 960f, cam);
        IsoProjector iso = new IsoProjector(96f);
        iso.centerBoard(3, 3, 540f, 960f);
        return new GameInput(session, vp, iso);
    }

    private Level onePushLevel() {
        Terminal source = new Terminal(new Pos(0, 0), Direction.EAST);
        Terminal receiver = new Terminal(new Pos(2, 0), Direction.WEST);
        Board start = new Board(3, 3, source, receiver, new Pos(1, 2));
        start.setPiece(new Pos(1, 1), new Piece(PieceType.STRAIGHT, 1));
        return new Level(0L, start, start.copy(), 1, 1);
    }

    @Test
    void upArrowMapsToSouthPushAndSolves() {
        PlaySession s = new PlaySession(onePushLevel());
        GameInput in = inputFor(s);
        assertTrue(in.keyDown(Input.Keys.UP)); // UP -> SOUTH -> push (1,1)->(1,0)
        assertTrue(s.isSolved());
        assertEquals(1, s.moves());
    }

    @Test
    void undoKeyRevertsTheMove() {
        PlaySession s = new PlaySession(onePushLevel());
        GameInput in = inputFor(s);
        in.keyDown(Input.Keys.UP);
        assertTrue(in.keyDown(Input.Keys.Z));
        assertFalse(s.isSolved());
        assertEquals(0, s.moves());
    }

    @Test
    void rotateKeyRotatesFacedPiece() {
        PlaySession s = new PlaySession(onePushLevel());
        GameInput in = inputFor(s);
        // lastDir defaults to EAST; face the piece by first stepping so lastDir=SOUTH.
        in.keyDown(Input.Keys.UP);   // pushes and sets lastDir=SOUTH (also solves)
        in.keyDown(Input.Keys.Z);    // undo back to start, lastDir stays SOUTH
        assertTrue(in.keyDown(Input.Keys.R)); // rotate the piece to the south of the player
        assertEquals(1, s.moves());
    }
}
