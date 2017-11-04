package net.simon987.server.game;

import net.simon987.server.game.pathfinding.Pathfinder;
import net.simon987.server.io.JSONSerialisable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.awt.*;
import java.util.ArrayList;
import java.util.Random;

public class World implements JSONSerialisable{

    /**
     * Size of the side of a world
     */
    public static final int WORLD_SIZE = 16;

    private static final char INFO_BLOCKED = 0x8000;
    private static final char INFO_IRON = 0x0200;
    private static final char INFO_COPPER = 0x0100;

    private int x;
    private int y;

    private TileMap tileMap;

    private ArrayList<GameObject> gameObjects = new ArrayList<>(16);

    public World(int x, int y, TileMap tileMap) {
        this.x = x;
        this.y = y;
        this.tileMap = tileMap;
    }

    private World(){

    }

    public TileMap getTileMap() {
        return tileMap;
    }

    /**
     * Check if a tile is blocked, either by a game object or an impassable tile type
     */
    public boolean isTileBlocked(int x, int y) {

        return getGameObjectsAt(x, y).size() > 0 || tileMap.getTileAt(x, y) == TileMap.WALL_TILE;

    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public ArrayList<GameObject> getGameObjects() {
        return gameObjects;
    }

    public void update(){

        ArrayList<GameObject> gameObjects_ = new ArrayList<>(gameObjects);

        for(GameObject object : gameObjects_){
            if(object instanceof Updatable){
                ((Updatable) object).update();
            }
            if(object.isDead()){
                System.out.println("Removed" + object.getObjectId());
                gameObjects.remove(object);
            }
        }
    }

    @Override
    public JSONObject serialise() {
        JSONObject json = new JSONObject();

        JSONArray objects = new JSONArray();
        for(GameObject obj : gameObjects){
            objects.add(obj.serialise());
        }
        json.put("objects", objects);

        json.put("terrain", tileMap.serialise());

        json.put("x", x);
        json.put("y", y);

        return json;
    }

    @Override
    public String toString() {

        String str = "World (" + x + ", " + y + ")\n";
        int[][] tileMap = this.tileMap.getTiles();

        for (int x = 0; x < WORLD_SIZE; x++) {
            for (int y = 0; y < WORLD_SIZE; y++) {
                str += tileMap[x][y] + " ";
            }
            str += "\n";
        }

        return str;

    }

    public static World deserialize(JSONObject json) {
        World world = new World();
        world.x = (int)(long)json.get("x");
        world.y = (int)(long)json.get("y");

        world.tileMap = TileMap.deserialize((JSONObject)json.get("terrain"));

        for(JSONObject objJson : (ArrayList<JSONObject>)json.get("objects")){

            GameObject object = GameObject.deserialize(objJson);

            object.setWorld(world);
            world.gameObjects.add(object);
        }

        return world;
    }

    /**
     * Get a binary representation of the map as an array of 16-bit bit fields, one word for each
     * tile.
     * <p>
     * todo Performance cache this?
     */
    public char[][] getMapInfo() {

        char[][] mapInfo = new char[World.WORLD_SIZE][World.WORLD_SIZE];
        int[][] tiles = tileMap.getTiles();

        //Tile
        for (int y = 0; y < World.WORLD_SIZE; y++) {
            for (int x = 0; x < World.WORLD_SIZE; x++) {


                if (tiles[y][x] == TileMap.PLAIN_TILE) {

                    mapInfo[x][y] = 0;

                } else if (tiles[y][x] == TileMap.WALL_TILE) {

                    mapInfo[x][y] = INFO_BLOCKED;
                } else if (tiles[y][x] == TileMap.COPPER_TILE) {
                    mapInfo[x][y] = INFO_COPPER;
                } else if (tiles[y][x] == TileMap.IRON_TILE) {
                    mapInfo[x][y] = INFO_IRON;
                }
            }
        }

        //Objects
        for (GameObject obj : this.gameObjects) {
            mapInfo[obj.getX()][obj.getY()] |= obj.getMapInfo();

        }

        return mapInfo;

    }

    /**
     * Get a random tile that is empty and passable
     * The function ensures that a object spawned there will not be trapped
     * and will be able to leave the World
     * <br>
     * Note: This function is quite expensive and shouldn't be used
     * by some CpuHardware in its current state
     *
     * @return random non-blocked tile
     */
    public Point getRandomPassableTile(){
        Random random = new Random();

        int counter = 0;
        while (true) {
            counter++;

            //Prevent infinite loop
            if (counter >= 2500) {
                return null;
            }

            int rx = random.nextInt(World.WORLD_SIZE);
            int ry = random.nextInt(World.WORLD_SIZE);

            if(!isTileBlocked(rx, ry)){

                Object path = Pathfinder.findPath(this, rx, ry, 1,1,0);

                if(path != null) {
                    return new Point(rx, ry);
                }
            }
        }
    }

    /**
     * Get the list of game objects at a location
     *
     * @param x X coordinate on the World
     * @param y Y coordinate on the World
     * @return the list of game objects at a location
     */
    public ArrayList<GameObject> getGameObjectsAt(int x, int y) {

        ArrayList<GameObject> gameObjects = new ArrayList<>(2);

        for (GameObject obj : this.gameObjects) {

            if (obj.isAt(x, y)) {
                gameObjects.add(obj);
            }

        }

        return gameObjects;
    }

}