package com.circuitsokoban;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.circuitsokoban.screen.GameScreen;
import com.circuitsokoban.solver.GenParams;
import java.nio.ByteBuffer;

/**
 * libGDX entry point. Owns the active screen. Platform launchers (desktop /
 * android) just construct this.
 *
 * <p>Supports a screenshot mode (render N frames, optionally fire a debug
 * animation trigger, write a PNG, then exit) for headless visual verification.
 */
public final class CircuitSokobanGame extends Game {

    private final long seed;
    private final GenParams params;
    private final String screenshotPath; // null == normal interactive run
    private final float shotDelay;        // seconds after the debug trigger to capture
    private final GameScreen.Debug debug;

    private GameScreen screen;
    private int frame;
    private float elapsed;
    private boolean debugFired;
    private float debugAt;

    public CircuitSokobanGame() {
        this(1L, GenParams.medium(), null, 0.1f, GameScreen.Debug.NONE);
    }

    public CircuitSokobanGame(long seed, GenParams params, String screenshotPath,
                              float shotDelay, GameScreen.Debug debug) {
        this.seed = seed;
        this.params = params;
        this.screenshotPath = screenshotPath;
        this.shotDelay = shotDelay;
        this.debug = debug;
    }

    @Override
    public void create() {
        screen = new GameScreen(seed, params, debug);
        setScreen(screen);
    }

    @Override
    public void render() {
        super.render();
        elapsed += Gdx.graphics.getDeltaTime();
        frame++;
        // Fire the debug trigger once, after one clean frame, and time the capture from there.
        if (!debugFired && frame >= 2) {
            screen.applyDebug(); // no-op unless a debug trigger was requested
            debugFired = true;
            debugAt = elapsed;
        }
        if (screenshotPath != null && debugFired && elapsed - debugAt >= shotDelay) {
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
