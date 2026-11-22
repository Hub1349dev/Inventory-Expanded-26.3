package com.example.matinventory.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin(Slot.class)
public interface GuiSlotPosition {
   @Mutable
   @Accessor("x")
   void matinventory$x(int var1);

   @Mutable
   @Accessor("y")
   void matinventory$y(int var1);
}
