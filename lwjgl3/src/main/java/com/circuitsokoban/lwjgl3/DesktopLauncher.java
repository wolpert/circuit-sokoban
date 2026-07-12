package com.circuitsokoban.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.circuitsokoban.CircuitSokobanGame;
import com.circuitsokoban.solver.GenParams;

/**
 * Desktop (LWJGL3) launcher. Portrait window to match the Android target.
 *
 * <p>Args (all optional):
 * <ul>
 *   <li>{@code --seed <long>}</li>
 *   <li>{@code --difficulty easy|medium|hard}</li>
 *   <li>{@code --screenshot <path>} &mdash; render a few frames, save a PNG, exit</li>
 * </ul>
 */
public final class DesktopLauncher {

    // Portrait 9:16.
    private static final int WINDOW_W = 540;
    private static final int WINDOW_H = 960;

    public static void main(String[] args) {
        long seed = 1L;
        GenParams params = GenParams.medium();
        String screenshot = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--seed" -> seed = Long.parseLong(args[++i]);
                case "--difficulty" -> params = difficulty(args[++i]);
                case "--screenshot" -> screenshot = args[++i];
                default -> throw new IllegalArgumentException("Unknown arg: " + args[i]);
            }
        }

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Circuit Sokoban");
        config.setWindowedMode(WINDOW_W, WINDOW_H);
        config.setResizable(true);
        config.useVsync(true);
        config.setForegroundFPS(60);

        new Lwjgl3Application(new CircuitSokobanGame(seed, params, screenshot), config);
    }

    private static GenParams difficulty(String name) {
        return switch (name.toLowerCase()) {
            case "easy" -> GenParams.easy();
            case "medium" -> GenParams.medium();
            case "hard" -> GenParams.hard();
            default -> throw new IllegalArgumentException("Unknown difficulty: " + name);
        };
    }
}
