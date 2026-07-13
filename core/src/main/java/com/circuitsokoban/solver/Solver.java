package com.circuitsokoban.solver;

import com.circuitsokoban.model.Board;
import com.circuitsokoban.model.Circuit;
import com.circuitsokoban.model.Direction;
import com.circuitsokoban.model.Piece;
import com.circuitsokoban.model.Pos;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Breadth-first solver over "meaningful" moves &mdash; pushes and rotates.
 *
 * <p>Because walking is free (scoring counts only pushes/rotates), the search
 * doesn't model individual walk steps. Instead each expansion computes the
 * player's walk-reachable region and enumerates every push/rotate reachable
 * from it. BFS then finds the <em>minimum number of scored moves</em> to solve,
 * which is exactly the par value we want &mdash; and unlike an A* heuristic
 * (hard to make admissible for mixed push+rotate moves), BFS can't return a
 * wrong par.
 *
 * <p>{@link StateKey} dedupes visited states (normalizing player position and
 * collapsing symmetric rotations), which keeps the search tractable on the
 * small boards this game generates.
 */
public final class Solver {

    /** Default ceiling on explored states; a generated level exceeding it is treated as "too hard". */
    public static final int DEFAULT_MAX_STATES = 300_000;

    /**
     * @param solvable whether the receiver can be powered within the state budget
     * @param moves    minimum scored moves to solve (0 if already solved), or -1 if unsolvable
     * @param path     boards from start to solved inclusive (empty if unsolvable);
     *                 length == moves + 1
     */
    public record Result(boolean solvable, int moves, List<Board> path) {}

    private final int maxStates;
    private final int maxDepth;

    public Solver() {
        this(DEFAULT_MAX_STATES, Integer.MAX_VALUE);
    }

    public Solver(int maxStates) {
        this(maxStates, Integer.MAX_VALUE);
    }

    /**
     * @param maxStates state-count safety cap (over budget -&gt; treated as unsolvable)
     * @param maxDepth  never search deeper than this many scored moves; a level whose
     *                  optimal solution is deeper is reported unsolvable. During
     *                  generation this is set to the accept window's {@code maxPar}
     *                  so rejecting "too hard" scrambles is cheap.
     */
    public Solver(int maxStates, int maxDepth) {
        this.maxStates = maxStates;
        this.maxDepth = maxDepth;
    }

    public Result solve(Board start) {
        if (Circuit.isSolved(start)) {
            return new Result(true, 0, List.of(start));
        }

        Set<StateKey> visited = new HashSet<>();
        ArrayDeque<Node> queue = new ArrayDeque<>();
        visited.add(StateKey.of(start));
        queue.add(new Node(start, 0, null));

        int expanded = 0;
        while (!queue.isEmpty() && expanded < maxStates) {
            Node node = queue.poll();
            expanded++;
            int childDist = node.dist + 1;
            if (childDist > maxDepth) {
                continue; // node is at the depth limit; its children are out of range
            }
            for (Board succ : successors(node.board)) {
                if (!visited.add(StateKey.of(succ))) {
                    continue;
                }
                Node child = new Node(succ, childDist, node);
                if (Circuit.isSolved(succ)) {
                    return new Result(true, child.dist, reconstruct(child));
                }
                if (childDist < maxDepth) {
                    queue.add(child); // states at exactly maxDepth are checked but never expanded
                }
            }
        }
        return new Result(false, -1, List.of());
    }

    /** Every board reachable by one push or one rotate from {@code board}. */
    List<Board> successors(Board board) {
        List<Board> out = new ArrayList<>();
        Set<Pos> reachable = board.reachableCells();

        for (int y = 0; y < board.height(); y++) {
            for (int x = 0; x < board.width(); x++) {
                Pos p = new Pos(x, y);
                Piece piece = board.pieceAt(p);
                if (piece == null) {
                    continue;
                }
                addPushes(board, reachable, p, piece, out);
                addRotate(board, reachable, p, piece, out);
            }
        }
        return out;
    }

    private void addPushes(Board board, Set<Pos> reachable, Pos p, Piece piece, List<Board> out) {
        for (Direction d : Direction.values()) {
            Pos stand = p.step(d.opposite()); // player must stand behind the piece
            Pos target = p.step(d);            // piece moves forward one cell...
            if (reachable.contains(stand) && board.isStandable(target)) {
                Pos dest = board.slideDestination(target, d); // ...and slides if it lands on ice
                Board nb = board.copy();
                nb.setPiece(p, null);
                nb.setPiece(dest, piece);
                nb.setPlayer(p); // after pushing, player ends on the piece's old cell
                out.add(nb);
            }
        }
    }

    private void addRotate(Board board, Set<Pos> reachable, Pos p, Piece piece, List<Board> out) {
        if (piece.type().rotationPeriod <= 1) {
            return; // rotating a CROSS never changes anything
        }
        for (Direction d : Direction.values()) {
            Pos adj = p.step(d);
            if (reachable.contains(adj)) {
                Board nb = board.copy();
                nb.setPiece(p, piece.rotatedCW());
                nb.setPlayer(adj);
                out.add(nb);
                return; // any adjacent standing cell yields the same normalized state
            }
        }
    }

    private static List<Board> reconstruct(Node end) {
        List<Board> path = new ArrayList<>();
        for (Node n = end; n != null; n = n.parent) {
            path.add(n.board);
        }
        Collections.reverse(path);
        return path;
    }

    private static final class Node {
        final Board board;
        final int dist;
        final Node parent;

        Node(Board board, int dist, Node parent) {
            this.board = board;
            this.dist = dist;
            this.parent = parent;
        }
    }
}
