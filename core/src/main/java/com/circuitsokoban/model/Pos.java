package com.circuitsokoban.model;

/** Immutable grid coordinate. Value type &mdash; safe as a map/set key. */
public record Pos(int x, int y) {

    public Pos step(Direction d) {
        return new Pos(x + d.dx, y + d.dy);
    }

    public Pos step(Direction d, int n) {
        return new Pos(x + d.dx * n, y + d.dy * n);
    }
}
