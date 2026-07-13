package com.circuitsokoban;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.circuitsokoban.game.Lesson;
import com.circuitsokoban.game.MemoryStore;
import com.circuitsokoban.game.Navigator;
import com.circuitsokoban.game.PreferencesStore;
import com.circuitsokoban.game.Progress;
import com.circuitsokoban.game.Store;
import com.circuitsokoban.game.Tier;
import com.circuitsokoban.screen.GameScreen;
import com.circuitsokoban.screen.LegendScreen;
import com.circuitsokoban.screen.MenuScreen;
import java.nio.ByteBuffer;

/**
 * libGDX entry point and screen {@link Navigator}. Owns {@link Progress} (backed
 * by persistent Preferences at runtime) and starts at the level-select menu.
 *
 * <p>Also supports a headless screenshot mode: jump straight into a level,
 * optionally fire a debug animation trigger, render for a while, save a PNG,
 * and exit.
 */
public final class CircuitSokobanGame extends Game implements Navigator {

    private final long startSeed;
    private final Tier startTier;
    private final String screenshotPath; // null == normal interactive run
    private final float shotDelay;
    private final GameScreen.Debug debug;
    private final boolean menuShot;      // screenshot the menu instead of a level
    private final String legendDir;      // non-null: export piece icons to this dir, then exit

    private Progress progress;
    private GameScreen shotScreen;
    private int frame;
    private float elapsed;
    private boolean debugFired;
    private float debugAt;

    public CircuitSokobanGame() {
        this(0L, Tier.MEDIUM, null, 0.1f, GameScreen.Debug.NONE, false, null);
    }

    public CircuitSokobanGame(long startSeed, Tier startTier, String screenshotPath,
                              float shotDelay, GameScreen.Debug debug, boolean menuShot,
                              String legendDir) {
        this.startSeed = startSeed;
        this.startTier = startTier;
        this.screenshotPath = screenshotPath;
        this.shotDelay = shotDelay;
        this.debug = debug;
        this.menuShot = menuShot;
        this.legendDir = legendDir;
    }

    private boolean screenshotMode() {
        return screenshotPath != null || debug != GameScreen.Debug.NONE;
    }

    private static boolean isTutorialDebug(GameScreen.Debug d) {
        return d == GameScreen.Debug.TUTORIAL_BASICS || d == GameScreen.Debug.TUTORIAL_DIODE
                || d == GameScreen.Debug.TUTORIAL_ICE || d == GameScreen.Debug.TUTORIAL_GATE;
    }

    @Override
    public void create() {
        if (legendDir != null) {
            setScreen(new LegendScreen(this, legendDir));
            return;
        }
        Store store = screenshotMode()
                ? new MemoryStore()
                : new PreferencesStore(Gdx.app.getPreferences("circuit-sokoban"));
        progress = new Progress(store);

        // Keep non-tutorial screenshots clean: pre-mark lessons seen so no overlay shows
        // unless a tutorial debug explicitly requests one.
        if (screenshotMode() && !isTutorialDebug(debug)) {
            for (Lesson lesson : Lesson.values()) {
                progress.markLessonSeen(lesson);
            }
        }

        if (screenshotMode() && menuShot) {
            showMenu();               // capture the menu; shotScreen stays null
        } else if (screenshotMode()) {
            shotScreen = new GameScreen(this, progress, startTier, startSeed, debug);
            setScreen(shotScreen);
        } else {
            showMenu();
        }
    }

    // ---- Navigator ----

    @Override
    public void playLevel(Tier tier, long seed) {
        switchTo(new GameScreen(this, progress, tier, seed, GameScreen.Debug.NONE));
    }

    @Override
    public void showMenu() {
        switchTo(new MenuScreen(this, progress));
    }

    private void switchTo(Screen next) {
        Screen old = getScreen();
        setScreen(next);       // hides old, shows next (sets input processor)
        if (old != null) {
            old.dispose();     // screens own GL resources; free the one we left
        }
    }

    @Override
    public void render() {
        super.render();
        if (legendDir != null || screenshotPath == null) {
            return; // legend export drives itself; interactive runs never capture
        }
        elapsed += Gdx.graphics.getDeltaTime();
        frame++;
        if (!debugFired && frame >= 2) {
            if (shotScreen != null) {
                shotScreen.applyDebug();
            }
            debugFired = true;
            debugAt = elapsed;
        }
        if (debugFired && elapsed - debugAt >= shotDelay) {
            saveScreenshot(screenshotPath);
            Gdx.app.exit();
        }
    }

    private static void saveScreenshot(String path) {
        int w = Gdx.graphics.getBackBufferWidth();
        int h = Gdx.graphics.getBackBufferHeight();
        Pixmap pixmap = Pixmap.createFromFrameBuffer(0, 0, w, h);
        flipVertically(pixmap, w, h); // framebuffer is bottom-up; PNG is top-down
        PixmapIO.writePNG(Gdx.files.absolute(path), pixmap);
        pixmap.dispose();
        Gdx.app.log("CircuitSokoban", "Wrote screenshot " + path + " (" + w + "x" + h + ")");
    }

    private static void flipVertically(Pixmap pixmap, int w, int h) {
        ByteBuffer pixels = pixmap.getPixels();
        int stride = w * 4; // RGBA8888
        byte[] flipped = new byte[stride * h];
        for (int row = 0; row < h; row++) {
            pixels.position((h - row - 1) * stride);
            pixels.get(flipped, row * stride, stride);
        }
        pixels.clear();
        pixels.put(flipped);
        pixels.clear();
    }
}
