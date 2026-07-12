package com.circuitsokoban.solver;

import com.circuitsokoban.model.Board;

/**
 * A generated, ready-to-play puzzle.
 *
 * <p>Solvable by construction (built by scrambling a solved board with legal
 * reverse moves) and independently validated by the {@link Solver}, which also
 * supplies {@link #par()} &mdash; the true minimum number of scored moves.
 *
 * <p>{@link #startBoard} and {@link #solvedBoard} are templates; call
 * {@link #freshBoard()} to get a mutable copy to actually play, so the template
 * is never disturbed.
 *
 * @param seed        the seed this level was generated from (reproducible/shareable)
 * @param startBoard  the scrambled starting position
 * @param solvedBoard the solved board it was scrambled from (handy for hints/debug)
 * @param par         optimal scored-move count (gold threshold)
 * @param difficulty  1 (easiest) .. 5 (hardest), bucketed from par
 */
public record Level(long seed, Board startBoard, Board solvedBoard, int par, int difficulty) {

    public enum Rank { GOLD, SILVER, BRONZE, NONE }

    /** A fresh mutable copy of the starting board to play on. */
    public Board freshBoard() {
        return startBoard.copy();
    }

    public int goldThreshold() {
        return par;
    }

    public int silverThreshold() {
        return par + Math.max(1, Math.round(par * 0.30f));
    }

    public int bronzeThreshold() {
        return par + Math.max(2, Math.round(par * 0.75f));
    }

    /** The medal a player earns for solving in {@code moves} scored moves. */
    public Rank rankFor(int moves) {
        if (moves <= goldThreshold()) return Rank.GOLD;
        if (moves <= silverThreshold()) return Rank.SILVER;
        if (moves <= bronzeThreshold()) return Rank.BRONZE;
        return Rank.NONE;
    }

    static int difficultyFor(int par) {
        if (par <= 3) return 1;
        if (par <= 4) return 2;
        if (par <= 6) return 3;
        if (par <= 7) return 4;
        return 5;
    }
}
