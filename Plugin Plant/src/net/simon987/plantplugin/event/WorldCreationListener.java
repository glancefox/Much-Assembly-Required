package net.simon987.plantplugin.event;

import net.simon987.plantplugin.Plant;
import net.simon987.server.GameServer;
import net.simon987.server.event.GameEvent;
import net.simon987.server.event.GameEventListener;
import net.simon987.server.event.WorldGenerationEvent;
import net.simon987.server.game.World;
import net.simon987.server.game.WorldGenerator;
import net.simon987.server.logging.LogManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.Random;

public class WorldCreationListener implements GameEventListener {
    @Override
    public Class getListenedEventType() {
        return WorldGenerationEvent.class;
    }

    @Override
    public void handle(GameEvent event) {

        ArrayList<Plant> plants = generatePlants(((WorldGenerationEvent)event).getWorld());

        ((WorldGenerationEvent)event).getWorld().getGameObjects().addAll(plants);

    }

    /**
     * Generate a list of plants for a world
     */
    public ArrayList<Plant> generatePlants(World world) {

        int minTreeCount = GameServer.INSTANCE.getConfig().getInt("minTreeCount");
        int maxTreeCount = GameServer.INSTANCE.getConfig().getInt("maxTreeCount");
        int plant_yield = GameServer.INSTANCE.getConfig().getInt("plant_yield");

        Random random = new Random();
        int treeCount = random.nextInt(maxTreeCount - minTreeCount) + minTreeCount;
        ArrayList<Plant> plants = new ArrayList<>(maxTreeCount);

        //Count number of plain tiles. If there is less plain tiles than desired amount of trees,
        //set the desired amount of trees to the plain tile count
        int[][] tiles = world.getTileMap().getTiles();
        int plainCount = 0;
        for (int y = 0; y < World.WORLD_SIZE; y++) {
            for (int x = 0; x < World.WORLD_SIZE; x++) {

                if (tiles[x][y] == 0) {
                    plainCount++;
                }
            }
        }

        if (treeCount > plainCount) {
            treeCount = plainCount;
        }

        outerLoop:
        for (int i = 0; i < treeCount; i++) {

            Point p = WorldGenerator.getRandomPlainTile(world.getTileMap().getTiles());

            if (p != null) {

                for (Plant plant : plants) {
                    if (plant.getX() == p.x && plant.getY() == p.y) {
                        //There is already a plant here
                        continue outerLoop;
                    }
                }

                Plant plant = new Plant();
                plant.setObjectId(GameServer.INSTANCE.getGameUniverse().getNextObjectId());
                plant.setStyle(0); //TODO: set style depending on difficulty level? or random? from config?
                plant.setBiomassCount(plant_yield);
                plant.setCreationTime(0); // Plants generated by the world generator always have creationTime of 0
                plant.setX(p.x);
                plant.setY(p.y);
                plant.setWorld(world);

                plants.add(plant);
            }
        }

        LogManager.LOGGER.info("Generated " + plants.size() + " plants for World (" + world.getX() + ',' +
                world.getY() + ')');

        return plants;
    }
}
