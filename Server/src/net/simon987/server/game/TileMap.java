package net.simon987.server.game;


import net.simon987.server.io.JSONSerialisable;
import org.json.simple.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;

/**
 * A 2D map of Tile objects of size width*height
 */
public class TileMap implements JSONSerialisable {

    public static final int PLAIN_TILE = 0;
    public static final int WALL_TILE = 1;
    public static final int IRON_TILE = 2;
    public static final int COPPER_TILE = 3;

    public static final int ITEM_IRON = 3;
    public static final int ITEM_COPPER = 4;

    /**
     * The map of tile
     */
    private int[][] tiles;

    /**
     * width, in tiles
     */
    private int width;

    /**
     * Height, in tiles
     */
    private int height;

    /**
     * Create a blank (All 0s) map
     */
    public TileMap(int width, int height) {
        this.width = width;
        this.height = height;

        tiles = new int[width][height];
    }

    /**
     * Change the tile at a specified position
     * Sets the modified flag
     *
     * @param tileId id of the new Tile
     * @param x      X coordinate of the tile to set
     * @param y      Y coordinate of the tile to set
     */
    public void setTileAt(int tileId, int x, int y) {

        try {
            tiles[x][y] = tileId;
        } catch (ArrayIndexOutOfBoundsException e) {
            //Shouldn't happen
            e.printStackTrace();
        }
    }

    /**
     * Get the tile at a specific position
     *
     * @param x X coordinate of the tile to get
     * @param y Y coordinate of the tile to get
     * @return the tile at the specified position, -1 if out of bounds
     */
    public int getTileAt(int x, int y) {
        try {
            return tiles[x][y];
        } catch (ArrayIndexOutOfBoundsException e) {
            return -1;
        }
    }

    public int[][] getTiles() {
        return tiles;
    }

    public int getWidth() {

        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public JSONObject serialise() {
        JSONObject json = new JSONObject();

        byte[] terrain = new byte[width*width];

        for (int x = 0; x < World.WORLD_SIZE; x++) {
            for (int y = 0; y < World.WORLD_SIZE; y++) {
                terrain[x * width + y] = (byte)tiles[x][y];
            }
        }
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            Deflater compressor = new Deflater(Deflater.BEST_COMPRESSION, true);
            DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(stream, compressor);

            deflaterOutputStream.write(terrain);

            deflaterOutputStream.close();
            byte[] compressedBytes = stream.toByteArray();

            json.put("zipTerrain", new String(Base64.getEncoder().encode(compressedBytes)));

        } catch (IOException e) {
            e.printStackTrace();
        }

        return json;
    }

    public static TileMap deserialize(JSONObject object) {

        TileMap tileMap = new TileMap(World.WORLD_SIZE, World.WORLD_SIZE);


        byte[] compressedBytes = Base64.getDecoder().decode((String)object.get("zipTerrain"));

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Inflater decompressor = new Inflater(true);
            InflaterOutputStream inflaterOutputStream = new InflaterOutputStream(baos, decompressor);
            inflaterOutputStream.write(compressedBytes);
            inflaterOutputStream.close();

            byte[] terrain = baos.toByteArray();

            for (int x = 0; x < World.WORLD_SIZE; x++) {
                for (int y = 0; y < World.WORLD_SIZE; y++) {
                    tileMap.tiles[x][y] = terrain[x * World.WORLD_SIZE + y];
                }
            }

            return tileMap;


        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
