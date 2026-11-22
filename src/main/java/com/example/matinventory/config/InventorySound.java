package com.example.matinventory.config;

import com.example.matinventory.MatInventoryClient;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public enum InventorySound {
   OPEN,
   DROP,
   CLOSE,
   LAND,
   HOVER,
   PICKUP,
   PLACE,
   CRAFT,
   EAT,
   BUTTONS,
   LED_AMBIENT,
   LED_FLICKER;

   public String id() {
      return this.name().toLowerCase(Locale.ROOT);
   }

   public boolean enabled() {
      MatConfig config = MatInventoryClient.config;
      return config == null || config.inventorySounds.getOrDefault(this.id(), true);
   }

   public void set(boolean enabled) {
      MatInventoryClient.config.inventorySounds.put(this.id(), enabled);
   }

   public static boolean allows(Identifier id) {
      if (!id.getNamespace().equals("matinventory")) {
         return true;
      }

      InventorySound option = switch (id.getPath()) {
         case "inventory_open", "inventory_open_short" -> OPEN;
         case "mat_drop" -> DROP;
         case "inventory_close" -> CLOSE;
         case "inventory_items_load" -> LAND;
         case "craft" -> CRAFT;
         case "eat_1", "eat_2", "eat_3" -> EAT;
         case "led_button_click" -> BUTTONS;
         case "led_ambient" -> LED_AMBIENT;
         case "led_flicker" -> LED_FLICKER;
         default -> null;
      };
      return option == null || option.enabled();
   }
}
