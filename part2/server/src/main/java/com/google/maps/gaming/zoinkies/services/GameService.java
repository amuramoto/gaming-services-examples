/**
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.maps.gaming.zoinkies.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.maps.gaming.zoinkies.GameConstants;
import com.google.maps.gaming.zoinkies.exceptions.LocationStillRespawningException;
import com.google.maps.gaming.zoinkies.exceptions.NotEnoughResourcesToUnlockException;
import com.google.maps.gaming.zoinkies.models.BattleData;
import com.google.maps.gaming.zoinkies.models.BattleSummaryData;
import com.google.maps.gaming.zoinkies.models.EnergyData;
import com.google.maps.gaming.zoinkies.models.Item;
import com.google.maps.gaming.zoinkies.models.LootRefItem;
import com.google.maps.gaming.zoinkies.models.PlayerData;
import com.google.maps.gaming.zoinkies.models.ReferenceData;
import com.google.maps.gaming.zoinkies.models.ReferenceItem;
import com.google.maps.gaming.zoinkies.models.RewardsData;
import com.google.maps.gaming.zoinkies.models.SpawnLocation;
import com.google.maps.gaming.zoinkies.models.WorldData;
import com.google.maps.gaming.zoinkies.models.playablelocations.PLLatLng;
import com.google.maps.gaming.zoinkies.models.playablelocations.PLLocation;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * This class contains gameplay logic for this demo.
 * Among others, it handles the player's inventory, battle loots or losses,
 * and locations respawn.
 *
 * It leverages services classes such as PlayerService and WorldService.
 * It also uses ReferenceData.
 */
@Service
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class GameService {

  @Autowired
  com.google.maps.gaming.zoinkies.services.WorldService WorldService;

  @Autowired
  com.google.maps.gaming.zoinkies.services.PlayerService PlayerService;

  /**
   * Keeps a reference to game data after being loaded from the resources folder.
   */
  private ReferenceData referenceData;

  /**
   * Gives access to cached reference data.
   * Reference data is provided as part of a resource json file.
   *
   * @return A new reference data
   */
  public ReferenceData getReferenceData() {
    if (referenceData == null) {
      String content = "";
      try {
        try (InputStream inputStream = getClass().getResourceAsStream("/ReferenceData.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
          content = reader.lines()
              .collect(Collectors.joining(System.lineSeparator()));
        }
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        referenceData = objectMapper.readValue(content, ReferenceData.class);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return referenceData;
  }

  /**
   * Creates a new PlayerData object
   * @return A new PlayerData.
   */
  public PlayerData CreateNewUser() {
    // Default Inventory
    List<Item> inventory = new ArrayList<>();
    inventory.add(new Item(GameConstants.BODY_ARMOR_TYPE_1, 1));
    inventory.add(new Item(GameConstants.WEAPON_TYPE_1, 1));
    // Character types are added to the default inventory
    inventory.add(new Item(GameConstants.CHARACTER_TYPE_1, 1));
    inventory.add(new Item(GameConstants.CHARACTER_TYPE_2, 1));
    inventory.add(new Item(GameConstants.CHARACTER_TYPE_3, 1));
    inventory.add(new Item(GameConstants.CHARACTER_TYPE_4, 1));
    PlayerData d = new PlayerData(GameConstants.DEFAULT_PLAYER_NAME,
        GameConstants.CHARACTER_TYPE_1,
        GameConstants.DEFAULT_PLAYER_ENERGY_LEVEL,
        GameConstants.DEFAULT_PLAYER_ENERGY_LEVEL,
        inventory);
    // Equip body armor and weapon starters
    d.setEquippedBodyArmor(GameConstants.BODY_ARMOR_TYPE_1);
    d.setEquippedWeapon(GameConstants.WEAPON_TYPE_1);
    return d;
  }

  /**
   * Helper function that checks and eventually progresses the timestamp of a respawnable location.
   *
   * @param Id  The player's unique identifier
   * @param LocationId The location id
   * @throws Exception
   */
  private void CheckLocationStatus(String Id, String LocationId) throws Exception{

    WorldData worldData = WorldService.GetWorldData(Id);
    if (!worldData.getLocations().containsKey(LocationId)) {
      throw new Exception("Location Id " + LocationId + " not found!");
    }
    SpawnLocation location = worldData.getLocations().get(LocationId);
    // If the location is inactive, progress its timestamp and update its status.
    if (!location.getActive()) {
      if (location.getRespawn_time() == null || location.getRespawn_time().isEmpty()) {
        throw new Exception("Invalid Timestamp found at location " + location.getId());
      }
      // Check timestamp progress
      Instant t = Instant.parse(location.getRespawn_time());
      if (t.compareTo(Instant.now()) > 0) {
        // Still respawning
        throw new LocationStillRespawningException("Location " + LocationId +
            " is still respawning!");
      } else {
        // Reactive the location
        location.setActive(true);
        location.setRespawn_time(null);
        WorldService.SetWorldData(Id,worldData);
      }
    }
  }

  /**
   * Helper function that sets the duration timestamp on a spawnable location.
   *
   * @param ItemId A valid item id
   * @param UserId The player id
   * @param LocationId The location id
   * @param WorldData A reference to the world data
   * @throws Exception
   */
  private void StartRespawiningLocation(String ItemId, String UserId, String LocationId,
      WorldData WorldData) throws Exception {
    if (ItemId == null || ItemId.isEmpty())
      throw new Exception("Invalid item Id!");
    if (UserId == null || UserId.isEmpty())
      throw new Exception("Invalid User Id!");
    if (LocationId == null || LocationId.isEmpty())
      throw new Exception("Invalid Location Id!");
    if (WorldData == null)
      throw new Exception("Invalid World Data reference!");
    ReferenceItem refItem = this.getReferenceData().getReferenceItem(ItemId);
    if (refItem == null)
      throw new Exception("Reference item " + ItemId + " not found!");
    // Respawnable items have a duration set in their config.
    if (refItem.getRespawnDuration() != null) {
      SpawnLocation location = WorldData.getLocations().get(LocationId);
      location.setActive(false);
      location.setRespawn_time(Instant.now().plus(refItem.getRespawnDuration()).toString());
      // Update World Data
      WorldService.SetWorldData(UserId, WorldData);
    }
  }

  /**
   * Returns the summary of the current battle.
   * There is no cheat code detection in this demo, but this could be a location for it.
   *
   * @param Id The unique player id
   * @param LocationId The unique location id
   * @param Winner Who the winner of the battle is (we trust the client in this demo)
   * @return A battle summary data with battle rewards or losses.
   * @throws Exception
   */
  public BattleSummaryData GetBattleSummaryData(String Id, String LocationId,
      Boolean Winner) throws Exception {
    WorldData worldData = WorldService.GetWorldData(Id);
    if (!worldData.getLocations().containsKey(LocationId)) {
      throw new Exception("Location Id " + LocationId + " not found!");
    }
    BattleSummaryData data = new BattleSummaryData();
    data.setWinner(Winner);
    data.setWonTheGame(false);
    SpawnLocation location = worldData.getLocations().get(LocationId);
    if (location.getObject_type_id().equals(GameConstants.MINION)
        || location.getObject_type_id().equals(GameConstants.TOWER)) {
      // Check that this location isn't already respawning
      CheckLocationStatus(Id,LocationId);
      // Get the associated reference Item
      ReferenceItem refItem = getReferenceData().getReferenceItem(location.getObject_type_id());
      if (refItem == null) {
        throw new Exception("Can't find reference Item for " + location.getObject_type_id() + "!");
      }
      PlayerData playerData = PlayerService.GetPlayerData(Id);
      RewardsData rewardsData;
      // Process the battle summary
      if (Winner) { // Player wins
        // Get battle rewards - 1 key + 1 items depending on who wins
        // Rewards vary based on opponent
        if (location.getObject_type_id().equals(GameConstants.MINION)) {
          rewardsData = GetRandomMinionBattleRewardsData();
        }
        else {
          rewardsData = GetRandomGeneralBattleRewardsData();
        }
        // Check if the game is won
        playerData.addAllInventoryItems(rewardsData.getItems());
        List<Item> freedLeaders = playerData.getInventoryItems(GameConstants.FREED_LEADERS);
        if (freedLeaders.size() > 0 && freedLeaders.get(0).getQuantity()
            >= GameConstants.FREED_LEADERS_TO_WIN) {
          data.setWonTheGame(true); // Hurray!
        }
      } else { // Zoinkies win
        rewardsData = new RewardsData();
        Item item;
          // Lose one key if we have any. Update inventory
        List<Item> keys = playerData.getInventoryItems(GameConstants.GOLD_KEY);
        if (keys.size() > 0) {
          int newQty = keys.get(0).getQuantity()-1;
          if (newQty == 0) {
            keys.remove(0);
          } else {
            keys.get(0).setQuantity(newQty);
          }
        }
        // Update "rewards"
        rewardsData.getItems().add(new Item(GameConstants.GOLD_KEY,-1));
      }
      data.setRewards(rewardsData);
      data.getRewards().setId(LocationId);
      // Update player data
      PlayerService.UpdatePlayerData(Id,playerData);
      // Lock the location as the battle has ended.
      if (refItem.getRespawnDuration() != null) {
        location.setActive(false);
        location.setRespawn_time(Instant.now().plus(refItem.getRespawnDuration()).toString());
        WorldService.SetWorldData(Id, worldData);
      }
    }
    return data;
  }

  /**
   * This function handles the logic for the battle setup.
   * More specifically, it checks that all pre-requisites are checked for either engaging minions
   * or attacking a tower.
   *
   * @param Id The userId
   * @param LocationId The LocationId
   * @return A Battle Data
   * @throws Exception
   */
  public BattleData GetBattleData(String Id, String LocationId) throws Exception {
    WorldData worldData = WorldService.GetWorldData(Id);
    if (!worldData.getLocations().containsKey(LocationId)) {
      throw new Exception("Location Id " + LocationId + " not found!");
    }
    // Minion or General?
    // Minion - regenerate - grant gold keys
    // General/towers - no regeneration - grant leaders
    // - tower disappears afterwards - requires diamond keys
    BattleData data = new BattleData();;
    data.setId(LocationId);
    // Check pre-requisites:
    SpawnLocation location = worldData.getLocations().get(LocationId);
    if (location.getObject_type_id().equals(GameConstants.MINION)) {
      ReferenceItem minionRefItem = getReferenceData().getReferenceItem(GameConstants.MINION);
      if (minionRefItem == null) {
        throw new Exception("Can't find reference data for Minions!");
      }
      // - Chest must be in active mode and not respawning
      // Get World Data
      CheckLocationStatus(Id,LocationId);
      data.setOpponentTypeId(GameConstants.MINION);
      data.setPlayerStarts(ThreadLocalRandom.current().nextBoolean());
      data.setCooldown(getReferenceData().getReferenceItem(GameConstants.MINION).getCooldown());
      data.setEnergyLevel(GameConstants.DEFAULT_MINION_ENERGY_LEVEL);
      data.setMaxAttackScoreBonus(GameConstants.MAX_ATTACK_BONUS_MINION);
      data.setMaxAttackScoreBonus(GameConstants.MAX_DEFENSE_BONUS_MINION);

    } else if (location.getObject_type_id().equals(GameConstants.TOWER)) {
      ReferenceItem generalRefItem = getReferenceData().getReferenceItem(GameConstants.GENERAL);
      if (generalRefItem == null) {
        throw new Exception("Can't find reference data for General!");
      }
      ReferenceItem towerRefItem = getReferenceData().getReferenceItem(GameConstants.TOWER);
      if (towerRefItem == null) {
        throw new Exception("Can't find reference data for Towers!");
      }
      // Check pre-requisites
      // Get PlayerData
      PlayerData playerData = PlayerService.GetPlayerData(Id);
      // - Player must have enough gold keys
      //ReferenceData referenceData = getReferenceData();
      ReferenceItem ri = getReferenceData().getReferenceItem(GameConstants.DIAMOND_KEY);
      if (ri == null)
        throw new Exception("Reference item " + GameConstants.DIAMOND_KEY + " not found!");
      // Consume diamond keys
      // If pre-reqs not met, return custom 20X code
      List<Item> items = playerData.getInventoryItems(GameConstants.DIAMOND_KEY);
      if (items.size() > 0 && items.get(0).getQuantity()
          >= location.getNumber_of_keys_to_activate()) {
        // Update player's inventory
        items.get(0).setQuantity(items.get(0).getQuantity()
            - location.getNumber_of_keys_to_activate());
        // Location is unlocked
        location.setNumber_of_keys_to_activate(0);
        // Update location
        WorldService.SetWorldData(Id,worldData);
        // Update Player Data
        PlayerService.UpdatePlayerData(Id, playerData);

      } else {
        throw new NotEnoughResourcesToUnlockException("Not enough Diamond keys to unlock tower!");
      }
      data.setOpponentTypeId(GameConstants.GENERAL);
      data.setPlayerStarts(ThreadLocalRandom.current().nextBoolean());
      data.setCooldown(getReferenceData().getReferenceItem(GameConstants.GENERAL).getCooldown());
      data.setEnergyLevel(GameConstants.DEFAULT_GENERAL_ENERGY_LEVEL);
      data.setMaxAttackScoreBonus(GameConstants.MAX_ATTACK_BONUS_GENERAL);
      data.setMaxAttackScoreBonus(GameConstants.MAX_DEFENSE_BONUS_GENERAL);

    } else {
      // Unexpected LocationId ?
      throw new Exception("Battles can only be started against Minions and Towers (Generals)");
    }
    return data;
  }

  /**
   * Returns the energy recovered from the energy station.
   *
   * @param Id Unique player Id
   * @param LocationId Unique location Id for the station
   * @return new Energy Data
   * @throws Exception
   */
  public EnergyData GetEnergyStationData(String Id, String LocationId) throws Exception {
    WorldData worldData = WorldService.GetWorldData(Id);
    if (!worldData.getLocations().containsKey(LocationId)) {
      throw new Exception("Location Id " + LocationId + " not found!");
    }
    // Check pre-requisites:
    // - Chest must be in active mode and not respawning
    // Get World Data
    CheckLocationStatus(Id,LocationId);
    // Get PlayerData
    PlayerData playerData = PlayerService.GetPlayerData(Id);
    int energy = playerData.getMaxEnergyLevel() - playerData.getEnergyLevel();
    playerData.setEnergyLevel(playerData.getMaxEnergyLevel());
    // Refill all energy points
    EnergyData data = new EnergyData();
    data.setId(LocationId);
    data.setAmountRestored(energy);
    // Update Player Data
    PlayerService.UpdatePlayerData(Id, playerData);
    // Change station status and start respawning
    StartRespawiningLocation(GameConstants.ENERGY_STATION, Id, LocationId, worldData);
    return data;
  }

  /**
   * Returns the rewards associated with the given chest location,
   * after checking that the pre-requisites are met and that the location is active.
   * Starts the respawning of the location afterwards.
   * Also updates the player's inventory in the process.
   * @param Id
   * @param LocationId
   * @return
   * @throws Exception
   */
  public RewardsData GetChestRewards(String Id, String LocationId) throws Exception {
    // Check pre-requisites:
    // - Chest must be in active mode and not respawning
    // Get World Data
    WorldData worldData = WorldService.GetWorldData(Id);
    if (!worldData.getLocations().containsKey(LocationId)) {
      throw new Exception("Location Id " + LocationId + " not found!");
    }
    CheckLocationStatus(Id,LocationId);
    SpawnLocation location = worldData.getLocations().get(LocationId);
    // Get PlayerData
    PlayerData playerData = PlayerService.GetPlayerData(Id);
    // - Player must have enough gold keys
    ReferenceData referenceData = getReferenceData();
    ReferenceItem ri = referenceData.getReferenceItem(GameConstants.GOLD_KEY);
    if (ri == null)
      throw new Exception("Reference item " + GameConstants.GOLD_KEY + " not found!");
    ReferenceItem chestRefItem = referenceData.getReferenceItem(GameConstants.CHEST);
    if (chestRefItem == null)
      throw new Exception("Reference item " + GameConstants.CHEST + " not found!");
    // Check if we have enough keys
    List<Item> items = playerData.getInventoryItems(GameConstants.GOLD_KEY);
    if (items.size() > 0
        && items.get(0).getQuantity() >= location.getNumber_of_keys_to_activate()) {
      // Consume gold keys and Update player's inventory
      items.get(0).setQuantity(items.get(0).getQuantity()
          - location.getNumber_of_keys_to_activate());

      // Start respawning
      StartRespawiningLocation(GameConstants.CHEST, Id, LocationId, worldData);
     // Return response
    } else {
      throw new NotEnoughResourcesToUnlockException("Not enough Gold keys to unlock chest!");
    }

    // Get rewards
    // Generate rewards from loot table
    RewardsData data = GetRandomChestRewardsData();
    data.setId(Id);

    // Update player's inventory
    for (Item i:data.getItems()) {
      playerData.addInventoryItem(i);
    }

    // Update Player Data
    PlayerService.UpdatePlayerData(Id, playerData);

    return data;
  }

    /**
     * Creates a spawn location based on a basic random distribution.
     * These locations are persisted for the duration of the game.
     * The distribution is as follows:
     * 5% -> Restore Stations
     * 20% -> Chests
     * 15% -> Towers
     * 60% -> Minions
     *
     * @return
     */
  public SpawnLocation CreateRandomSpawnLocation(PLLocation loc) throws Exception {

    if (loc == null) {
      throw new Exception("Invalid location data found while creating random spawn location!");
    }

    String locationId = null;
    if (loc.getName() != null && !loc.getName().isEmpty()) {
      locationId = loc.getName().replace("/","_");
    }
    else {
      throw new Exception("Invalid location name found while creating random spawn location!");
    }

    if (loc.getSnappedPoint() == null && loc.getCenterPoint() == null) {
      throw new Exception(
          "Invalid Lat Lng coordinates found while creating random spawn location!");
    }

    PLLatLng point = loc.getSnappedPoint()==null?loc.getCenterPoint():loc.getSnappedPoint();

    SpawnLocation location = new SpawnLocation();
    location.setSnappedPoint(point);
    location.setId(locationId);
    int randomNum = ThreadLocalRandom.current().nextInt(0, 100 + 1);
    if (IsBetween(randomNum, 0,4)) {
      location.setObject_type_id(GameConstants.ENERGY_STATION);
      location.setActive(true);
      location.setNumber_of_keys_to_activate(0);
      location.setKey_type_id(null);
      location.setRespawns(true);
    } else if (IsBetween(randomNum, 5,24)) {
      location.setObject_type_id(GameConstants.CHEST);
      location.setActive(true);
      location.setNumber_of_keys_to_activate(3);
      location.setKey_type_id(GameConstants.GOLD_KEY);
      location.setRespawns(true);
    } else if (IsBetween(randomNum, 25,39)) {
      location.setObject_type_id( GameConstants.TOWER);
      location.setActive(true);
      location.setNumber_of_keys_to_activate(3);
      location.setKey_type_id(GameConstants.DIAMOND_KEY);
      location.setRespawns(false);
    } else {
      location.setObject_type_id(GameConstants.MINION);
      location.setActive(true);
      location.setNumber_of_keys_to_activate(0);
      location.setKey_type_id(null);
      location.setRespawns(true);
    }
    return location;
  }

  /**
   * Returns a reference table for bosses battles
   */
  private List<LootRefItem> GeneralBattleLootTable = new ArrayList<LootRefItem>() {
    {
      add(new LootRefItem(GameConstants.GOLD_KEY, 0.4, 1, 1));
      add(new LootRefItem(GameConstants.HELMET_TYPE_2,0.1,1,1));
      add(new LootRefItem(GameConstants.HELMET_TYPE_3,0.05,1,1));
      add(new LootRefItem(GameConstants.BODY_ARMOR_TYPE_2,0.1,1,1));
      add(new LootRefItem(GameConstants.BODY_ARMOR_TYPE_3,0.05,1,1));
      add(new LootRefItem(GameConstants.SHIELD_TYPE_2,0.1,1,1));
      add(new LootRefItem(GameConstants.SHIELD_TYPE_3,0.05,1,1));
      add(new LootRefItem(GameConstants.WEAPON_TYPE_2,0.1,1,1));
      add(new LootRefItem(GameConstants.WEAPON_TYPE_3,0.05,1,1));
    }
  };

  /**
   * Returns a reference table for minion battles
   */
  private List<LootRefItem> MinionBattleLootTable = new ArrayList<LootRefItem>() {
    {
      add(new LootRefItem(GameConstants.HELMET_TYPE_1,0.2,1,1));
      add(new LootRefItem(GameConstants.HELMET_TYPE_2,0.05,1,1));
      add(new LootRefItem(GameConstants.BODY_ARMOR_TYPE_1,0.2,1,1));
      add(new LootRefItem(GameConstants.BODY_ARMOR_TYPE_2,0.05,1,1));
      add(new LootRefItem(GameConstants.SHIELD_TYPE_1,0.2,1,1));
      add(new LootRefItem(GameConstants.SHIELD_TYPE_2,0.05,1,1));
      add(new LootRefItem(GameConstants.WEAPON_TYPE_1,0.2,1,1));
      add(new LootRefItem(GameConstants.WEAPON_TYPE_2,0.05,1,1));
    }
  };

  /**
   * Returns a reference table for chests
   */
  private List<LootRefItem> ChestLootTable = new ArrayList<LootRefItem>(){
    {
      add(new LootRefItem(GameConstants.HELMET_TYPE_1,0.15,1,1));
      add(new LootRefItem(GameConstants.HELMET_TYPE_2,0.07,1,1));
      add(new LootRefItem(GameConstants.HELMET_TYPE_3,0.03,1,1));
      add(new LootRefItem(GameConstants.BODY_ARMOR_TYPE_1,0.15,1,1));
      add(new LootRefItem(GameConstants.BODY_ARMOR_TYPE_2,0.07,1,1));
      add(new LootRefItem(GameConstants.BODY_ARMOR_TYPE_3,0.03,1,1));
      add(new LootRefItem(GameConstants.SHIELD_TYPE_1,0.15,1,1));
      add(new LootRefItem(GameConstants.SHIELD_TYPE_2,0.07,1,1));
      add(new LootRefItem(GameConstants.SHIELD_TYPE_3,0.03,1,1));
      add(new LootRefItem(GameConstants.WEAPON_TYPE_1,0.15,1,1));
      add(new LootRefItem(GameConstants.WEAPON_TYPE_2,0.07,1,1));
      add(new LootRefItem(GameConstants.WEAPON_TYPE_3,0.03,1,1));
    }
  };

  /**
   * With chests you get a gold key and 2 random items on the list.
   * @return
   */
  public RewardsData GetRandomMinionBattleRewardsData() throws Exception {
    RewardsData d = new RewardsData();
    d.getItems().add(new Item(GameConstants.GOLD_KEY,1));
    for (int i=0; i<1; i++) {
      d.getItems().add(GetRandomLootItem(MinionBattleLootTable));
    }
    return d;
  }

  /**
   * With chests you get a gold key and 2 random items on the list.
   * @return
   */
  public RewardsData GetRandomGeneralBattleRewardsData() throws Exception {
    RewardsData d = new RewardsData();
    d.getItems().add(new Item(GameConstants.FREED_LEADERS,1));
    for (int i=0; i<2; i++) {
      d.getItems().add(GetRandomLootItem(GeneralBattleLootTable));
    }
    return d;
  }

  /**
   * With chests you get a gold key and 2 random items on the list.
   * @return
   */
  public RewardsData GetRandomChestRewardsData() throws Exception {
    RewardsData d = new RewardsData();
    d.getItems().add(new Item(GameConstants.DIAMOND_KEY,1));
    for (int i=0; i<2; i++) {
      d.getItems().add(GetRandomLootItem(ChestLootTable));
    }
    return d;
  }

  /**
   * Helper function that returns a random item from a given selection, based on the item weight.
   *
   * @param selections
   * @return A new item
   * @throws Exception
   */
  private Item GetRandomLootItem(List<LootRefItem> selections) throws Exception {
    double rand = ThreadLocalRandom.current().nextDouble(0,1);
    float currentProb = 0f;
    for ( LootRefItem lri : selections) {
      currentProb += lri.getWeight();
      if (rand <= currentProb)
        return new Item(lri.getObjectTypeId(),lri.getMinQuantity());
    }
    //will happen if the input's probabilities sums to less than 1
    //throw error here if that's appropriate
    throw new Exception("The sum of all weights from the given loot table is not equal to 1!");
  }

  /**
   * Util function to check if a number is within range.
   * @param x
   * @param lower
   * @param upper
   * @return
   */
  public boolean IsBetween(int x, int lower, int upper) {
    return lower <= x && x <= upper;
  }

}