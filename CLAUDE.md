# CLAUDE.md

Guidance for working in this repo. Circuit Sokoban is an isometric Sokoban-style
circuit puzzle (libGDX, Java 21). See [README.md](README.md) for the player-facing
overview.

## Commands

Always use the **wrapper** (`./gradlew`), never a system Gradle.

```bash
./gradlew :core:test            # run all unit tests (pure logic, fast)
./gradlew :core:test --tests 'com.circuitsokoban.solver.SolverTest'   # one class
./gradlew :lwjgl3:run           # play on desktop (portrait window)
./gradlew :lwjgl3:run --args="--difficulty hard --seed 4"
```

### Headless visual verification

There's a live display, so a plain `run` opens a window on the user's screen. To
check rendering **without hijacking their display**, use screenshot mode under
Xvfb — the launcher renders a few frames, writes a PNG, and exits. Then Read the
PNG.

```bash
xvfb-run -a -s "-screen 0 540x960x24" ./gradlew :lwjgl3:run \
  --args="--screenshot /abs/out.png --shotdelay 0.15 --difficulty medium --seed 3"
```

Launcher flags: `--seed <long>`, `--difficulty easy|medium|hard`,
`--screenshot <abs path>`, `--shotdelay <seconds>` (time-based capture, since
headless FPS varies), `--menu` (capture the level-select), and `--debug`:
`kick-rotate` / `kick-push` / `solved` (animation triggers) and
`tutorial-basics|diode|ice|gate` (force a tutorial overlay). The framebuffer is
captured bottom-up and flipped before writing — the live game renders upright.

## Architecture

Two Gradle modules: `core` (all game code) and `lwjgl3` (desktop launcher). The
**android module is intentionally NOT in `settings.gradle`** yet.

The critical rule is the dependency boundary:

- **`model/`, `solver/`, and most of `game/` are pure Java — no libGDX imports.**
  This is what makes the entire game logic + solver + generation unit-testable
  with plain JUnit. Do not add libGDX imports there.
- `render/`, `screen/`, `input/`, and `CircuitSokobanGame` use libGDX.

Logic is authoritative and instantaneous; the view eases toward it:

- `game/PlaySession` — authoritative board state, move counter (push/rotate = 1,
  walk = 0), undo/redo, cached circuit + solved status.
- `render/BoardView` — the **juice layer**: visual state that tweens toward the
  logic (slides, rotation, energize sweep, particles, camera punch). It can never
  affect correctness.
- `game/PlayController` — glue that applies a move to `PlaySession` then tells
  `BoardView` how to animate it. Input talks only to this.

The logical grid is **orthogonal**; isometric is purely a render transform
(`render/IsoProjector` is the only place that knows about iso).

## Key facts & gotchas

- **Gradle:** wrapper pinned to **8.13**; the user's system Gradle is 9.x, which
  is incompatible with the (future) Android Gradle Plugin. Build with `./gradlew`.
- **Android SDK** is installed at `~/Android/Sdk` (platforms to android-37). When
  wiring the android module, that's the AGP/Gradle-version-sensitive part.
- **Solver perf envelope:** par comes from an *optimal* BFS whose cost explodes
  with board size and solution depth. It's only fast on **5×5, par ≤ 8–9**. Do
  NOT raise `GenParams` board size or `maxPar` without first making the solver
  faster (bidirectional BFS / IDA*) — generation will hang.
- **Reverse generation** guarantees solvability by construction (scramble a
  solved board with pull/rotate reverse-moves); the solver then re-validates and
  sets par. `LevelGenerator` rejects degenerate/too-hard results and retries.
- **Directed connectivity:** `Circuit` flows power A→B iff A outputs toward B and
  B inputs from A. Normal pieces have `inputs()==outputs()==openings()`; diodes
  are one-way; gates conduct only when the secondary circuit is complete (two-pass
  evaluation). `StateKey` encodes `(outputs<<4)|inputs` plus gate/fuse bits and a
  latch bit, and normalizes the player to its min reachable cell (walking is free).
- **Stateful resolution (gates/fuses):** `Circuit.evaluate` is pure, but
  `Circuit.resolve(board)` **mutates** — it latches gates open once the secondary
  completes (`Board.gateLatched`) and removes energized FUSE pieces. It runs after
  loading a board and after every move, in **both** `PlaySession` and
  `Solver.successors`, so solver par matches play. Generation rejects starts whose
  secondary is already complete (else a fuse would auto-burn on load).
- **Scoring/solver coupling:** because walking is free, the solver searches over
  "meaningful moves" (pushes + rotates) using player reachability, not walk steps.

## Extension seams

- **New advanced piece type:** touch `PieceType`, `Circuit` (its conduction rule,
  and `resolve` if it changes state like a fuse/gate), `StateKey` (if its state
  isn't captured by in/out masks — e.g. the gate/fuse bits or the latch),
  `BoardRenderer`, and `LevelGenerator` (placement) + `GenParams`. See how
  DIODE/GATE/ICE/FUSE were added (git log). Keep it inside the 5×5/par envelope.
- **New tutorial (text-free):** exactly three spots — add a `game/Lesson` value, a
  rule in `game/Tutorials.firstUnseen`, and a `case` in `render/TutorialOverlay`.
  The tutorial must stay text-free (i18n avoidance).

## Testing

- JUnit 5. `model` / `solver` / `game` tests are pure and fast.
- Some UI classes are testable without a GL context (`IsoProjector`, `GameInput`,
  `Progress`, `Tutorials`) — but **avoid** calling `Viewport.update()` or
  constructing `SpriteBatch`/`Texture` in tests; those need real GL and throw.
- Add a test with each logic change; the solver/generation tests double as an
  end-to-end check that every generated level is solvable with an honest par.

## Conventions

- Commits are scoped per increment/feature with a short body explaining the why;
  end with the project's `Co-Authored-By` trailer.
- Prefer lightweight, readable implementations over clever ones — this is a hobby
  project the owner wants to understand and extend.
