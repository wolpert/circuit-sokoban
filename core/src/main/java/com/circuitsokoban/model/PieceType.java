package com.circuitsokoban.model;

/**
 * Kinds of movable connector piece.
 *
 * <p>Each type is defined by its opening mask in orientation 0. Every other
 * orientation is derived by {@link Direction#rotateMask} &mdash; we get variety
 * from rotation, not from new sprites.
 *
 * <p>{@code rotationPeriod} is how many distinct orientations the piece has
 * (before it repeats, considering flow as well as openings). Used by the solver
 * to avoid treating a no-op rotation (e.g. rotating a CROSS) as a real move.
 *
 * <p>DIODE is a straight connector that conducts one way only: its
 * <em>flow direction</em> is {@code Direction.values()[orientation]}, so all four
 * orientations are distinct even though the opening axis repeats every two.
 *
 * <p>GATE is a straight connector that only conducts while a separate secondary
 * circuit on the board is complete (see {@code Circuit}); otherwise it blocks the
 * path. FUSE is a straight connector that burns out (is removed) the instant it's
 * energized &mdash; a one-use fuse for latching a gate open. ICE is not a piece
 * &mdash; it's a board tile (see {@code Board.isIce}).
 */
public enum PieceType {
    STRAIGHT(Direction.NORTH.bit() | Direction.SOUTH.bit(), 2),
    ELBOW(Direction.NORTH.bit() | Direction.EAST.bit(), 4),
    TEE(Direction.NORTH.bit() | Direction.EAST.bit() | Direction.SOUTH.bit(), 4),
    CROSS(Direction.NORTH.bit() | Direction.EAST.bit()
            | Direction.SOUTH.bit() | Direction.WEST.bit(), 1),
    DIODE(Direction.NORTH.bit() | Direction.SOUTH.bit(), 4),
    GATE(Direction.NORTH.bit() | Direction.SOUTH.bit(), 2),
    FUSE(Direction.NORTH.bit() | Direction.SOUTH.bit(), 2);

    /** The four basic (non-directional, always-present) connectors, for generation. */
    public static final PieceType[] BASIC = {STRAIGHT, ELBOW, TEE, CROSS};

    /** Opening mask when orientation == 0. */
    public final int baseMask;

    /** Number of distinct orientations before the opening mask repeats (1, 2, or 4). */
    public final int rotationPeriod;

    PieceType(int baseMask, int rotationPeriod) {
        this.baseMask = baseMask;
        this.rotationPeriod = rotationPeriod;
    }
}
