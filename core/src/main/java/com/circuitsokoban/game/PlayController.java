package com.circuitsokoban.game;

import com.circuitsokoban.model.Board;
import com.circuitsokoban.model.Direction;
import com.circuitsokoban.model.MoveResult;
import com.circuitsokoban.model.Pos;
import com.circuitsokoban.render.BoardView;
import java.util.List;

/**
 * Coordinates the authoritative logic ({@link PlaySession}) with the visual
 * layer ({@link BoardView}): applies a move, then tells the view how to animate
 * it (slide/rotate, invalid-push bump, energize sweep on solve). Input talks
 * only to this.
 */
public final class PlayController {

    private final PlaySession session;
    private final BoardView view;

    public PlayController(PlaySession session, BoardView view) {
        this.session = session;
        this.view = view;
        view.syncTo(session.board(), session.circuit());
    }

    public PlaySession session() {
        return session;
    }

    public Board board() {
        return session.board();
    }

    public MoveResult step(Direction d) {
        Board before = session.board();
        Pos ahead = before.player().step(d);
        boolean pushingPiece = before.pieceAt(ahead) != null;
        boolean solvedBefore = session.isSolved();

        MoveResult r = session.step(d);
        if (!r.isLegal()) {
            if (pushingPiece) {
                view.onInvalidPush(ahead, d); // bumped a piece into an obstacle
            }
            return r;
        }
        animate(r, solvedBefore);
        return r;
    }

    public MoveResult rotate(Pos target) {
        boolean solvedBefore = session.isSolved();
        MoveResult r = session.rotate(target);
        if (r.isLegal()) {
            animate(r, solvedBefore);
        }
        return r;
    }

    public void undo() {
        if (session.undo()) {
            view.syncTo(session.board(), session.circuit());
        }
    }

    public void redo() {
        if (session.redo()) {
            view.syncTo(session.board(), session.circuit());
        }
    }

    private void animate(MoveResult r, boolean solvedBefore) {
        view.onMove(r, session.circuit());
        List<Pos> burned = session.lastBurned();
        if (!burned.isEmpty()) {
            view.onFuseBurn(burned); // shatter where a fuse was consumed
        }
        if (session.isSolved() && !solvedBefore) {
            view.onSolved(session.circuit());
        }
    }
}
