﻿using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using Google.Maps;
using Google.Maps.Coord;
using Google.Maps.Examples;
using Google.Maps.Examples.Shared;
using Google.Maps.Feature.Style;
using Unity.Collections;
using UnityEngine;
using UnityEngine.Assertions;
using UnityEngine.Events;

namespace Google.Maps.Demos.Zoinkies {

  [Serializable] public class StringEvent : UnityEvent<string> { }

  /// <summary>
  /// This class initializes reference, player, map data.
  /// It also manages the creation and maintenance of spawned game objects.
  ///
  /// </summary>
  public class WorldController : BaseMapLoader {

    #region properties

    /// <summary>
    /// Dispatched to the game when Reference Data, World Data and Player Data
    /// are initialized.
    /// </summary>
    public UnityEvent GameReady;

    /// <summary>
    /// Dispatched to the game when some loading errors occur.
    /// </summary>
    public StringEvent GameLoadingError;

    /// <summary>
    /// Reference to the main camera
    /// </summary>
    public Camera mainCamera;

    /// <summary>
    /// Reference to the battle camera
    /// </summary>
    //public Camera battleCamera;

    /// <summary>
    /// Reference to the server Manager, responsible for all REST calls.
    /// </summary>
    public ServerManager ServerManager;

    /// <summary>
    /// Convenient container to hold all spawned locations for quick access.
    /// </summary>
    //public Transform PlayableLocationsContainer;

    /*
    /// <summary>
    /// Tower prefab
    /// </summary>
    public GameObject TowerPrefab;

    /// <summary>
    /// Minion prefab
    /// </summary>
    public GameObject MinionPrefab;

    /// <summary>
    /// Chest prefab
    /// </summary>
    public GameObject ChestPrefab;

    /// <summary>
    /// Energy station prefab
    /// </summary>
    public GameObject EnergyStationPrefab;
    */

    /// <summary>
    /// Reference to avatar
    /// </summary>
    public GameObject Avatar;

    /// <summary>
    /// Reference to ground material
    /// </summary>
    public Material GroundMaterial;

    /// <summary>
    /// Reference to roads material
    /// </summary>
    public Material RoadsMaterial;

    /// <summary>
    /// Reference to building walls material
    /// </summary>
    public Material BuildingsWallMaterial;

    /// <summary>
    /// Reference to building roof material
    /// </summary>
    public Material BuildingsRoofMaterial;

    /// <summary>
    ///   Distance inside which buildings will be completely squashed (<see cref="MaximumSquash" />)
    /// </summary>
    public float SquashNear = 50;

    /// <summary>
    ///   Distance outside which buildings will not be squashed.
    /// </summary>
    public float SquashFar = 200;

    /// <summary>
    ///   The vertical scaling factor applied at maximum squashing.
    /// </summary>
    public float MaximumSquash = 0.1f;

    /// <summary>
    /// Indicates if we have acquired a GPS Location
    /// </summary>
    [ReadOnly] [SerializeField] private bool hasGPSLocation;

    /// <summary>
    /// Indicates if the floating origin has been set
    /// </summary>
    private bool FloatingOriginIsSet;

    /// <summary>
    ///   Keeps track of game objects already instantiated.
    ///   We only display game objects within a certain range.
    /// </summary>
    //private Dictionary<string, GameObject> SpawnedGameObjects;

    /// <summary>
    /// Reference to the styles options applied to loaded map features
    /// </summary>
    private GameObjectOptions ZoinkiesStylesOptions;

    /// <summary>
    /// Keeps track of startup milestones
    /// </summary>
    private List<String> StartupCheckList;

    /// <summary>
    /// Setup milestone: Playable Locations loaded and initialized.
    /// </summary>
    //private static string PLAYABLE_LOCATIONS_INITIALIZED = "PLAYABLE_LOCATIONS_INITIALIZED";

    /// <summary>
    /// Setup milestone: Reference data loaded and initialized.
    /// </summary>
    private static string REFERENCE_DATA_INITIALIZED = "REFERENCE_DATA_INITIALIZED";

    /// <summary>
    /// Setup milestone: Player data loaded and initialized.
    /// </summary>
    private static string PLAYER_DATA_INITIALIZED = "PLAYER_DATA_INITIALIZED";

    /// <summary>
    /// Setup milestone: Map data loaded and initialized.
    /// </summary>
    private static string MAP_INITIALIZED = "MAP_INITIALIZED";

    /// <summary>
    /// Indicates if the game has started
    /// </summary>
    private bool GameStarted;

    //private bool WorldDataIsLoading;

    #endregion


    private void CheckStartConditions() {
      if (StartupCheckList.Count == 0 && !GameStarted) {
        GameReady?.Invoke();
        GameStarted = true;
      }
    }


    /// <summary>
    /// Sets up the setup milestones list.
    /// Loads reference data and player data.
    /// </summary>
    private void Awake() {


      // Initialize start up check list
      StartupCheckList = new List<string> {
        //PLAYABLE_LOCATIONS_INITIALIZED,
        REFERENCE_DATA_INITIALIZED,
        PLAYER_DATA_INITIALIZED,
        MAP_INITIALIZED
      };

      // Load and initialize Reference Data
      ReferenceService.GetInstance().Init(ServerManager.GetReferenceData());
      StartupCheckList.Remove(REFERENCE_DATA_INITIALIZED);

      // Load and initialize Player Data
      PlayerService.GetInstance().Init(ServerManager.GetPlayerData());
      StartupCheckList.Remove(PLAYER_DATA_INITIALIZED);

      /*
      StartCoroutine(ServerManager.GetReferenceData(data => {
        ReferenceService.GetInstance().Init(data);
        StartupCheckList.Remove(REFERENCE_DATA_INITIALIZED);
        CheckStartConditions();
      }, OnError));
      */


      /*
      // Load and initialize Player Data
      StartCoroutine(ServerManager.GetPlayerData(data => {
        PlayerService.GetInstance().Init(data);
        StartupCheckList.Remove(PLAYER_DATA_INITIALIZED);
        CheckStartConditions();
      }, OnError));
      */

      CheckStartConditions();

    }

    /// <summary>
    /// Performs the initial Map load
    /// </summary>
    protected override void Start() {

      Assert.IsNotNull(mainCamera);
      //Assert.IsNotNull(battleCamera);
      //Assert.IsNotNull(PlayableLocationsContainer);
      Assert.IsNotNull(ServerManager);
      Assert.IsNotNull(Avatar);
      base.Start();

      //SpawnedGameObjects = new Dictionary<string, GameObject>();

      // Load Initial Map
      LoadMap();
    }

    /*
    /// <summary>
    ///   This function performs a sync of important data with the server.
    ///   Althought the client is making local game changes for usability reasons
    ///   the server is the ultimate authority for player stats and inventory
    ///   as well as spawn locations states.
    /// </summary>
    public void SyncData() {
      Debug.Log("SyncData+++");

      if (PlayerService.GetInstance().DataHasChanged) {
        StartCoroutine(ServerManager.PostPlayerData(PlayerService.GetInstance().data,
          data => { PlayerService.GetInstance().Init(data); }, OnError));
      }
    }
    */

/*
    public override void LoadMap() {
      // LoadMap is called from Dynamic updater
      // Get our new GPS coordinates and use these to load the map
      // We don't need to update the floating origin all the time.
      //StartCoroutine(GetGPSLocation(OnLocationServicesEvalComplete));

      base.LoadMap();
    }
    */

    /*
    /// <summary>
    ///   Sets the lat lng to our current position is the GPS is enabled
    ///   Otherwise use the default position, which is currently the Googleplex
    /// in Mountain View, CA
    /// </summary>
    protected IEnumerator GetGPSLocation(Action<LocationInfo> onInitComplete) {
      Assert.IsNotNull(onInitComplete);

      // Invalidate the previous position
      hasGPSLocation = false;
      var locInfo = new LocationInfo();

      if (Input.location.isEnabledByUser) {
        Debug.Log("Location services enabled");

        // Start service before querying location
        Input.location.Start();

        // Wait until service initializes
        var maxWait = 20;
        while (Input.location.status == LocationServiceStatus.Initializing && maxWait > 0) {
          yield return new WaitForSeconds(1);
          maxWait--;
        }

        // Service didn't initialize in 20 seconds
        if (maxWait < 1) {
          Debug.Log("Timed out");
          onInitComplete.Invoke(locInfo);
          yield break;
        }

        // Connection has failed
        if (Input.location.status == LocationServiceStatus.Failed) {
          Debug.Log("Unable to determine device location");
          // Failed to get
          onInitComplete.Invoke(locInfo);
          yield break;
        }

        locInfo = Input.location.lastData;
        hasGPSLocation = true;

        // Access granted and location value could be retrieved
        Debug.Log("Location: " + Input.location.lastData.latitude + " " +
                  Input.location.lastData.longitude +
                  " " +
                  Input.location.lastData.altitude + " " +
                  Input.location.lastData.horizontalAccuracy + " " +
                  Input.location.lastData.timestamp);

        // Success - locInfo is set
        onInitComplete.Invoke(locInfo);
      }
      else {
        Debug.Log("Location services not enabled");
        onInitComplete.Invoke(locInfo);
      }
    }
    */

    /// <summary>
    /// Initializes the style options for this game, by setting materials to roads, buildings
    /// and water areas.
    ///
    /// </summary>
    protected override void InitStylingOptions() {
      ZoinkiesStylesOptions = ExampleDefaults.DefaultGameObjectOptions;

      // The default maps shader has a glossy property that allows the sky to reflect on it. Cool.
      Material waterMaterial = ExampleDefaults.DefaultGameObjectOptions.RegionStyle.FillMaterial;
      waterMaterial.color = new Color(0.4274509804f, 0.7725490196f, 0.8941176471f);

      ZoinkiesStylesOptions.ExtrudedStructureStyle = new ExtrudedStructureStyle.Builder {
        RoofMaterial = BuildingsRoofMaterial,
        WallMaterial = BuildingsWallMaterial
      }.Build();

      ZoinkiesStylesOptions.ModeledStructureStyle = new ModeledStructureStyle.Builder {
        Material = BuildingsWallMaterial
      }.Build();

      ZoinkiesStylesOptions.RegionStyle = new RegionStyle.Builder {
        FillMaterial = GroundMaterial
      }.Build();

      ZoinkiesStylesOptions.AreaWaterStyle = new AreaWaterStyle.Builder {
        FillMaterial = waterMaterial
      }.Build();

      ZoinkiesStylesOptions.LineWaterStyle = new LineWaterStyle.Builder {
        Material = waterMaterial
      }.Build();

      ZoinkiesStylesOptions.SegmentStyle = new SegmentStyle.Builder {
        Material = RoadsMaterial
      }.Build();

      if (RenderingStyles == null) RenderingStyles = ZoinkiesStylesOptions;
    }

    /// <summary>
    /// Adds some squashing behavior to all extruded structures.
    /// Basically, we squash everything around our Avatar so that generated game items can be seen
    /// from a distance.
    /// </summary>
    protected override void InitEventListeners() {
      base.InitEventListeners();

      if (MapsService == null) return;

      // Apply a post-creation listener that adds the squashing MonoBehaviour to each building.
      MapsService.Events.ExtrudedStructureEvents.DidCreate.AddListener(
        e => { AddSquasher(e.GameObject); });

      // Apply a post-creation listener that adds the squashing MonoBehaviour to each building.
      MapsService.Events.ModeledStructureEvents.DidCreate.AddListener(
        e => { AddSquasher(e.GameObject); });

      MapsService.Events.MapEvents.Loaded.AddListener(arg0 => {
        StartupCheckList.Remove(MAP_INITIALIZED);
        CheckStartConditions();
      });
    }

    /*
    /// <summary>
    ///   Loads all playable locations within a round area centered on the player's GPS position
    /// </summary>
    /// <param name="currentPosition"></param>
    /// <param name="radius"></param>
    private void UpdateWorldData(LatLng currentPosition, float distance) {
      if (!WorldDataIsLoading) {
        WorldDataIsLoading = true;

        var center = MapsService.Coords.FromLatLngToVector3(currentPosition);

        var NorthEastCorner = center + new Vector3(distance, 0f, distance);
        var NorthEastLatLng = MapsService.Coords.FromVector3ToLatLng(NorthEastCorner);

        var SouthWestCorner = center + new Vector3(-distance, 0f, -distance);
        var SouthWestLatLng = MapsService.Coords.FromVector3ToLatLng(SouthWestCorner);

        var wdr = new WorldDataRequest();
        wdr.CopyFrom(SouthWestLatLng, NorthEastLatLng);

        StartCoroutine(ServerManager.PostWorldData(wdr, OnWorldDataLoaded, OnError));
      }
    }
    */


    /*
    /// <summary>
    /// Creates all new spawn locations for the game from the World Data provided by the server.
    /// </summary>
    /// <param name="wd"></param>
    private void CreateNewLocations(WorldData wd) {
      // Render the new data on the map
      Debug.Log("OnWorldDataLoaded+++ " + wd.locations.Count);

      // Only render the playable locations that are within range of the loaded map.
      // The API returns all locations within the overlapping cells - which may be way larger
      // than our map.
      CreateAssets(WorldService.GetInstance().GetMinions(),
        MinionPrefab,
        PlayableLocationsContainer);
      CreateAssets(WorldService.GetInstance().GetTowers(), TowerPrefab, PlayableLocationsContainer);
      CreateAssets(WorldService.GetInstance().GetChests(), ChestPrefab, PlayableLocationsContainer);
      CreateAssets(WorldService.GetInstance().GetEnergyStations(),
        EnergyStationPrefab,
        PlayableLocationsContainer);
    }

    /// <summary>
    /// This helper class creates all gameobjects based on the given collection of spawn locations.
    ///
    /// </summary>
    /// <param name="collection">A collection of spawn locations</param>
    /// <param name="prefab">The prefab to instantiate for each location</param>
    /// <param name="container">The container that holds all created gameobjects</param>
    /// <returns></returns>
    private int CreateAssets(IEnumerable<SpawnLocation> collection,
      GameObject prefab,
      Transform container) {
      var numberOfObjectsCreated = 0;
      foreach (var loc in collection)
        if (loc.snappedPoint != null) {
          var pos = MapsService.Coords.FromLatLngToVector3(
            new LatLng(loc.snappedPoint.latitude, loc.snappedPoint.longitude));
          // Do we already have this object in our scene?
          if (!SpawnedGameObjects.ContainsKey(loc.id)) {
            var go =
              Instantiate(prefab, container);
            go.transform.position = pos;

            // The reference to placeId allows us to find the associated data
            // through WorldService
            go.name = loc.id;

            BaseSpawnLocationController sl = go.GetComponent<BaseSpawnLocationController>();
            Assert.IsNotNull(sl);
            sl.Init(loc.id);

            SpawnedGameObjects.Add(loc.id, go);
            numberOfObjectsCreated++;
          }
        }

      return numberOfObjectsCreated;
    }
    */

    #region event listeners

    /// <summary>
    /// Triggered by the UI when a new game needs to be created.
    /// This event listener resets player and world data.
    ///
    /// </summary>
    public void OnNewGame() {
      Debug.Log("OnNewGame+++");

      //this.SpawnedGameObjects.Clear();

      // Clear the world
      //ClearContainer(PlayableLocationsContainer);

      GameStarted = false;
      StartupCheckList = new List<string> {PLAYER_DATA_INITIALIZED, MAP_INITIALIZED};

      //PlayerService.GetInstance().data = new PlayerData();
      // Load and initialize Player Data
      PlayerService.GetInstance().Init(ServerManager.GetPlayerData());
      StartupCheckList.Remove(PLAYER_DATA_INITIALIZED);

      //WorldService.GetInstance().data = new WorldData();
      // Reload Maps
      LoadMap();

      /*
      // Maps data stay the same (we are at the same location)PLAYER_DATA_INITIALIZED,
      StartupCheckList = new List<string> {PLAYER_DATA_INITIALIZED};

      // Reset Player data

      StartCoroutine(ServerManager.PostPlayerData(null, data => {
        PlayerService.GetInstance().data = data;
        StartupCheckList.Remove(PLAYER_DATA_INITIALIZED);
        CheckStartConditions();
      }, s => { Debug.LogError(s); }));
      */

      /*
      StartCoroutine(ServerManager.DeleteWorldData(data => {
        // Generate new world
        UpdateWorldData(LatLng, MaxDistance);
      }, s => { Debug.LogError(s); }));
      */
    }

    /// <summary>
    /// Stops the GPS location service when the app is disabled (to save battery)
    /// </summary>
    private void OnDisable() {
      if (Input.location.status == LocationServiceStatus.Running)
        Input.location.Stop();
    }

    /*
    private void OnLocationServicesEvalComplete(LocationInfo locInfo) {
      if (hasGPSLocation) // aka we were able to get a valid GPS location
      {
        LatLng = new LatLng(locInfo.latitude, locInfo.longitude);
        if (!FloatingOriginIsSet) {
          // Reset the floating origin
          InitFloatingOrigin();
          FloatingOriginIsSet = true;
        }
      }

      base.LoadMap();
      //UpdateWorldData(LatLng, MaxDistance);
    }
    */

    public void OnShowWorld() {
      Debug.Log("OnShowWorld+++");
      Avatar.gameObject.SetActive(true);
      GetComponent<DynamicMapsUpdater>().enabled = true;

      mainCamera.enabled = true;
      //battleCamera.enabled = false;

      // Good time to do a server sync
      //SyncData();
    }

    /*
    public void OnShowBattleground() {
      Debug.Log("OnShowBattleground+++");
      Avatar.gameObject.SetActive(false);
      GetComponent<DynamicMapsUpdater>().enabled = false;

      mainCamera.enabled = false;
      //battleCamera.enabled = true;
    }
    */

    public void OnMapLoadStart() {
      // We've moved enough to restart a new map

      // Get all game objects near our avatar (us)
      // Note that we position the camera at the current GPS coordinates, so the Avatar position is slightly offset.
      // At start, the Avatar is positioned at the origin of the world space, which coincide with the floating origin lat lng.
      //var avatarLatLng = MapsService.Coords.FromVector3ToLatLng(Avatar.transform.position);
      //UpdateWorldData(avatarLatLng, MaxDistance);
    }

    /*
    private void OnWorldDataLoaded(WorldData wd) {
      // Init the playerservice with the new batch of data
      WorldService.GetInstance().Init(wd);

      // Init gameobjects on the map
      var keysFromWorldData = WorldService.GetInstance().GetSpawnLocationsIds();

      // Delete all gameobjects not on the new list
      var deletedEntries = 0;
      var missingKeysFromLocalCache =
        new List<string>(SpawnedGameObjects.Keys.Except(keysFromWorldData));
      foreach (var k in missingKeysFromLocalCache) {
        Destroy(SpawnedGameObjects[k].gameObject);
        SpawnedGameObjects.Remove(k);
        deletedEntries++;
      }

      Debug.Log("Deleted " + deletedEntries);

      // Add all new locations
      CreateNewLocations(wd);

      // Show/hide objects based on distance from avatar
      foreach (var k in SpawnedGameObjects.Keys) {
        var sl = WorldService.GetInstance().GetSpawnLocation(k);
        var pos = MapsService.Coords.FromLatLngToVector3(
          new LatLng(sl.snappedPoint.latitude, sl.snappedPoint.longitude));

        SpawnedGameObjects[k].SetActive(Vector3.Distance(Avatar.transform.position, pos) <=
                                        MaxDistance);
      }

      StartupCheckList.Remove(PLAYABLE_LOCATIONS_INITIALIZED);
      CheckStartConditions();

      WorldDataIsLoading = false;
    }
    */

    /*
    private void OnError(string errMsg) {
      Debug.LogError(errMsg);
      //if (WorldDataIsLoading) {
      //  WorldDataIsLoading = false;
      //}
      GameLoadingError?.Invoke(errMsg);
    }
    */

    #endregion

    #region utils

    /*
    /// <summary>
    /// Clears all children of a transform
    /// </summary>
    /// <param name="container"></param>
    private void ClearContainer(Transform container) {
      foreach (Transform child in container) Destroy(child.gameObject);
    }
    */

    /// <summary>
    ///   Adds a Squasher MonoBehaviour to the supplied GameObject.
    /// </summary>
    /// <remarks>
    ///   The Squasher MonoBehaviour reduced the vertical scale of the GameObject's transform
    ///   when a building object is nearby.
    /// </remarks>
    /// <param name="go">The GameObject to which to add the Squasher behaviour.</param>
    private void AddSquasher(GameObject go) {
      var squasher = go.AddComponent<Squasher>();
      squasher.Target = Avatar.transform;
      squasher.Near = SquashNear;
      squasher.Far = SquashFar;
      squasher.MaximumSquashing = MaximumSquash;
    }

    #endregion
  }
}