package com.circuitsokoban.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CircuitTest {

    /** source(0,0)->E ... row of cells ... W<-receiver, on a 1-row-tall strip. */
    private Board strip(int width) {
        Terminal source = new Terminal(new Pos(0, 0), Direction.EAST);
        Terminal receiver = new Terminal(new Pos(width - 1, 0), Direction.WEST);
        // Player parked out of the way is irrelevant to connectivity; keep in bounds.
        return new Board(width, 1, source, receiver, new Pos(1, 0));
    }

    @Test
    void solvedWhenAlignedStraightBridgesSourceAndReceiver() {
        Board b = strip(3);
        // Straight rotated to E|W bridges source(0,0) and receiver(2,0).
        b.setPiece(new Pos(1, 0), new Piece(PieceType.STRAIGHT, 1));
        Circuit.Result r = Circuit.evaluate(b);
        assertTrue(r.solved());
        assertTrue(r.energized().contains(new Pos(1, 0)));
    }

    @Test
    void unsolvedWhenPieceMisaligned() {
        Board b = strip(3);
        b.setPiece(new Pos(1, 0), new Piece(PieceType.STRAIGHT, 0)); // N|S, doesn't bridge
        Circuit.Result r = Circuit.evaluate(b);
        assertFalse(r.solved());
        assertFalse(r.energized().contains(new Pos(1, 0)));
    }

    @Test
    void energizesConnectedRunButNotSolvedIfLastPieceMisaligned() {
        Board b = strip(4); // source(0,0), cells 1,2, receiver(3,0)
        b.setPiece(new Pos(1, 0), new Piece(PieceType.STRAIGHT, 1)); // E|W, joined to source
        b.setPiece(new Pos(2, 0), new Piece(PieceType.STRAIGHT, 0)); // N|S, dead end before receiver
        Circuit.Result r = Circuit.evaluate(b);
        assertFalse(r.solved());
        assertTrue(r.energized().contains(new Pos(1, 0)));
        assertFalse(r.energized().contains(new Pos(2, 0)));
    }

    @Test
    void layersOrderPowerFromSourceOutward() {
        Board b = strip(4);
        b.setPiece(new Pos(1, 0), new Piece(PieceType.STRAIGHT, 1));
        b.setPiece(new Pos(2, 0), new Piece(PieceType.STRAIGHT, 1));
        Circuit.Result r = Circuit.evaluate(b);
        assertTrue(r.solved());
        assertEquals(new Pos(1, 0), r.layers().get(0).get(0));
        assertEquals(new Pos(2, 0), r.layers().get(1).get(0));
    }

    @Test
    void sourceFacingReceiverDirectlyIsSolved() {
        Terminal source = new Terminal(new Pos(0, 0), Direction.EAST);
        Terminal receiver = new Terminal(new Pos(1, 0), Direction.WEST);
        Board b = new Board(2, 1, source, receiver, new Pos(0, 0));
        assertTrue(Circuit.isSolved(b));
    }
}
