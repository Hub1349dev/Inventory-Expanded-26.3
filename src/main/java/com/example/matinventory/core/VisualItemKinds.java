package com.example.matinventory.core;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.BaseTorchBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.GrowingPlantBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.VineBlock;

@Environment(EnvType.CLIENT)
public final class VisualItemKinds {
   public static boolean torch(ItemStack stack) {
      return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof BaseTorchBlock;
   }

   public static boolean statue(ItemStack stack) {
      return stack.getItem() instanceof BlockItem item && item.getBlock().getClass().getSimpleName().contains("GolemStatue");
   }

   public static boolean bundle(ItemStack stack) {
      return stack.getItem() instanceof BlockItem item
         && (
            item.getBlock() instanceof BaseTorchBlock
               || item.getBlock() instanceof FenceBlock
               || item.getBlock() instanceof FenceGateBlock
               || item.getBlock() instanceof SignBlock
         );
   }

   public static boolean flatStack(ItemStack stack) {
      return stack.getItem() instanceof BlockItem item && (item.getBlock() instanceof BasePressurePlateBlock || item.getBlock() instanceof ButtonBlock);
   }

   public static boolean plant(ItemStack stack) {
      return stack.getItem() instanceof BlockItem item
         && (
            item.getBlock() instanceof VegetationBlock
               || item.getBlock() instanceof GrowingPlantBlock
               || item.getBlock() instanceof VineBlock
               || item.getBlock() instanceof MultifaceBlock
               || item.getBlock() instanceof SugarCaneBlock
               || item.getBlock() instanceof BambooStalkBlock
         );
   }

   public static boolean decorative(ItemStack stack) {
      if (stack.getItem() instanceof BlockItem item) {
         try {
            return !item.getBlock().defaultBlockState().isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
         } catch (RuntimeException unsupportedShape) {
            return false;
         }
      } else {
         return false;
      }
   }

   private VisualItemKinds() {
   }
}
