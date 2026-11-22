package com.example.matinventory.screen;

import java.lang.reflect.Method;
import java.util.Arrays;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
final class NativeItemClicks {
   private static final ClassValue<Integer> HANDLERS = new ClassValue<Integer>() {
      protected Integer computeValue(Class<?> type) {
         int flags = 0;

         for (Method method : type.getMethods()) {
            if (method.getReturnType() == boolean.class && method.getDeclaringClass() != Item.class) {
               Class<?>[] args = method.getParameterTypes();
               if (Arrays.equals(args, new Class[]{ItemStack.class, Slot.class, ClickAction.class, Player.class})) {
                  flags |= 1;
               }

               if (Arrays.equals(args, new Class[]{ItemStack.class, ItemStack.class, Slot.class, ClickAction.class, Player.class, SlotAccess.class})) {
                  flags |= 2;
               }
            }
         }

         return flags;
      }
   };

   static boolean applies(ItemStack carried, ItemStack target) {
      return !carried.isEmpty()
         && ((HANDLERS.get(carried.getItem().getClass()) & 1) != 0 || !target.isEmpty() && (HANDLERS.get(target.getItem().getClass()) & 2) != 0);
   }
}
