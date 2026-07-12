package com.circuitsokoban.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.circuitsokoban.game.PlaySession;
import com.circuitsokoban.input.GameInput;
import com.circuitsokoban.render.BoardRenderer;
import com.circuitsokoban.render.IsoProjector;
import com.circuitsokoban.render.Palette;
import com.circuitsokoban.solver.GenParams;
import com.circuitsokoban.solver.Level;
import com.circuitsokoban.solver.LevelGenerator;

/**
 * Plays a single generated level: renders the board isometrically, shows a HUD
 * (move counter / par / solved medal), and routes input through {@link GameInput}
 * into a {@link PlaySession}.
 *
 * <p>The world is a fixed portrait box scaled by a {@link FitViewport}, so
 * desktop and Android portrait share one layout.
 */
public final class GameScreen extends ScreenAdapter {

    private static final float WORLD_W = 540f;
    private static final float WORLD_H = 960f;
    private static final float TILE_WIDTH = 96f;

    private final Viewport viewport;
    private final OrthographicCamera camera;
    private final ShapeRenderer shapes;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private final IsoProjector iso;
    private final BoardRenderer renderer;
    private final PlaySession session;

    public GameScreen(long seed, GenParams params) {
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(WORLD_W, WORLD_H, camera);
        this.shapes = new ShapeRenderer();
        this.batch = new SpriteBatch();
        this.font = new BitmapFont();
        this.font.getData().setScale(1.6f);

        Level level = new LevelGenerator().generate(seed, params);
        this.session = new PlaySession(level);

        this.iso = new IsoProjector(TILE_WIDTH);
        iso.centerBoard(level.startBoard().width(), level.startBoard().height(), WORLD_W, WORLD_H);
        this.renderer = new BoardRenderer(iso);
    }

    public PlaySession session() {
        return session;
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(new GameInput(session, viewport, iso));
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(Palette.BACKGROUND);
        camera.update();
        shapes.setProjectionMatrix(camera.combined);
        renderer.render(shapes, session.board(), session.circuit());

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
