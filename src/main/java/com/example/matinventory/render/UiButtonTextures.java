package com.example.matinventory.render;

import com.example.matinventory.MatInventoryClient;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public final class UiButtonTextures {
   private static final Map<Identifier, Identifier> variants = new HashMap<>();
   private static int generation = -1;

   public static Identifier themed(Identifier source) {
      boolean dark = MatInventoryClient.config.darkUiButtons;
      if (!dark && !source.getPath().contains("/mouse_")) {
         return source;
      }

      if (generation != MatMaterials.generation) {
         variants.clear();
         generation = MatMaterials.generation;
      }

      Identifier target = Identifier.fromNamespaceAndPath("matinventory", "dynamic/" + (dark ? "dark/" : "light/") + source.getPath());
      return variants.computeIfAbsent(target, key -> create(source, key, dark));
   }

   private static Identifier create(Identifier source, Identifier target, boolean dark) {
      Minecraft client = Minecraft.getInstance();

      try (InputStream input = client.getResourceManager().getResourceOrThrow(source).open()) {
         NativeImage image = NativeImage.read(input);
         joinButton(image, source);
         if (dark) {
            for (int y = 0; y < image.getHeight(); y++) {
               for (int x = 0; x < image.getWidth(); x++) {
                  image.setPixelABGR(x, y, darkPixel(Integer.reverseBytes(Integer.rotateLeft(image.getPixel(x, y), 8))));
               }
            }
         }

         client.getTextureManager().register(target, new DynamicTexture(() -> target.toString(), image));
         return target;
      } catch (IOException error) {
         MatInventoryClient.LOG.warn("Cannot load dark UI icon {}", source, error);
         return source;
      }
   }

   private static void joinButton(NativeImage image, Identifier source) {
      boolean left = source.getPath().endsWith("mouse_left.png");
      boolean right = source.getPath().endsWith("mouse_right.png");
      if ((left || right) && image.getWidth() == 12 && image.getHeight() == 16) {
         int accent = Integer.reverseBytes(Integer.rotateLeft(image.getPixel(left ? 2 : 7, 3), 8));
         int start = left ? 1 : 7;
         int end = left ? 4 : 10;

         for (int y = 1; y <= 6; y++) {
            for (int x = start; x <= end; x++) {
               int pixel = Integer.reverseBytes(Integer.rotateLeft(image.getPixel(x, y), 8));
               int rgb = pixel & 16777215;
               if (pixel >>> 24 != 0 && (rgb == 16250612 || rgb == 11446691)) {
                  image.setPixelABGR(x, y, pixel & 0xFF000000 | accent & 16777215);
               }
            }
         }
      }
   }

   public static int darkPixel(int rgba) {
      int alpha = rgba >>> 24;
      if (alpha == 0) {
         return rgba;
      }

      int r = rgba & 0xFF;
      int g = rgba >> 8 & 0xFF;
      int b = rgba >> 16 & 0xFF;
      int rgb = r << 16 | g << 8 | b;

      int dark = switch (rgb) {
         case 2830131, 3357248 -> 14079704;
         case 3422784, 5396574 -> 1644827;
         case 9016734 -> 13750739;
         case 10331305, 10725806 -> 5526614;
         case 11187131 -> 10000538;
         case 14541799 -> 2697515;
         case 15790835, 16054007, 16777215 -> 3552824;
         default -> {
            int grey = Math.round(25.0F + 50.0F * (r * 0.2126F + g * 0.7152F + b * 0.0722F) / 255.0F);
            yield grey << 16 | grey << 8 | grey;
         }
      };
      return alpha << 24 | (dark & 0xFF) << 16 | dark & 0xFF00 | dark >>> 16 & 0xFF;
   }

   private UiButtonTextures() {
   }
}
