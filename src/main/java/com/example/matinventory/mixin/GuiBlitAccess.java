package com.example.matinventory.mixin;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Environment(EnvType.CLIENT)
@Mixin(GuiGraphicsExtractor.class)
public interface GuiBlitAccess {
   @Invoker("innerBlit")
   void matinventory$blit(
      RenderPipeline var1, Identifier var2, int var3, int var4, int var5, int var6, float var7, float var8, float var9, float var10, int var11
   );
}
