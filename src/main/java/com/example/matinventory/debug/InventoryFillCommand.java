package com.example.matinventory.debug;

import com.example.matinventory.capacity.BlockKinds;
import com.example.matinventory.capacity.ConsumableKinds;
import com.example.matinventory.capacity.EquipmentKinds;
import com.example.matinventory.capacity.ExtraInventory;
import com.example.matinventory.capacity.MaterialKinds;
import com.example.matinventory.capacity.ModToolRules;
import com.example.matinventory.capacity.ToolKinds;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab.Type;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;

public final class InventoryFillCommand {
   public static void register() {
      CommandRegistrationCallback.EVENT
         .register(
            (CommandRegistrationCallback)(dispatcher, access, environment) -> dispatcher.register(
               Commands.literal("inventory")
                  .then(
                     Commands.literal("fill")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(c -> fill(c.getSource(), "full", 1.0))
                        .then(Commands.literal("low").executes(c -> fill(c.getSource(), "low", 0.1)))
                        .then(Commands.literal("medium").executes(c -> fill(c.getSource(), "medium", 0.5)))
                        .then(Commands.literal("full").executes(c -> fill(c.getSource(), "full", 1.0)))
                  )
            )
         );
   }

   private static int fill(CommandSourceStack source, String mode, double fraction) throws CommandSyntaxException {
      ServerPlayer player = source.getPlayerOrException();
      ExtraInventory extra = ExtraInventory.of(player);
      int capacity = 36 + extra.activeSlots();
      int target = Math.max(1, (int)Math.round(capacity * fraction));
      Random random = new Random();
      ArrayList<ItemStack> catalog = new ArrayList<>();
      CreativeModeTabs.tryRebuildTabContents(player.level().enabledFeatures(), true, player.registryAccess());
      Set<ItemStack> variants = ItemStackLinkedSet.createTypeAndComponentsSet();

      for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
         if (tab.getType() == Type.CATEGORY) {
            for (ItemStack entry : tab.getDisplayItems()) {
               if (!entry.isEmpty() && entry.getItem().isEnabled(player.level().enabledFeatures())) {
                  variants.add(entry.copyWithCount(1));
               }
            }
         }
      }

      Set<Item> represented = Collections.newSetFromMap(new IdentityHashMap<>());

      for (ItemStack entry : variants) {
         catalog.add(entry);
         represented.add(entry.getItem());
      }

      for (Item item : BuiltInRegistries.ITEM) {
         ItemStack stack = item.getDefaultInstance().copy();
         if (!represented.contains(item) && !stack.isEmpty() && item.isEnabled(player.level().enabledFeatures())) {
            catalog.add(stack);
         }
      }

      Collections.shuffle(catalog, random);
      InventoryFillCommand.LoadoutLimits limits = new InventoryFillCommand.LoadoutLimits();
      ArrayList<ItemStack> loadout = new ArrayList<>();

      for (ItemStack stack : catalog) {
         if (loadout.size() == target) {
            break;
         }

         if (limits.accepts(stack)) {
            limits.add(stack);
            loadout.add(stack);
         }
      }

      ArrayList<ItemStack> repeatable = new ArrayList<>();

      for (ItemStack stack : loadout) {
         if (ToolKinds.kind(stack) == null && EquipmentKinds.kind(stack) == null) {
            repeatable.add(stack);
         }
      }

      if (repeatable.isEmpty()) {
         for (ItemStack stack : catalog) {
            if (ToolKinds.kind(stack) == null && EquipmentKinds.kind(stack) == null && limits.accepts(stack)) {
               repeatable.add(stack);
            }
         }
      }

      if (repeatable.isEmpty() && loadout.size() < target) {
         source.sendFailure(Component.translatable("matinventory.fill.failed"));
         return 0;
      }

      while (loadout.size() < target) {
         loadout.add(repeatable.get(random.nextInt(repeatable.size())).copy());
      }

      Collections.shuffle(loadout, random);
      if (player.containerMenu != player.inventoryMenu) {
         player.closeContainer();
      }

      player.inventoryMenu.setCarried(ItemStack.EMPTY);
      player.getInventory().clearContent();
      extra.clearContent();
      extra.ownedCursorKind = null;
      ArrayList<Integer> destinations = new ArrayList<>();

      for (int i = 0; i < capacity; i++) {
         destinations.add(i);
      }

      Collections.shuffle(destinations, random);

      for (int i = 0; i < target; i++) {
         ItemStack stack = loadout.get(i).copy();
         int max = ModToolRules.applies(stack) ? Math.min(2, stack.getMaxStackSize()) : stack.getMaxStackSize();
         stack.setCount(mode.equals("full") ? max : 1 + random.nextInt(mode.equals("low") ? Math.min(8, max) : max));
         int slot = destinations.get(i);
         if (slot < 36) {
            player.getInventory().setItem(slot, stack);
         } else {
            extra.setItem(slot - 36, stack);
         }
      }

      player.getInventory().setChanged();
      extra.setChanged();
      player.inventoryMenu.broadcastChanges();
      source.sendSuccess(() -> Component.translatable("matinventory.fill.done", mode, target, capacity), false);
      return target;
   }

   private InventoryFillCommand() {
   }

   private static final class LoadoutLimits {
      private final Map<String, Integer> tools = new HashMap<>();
      private final Map<String, Integer> equipment = new HashMap<>();
      private final Set<String> blocks = new HashSet<>();
      private final Set<String> food = new HashSet<>();
      private final Set<String> materials = new HashSet<>();

      boolean accepts(ItemStack stack) {
         String tool = ToolKinds.kind(stack);
         String gear = EquipmentKinds.kind(stack);
         if (tool != null && this.tools.getOrDefault(tool, 0) >= ToolKinds.limit(tool)) {
            return false;
         }

         if (gear != null && this.equipment.getOrDefault(gear, 0) >= (!gear.equals("elytra") && !gear.equals("shield") ? 3 : 1)) {
            return false;
         }

         String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
         return (!BlockKinds.isBlock(stack) || this.blocks.contains(id) || this.blocks.size() < 16)
            && (!ConsumableKinds.isConsumable(stack) || this.food.contains(ConsumableKinds.key(stack)) || this.food.size() < 14)
            && (!MaterialKinds.isMaterial(stack) || this.materials.contains(MaterialKinds.key(stack)) || this.materials.size() < 25);
      }

      void add(ItemStack stack) {
         String tool = ToolKinds.kind(stack);
         String gear = EquipmentKinds.kind(stack);
         if (tool != null) {
            this.tools.merge(tool, 1, Integer::sum);
         }

         if (gear != null) {
            this.equipment.merge(gear, 1, Integer::sum);
         }

         if (BlockKinds.isBlock(stack)) {
            this.blocks.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
         }

         if (ConsumableKinds.isConsumable(stack)) {
            this.food.add(ConsumableKinds.key(stack));
         }

         if (MaterialKinds.isMaterial(stack)) {
            this.materials.add(MaterialKinds.key(stack));
         }
      }
   }
}
