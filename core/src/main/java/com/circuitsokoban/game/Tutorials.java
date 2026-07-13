package com.circuitsokoban.game;

import com.circuitsokoban.model.Board;
import com.circuitsokoban.model.Piece;
import com.circuitsokoban.model.PieceType;
import com.circuitsokoban.model.Pos;

/**
 * Decides which one-time {@link Lesson} (if any) a freshly-loaded board should
 * teach. This is the single detection seam &mdash; add a rule here when adding a
 * new teachable piece (see {@link Lesson}).
 */
public final class Tutorials {

    private Tutorials() {}

    /** The first not-yet-seen lesson this board can teach, in learning order, or null. */
    public static Lesson firstUnseen(Board board, Progress progress) {
        if (!progress.hasSeenLesson(Lesson.BASICS)) {
            return Lesson.BASICS;
        }
        if (contains(board, PieceType.DIODE) && !progress.hasSeenLesson(Lesson.DIODE)) {
            return Lesson.DIODE;
        }
        if (hasIce(board) && !progress.hasSeenLesson(Lesson.ICE)) {
            return Lesson.ICE;
        }
        if (board.hasSecondary() && !progress.hasSeenLesson(Lesson.GATE)) {
            return Lesson.GATE;
        }
        return null;
    }

    public static boolean contains(Board board, PieceType type) {
        for (int y = 0; y < board.height(); y++) {
            for (int x = 0; x < board.width(); x++) {
                Piece p = board.pieceAt(new Pos(x, y));
                if (p != null && p.type() == type) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean hasIce(Board board) {
        for (int y = 0; y < board.height(); y++) {
            for (int x = 0; x < board.width(); x++) {
                if (board.isIce(new Pos(x, y))) {
                    return true;
                }
            }
        }
        return false;
    }
}
