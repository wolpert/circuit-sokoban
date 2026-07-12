package com.circuitsokoban.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.circuitsokoban.game.PlayController;
import com.circuitsokoban.game.PlaySession;
import com.circuitsokoban.model.Board;
import com.circuitsokoban.model.Direction;
import com.circuitsokoban.model.Pos;
import com.circuitsokoban.render.BoardRenderer;
import com.circuitsokoban.render.BoardView;
import com.circuitsokoban.render.IsoProjector;
import com.circuitsokoban.render.Palette;
import com.circuitsokoban.solver.GenParams;
import com.circuitsokoban.solver.Level;
import com.circuitsokoban.solver.LevelGenerator;

/**
 * Plays a single generated level: renders the board isometrically with the juice
 * layer ({@link BoardView}), shows a HUD, and routes input through
 * {@link com.circuitsokoban.input.GameInput} into a {@link PlayController}.
 */
public final class GameScreen extends ScreenAdapter {

    /** Debug triggers for headless verification of the animation layer. */
    public enum Debug { NONE, KICK_ROTATE, KICK_PUSH, SOLVED_WAVE }

    private static final float WORLD_W = 540f;
    private static final float WORLD_H = 960f;
    private static final float TILE_WIDTH = 122f;
    private static final float BOARD_CENTER_Y = 500f;

    private final Viewport viewport;
    private final OrthographicCamera camera;
    private final ShapeRenderer shapes;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private final IsoProjector iso;
    private final BoardRenderer renderer;
    private final BoardView view;
    private final PlaySession session;
    private final PlayController controller;
    private final Debug debug;

    public GameScreen(long seed, GenParams params, Debug debug) {
        this.debug = debug;
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(WORLD_W, WORLD_H, camera);
        this.shapes = new ShapeRenderer();
        this.batch = new SpriteBatch();
        this.font = new BitmapFont();
        this.font.getData().setScale(1.6f);

        Level level = new LevelGenerator().generate(seed, params);
        // For the solve-wave demo, start already-solved so the sweep can be triggered.
        Level playLevel = debug == Debug.SOLVED_WAVE
                ? new Level(level.seed(), level.solvedBoard(), level.solvedBoard(),
                        level.par(), level.difficulty())
                : level;
        this.session = new PlaySession(playLevel);

        this.iso = new IsoProjector(TILE_WIDTH);
        iso.setBoardCenter(session.board().width(), session.board().height(),
                WORLD_W / 2f, BOARD_CENTER_Y);
        this.view = new BoardView(iso);
        this.renderer = new BoardRenderer(iso);
        this.controller = new PlayController(session, view);
    }

    public PlaySession session() {
        return session;
    }

    /** Fires the configured debug action once (called a frame or two in, for screenshots). */
    public void applyDebug() {
        Board board = session.board();
        Pos player = board.player();
        switch (debug) {
            case KICK_ROTATE -> {
                for (Direction d : Direction.values()) {
                    if (board.pieceAt(player.step(d)) != null) {
                        controller.rotate(player.step(d));
                        break;
                    }
                }
            }
            case KICK_PUSH -> {
                for (Direction d : Direction.values()) {
                    Pos ahead = player.step(d);
                    if (board.pieceAt(ahead) != null && board.isStandable(ahead.step(d))) {
                        controller.step(d);
                        break;
                    }
                }
            }
            case SOLVED_WAVE -> view.onSolved(session.circuit());
            case NONE -> { }
        }
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(new com.circuitsokoban.input.GameInput(controller, viewport, iso));
    }

    @Override
    public void render(float delta) {
        view.update(delta);

        ScreenUtils.clear(Palette.BACKGROUND);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        camera.zoom = view.cameraZoom();
        camera.update();
        shapes.setProjectionMatrix(camera.combined);
        renderer.render(shapes, session.board(), view);

        drawHud();
    }

    private void drawHud() {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "Moves  " + session.moves() + "     Par  " + session.level().par(),
                28f, WORLD_H - 34f);
        font.draw(batch, "Difficulty  " + stars(session.level().difficulty()),
                28f, WORLD_H - 74f);
        if (session.isSolved()) {
            font.setColor(Color.valueOf("7CF6B0"));
            font.draw(batch, "SOLVED!   " + session.rank(), 28f, 150f);
        } else {
            font.setColor(Color.valueOf("8792A8"));
            font.draw(batch, "Swipe / arrows: move    Tap piece: rotate", 28f, 110f);
            font.draw(batch, "Z: undo    Y: redo", 28f, 74f);
        }
        batch.end();
    }

    private static String stars(int difficulty) {
        return "*".repeat(difficulty) + "-".repeat(5 - difficulty);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        shapes.dispose();
        batch.dispose();
        font.dispose();
    }
}
