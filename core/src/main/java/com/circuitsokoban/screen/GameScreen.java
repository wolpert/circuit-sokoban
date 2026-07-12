package com.circuitsokoban.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.circuitsokoban.game.Navigator;
import com.circuitsokoban.game.PlayController;
import com.circuitsokoban.game.PlaySession;
import com.circuitsokoban.game.Progress;
import com.circuitsokoban.game.Tier;
import com.circuitsokoban.input.GameInput;
import com.circuitsokoban.model.Board;
import com.circuitsokoban.model.Direction;
import com.circuitsokoban.model.Pos;
import com.circuitsokoban.render.BoardRenderer;
import com.circuitsokoban.render.BoardView;
import com.circuitsokoban.render.IsoProjector;
import com.circuitsokoban.render.Palette;
import com.circuitsokoban.solver.Level;
import com.circuitsokoban.solver.LevelGenerator;

/**
 * Plays one level of a tier's endless stream: renders it with the juice layer,
 * records the result to {@link Progress} on solve, and on completion offers
 * "next level" (advance the tier's seed) or "menu".
 */
public final class GameScreen extends ScreenAdapter {

    /** Debug triggers for headless verification of the animation layer. */
    public enum Debug { NONE, KICK_ROTATE, KICK_PUSH, SOLVED_WAVE }

    private static final float WORLD_W = 540f;
    private static final float WORLD_H = 960f;
    private static final float TILE_WIDTH = 122f;
    private static final float BOARD_CENTER_Y = 500f;
    private static final Rectangle MENU_BUTTON = new Rectangle(392f, 902f, 120f, 44f);

    private final Navigator nav;
    private final Progress progress;
    private final Tier tier;
    private final long seed;
    private final Debug debug;

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

    private final Vector2 tmp = new Vector2();
    private boolean recorded;

    public GameScreen(Navigator nav, Progress progress, Tier tier, long seed, Debug debug) {
        this.nav = nav;
        this.progress = progress;
        this.tier = tier;
        this.seed = seed;
        this.debug = debug;

        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(WORLD_W, WORLD_H, camera);
        this.shapes = new ShapeRenderer();
        this.batch = new SpriteBatch();
        this.font = new BitmapFont();
        this.font.getData().setScale(1.6f);

        Level level = new LevelGenerator().generate(seed, tier.params());
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

    /** Fires the configured debug action once (headless screenshots). */
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
        Gdx.input.setInputProcessor(new InputMultiplexer(new NavInput(),
                new GameInput(controller, viewport, iso)));
    }

    @Override
    public void render(float delta) {
        view.update(delta);
        if (session.isSolved() && !recorded) {
            progress.record(tier, session.level(), session.moves());
            recorded = true;
        }

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
        font.draw(batch, tier.displayName() + "  #" + seed, 28f, WORLD_H - 34f);
        font.draw(batch, "Moves  " + session.moves() + "     Par  " + session.level().par(),
                28f, WORLD_H - 74f);
        font.setColor(Color.valueOf("8792A8"));
        font.draw(batch, "Menu", MENU_BUTTON.x + 14f, MENU_BUTTON.y + 30f);

        if (session.isSolved()) {
            font.setColor(Color.valueOf("7CF6B0"));
            font.draw(batch, "SOLVED!   " + session.rank().name(), 28f, 168f);
            font.setColor(Color.WHITE);
            font.draw(batch, "Best  " + progress.bestMoves(tier, seed) + " moves", 28f, 128f);
            font.setColor(Color.valueOf("8792A8"));
            font.draw(batch, "Tap / Enter: next level     Esc: menu", 28f, 88f);
        } else {
            font.setColor(Color.valueOf("8792A8"));
            font.draw(batch, "Swipe / arrows: move    Tap piece: rotate", 28f, 108f);
            font.draw(batch, "Z: undo   Y: redo", 28f, 72f);
        }
        batch.end();
    }

    private void advanceToNext() {
        long next = seed + 1;
        progress.setCurrentSeed(tier, next);
        nav.playLevel(tier, next);
    }

    /** Navigation input: menu button, and (once solved) advance to the next level. */
    private final class NavInput extends InputAdapter {
        @Override
        public boolean keyDown(int keycode) {
            if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.Q) {
                nav.showMenu();
                return true;
            }
            if (session.isSolved()
                    && (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE
                        || keycode == Input.Keys.N)) {
                advanceToNext();
                return true;
            }
            return false;
        }

        @Override
        public boolean touchUp(int screenX, int screenY, int pointer, int button) {
            viewport.unproject(tmp.set(screenX, screenY));
            if (MENU_BUTTON.contains(tmp.x, tmp.y)) {
                nav.showMenu();
                return true;
            }
            if (session.isSolved()) {
                advanceToNext();
                return true;
            }
            return false; // let gameplay input handle it
        }
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
