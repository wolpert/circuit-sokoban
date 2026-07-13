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
 *
 * <p>Gates <b>latch</b>: {@link #resolve} sets {@code board.gateLatched} once the
 * secondary completes, so gates stay open afterwards. {@code resolve} also burns
 * out any energized FUSE piece (removes it) &mdash; a one-use fuse that latches a
 * gate open then vanishes. {@link #evaluate} is pure; {@code resolve} mutates.
 */
public final class Circuit {

    private Circuit() {}

    /**
     * @param solved        true if the primary source reaches its receiver
     * @param energized     every piece cell lit by either circuit (for rendering)
     * @param layers        primary energized cells grouped by BFS distance (energize sweep)
     * @param gatesUnlocked  whether gates conduct (secondary complete, or latched)
     * @param secondarySolved whether the secondary circuit is complete right now
     */
    public record Result(boolean solved, Set<Pos> energized, List<List<Pos>> layers,
                         boolean gatesUnlocked, boolean secondarySolved) {}

    private record Flood(boolean solved, Set<Pos> energized, List<List<Pos>> layers) {}

    public static Result evaluate(Board board) {
        boolean secondarySolved = false;
        Set<Pos> secondaryEnergized = Set.of();
        if (board.hasSecondary()) {
            // Gates are locked while judging the secondary, so it can't depend on them.
            Flood secondary = flood(board, board.source2(), board.receiver2(), false);
            secondarySolved = secondary.solved();
            secondaryEnergized = secondary.energized();
        }
        boolean gatesUnlocked = board.isGateLatched() || secondarySolved;
        Flood primary = flood(board, board.source(), board.receiver(), gatesUnlocked);

        Set<Pos> energized = new HashSet<>(primary.energized());
        energized.addAll(secondaryEnergized);
        return new Result(primary.solved(), energized, primary.layers(), gatesUnlocked, secondarySolved);
    }

    public static boolean isSolved(Board board) {
        return evaluate(board).solved();
    }

    /**
     * Applies the state changes a move can trigger and returns the settled result:
     * latch gates open if the secondary is complete, and burn out (remove) any
     * energized FUSE. <b>Mutates {@code board}.</b> Call after loading a board and
     * after every move (game and solver alike) so state stays consistent.
     */
    public static Result resolve(Board board) {
        Result r = evaluate(board);
        boolean changed = false;
        if (board.hasSecondary() && r.secondarySolved() && !board.isGateLatched()) {
            board.setGateLatched(true);
            changed = true;
        }
        for (Pos p : r.energized()) {
            Piece piece = board.pieceAt(p);
            if (piece != null && piece.type() == PieceType.FUSE) {
                board.setPiece(p, null); // one-use: power flowed through, it's spent
                changed = true;
            }
        }
        return changed ? evaluate(board) : r;
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
