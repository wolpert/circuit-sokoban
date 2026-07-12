package com.circuitsokoban.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.circuitsokoban.model.Board;
import com.circuitsokoban.model.Direction;
import com.circuitsokoban.model.Piece;
import com.circuitsokoban.model.PieceType;
import com.circuitsokoban.model.Pos;
import com.circuitsokoban.model.Terminal;
import com.circuitsokoban.solver.Level;
import org.junit.jupiter.api.Test;

class PlaySessionTest {

    /** A level solvable in one push: STRAIGHT(E|W) at (1,1) must be pushed south to (1,0). */
    private Level onePushLevel() {
        Terminal source = new Terminal(new Pos(0, 0), Direction.EAST);
        Terminal receiver = new Terminal(new Pos(2, 0), Direction.WEST);
        Board start = new Board(3, 3, source, receiver, new Pos(1, 2));
        start.setPiece(new Pos(1, 1), new Piece(PieceType.STRAIGHT, 1));
        return new Level(0L, start, start.copy(), 1, 1);
    }

    @Test
    void pushCountsAsMoveAndSolves() {
        PlaySession s = new PlaySession(onePushLevel());
        assertFalse(s.isSolved());
        assertTrue(s.step(Direction.SOUTH).isLegal());
        assertTrue(s.isSolved());
        assertEquals(1, s.moves());
    }

    @Test
    void walkingIsFree() {
        PlaySession s = new PlaySession(onePushLevel());
        assertTrue(s.step(Direction.EAST).isLegal()); // (1,2) -> (2,2), empty walk
        assertEquals(0, s.moves());
        assertFalse(s.isSolved());
    }

    @Test
    void illegalMoveIsRejectedAndChangesNothing() {
        PlaySession s = new PlaySession(onePushLevel());
        assertFalse(s.step(Direction.NORTH).isLegal()); // walking off the top edge from (1,2)
        assertEquals(0, s.moves());
        assertFalse(s.canUndo());
    }

    @Test
    void undoRestoresBoardAndMoveCount() {
        PlaySession s = new PlaySession(onePushLevel());
        s.step(Direction.SOUTH);
        assertTrue(s.isSolved());
        assertTrue(s.undo());
        assertFalse(s.isSolved());
        assertEquals(0, s.moves());
        assertFalse(s.canUndo());
    }

    @Test
    void redoReappliesUndoneMove() {
        PlaySession s = new PlaySession(onePushLevel());
        s.step(Direction.SOUTH);
        s.undo();
        assertTrue(s.redo());
        assertTrue(s.isSolved());
        assertEquals(1, s.moves());
    }

    @Test
    void newMoveClearsRedoHistory() {
        PlaySession s = new PlaySession(onePushLevel());
        s.step(Direction.SOUTH);
        s.undo();
        s.step(Direction.EAST); // a different move
        assertFalse(s.canRedo());
    }

    @Test
    void rotateCountsAsMove() {
        PlaySession s = new PlaySession(onePushLevel());
        // Player at (1,2) is adjacent to the piece at (1,1): rotate is legal.
        assertTrue(s.rotate(new Pos(1, 1)).isLegal());
        assertEquals(1, s.moves());
    }
}
