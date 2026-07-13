package com.circuitsokoban.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.circuitsokoban.model.Board;
import com.circuitsokoban.model.Circuit;
import com.circuitsokoban.model.Direction;
import com.circuitsokoban.model.Piece;
import com.circuitsokoban.model.PieceType;
import com.circuitsokoban.model.Pos;
import com.circuitsokoban.model.Terminal;
import com.circuitsokoban.render.BoardRenderer;
import com.circuitsokoban.render.BoardView;
import com.circuitsokoban.render.IsoProjector;
import com.circuitsokoban.render.Palette;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Dev tool: renders each piece/tile to its own small PNG icon for the README,
 * using the real {@link BoardRenderer} so the legend always matches the game.
 *
 * <p>Each icon puts its subject on the centre cell of a 5x5 board (terminals
 * parked in the corners, off-crop) and captures a fixed box around screen centre.
 * Run via {@code --legend <dir>}.
 */
public final class LegendScreen extends ScreenAdapter {

    private static final float WORLD_W = 540f;
    private static final float WORLD_H = 960f;
    private static final float TILE_WIDTH = 122f;
    private static final int CROP_W = 168;
    private static final int CROP_H = 150;
    private static final int CROP_UP = 8; // subject sits a touch above centre (arms/player lift)

    private final Game game;
    private final String outDir;
    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final ShapeRenderer shapes;
    private final IsoProjector iso;
    private final BoardRenderer renderer;
    private final BoardView view;
    private final List<Item> items = new ArrayList<>();
    private int index;
    private int frame;

    private record Item(String name, Board board) {}

    public LegendScreen(Game game, String outDir) {
        this.game = game;
        this.outDir = outDir;
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(WORLD_W, WORLD_H, camera);
        this.shapes = new ShapeRenderer();
        this.iso = new IsoProjector(TILE_WIDTH);
        iso.setBoardCenter(5, 5, WORLD_W / 2f, WORLD_H / 2f); // centre cell (2,2) -> screen centre
        this.renderer = new BoardRenderer(iso);
        this.view = new BoardView(iso);
        buildItems();
    }

    private static final Pos CENTER = new Pos(2, 2);

    /** 5x5 board with terminals + player parked in the corners, out of the icon crop. */
    private Board base() {
        Terminal source = new Terminal(new Pos(0, 0), Direction.EAST);
        Terminal receiver = new Terminal(new Pos(4, 4), Direction.WEST);
        return new Board(5, 5, source, receiver, new Pos(0, 4));
    }

    private Board piece(PieceType type, int orientation) {
        Board b = base();
        b.setPiece(CENTER, new Piece(type, orientation));
        return b;
    }

    private void buildItems() {
        items.add(new Item("floor", base()));

        // Terminals: put the subject on the centre cell.
        Board src = new Board(5, 5, new Terminal(CENTER, Direction.EAST),
                new Terminal(new Pos(4, 4), Direction.WEST), new Pos(0, 4));
        items.add(new Item("source", src));
        Board rcv = new Board(5, 5, new Terminal(new Pos(0, 0), Direction.EAST),
                new Terminal(CENTER, Direction.WEST), new Pos(0, 4));
        items.add(new Item("receiver", rcv));

        Board s2 = base();
        s2.setSecondary(new Terminal(CENTER, Direction.EAST), new Terminal(new Pos(4, 0), Direction.WEST));
        items.add(new Item("source2", s2));
        Board r2 = base();
        r2.setSecondary(new Terminal(new Pos(0, 2), Direction.EAST), new Terminal(CENTER, Direction.WEST));
        items.add(new Item("receiver2", r2));

        Board player = base();
        player.setPlayer(CENTER);
        items.add(new Item("player", player));

        items.add(new Item("straight", piece(PieceType.STRAIGHT, 1))); // E|W
        items.add(new Item("elbow", piece(PieceType.ELBOW, 0)));
        items.add(new Item("tee", piece(PieceType.TEE, 0)));
        items.add(new Item("cross", piece(PieceType.CROSS, 0)));
        items.add(new Item("diode", piece(PieceType.DIODE, 1)));      // flow east
        items.add(new Item("fuse", piece(PieceType.FUSE, 1)));

        items.add(new Item("gate-locked", piece(PieceType.GATE, 1)));
        Board gateOpen = piece(PieceType.GATE, 1);
        gateOpen.setGateLatched(true); // renders open (doors retracted)
        items.add(new Item("gate-open", gateOpen));

        Board ice = base();
        ice.setIce(CENTER, true);
        items.add(new Item("ice", ice));

        // Energized example: source -> straight -> receiver, so the straight lights green.
        Board lit = new Board(5, 5, new Terminal(new Pos(1, 2), Direction.EAST),
                new Terminal(new Pos(3, 2), Direction.WEST), new Pos(0, 4));
        lit.setPiece(CENTER, new Piece(PieceType.STRAIGHT, 1));
        items.add(new Item("energized", lit));
    }

    @Override
    public void render(float delta) {
        if (index >= items.size()) {
            game.getScreen().dispose();
            Gdx.app.exit();
            return;
        }
        Item it = items.get(index);
        view.syncTo(it.board, Circuit.evaluate(it.board));

        ScreenUtils.clear(Palette.BACKGROUND);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        camera.update();
        shapes.setProjectionMatrix(camera.combined);
        renderer.render(shapes, it.board, view);

        frame++;
        if (frame < 2) {
            return; // warm up GL before the first capture
        }
        saveIcon(it.name());
        index++;
    }

    private void saveIcon(String name) {
        int w = Gdx.graphics.getBackBufferWidth();
        int h = Gdx.graphics.getBackBufferHeight();
        Pixmap full = Pixmap.createFromFrameBuffer(0, 0, w, h);
        flipVertically(full, w, h); // framebuffer is bottom-up

        int cx = (w - CROP_W) / 2;
        int cy = (h - CROP_H) / 2 - CROP_UP;
        Pixmap crop = new Pixmap(CROP_W, CROP_H, Pixmap.Format.RGBA8888);
        crop.drawPixmap(full, 0, 0, cx, cy, CROP_W, CROP_H);
        PixmapIO.writePNG(Gdx.files.absolute(outDir + "/" + name + ".png"), crop);
        crop.dispose();
        full.dispose();
        Gdx.app.log("Legend", "Wrote " + name + ".png");
    }

    private static void flipVertically(Pixmap pixmap, int w, int h) {
        ByteBuffer pixels = pixmap.getPixels();
        int stride = w * 4;
        byte[] flipped = new byte[stride * h];
        for (int row = 0; row < h; row++) {
            pixels.position((h - row - 1) * stride);
            pixels.get(flipped, row * stride, stride);
        }
        pixels.clear();
        pixels.put(flipped);
        pixels.clear();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        shapes.dispose();
    }
}
