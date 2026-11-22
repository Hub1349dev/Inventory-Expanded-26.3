package com.example.matinventory.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.textures.AddressMode;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import java.util.OptionalDouble;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
final class MippedMaterial extends SimpleTexture {
   private GpuSampler ownedSampler;

   MippedMaterial(Identifier id) {
      super(id);
   }

   @Override
   public void apply(TextureContents contents) {
      MatMaterials.generation++;
      this.close();
      GpuDevice device = RenderSystem.getDevice();

      try (NativeImage base = contents.image()) {
         boolean pixel = MatMaterials.isPixel(this.resourceId());
         int levels = pixel ? 0 : Math.min(8, 31 - Integer.numberOfLeadingZeros(Math.min(base.getWidth(), base.getHeight())));
         this.texture = device.createTexture(this.resourceId()::toString, 5, GpuFormat.RGBA8_UNORM, base.getWidth(), base.getHeight(), 1, levels + 1);
         this.textureView = device.createTextureView(this.texture);
         CommandEncoder encoder = device.createCommandEncoder();
         encoder.writeToTexture(this.texture, base);

         for (int level = 1; level <= levels; level++) {
            int w = Math.max(1, base.getWidth() >> level);
            int h = Math.max(1, base.getHeight() >> level);

            try (NativeImage mip = new NativeImage(w, h, false)) {
               base.resizeSubRectTo(0, 0, base.getWidth(), base.getHeight(), mip);
               encoder.writeToTexture(this.texture, mip, level, 0, 0, 0);
            }
         }

         FilterMode filter = pixel ? FilterMode.NEAREST : FilterMode.LINEAR;
         this.ownedSampler = device.createSampler(AddressMode.REPEAT, AddressMode.REPEAT, filter, filter, pixel ? 1 : 4, OptionalDouble.empty());
         this.sampler = this.ownedSampler;
      }
   }

   @Override
   public void close() {
      super.close();
      if (this.ownedSampler != null) {
         this.ownedSampler.close();
         this.ownedSampler = null;
      }
   }
}
