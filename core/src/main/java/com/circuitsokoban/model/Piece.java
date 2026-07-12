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

    /**
     * For a DIODE, the direction current flows out of; undefined for other types.
     * Equal to {@code Direction.values()[orientation]} so it rotates with the piece.
     */
    public Direction flowDirection() {
        return Direction.values()[orientation];
    }

    /**
     * Sides power may flow <em>out</em> of. Every opening for normal pieces; only
     * the flow side for a diode.
     */
    public int outputs() {
        return type == PieceType.DIODE ? flowDirection().bit() : openings();
    }

    /**
     * Sides power may flow <em>in</em> through. Every opening for normal pieces;
     * only the side opposite the flow for a diode.
     */
    public int inputs() {
        return type == PieceType.DIODE ? flowDirection().opposite().bit() : openings();
    }

    /** This piece rotated one step clockwise. */
    public Piece rotatedCW() {
        return new Piece(type, orientation + 1);
    }
}
