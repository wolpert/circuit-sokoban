package com.circuitsokoban.solver;

import com.circuitsokoban.model.Board;
import com.circuitsokoban.model.Circuit;
import com.circuitsokoban.model.Direction;
import com.circuitsokoban.model.Piece;
import com.circuitsokoban.model.PieceType;
import com.circuitsokoban.model.Pos;
import com.circuitsokoban.model.Terminal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Generates guaranteed-solvable puzzles by <b>reverse generation</b>:
 *
 * <ol>
 *   <li>Build a fully solved board (a source&rarr;receiver path of aligned
 *       connectors).</li>
 *   <li>Scramble it by applying random <em>reverse moves</em> &mdash; pulls
 *       (the inverse of a push) and rotates. Every reverse move's forward
 *       inverse is a legal push/rotate, so the scrambled board is solvable by
 *       construction.</li>
 *   <li>Run the {@link Solver} forward to (a) independently confirm solvability
 *       and (b) measure the true optimal move count (par), which may be shorter
 *       than the scramble length.</li>
 *   <li>Reject degenerate results (already solved, too easy, too hard, or over
 *       the solver's state budget) and retry with the next RNG draw.</li>
 * </ol>
 *
 * <p><b>Why the pull is correct.</b> A forward push is: player at {@code A}
 * pushes the piece at {@code A+d} to {@code A+2d}, ending at {@code A+d}. Its
 * inverse (a pull) takes the current piece at {@code Pc} and a direction
 * {@code t} pointing at the player, moving the piece to {@code Pc+t} and the
 * player to {@code Pc+2t}. Reversing that pull is exactly the push above, so
 * the whole scramble sequence reversed is a valid solution. The solver in step
 * 3 is also a safety net: anything it can't solve is rejected, so no unsolvable
 * level can ever escape.
 */
public final class LevelGenerator {

    public Level generate(long seed) {
        return generate(seed, GenParams.medium());
    }

    public Level generate(long seed, GenParams params) {
        Random rng = new Random(seed);
        for (int attempt = 0; attempt < params.maxAttempts(); attempt++) {
            Board solved = buildSolvedBoard(rng, params);
            if (solved == null || !Circuit.isSolved(solved)) {
                continue;
            }
            Board scrambled = solved.copy();
            scramble(scrambled, rng, params);

            // Depth-bound the search at maxPar: anything deeper is a level we'd
            // reject anyway, and bounding keeps rejecting "too hard" scrambles cheap.
            Solver.Result result =
                    new Solver(params.solverMaxStates(), params.maxPar()).solve(scrambled);
            if (!result.solvable()) {
                continue; // unsolvable, over-budget, or optimal > maxPar -> reject
            }
            int par = result.moves();
            if (par < params.minPar()) {
                continue; // too trivial
            }
            return new Level(seed, scrambled, solved, par, Level.difficultyFor(par));
        }
        throw new IllegalStateException(
                "Could not generate a level matching params within "
                        + params.maxAttempts() + " attempts (seed=" + seed + "). "
                        + "Widen the par window or raise maxAttempts.");
    }

    // ---- step 1: build a solved board ----

    private Board buildSolvedBoard(Random rng, GenParams p) {
        int w = p.width();
        int h = p.height();

        int srcRow = rng.nextInt(h);
        int recRow = rng.nextInt(h);
        Terminal source = new Terminal(new Pos(0, srcRow), Direction.EAST);
        Terminal receiver = new Terminal(new Pos(w - 1, recRow), Direction.WEST);

        Pos startCell = new Pos(1, srcRow);       // cell the source opens onto
        Pos endCell = new Pos(w - 2, recRow);     // cell the receiver opens onto
        if (startCell.equals(endCell)) {
            return null; // too narrow for this row pairing; caller retries
        }

        List<Pos> path = randomSimplePath(rng, w, h, startCell, endCell, source, receiver);
        if (path == null) {
            return null;
        }

        Board board = new Board(w, h, source, receiver, startCell /* placeholder */);
        placePathPieces(board, path, source, receiver);
        placeDiodes(board, path, source, receiver, rng, p.diodesOnPath());
        if (p.gateCount() > 0 && !placeGateAndSecondary(board, path, source, receiver, rng)) {
            return null; // couldn't fit a gate + secondary this attempt; caller retries
        }
        addDecoys(board, rng, p.extraPieces());

        Pos player = randomEmptyCell(board, rng);
        if (player == null) {
            return null;
        }
        board.setPlayer(player);
        placeIce(board, rng, p.iceTiles());
        return board;
    }

    /**
     * Sprinkles ice on empty, off-path cells (never a piece cell, terminal, or the
     * player's start). Because the solution path and scramble never rest a piece on
     * ice, every level stays solvable-by-construction; ice only constrains where
     * pieces the player pushes off-path can come to rest.
     */
    private void placeIce(Board board, Random rng, int count) {
        if (count <= 0) {
            return;
        }
        List<Pos> empties = emptyCells(board);
        empties.remove(board.player());
        Collections.shuffle(empties, rng);
        for (int i = 0; i < count && i < empties.size(); i++) {
            board.setIce(empties.get(i), true);
        }
    }

    /**
     * Randomized depth-first search for a simple (non-repeating) path between
     * two interior cells. Neighbor order is shuffled so paths meander rather
     * than run straight, which makes for more interesting puzzles.
     */
    private List<Pos> randomSimplePath(Random rng, int w, int h, Pos from, Pos to,
                                       Terminal source, Terminal receiver) {
        boolean[][] visited = new boolean[w][h];
        List<Pos> path = new ArrayList<>();
        if (dfs(rng, w, h, from, to, source, receiver, visited, path)) {
            return path;
        }
        return null;
    }

    private boolean dfs(Random rng, int w, int h, Pos cur, Pos to,
                        Terminal source, Terminal receiver,
                        boolean[][] visited, List<Pos> path) {
        visited[cur.x()][cur.y()] = true;
        path.add(cur);
        if (cur.equals(to)) {
            return true;
        }
        List<Direction> dirs = new ArrayList<>(List.of(Direction.values()));
        Collections.shuffle(dirs, rng);
        for (Direction d : dirs) {
            Pos next = cur.step(d);
            if (next.x() < 0 || next.x() >= w || next.y() < 0 || next.y() >= h) continue;
            if (visited[next.x()][next.y()]) continue;
            if (next.equals(source.pos()) || next.equals(receiver.pos())) continue; // don't route through terminals
            if (dfs(rng, w, h, next, to, source, receiver, visited, path)) {
                return true;
            }
        }
        path.remove(path.size() - 1); // backtrack
        return false;
    }

    /** Assigns each path cell a piece whose two openings face its path neighbours (and the terminals at the ends). */
    private void placePathPieces(Board board, List<Pos> path, Terminal source, Terminal receiver) {
        for (int i = 0; i < path.size(); i++) {
            Pos cell = path.get(i);
            int mask = 0;
            if (i == 0) {
                mask |= directionBetween(cell, source.pos()).bit();
            } else {
                mask |= directionBetween(cell, path.get(i - 1)).bit();
            }
            if (i == path.size() - 1) {
                mask |= directionBetween(cell, receiver.pos()).bit();
            } else {
                mask |= directionBetween(cell, path.get(i + 1)).bit();
            }
            board.setPiece(cell, pieceMatching(mask));
        }
    }

    /**
     * Turns up to {@code count} collinear solution-path pieces into one-way diodes,
     * each flowing toward the receiver so the solved board stays solved. After
     * scrambling rotates them, the player must re-aim them &mdash; the directional
     * twist. Only straight (collinear) segments qualify, since a diode is straight.
     */
    private void placeDiodes(Board board, List<Pos> path, Terminal source, Terminal receiver,
                             Random rng, int count) {
        if (count <= 0) {
            return;
        }
        List<Integer> collinear = new ArrayList<>();
        for (int i = 0; i < path.size(); i++) {
            Direction toPrev = (i == 0)
                    ? directionBetween(path.get(i), source.pos())
                    : directionBetween(path.get(i), path.get(i - 1));
            Direction toNext = flowAt(path, receiver, i);
            if (toNext == toPrev.opposite()) {
                collinear.add(i);
            }
        }
        Collections.shuffle(collinear, rng);
        int placed = 0;
        for (int idx : collinear) {
            if (placed >= count) {
                break;
            }
            Direction flow = flowAt(path, receiver, idx); // toward the receiver
            board.setPiece(path.get(idx), new Piece(PieceType.DIODE, flow.ordinal()));
            placed++;
        }
    }

    /**
     * Turns one collinear straight path piece into a locked GATE and adds a minimal
     * secondary circuit (source2 &rarr; a key straight &rarr; receiver2) on a free
     * 3-cell line. In the solved board the key is aligned, so the secondary is
     * complete and the gate conducts; scrambling then rotates the key, so the player
     * must complete circuit B to unlock gate A. Returns false if it can't fit.
     */
    private boolean placeGateAndSecondary(Board board, List<Pos> path,
                                          Terminal source, Terminal receiver, Random rng) {
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < path.size(); i++) {
            Direction toPrev = (i == 0)
                    ? directionBetween(path.get(i), source.pos())
                    : directionBetween(path.get(i), path.get(i - 1));
            Direction toNext = flowAt(path, receiver, i);
            if (toNext == toPrev.opposite() && board.pieceAt(path.get(i)).type() == PieceType.STRAIGHT) {
                candidates.add(i);
            }
        }
        if (candidates.isEmpty()) {
            return false;
        }
        Collections.shuffle(candidates, rng);
        int idx = candidates.get(0);
        Direction axis = flowAt(path, receiver, idx);
        board.setPiece(path.get(idx), new Piece(PieceType.GATE, axis.ordinal()));

        int[] line = findFreeLine(board, rng);
        if (line == null) {
            return false;
        }
        Pos a = new Pos(line[0], line[1]);
        Direction d = Direction.values()[line[2]];
        Pos mid = a.step(d);
        Pos c = a.step(d, 2);
        board.setSecondary(new Terminal(a, d), new Terminal(c, d.opposite()));
        board.setPiece(mid, pieceMatching(d.bit() | d.opposite().bit())); // aligned key straight
        return true;
    }

    /** A run of three empty, non-terminal cells for the secondary circuit; {x, y, dirOrdinal} or null. */
    private int[] findFreeLine(Board board, Random rng) {
        List<int[]> lines = new ArrayList<>();
        Direction[] dirs = {Direction.EAST, Direction.NORTH};
        for (int y = 0; y < board.height(); y++) {
            for (int x = 0; x < board.width(); x++) {
                Pos a = new Pos(x, y);
                for (Direction d : dirs) {
                    if (freeForSecondary(board, a) && freeForSecondary(board, a.step(d))
                            && freeForSecondary(board, a.step(d, 2))) {
                        lines.add(new int[]{x, y, d.ordinal()});
                    }
                }
            }
        }
        return lines.isEmpty() ? null : lines.get(rng.nextInt(lines.size()));
    }

    private boolean freeForSecondary(Board b, Pos p) {
        return b.inBounds(p) && b.pieceAt(p) == null && !b.isTerminal(p) && !b.isWall(p);
    }

    /** Direction power flows out of path cell {@code i} (toward the next cell, or the receiver at the end). */
    private static Direction flowAt(List<Pos> path, Terminal receiver, int i) {
        return (i == path.size() - 1)
                ? directionBetween(path.get(i), receiver.pos())
                : directionBetween(path.get(i), path.get(i + 1));
    }

    private void addDecoys(Board board, Random rng, int count) {
        List<Pos> empties = emptyCells(board);
        Collections.shuffle(empties, rng);
        for (int i = 0; i < count && i < empties.size(); i++) {
            // Basic types only: diodes are introduced deliberately on the path, and
            // their extra rotation states would inflate the solver's search here.
            PieceType type = PieceType.BASIC[rng.nextInt(PieceType.BASIC.length)];
            board.setPiece(empties.get(i), new Piece(type, rng.nextInt(4)));
        }
    }

    // ---- step 2: scramble with reverse moves ----

    private void scramble(Board board, Random rng, GenParams p) {
        // Jitter the actual count down to ~half the bound so generated par spans
        // both parities (par parity otherwise tracks a fixed scramble length).
        int max = Math.max(1, p.scrambleSteps());
        int low = Math.max(1, (max + 1) / 2);
        int steps = low + rng.nextInt(max - low + 1);
        for (int step = 0; step < steps; step++) {
            boolean preferRotate = rng.nextDouble() < p.rotateChance();
            boolean done = preferRotate
                    ? tryRotate(board, rng) || tryPull(board, rng)
                    : tryPull(board, rng) || tryRotate(board, rng);
            if (!done) {
                break; // fully stuck (rare); solver still validates whatever we have
            }
        }
    }

    /** A pull: piece at {@code Pc} moves one step toward the player ({@code t}), player retreats one step further. */
    private boolean tryPull(Board board, Random rng) {
        var reachable = board.reachableCells();
        List<Pos[]> candidates = new ArrayList<>(); // {pieceCell, mid, land}
        for (Pos pc : occupiedCells(board)) {
            for (Direction t : Direction.values()) {
                Pos mid = pc.step(t);      // piece's new cell == where the player stands before pulling
                Pos land = pc.step(t, 2);  // player's cell after retreating
                // Never rest a piece on ice: a forward push would slide it further,
                // so the pull would not invert it. Keeping pieces off ice during
                // scramble preserves solvability-by-construction (ice stays off-path).
                if (board.isStandable(mid) && board.isStandable(land)
                        && !board.isIce(mid) && reachable.contains(mid)) {
                    candidates.add(new Pos[]{pc, mid, land});
                }
            }
        }
        if (candidates.isEmpty()) {
            return false;
        }
        Pos[] chosen = candidates.get(rng.nextInt(candidates.size()));
        Piece piece = board.pieceAt(chosen[0]);
        board.setPiece(chosen[0], null);
        board.setPiece(chosen[1], piece);
        board.setPlayer(chosen[2]);
        return true;
    }

    private boolean tryRotate(Board board, Random rng) {
        var reachable = board.reachableCells();
        List<Pos[]> candidates = new ArrayList<>(); // {pieceCell, standCell}
        for (Pos pc : occupiedCells(board)) {
            Piece piece = board.pieceAt(pc);
            if (piece.type().rotationPeriod <= 1) {
                continue; // rotating a CROSS is a no-op
            }
            for (Direction d : Direction.values()) {
                Pos adj = pc.step(d);
                if (reachable.contains(adj)) {
                    candidates.add(new Pos[]{pc, adj});
                    break;
                }
            }
        }
        if (candidates.isEmpty()) {
            return false;
        }
        Pos[] chosen = candidates.get(rng.nextInt(candidates.size()));
        Piece piece = board.pieceAt(chosen[0]);
        board.setPiece(chosen[0], piece.rotatedCW());
        board.setPlayer(chosen[1]);
        return true;
    }

    // ---- helpers ----

    /** The direction to step from {@code a} to its orthogonal neighbour {@code b}. */
    private static Direction directionBetween(Pos a, Pos b) {
        for (Direction d : Direction.values()) {
            if (a.step(d).equals(b)) {
                return d;
            }
        }
        throw new IllegalArgumentException(a + " and " + b + " are not orthogonally adjacent");
    }

    /** The lowest-opening-count piece whose openings exactly equal {@code mask}. */
    static Piece pieceMatching(int mask) {
        for (PieceType type : PieceType.values()) {
            for (int r = 0; r < 4; r++) {
                if (Direction.rotateMask(type.baseMask, r) == mask) {
                    return new Piece(type, r);
                }
            }
        }
        throw new IllegalArgumentException("No piece has openings " + Integer.toBinaryString(mask));
    }

    private static List<Pos> occupiedCells(Board board) {
        List<Pos> out = new ArrayList<>();
        for (int y = 0; y < board.height(); y++) {
            for (int x = 0; x < board.width(); x++) {
                Pos p = new Pos(x, y);
                if (board.pieceAt(p) != null) {
                    out.add(p);
                }
            }
        }
        return out;
    }

    private static List<Pos> emptyCells(Board board) {
        List<Pos> out = new ArrayList<>();
        for (int y = 0; y < board.height(); y++) {
            for (int x = 0; x < board.width(); x++) {
                Pos p = new Pos(x, y);
                if (board.isStandable(p)) {
                    out.add(p);
                }
            }
        }
        return out;
    }

    private static Pos randomEmptyCell(Board board, Random rng) {
        List<Pos> empties = emptyCells(board);
        if (empties.isEmpty()) {
            return null;
        }
        return empties.get(rng.nextInt(empties.size()));
    }
}
