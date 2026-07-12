package com.circuitsokoban.model;

/**
 * A movable connector: a {@link PieceType} plus an orientation in 0..3
 * (number of clockwise 90&deg; rotations from the type's base orientation).
 *
 * <p>Immutable value type. Rotating produces a new {@code Piece}; the board
 * stores which cell holds which piece.
 */
public record Piece(PieceType type, int orientation) {

    public Piece {
        orientation = ((orientation % 4) + 4) % 4;
    }

    public Piece(PieceType type) {
        this(type, 0);
    }

    /** Current opening mask (base mask rotated by this piece's orientation). */
    public int openings() {
        return Direction.rotateMask(type.baseMask, orientation);
    }

    public boolean hasOpening(Direction d) {
        return (openings() & d.bit()) != 0;
    }

    /** This piece rotated one step clockwise. */
    public Piece rotatedCW() {
        return new Piece(type, orientation + 1);
    }
}
