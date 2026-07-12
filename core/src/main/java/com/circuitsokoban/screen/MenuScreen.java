package com.circuitsokoban.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.circuitsokoban.game.Navigator;
import com.circuitsokoban.game.Progress;
import com.circuitsokoban.game.Tier;
import com.circuitsokoban.render.Palette;

/**
 * Endless-by-tier level select, themed as an abstract circuit board. Each tier
 * is a panel (a connector "node") showing its solved/gold tally and the current
 * level index; pick a tier to play its current level, or browse the index with
 * the arrows to revisit / share a specific seed.
 */
public final class MenuScreen extends ScreenAdapter {

    private static final float WORLD_W = 540f;
    private static final float WORLD_H = 960f;
    private static final float PANEL_X = 40f;
    private static final float PANEL_W = 460f;
    private static final float PANEL_H = 150f;
    private static final float PANEL_TOP = 770f;
    private static final float PANEL_STEP = 180f;

    private final Navigator nav;
    private final Progress progress;
    private final Tier[] tiers = Tier.values();

    private final Viewport viewport;
    private final OrthographicCamera camera;
    private final ShapeRenderer shapes;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private final Vector2 tmp = new Vector2();

    private int selected;

    public MenuScreen(Navigator nav, Progress progress) {
        this.nav = nav;
        this.progress = progress;
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(WORLD_W, WORLD_H, camera);
        this.shapes = new ShapeRenderer();
        this.batch = new SpriteBatch();
        this.font = new BitmapFont();
    }

    private static Color tierColor(Tier tier) {
        return switch (tier) {
            case EASY -> Palette.WIRE_ENERGIZED;
            case MEDIUM -> Palette.SOURCE;
            case HARD -> Palette.PLAYER;
        };
    }

    private Rectangle panelRect(int i) {
        return new Rectangle(PANEL_X, PANEL_TOP - i * PANEL_STEP - PANEL_H, PANEL_W, PANEL_H);
    }

    private Rectangle leftArrow(int i) {
        Rectangle p = panelRect(i);
        return new Rectangle(p.x + 300f, p.y + p.height / 2f - 28f, 44f, 56f);
    }

    private Rectangle rightArrow(int i) {
        Rectangle p = panelRect(i);
        return new Rectangle(p.x + p.width - 52f, p.y + p.height / 2f - 28f, 44f, 56f);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(new MenuInput());
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(Palette.BACKGROUND);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        camera.update();

        shapes.setProjectionMatrix(camera.combined);
        drawBackgroundLattice();
        drawPanels();

        drawText();
    }

    /** Faint circuit-board lattice behind everything. */
    private void drawBackgroundLattice() {
        shapes.begin(ShapeType.Line);
        shapes.setColor(0.16f, 0.19f, 0.26f, 1f);
        float step = 90f;
        for (float x = 30f; x < WORLD_W; x += step) {
            shapes.line(x, 0f, x, WORLD_H);
        }
        for (float y = 30f; y < WORLD_H; y += step) {
            shapes.line(0f, y, WORLD_W, y);
        }
        shapes.end();

        shapes.begin(ShapeType.Filled);
        shapes.setColor(0.20f, 0.24f, 0.32f, 1f);
        for (float x = 30f; x < WORLD_W; x += step) {
            for (float y = 30f; y < WORLD_H; y += step) {
                shapes.circle(x, y, 4f, 10);
            }
        }
        shapes.end();
    }

    private void drawPanels() {
        for (int i = 0; i < tiers.length; i++) {
            Rectangle p = panelRect(i);
            boolean sel = i == selected;

            shapes.begin(ShapeType.Filled);
            shapes.setColor(sel ? Palette.TILE_LIGHT : Palette.TILE_DARK);
            shapes.rect(p.x, p.y, p.width, p.height);
            // Connector-node accent: a wire stub + joint in the tier's colour.
            Color c = tierColor(tiers[i]);
            shapes.setColor(c);
            float jy = p.y + p.height / 2f;
            shapes.rectLine(p.x, jy, p.x + 70f, jy, 8f);
            shapes.circle(p.x + 70f, jy, 16f, 24);
            // Level index arrows.
            drawArrow(leftArrow(i), false);
            drawArrow(rightArrow(i), true);
            shapes.end();

            shapes.begin(ShapeType.Line);
            shapes.setColor(sel ? Color.WHITE : Palette.TILE_BORDER);
            shapes.rect(p.x, p.y, p.width, p.height);
            shapes.end();
        }
    }

    private void drawArrow(Rectangle r, boolean pointRight) {
        shapes.setColor(Palette.JOINT);
        float cy = r.y + r.height / 2f;
        if (pointRight) {
            shapes.triangle(r.x, r.y, r.x, r.y + r.height, r.x + r.width, cy);
        } else {
            shapes.triangle(r.x + r.width, r.y, r.x + r.width, r.y + r.height, r.x, cy);
        }
    }

    private void drawText() {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        font.getData().setScale(2.6f);
        font.setColor(Color.WHITE);
        font.draw(batch, "CIRCUIT SOKOBAN", 40f, WORLD_H - 60f);

        font.getData().setScale(1.7f);
        for (int i = 0; i < tiers.length; i++) {
            Tier t = tiers[i];
            Rectangle p = panelRect(i);
            long lvl = progress.currentSeed(t);
            font.setColor(Color.WHITE);
            font.draw(batch, t.displayName(), p.x + 100f, p.y + p.height - 26f);
            font.setColor(Palette.RECEIVER);
            font.draw(batch, "Lv " + lvl, p.x + 352f, p.y + p.height / 2f + 12f);
            font.setColor(Color.valueOf("8792A8"));
            font.draw(batch, "Solved " + progress.solvedCount(t) + "     Gold " + progress.goldCount(t),
                    p.x + 100f, p.y + 44f);
        }

        font.getData().setScale(1.4f);
        font.setColor(Color.valueOf("8792A8"));
        font.draw(batch, "Up/Down: tier    Left/Right: level    Enter / tap: play", 40f, 150f);
        batch.end();
    }

    private void play(int i) {
        Tier t = tiers[i];
        nav.playLevel(t, progress.currentSeed(t));
    }

    private void changeLevel(int i, int delta) {
        Tier t = tiers[i];
        progress.setCurrentSeed(t, progress.currentSeed(t) + delta);
    }

    private final class MenuInput extends InputAdapter {
        @Override
        public boolean keyDown(int keycode) {
            switch (keycode) {
                case Input.Keys.UP, Input.Keys.W -> selected = Math.max(0, selected - 1);
                case Input.Keys.DOWN, Input.Keys.S -> selected = Math.min(tiers.length - 1, selected + 1);
                case Input.Keys.LEFT, Input.Keys.A -> changeLevel(selected, -1);
                case Input.Keys.RIGHT, Input.Keys.D -> changeLevel(selected, 1);
                case Input.Keys.ENTER, Input.Keys.SPACE -> play(selected);
                case Input.Keys.NUM_1 -> { selected = 0; play(0); }
                case Input.Keys.NUM_2 -> { selected = 1; play(1); }
                case Input.Keys.NUM_3 -> { selected = 2; play(2); }
                default -> { return false; }
            }
            return true;
        }

        @Override
        public boolean touchUp(int screenX, int screenY, int pointer, int button) {
            viewport.unproject(tmp.set(screenX, screenY));
            for (int i = 0; i < tiers.length; i++) {
                if (leftArrow(i).contains(tmp.x, tmp.y)) {
                    selected = i;
                    changeLevel(i, -1);
                    return true;
                }
                if (rightArrow(i).contains(tmp.x, tmp.y)) {
                    selected = i;
                    changeLevel(i, 1);
                    return true;
                }
                if (panelRect(i).contains(tmp.x, tmp.y)) {
                    selected = i;
                    play(i);
                    return true;
                }
            }
            return false;
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
