package com.circuitsokoban.model;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * The full game state: a rectangular grid of walls / movable pieces / a walking
 * player, plus the two fixed terminals (source and receiver).
 *
 * <p>Coordinates are grid cells with (0,0) at the bottom-left. Walls are fixed
 * for the life of a level; pieces and the player move.
 *
 * <p>Move application is functional: {@link #stepPlayer} and {@link #rotateAt}
 * return a {@link MoveResult} wrapping a <em>new</em> board, leaving {@code this}
 * untouched. That keeps the undo stack and the solver trivial (every state is a
 * snapshot) at the cost of copying the piece grid per move &mdash; fine for the
 * small boards this game uses.
 */
public final class Board {

    private final int width;
    private final int height;
    private final boolean[][] walls;   // [x][y], immutable for a level's lifetime
    private final Piece[][] pieces;    // [x][y], null == empty
    private final boolean[][] ice;     // [x][y], slide tiles; fixed for a level
    private final Terminal source;
    private final Terminal receiver;
    private Terminal source2;          // optional secondary circuit that unlocks gates
    private Terminal receiver2;
    private boolean gateLatched;       // secondary completed at least once -> gates stay open
    private Pos player;

    public Board(int width, int height, Terminal source, Terminal receiver, Pos player) {
        this.width = width;
        this.height = height;
        this.walls = new boolean[width][height];
        this.pieces = new Piece[width][height];
        this.ice = new boolean[width][height];
        this.source = source;
        this.receiver = receiver;
        this.player = player;
    }

    private Board(Board other) {
        this.width = other.width;
        this.height = other.height;
        this.walls = other.walls; // shared: never mutated after construction
        this.ice = other.ice;     // shared: fixed for a level
        this.pieces = new Piece[width][height];
        for (int x = 0; x < width; x++) {
            System.arraycopy(other.pieces[x], 0, this.pieces[x], 0, height);
        }
        this.source = other.source;
        this.receiver = other.receiver;
        this.source2 = other.source2;
        this.receiver2 = other.receiver2;
        this.gateLatched = other.gateLatched;
        this.player = other.player;
    }

    public Board copy() {
        return new Board(this);
    }

    // ---- construction / generation helpers (mutating; use before play) ----

    public void setWall(Pos p, boolean wall) {
        walls[p.x()][p.y()] = wall;
    }

    public void setIce(Pos p, boolean isIce) {
        ice[p.x()][p.y()] = isIce;
    }

    public void setPiece(Pos p, Piece piece) {
        pieces[p.x()][p.y()] = piece;
    }

    public void setPlayer(Pos p) {
        player = p;
    }

    /** Adds a secondary source/receiver pair whose completion unlocks GATE pieces. */
    public void setSecondary(Terminal source2, Terminal receiver2) {
        this.source2 = source2;
        this.receiver2 = receiver2;
    }

    // ---- queries ----

    public int width() { return width; }
    public int height() { return height; }
    public Pos player() { return player; }
    public Terminal source() { return source; }
    public Terminal receiver() { return receiver; }
    public Terminal source2() { return source2; }
    public Terminal receiver2() { return receiver2; }
    public boolean hasSecondary() { return source2 != null && receiver2 != null; }
    public boolean isGateLatched() { return gateLatched; }
    public void setGateLatched(boolean latched) { this.gateLatched = latched; }

    public boolean inBounds(Pos p) {
        return p.x() >= 0 && p.x() < width && p.y() >= 0 && p.y() < height;
    }

    public boolean isWall(Pos p) {
        return inBounds(p) && walls[p.x()][p.y()];
    }

    /** A slide tile: a pushed piece that lands on ice keeps sliding until an obstacle. */
    public boolean isIce(Pos p) {
        return inBounds(p) && ice[p.x()][p.y()];
    }

    public Piece pieceAt(Pos p) {
        return inBounds(p) ? pieces[p.x()][p.y()] : null;
    }

    public boolean isTerminal(Pos p) {
        return source.pos().equals(p) || receiver.pos().equals(p)
                || (source2 != null && source2.pos().equals(p))
                || (receiver2 != null && receiver2.pos().equals(p));
    }

    /** A cell the player (or a pushed piece) can occupy: in bounds and empty. */
    public boolean isStandable(Pos p) {
        return inBounds(p) && !isWall(p) && pieceAt(p) == null && !isTerminal(p);
    }

    /** Cells the player can walk to from its current position (pieces block; free to enter). */
    public Set<Pos> reachableCells() {
        Set<Pos> seen = new HashSet<>();
        ArrayDeque<Pos> queue = new ArrayDeque<>();
        seen.add(player);
        queue.add(player);
        while (!queue.isEmpty()) {
            Pos cur = queue.poll();
            for (Direction d : Direction.values()) {
                Pos next = cur.step(d);
                if (isStandable(next) && seen.add(next)) {
                    queue.add(next);
                }
            }
        }
        return seen;
    }

    // ---- game-layer moves (explicit player, for input + animation) ----

    /**
     * The player attempts to step one tile in direction {@code d}. Empty ahead
     * -> a free walk; a piece ahead -> a push (if the tile beyond is standable).
     * Returns an illegal result (original board unchanged) if blocked.
     */
    public MoveResult stepPlayer(Direction d) {
        Pos ahead = player.step(d);

        if (isStandable(ahead)) {
            Board next = copy();
            next.player = ahead;
            return MoveResult.walk(next, player, ahead);
        }

        Piece pushed = pieceAt(ahead);
        if (pushed != null) {
            Pos beyond = ahead.step(d);
            if (isStandable(beyond)) {
                Pos dest = slideDestination(beyond, d);
                Board next = copy();
                next.pieces[ahead.x()][ahead.y()] = null;
                next.pieces[dest.x()][dest.y()] = pushed;
                next.player = ahead; // the player moves one tile; only the piece slides
                return MoveResult.push(next, ahead, dest, d);
            }
        }
        return MoveResult.illegal();
    }

    /**
     * Where a piece pushed onto {@code landing} comes to rest: it keeps sliding in
     * direction {@code d} while it's on ice and the next tile is free, stopping on
     * the first solid tile or against an obstacle.
     */
    public Pos slideDestination(Pos landing, Direction d) {
        Pos cur = landing;
        while (isIce(cur) && isStandable(cur.step(d))) {
            cur = cur.step(d);
        }
        return cur;
    }

    /**
     * The player rotates the piece at {@code target} 90&deg; clockwise. Legal
     * only when {@code target} holds a piece and the player currently stands on
     * a cell orthogonally adjacent to it.
     */
    public MoveResult rotateAt(Pos target) {
        Piece piece = pieceAt(target);
        if (piece == null || !isPlayerAdjacent(target)) {
            return MoveResult.illegal();
        }
        Board next = copy();
        next.pieces[target.x()][target.y()] = piece.rotatedCW();
        return MoveResult.rotate(next, target);
    }

    private boolean isPlayerAdjacent(Pos p) {
        for (Direction d : Direction.values()) {
            if (player.step(d).equals(p)) {
                return true;
            }
        }
        return false;
    }
}
