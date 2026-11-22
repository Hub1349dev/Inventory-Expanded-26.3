package com.example.matinventory.core;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ScreenLabelPlacer {
   public static ScreenLabelPlacer.Box place(
      ScreenLabelPlacer.Box item, int textWidth, int textHeight, List<ScreenLabelPlacer.Box> occupied, int width, int height
   ) {
      for (int margin = 3; margin <= 63; margin += 10) {
         float[][] candidates = new float[][]{
            {item.right - textWidth, item.bottom + margin},
            {item.right + margin, item.bottom - textHeight},
            {item.left - margin - textWidth, item.bottom - textHeight},
            {item.right - textWidth, item.top - margin - textHeight},
            {item.left, item.bottom + margin},
            {item.left, item.top - margin - textHeight}
         };

         for (float[] candidate : candidates) {
            int x = Math.round(candidate[0]);
            int y = Math.round(candidate[1]);
            ScreenLabelPlacer.Box box = new ScreenLabelPlacer.Box(x - 1, y - 1, x + textWidth + 1, y + textHeight + 1);
            if (!(box.left < 1.0F) && !(box.top < 1.0F) && !(box.right > width - 1) && !(box.bottom > height - 1) && occupied.stream().noneMatch(box::overlaps)
               )
             {
               return box;
            }
         }
      }

      return null;
   }

   private ScreenLabelPlacer() {
   }

   @Environment(EnvType.CLIENT)
   public record Box(float left, float top, float right, float bottom) {
      public boolean overlaps(ScreenLabelPlacer.Box b) {
         return this.left < b.right && this.right > b.left && this.top < b.bottom && this.bottom > b.top;
      }
   }
}
