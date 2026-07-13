package com.circuitsokoban.game;

import com.circuitsokoban.model.Board;
import com.circuitsokoban.model.Circuit;
import com.circuitsokoban.model.Direction;
import com.circuitsokoban.model.MoveResult;
import com.circuitsokoban.model.Piece;
import com.circuitsokoban.model.PieceType;
import com.circuitsokoban.model.Pos;
import com.circuitsokoban.solver.Level;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Mutable per-play state for one level: the live board, the move counter,
 * undo/redo history, and cached circuit/solved status.
 *
 * <p>Pure Java (no libGDX) so the play rules &mdash; what counts as a move, when
 * a level is solved, how undo restores &mdash; are unit-testable. The screen
 * layer just calls {@link #step}/{@link #rotate}/{@link #undo}/{@link #redo} and
 * renders {@link #board()} + {@link #circuit()}.
 *
 * <p>Board moves are functional (each returns a new board and never mutates the
 * old one), so undo history just holds prior board references &mdash; no copies.
 */
public final class PlaySession {

    private record Snapshot(Board board, int moves) {}

    private final Level level;
    private Board board;
    private int moves;
    private Circuit.Result circuit;
    private boolean solved;
    private List<Pos> lastBurned = List.of(); // fuses that burned out on the last move

    private final Deque<Snapshot> undoStack = new ArrayDeque<>();
    private final Deque<Snapshot> redoStack = new ArrayDeque<>();

    public PlaySession(Level level) {
        this.level = level;
        this.board = level.freshBoard();
        recompute();
    }

    /** Applies a player step (walk or push). Returns the result (illegal if blocked). */
    public MoveResult step(Direction d) {
        return apply(board.stepPlayer(d));
    }

    /** Rotates the piece at {@code target} if legal. Returns the result. */
    public MoveResult rotate(Pos target) {
        return apply(board.rotateAt(target));
    }

    private MoveResult apply(MoveResult result) {
        if (!result.isLegal()) {
            return result;
        }
        undoStack.push(new Snapshot(board, moves));
        redoStack.clear();
        board = result.board();
        if (result.scored()) {
            moves++;
        }
        recompute();
        return result;
    }

    public boolean undo() {
        if (undoStack.isEmpty()) {
            return false;
        }
        redoStack.push(new Snapshot(board, moves));
        restore(undoStack.pop());
        return true;
    }

    public boolean redo() {
        if (redoStack.isEmpty()) {
            return false;
        }
        undoStack.push(new Snapshot(board, moves));
        restore(redoStack.pop());
        return true;
    }

    private void restore(Snapshot s) {
        board = s.board();
        moves = s.moves();
        recompute();
    }

    private void recompute() {
        List<Pos> fusesBefore = fuseCells();
        circuit = Circuit.resolve(board); // settle latch + burn energized fuses
        solved = circuit.solved();
        lastBurned = removed(fusesBefore, fuseCells());
    }

    /** Fuse cells that disappeared during the last recompute (for the shatter effect). */
    public List<Pos> lastBurned() {
        return lastBurned;
    }

    private List<Pos> fuseCells() {
        List<Pos> out = new ArrayList<>();
        for (int y = 0; y < board.height(); y++) {
            for (int x = 0; x < board.width(); x++) {
                Pos p = new Pos(x, y);
                Piece piece = board.pieceAt(p);
                if (piece != null && piece.type() == PieceType.FUSE) {
                    out.add(p);
                }
            }
        }
        return out;
    }

    private static List<Pos> removed(List<Pos> before, List<Pos> after) {
        List<Pos> gone = new ArrayList<>(before);
        gone.removeAll(after);
        return gone;
    }

    public Level level() { return level; }
    public Board board() { return board; }
    public int moves() { return moves; }
    public Circuit.Result circuit() { return circuit; }
    public boolean isSolved() { return solved; }
    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }

    /** The medal earned at the current move count, if solved. */
    public Level.Rank rank() {
        return level.rankFor(moves);
    }
}
