package com.example.matinventory.render;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
final class ShaderCanvas {
   private static final Identifier ID = Identifier.fromNamespaceAndPath("matinventory", "runtime/shader_canvas");
   private static int generation = -1;

   static Identifier texture() {
      if (generation == MatMaterials.generation) {
         return ID;
      }

      Minecraft c = Minecraft.getInstance();

      try (InputStream stream = c.getResourceManager().getResourceOrThrow(MatMaterials.LEATHER).open()) {
         NativeImage image = NativeImage.read(stream);

         for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
               int pixel = image.getPixel(x, y);
               int gray = Math.min(255, Math.round(2.0F * (0.2126F * (pixel >> 16 & 0xFF) + 0.7152F * (pixel >> 8 & 0xFF) + 0.0722F * (pixel & 0xFF))));
               image.setPixelABGR(x, y, pixel & 0xFF000000 | gray * 65793);
            }
         }

         c.getTextureManager().release(ID);
         c.getTextureManager().register(ID, new DynamicTexture(() -> "Inventory Expanded shader canvas", image));
         generation = MatMaterials.generation;
         return ID;
      } catch (IOException e) {
         throw new IllegalStateException("Cannot prepare inventory shader canvas", e);
      }
   }

   private ShaderCanvas() {
   }
}
