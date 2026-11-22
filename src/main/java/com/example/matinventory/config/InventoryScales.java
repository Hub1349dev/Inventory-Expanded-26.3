package com.example.matinventory.config;

import com.example.matinventory.MatInventoryClient;
import com.example.matinventory.arrangement.ModelVariant;
import com.example.matinventory.render.MatMaterials;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public final class InventoryScales {
   public static long revision;
   private static final Map<Identifier, String> packs = new HashMap<>();
   private static int generation = -1;
   private static Map<String, Float> indexedValues;
   private static long indexedRevision = -1L;
   private static final Set<String> variantItems = new HashSet<>();

   public static float multiplier(float percent) {
      return percent < 0.0F ? 1.0F / (1.0F - percent / 100.0F) : 1.0F + percent / 100.0F;
   }

   public static String itemKey(ItemStack stack) {
      return "item:" + BuiltInRegistries.ITEM.getKey(stack.getItem()) + ModelVariant.key(stack);
   }

   public static float factor(ItemStack stack) {
      if (!stack.isEmpty() && MatInventoryClient.config != null) {
         Map<String, Float> values = MatInventoryClient.config.inventoryScales;
         Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
         if (values.isEmpty()) {
            return 1.0F;
         }

         if (indexedValues != values || indexedRevision != revision) {
            indexedValues = values;
            indexedRevision = revision;
            variantItems.clear();

            for (String key : values.keySet()) {
               int split = key.indexOf(124);
               if (key.startsWith("item:") && split > 5) {
                  variantItems.add(key.substring(0, split));
               }
            }
         }

         String base = "item:" + id;
         Float percent = variantItems.contains(base) ? values.get(itemKey(stack)) : null;
         if (percent == null) {
            percent = values.get(base);
         }

         if (percent == null && values.keySet().stream().anyMatch(k -> k.startsWith("pack:"))) {
            Identifier model = id;
            if (generation != MatMaterialsGeneration()) {
               generation = MatMaterialsGeneration();
               packs.clear();
            }

            String pack = packs.computeIfAbsent(
               model,
               key -> Minecraft.getInstance()
                  .getResourceManager()
                  .getResource(key.withPath(p -> "models/item/" + p + ".json"))
                  .map(r -> r.sourcePackId())
                  .orElse("")
            );
            percent = values.get("pack:" + pack);
         }

         if (percent == null) {
            percent = values.get(id.getNamespace());
         }

         return percent == null ? 1.0F : multiplier(percent);
      } else {
         return 1.0F;
      }
   }

   private static int MatMaterialsGeneration() {
      return MatMaterials.generation;
   }

   public static void set(String key, float percent) {
      if (Float.isFinite(percent) && !(percent < -200.0F) && !(percent > 200.0F)) {
         if (percent == 0.0F && !key.startsWith("item:")) {
            MatInventoryClient.config.inventoryScales.remove(key);
         } else {
            MatInventoryClient.config.inventoryScales.put(key, percent);
         }

         revision++;
         packs.clear();
         MatInventoryClient.config.save();
      } else {
         throw new IllegalArgumentException("Escala entre -200 e 200");
      }
   }

   private InventoryScales() {
   }
}
