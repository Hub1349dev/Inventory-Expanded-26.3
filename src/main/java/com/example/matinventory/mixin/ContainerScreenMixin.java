package com.example.matinventory.mixin;

import com.example.matinventory.MatInventoryClient;
import com.example.matinventory.compat.MatInput;
import com.example.matinventory.screen.MatBlockInventoryScreen;
import com.example.matinventory.screen.MatInventoryScreen;
import com.example.matinventory.screen.NativeMenuPreview;
import com.example.matinventory.screen.NativePreviewAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(AbstractContainerScreen.class)
abstract class ContainerScreenMixin implements NativePreviewAccess {
   @Unique
   private NativeMenuPreview matinventory$native;

   @Override
   public NativeMenuPreview matinventory$nativePreview() {
      return this.matinventory$native;
   }

   @Override
   public void matinventory$refreshNativePreview() {
      if (this.matinventory$native != null) {
         this.matinventory$native.restore();
      }

      this.matinventory$native = NativeMenuPreview.create((AbstractContainerScreen<?>)(Object)this);
   }

   @Inject(method = "init", at = @At("HEAD"))
   private void matinventory$restore(CallbackInfo ci) {
      if (this.matinventory$native != null) {
         this.matinventory$native.restore();
      }

      this.matinventory$native = null;
   }

   @Inject(method = "init", at = @At("RETURN"))
   private void matinventory$attach(CallbackInfo ci) {
      this.matinventory$native = NativeMenuPreview.create((AbstractContainerScreen<?>)(Object)this);
   }

   @Redirect(
      method = "extractContents",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractLabels(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V"
      )
   )
   private void matinventory$nativeLabels(AbstractContainerScreen<?> screen, GuiGraphicsExtractor g, int x, int y) {
      if (this.matinventory$native != null) {
         this.matinventory$native.labels(g, x, y);
      } else {
         ((NativeScreenInvoker)screen).matinventory$labels(g, x, y);
      }
   }

   @Inject(method = "extractRenderState", at = @At("TAIL"))
   private void matinventory$nativeControls(GuiGraphicsExtractor g, int x, int y, float partial, CallbackInfo ci) {
      if (this.matinventory$native != null) {
         this.matinventory$native.controls(g, x, y, partial);
      }
   }

   @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
   private void matinventory$nativeClick(MouseButtonEvent event, boolean twice, CallbackInfoReturnable<Boolean> ci) {
      if (this.matinventory$native != null && this.matinventory$native.click(event)) {
         ci.setReturnValue(true);
      }
   }

   @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
   private void matinventory$nativeRelease(MouseButtonEvent event, CallbackInfoReturnable<Boolean> ci) {
      double x = event.x();
      double y = event.y();
      int button = MatInput.button(event);
      if (this.matinventory$native != null && this.matinventory$native.release(x, y, button)) {
         ci.setReturnValue(true);
      }
   }

   @Inject(method = "extractTooltip", at = @At("HEAD"), cancellable = true)
   private void matinventory$nativeTooltip(GuiGraphicsExtractor g, int x, int y, CallbackInfo ci) {
      if (this.matinventory$native != null && this.matinventory$native.inPreview(x, y)) {
         ci.cancel();
      }
   }

   @Inject(method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V", at = @At("HEAD"))
   private void matinventory$beforeNativeSlot(Slot slot, int index, int button, ContainerInput type, CallbackInfo ci) {
      if (this.matinventory$native != null) {
         this.matinventory$native.beforeClick(slot);
      }
   }

   @ModifyVariable(method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V", at = @At("HEAD"), argsOnly = true, ordinal = 1)
   private int matinventory$oneNativeTool(int button, Slot slot, int index, int originalButton, ContainerInput type) {
      return this.matinventory$native == null ? button : this.matinventory$native.pickupButton(slot, button, type);
   }

   @Inject(method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V", at = @At("RETURN"))
   private void matinventory$afterNativeSlot(Slot slot, int index, int button, ContainerInput type, CallbackInfo ci) {
      if (this.matinventory$native != null) {
         this.matinventory$native.afterClick(slot, type);
      }
   }

   @Inject(method = "extractSlot", at = @At("HEAD"), cancellable = true)
   private void matinventory$embeddedItem(GuiGraphicsExtractor graphics, Slot slot, int x, int y, CallbackInfo ci) {
      if ((Object)this instanceof MatBlockInventoryScreen block && block.preview != null && block.preview.owns(slot)) {
         ci.cancel();
      }

      if (this.matinventory$native != null && this.matinventory$native.owns(slot)) {
         ci.cancel();
      }
   }

   @Inject(method = {"extractSlotHighlightBack", "extractSlotHighlightFront"}, at = @At("HEAD"), cancellable = true)
   private void matinventory$embeddedHighlight(GuiGraphicsExtractor graphics, CallbackInfo ci) {
      Slot slot = ((NativeScreenInvoker)(Object)this).matinventory$hovered();
      if (slot != null
         && (
            (Object)this instanceof MatBlockInventoryScreen block && block.preview.owns(slot)
               || this.matinventory$native != null && this.matinventory$native.owns(slot)
         )) {
         ci.cancel();
      }
   }

   @Inject(method = "getHoveredSlot", at = @At("HEAD"), cancellable = true)
   private void matinventory$pick(double x, double y, CallbackInfoReturnable<Slot> cir) {
      if ((Object)this instanceof MatInventoryScreen mat) {
         cir.setReturnValue(mat.pickSlot(x, y));
      }

      if ((Object)this instanceof MatBlockInventoryScreen block && block.preview != null && block.preview.contains(x, y) && !block.blockAt(x, y)) {
         cir.setReturnValue(block.preview.pick(x, y));
      }

      if (this.matinventory$native != null && this.matinventory$native.inPreview(x, y)) {
         cir.setReturnValue(this.matinventory$native.preview.pick(x, y));
      }
   }

   @Inject(method = "removed", at = @At("HEAD"), cancellable = true)
   private void matinventory$preserveMenu(CallbackInfo ci) {
      if (this.matinventory$native != null) {
         this.matinventory$native.restore();
      }

      if (MatInventoryClient.switching) {
         ci.cancel();
      }
   }
}
