package com.example.matinventory.arrangement;

import java.util.Map;
import java.util.TreeMap;
import java.util.WeakHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

@Environment(EnvType.CLIENT)
public final class ModelVariant {
   private static final Map<ItemStack, ModelVariant.Cached> CACHE = new WeakHashMap<>();

   public static String key(ItemStack stack) {
      TreeMap<String, String> ids = new TreeMap<>();

      for (TypedDataComponent<?> component : stack.getComponents()) {
         if (component.value() instanceof Identifier id) {
            ids.put(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(component.type()).toString(), id.toString());
         }
      }

      CustomData data = stack.get(DataComponents.CUSTOM_DATA);
      ModelVariant.Cached cached = CACHE.get(stack);
      if (cached == null || cached.data() != data) {
         TreeMap<String, String> values = new TreeMap<>();
         if (data != null) {
            collect(data.copyTag(), "", values, 0);
         }

         cached = new ModelVariant.Cached(data, values.toString());
         CACHE.put(stack, cached);
      }

      CustomModelData variant = stack.get(DataComponents.CUSTOM_MODEL_DATA);
      return (ids.isEmpty() ? "" : "|ids=" + ids) + (cached.key().equals("{}") ? "" : "|data=" + cached.key()) + (variant == null ? "" : "|variant=" + variant);
   }

   private static void collect(Tag tag, String path, Map<String, String> values, int depth) {
      if (depth <= 12) {
         if (tag instanceof CompoundTag compound) {
            for (String key : compound.keySet()) {
               collect(compound.get(key), path + "/" + key, values, depth + 1);
            }
         } else if (tag instanceof ListTag list) {
            for (int i = 0; i < list.size(); i++) {
               collect(list.get(i), path + "/" + i, values, depth + 1);
            }
         } else if (tag instanceof StringTag text) {
            String value = text.value();
            if (value.indexOf(58) > 0 && Identifier.tryParse(value) != null) {
               values.put(path, value);
            }
         }
      }
   }

   private ModelVariant() {
   }

   @Environment(EnvType.CLIENT)
   private record Cached(CustomData data, String key) {
   }
}
