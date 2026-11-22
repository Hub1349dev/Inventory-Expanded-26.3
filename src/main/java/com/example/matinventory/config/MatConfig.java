package com.example.matinventory.config;

import com.example.matinventory.capacity.EquipmentKinds;
import com.example.matinventory.core.ToolOrder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class MatConfig {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("matinventory.json");
   public boolean enabled = true;
   public boolean darkUiButtons = false;
   public MatConfig.CraftFocusEffect craftFocusEffect = MatConfig.CraftFocusEffect.VIGNETTE;
   public Map<String, Boolean> inventorySounds = new HashMap<>();
   public int visualVersion = 12;
   public float parallax = 0.12F;
   public float cameraPitch = 58.0F;
   public float cameraFov = 45.0F;
   public float helpOpacity = 0.6F;
   public boolean showHelp = true;
   public int supersampling = 1;
   public float glow = 0.75F;
   public int ledColor = 0;
   public boolean ledEnabled = false;
   public int matColor = 0;
   public boolean gridEnabled = true;
   public Map<String, Float> inventoryScales = new HashMap<>();
   public String placementProfile = "";
   public Map<String, Map<String, MatConfig.DetachedItem>> placementProfiles = new HashMap<>();
   public boolean craftingTable = true;
   public float worldUnitsPerCm = 0.12F;
   public float itemSizeMultiplier = 1.0F;
   public String arrangeMode = "CATEGORIZED";
   public List<String> toolOrder = new ArrayList<>(ToolOrder.DEFAULT);
   public List<String> consumableOrder = new ArrayList<>();
   public List<String> materialOrder = new ArrayList<>();
   public List<String> blockOrder = new ArrayList<>();
   public List<String> vehicleOrder = new ArrayList<>();
   public Map<String, float[]> flexiblePositions = new HashMap<>();
   public Map<String, MatConfig.DetachedItem> detachedItems = new HashMap<>();
   public List<String> equipmentOrder = new ArrayList<>(EquipmentKinds.DEFAULT);
   public Map<String, List<String>> equipmentPresentation = new HashMap<>();
   public float compartmentPadding = 0.1F;
   public float itemSpacing = 0.1F;
   public float reflowAnimationSeconds = 0.2F;
   public String itemSizesPath = "matinventory/item_sizes.json";
   public String itemCategoriesPath = "matinventory/item_categories.json";
   public String compartmentsPath = "matinventory/compartments-v6.json";
   public boolean groundEnabled = true;
   public boolean groundAlignToYaw = true;
   public boolean worldLighting = true;
   public boolean debugOverlay = false;
   public float groundBlockSizeCm = 10.0F;
   public float tarpCoverage = 0.86F;
   public float minBrightness = 0.3F;
   public float exposure = 1.6F;
   public float saturation = 0.72F;
   public float scaleGamma = 0.6F;
   public float minItemSizeFrac = 0.024F;
   public int groundRadius = 6;
   public String countDisplay = "TAG";
   public float itemRotationJitterDeg = 18.0F;
   public float railHeightCm = 1.8F;
   public String tarpColorMode = "KHAKI";

   public static MatConfig load() {
      MatConfig config = new MatConfig();
      if (Files.exists(FILE)) {
         try (BufferedReader reader = Files.newBufferedReader(FILE)) {
            MatConfig parsed = GSON.fromJson(reader, MatConfig.class);
            if (parsed != null) {
               config = parsed;
            }
         } catch (Exception ex) {
            LoggerFactory.getLogger("matinventory").warn("Invalid config; using defaults", ex);
         }
      }

      if (config.craftFocusEffect == null) {
         config.craftFocusEffect = MatConfig.CraftFocusEffect.VIGNETTE;
      }

      if (config.inventorySounds == null) {
         config.inventorySounds = new HashMap<>();
      }

      config.inventorySounds.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);
      if (config.equipmentPresentation == null) {
         config.equipmentPresentation = new HashMap<>();
      }

      if (config.flexiblePositions == null) {
         config.flexiblePositions = new HashMap<>();
      }

      if (config.detachedItems == null) {
         config.detachedItems = new HashMap<>();
      }

      if (config.inventoryScales == null) {
         config.inventoryScales = new HashMap<>();
      }

      config.inventoryScales
         .entrySet()
         .removeIf(e -> e.getKey() == null || e.getValue() == null || !Float.isFinite(e.getValue()) || e.getValue() < -200.0F || e.getValue() > 200.0F);
      if (config.placementProfiles == null) {
         config.placementProfiles = new HashMap<>();
      }

      config.placementProfiles.entrySet().removeIf(e -> e.getValue() == null);

      for (Map<String, MatConfig.DetachedItem> profile : config.placementProfiles.values()) {
         profile.entrySet().removeIf(e -> e.getValue() == null || e.getValue().item == null || !Float.isFinite(e.getValue().x + e.getValue().z));
      }

      if (config.placementProfile == null) {
         config.placementProfile = "";
      }

      config.detachedItems.entrySet().removeIf(e -> e.getValue() == null || e.getValue().item == null || !Float.isFinite(e.getValue().x + e.getValue().z));
      config.flexiblePositions.entrySet().removeIf(e -> e.getValue() == null || e.getValue().length != 2 || !Float.isFinite(e.getValue()[0] + e.getValue()[1]));
      if (config.visualVersion < 3) {
         config.visualVersion = 3;
         config.supersampling = 2;
      }

      if (config.visualVersion < 4) {
         config.visualVersion = 4;
         config.compartmentsPath = "matinventory/compartments-v2.json";
         config.parallax = 0.08F;
      }

      if (config.visualVersion < 5) {
         config.visualVersion = 5;
         config.exposure = 1.32F;
      }

      if (config.visualVersion < 6) {
         config.visualVersion = 6;
         config.cameraPitch = 70.0F;
         if (config.exposure == 1.32F) {
            config.exposure = 1.6F;
         }

         if ("matinventory/compartments-v2.json".equals(config.compartmentsPath)) {
            config.compartmentsPath = "matinventory/compartments-v3.json";
         }
      }

      if (config.visualVersion < 7) {
         config.visualVersion = 7;
         if ("matinventory/compartments-v3.json".equals(config.compartmentsPath)) {
            config.compartmentsPath = "matinventory/compartments-v4.json";
         }
      }

      if (config.visualVersion < 8) {
         config.visualVersion = 8;
         if ("matinventory/compartments-v4.json".equals(config.compartmentsPath)) {
            config.compartmentsPath = "matinventory/compartments-v5.json";
         }
      }

      if (config.visualVersion < 9) {
         config.visualVersion = 9;
         if ("matinventory/compartments-v5.json".equals(config.compartmentsPath)) {
            config.compartmentsPath = "matinventory/compartments-v6.json";
         }
      }

      if (config.visualVersion < 10) {
         config.visualVersion = 10;
         config.cameraPitch = 58.0F;
         config.cameraFov = 45.0F;
      }

      if (config.visualVersion < 11) {
         config.visualVersion = 11;
         config.supersampling = 1;
      }

      if (config.visualVersion < 12) {
         config.visualVersion = 12;
         config.cameraPitch = 58.0F;
         config.cameraFov = 45.0F;
      }

      config.ledColor = Math.clamp(config.ledColor, 0, 4);
      config.matColor = Math.clamp(config.matColor, 0, 5);
      config.groundRadius = Math.clamp(config.groundRadius, 2, 12);
      config.itemRotationJitterDeg = finiteClamp(config.itemRotationJitterDeg, 0.0F, 25.0F, 18.0F);
      config.railHeightCm = finiteClamp(config.railHeightCm, 1.5F, 2.0F, 1.8F);
      if (!"BROWN".equals(config.tarpColorMode) && !"NEUTRAL".equals(config.tarpColorMode)) {
         config.tarpColorMode = "KHAKI";
      }

      config.groundBlockSizeCm = finiteClamp(config.groundBlockSizeCm, 5.0F, 30.0F, 10.0F);
      config.tarpCoverage = finiteClamp(config.tarpCoverage, 0.65F, 0.9F, 0.86F);
      config.minBrightness = finiteClamp(config.minBrightness, 0.2F, 0.65F, 0.3F);
      config.exposure = finiteClamp(config.exposure, 0.4F, 1.6F, 1.6F);
      config.saturation = finiteClamp(config.saturation, 0.0F, 1.2F, 0.72F);
      config.scaleGamma = finiteClamp(config.scaleGamma, 0.45F, 0.8F, 0.6F);
      config.minItemSizeFrac = finiteClamp(config.minItemSizeFrac, 0.018F, 0.04F, 0.024F);
      if (!"HOVER".equals(config.countDisplay) && !"OFF".equals(config.countDisplay)) {
         config.countDisplay = "TAG";
      }

      config.parallax = finiteClamp(config.parallax, 0.0F, 0.35F, 0.12F);
      config.cameraPitch = finiteClamp(config.cameraPitch, 54.0F, 60.0F, 58.0F);
      config.cameraFov = finiteClamp(config.cameraFov, 41.0F, 49.0F, 45.0F);
      config.helpOpacity = finiteClamp(config.helpOpacity, 0.0F, 1.0F, 0.6F);
      config.supersampling = Math.clamp(config.supersampling, 1, 2);
      config.glow = finiteClamp(config.glow, 0.0F, 1.0F, 0.75F);
      config.worldUnitsPerCm = finiteClamp(config.worldUnitsPerCm, 0.04F, 0.3F, 0.12F);
      config.itemSizeMultiplier = finiteClamp(config.itemSizeMultiplier, 0.25F, 3.0F, 1.0F);
      config.compartmentPadding = finiteClamp(config.compartmentPadding, 0.03F, 0.6F, 0.1F);
      config.itemSpacing = finiteClamp(config.itemSpacing, 0.02F, 0.8F, 0.1F);
      config.reflowAnimationSeconds = finiteClamp(config.reflowAnimationSeconds, 0.0F, 1.0F, 0.2F);
      if (!"SLOT_ORDER".equals(config.arrangeMode)) {
         config.arrangeMode = "CATEGORIZED";
      }

      if (config.itemSizesPath == null || config.itemSizesPath.isBlank()) {
         config.itemSizesPath = "matinventory/item_sizes.json";
      }

      if (config.itemCategoriesPath == null || config.itemCategoriesPath.isBlank()) {
         config.itemCategoriesPath = "matinventory/item_categories.json";
      }

      if (config.compartmentsPath == null || config.compartmentsPath.isBlank()) {
         config.compartmentsPath = "matinventory/compartments-v6.json";
      }

      config.toolOrder = new ArrayList<>(new ToolOrder(config.toolOrder).keys());
      config.save();
      return config;
   }

   private static float finiteClamp(float value, float min, float max, float fallback) {
      return Float.isFinite(value) ? Math.clamp(value, min, max) : fallback;
   }

   public void save() {
      try {
         Files.createDirectories(FILE.getParent());
         Files.writeString(FILE, GSON.toJson(this));
      } catch (Exception ex) {
         LoggerFactory.getLogger("matinventory").warn("Cannot save config", ex);
      }
   }

   @Environment(EnvType.CLIENT)
   public enum CraftFocusEffect {
      VIGNETTE,
      CINEMA_BARS,
      OFF;

      public MatConfig.CraftFocusEffect next() {
         return values()[(this.ordinal() + 1) % values().length];
      }
   }

   @Environment(EnvType.CLIENT)
   public static final class DetachedItem {
      public Boolean freePosition;
      public Boolean independent;
      public Float presentationScale;
      public String item;
      public float x;
      public float z;
      public String gridCell;

      public DetachedItem(String item, float x, float z) {
         this.item = item;
         this.x = x;
         this.z = z;
      }
   }
}
