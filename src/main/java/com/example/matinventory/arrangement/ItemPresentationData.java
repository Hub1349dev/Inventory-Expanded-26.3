package com.example.matinventory.arrangement;

import com.example.matinventory.MatInventoryClient;
import com.example.matinventory.config.MatConfig;
import com.example.matinventory.core.MatTopology;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.WeakHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public final class ItemPresentationData {
   private static JsonObject sizes = new JsonObject();
   private static JsonObject categories = new JsonObject();
   private static List<ItemPresentationData.Compartment> compartments = List.of();
   private static final Map<ItemStack, String> debugUnknown = Collections.synchronizedMap(new WeakHashMap<>());
   public static int generation;

   public static void reload() {
      MatConfig c = MatInventoryClient.config;
      sizes = merged("item_sizes.json", c.itemSizesPath);
      categories = merged("item_categories.json", c.itemCategoriesPath);
      JsonArray data = read("compartments.json", c.compartmentsPath).getAsJsonArray();
      List<ItemPresentationData.Compartment> parsed = parseCompartments(data);
      if (parsed.size() != 8) {
         MatInventoryClient.LOG.warn("Invalid compartments; using the eight bundled compartments");
         parsed = parseCompartments(defaults("compartments.json").getAsJsonArray());
      }

      compartments = List.copyOf(parsed);
      generation++;
   }

   private static List<ItemPresentationData.Compartment> parseCompartments(JsonArray data) {
      ArrayList<ItemPresentationData.Compartment> result = new ArrayList<>();
      EnumSet<ItemCategoryClassifier.Category> used = EnumSet.noneOf(ItemCategoryClassifier.Category.class);

      for (JsonElement value : data) {
         try {
            JsonObject o = value.getAsJsonObject();
            ItemCategoryClassifier.Category category = ItemCategoryClassifier.Category.valueOf(o.get("category").getAsString());
            float x0 = o.get("x0").getAsFloat();
            float x1 = o.get("x1").getAsFloat();
            float z0 = o.get("z0").getAsFloat();
            float z1 = o.get("z1").getAsFloat();
            if (Float.isFinite(x0 + x1 + z0 + z1)
               && !(x1 - x0 < 0.5F)
               && !(z1 - z0 < 0.5F)
               && !(Math.max(Math.max(Math.abs(x0), Math.abs(x1)), Math.max(Math.abs(z0), Math.abs(z1))) > 60.0F)
               && used.add(category)) {
               ItemPresentationData.Compartment r = new ItemPresentationData.Compartment(o.get("id").getAsString(), category, x0, z0, x1, z1);
               if (result.stream().anyMatch(p -> r.x0 < p.x1 && r.x1 > p.x0 && r.z0 < p.z1 && r.z1 > p.z0)) {
                  return List.of();
               }

               result.add(r);
            }
         } catch (RuntimeException var13) {
         }
      }

      try {
         MatTopology.build(result.stream().map(c -> new MatTopology.Rect(c.x0(), c.z0(), c.x1(), c.z1())).toList());
         return result;
      } catch (IllegalArgumentException invalidSilhouette) {
         return List.of();
      }
   }

   private static JsonElement defaults(String name) {
      try (InputStream stream = ItemPresentationData.class.getResourceAsStream("/assets/matinventory/arrangement/" + name)) {
         if (stream == null) {
            throw new IOException("Missing bundled " + name);
         } else {
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
         }
      } catch (Exception ex) {
         throw new IllegalStateException(ex);
      }
   }

   private static JsonElement read(String name, String configured) {
      JsonElement fallback = defaults(name);

      try {
         Path path = FabricLoader.getInstance().getConfigDir().resolve(configured).normalize();
         if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
            Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(fallback));
         }

         try (BufferedReader reader = Files.newBufferedReader(path)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (parsed.isJsonObject() == fallback.isJsonObject() && parsed.isJsonArray() == fallback.isJsonArray()) {
               return parsed;
            }
         }
      } catch (Exception ex) {
         MatInventoryClient.LOG.warn("Cannot read presentation file {}; using bundled defaults", configured, ex);
      }

      return fallback;
   }

   private static JsonObject merged(String name, String path) {
      JsonObject result = defaults(name).getAsJsonObject();
      read(name, path).getAsJsonObject().entrySet().forEach(e -> result.add(e.getKey(), e.getValue()));
      return result;
   }

   public static String id(ItemStack stack) {
      String fake = debugUnknown.get(stack);
      return fake != null ? fake : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
   }

   public static void debugUnknown(ItemStack stack, String id) {
      debugUnknown.put(stack, id);
   }

   public static boolean isDebugUnknown(ItemStack stack) {
      return debugUnknown.containsKey(stack);
   }

   public static ItemPresentationData.Size size(ItemStack stack) {
      try {
         JsonElement e = sizes.get(id(stack));
         if (e != null && !e.isJsonNull()) {
            float cm = e.isJsonPrimitive() ? e.getAsFloat() : e.getAsJsonObject().get("cm").getAsFloat();
            Float angle = e.isJsonObject() && e.getAsJsonObject().has("angle") ? e.getAsJsonObject().get("angle").getAsFloat() : null;
            if (!Float.isFinite(cm)) {
               return null;
            }

            if (angle != null && !Float.isFinite(angle)) {
               angle = null;
            }

            return new ItemPresentationData.Size(Math.clamp(cm, 2.0F, 70.0F), angle);
         } else {
            return null;
         }
      } catch (RuntimeException ex) {
         return null;
      }
   }

   public static ItemCategoryClassifier.Classification category(ItemStack stack) {
      try {
         JsonElement e = categories.get(id(stack));
         return e != null && !e.isJsonNull()
            ? new ItemCategoryClassifier.Classification(
               ItemCategoryClassifier.Category.valueOf(e.isJsonPrimitive() ? e.getAsString() : e.getAsJsonObject().get("category").getAsString()),
               e.isJsonPrimitive() ? "custom" : (e.getAsJsonObject().has("subtype") ? e.getAsJsonObject().get("subtype").getAsString() : "custom")
            )
            : null;
      } catch (RuntimeException ex) {
         return null;
      }
   }

   public static List<ItemPresentationData.Compartment> compartments() {
      return compartments;
   }

   public static ItemPresentationData.Compartment compartment(ItemCategoryClassifier.Category category) {
      for (ItemPresentationData.Compartment c : compartments) {
         if (c.category == category) {
            return c;
         }
      }

      throw new NoSuchElementException();
   }

   public static List<float[]> ledChannels() {
      return ledChannels(compartments);
   }

   public static List<float[]> ledChannels(List<ItemPresentationData.Compartment> areas) {
      ItemPresentationData.Compartment craft = areas.stream().filter(c -> c.category() == ItemCategoryClassifier.Category.CRAFT).findFirst().orElseThrow();
      ItemPresentationData.Compartment resources = areas.stream()
         .filter(c -> c.category() == ItemCategoryClassifier.Category.RESOURCES)
         .findFirst()
         .orElseThrow();
      ItemPresentationData.Compartment tools = areas.stream().filter(c -> c.category() == ItemCategoryClassifier.Category.LONG_TOOLS).findFirst().orElseThrow();
      ItemPresentationData.Compartment blocks = areas.stream().filter(c -> c.category() == ItemCategoryClassifier.Category.BLOCKS).findFirst().orElseThrow();
      ArrayList<float[]> channels = new ArrayList<>(
         List.of(
            new float[]{blocks.x0(), blocks.z0() + 0.35F, blocks.x0(), blocks.z1() - 0.35F},
            new float[]{resources.x0(), resources.z0() + 0.18F, resources.x0(), resources.z1() - 0.18F},
            new float[]{craft.x0(), resources.z0() + 0.18F, craft.x0(), craft.z1() - 0.18F},
            new float[]{craft.x1(), tools.z0() + 0.18F, craft.x1(), craft.z1() - 0.18F},
            new float[]{tools.x1(), tools.z0() + 0.35F, tools.x1(), tools.z1() - 0.35F},
            new float[]{craft.x0() + 0.18F, craft.z1(), craft.x1() - 0.18F, craft.z1()},
            new float[]{craft.x0() + 0.18F, craft.z0(), craft.x1() - 0.18F, craft.z0()}
         )
      );

      for (ItemPresentationData.Compartment area : areas) {
         if (area.id().equals("extra_tools_trident")) {
            channels.add(new float[]{area.x0() + 0.18F, area.z1(), area.x1() - 0.18F, area.z1()});
         }

         if (area.category() == ItemCategoryClassifier.Category.VEHICLES) {
            for (ItemPresentationData.Compartment food : areas) {
               if (food.category() == ItemCategoryClassifier.Category.FOOD && Math.abs(area.z1() - food.z0()) < 0.001F) {
                  float left = Math.max(area.x0(), food.x0()) + 0.18F;
                  float right = Math.min(area.x1(), food.x1()) - 0.18F;
                  if (right > left) {
                     channels.add(new float[]{left, food.z0(), right, food.z0()});
                  }
               }
            }
         }
      }

      return channels;
   }

   private ItemPresentationData() {
   }

   @Environment(EnvType.CLIENT)
   public record Compartment(String id, ItemCategoryClassifier.Category category, float x0, float z0, float x1, float z1) {
      public boolean contains(float x, float z) {
         return x >= this.x0 && x <= this.x1 && z >= this.z0 && z <= this.z1;
      }

      public float width() {
         return this.x1 - this.x0;
      }

      public float depth() {
         return this.z1 - this.z0;
      }
   }

   @Environment(EnvType.CLIENT)
   public record Size(float cm, Float angle) {
   }
}
