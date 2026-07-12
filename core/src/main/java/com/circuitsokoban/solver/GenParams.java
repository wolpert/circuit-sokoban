package com.circuitsokoban.solver;

/**
 * Tunable knobs for {@link LevelGenerator}. Difficulty is shaped almost
 * entirely here: bigger grids + more scramble steps + a higher par window
 * produce harder puzzles.
 *
 * <p>The values in {@link #easy()} / {@link #medium()} / {@link #hard()} are
 * starting points chosen by hand; the generation test sweeps many seeds so we
 * can see the real par distribution and retune. Everything here is deliberately
 * data, not code, so tuning never touches the algorithm.
 *
 * <p><b>Why every preset is 5&times;5 with a modest par ceiling.</b> Par comes
 * from an <em>optimal</em> BFS, whose cost grows sharply with board size and
 * solution depth. Empirically 5&times;5 puzzles up to ~8 optimal moves solve in
 * well under a second, while 6&times;6 or par&gt;9 can take many seconds each and
 * make generation (which solves once per attempt) impractically slow. Bigger,
 * deeper puzzles would need a faster optimal solver (e.g. bidirectional BFS or
 * IDA*) &mdash; a deliberate future improvement, not a v1 requirement.
 *
 * @param width          grid width (terminals sit on the left/right edges)
 * @param height         grid height
 * @param scrambleSteps  <em>upper bound</em> on reverse moves; the actual count is
 *                        jittered down to ~half of this so par spans both parities
 * @param minPar         reject a generated level whose optimal solution is shorter than this
 * @param maxPar         reject a generated level whose optimal solution is longer than this
 * @param rotateChance   probability a scramble step is a rotate rather than a pull (0..1)
 * @param extraPieces    decoy connectors scattered on empty cells before scrambling
 * @param diodesOnPath   collinear solution-path pieces to turn into one-way diodes
 * @param solverMaxStates state budget for the validating solver; exceeding it -> "too hard", reject
 * @param maxAttempts    how many generate-and-check tries before giving up
 */
public record GenParams(
        int width,
        int height,
        int scrambleSteps,
        int minPar,
        int maxPar,
        double rotateChance,
        int extraPieces,
        int diodesOnPath,
        int solverMaxStates,
        int maxAttempts) {

    /** Fast backstop; a 5x5 par&le;8 solve stays well under this, so hitting it means "reject". */
    private static final int GEN_MAX_STATES = 120_000;

    public static GenParams easy() {
        return new GenParams(5, 5, 6, 2, 4, 0.5, 0, 0, GEN_MAX_STATES, 400);
    }

    public static GenParams medium() {
        return new GenParams(5, 5, 9, 4, 6, 0.5, 0, 1, GEN_MAX_STATES, 400);
    }

    public static GenParams hard() {
        return new GenParams(5, 5, 12, 6, 8, 0.45, 1, 1, GEN_MAX_STATES, 400);
    }
}
