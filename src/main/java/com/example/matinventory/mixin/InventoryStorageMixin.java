package com.example.matinventory.mixin;

import com.example.matinventory.capacity.ExtraInventory;
import com.example.matinventory.interaction.InventoryCrafting;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
abstract class InventoryStorageMixin {
   @Shadow
   @Final
   public Player player;

   @Inject(method = "fillStackedContents", at = @At("TAIL"))
   private void matinventory$recipeContents(StackedItemContents contents, CallbackInfo ci) {
      if (ExtraInventory.enabled(this.player)) {
         ExtraInventory extra = ExtraInventory.of(this.player);

         for (int i = 0; i < extra.activeSlots(); i++) {
            contents.accountSimpleStack(extra.getItem(i));
         }
      }
   }

   @Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
   private void matinventory$add(int index, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
      if (ExtraInventory.enabled(this.player) && !this.player.hasInfiniteMaterials()) {
         ExtraInventory extra = ExtraInventory.of(this.player);
         if (index < 0) {
            cir.setReturnValue(extra.insert(stack, true));
         } else if (index >= 9 && index < 36
            ? !extra.acceptsStorage(stack, this.player.getInventory().getItem(index))
            : !extra.accepts(stack, this.player.getInventory().getItem(index))) {
            cir.setReturnValue(false);
         }
      }
   }

   @Inject(method = "placeItemBackInInventory(Lnet/minecraft/world/item/ItemStack;ZLnet/minecraft/util/Prediction;)V", at = @At("HEAD"), cancellable = true)
   private void matinventory$return(ItemStack stack, boolean notify, Prediction prediction, CallbackInfo ci) {
      if (ExtraInventory.enabled(this.player) && !this.player.hasInfiniteMaterials()) {
         ExtraInventory extra = ExtraInventory.of(this.player);
         extra.insert(stack, true);
         if (!stack.isEmpty()) {
            this.player.drop(stack, false, prediction);
         }

         if (this.player instanceof ServerPlayer serverPlayer) {
            serverPlayer.inventoryMenu.broadcastChanges();
         }

         ci.cancel();
      }
   }

   @Inject(method = "dropAll", at = @At("TAIL"))
   private void matinventory$drop(CallbackInfo ci) {
      if (!this.player.level().isClientSide()) {
         ExtraInventory.of(this.player).dropAll();
         this.matinventory$dropOutput(this.player.inventoryMenu);
         if (this.player.containerMenu != this.player.inventoryMenu) {
            this.matinventory$dropOutput(this.player.containerMenu);
         }
      }
   }

   @Unique
   private void matinventory$dropOutput(AbstractContainerMenu menu) {
      Slot output = InventoryCrafting.output(menu);
      if (output != null && output.hasItem()) {
         ItemEntity drop = this.player.createItemStackToDrop(output.remove(output.getItem().getCount()), true, false);
         if (drop != null) {
            this.player.level().addFreshEntity(drop);
         }
      }
   }

   @Inject(method = "clearContent", at = @At("TAIL"))
   private void matinventory$clear(CallbackInfo ci) {
      ExtraInventory.of(this.player).clearContent();
   }

   @Inject(method = "clearOrCountMatchingItems", at = @At("RETURN"), cancellable = true)
   private void matinventory$clearMatching(Predicate<ItemStack> predicate, boolean countingOnly, int limit, Container container, CallbackInfoReturnable<Integer> cir) {
      if (ExtraInventory.enabled(this.player)) {
         int previous = cir.getReturnValue();
         if (countingOnly || limit < 0 || previous < limit) {
            int extra = ContainerHelper.clearOrCountMatchingItems(ExtraInventory.of(this.player), predicate, limit < 0 ? -1 : limit - previous, countingOnly);
            cir.setReturnValue(previous + extra);
         }
      }
   }
}
