package com.example.matinventory.mixin;

import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Environment(EnvType.CLIENT)
@Mixin(AbstractContainerScreen.class)
public interface NativeScreenInvoker {
   @Accessor("hoveredSlot")
   Slot matinventory$hovered();

   @Accessor("topPos")
   int matinventory$top();

   @Accessor("leftPos")
   int matinventory$left();

   @Accessor("imageWidth")
   int matinventory$width();

   @Accessor("imageHeight")
   int matinventory$height();

   @Invoker("slotClicked")
   void matinventory$click(Slot var1, int var2, int var3, ContainerInput var4);

   @Invoker("getHoveredSlot")
   Slot matinventory$find(double var1, double var3);

   @Accessor("skipNextRelease")
   void matinventory$skipRelease(boolean var1);

   @Accessor("isQuickCrafting")
   void matinventory$quickCrafting(boolean var1);

   @Accessor("doubleclick")
   void matinventory$doubleClick(boolean var1);

   @Accessor("lastClickSlot")
   void matinventory$lastClickSlot(Slot var1);

   @Accessor("quickCraftSlots")
   Set<Slot> matinventory$dragSlots();

   @Invoker("extractLabels")
   void matinventory$labels(GuiGraphicsExtractor var1, int var2, int var3);

   @Accessor("topPos")
   void matinventory$top(int var1);

   @Accessor("playerInventoryTitle")
   Component matinventory$inventoryTitle();

   @Mutable
   @Accessor("playerInventoryTitle")
   void matinventory$inventoryTitle(Component var1);
}
