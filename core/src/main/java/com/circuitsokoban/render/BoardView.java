package com.circuitsokoban.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.circuitsokoban.model.Board;
import com.circuitsokoban.model.Circuit;
import com.circuitsokoban.model.Direction;
import com.circuitsokoban.model.MoveResult;
import com.circuitsokoban.model.Piece;
import com.circuitsokoban.model.Pos;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * The animation / juice layer. Logic (in {@link com.circuitsokoban.game.PlaySession})
 * is instantaneous and authoritative; this class holds the <em>visual</em> state
 * that eases toward it: eased piece/player movement, smooth rotation, an
 * invalid-push bump, a sequential energize sweep on solve, an idle pulse on the
 * live-but-incomplete chain, particle bursts, and a camera punch.
 *
 * <p>Driven by {@code PlayController}: it calls {@link #onMove}/{@link #onInvalidPush}/
 * {@link #onSolved} as the logic changes and {@link #syncTo} to snap (undo/redo,
 * load). The renderer queries the getters each frame.
 */
public final class BoardView {

    private static final float MOVE_DUR = 0.14f;
    private static final float ROTATE_DUR = 0.16f;
    private static final float BUMP_DUR = 0.20f;
    private static final float BUMP_PX = 10f;
    private static final float WAVE_LAYER_INTERVAL = 0.06f;
    private static final float PUNCH_DUR = 0.30f;
    private static final float PUNCH_AMOUNT = 0.06f;
    private static final float PULSE_SPEED = 3.4f;

    // Mild overshoot-and-settle for movement and rotation.
    private static final Interpolation EASE = new Interpolation.SwingOut(1.3f);

    private final IsoProjector iso;
    private final Random rng = new Random(1); // visual jitter only; determinism not required

    private float time;

    // Player movement.
    private final Vector2 playerFrom = new Vector2();
    private final Vector2 playerTo = new Vector2();
    private float playerElapsed = MOVE_DUR;

    // Sliding piece (a push).
    private Pos slideTo;
    private final Vector2 slideDelta = new Vector2(); // fromWorld - toWorld
    private float slideElapsed;

    // Rotating piece.
    private Pos rotCell;
    private float rotElapsed;

    // Invalid-push bump.
    private Pos bumpCell;
    private final Vector2 bumpDir = new Vector2();
    private float bumpElapsed = BUMP_DUR;

    // Energize wave (on solve).
    private List<List<Pos>> waveLayers = List.of();
    private Map<Pos, Integer> layerOf = new HashMap<>();
    private float waveElapsed;
    private int litLayers;
    private boolean waving;
    private Pos receiverCell;

    private boolean solved;
    private Set<Pos> energized = new HashSet<>();

    private final List<Particle> particles = new ArrayList<>();
    private float punchElapsed = PUNCH_DUR;

    private final Vector2 tmp = new Vector2();

    public BoardView(IsoProjector iso) {
        this.iso = iso;
    }

    // ---- events from the controller ----

    /** Snap all visual state to match the board exactly (load, undo, redo). */
    public void syncTo(Board board, Circuit.Result circuit) {
        iso.center(board.player(), playerFrom);
        playerTo.set(playerFrom);
        playerElapsed = MOVE_DUR;
        slideTo = null;
        rotCell = null;
        bumpCell = null;
        waving = false;
        particles.clear();
        energized = new HashSet<>(circuit.energized());
        solved = circuit.solved();
        receiverCell = board.receiver().pos();
    }

    public void onMove(MoveResult r, Circuit.Result circuit) {
        // Player eases from wherever it visually is now to its new cell.
        playerFrom.set(currentPlayer(tmp));
        iso.center(r.board().player(), playerTo);
        playerElapsed = 0f;

        if (r.kind() == MoveResult.Kind.PUSH) {
            slideTo = r.to();
            iso.center(r.from(), slideDelta);            // from-world
            slideDelta.sub(iso.worldX(r.to().x(), r.to().y()),
                    iso.worldY(r.to().x(), r.to().y())); // minus to-world
            slideElapsed = 0f;
        } else if (r.kind() == MoveResult.Kind.ROTATE) {
            rotCell = r.rotated();
            rotElapsed = 0f;
        }

        spawnJoinBursts(circuit.energized());
        energized = new HashSet<>(circuit.energized());
    }

    public void onInvalidPush(Pos pieceCell, Direction dir) {
        bumpCell = pieceCell;
        float cx = iso.worldX(pieceCell.x(), pieceCell.y());
        float cy = iso.worldY(pieceCell.x(), pieceCell.y());
        bumpDir.set(iso.worldX(pieceCell.x() + dir.dx, pieceCell.y() + dir.dy) - cx,
                iso.worldY(pieceCell.x() + dir.dx, pieceCell.y() + dir.dy) - cy).nor();
        bumpElapsed = 0f;
    }

    /** Start the sequential energize sweep, the solve burst schedule, and the camera punch. */
    public void onSolved(Circuit.Result circuit) {
        solved = true;
        energized = new HashSet<>(circuit.energized());
        waveLayers = circuit.layers();
        layerOf = new HashMap<>();
        for (int i = 0; i < waveLayers.size(); i++) {
            for (Pos p : waveLayers.get(i)) {
                layerOf.put(p, i);
            }
        }
        waveElapsed = 0f;
        litLayers = 0;
        waving = !waveLayers.isEmpty();
        punchElapsed = 0f;
    }

    // ---- per-frame update ----

    public void update(float dt) {
        time += dt;
        playerElapsed = Math.min(playerElapsed + dt, MOVE_DUR);
        if (slideTo != null) {
            slideElapsed += dt;
            if (slideElapsed >= MOVE_DUR) {
                slideTo = null;
            }
        }
        if (rotCell != null) {
            rotElapsed += dt;
            if (rotElapsed >= ROTATE_DUR) {
                rotCell = null;
            }
        }
        if (bumpCell != null) {
            bumpElapsed += dt;
            if (bumpElapsed >= BUMP_DUR) {
                bumpCell = null;
            }
        }
        if (punchElapsed < PUNCH_DUR) {
            punchElapsed += dt;
        }
        updateWave(dt);
        updateParticles(dt);
    }

    private void updateWave(float dt) {
        if (!waving) {
            return;
        }
        waveElapsed += dt;
        int nowLit = (int) (waveElapsed / WAVE_LAYER_INTERVAL);
        for (int layer = litLayers; layer < nowLit && layer < waveLayers.size(); layer++) {
            for (Pos p : waveLayers.get(layer)) {
                spawnBurst(iso.worldX(p.x(), p.y()), iso.worldY(p.x(), p.y()),
                        6, 60f, Palette.WIRE_ENERGIZED, false);
            }
        }
        litLayers = Math.min(nowLit, waveLayers.size());
        if (litLayers >= waveLayers.size()) {
            waving = false;
            // Big burst at the receiver as power arrives.
            spawnBurst(iso.worldX(receiverCell.x(), receiverCell.y()),
                    iso.worldY(receiverCell.x(), receiverCell.y()),
                    26, 150f, Palette.RECEIVER, true);
        }
    }

    private void updateParticles(float dt) {
        for (int i = particles.size() - 1; i >= 0; i--) {
            if (!particles.get(i).update(dt)) {
                particles.remove(i);
            }
        }
    }

    // ---- queries for the renderer ----

    public Vector2 playerWorld(Vector2 out) {
        return currentPlayer(out);
    }

    private Vector2 currentPlayer(Vector2 out) {
        float t = EASE.apply(Math.min(playerElapsed / MOVE_DUR, 1f));
        return out.set(MathUtils.lerp(playerFrom.x, playerTo.x, t),
                MathUtils.lerp(playerFrom.y, playerTo.y, t));
    }

    /** Extra world offset applied to a whole piece: a push slide, or an invalid-push bump. */
    public Vector2 offsetFor(Pos cell, Vector2 out) {
        out.set(0f, 0f);
        if (cell.equals(slideTo)) {
            float t = EASE.apply(Math.min(slideElapsed / MOVE_DUR, 1f));
            out.set(slideDelta).scl(1f - t); // from (fromWorld-toWorld) down to 0
        }
        if (cell.equals(bumpCell)) {
            float mag = BUMP_PX * MathUtils.sin(MathUtils.PI * Math.min(bumpElapsed / BUMP_DUR, 1f));
            out.add(bumpDir.x * mag, bumpDir.y * mag);
        }
        return out;
    }

    /** Rotation progress 0..~1 for the rotating piece at {@code cell}, or -1 if not rotating. */
    public float rotationProgress(Pos cell) {
        if (!cell.equals(rotCell)) {
            return -1f;
        }
        return EASE.apply(Math.min(rotElapsed / ROTATE_DUR, 1f));
    }

    /** Whether a piece should currently look energized (respects the sequential sweep). */
    public boolean isLit(Pos cell) {
        if (waving) {
            Integer layer = layerOf.get(cell);
            return layer != null && layer < litLayers;
        }
        return energized.contains(cell);
    }

    /** Brightness multiplier for the live-but-incomplete chain (a gentle pulse); 1 when solved. */
    public float pulse() {
        if (solved) {
            return 1f;
        }
        return 0.72f + 0.28f * (0.5f + 0.5f * MathUtils.sin(time * PULSE_SPEED));
    }

    /** Camera zoom for the solve punch (dips in then settles back to 1). */
    public float cameraZoom() {
        if (punchElapsed >= PUNCH_DUR) {
            return 1f;
        }
        return 1f - PUNCH_AMOUNT * MathUtils.sin(MathUtils.PI * (punchElapsed / PUNCH_DUR));
    }

    List<Particle> particles() {
        return particles;
    }

    // ---- particle spawning ----

    private void spawnJoinBursts(Set<Pos> newlyEnergized) {
        for (Pos p : newlyEnergized) {
            if (!energized.contains(p)) {
                spawnBurst(iso.worldX(p.x(), p.y()), iso.worldY(p.x(), p.y()),
                        7, 70f, Palette.WIRE_ENERGIZED, false);
            }
        }
    }

    private void spawnBurst(float x, float y, int count, float speed, Color color, boolean triangles) {
        for (int i = 0; i < count; i++) {
            float ang = rng.nextFloat() * MathUtils.PI2;
            float spd = speed * (0.4f + 0.6f * rng.nextFloat());
            float life = 0.35f + 0.35f * rng.nextFloat();
            float size = 5f + 6f * rng.nextFloat();
            float spin = (rng.nextFloat() - 0.5f) * 720f;
            particles.add(new Particle(x, y,
                    MathUtils.cos(ang) * spd, MathUtils.sin(ang) * spd + speed * 0.3f,
                    life, size, spin, color, triangles && rng.nextBoolean()));
        }
    }
}
