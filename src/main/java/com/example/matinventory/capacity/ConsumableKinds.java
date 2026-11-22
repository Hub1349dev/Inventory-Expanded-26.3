package com.example.matinventory.capacity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;

public final class ConsumableKinds {
   private static final Map<String, Boolean> categories = new HashMap<>();

   public static boolean isConsumable(ItemStack stack) {
      return !stack.isEmpty()
         && categories.getOrDefault(
            BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.has(DataComponents.FOOD) || stack.has(DataComponents.POTION_CONTENTS)
         );
   }

   public static String key(ItemStack stack) {
      if (!isConsumable(stack)) {
         return null;
      }

      String key = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
      if (stack.has(DataComponents.POTION_CONTENTS)) {
         PotionContents potion = stack.get(DataComponents.POTION_CONTENTS);
         key = key
            + "|potion="
            + potion.potion().flatMap(Holder::unwrapKey).map(k -> k.identifier().toString()).orElse("custom")
            + "|color="
            + potion.customColor()
            + "|effects="
            + potion.customEffects();
      }

      if (stack.has(DataComponents.SUSPICIOUS_STEW_EFFECTS)) {
         key = key
            + "|stew="
            + stack.get(DataComponents.SUSPICIOUS_STEW_EFFECTS)
               .effects()
               .stream()
               .map(effect -> effect.effect().unwrapKey().map(k -> k.identifier().toString()).orElse("custom") + ":" + effect.duration())
               .toList();
      }

      return key;
   }

   private ConsumableKinds() {
   }

   static {
      try (InputStream in = ConsumableKinds.class.getResourceAsStream("/assets/matinventory/arrangement/item_categories.json")) {
         if (in == null) {
            throw new IOException("Missing item categories");
         }

         JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();

         for (Entry<String, JsonElement> e : json.entrySet()) {
            categories.put(
               e.getKey(),
               "FOOD".equals(e.getValue().isJsonPrimitive() ? e.getValue().getAsString() : e.getValue().getAsJsonObject().get("category").getAsString())
            );
         }
      } catch (IOException ex) {
         throw new ExceptionInInitializerError(ex);
      }
   }
}
