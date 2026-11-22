package com.example.matinventory.capacity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;

public final class CapacityConfig {
   public static final int MAX_STORAGE = 256;
   public static final int EXTRA_STORAGE = 229;
   public static final int EXTRA_SLOTS = 234;
   public static final int TOOL_LIMIT = 3;
   public static final int VEHICLE_LIMIT = 3;
   public static final int BLOCK_TYPE_LIMIT = 16;
   public static final int CONSUMABLE_TYPE_LIMIT = 14;
   public static final int MATERIAL_TYPE_LIMIT = 25;
   public static int storageSlots = 144;

   public static void load() {
      Path file = FabricLoader.getInstance().getConfigDir().resolve("matinventory-capacity.json");

      try {
         if (Files.exists(file)) {
            JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            if (json.has("storageSlots")) {
               storageSlots = Math.clamp(json.get("storageSlots").getAsInt(), 27, 256);
            }
         } else {
            Files.writeString(file, "{\n  \"storageSlots\": 144\n}\n");
         }
      } catch (Exception ex) {
         LoggerFactory.getLogger("matinventory").warn("Cannot read capacity config; using 144 storage slots", ex);
      }
   }

   private CapacityConfig() {
   }
}
