package com.example.matinventory.core;

import com.example.matinventory.arrangement.ItemCategoryClassifier;
import com.example.matinventory.arrangement.ItemPresentationData;
import com.example.matinventory.scene.MatCamera;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class RecipeBookProp {
   public static final float HALF_X = 0.276F;
   public static final float HALF_Z = 0.46F;
   public static final float BASE = 0.074F;
   public static final float TOP = 0.258F;
   public static final float SCALE = 1.4719999F;
   public static final float YAW = 45.0F;
   private static final float C = (float)Math.cos(Math.toRadians(45.0));
   private static final float S = (float)Math.sin(Math.toRadians(45.0));

   public static float x() {
      return ItemPresentationData.compartment(ItemCategoryClassifier.Category.CRAFT).x1() - 0.78F;
   }

   public static float z() {
      return ItemPresentationData.compartment(ItemCategoryClassifier.Category.CRAFT).z0() + 0.78F;
   }

   public static float worldX(float localX, float localZ) {
      return x() + C * localX + S * localZ;
   }

   public static float worldZ(float localX, float localZ) {
      return z() - S * localX + C * localZ;
   }

   public static boolean hit(MatCamera camera, double sx, double sy) {
      if (sx >= 0.0 && sy >= 0.0 && sx < camera.width && sy < camera.height && camera.raycastAt(sx, sy, 0.258F)) {
         float dx = camera.hit.x - x();
         float dz = camera.hit.z - z();
         return Math.abs(C * dx - S * dz) <= 0.276F && Math.abs(S * dx + C * dz) <= 0.46F;
      } else {
         return false;
      }
   }

   private RecipeBookProp() {
   }
}
