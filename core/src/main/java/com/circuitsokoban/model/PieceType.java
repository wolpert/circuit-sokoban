package com.circuitsokoban.model;

/**
 * Kinds of movable connector piece.
 *
 * <p>Each type is defined by its opening mask in orientation 0. Every other
 * orientation is derived by {@link Direction#rotateMask} &mdash; we get variety
 * from rotation, not from new sprites.
 *
 * <p>{@code rotationPeriod} is how many distinct orientations the piece has
 * (before its opening mask repeats). Used by the solver to avoid treating a
 * no-op rotation (e.g. rotating a CROSS) as a real move.
 *
 * <p>v1 ships the four basic connectors; DIODE / GATE / ICE are placeholders
 * for a later pass and are not yet handled by {@code Circuit} or the solver.
 */
public enum PieceType {
    STRAIGHT(Direction.NORTH.bit() | Direction.SOUTH.bit(), 2),
    ELBOW(Direction.NORTH.bit() | Direction.EAST.bit(), 4),
    TEE(Direction.NORTH.bit() | Direction.EAST.bit() | Direction.SOUTH.bit(), 4),
    CROSS(Direction.NORTH.bit() | Direction.EAST.bit()
            | Direction.SOUTH.bit() | Direction.WEST.bit(), 1);

    /** Opening mask when orientation == 0. */
    public final int baseMask;

    /** Number of distinct orientations before the opening mask repeats (1, 2, or 4). */
    public final int rotationPeriod;

    PieceType(int baseMask, int rotationPeriod) {
        this.baseMask = baseMask;
        this.rotationPeriod = rotationPeriod;
    }
}
