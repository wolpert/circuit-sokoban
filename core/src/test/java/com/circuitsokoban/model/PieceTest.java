package com.circuitsokoban.model;

import static com.circuitsokoban.model.Direction.EAST;
import static com.circuitsokoban.model.Direction.NORTH;
import static com.circuitsokoban.model.Direction.SOUTH;
import static com.circuitsokoban.model.Direction.WEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PieceTest {

    @Test
    void straightConnectsNorthSouthAtOrientationZero() {
        Piece p = new Piece(PieceType.STRAIGHT);
        assertTrue(p.hasOpening(NORTH));
        assertTrue(p.hasOpening(SOUTH));
        assertFalse(p.hasOpening(EAST));
        assertFalse(p.hasOpening(WEST));
    }

    @Test
    void rotatingStraightGivesEastWest() {
        Piece p = new Piece(PieceType.STRAIGHT).rotatedCW();
        assertTrue(p.hasOpening(EAST));
        assertTrue(p.hasOpening(WEST));
        assertFalse(p.hasOpening(NORTH));
    }

    @Test
    void straightHasPeriodTwo() {
        Piece p = new Piece(PieceType.STRAIGHT);
        assertEquals(p.openings(), p.rotatedCW().rotatedCW().openings());
    }

    @Test
    void crossIsRotationInvariant() {
        Piece p = new Piece(PieceType.CROSS);
        assertEquals(p.openings(), p.rotatedCW().openings());
        assertEquals(0b1111, p.openings());
    }

    @Test
    void elbowRotatesThroughAllFourCorners() {
        Piece e = new Piece(PieceType.ELBOW); // N|E
        assertEquals(NORTH.bit() | EAST.bit(), e.openings());
        assertEquals(EAST.bit() | SOUTH.bit(), e.rotatedCW().openings());
        assertEquals(SOUTH.bit() | WEST.bit(), e.rotatedCW().rotatedCW().openings());
        assertEquals(WEST.bit() | NORTH.bit(), e.rotatedCW().rotatedCW().rotatedCW().openings());
    }

    @Test
    void diodeInputsAndOutputsFollowFlowDirection() {
        Piece d = new Piece(PieceType.DIODE, EAST.ordinal()); // flow EAST
        assertEquals(EAST, d.flowDirection());
        assertEquals(EAST.bit(), d.outputs());
        assertEquals(WEST.bit(), d.inputs());
        assertEquals(EAST.bit() | WEST.bit(), d.openings(), "diode is a straight along its flow axis");
    }

    @Test
    void diodeHasFourDistinctOrientations() {
        assertEquals(4, PieceType.DIODE.rotationPeriod);
        // Flow N and flow S share openings but differ in/out.
        Piece north = new Piece(PieceType.DIODE, NORTH.ordinal());
        Piece south = new Piece(PieceType.DIODE, SOUTH.ordinal());
        assertEquals(north.openings(), south.openings());
        assertEquals(NORTH.bit(), north.outputs());
        assertEquals(SOUTH.bit(), south.outputs());
    }

    @Test
    void orientationWrapsModuloFour() {
        assertEquals(new Piece(PieceType.ELBOW, 0), new Piece(PieceType.ELBOW, 4));
        assertEquals(new Piece(PieceType.ELBOW, 1), new Piece(PieceType.ELBOW, -3));
    }
}
