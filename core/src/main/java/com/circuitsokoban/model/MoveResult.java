package com.circuitsokoban.model;

/**
 * The outcome of attempting a game-layer move, carrying both the resulting
 * {@link Board} and enough detail for the animation layer to tween it.
 *
 * <p>Scoring rule (locked design decision): a PUSH or ROTATE counts as one move;
 * a WALK is free. See {@link #scored()}.
 */
public final class MoveResult {

    public enum Kind { WALK, PUSH, ROTATE, ILLEGAL }

    private final Kind kind;
    private final Board board;   // resulting state, or null when illegal
    private final Pos from;      // mover's origin (player for walk, piece for push)
    private final Pos to;        // mover's destination
    private final Direction dir; // direction of a walk/push
    private final Pos rotated;   // rotated cell (ROTATE only)

    private MoveResult(Kind kind, Board board, Pos from, Pos to, Direction dir, Pos rotated) {
        this.kind = kind;
        this.board = board;
        this.from = from;
        this.to = to;
        this.dir = dir;
        this.rotated = rotated;
    }

    public static MoveResult walk(Board board, Pos from, Pos to) {
        return new MoveResult(Kind.WALK, board, from, to, null, null);
    }

    public static MoveResult push(Board board, Pos pieceFrom, Pos pieceTo, Direction dir) {
        return new MoveResult(Kind.PUSH, board, pieceFrom, pieceTo, dir, null);
    }

    public static MoveResult rotate(Board board, Pos target) {
        return new MoveResult(Kind.ROTATE, board, null, null, null, target);
    }

    public static MoveResult illegal() {
        return new MoveResult(Kind.ILLEGAL, null, null, null, null, null);
    }

    public Kind kind() { return kind; }
    public boolean isLegal() { return kind != Kind.ILLEGAL; }
    public Board board() { return board; }
    public Pos from() { return from; }
    public Pos to() { return to; }
    public Direction dir() { return dir; }
    public Pos rotated() { return rotated; }

    /** Whether this move counts toward the move counter / par (push or rotate). */
    public boolean scored() {
        return kind == Kind.PUSH || kind == Kind.ROTATE;
    }
}
