package com.example.matinventory.mixin;

import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin(PictureInPictureRenderer.class)
public interface PipTargetAccessor {
   @Accessor("depthTextureView")
   GpuTextureView matinventory$depthTarget();

   @Accessor("textureView")
   GpuTextureView matinventory$colorTarget();
}
