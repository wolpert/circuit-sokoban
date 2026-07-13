# Circuit Sokoban

A small, polished isometric **Sokoban-style circuit puzzle** built with libGDX,
targeting Linux desktop and Android from a single codebase. Push and rotate
connector pieces on a grid to route power from a source to a receiver and
complete the circuit.

> Personal hobby project — the emphasis is a clean, fun, replayable core loop
> over breadth of content. No audio.

## Gameplay

- **Isometric grid.** A walking avatar pushes pieces (classic Sokoban rules) and
  rotates them to align connectors.
- **Goal.** Energize a path of aligned connectors from the power **source** to
  the **receiver**.
- **Piece types**
  - **Straight / Elbow / Tee / Cross** — basic connectors; variety comes from
    rotation, not new sprites.
  - **Diode** — conducts one way only; rotate it to aim its flow arrow.
  - **Gate** — a locked connector that only conducts once a *separate secondary
    circuit* (its own source/receiver) is completed.
  - **Ice tiles** — a pushed piece slides across ice until it hits an obstacle.
- **Scoring.** Pushes and rotations count toward your move total; walking is
  free. Solving under par earns **gold / silver / bronze**.
- **Undo / redo**, a **move counter**, and **par** derived from an optimal solver.
- **Endless, by tier.** Easy / Medium / Hard each stream generated levels; a
  level's *seed is its index*, so levels are reproducible and shareable.
- **Text-free tutorials.** The first time a new piece appears, the board dims and
  an animated gesture points you at the cell to act on — no words, no
  localization needed.
- **Juice.** Eased movement, a tile-by-tile "energize" sweep on solve, an idle
  pulse on the live-but-incomplete chain, particle bursts, and a camera punch.

## Build & run

Requires **JDK 21**. Use the bundled Gradle wrapper (do **not** use a
system-wide Gradle — see [CLAUDE.md](CLAUDE.md)).

```bash
# Play (desktop, portrait window)
./gradlew :lwjgl3:run

# Pick a difficulty and/or a specific level seed
./gradlew :lwjgl3:run --args="--difficulty hard --seed 4"

# Run the tests (pure-logic, no rendering needed)
./gradlew :core:test
```

The app starts at the level-select menu; pick a tier and solve. Progress (best
moves, medals, current level per tier) persists between runs.

### Controls

| Action        | Desktop                          | Touch                       |
|---------------|----------------------------------|-----------------------------|
| Move / push   | Arrow keys or WASD               | Swipe                       |
| Rotate a piece| Click an adjacent piece (or `R`) | Tap a piece next to you     |
| Undo / redo   | `Z` / `Y`                        | —                           |
| Next level    | `Enter` (when solved)            | Tap (when solved)           |
| Back to menu  | `Esc`                            | Tap the **Menu** button     |

## Level generation

Levels are **solvable by construction**: generation starts from a fully solved
board and scrambles it with legal *reverse moves* (pulls + rotations). An
optimal breadth-first **solver** then validates each candidate and computes its
minimum move count, which becomes par and the difficulty rating. Diodes are
placed on the solution path, ice stays off it, and gates add a minimal secondary
circuit — all kept within a fast 5×5 / short-par envelope so generation is
instant.

## Project structure

Two Gradle modules. The `model`, `solver`, and most of `game` packages are
**pure Java (no libGDX)** and fully unit-tested; rendering/input sit on top.

```
core/                         # shared game module
  com/circuitsokoban/
    model/     Direction, Pos, Piece, PieceType, Board, Circuit, MoveResult, Terminal
    solver/    Solver (BFS), StateKey, LevelGenerator (reverse-gen), GenParams, Level
    game/      PlaySession, PlayController, Progress, Tier, Navigator, Lesson, Tutorials, Store
    render/    IsoProjector, BoardRenderer, BoardView (juice), Palette, Particle, TutorialOverlay
    screen/    MenuScreen, GameScreen
    input/     GameInput  (one InputProcessor for desktop + touch)
    CircuitSokobanGame.java  (Game entry / navigator)
lwjgl3/                       # desktop launcher (portrait 540×960)
assets/                       # runtime working dir (procedural art — no texture assets)
```

## Tech

- **libGDX 1.14.2**, LWJGL3 desktop backend
- **Java 21**, **Gradle 8.13** (via the wrapper)
- Art is drawn procedurally with `ShapeRenderer` (flat geometric shapes) — no
  sprite assets to manage.

## Status

Playable end to end: generation, solver, isometric rendering, input, the juice
layer, endless-by-tier level select with persistent medals, all three advanced
piece types (diode / gate / ice), and text-free tutorials.

Not yet built: the **Android launcher module** (desktop-first; the Android SDK
side is wired but the module is intentionally deferred), a **hint** system, and
the stretch **fragile / one-use connector**.
