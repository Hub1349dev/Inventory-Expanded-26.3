package com.example.matinventory.core;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ShelfPacker {
   private static float effective(ShelfPacker.Box b, float scale) {
      return Math.max(scale, Math.min(1.0F, b.minimum / Math.max(b.w, b.d)));
   }

   private static List<ShelfPacker.Position> shelves(List<ShelfPacker.Box> list, ShelfPacker.Rect r, float scale, float pad, float gap) {
      if (list.isEmpty()) {
         return List.of();
      }

      List<ShelfPacker.Position> best = null;
      float cost = Float.POSITIVE_INFINITY;

      for (int cols = r.id.equals("LONG_TOOLS") ? list.size() : 1; cols <= list.size(); cols++) {
         int rows = (list.size() + cols - 1) / cols;
         float[] depths = new float[rows];
         float[] widths = new float[rows];
         float usedD = 0.0F;
         boolean fits = true;

         for (int row = 0; row < rows; row++) {
            int n = Math.min(cols, list.size() - row * cols);

            for (int col = 0; col < n; col++) {
               ShelfPacker.Box b = list.get(row * cols + col);
               widths[row] += b.w * effective(b, scale);
               depths[row] = Math.max(depths[row], b.d * effective(b, scale));
            }

            if (widths[row] + gap * (n - 1) > r.width() - 2.0F * pad) {
               fits = false;
               break;
            }

            usedD += depths[row];
         }

         if (fits && !(usedD + gap * (rows - 1) > r.depth() - 2.0F * pad)) {
            float maxW = 0.0F;

            for (float w : widths) {
               maxW = Math.max(maxW, w);
            }

            float score = Math.abs(maxW / (r.width() - 2.0F * pad) - usedD / (r.depth() - 2.0F * pad))
               + 0.012F * rows
               + 0.3F * (rows * cols - list.size()) / list.size();
            if (!(score >= cost)) {
               cost = score;
               ArrayList<ShelfPacker.Position> positions = new ArrayList<>();
               float zg = gap;
               float z = r.z0 + (r.depth() - usedD - gap * (rows - 1)) / 2.0F;

               for (int row = 0; row < rows; row++) {
                  int n = Math.min(cols, list.size() - row * cols);
                  float xg = gap;
                  float x = r.x0 + (r.width() - widths[row] - gap * (n - 1)) / 2.0F;

                  for (int col = 0; col < n; col++) {
                     ShelfPacker.Box b = list.get(row * cols + col);
                     float w = b.w * effective(b, scale);
                     float d = b.d * effective(b, scale);
                     positions.add(new ShelfPacker.Position(b.id, r.id, x + w / 2.0F, z + depths[row] / 2.0F, w, d));
                     x += w + xg;
                  }

                  z += depths[row] + zg;
               }

               best = positions;
            }
         }
      }

      return best;
   }

   public static ShelfPacker.Result pack(List<ShelfPacker.Box> boxes, List<ShelfPacker.Rect> bins, float padding, float gap) {
      ArrayList<ShelfPacker.Position> result = new ArrayList<>();
      float minimumScale = 1.0F;

      for (ShelfPacker.Box box : boxes) {
         if (bins.stream().noneMatch(rx -> rx.id.equals(box.category))) {
            throw new IllegalArgumentException("No compartment for " + box.category);
         }
      }

      for (ShelfPacker.Rect r : bins) {
         List<ShelfPacker.Box> group = boxes.stream().filter(b -> b.category.equals(r.id)).toList();
         if (!group.isEmpty()) {
            List<ShelfPacker.Position> placed = shelves(group, r, 1.0F, padding, gap);
            float selected = 1.0F;
            if (placed == null) {
               throw new IllegalStateException("Fixed-size category capacity exceeded: " + r.id);
            }

            minimumScale = Math.min(minimumScale, selected);
            result.addAll(placed);
         }
      }

      return new ShelfPacker.Result(List.copyOf(result), minimumScale);
   }

   public static int windowOffset(List<ShelfPacker.Box> boxes, ShelfPacker.Rect bin, int offset, float padding, float gap) {
      int first = Math.clamp(offset, 0, Math.max(0, boxes.size() - 1));

      while (first > 0 && shelves(boxes.subList(first - 1, boxes.size()), bin, 1.0F, padding, gap) != null) {
         first--;
      }

      return first;
   }

   public static ShelfPacker.Result window(List<ShelfPacker.Box> boxes, ShelfPacker.Rect bin, int offset, float padding, float gap) {
      if (boxes.isEmpty()) {
         return new ShelfPacker.Result(List.of(), 1.0F);
      }

      int start = windowOffset(boxes, bin, offset, padding, gap);
      List<ShelfPacker.Position> best = null;

      for (int end = start + 1; end <= boxes.size(); end++) {
         List<ShelfPacker.Position> attempt = shelves(boxes.subList(start, end), bin, 1.0F, padding, gap);
         if (attempt == null) {
            break;
         }

         best = attempt;
      }

      if (best == null) {
         throw new IllegalStateException("Item exceeds compartment: " + bin.id);
      } else {
         return new ShelfPacker.Result(List.copyOf(best), 1.0F);
      }
   }

   public static boolean overlaps(ShelfPacker.Position a, ShelfPacker.Position b) {
      return Math.abs(a.x - b.x) < (a.w + b.w) / 2.0F - 1.0E-4F && Math.abs(a.z - b.z) < (a.d + b.d) / 2.0F - 1.0E-4F;
   }

   private ShelfPacker() {
   }

   @Environment(EnvType.CLIENT)
   public record Box(int id, String category, float w, float d, float minimum) {
      public Box(int id, String category, float w, float d) {
         this(id, category, w, d, 0.8F);
      }
   }

   @Environment(EnvType.CLIENT)
   public record Position(int id, String bin, float x, float z, float w, float d) {
   }

   @Environment(EnvType.CLIENT)
   public record Rect(String id, float x0, float z0, float x1, float z1) {
      public float width() {
         return this.x1 - this.x0;
      }

      public float depth() {
         return this.z1 - this.z0;
      }

      public boolean contains(float x, float z, float w, float d) {
         return x - w / 2.0F >= this.x0 - 1.0E-4F
            && x + w / 2.0F <= this.x1 + 1.0E-4F
            && z - d / 2.0F >= this.z0 - 1.0E-4F
            && z + d / 2.0F <= this.z1 + 1.0E-4F;
      }
   }

   @Environment(EnvType.CLIENT)
   public record Result(List<ShelfPacker.Position> positions, float scale) {
   }
}
