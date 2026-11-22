package com.example.matinventory;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class MatSounds {
   public static final float GAIN = 0.35F;
   public static final SoundEvent INVENTORY_OPEN = register("inventory_open");
   public static final SoundEvent INVENTORY_OPEN_SHORT = register("inventory_open_short");
   public static final SoundEvent INVENTORY_CLOSE = register("inventory_close");
   public static final SoundEvent MAT_DROP = register("mat_drop");
   public static final SoundEvent ITEMS_LOAD = register("inventory_items_load");
   public static final SoundEvent LED_BUTTON_CLICK = register("led_button_click");
   public static final SoundEvent LED_AMBIENT = register("led_ambient");
   public static final SoundEvent LED_FLICKER = register("led_flicker");
   public static final SoundEvent SEARCH_BLOCKS = register("search_blocks");
   public static final SoundEvent SEARCH_TOOLS = register("search_tools");
   public static final SoundEvent SEARCH_CRAFT_FOOD = register("search_craft_food");
   public static final SoundEvent SEARCH_CLOTHING = register("search_clothing");
   public static final SoundEvent CRAFT = register("craft");
   public static final SoundEvent[] EAT = new SoundEvent[]{register("eat_1"), register("eat_2"), register("eat_3")};

   private static SoundEvent register(String name) {
      Identifier id = Identifier.fromNamespaceAndPath("matinventory", name);
      return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
   }

   public static void register() {
   }

   private MatSounds() {
   }
}
