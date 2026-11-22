package com.example.matinventory.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import java.util.Optional;
import java.util.OptionalDouble;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class SceneTarget {
   GpuTextureView color;
   GpuTextureView depth;

   void bind(GpuTextureView color, GpuTextureView depth) {
      this.color = color;
      this.depth = depth;
   }

   RenderPass open() {
      RenderPass pass = RenderSystem.getDevice()
         .createCommandEncoder()
         .createRenderPass(() -> "Inventory scene", this.color, Optional.empty(), this.depth, OptionalDouble.empty());
      RenderSystem.bindDefaultUniforms(pass);
      return pass;
   }
}
