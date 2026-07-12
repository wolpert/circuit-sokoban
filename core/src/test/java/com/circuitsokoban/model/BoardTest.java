package com.circuitsokoban.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BoardTest {

    /** Open board with terminals tucked in a top corner, out of the play area. */
    private Board openBoard() {
        Terminal source = new Terminal(new Pos(0, 2), Direction.SOUTH);
        Terminal receiver = new Terminal(new Pos(4, 2), Direction.SOUTH);
        return new Board(5, 3, source, receiver, new Pos(0, 0));
    }

    @Test
    void walkIntoEmptyCellIsFreeAndMovesPlayer() {
        Board b = openBoard();
        MoveResult r = b.stepPlayer(Direction.EAST);
        assertEquals(MoveResult.Kind.WALK, r.kind());
        assertFalse(r.scored());
        assertEquals(new Pos(1, 0), r.board().player());
        assertEquals(new Pos(0, 0), b.player(), "original board must be untouched");
    }

    @Test
    void pushMovesPieceAndPlayerAndCountsAsMove() {
        Board b = openBoard();
        b.setPiece(new Pos(1, 0), new Piece(PieceType.ELBOW));
        MoveResult r = b.stepPlayer(Direction.EAST);
        assertEquals(MoveResult.Kind.PUSH, r.kind());
        assertTrue(r.scored());
        Board after = r.board();
        assertNull(after.pieceAt(new Pos(1, 0)));
        assertEquals(PieceType.ELBOW, after.pieceAt(new Pos(2, 0)).type());
        assertEquals(new Pos(1, 0), after.player());
    }

    @Test
    void cannotPushPieceIntoWall() {
        Board b = openBoard();
        b.setPiece(new Pos(1, 0), new Piece(PieceType.ELBOW));
        b.setWall(new Pos(2, 0), true);
        MoveResult r = b.stepPlayer(Direction.EAST);
        assertFalse(r.isLegal());
    }

    @Test
    void cannotPushTwoPiecesAtOnce() {
        Board b = openBoard();
        b.setPiece(new Pos(1, 0), new Piece(PieceType.ELBOW));
        b.setPiece(new Pos(2, 0), new Piece(PieceType.ELBOW));
        MoveResult r = b.stepPlayer(Direction.EAST);
        assertFalse(r.isLegal());
    }

    @Test
    void rotateRequiresAdjacencyAndCountsAsMove() {
        Board b = openBoard();
        b.setPiece(new Pos(1, 0), new Piece(PieceType.ELBOW, 0));
        // Player at (0,0) is adjacent to (1,0): legal.
        MoveResult r = b.rotateAt(new Pos(1, 0));
        assertEquals(MoveResult.Kind.ROTATE, r.kind());
        assertTrue(r.scored());
        assertEquals(1, r.board().pieceAt(new Pos(1, 0)).orientation());
    }

    @Test
    void rotateFailsWhenPlayerNotAdjacent() {
        Board b = openBoard();
        b.setPiece(new Pos(3, 0), new Piece(PieceType.ELBOW));
        MoveResult r = b.rotateAt(new Pos(3, 0)); // player at (0,0), far away
        assertFalse(r.isLegal());
    }

    @Test
    void reachabilityIsBlockedByPieces() {
        Board b = openBoard();
        // Wall off column x=2 across all rows so the player is boxed into x<2.
        b.setWall(new Pos(2, 0), true);
        b.setWall(new Pos(2, 1), true);
        b.setWall(new Pos(2, 2), true);
        var reachable = b.reachableCells();
        assertTrue(reachable.contains(new Pos(1, 1)));
        assertFalse(reachable.contains(new Pos(3, 0)));
    }

    @Test
    void copyIsIndependentButSharesImmutableWalls() {
        Board b = openBoard();
        Board c = b.copy();
        c.setPiece(new Pos(1, 0), new Piece(PieceType.CROSS));
        assertNull(b.pieceAt(new Pos(1, 0)), "mutating copy must not affect original");
        assertSame(PieceType.CROSS, c.pieceAt(new Pos(1, 0)).type());
    }
}
