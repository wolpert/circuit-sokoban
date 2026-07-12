package com.circuitsokoban.game;

import com.circuitsokoban.solver.Level;

/**
 * Persistent player progress across the endless tiers: best move-count and best
 * medal per level, the current level index per tier (for "continue"), and
 * per-tier tallies (levels solved, golds earned) for the level-select summary.
 *
 * <p>Pure logic over a {@link Store}, so it's unit-tested with {@link MemoryStore}.
 * Seeds are small level indices, stored as ints.
 */
public final class Progress {

    private final Store store;

    public Progress(Store store) {
        this.store = store;
    }

    // ---- per-level records ----

    /** Best move-count for this level, or -1 if never solved. */
    public int bestMoves(Tier tier, long seed) {
        return store.getInt(bestKey(tier, seed), -1);
    }

    public Level.Rank bestRank(Tier tier, long seed) {
        return Level.Rank.values()[store.getInt(rankKey(tier, seed), Level.Rank.NONE.ordinal())];
    }

    public boolean isSolved(Tier tier, long seed) {
        return bestMoves(tier, seed) >= 0;
    }

    /**
     * Records a solve. Keeps the best move-count and best medal, and maintains
     * the per-tier "solved" and "gold" tallies without double-counting replays.
     */
    public void record(Tier tier, Level level, int moves) {
        long seed = level.seed();
        Level.Rank newRank = level.rankFor(moves);
        int prevBest = bestMoves(tier, seed);
        int prevRankOrd = store.getInt(rankKey(tier, seed), Level.Rank.NONE.ordinal());

        if (prevBest < 0) {
            store.putInt(solvedKey(tier), solvedCount(tier) + 1); // first solve of this level
        }
        if (prevBest < 0 || moves < prevBest) {
            store.putInt(bestKey(tier, seed), moves);
        }
        if (newRank.ordinal() < prevRankOrd) { // a better (lower-ordinal) medal
            store.putInt(rankKey(tier, seed), newRank.ordinal());
            if (newRank == Level.Rank.GOLD && prevRankOrd != Level.Rank.GOLD.ordinal()) {
                store.putInt(goldKey(tier), goldCount(tier) + 1);
            }
        }
        store.flush();
    }

    // ---- per-tier summary + current position ----

    public int solvedCount(Tier tier) {
        return store.getInt(solvedKey(tier), 0);
    }

    public int goldCount(Tier tier) {
        return store.getInt(goldKey(tier), 0);
    }

    public long currentSeed(Tier tier) {
        return store.getInt(curKey(tier), 0);
    }

    public void setCurrentSeed(Tier tier, long seed) {
        store.putInt(curKey(tier), (int) Math.max(0, seed));
        store.flush();
    }

    // ---- keys ----

    private static String bestKey(Tier t, long seed) { return "best." + t.name() + "." + seed; }
    private static String rankKey(Tier t, long seed) { return "rank." + t.name() + "." + seed; }
    private static String curKey(Tier t) { return "cur." + t.name(); }
    private static String solvedKey(Tier t) { return "solved." + t.name(); }
    private static String goldKey(Tier t) { return "gold." + t.name(); }
}
