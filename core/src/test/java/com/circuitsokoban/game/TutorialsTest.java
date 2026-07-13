package com.circuitsokoban.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.circuitsokoban.model.Board;
import com.circuitsokoban.model.Direction;
import com.circuitsokoban.model.Piece;
import com.circuitsokoban.model.PieceType;
import com.circuitsokoban.model.Pos;
import com.circuitsokoban.model.Terminal;
import org.junit.jupiter.api.Test;

class TutorialsTest {

    private Board board() {
        Terminal s = new Terminal(new Pos(0, 0), Direction.EAST);
        Terminal r = new Terminal(new Pos(2, 0), Direction.WEST);
        return new Board(3, 2, s, r, new Pos(1, 1));
    }

    @Test
    void basicsComesFirstRegardlessOfPieces() {
        Board b = board();
        b.setPiece(new Pos(1, 0), new Piece(PieceType.DIODE));
        assertEquals(Lesson.BASICS, Tutorials.firstUnseen(b, new Progress(new MemoryStore())));
    }

    @Test
    void diodeTaughtAfterBasics() {
        Board b = board();
        b.setPiece(new Pos(1, 0), new Piece(PieceType.DIODE));
        Progress p = new Progress(new MemoryStore());
        p.markLessonSeen(Lesson.BASICS);
        assertEquals(Lesson.DIODE, Tutorials.firstUnseen(b, p));
    }

    @Test
    void gateDetectedFromSecondaryCircuit() {
        Board b = board();
        b.setSecondary(new Terminal(new Pos(0, 1), Direction.EAST),
                new Terminal(new Pos(2, 1), Direction.WEST));
        Progress p = new Progress(new MemoryStore());
        p.markLessonSeen(Lesson.BASICS);
        assertEquals(Lesson.GATE, Tutorials.firstUnseen(b, p));
    }

    @Test
    void nothingToTeachWhenAllSeen() {
        Board b = board();
        b.setPiece(new Pos(1, 0), new Piece(PieceType.DIODE));
        Progress p = new Progress(new MemoryStore());
        for (Lesson l : Lesson.values()) {
            p.markLessonSeen(l);
        }
        assertNull(Tutorials.firstUnseen(b, p));
    }
}
