package com.example.matinventory.render;

import java.util.stream.IntStream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public final class MatMaterials {
   private static boolean loaded;
   public static int generation;
   public static final Identifier LED = id("pixel/led");
   public static final Identifier CONTACT = id("contact");
   public static final Identifier TARP = id("pixel/hotbar");
   public static final Identifier WOOD = id("pixel/wood");
   public static final Identifier VIGNETTE = id("vignette");
   public static final Identifier LEATHER = id("pixel/canvas");
   public static final Identifier CLOTH = id("pixel/pad");
   public static final Identifier METAL = id("pixel/rubber");
   public static final Identifier BORDER = id("pixel/border");
   public static final Identifier HOTBAR = id("pixel/hotbar");
   public static final Identifier THREAD = id("pixel/thread");
   public static final Identifier FLOOR = id("floor");
   public static final Identifier SHADOW = id("shadow");
   public static final Identifier GLOW = id("glow");
   public static final Identifier CUTTING_EDGE = id("pixel/cutting_edge");
   public static final Identifier CONTROLLER = id("pixel/controller");
   public static final Identifier CONTROL_DARK = id("pixel/controller_dark");
   public static final Identifier CONTROL_BUTTON = id("pixel/controller_button");
   public static final Identifier CONTROL_BLUE = id("pixel/controller_blue");
   public static final Identifier[] DIGITS = IntStream.rangeClosed(1, 10).mapToObj(i -> id("pixel/hotbar_" + i)).toArray(Identifier[]::new);

   public static Identifier id(String name) {
      return Identifier.fromNamespaceAndPath("matinventory", "textures/scene/" + name + ".png");
   }

   public static boolean isPixel(Identifier texture) {
      return texture.getPath().startsWith("textures/scene/pixel/");
   }

   public static void ensureLoaded() {
      if (!loaded) {
         TextureManager manager = Minecraft.getInstance().getTextureManager();

         for (Identifier id : new Identifier[]{LEATHER, CLOTH, METAL, BORDER, HOTBAR, FLOOR, SHADOW, GLOW, WOOD, VIGNETTE, CONTACT, LED}) {
            manager.registerAndLoad(id, new MippedMaterial(id));
         }

         for (Identifier id : DIGITS) {
            manager.registerAndLoad(id, new MippedMaterial(id));
         }

         manager.registerAndLoad(CUTTING_EDGE, new MippedMaterial(CUTTING_EDGE));

         for (Identifier id : new Identifier[]{CONTROLLER, CONTROL_DARK, CONTROL_BUTTON, CONTROL_BLUE}) {
            manager.registerAndLoad(id, new MippedMaterial(id));
         }

         loaded = true;
      }
   }

   public static void invalidate() {
      loaded = false;
      generation++;
   }

   private MatMaterials() {
   }
}
