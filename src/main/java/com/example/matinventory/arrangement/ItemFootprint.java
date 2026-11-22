package com.example.matinventory.arrangement;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public record ItemFootprint(
   float width, float depth, float cm, float modelYaw, float preferredYaw, ItemCategoryClassifier.Classification classification, String id, int count
) {
   public float storageScale() {
      return switch (this.classification.category()) {
         case FOOD -> 0.74F;
         case BLOCKS -> 0.57199997F;
         default -> 1.0F;
      };
   }
}
