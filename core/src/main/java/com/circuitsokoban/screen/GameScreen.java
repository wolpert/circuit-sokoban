package com.circuitsokoban.screen;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.circuitsokoban.model.Board;
import com.circuitsokoban.model.Circuit;
import com.circuitsokoban.render.BoardRenderer;
import com.circuitsokoban.render.IsoProjector;
import com.circuitsokoban.render.Palette;
import com.circuitsokoban.solver.GenParams;
import com.circuitsokoban.solver.Level;
import com.circuitsokoban.solver.LevelGenerator;

/**
 * Renders a single generated level isometrically. Input and animation come in
 * later increments; for now this proves the rendering pipeline against a real
 * procedurally-generated board.
 *
 * <p>The world is a fixed portrait box scaled to any window/screen by a
 * {@link FitViewport}, so desktop and Android portrait share one layout.
 */
public final class GameScreen extends ScreenAdapter {

    private static final float WORLD_W = 540f;
    private static final float WORLD_H = 960f;
    private static final float TILE_WIDTH = 96f;

    private final Viewport viewport;
    private final OrthographicCamera camera;
    private final ShapeRenderer shapes;
    private final IsoProjector iso;
    private final BoardRenderer renderer;

    private final Level level;
    private Board board;
    private Circuit.Result circuit;

    public GameScreen(long seed, GenParams params) {
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(WORLD_W, WORLD_H, camera);
        this.shapes = new ShapeRenderer();
        this.level = new LevelGenerator().generate(seed, params);
        this.board = level.freshBoard();
        this.circuit = Circuit.evaluate(board);

        this.iso = new IsoProjector(TILE_WIDTH);
        iso.centerBoard(board.width(), board.height(), WORLD_W, WORLD_H);
        this.renderer = new BoardRenderer(iso);
    }

    public Level level() {
        return level;
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(Palette.BACKGROUND);
        camera.update();
        shapes.setProjectionMatrix(camera.combined);
        renderer.render(shapes, board, circuit);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        shapes.dispose();
    }
}
