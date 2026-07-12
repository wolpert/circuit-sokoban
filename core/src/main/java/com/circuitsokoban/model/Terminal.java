package com.circuitsokoban.model;

/**
 * A fixed circuit endpoint &mdash; the power source or the receiver.
 *
 * <p>Terminals occupy a cell but are never pushed or rotated. Each has a single
 * opening pointing at the adjacent grid cell it can connect through.
 */
public record Terminal(Pos pos, Direction opening) {

    public boolean hasOpening(Direction d) {
        return d == opening;
    }
}
