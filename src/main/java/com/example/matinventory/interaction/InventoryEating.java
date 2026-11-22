package com.example.matinventory.interaction;

import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;

public final class InventoryEating {
   private static final ThreadLocal<LivingEntity> SILENT_USE = new ThreadLocal<>();
   private static final Map<ServerPlayer, Long> lastUse = new WeakHashMap<>();

   public static boolean isFood(ItemStack stack) {
      return !stack.isEmpty() && stack.has(DataComponents.FOOD) && stack.has(DataComponents.CONSUMABLE);
   }

   public static boolean silentUse(LivingEntity player) {
      return SILENT_USE.get() == player;
   }

   public static boolean silentUse() {
      return SILENT_USE.get() != null;
   }

   public static void register() {
      PayloadTypeRegistry.serverboundPlay().register(EatItemRequest.TYPE, EatItemRequest.CODEC);
      PayloadTypeRegistry.clientboundPlay().register(EatItemResult.TYPE, EatItemResult.CODEC);
      ServerPlayNetworking.registerGlobalReceiver(EatItemRequest.TYPE, (request, context) -> {
         int variant = consume(context.player(), request);
         ServerPlayNetworking.send(context.player(), new EatItemResult(request.menuId(), request.token(), variant));
      });
   }

   public static int consume(ServerPlayer player, EatItemRequest request) {
      AbstractContainerMenu menu = player.containerMenu;
      if (player.isAlive()
         && !player.isSpectator()
         && player.getFoodData().needsFood()
         && !player.isUsingItem()
         && menu.containerId == request.menuId()
         && (menu == player.inventoryMenu || menu instanceof CraftingMenu)
         && menu.getCarried().isEmpty()
         && request.slot() >= 1
         && request.slot() < menu.slots.size()) {
         Slot slot = menu.getSlot(request.slot());
         ItemStack stack = slot.getItem();
         long now = player.level().getGameTime();
         if (isFood(stack)
            && slot.mayPickup(player)
            && ItemStack.matches(stack, request.expected())
            && !player.getCooldowns().isOnCooldown(stack)
            && now - lastUse.getOrDefault(player, -4611686018427387904L) >= 4L) {
            Consumable consumable = stack.get(DataComponents.CONSUMABLE);
            if (!consumable.canConsume(player, stack)) {
               return -1;
            }

            lastUse.put(player, now);
            SILENT_USE.set(player);

            try {
               slot.set(stack.finishUsingItem(player.level(), player));
               slot.setChanged();
            } finally {
               SILENT_USE.remove();
            }

            menu.broadcastChanges();
            return player.getRandom().nextInt(3);
         } else {
            return -1;
         }
      } else {
         return -1;
      }
   }

   private InventoryEating() {
   }
}
