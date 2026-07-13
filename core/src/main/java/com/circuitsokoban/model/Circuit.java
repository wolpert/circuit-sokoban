package com.circuitsokoban.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Evaluates board connectivity: which pieces are joined to a power source, and
 * whether the source reaches the receiver (a solved level).
 *
 * <p>Rule (directed): power flows from cell A into neighbour B when A has an
 * <em>output</em> facing B and B has an <em>input</em> facing A. Normal
 * connectors have input == output == openings (mutual openings); a diode is
 * one-way.
 *
 * <p>Gates and the secondary circuit make evaluation two-pass: first flood the
 * secondary source&rarr;receiver with GATE pieces treated as non-conductive; if
 * that circuit completes, gates are unlocked and conduct for the primary flood.
 * The level is solved when the <em>primary</em> source reaches its receiver.
 */
public final class Circuit {

    private Circuit() {}

    /**
     * @param solved        true if the primary source reaches its receiver
     * @param energized     every piece cell lit by either circuit (for rendering)
     * @param layers        primary energized cells grouped by BFS distance (energize sweep)
     * @param gatesUnlocked whether the secondary circuit is complete (gates conduct)
     */
    public record Result(boolean solved, Set<Pos> energized, List<List<Pos>> layers,
                         boolean gatesUnlocked) {}

    private record Flood(boolean solved, Set<Pos> energized, List<List<Pos>> layers) {}

    public static Result evaluate(Board board) {
        boolean gatesUnlocked = true;
        Set<Pos> secondaryEnergized = Set.of();
        if (board.hasSecondary()) {
            // Gates are locked while judging the secondary, so it can't depend on them.
            Flood secondary = flood(board, board.source2(), board.receiver2(), false);
            gatesUnlocked = secondary.solved();
            secondaryEnergized = secondary.energized();
        }
        Flood primary = flood(board, board.source(), board.receiver(), gatesUnlocked);

        Set<Pos> energized = new HashSet<>(primary.energized());
        energized.addAll(secondaryEnergized);
        return new Result(primary.solved(), energized, primary.layers(), gatesUnlocked);
    }

    public static boolean isSolved(Board board) {
        return evaluate(board).solved();
    }

    /** Directed flood from {@code src} toward {@code rcv}; a locked gate conducts nothing. */
    private static Flood flood(Board board, Terminal src, Terminal rcv, boolean gatesConductive) {
        Set<Pos> energized = new HashSet<>();
        List<List<Pos>> layers = new ArrayList<>();
        boolean solved = false;

        Pos seedCell = src.pos().step(src.opening());
        Direction backToSource = src.opening().opposite();
        if (seedCell.equals(rcv.pos()) && rcv.hasOpening(backToSource)) {
            solved = true;
        }

        List<Pos> frontier = new ArrayList<>();
        Piece seedPiece = board.pieceAt(seedCell);
        if (seedPiece != null && (inputs(seedPiece, gatesConductive) & backToSource.bit()) != 0) {
            energized.add(seedCell);
            frontier.add(seedCell);
        }
        if (!frontier.isEmpty()) {
            layers.add(new ArrayList<>(frontier));
        }

        while (!frontier.isEmpty()) {
            List<Pos> nextLayer = new ArrayList<>();
            for (Pos p : frontier) {
                Piece piece = board.pieceAt(p);
                for (Direction d : Direction.values()) {
                    if ((outputs(piece, gatesConductive) & d.bit()) == 0) {
                        continue;
                    }
                    Pos q = p.step(d);
                    Direction back = d.opposite();
                    if (q.equals(rcv.pos()) && rcv.hasOpening(back)) {
                        solved = true;
                    }
                    Piece qPiece = board.pieceAt(q);
                    if (qPiece != null && (inputs(qPiece, gatesConductive) & back.bit()) != 0
                            && energized.add(q)) {
                        nextLayer.add(q);
                    }
                }
            }
            if (!nextLayer.isEmpty()) {
                layers.add(nextLayer);
            }
            frontier = nextLayer;
        }
        return new Flood(solved, energized, layers);
    }

    private static int outputs(Piece p, boolean gatesConductive) {
        return (p.type() == PieceType.GATE && !gatesConductive) ? 0 : p.outputs();
    }

    private static int inputs(Piece p, boolean gatesConductive) {
        return (p.type() == PieceType.GATE && !gatesConductive) ? 0 : p.inputs();
    }
}
