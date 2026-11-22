package com.example.matinventory.core;

import com.example.matinventory.arrangement.ItemCategoryClassifier;
import com.example.matinventory.arrangement.ItemPresentationData;
import com.example.matinventory.scene.MatCamera;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class LedController {
   private static final int[] COLORS = new int[]{16777215, 4887039, 12151039, 5628550, 16755277};
   public static final float HALF = 0.3F;
   public static final float HALF_Z = 0.23F;
   public static final float BUTTON_X = 0.135F;
   public static final float BUTTON_HALF_X = 0.105F;
   public static final float BUTTON_HALF_Z = 0.125F;
   public static final float CHAMFER = 0.022F;
   public static final float BASE = 0.074F;
   public static final float TOP = 0.194F;
   public static final float BUTTON_TOP = 0.23F;

   public static int color(int index) {
      return COLORS[Math.floorMod(index, COLORS.length)];
   }

   public static int next(int index) {
      return Math.floorMod(index + 1, COLORS.length);
   }

   public static float x() {
      return ItemPresentationData.compartment(ItemCategoryClassifier.Category.CRAFT).x1() - 0.56F;
   }

   public static float z() {
      return ItemPresentationData.compartment(ItemCategoryClassifier.Category.CRAFT).z1() - 0.56F;
   }

   public static float buttonX() {
      return x() + 0.135F;
   }

   public static float matButtonX() {
      return x() - 0.135F;
   }

   public static float[][] leads(boolean grid) {
      ItemPresentationData.Compartment c = ItemPresentationData.compartment(ItemCategoryClassifier.Category.CRAFT);
      float x = grid ? GridController.x() : x();
      float z = grid ? GridController.z() : z();
      return grid
         ? new float[][]{{x - 0.3F + 0.015F, z - 0.11F, c.x0() + 0.055F, z - 0.11F}, {x - 0.11F, z - 0.23F + 0.015F, x - 0.11F, c.z0() + 0.055F}}
         : new float[][]{{x + 0.3F - 0.015F, z - 0.11F, c.x1() - 0.055F, z - 0.11F}, {x - 0.11F, z + 0.23F - 0.015F, x - 0.11F, c.z1() - 0.055F}};
   }

   public static boolean hitMatButton(MatCamera camera, double sx, double sy) {
      return sx >= 0.0
         && sy >= 0.0
         && sx < camera.width
         && sy < camera.height
         && camera.raycastAt(sx, sy, 0.23F)
         && containsButton(camera.hit.x - matButtonX(), camera.hit.z - z());
   }

   public static float[][] buttonShape(float expand) {
      float x = 0.105F + expand;
      float z = 0.125F + expand;
      float c = 0.022F;
      return new float[][]{{-x + c, -z}, {x - c, -z}, {x, -z + c}, {x, z - c}, {x - c, z}, {-x + c, z}, {-x, z - c}, {-x, -z + c}};
   }

   public static boolean containsButton(float dx, float dz) {
      dx = Math.abs(dx);
      dz = Math.abs(dz);
      return dx <= 0.105F && dz <= 0.125F && dx + dz <= 0.20799999F;
   }

   public static boolean hit(MatCamera camera, double sx, double sy, boolean button) {
      if (!(sx < 0.0) && !(sy < 0.0) && !(sx >= camera.width) && !(sy >= camera.height) && camera.raycastAt(sx, sy, button ? 0.23F : 0.194F)) {
         float dx = camera.hit.x - (button ? buttonX() : x());
         float dz = camera.hit.z - z();
         return button ? containsButton(dx, dz) : Math.abs(dx) <= 0.3F && Math.abs(dz) <= 0.23F;
      } else {
         return false;
      }
   }

   private LedController() {
   }
}
