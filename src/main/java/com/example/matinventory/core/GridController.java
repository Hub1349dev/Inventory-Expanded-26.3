package com.example.matinventory.core;

import com.example.matinventory.arrangement.ItemCategoryClassifier;
import com.example.matinventory.arrangement.ItemPresentationData;
import com.example.matinventory.scene.MatCamera;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class GridController {
   public static final float HALF = 0.3F;
   public static final float TOP = 0.23F;

   public static float x() {
      return ItemPresentationData.compartment(ItemCategoryClassifier.Category.CRAFT).x0() + 0.64F;
   }

   public static float z() {
      return ItemPresentationData.compartment(ItemCategoryClassifier.Category.CRAFT).z0() + 0.64F;
   }

   public static boolean hit(MatCamera camera, double x, double y) {
      return x >= 0.0
         && y >= 0.0
         && x < camera.width
         && y < camera.height
         && camera.raycastAt(x, y, 0.23F)
         && LedController.containsButton(camera.hit.x - x(), camera.hit.z - z());
   }

   public static boolean bodyHit(MatCamera camera, double x, double y) {
      return x >= 0.0
         && y >= 0.0
         && x < camera.width
         && y < camera.height
         && camera.raycastAt(x, y, 0.194F)
         && Math.abs(camera.hit.x - x()) <= 0.3F
         && Math.abs(camera.hit.z - z()) <= 0.23F;
   }

   private GridController() {
   }
}
