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
- **Pieces & tiles.** Basic connectors plus one-way diodes, locked gates, one-use
  fuses, and ice — see the [reference table](#pieces--tiles) below for what each
  does, how it looks, and its states.
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

## Pieces & tiles

The art is flat and abstract (drawn procedurally, no sprite assets). Connectors
are **arms radiating from a round joint** toward their open sides: the **shape**
tells you the type, the **colour** tells you the power state —

- **Idle** (aligned but unpowered): grey arms.
- **Energized** (carrying power from a source): green arms — gently pulsing while
  the circuit is live but incomplete, steady once solved.

| Element | What it does | How it looks · states |
|---|---|---|
| **Floor** | Ordinary tile you stand on and push pieces across. | Dark blue-grey diamonds in a subtle checker. |
| **Power source** | Emits power; the start of the circuit. | Amber diamond with a short connector stub toward its opening. |
| **Receiver** | The goal — power must reach it. | Cyan diamond with a stub. |
| **Secondary source / receiver** | Endpoints of the extra circuit that unlocks gates. | Violet (source) and teal (receiver) diamonds with stubs. |
| **Player** | The token you move; walks freely and pushes pieces. | A pink-red round token. |
| **Straight** | Conducts along one axis. | Two arms on opposite sides. |
| **Elbow** | Conducts around a corner. | Two arms on adjacent sides (an L). |
| **Tee** | A three-way junction. | Three arms. |
| **Cross** | Conducts all four ways. | Four arms (rotating it changes nothing). |
| **Diode** | Conducts **one way only**; rotate to aim it. | A straight with a dark arrowhead on one arm — current flows only in the arrow's direction. |
| **Gate** | Blocks the path until its secondary circuit completes, then **latches** open. | **Locked:** a solid red bar across the wire. **Open:** the bar retracts to green nubs at the tile edges and the wire runs through — it slides open the moment it unlocks. |
| **Fuse** | A **one-use** key on a secondary circuit: completing that circuit latches the gate, and the fuse is spent in the same instant. | Coral wire with a bright crack (✕) mark. **Burned:** removed from the board with a shatter burst. |
| **Ice tile** | A pushed piece that lands on it **slides** until it hits an obstacle. | A frosted steel-blue diamond with a bright frost-crystal mark; a piece sliding across leaves a faint fading trail. |

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
layer, endless-by-tier level select with persistent medals, all four advanced
piece types (diode / gate / ice / one-use fuse), and text-free tutorials.

Not yet built: the **Android launcher module** (desktop-first; the Android SDK
side is wired but the module is intentionally deferred) and a **hint** system.
The HUD still uses a few English words (only the tutorial is deliberately
text-free).
