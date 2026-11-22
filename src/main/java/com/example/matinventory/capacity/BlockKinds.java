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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseTorchBlock;

public final class BlockKinds {
   private static final Map<String, Boolean> categories = new HashMap<>();

   public static boolean isTorch(ItemStack stack) {
      return !stack.isEmpty() && stack.getItem() instanceof BlockItem item && item.getBlock() instanceof BaseTorchBlock;
   }

   public static boolean isBlock(ItemStack stack) {
      return isTorch(stack)
         ? false
         : !stack.isEmpty() && categories.getOrDefault(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getItem() instanceof BlockItem);
   }

   private BlockKinds() {
   }

   static {
      try (InputStream in = BlockKinds.class.getResourceAsStream("/assets/matinventory/arrangement/item_categories.json")) {
         if (in == null) {
            throw new IOException("Missing item categories");
         }

         JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();

         for (Entry<String, JsonElement> e : json.entrySet()) {
            categories.put(
               e.getKey(),
               "BLOCKS".equals(e.getValue().isJsonPrimitive() ? e.getValue().getAsString() : e.getValue().getAsJsonObject().get("category").getAsString())
            );
         }
      } catch (IOException ex) {
         throw new ExceptionInInitializerError(ex);
      }
   }
}
