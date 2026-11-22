package com.example.matinventory.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class MatTopology {
   public static MatTopology.Shape build(List<MatTopology.Rect> rects) {
      TreeSet<Float> xs = new TreeSet<>();
      TreeSet<Float> zs = new TreeSet<>();

      for (MatTopology.Rect r : rects) {
         xs.add(r.x0);
         xs.add(r.x1);
         zs.add(r.z0);
         zs.add(r.z1);
      }

      ArrayList<Float> x = new ArrayList<>(xs);
      ArrayList<Float> z = new ArrayList<>(zs);
      int nx = x.size() - 1;
      int nz = z.size() - 1;
      int[][] cells = new int[nx][nz];

      for (int i = 0; i < nx; i++) {
         for (int j = 0; j < nz; j++) {
            cells[i][j] = -1;
            float cx = (x.get(i) + x.get(i + 1)) / 2.0F;
            float cz = (z.get(j) + z.get(j + 1)) / 2.0F;

            for (int k = 0; k < rects.size(); k++) {
               MatTopology.Rect r = rects.get(k);
               if (cx > r.x0 && cx < r.x1 && cz > r.z0 && cz < r.z1) {
                  cells[i][j] = k;
               }
            }
         }
      }

      LinkedHashMap<MatTopology.Point, MatTopology.Point> border = new LinkedHashMap<>();
      ArrayList<MatTopology.Segment> seams = new ArrayList<>();

      for (int i = 0; i < nx; i++) {
         for (int j = 0; j < nz; j++) {
            if (cells[i][j] >= 0) {
               MatTopology.Point a = new MatTopology.Point(x.get(i), z.get(j));
               MatTopology.Point b = new MatTopology.Point(x.get(i + 1), z.get(j));
               MatTopology.Point c = new MatTopology.Point(x.get(i + 1), z.get(j + 1));
               MatTopology.Point d = new MatTopology.Point(x.get(i), z.get(j + 1));
               if (j == 0 || cells[i][j - 1] < 0) {
                  border.put(a, b);
               }

               if (i != nx - 1 && cells[i + 1][j] >= 0) {
                  if (cells[i + 1][j] != cells[i][j]) {
                     seams.add(new MatTopology.Segment(b, c, false));
                  }
               } else {
                  border.put(b, c);
               }

               if (j != nz - 1 && cells[i][j + 1] >= 0) {
                  if (cells[i][j + 1] != cells[i][j]) {
                     seams.add(new MatTopology.Segment(d, c, false));
                  }
               } else {
                  border.put(c, d);
               }

               if (i == 0 || cells[i - 1][j] < 0) {
                  border.put(d, a);
               }
            }
         }
      }

      ArrayList<MatTopology.Point> loop = new ArrayList<>();
      if (!border.isEmpty()) {
         MatTopology.Point first = border.keySet().iterator().next();
         MatTopology.Point p = first;

         do {
            loop.add(p);
            p = border.get(p);
            if (p == null || loop.size() > border.size()) {
               throw new IllegalArgumentException("Disconnected mat perimeter");
            }
         } while (!p.equals(first));

         if (loop.size() != border.size()) {
            throw new IllegalArgumentException("Mat must have one connected silhouette without holes");
         }

         boolean changed = true;

         while (changed && loop.size() > 3) {
            changed = false;

            for (int i = 0; i < loop.size(); i++) {
               MatTopology.Point a = loop.get((i + loop.size() - 1) % loop.size());
               MatTopology.Point b = loop.get(i);
               MatTopology.Point c = loop.get((i + 1) % loop.size());
               if (a.x == b.x && b.x == c.x || a.z == b.z && b.z == c.z) {
                  loop.remove(i);
                  changed = true;
                  break;
               }
            }
         }
      }

      boolean changed = true;

      label163:
      while (changed) {
         changed = false;

         for (int i = 0; i < seams.size(); i++) {
            for (int j = i + 1; j < seams.size(); j++) {
               MatTopology.Segment a = seams.get(i);
               MatTopology.Segment b = seams.get(j);
               boolean vertical = a.a.x == a.b.x && b.a.x == b.b.x && a.a.x == b.a.x;
               boolean horizontal = a.a.z == a.b.z && b.a.z == b.b.z && a.a.z == b.a.z;
               if (vertical || horizontal) {
                  float a0 = vertical ? a.a.z : a.a.x;
                  float a1 = vertical ? a.b.z : a.b.x;
                  float b0 = vertical ? b.a.z : b.a.x;
                  float b1 = vertical ? b.b.z : b.b.x;
                  if (!(Math.max(a0, b0) > Math.min(a1, b1) + 0.001F)) {
                     MatTopology.Point start = vertical ? new MatTopology.Point(a.a.x, Math.min(a0, b0)) : new MatTopology.Point(Math.min(a0, b0), a.a.z);
                     MatTopology.Point end = vertical ? new MatTopology.Point(a.a.x, Math.max(a1, b1)) : new MatTopology.Point(Math.max(a1, b1), a.a.z);
                     seams.set(i, new MatTopology.Segment(start, end, false));
                     seams.remove(j);
                     changed = true;
                     continue label163;
                  }
               }
            }
         }
      }

      return new MatTopology.Shape(List.copyOf(loop), List.copyOf(seams));
   }

   public static List<MatTopology.Point> rounded(List<MatTopology.Point> loop, float radius) {
      ArrayList<MatTopology.Point> out = new ArrayList<>();

      for (int i = 0; i < loop.size(); i++) {
         MatTopology.Point a = loop.get((i + loop.size() - 1) % loop.size());
         MatTopology.Point b = loop.get(i);
         MatTopology.Point c = loop.get((i + 1) % loop.size());
         float ab = (float)Math.hypot(b.x - a.x, b.z - a.z);
         float bc = (float)Math.hypot(c.x - b.x, c.z - b.z);
         float r = Math.min(radius, Math.min(ab, bc) * 0.3F);
         float sx = b.x + (a.x - b.x) * r / ab;
         float sz = b.z + (a.z - b.z) * r / ab;
         float ex = b.x + (c.x - b.x) * r / bc;
         float ez = b.z + (c.z - b.z) * r / bc;

         for (int k = 0; k <= 8; k++) {
            float t = k / 8.0F;
            float s = 1.0F - t;
            out.add(new MatTopology.Point(s * s * sx + 2.0F * s * t * b.x + t * t * ex, s * s * sz + 2.0F * s * t * b.z + t * t * ez));
         }
      }

      return out;
   }

   private MatTopology() {
   }

   @Environment(EnvType.CLIENT)
   public record Point(float x, float z) {
   }

   @Environment(EnvType.CLIENT)
   public record Rect(float x0, float z0, float x1, float z1) {
   }

   @Environment(EnvType.CLIENT)
   public record Segment(MatTopology.Point a, MatTopology.Point b, boolean exterior) {
   }

   @Environment(EnvType.CLIENT)
   public record Shape(List<MatTopology.Point> perimeter, List<MatTopology.Segment> seams) {
   }
}
