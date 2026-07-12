package com.circuitsokoban.game;

import com.circuitsokoban.model.Board;
import com.circuitsokoban.model.Circuit;
import com.circuitsokoban.model.Direction;
import com.circuitsokoban.model.MoveResult;
import com.circuitsokoban.model.Pos;
import com.circuitsokoban.solver.Level;
import java.util.ArrayDeque;
import java.util.Deque;

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

    private final Deque<Snapshot> undoStack = new ArrayDeque<>();
    private final Deque<Snapshot> redoStack = new ArrayDeque<>();

    public PlaySession(Level level) {
        this.level = level;
        this.board = level.freshBoard();
        recompute();
    }

    public boolean step(Direction d) {
        return apply(board.stepPlayer(d));
    }

    public boolean rotate(Pos target) {
        return apply(board.rotateAt(target));
    }

    private boolean apply(MoveResult result) {
        if (!result.isLegal()) {
            return false;
        }
        undoStack.push(new Snapshot(board, moves));
        redoStack.clear();
        board = result.board();
        if (result.scored()) {
            moves++;
        }
        recompute();
        return true;
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
        circuit = Circuit.evaluate(board);
        solved = circuit.solved();
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
