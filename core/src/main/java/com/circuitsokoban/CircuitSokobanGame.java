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
 * <p>Supports an optional screenshot mode: render a few frames, write a PNG,
 * then exit. Used for headless visual verification during development.
 */
public final class CircuitSokobanGame extends Game {

    private final long seed;
    private final GenParams params;
    private final String screenshotPath; // null == normal interactive run
    private int frame;

    public CircuitSokobanGame() {
        this(1L, GenParams.medium(), null);
    }

    public CircuitSokobanGame(long seed, GenParams params, String screenshotPath) {
        this.seed = seed;
        this.params = params;
        this.screenshotPath = screenshotPath;
    }

    @Override
    public void create() {
        setScreen(new GameScreen(seed, params));
    }

    @Override
    public void render() {
        super.render();
        if (screenshotPath != null && ++frame == 3) {
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
