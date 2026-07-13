package com.circuitsokoban.solver;

import com.circuitsokoban.model.Board;
import com.circuitsokoban.model.Piece;
import com.circuitsokoban.model.Pos;
import java.util.Arrays;
import java.util.Set;

/**
 * A canonical, hashable identity for a board state, used to dedupe the solver's
 * search.
 *
 * <p>Two design points make this canonical:
 * <ul>
 *   <li><b>Pieces are keyed by their input/output masks, not (type, orientation).</b>
 *       A rotation that changes nothing connectivity-wise (e.g. rotating a CROSS,
 *       or a STRAIGHT by 180&deg;) collapses to the same key &mdash; the solver
 *       never explores a wasted rotation twice &mdash; while a diode's two flow
 *       directions (identical openings, swapped in/out) stay distinct.</li>
 *   <li><b>The player is normalized to the minimum reachable cell.</b> Since
 *       walking is free and scoring counts only pushes/rotates, every player
 *       position within one walk-reachable region is equivalent; we represent
 *       the region by its smallest cell index.</li>
 * </ul>
 *
 * <p>Walls and terminals are fixed for a level, so they're omitted from the key.
 */
public final class StateKey {

    private final int[] masks;      // opening mask per cell (row-major), 0 if empty
    private final int playerIndex;  // canonical (minimum) reachable cell index
    private final int hash;

    private StateKey(int[] masks, int playerIndex) {
        this.masks = masks;
        this.playerIndex = playerIndex;
        this.hash = 31 * Arrays.hashCode(masks) + playerIndex;
    }

    public static StateKey of(Board board) {
        int w = board.width();
        int h = board.height();
        int[] masks = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Piece p = board.pieceAt(new Pos(x, y));
                // Encode both output and input masks: symmetric pieces still collapse
                // rotationally, but a diode's two flow directions (same openings,
                // swapped in/out) get distinct keys. A gate carries an extra bit so
                // it isn't confused with a same-shaped straight sitting in that cell.
                if (p == null) {
                    masks[y * w + x] = 0;
                } else {
                    int code = (p.outputs() << 4) | p.inputs();
                    if (p.type() == com.circuitsokoban.model.PieceType.GATE) {
                        code |= 1 << 8;
                    }
                    masks[y * w + x] = code;
                }
            }
        }
        int canonical = Integer.MAX_VALUE;
        Set<Pos> reachable = board.reachableCells();
        for (Pos p : reachable) {
            canonical = Math.min(canonical, p.y() * w + p.x());
        }
        return new StateKey(masks, canonical);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StateKey other)) return false;
        return playerIndex == other.playerIndex && Arrays.equals(masks, other.masks);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
