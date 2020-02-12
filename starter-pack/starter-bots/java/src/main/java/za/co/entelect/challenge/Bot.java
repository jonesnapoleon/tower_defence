package za.co.entelect.challenge;

import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.BuildingType;
import za.co.entelect.challenge.enums.PlayerType;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Optional;
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

        buildings = gameState.getGameMap().stream()
                .flatMap(c -> c.getBuildings().stream())
                .collect(Collectors.toList());

        missiles = gameState.getGameMap().stream()
                .flatMap(c -> c.getMissiles().stream())
                .collect(Collectors.toList());
    }

    /**
     * Run
     *
     * @return the result
     **/
    public String run() {
        String command = "";

        // If the enemy has more than 1 attack building on row, then block on the front and make attack building
        // Oh yeah, make a defense building, and make it double.
        for (int i = 0; i < gameState.gameDetails.mapHeight; i++) {
            int enemyAttackOnRow = getAllBuildingsForPlayer(PlayerType.B, b -> b.buildingType == BuildingType.ATTACK, i).size();
            int myDefenseOnRow = getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.DEFENSE, i).size();

            if (enemyAttackOnRow >= 1 && myDefenseOnRow < 1) {
                if (canAffordBuilding(BuildingType.DEFENSE))
                    if (isCellEmpty(6, i)) {
                        command = buildCommand(6, i, BuildingType.DEFENSE);
                    }
                else
                    command = "";
                break;
            }
        }

        // Place attack building on the row with highest amount of enemy attack building
        if(command.equals("")){
            int i = 0;
            int previous = 0;
            int countNow = 0;
            int selectedLane = 0;
            for (i = 0; i < gameState.gameDetails.mapHeight; i++) {
                int currentEnemyAttackOnRow = getAllBuildingsForPlayer(PlayerType.B, b -> b.buildingType == BuildingType.ATTACK, i).size();
                countNow = currentEnemyAttackOnRow;
                if (countNow >= previous) {
                    selectedLane = i;
                    previous = countNow;
                }
                
            }
            int enemyAttackOnRow = getAllBuildingsForPlayer(PlayerType.B, b -> b.buildingType == BuildingType.ATTACK,selectedLane).size();
            int myDefenseOnRow = getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.DEFENSE, selectedLane).size();

            if (enemyAttackOnRow > 0 && myDefenseOnRow >= 1) {
                if (canAffordBuilding(BuildingType.ATTACK))
                    command = placeBuildingInRowFromBack(BuildingType.ATTACK, selectedLane);
                else 
                    command = "";
            } else if (enemyAttackOnRow > 0 && myDefenseOnRow < 2) {
                if (canAffordBuilding(BuildingType.DEFENSE))
                    command = placeBuildingInRowFromFront(BuildingType.DEFENSE, i);
                else
                    command = "";
                break;
            }
        }


        //If there is a row where I don't have energy and there is no enemy attack building, then build energy in the back row.
        if (command.equals("")) {
            for (int i = 0; i < gameState.gameDetails.mapHeight; i++) {
                int enemyAttackOnRow = getAllBuildingsForPlayer(PlayerType.B, b -> b.buildingType == BuildingType.ATTACK, i).size();
                int myEnergyOnRow = getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.ENERGY, i).size();

                if (enemyAttackOnRow == 0 && myEnergyOnRow == 0) {
                    if (canAffordBuilding(BuildingType.ENERGY)) {
                        command = placeBuildingInRowFromBack(BuildingType.ENERGY, i);
                        break;
                    }
                }
            }
        }

        //If I have a defense building on a row, then build an attack building behind it.
        if (command.equals("")) {
            for (int i = 0; i < gameState.gameDetails.mapHeight; i++) {
                if (getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.DEFENSE, i).size() > 0
                        && canAffordBuilding(BuildingType.ATTACK)) {
                    command = placeNonEnergyBuildingInRowFromBack(BuildingType.ATTACK, i);
                }
            }
        }

        // If I have attack building but no defense building, then build a defense building on front
        if (command.equals("")) {
            for (int i = 0; i < gameState.gameDetails.mapHeight; i++) {
                if (getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.DEFENSE, i).size() == 0 && 
                    getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.ATTACK, i).size() > 0
                        && canAffordBuilding(BuildingType.DEFENSE)) {
                    command = placeBuildingInRowFromFront(BuildingType.DEFENSE, i);  
                }
            }
        }
        
        // If everything is balanced as everything should be, then build an attack building from the back.
        if (command.equals("")) {
            for (int i = 0; i < gameState.gameDetails.mapHeight; i++) {
                if (getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.ATTACK, i).size() == 0
                        && canAffordBuilding(BuildingType.ATTACK)) {
                    command = placeNonEnergyBuildingInRowFromBack(BuildingType.ATTACK, i);
                }
            }
        }

        // If things go nice, starts attack building from the second layer
        if(command.equals("")){
            for (int i = 0; i < gameState.gameDetails.mapHeight; i++) {
                if (getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.ATTACK, i).size() <= 1) {
                    command = placeNonEnergyBuildingInRowFromBack(BuildingType.ATTACK, i);
                }
            }
        }
        
        // If everything goes very good and nais and we have more than 300 energy, then start tesla.
        if (command.equals("") && myself.energy >= 180 && countBuilding(PlayerType.A, b -> b.buildingType == BuildingType.TESLA) < 2) {
            for (int i = 0; i < gameState.gameDetails.mapHeight; i++) {
                if(isCellEmpty(6, i)){ 
                    command = buildCommand(6, i, BuildingType.TESLA);
                }
            }
        }

        // If backline attack is filled and we have enough energy to make Tesla Tower, then just do it *winks*
        if (command.equals("")) {
            
            if (myself.energy >= 300 && getAllBuildingsForPlayer(PlayerType.A, b -> b.buildingType == BuildingType.TESLA, 2).size() == 0) {
                command = buildCommand(6,2, BuildingType.DECONSTRUCT);
            }
            
        }

        return command;
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
     * Place building in row y nearest to the back
     *
     * @param buildingType the building type
     * @param y            the y
     * @return the result
     **/
    private String placeBuildingInRowFromBack(BuildingType buildingType, int y) {
        for (int i = 0; i < gameState.gameDetails.mapWidth / 2; i++) {
            if (isCellEmpty(i, y)) {
                return buildCommand(i, y, buildingType);
            }
        }
        return "";
    }

    /**
     * Place building in row y nearest to the back
     *
     * @param buildingType the building type
     * @param y            the y
     * @return the result
     **/
    private String placeNonEnergyBuildingInRowFromBack(BuildingType buildingType, int y) {
        for (int i = 1; i < gameState.gameDetails.mapWidth / 2; i++) {
            if (isCellEmpty(i, y)) {
                return buildCommand(i, y, buildingType);
            }
        }
        return "";
    }


    /**
     * Get all buildings for player in row y
     *
     * @param playerType the player type
     * @param filter     the filter
     * @param y          the y
     * @return the result
     **/
    private List<Building> getAllBuildingsForPlayer(PlayerType playerType, Predicate<Building> filter, int y) {
        return gameState.getGameMap().stream()
                .filter(c -> c.cellOwner == playerType && c.y == y)
                .flatMap(c -> c.getBuildings().stream())
                .filter(filter)
                .collect(Collectors.toList());
    }


    private int countBuilding(PlayerType playerType, Predicate<Building> filter){
        return gameState.getGameMap().stream()
                .filter(c -> c.cellOwner == playerType)
                .flatMap(c -> c.getBuildings().stream())
                .filter(filter)
                .collect(Collectors.toList())
                .size();
    }


    /**
     * Get the lane with most enemy attack building
     * 
     */
    private int getEnemyLaneWithMostAttackBuilding() {
        return 0;
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
    private String defendRow() {
        for (int i = 0; i < gameHeight; i++) {
            boolean opponentAttacking = getAnyBuildingsForPlayer(PlayerType.B, b -> b.buildingType == ATTACK, i);
            if (opponentAttacking && canAffordBuilding(DEFENSE)) {
                return placeBuildingInRow(DEFENSE, i);
            }
        }

        return buildRandom();
    }

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

    /**
     * Do nothing command
     *
     * @return the result
     **/
    private String doNothingCommand() {
        return NOTHING_COMMAND;
    }

    /**
     * Place building in row
     *
     * @param buildingType the building type
     * @param y            the y
     * @return the result
     **/
    private String placeBuildingInRow(BuildingType buildingType, int y) {
        List<CellStateContainer> emptyCells = gameState.getGameMap().stream()
                .filter(c -> c.getBuildings().isEmpty()
                        && c.y == y
                        && c.x < (gameWidth / 2) - 1)
                .collect(Collectors.toList());

        if (emptyCells.isEmpty()) {
            return buildRandom();
        }

        CellStateContainer randomEmptyCell = getRandomElementOfList(emptyCells);
        return buildingType.buildCommand(randomEmptyCell.x, randomEmptyCell.y);
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

    /**
     * Place building in row y nearest to the front
     *
     * @param buildingType the building type
     * @param y            the y
     * @return the result
     **/
    private String placeBuildingInRowFromFront(BuildingType buildingType, int y) {
        for (int i = (gameState.gameDetails.mapWidth / 2) - 1; i >= 0; i--) {
            if (isCellEmpty(i, y)) {
                return buildCommand(i, y, buildingType);
            }
        }
        return "";
    }

    /**
     * Place building in row y nearest to the front
     *
     * @param buildingType the building type
     * @param y            the y
     * @return the result
     **/
    private String placeBuildingInRowXColumnFromFront(BuildingType buildingType, int y, int back) {
        for (int i = (gameState.gameDetails.mapWidth / 2) - 1 - back; i >= 0; i--) {
            if (isCellEmpty(i, y)) {
                return buildCommand(i, y, buildingType);
            }
        }
        return "";
    }

    /**
     * Construct build command
     *
     * @param x            the x
     * @param y            the y
     * @param buildingType the building type
     * @return the result
     **/
    private String buildCommand(int x, int y, BuildingType buildingType) {
        return String.format("%s,%d,%d", String.valueOf(x), y, buildingType.getType());
    }

    /**
     * Checks if cell at x,y is empty
     *
     * @param x the x
     * @param y the y
     * @return the result
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
}
