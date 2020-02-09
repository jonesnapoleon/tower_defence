package za.co.entelect.challenge;

import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.BuildingType;
import za.co.entelect.challenge.enums.PlayerType;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static za.co.entelect.challenge.enums.BuildingType.ATTACK;
import static za.co.entelect.challenge.enums.BuildingType.DEFENSE;

public class Bot {
    private static final String NOTHING_COMMAND = "";
    private GameState gameState;
    private GameDetails gameDetails;
    private int gameWidth;
    private int gameHeight;
    private Player myself;
    private Player opponent;
    private List<Building> buildings;
    private List<Missile> missiles;

    /**
     * Constructor
     *
     * @param gameState the game state
     **/
    public Bot(GameState gameState) {
        this.gameState = gameState;
        gameDetails = gameState.getGameDetails();
        gameWidth = gameDetails.mapWidth;
        gameHeight = gameDetails.mapHeight;
        myself = gameState.getPlayers().stream().filter(p -> p.playerType == PlayerType.A).findFirst().get();
        opponent = gameState.getPlayers().stream().filter(p -> p.playerType == PlayerType.B).findFirst().get();
        buildings = gameState.getGameMap().stream().flatMap(c -> c.getBuildings().stream()).collect(Collectors.toList());
        missiles = gameState.getGameMap().stream().flatMap(c -> c.getMissiles().stream()).collect(Collectors.toList());
    }

    /**
     * Run
     *
     * @return the result
     **/
    public String run() {
        String command = "";
        
        // If there are defense building and attack building in a row, then construct energy building
        // for(int i = 0; i < gameHeight; i ++){
        //     if(!isCellEmpty(gameWidth / 2, i) && doesRowHasBuilding(myself, BuildingType.ATTACK, i) && canAffordBuilding(BuildingType.ENERGY) && isCellEmpty(0, i)){
        //         command = BuildingType.ENERGY.buildCommand(0, i);
        //     }
        //     if(isCellEmpty(0, i) && !doesRowHasBuilding(opponent, BuildingType.ATTACK, i)){
        //         command = BuildingType.ENERGY.buildCommand(0, i);
        //     }
        // }

        // If there is a defense building in one my the row, then start construct attack building behind it
        for(int i = 0; i < gameHeight; i ++){
            if(!isCellEmpty(gameWidth / 2, i)){
                for(int j = gameWidth / 2 - 1 - i; j >= 0; j --){
                    if(isCellEmpty(j, i) && canAffordBuilding(buildingType.ATTACK)){
                        command = BuildingType.ATTACK.buildCommand(j, i);
                    }
                    else {
                        command = "";
                    }
                }
            }
        }

        // If the enemy has an attack building of one and I don't have a blocking wall, then block from the front.
        // for (int i = 0; i < gameHeight; i++) {
        //     int enemyAttackOnRow = getAllBuildingsForPlayer(opponent, b -> b.buildingType == BuildingType.ATTACK, i).size();
        //     int myDefenseOnRow = getAllBuildingsForPlayer(myself, b -> b.buildingType == BuildingType.DEFENSE, i).size();
        //     if (enemyAttackOnRow < 3  && enemyAttackOnRow > 0 && enemyDefenseOnRow == 0) {
        //         if (canAffordBuilding(BuildingType.DEFENSE))
        //             command = BuildingType.DEFENSE.buildCommand((gameWidth / 2) - 1, i);
        //         else
        //             command = "";
        //         break;
        //     }
        // }

        //If I have a defense building on a row, then build an attack building behind it.
        for (int i = 0; i < gameHeight; i++) {
            if (getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.DEFENSE, i).size() > 0
                    && canAffordBuilding(BuildingType.ATTACK)) {
                command = placeBuildingInRowFromFront(BuildingType.ATTACK, i);
            }
        }

        // If there is a row where there is no enemy attack building, then build attack building starting from the second column to gameWidth / 2 - 1.
        if (command.equals("")) {
            for (int i = 0; i < gameHeight; i++) {
                int enemyAttackOnRow = getAllBuildingsForPlayer(opponent, b -> b.buildingType == BuildingType.ATTACK, i).size();
                if (enemyAttackOnRow == 0) {
                    if (canAffordBuilding(BuildingType.ATTACK))
                        command = placeBuildingInRowForAttack(BuildingType.ATTACK, i);
                    break;
                }
            }
        }
        
        // if (isUnderAttack()) {
        //     return defendRow();
        // } else if (hasEnoughEnergyForMostExpensiveBuilding()) {
        //     return buildRandom();
        // } else {
        //     return doNothingCommand();
        // }
    }

    /**
     * Build random building
     *
     * @return the result
     **/
    private String buildRandom() {
        List<CellStateContainer> emptyCells = gameState.getGameMap().stream()
                .filter(c -> c.getBuildings().size() == 0 && c.x < (gameWidth / 2))
                .collect(Collectors.toList());

        if (emptyCells.isEmpty()) {
            return doNothingCommand();
        }

        CellStateContainer randomEmptyCell = getRandomElementOfList(emptyCells);
        BuildingType randomBuildingType = getRandomElementOfList(Arrays.asList(BuildingType.values()));

        if (!canAffordBuilding(randomBuildingType)) {
            return doNothingCommand();
        }

        return randomBuildingType.buildCommand(randomEmptyCell.x, randomEmptyCell.y);
    }

    /**
     * Has enough energy for most expensive building
     *
     * @return the result
     **/
    private boolean hasEnoughEnergyForMostExpensiveBuilding() {
        return gameDetails.buildingsStats.values().stream()
                .filter(b -> b.price <= myself.energy)
                .toArray()
                .length == 3;
    }

    /**
     * Defend row
     *
     * @return the result
     **/
    // private String defendRow() {
    //     for (int i = 0; i < gameHeight; i++) {
    //         boolean opponentAttacking = getAnyBuildingsForPlayer(PlayerType.B, b -> b.buildingType == ATTACK, i);
    //         if (opponentAttacking && canAffordBuilding(DEFENSE)) {
    //             return placeBuildingInRowForDefense(DEFENSE, i);
    //         }
    //     }
    //     return placeBuildingInRowForEnergy();
    // }
    
    /**
     * Checks if this is under attack
     *
     * @return true if this is under attack
     **/
    private boolean isUnderAttack() {
        //if enemy has an attack building and i dont have a blocking wall
        for (int i = 0; i < gameHeight; i++) {
            boolean opponentAttacks = getAnyBuildingsForPlayer(PlayerType.B, building -> building.buildingType == ATTACK, i);
            boolean myDefense = getAnyBuildingsForPlayer(PlayerType.A, building -> building.buildingType == DEFENSE, i);
            
            if (opponentAttacks && !myDefense) {
                return true;
            }
        }
        return false;
    }
    
    private boolean doesRowHasBuilding(Player player, BuildingType buildingType, int y){
        return getAnyBuildingsForPlayer(player.playerType, building -> building.buildingType == buildingType, y);
    }


    /**
     * Do nothing command
     *
     * @return the result
     **/
    private String doNothingCommand() {
        return NOTHING_COMMAND;
    }

    /**
     * 
     * @param x
     * @param y
     * @return whether or not the cell is empty
     **/
    private boolean isCellEmpty(int x, int y) {
        Optional<CellStateContainer> cellOptional = gameState.getGameMap().stream()
                .filter(c -> c.x == x && c.y == y)
                .findFirst();

        if (cellOptional.isPresent()) {
            CellStateContainer cell = cellOptional.get();
            return cell.getBuildings().size() <= 0;
        } else {
            System.out.println("Invalid cell selected");
        }
        return true;
    }


    /**
     * Place building in row
     *
     * @param buildingType the building type
     * @param y            the y
     * @return the result
     **/
    private String placeBuildingInRowForDefense(BuildingType buildingType, int y) {
        List<CellStateContainer> emptyCells = gameState.getGameMap().stream()
                .filter(c -> c.getBuildings().isEmpty() && c.y == y && c.x < (gameWidth / 2) - 1)
                .collect(Collectors.toList());
        
        x = gameWidth / 2;
        while(x >= 1){
            if(!isCellEmpty(x, y)){
                return buildingType.buildCommand(x, y);
            }
            x --;
        }
    }

    private String placeBuildingInRowForAttack(BuildingType buildingType, int y) {
        List<CellStateContainer> emptyCells = gameState.getGameMap().stream()
                .filter(c -> c.getBuildings().isEmpty() && c.y == y && c.x < (gameWidth / 2) - 1)
                .collect(Collectors.toList());
        
        x = 1;
        while(x <= gameWidth / 2 - 1){
            if(!isCellEmpty(x, y)){
                return buildingType.buildCommand(x, y);
            }
            x ++;
        }
    }




    /**
     * Get random element of list
     *
     * @param list the list < t >
     * @return the result
     **/
    private <T> T getRandomElementOfList(List<T> list) {
        return list.get((new Random()).nextInt(list.size()));
    }

    private boolean getAnyBuildingsForPlayer(PlayerType playerType, Predicate<Building> filter, int y) {
        return buildings.stream()
                .filter(b -> b.getPlayerType() == playerType
                        && b.getY() == y)
                .anyMatch(filter);
    }

    /**
     * Can afford building
     *
     * @param buildingType the building type
     * @return the result
     **/
    private boolean canAffordBuilding(BuildingType buildingType) {
        return myself.energy >= gameDetails.buildingsStats.get(buildingType).price;
    }
}
