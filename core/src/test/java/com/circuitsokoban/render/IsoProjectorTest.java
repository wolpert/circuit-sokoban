package com.circuitsokoban.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.circuitsokoban.model.Direction;
import com.circuitsokoban.model.Pos;
import org.junit.jupiter.api.Test;

class IsoProjectorTest {

    @Test
    void projectThenUnprojectRecoversTheCell() {
        IsoProjector iso = new IsoProjector(96f);
        iso.centerBoard(5, 5, 540f, 960f);
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                Pos recovered = iso.unproject(iso.worldX(x, y), iso.worldY(x, y));
                assertEquals(new Pos(x, y), recovered);
            }
        }
    }

    @Test
    void swipeDirectionsMapToIsoDiagonals() {
        IsoProjector iso = new IsoProjector(96f);
        iso.centerBoard(5, 5, 540f, 960f);
        // Screen y grows downward; the four diagonals map to the four grid dirs.
        assertEquals(Direction.SOUTH, iso.directionForSwipe(10, -10)); // up-right
        assertEquals(Direction.EAST, iso.directionForSwipe(10, 10));   // down-right
        assertEquals(Direction.NORTH, iso.directionForSwipe(-10, 10)); // down-left
        assertEquals(Direction.WEST, iso.directionForSwipe(-10, -10)); // up-left
    }
}
