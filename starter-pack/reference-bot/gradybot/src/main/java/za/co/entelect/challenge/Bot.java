package za.co.entelect.challenge;

import za.co.entelect.challenge.entities.Building;
import za.co.entelect.challenge.entities.CellStateContainer;
import za.co.entelect.challenge.entities.GameState;
import za.co.entelect.challenge.entities.Player;
import za.co.entelect.challenge.enums.BuildingType;
import za.co.entelect.challenge.enums.PlayerType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Bot {

    private GameState gameState;

    // Bot ctor
    public Bot(GameState gameState)
    {
        
        // Read current game state
        this.gameState = gameState;

        // Get game map of the gamestate
        this.gameState.getGameMap();
    }

    // Generate command for the bot to execute
    public String run() {
        String command = "";
        
        if(this.getPlayerHealth(PlayerType.A)==100)
        {
            int countEnergyAttack = this.getEnergyBuildingCount(PlayerType.A) + this.getAttackBuildingCount(PlayerType.A);

            // STAGE 1 : ENERGY GREED
            
            if(this.getEnergyIncome(PlayerType.A)<=30)
            {
                // First round
                if(command=="" && countEnergyAttack%4==0)
                {
                    command = this.placeBuildingRandomlyFromBack(BuildingType.ENERGY);
                }
    
                // Building count not multiple of 3
                if(command=="" && this.getEnergyBuildingCount(PlayerType.A)%3!=0)
                {
                    command = this.placeBuildingRandomlyFromBack(BuildingType.ENERGY);
                }
    
                // If energy building count is multiple of 3 and no turret is placed(hence total energy + attack is not multiple of 4)
                if(command=="" && this.getEnergyBuildingCount(PlayerType.A)%3==0 && countEnergyAttack%4!=0)
                {
                    command = this.placeBuildingRandomlyFromFront(BuildingType.ATTACK);
                }
            }
            else
            {
                // STAGE 2 : ATTACK GREED
                // cari yang paling kosong
                command = this.placeBuildingInRowFromFront(BuildingType.ATTACK, this.getEnemyLeastBuildingRow(), 6);
            }
        }
        else
        {
            // STAGE 3 : DEFENSE GREED
            int mostRow = this.getEnemyMostBuildingRow();
            System.out.print("PISANG: ");
            System.out.println(mostRow);
            if(command=="" && this.isCellEmpty(6, mostRow))
            {
                command = this.placeBuilding(BuildingType.DEFENSE, 6,mostRow);
            }
            else if(command=="" && this.isCellEmpty(7, mostRow))
            {
                command = this.placeBuilding(BuildingType.DEFENSE, 7,mostRow);
            }
            else
            {
                command = this.placeBuildingInRowFromBack(BuildingType.ATTACK,  mostRow);
            }
        }
        
        for(int i=0;i<8;i++)
        {
            System.out.println(this.getEnemyBuildingCountInRow(i));
        }

        return command;
    }

    /* -=-=-=-=-=-=-=-=-=-=-=-= GAME DETAILS GETTER -=-=-=-=-=-=-=-=-=-=-=-= */
    // Private methods for getting game details, such as health, energy, etc.
    
    // Get playing area width
    private int mapWidth()
    {
        return this.gameState.gameDetails.mapWidth/2;
    }
    
    // Get playing area height
    private int mapHeight()
    {
        return this.gameState.gameDetails.mapHeight;
    }
    
    // Get player health of playerType
    private int getPlayerHealth(PlayerType playerType)
    {
        return this.gameState.getPlayers().stream()
        .filter(x -> x.playerType == playerType)
        .mapToInt(x -> x.health)
        .sum();
    }

    // Get player energy of playerType
    private int getPlayerEnergy(PlayerType playerType)
    {
        return this.gameState.getPlayers().stream()
        .filter(x -> x.playerType == playerType)
        .mapToInt(x -> x.energy)
        .sum();
    }

    // Get current energy income of playerType
    private int getEnergyIncome(PlayerType playerType) 
    {
        return this.gameState.gameDetails.roundIncomeEnergy + this.getEnergyBuildingCount(playerType)*3;
    }

    /* -=-=-=-=-=-=-=-=-=-=-=-= BUILDINGS GETTER -=-=-=-=-=-=-=-=-=-=-=-= */
    // Private methods for getting buildings related status.
    
    // Get building price
    private int getBuildingPrice(BuildingType buildingType) {
        return gameState.gameDetails.buildingsStats.get(buildingType).price;
    }
    
    // Get energy building count of playerType
    private int getEnergyBuildingCount(PlayerType playerType)
    {
        return this.getAllBuildingsForPlayer(
            playerType, 
            x->x.buildingType==BuildingType.ENERGY && x.constructionTimeLeft<=0
        )
        .size();
    }

    // Get attack building count of playerType
    private int getAttackBuildingCount(PlayerType playerType)
    {
        return this.getAllBuildingsForPlayer(
            playerType, 
            x->x.buildingType==BuildingType.ATTACK && x.constructionTimeLeft<=0
        )
        .size();
    }
    
    // Get defense building count of playerType
    private int getDefenseBuildingCount(PlayerType playerType)
    {
        return this.getAllBuildingsForPlayer(
            playerType, 
            x->x.buildingType==BuildingType.DEFENSE && x.constructionTimeLeft<=0
            )
            .size();
    }
    
    // Get defense building count of playerType
    private int getTeslaBuildingCount(PlayerType playerType)
    {
        return this.getAllBuildingsForPlayer(
            playerType, 
            x->x.buildingType==BuildingType.TESLA && x.constructionTimeLeft<=0
        )
        .size();
    }

    /* NOTE: ini bisa pake getAllBuildingsForPlayer(playerType, filter) */

    // Get count of enemy's energy building in row y
    private int getEnemyEnergyInRow(int row)
    {
        return this.getAllBuildingsForPlayerCellFilter(PlayerType.B, c->c.y==row,c -> c.buildingType==BuildingType.ENERGY).size();   
    }
    
    // Get count of enemy's attack building in row y
    private int getEnemyAttackInRow(int row)     
    {
        return this.getAllBuildingsForPlayerCellFilter(PlayerType.B, c->c.y==row,c -> c.buildingType==BuildingType.ATTACK).size();   
    }
    
    // Get count of enemy's defense building in row y
    private int getEnemyDefenseInRow(int row)
    {
        return this.getAllBuildingsForPlayerCellFilter(PlayerType.B, c->c.y==row,c -> c.buildingType==BuildingType.DEFENSE).size();   
    }
    
    // Get total count of enemy's building in row y
    private int getEnemyBuildingCountInRow(int row)
    {
        return getEnemyAttackInRow(row)+getEnemyEnergyInRow(row)+getEnemyDefenseInRow(row);
    }

    // Return enemy's least row building, selects random least if many
    private int getEnemyLeastBuildingRow()
    {
        ArrayList<Integer> minimumRows = new ArrayList<Integer>();
        int minimumCount = 999;
        for(int i=0;i<8;i++)
        {
            if(minimumCount==this.getEnemyBuildingCountInRow(i))
            {
                minimumRows.add(i);
            }
            else if(minimumCount>this.getEnemyBuildingCountInRow(i))
            {
                minimumRows = new ArrayList<Integer>();
                minimumRows.add(i);
                minimumCount=i;
            }
        }

        return minimumRows.get((new Random()).nextInt(minimumRows.size()));

    }

    // Return enemy's most row building, selects random most if many
    private int getEnemyMostBuildingRow()
    {
        ArrayList<Integer> maximumRows = new ArrayList<Integer>();
        int maximumCount = -999;
        for(int i=0;i<8;i++)
        {
            if(maximumCount==this.getEnemyBuildingCountInRow(i))
            {
                maximumRows.add(i);
            }
            else if(maximumCount<this.getEnemyBuildingCountInRow(i))
            {
                maximumRows = new ArrayList<Integer>();
                maximumRows.add(i);
                maximumCount=i;
            }
        }

        return maximumRows.get((new Random()).nextInt(maximumRows.size()));

    }

    /* -=-=-=-=-=-=-=-=-=-=-=-= BUILDINGS PLACER -=-=-=-=-=-=-=-=-=-=-=-= */
    // Private methods for placing buildings.
    
    // Place building in coordinate (x,y)
    private String placeBuilding(BuildingType buildingType, int x, int y)
    {
        return buildCommand(x, y, buildingType);
    }   
    
    // Placing building randomly in the back
    private String placeBuildingRandomlyFromBack(BuildingType buildingType) {
        for (int i = 0; i < this.mapWidth(); i++) {
            List<CellStateContainer> listOfFreeCells = getColumnEmptyCellList(i);
            if (!listOfFreeCells.isEmpty()) {
                CellStateContainer pickedCell = listOfFreeCells.get((new Random()).nextInt(listOfFreeCells.size()));
                return buildCommand(pickedCell.x, pickedCell.y, buildingType);
            }
        }
        return "";
    }

    // Placing building randomly in the front
    private String placeBuildingRandomlyFromFront(BuildingType buildingType) {
        for (int i = this.mapWidth() - 1; i >= 0; i--) {
            List<CellStateContainer> listOfFreeCells = getColumnEmptyCellList(i);
            if (!listOfFreeCells.isEmpty()) {
                CellStateContainer pickedCell = listOfFreeCells.get((new Random()).nextInt(listOfFreeCells.size()));
                return buildCommand(pickedCell.x, pickedCell.y, buildingType);
            }
        }
        return "";
    }

    // Placing building in a row from front line
    private String placeBuildingInRowFromFront(BuildingType buildingType, int y, int start) {
        for (int i = (gameState.gameDetails.mapWidth / 2) - 1; i >= 0; i--) {
            if (i<=start && isCellEmpty(i, y)) {
                return buildCommand(i, y, buildingType);
            }
        }
        return "";
    }

    // Placing building in a row from back line
    private String placeBuildingInRowFromBack(BuildingType buildingType, int y) {
        for (int i = 0; i < gameState.gameDetails.mapWidth / 2; i++) {
            if (isCellEmpty(i, y)) {
                return buildCommand(i, y, buildingType);
            }
        }
        return "";
    }

    // Build command construction
    private String buildCommand(int x, int y, BuildingType buildingType) {
        return String.format("%s,%d,%s", String.valueOf(x), y, buildingType.getCommandCode());
    }

    // Returns boolean to check if a building can be planted in (x,y) with calculating bullets, turrets, and its construction time as well
    // private boolean isCellSafe(BuildingType buildingType, int x, int y)

    /* -=-=-=-=-=-=-=-=-=-=-=-= BUILDINGS LIST -=-=-=-=-=-=-=-=-=-=-=-= */
    // Private methods for getting building List, relating to cellstatecontainer

    // Get list for all building of playerType with additional filter
    private List<Building> getAllBuildingsForPlayer(PlayerType playerType, Predicate<Building> filter) {
        return gameState.getGameMap().stream()
                .filter(c -> c.cellOwner == playerType)
                .flatMap(c -> c.getBuildings().stream())
                .filter(filter)
                .collect(Collectors.toList());
    }
    // Get list for all building of playerType with additional filter
    private List<Building> getAllBuildingsForPlayerCellFilter(PlayerType playerType,  Predicate<CellStateContainer> filter1, Predicate<Building> filter2) {
        return gameState.getGameMap().stream()
                .filter(c -> c.cellOwner == playerType)
                .filter(filter1)
                .flatMap(c -> c.getBuildings().stream())
                .filter(filter2)
                .collect(Collectors.toList());
    }
    
    // Get list of empty cells in column x
    private List<CellStateContainer> getColumnEmptyCellList(int x) {
        return gameState.getGameMap().stream()
                .filter(c -> c.x == x && isCellEmpty(x, c.y))
                .collect(Collectors.toList());
    }

    // Get list of empty row in row y of playerType
    private List<CellStateContainer> getListOfEmptyCellsForRow(int y, PlayerType playerType) {
        if(playerType == PlayerType.A){
            return gameState.getGameMap().stream()
                    .filter(c -> c.y == y && c.x<=7 && isCellEmpty(c.x, c.y))
                    .collect(Collectors.toList());
        }
        else{
            return gameState.getGameMap().stream()
                    .filter(c -> c.y == y && c.x>7 && isCellEmpty(c.x, c.y))
                    .collect(Collectors.toList());
        }
    }

    // Check if cell empty
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
