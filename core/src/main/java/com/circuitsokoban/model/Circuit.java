package com.circuitsokoban.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Evaluates board connectivity: which pieces are electrically joined to the
 * power source, and whether the source reaches the receiver (a solved level).
 *
 * <p>Rule (directed): power flows from cell A into neighbour B when A has an
 * <em>output</em> facing B and B has an <em>input</em> facing A. For normal
 * connectors input == output == openings, so this is just mutual openings; a
 * diode outputs only along its flow direction and inputs only from the opposite
 * side, making it one-way. Gates (conditional edges) are a later pass.
 */
public final class Circuit {

    private Circuit() {}

    /**
     * @param solved    true if source reaches receiver
     * @param energized every piece cell electrically joined to the source
     * @param layers    energized cells grouped by BFS distance from the source,
     *                  i.e. the order power visually travels for the animation
     */
    public record Result(boolean solved, Set<Pos> energized, List<List<Pos>> layers) {}

    public static Result evaluate(Board board) {
        Terminal source = board.source();
        Terminal receiver = board.receiver();

        Set<Pos> energized = new HashSet<>();
        List<List<Pos>> layers = new ArrayList<>();
        boolean solved = false;

        // Seed: the cell the source opens onto.
        Pos seedCell = source.pos().step(source.opening());
        Direction backToSource = source.opening().opposite();

        // Degenerate case: source directly faces the receiver.
        if (seedCell.equals(receiver.pos()) && receiver.hasOpening(backToSource)) {
            solved = true;
        }

        List<Pos> frontier = new ArrayList<>();
        Piece seedPiece = board.pieceAt(seedCell);
        // The seed piece is energized only if it accepts power arriving from the source side.
        if (seedPiece != null && (seedPiece.inputs() & backToSource.bit()) != 0) {
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
                    if ((piece.outputs() & d.bit()) == 0) {
                        continue; // power can only leave along an output side
                    }
                    Pos q = p.step(d);
                    Direction back = d.opposite();

                    if (q.equals(receiver.pos()) && receiver.hasOpening(back)) {
                        solved = true;
                    }
                    Piece qPiece = board.pieceAt(q);
                    if (qPiece != null && (qPiece.inputs() & back.bit()) != 0 && energized.add(q)) {
                        nextLayer.add(q);
                    }
                }
            }
            if (!nextLayer.isEmpty()) {
                layers.add(nextLayer);
            }
            frontier = nextLayer;
        }

        return new Result(solved, energized, layers);
    }

    public static boolean isSolved(Board board) {
        return evaluate(board).solved();
    }
}
