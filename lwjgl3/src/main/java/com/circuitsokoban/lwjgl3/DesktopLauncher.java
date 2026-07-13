package com.circuitsokoban.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.circuitsokoban.CircuitSokobanGame;
import com.circuitsokoban.game.Tier;
import com.circuitsokoban.screen.GameScreen;

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
        long seed = 0L;
        Tier tier = Tier.MEDIUM;
        String screenshot = null;
        float shotDelay = 0.1f;
        GameScreen.Debug debug = GameScreen.Debug.NONE;
        boolean menuShot = false;
        String legendDir = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--seed" -> seed = Long.parseLong(args[++i]);
                case "--difficulty" -> tier = tier(args[++i]);
                case "--screenshot" -> screenshot = args[++i];
                case "--shotdelay" -> shotDelay = Float.parseFloat(args[++i]);
                case "--debug" -> debug = debug(args[++i]);
                case "--menu" -> menuShot = true;
                case "--legend" -> legendDir = args[++i];
                default -> throw new IllegalArgumentException("Unknown arg: " + args[i]);
            }
        }

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Circuit Sokoban");
        config.setWindowedMode(WINDOW_W, WINDOW_H);
        config.setResizable(true);
        config.useVsync(true);
        config.setForegroundFPS(60);

        new Lwjgl3Application(
                new CircuitSokobanGame(seed, tier, screenshot, shotDelay, debug, menuShot, legendDir),
                config);
    }

    private static GameScreen.Debug debug(String name) {
        return switch (name.toLowerCase()) {
            case "kick-rotate" -> GameScreen.Debug.KICK_ROTATE;
            case "kick-push" -> GameScreen.Debug.KICK_PUSH;
            case "solved" -> GameScreen.Debug.SOLVED_WAVE;
            case "tutorial-basics" -> GameScreen.Debug.TUTORIAL_BASICS;
            case "tutorial-diode" -> GameScreen.Debug.TUTORIAL_DIODE;
            case "tutorial-ice" -> GameScreen.Debug.TUTORIAL_ICE;
            case "tutorial-gate" -> GameScreen.Debug.TUTORIAL_GATE;
            default -> throw new IllegalArgumentException("Unknown debug: " + name);
        };
    }

    private static Tier tier(String name) {
        return switch (name.toLowerCase()) {
            case "easy" -> Tier.EASY;
            case "medium" -> Tier.MEDIUM;
            case "hard" -> Tier.HARD;
            default -> throw new IllegalArgumentException("Unknown difficulty: " + name);
        };
    }
}
