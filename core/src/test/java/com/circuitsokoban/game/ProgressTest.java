package com.circuitsokoban.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.circuitsokoban.model.Board;
import com.circuitsokoban.model.Direction;
import com.circuitsokoban.model.Pos;
import com.circuitsokoban.model.Terminal;
import com.circuitsokoban.solver.Level;
import org.junit.jupiter.api.Test;

class ProgressTest {

    /** A Level with the given seed/par; boards are placeholders (record only uses seed + par). */
    private Level level(long seed, int par) {
        Terminal src = new Terminal(new Pos(0, 0), Direction.EAST);
        Terminal rec = new Terminal(new Pos(1, 0), Direction.WEST);
        Board b = new Board(2, 1, src, rec, new Pos(0, 0));
        return new Level(seed, b, b, par, 3);
    }

    @Test
    void firstSolveRecordsBestAndCountsSolved() {
        Progress p = new Progress(new MemoryStore());
        Level lvl = level(5, 4); // par 4: gold<=4, silver<=5, bronze<=7
        assertFalse(p.isSolved(Tier.EASY, 5));
        p.record(Tier.EASY, lvl, 5); // silver
        assertTrue(p.isSolved(Tier.EASY, 5));
        assertEquals(5, p.bestMoves(Tier.EASY, 5));
        assertEquals(Level.Rank.SILVER, p.bestRank(Tier.EASY, 5));
        assertEquals(1, p.solvedCount(Tier.EASY));
        assertEquals(0, p.goldCount(Tier.EASY));
    }

    @Test
    void improvingToGoldUpdatesBestAndGoldTallyOnce() {
        Progress p = new Progress(new MemoryStore());
        Level lvl = level(5, 4);
        p.record(Tier.EASY, lvl, 5); // silver
        p.record(Tier.EASY, lvl, 4); // gold, better
        assertEquals(4, p.bestMoves(Tier.EASY, 5));
        assertEquals(Level.Rank.GOLD, p.bestRank(Tier.EASY, 5));
        assertEquals(1, p.solvedCount(Tier.EASY), "replays must not re-count solved");
        assertEquals(1, p.goldCount(Tier.EASY));
        p.record(Tier.EASY, lvl, 4); // gold again, no change
        assertEquals(1, p.goldCount(Tier.EASY), "gold counted once");
    }

    @Test
    void worseReplayDoesNotRegressBestOrRank() {
        Progress p = new Progress(new MemoryStore());
        Level lvl = level(5, 4);
        p.record(Tier.EASY, lvl, 4); // gold
        p.record(Tier.EASY, lvl, 9); // worse
        assertEquals(4, p.bestMoves(Tier.EASY, 5));
        assertEquals(Level.Rank.GOLD, p.bestRank(Tier.EASY, 5));
    }

    @Test
    void lessonsStartUnseenAndPersistIndependently() {
        Progress p = new Progress(new MemoryStore());
        assertFalse(p.hasSeenLesson(Lesson.DIODE));
        p.markLessonSeen(Lesson.DIODE);
        assertTrue(p.hasSeenLesson(Lesson.DIODE));
        assertFalse(p.hasSeenLesson(Lesson.GATE), "marking one lesson doesn't mark others");
    }

    @Test
    void tiersAndCurrentSeedAreIndependent() {
        Progress p = new Progress(new MemoryStore());
        p.record(Tier.EASY, level(0, 3), 3);
        assertEquals(0, p.solvedCount(Tier.HARD));
        assertEquals(0, p.currentSeed(Tier.MEDIUM));
        p.setCurrentSeed(Tier.MEDIUM, 7);
        assertEquals(7, p.currentSeed(Tier.MEDIUM));
        assertEquals(0, p.currentSeed(Tier.EASY));
    }
}
