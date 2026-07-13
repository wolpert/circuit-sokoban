package com.circuitsokoban.solver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.circuitsokoban.model.Board;
import com.circuitsokoban.model.Circuit;
import com.circuitsokoban.model.Piece;
import com.circuitsokoban.model.Pos;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class LevelGeneratorTest {

    private final LevelGenerator generator = new LevelGenerator();

    /** Exact identity of a board: pieces + player + terminals (for determinism checks). */
    private static String fingerprint(Board b) {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < b.height(); y++) {
            for (int x = 0; x < b.width(); x++) {
                Piece p = b.pieceAt(new Pos(x, y));
                sb.append(p == null ? "." : p.type().name().charAt(0) + "" + p.orientation());
                sb.append('|');
            }
        }
        sb.append("player=").append(b.player());
        sb.append("src=").append(b.source()).append("rec=").append(b.receiver());
        return sb.toString();
    }

    @Test
    void everyEasyLevelIsSolvableWithParInWindow() {
        GenParams p = GenParams.easy();
        IntStream.range(0, 40).forEach(seed -> {
            Level level = generator.generate(seed, p);
            // Independently re-solve the emitted board (not trusting the cached par).
            Solver.Result check = new Solver(p.solverMaxStates()).solve(level.freshBoard());
            assertTrue(check.solvable(), "seed " + seed + " must be solvable");
            assertEquals(level.par(), check.moves(), "cached par must match a fresh solve, seed " + seed);
            assertTrue(level.par() >= p.minPar() && level.par() <= p.maxPar(),
                    "seed " + seed + " par " + level.par() + " outside window");
        });
    }

    @Test
    void startBoardIsScrambledNotAlreadySolved() {
        GenParams p = GenParams.easy();
        IntStream.range(0, 20).forEach(seed -> {
            Level level = generator.generate(seed, p);
            assertFalse(Circuit.isSolved(level.startBoard()),
                    "seed " + seed + " should not start solved");
        });
    }

    @Test
    void solvedTemplateActuallySolvesTheCircuit() {
        IntStream.range(0, 20).forEach(seed -> {
            Level level = generator.generate(seed, GenParams.easy());
            assertTrue(Circuit.isSolved(level.solvedBoard()),
                    "seed " + seed + " solved template must complete the circuit");
        });
    }

    @Test
    void generationIsDeterministicPerSeed() {
        Level a = generator.generate(12345L, GenParams.medium());
        Level b = generator.generate(12345L, GenParams.medium());
        assertEquals(a.par(), b.par());
        assertEquals(fingerprint(a.startBoard()), fingerprint(b.startBoard()));
    }

    @Test
    void mediumLevelsSolveWithinWindow() {
        GenParams p = GenParams.medium();
        IntStream.range(0, 12).forEach(seed -> {
            Level level = generator.generate(seed, p);
            assertTrue(level.par() >= p.minPar() && level.par() <= p.maxPar(),
                    "medium seed " + seed + " par " + level.par());
            assertTrue(Circuit.isSolved(level.solvedBoard()));
        });
    }

    private static boolean containsDiode(Board b) {
        for (int y = 0; y < b.height(); y++) {
            for (int x = 0; x < b.width(); x++) {
                Piece p = b.pieceAt(new Pos(x, y));
                if (p != null && p.type() == com.circuitsokoban.model.PieceType.DIODE) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    void diodeLevelsStaySolvableAndSometimesContainDiodes() {
        GenParams p = GenParams.medium(); // diodesOnPath == 1
        boolean anyDiode = false;
        for (int seed = 0; seed < 16; seed++) {
            Level level = generator.generate(seed, p);
            Solver.Result check = new Solver(p.solverMaxStates()).solve(level.freshBoard());
            assertTrue(check.solvable(), "diode seed " + seed + " must be solvable");
            assertEquals(level.par(), check.moves(), "cached par must match, seed " + seed);
            anyDiode |= containsDiode(level.solvedBoard());
        }
        assertTrue(anyDiode, "diodesOnPath=1 should place a diode on at least one of these levels");
    }

    @Test
    void iceLevelsStaySolvable() {
        // Ice-focused params: 3 slide tiles, no diode/gate.
        GenParams p = new GenParams(5, 5, 10, 4, 8, 0.5, 0, 0, 3, 0, false, 120_000, 400);
        for (int seed = 0; seed < 10; seed++) {
            Level level = generator.generate(seed, p);
            Solver.Result check = new Solver(p.solverMaxStates()).solve(level.freshBoard());
            assertTrue(check.solvable(), "iced seed " + seed + " must be solvable");
            assertEquals(level.par(), check.moves(), "cached par must match, seed " + seed);
        }
    }

    @Test
    void gateLevelsHaveASecondaryCircuitAndStaySolvable() {
        GenParams p = GenParams.hard(); // gateCount == 1
        for (int seed = 0; seed < 12; seed++) {
            Level level = generator.generate(seed, p);
            Board b = level.freshBoard();
            assertTrue(b.hasSecondary(), "gate seed " + seed + " must have a secondary circuit");
            boolean gate = false;
            for (int y = 0; y < b.height(); y++) {
                for (int x = 0; x < b.width(); x++) {
                    Piece pc = b.pieceAt(new Pos(x, y));
                    if (pc != null && pc.type() == com.circuitsokoban.model.PieceType.GATE) {
                        gate = true;
                    }
                }
            }
            assertTrue(gate, "gate seed " + seed + " must contain a gate");
            Solver.Result check = new Solver(p.solverMaxStates()).solve(b);
            assertTrue(check.solvable(), "gate seed " + seed + " must be solvable");
            assertEquals(level.par(), check.moves(), "cached par must match, seed " + seed);
        }
    }

    @Test
    void hardLevelsHaveAFuseSecondaryAndStaySolvable() {
        GenParams p = GenParams.hard(); // fragileSecondary == true
        for (int seed = 0; seed < 12; seed++) {
            Level level = generator.generate(seed, p);
            Board b = level.freshBoard();
            boolean fuse = false;
            for (int y = 0; y < b.height(); y++) {
                for (int x = 0; x < b.width(); x++) {
                    Piece pc = b.pieceAt(new Pos(x, y));
                    if (pc != null && pc.type() == com.circuitsokoban.model.PieceType.FUSE) {
                        fuse = true;
                    }
                }
            }
            assertTrue(fuse, "hard seed " + seed + " must contain a fuse");
            Solver.Result check = new Solver(p.solverMaxStates()).solve(b);
            assertTrue(check.solvable(), "fuse seed " + seed + " must be solvable");
            assertEquals(level.par(), check.moves(), "cached par must match, seed " + seed);
        }
    }

    @Test
    void hardLevelGeneratesAndIsSolvable() {
        Level level = generator.generate(7L, GenParams.hard());
        Solver.Result check = new Solver().solve(level.freshBoard());
        assertTrue(check.solvable());
        assertEquals(level.par(), check.moves());
        assertTrue(level.difficulty() >= 1 && level.difficulty() <= 5);
    }
}
