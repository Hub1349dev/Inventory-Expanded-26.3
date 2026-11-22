package com.example.matinventory.arrangement;

import com.example.matinventory.capacity.BlockKinds;
import com.example.matinventory.capacity.EquipmentKinds;
import com.example.matinventory.capacity.ModItemSemantics;
import com.example.matinventory.capacity.ToolKinds;
import com.example.matinventory.capacity.VehicleKinds;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@Environment(EnvType.CLIENT)
public final class ItemCategoryClassifier {
   public static boolean longToolPose(ItemCategoryClassifier.Classification c) {
      return c.category() == ItemCategoryClassifier.Category.LONG_TOOLS && !c.subtype().equals("brush") && !c.subtype().equals("shears");
   }

   public static ItemCategoryClassifier.Classification classify(ItemStack stack) {
      if (BlockKinds.isTorch(stack)) {
         return new ItemCategoryClassifier.Classification(ItemCategoryClassifier.Category.OTHER, "rod");
      }

      if (stack.is(Items.BRUSH) || stack.is(Items.SHEARS) || stack.is(Items.FLINT_AND_STEEL)) {
         return new ItemCategoryClassifier.Classification(ItemCategoryClassifier.Category.OTHER, "instrument");
      }

      if (VehicleKinds.isVehicle(stack)) {
         return new ItemCategoryClassifier.Classification(ItemCategoryClassifier.Category.VEHICLES, "vehicle");
      }

      String tool = toolKind(stack);
      if (tool != null) {
         return new ItemCategoryClassifier.Classification(ItemCategoryClassifier.Category.LONG_TOOLS, tool);
      }

      ItemCategoryClassifier.Classification override = ItemPresentationData.category(stack);
      if (override != null) {
         return override;
      }

      if (ItemPresentationData.isDebugUnknown(stack)) {
         return new ItemCategoryClassifier.Classification(ItemCategoryClassifier.Category.OTHER, "unknown");
      }

      if (stack.is(ItemTags.SWORDS)) {
         return new ItemCategoryClassifier.Classification(ItemCategoryClassifier.Category.LONG_TOOLS, "sword");
      }

      if (stack.is(ItemTags.PICKAXES)) {
         return new ItemCategoryClassifier.Classification(ItemCategoryClassifier.Category.LONG_TOOLS, "pickaxe");
      }

      if (stack.is(ItemTags.AXES)) {
         return new ItemCategoryClassifier.Classification(ItemCategoryClassifier.Category.LONG_TOOLS, "axe");
      }

      if (stack.is(ItemTags.SHOVELS)) {
         return new ItemCategoryClassifier.Classification(ItemCategoryClassifier.Category.LONG_TOOLS, "shovel");
      }

      if (stack.is(ItemTags.HOES)) {
         return new ItemCategoryClassifier.Classification(ItemCategoryClassifier.Category.LONG_TOOLS, "hoe");
      }

      if (stack.is(ItemTags.SPEARS)) {
         return new ItemCategoryClassifier.Classification(ItemCategoryClassifier.Category.LONG_TOOLS, "spear");
      }

      if (stack.has(DataComponents.FOOD) || stack.has(DataComponents.POTION_CONTENTS)) {
         return new ItemCategoryClassifier.Classification(ItemCategoryClassifier.Category.FOOD, stack.has(DataComponents.POTION_CONTENTS) ? "potion" : "food");
      }

      if (EquipmentKinds.kind(stack) != null) {
         return new ItemCategoryClassifier.Classification(ItemCategoryClassifier.Category.EQUIPMENT, "equipment");
      }

      if (stack.getItem() instanceof BlockItem) {
         return new ItemCategoryClassifier.Classification(ItemCategoryClassifier.Category.BLOCKS, "block");
      }

      for (TagKey<Item> tag : stack.tags().toList()) {
         String path = tag.location().getPath();
         if (ModItemSemantics.resourceTag(path)) {
            return new ItemCategoryClassifier.Classification(ItemCategoryClassifier.Category.RESOURCES, path);
         }

         if (path.contains("bow") || path.startsWith("tools/") || path.startsWith("weapons/")) {
            return new ItemCategoryClassifier.Classification(ItemCategoryClassifier.Category.LONG_TOOLS, path);
         }
      }

      return new ItemCategoryClassifier.Classification(ItemCategoryClassifier.Category.OTHER, "unknown");
   }

   public static String toolKind(ItemStack stack) {
      if (stack.isEmpty() || BlockKinds.isTorch(stack)) {
         return null;
      }

      if (stack.is(ItemTags.AXES)) {
         return "axe";
      }

      if (stack.is(ItemTags.PICKAXES)) {
         return "pickaxe";
      }

      if (stack.is(ItemTags.SHOVELS)) {
         return "shovel";
      }

      if (stack.is(ItemTags.SWORDS)) {
         return "sword";
      }

      if (stack.is(ItemTags.HOES)) {
         return "hoe";
      }

      Item item = stack.getItem();
      if (item == Items.CROSSBOW) {
         return "crossbow";
      }

      if (item == Items.MACE) {
         return "mace";
      }

      if (item == Items.BOW) {
         return "bow";
      }

      if (item == Items.FISHING_ROD || item == Items.CARROT_ON_A_STICK || item == Items.WARPED_FUNGUS_ON_A_STICK) {
         return "fishing_rod";
      }

      if (item == Items.TRIDENT) {
         return "trident";
      }

      if (stack.is(ItemTags.SPEARS)) {
         return "spear";
      }

      String shared = ToolKinds.kind(stack);
      return !"vehicle".equals(shared) && !"shield".equals(shared) && !"brush".equals(shared) && !"shears".equals(shared) ? shared : null;
   }

   public static boolean flexible(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      }

      ItemCategoryClassifier.Classification c = classify(stack);
      return c.category() == ItemCategoryClassifier.Category.OTHER && "unknown".equals(c.subtype());
   }

   public static boolean acceptsSector(ItemStack stack, ItemCategoryClassifier.Category sector) {
      return !stack.isEmpty()
         && (sector == ItemCategoryClassifier.Category.HOTBAR || sector == ItemCategoryClassifier.Category.CRAFT || classify(stack).category() == sector);
   }

   private ItemCategoryClassifier() {
   }

   @Environment(EnvType.CLIENT)
   public enum Category {
      LONG_TOOLS,
      FOOD,
      BLOCKS,
      RESOURCES,
      OTHER,
      EQUIPMENT,
      CRAFT,
      HOTBAR,
      VEHICLES;
   }

   @Environment(EnvType.CLIENT)
   public record Classification(ItemCategoryClassifier.Category category, String subtype) {
   }
}
