package com.example.matinventory.capacity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class MaterialKinds {
   private static final Map<String, String> categories = new HashMap<>();
   private static final Map<String, String> subtypes = new HashMap<>();

   public static String category(ItemStack stack) {
      if (BlockKinds.isTorch(stack)) {
         return "OTHER";
      }

      if (stack.is(Items.BRUSH) || stack.is(Items.SHEARS) || stack.is(Items.FLINT_AND_STEEL)) {
         return "OTHER";
      }

      if (!stack.isEmpty() && ToolKinds.kind(stack) == null) {
         String known = categories.get(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
         if (known == null) {
            if (!stack.has(DataComponents.FOOD)
               && !stack.has(DataComponents.POTION_CONTENTS)
               && EquipmentKinds.kind(stack) == null
               && !(stack.getItem() instanceof BlockItem)) {
               for (TagKey<Item> tag : stack.tags().toList()) {
                  if (ModItemSemantics.resourceTag(tag.location().getPath())) {
                     return "RESOURCES";
                  }
               }

               return ModItemSemantics.external(stack) ? null : "OTHER";
            } else {
               return null;
            }
         } else {
            return !known.equals("RESOURCES") && !known.equals("OTHER") ? null : known;
         }
      } else {
         return null;
      }
   }

   public static boolean isMaterial(ItemStack stack) {
      return category(stack) != null;
   }

   public static String key(ItemStack stack) {
      return isMaterial(stack) ? BuiltInRegistries.ITEM.getKey(stack.getItem()).toString() : null;
   }

   public static boolean rod(ItemStack stack) {
      if (BlockKinds.isTorch(stack)) {
         return true;
      }

      if (!isMaterial(stack)) {
         return false;
      }

      String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
      return "rod".equals(subtypes.get(id))
         ? true
         : stack.tags().anyMatch(tag -> tag.location().getPath().equals("rods") || tag.location().getPath().startsWith("rods/"));
   }

   public static boolean mineral(ItemStack stack) {
      if ("RESOURCES".equals(category(stack)) && !rod(stack)) {
         String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
         String sub = subtypes.getOrDefault(id, "");
         return sub.equals("gem")
            || sub.equals("metal")
            || sub.equals("tiny")
            || List.of("coal", "raw_", "ingot", "nugget", "flint", "clay_ball", "prismarine", "resin").stream().anyMatch(id::contains);
      } else {
         return false;
      }
   }

   private MaterialKinds() {
   }

   static {
      try (InputStream in = MaterialKinds.class.getResourceAsStream("/assets/matinventory/arrangement/item_categories.json")) {
         if (in == null) {
            throw new IOException("Missing item categories");
         }

         JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();

         for (Entry<String, JsonElement> e : json.entrySet()) {
            JsonElement v = e.getValue();
            categories.put(e.getKey(), v.isJsonPrimitive() ? v.getAsString() : v.getAsJsonObject().get("category").getAsString());
            if (v.isJsonObject() && v.getAsJsonObject().has("subtype")) {
               subtypes.put(e.getKey(), v.getAsJsonObject().get("subtype").getAsString());
            }
         }
      } catch (IOException ex) {
         throw new ExceptionInInitializerError(ex);
      }
   }
}
