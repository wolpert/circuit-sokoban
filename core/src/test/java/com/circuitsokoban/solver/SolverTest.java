package com.circuitsokoban.solver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.circuitsokoban.model.Board;
import com.circuitsokoban.model.Circuit;
import com.circuitsokoban.model.Direction;
import com.circuitsokoban.model.Piece;
import com.circuitsokoban.model.PieceType;
import com.circuitsokoban.model.Pos;
import com.circuitsokoban.model.Terminal;
import org.junit.jupiter.api.Test;

class SolverTest {

    /** source at bottom-left opening EAST, receiver at bottom-right opening WEST. */
    private Board board(int w, int h, Pos player) {
        Terminal source = new Terminal(new Pos(0, 0), Direction.EAST);
        Terminal receiver = new Terminal(new Pos(w - 1, 0), Direction.WEST);
        return new Board(w, h, source, receiver, player);
    }

    @Test
    void alreadySolvedIsZeroMoves() {
        Board b = board(3, 2, new Pos(1, 1));
        b.setPiece(new Pos(1, 0), new Piece(PieceType.STRAIGHT, 1)); // E|W bridge in place
        Solver.Result r = new Solver().solve(b);
        assertTrue(r.solvable());
        assertEquals(0, r.moves());
        assertEquals(1, r.path().size());
    }

    @Test
    void singleRotateSolve() {
        Board b = board(3, 2, new Pos(1, 1));
        b.setPiece(new Pos(1, 0), new Piece(PieceType.STRAIGHT, 0)); // N|S, needs one rotate
        Solver.Result r = new Solver().solve(b);
        assertTrue(r.solvable());
        assertEquals(1, r.moves());
    }

    @Test
    void singlePushSolve() {
        Board b = board(3, 3, new Pos(1, 2));
        b.setPiece(new Pos(1, 1), new Piece(PieceType.STRAIGHT, 1)); // E|W, needs pushing down to (1,0)
        Solver.Result r = new Solver().solve(b);
        assertTrue(r.solvable());
        assertEquals(1, r.moves());
    }

    @Test
    void pushThenRotateNeedsTwoMoves() {
        Board b = board(3, 3, new Pos(1, 2));
        b.setPiece(new Pos(1, 1), new Piece(PieceType.STRAIGHT, 0)); // N|S and wrong cell
        Solver.Result r = new Solver().solve(b);
        assertTrue(r.solvable());
        assertEquals(2, r.moves());
    }

    @Test
    void pathIsAValidOptimalSolution() {
        Board b = board(3, 3, new Pos(1, 2));
        b.setPiece(new Pos(1, 1), new Piece(PieceType.STRAIGHT, 0));
        Solver.Result r = new Solver().solve(b);
        assertEquals(r.moves() + 1, r.path().size());
        assertFalse(Circuit.isSolved(r.path().get(0)));
        assertTrue(Circuit.isSolved(r.path().get(r.path().size() - 1)));
    }

    @Test
    void unsolvableWhenNoPieceCanBridge() {
        Board b = board(3, 2, new Pos(1, 1)); // no pieces at all
        Solver.Result r = new Solver().solve(b);
        assertFalse(r.solvable());
        assertEquals(-1, r.moves());
    }
}
