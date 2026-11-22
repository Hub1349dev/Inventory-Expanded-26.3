package com.example.matinventory.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class BorderSurface {
   public static final float WIDTH_SCALE = 0.9F;
   public static final float TOP = 0.155F;
   public static final float TEXTURE_TILE = 0.64F;
   public static final float BASE = -0.1098F;
   private static final double EPS = 1.0E-9;

   private static double cross(BorderSurface.Vertex a, BorderSurface.Vertex b, BorderSurface.Vertex c) {
      return (b.x - a.x) * (c.z - a.z) - (b.z - a.z) * (c.x - a.x);
   }

   private static List<BorderSurface.Vertex> clip(List<BorderSurface.Vertex> input, double ax, double az, double c) {
      ArrayList<BorderSurface.Vertex> out = new ArrayList<>();
      if (input.isEmpty()) {
         return out;
      }

      BorderSurface.Vertex previous = input.getLast();
      double was = ax * previous.x + az * previous.z + c;

      for (BorderSurface.Vertex current : input) {
         double value = ax * current.x + az * current.z + c;
         if (value >= 0.0 != was >= 0.0) {
            out.add(previous.mix(current, was / (was - value)));
         }

         if (value >= 0.0) {
            out.add(current);
         }

         previous = current;
         was = value;
      }

      return out;
   }

   private static double area(List<BorderSurface.Vertex> polygon) {
      double sum = 0.0;

      for (int i = 0; i < polygon.size(); i++) {
         BorderSurface.Vertex a = polygon.get(i);
         BorderSurface.Vertex b = polygon.get((i + 1) % polygon.size());
         sum += a.x * b.z - b.x * a.z;
      }

      return sum * 0.5;
   }

   private static void subtract(List<BorderSurface.Vertex> polygon, List<BorderSurface.Vertex> blocker, List<List<BorderSurface.Vertex>> out) {
      List<BorderSurface.Vertex> inside = polygon;

      for (int i = 0; i < blocker.size() && !inside.isEmpty(); i++) {
         BorderSurface.Vertex a = blocker.get(i);
         BorderSurface.Vertex b = blocker.get((i + 1) % blocker.size());
         double ax = a.z - b.z;
         double az = b.x - a.x;
         double c = -(ax * a.x + az * a.z);
         List<BorderSurface.Vertex> outside = clip(inside, -ax, -az, -c);
         if (outside.size() >= 3 && Math.abs(area(outside)) > 1.0E-9) {
            out.add(outside);
         }

         inside = clip(inside, ax, az, c);
      }
   }

   public static List<BorderSurface.Face> weld(List<BorderSurface.Face> faces) {
      ArrayList<BorderSurface.Face> result = new ArrayList<>();

      for (int i = 0; i < faces.size(); i++) {
         BorderSurface.Face face = faces.get(i);
         List<List<BorderSurface.Vertex>> pieces = new ArrayList<>();
         pieces.add(List.of(face.a, face.b, face.c));

         for (int j = 0; j < faces.size() && !pieces.isEmpty(); j++) {
            if (i != j) {
               BorderSurface.Face other = faces.get(j);
               if (face.overlaps(other)) {
                  double dx = other.sx - face.sx;
                  double dz = other.sz - face.sz;
                  double dc = other.offset - face.offset;
                  boolean coplanar = Math.abs(dx) < 1.0E-9 && Math.abs(dz) < 1.0E-9 && Math.abs(dc) < 1.0E-9;
                  if (!coplanar || j <= i) {
                     List<BorderSurface.Vertex> blocker = new ArrayList<>(List.of(other.a, other.b, other.c));
                     if (!coplanar) {
                        blocker = clip(blocker, dx, dz, dc);
                     }

                     if (blocker.size() >= 3 && !(Math.abs(area(blocker)) < 1.0E-9)) {
                        if (area(blocker) < 0.0) {
                           Collections.reverse(blocker);
                        }

                        ArrayList<List<BorderSurface.Vertex>> remaining = new ArrayList<>();

                        for (List<BorderSurface.Vertex> piece : pieces) {
                           subtract(piece, blocker, remaining);
                        }

                        pieces = remaining;
                     }
                  }
               }
            }
         }

         for (List<BorderSurface.Vertex> piece : pieces) {
            for (int k = 1; k < piece.size() - 1; k++) {
               BorderSurface.Vertex a = piece.getFirst();
               BorderSurface.Vertex b = piece.get(k);
               BorderSurface.Vertex c = piece.get(k + 1);
               if (Math.abs(cross(a, b, c)) > 1.0E-9) {
                  result.add(new BorderSurface.Face(a, b, c));
               }
            }
         }
      }

      return result;
   }

   public static double heightAt(List<BorderSurface.Face> faces, double x, double z) {
      BorderSurface.Vertex p = new BorderSurface.Vertex(x, 0.0, z, 0.0, 0.0);
      double height = Double.NEGATIVE_INFINITY;

      for (BorderSurface.Face face : faces) {
         if (!(x < face.x0 - 1.0E-9) && !(x > face.x1 + 1.0E-9) && !(z < face.z0 - 1.0E-9) && !(z > face.z1 + 1.0E-9)) {
            double a = cross(face.a, face.b, p);
            double b = cross(face.b, face.c, p);
            double c = cross(face.c, face.a, p);
            if (a >= -1.0E-9 && b >= -1.0E-9 && c >= -1.0E-9 || a <= 1.0E-9 && b <= 1.0E-9 && c <= 1.0E-9) {
               height = Math.max(height, face.height(x, z));
            }
         }
      }

      return height;
   }

   public static List<BorderSurface.Face> outline(List<MatTopology.Point> loop) {
      ArrayList<BorderSurface.Face> faces = new ArrayList<>();
      if (loop.size() < 3) {
         return faces;
      }

      double perimeter = 0.0;

      for (int i = 0; i < loop.size(); i++) {
         MatTopology.Point a = loop.get(i);
         MatTopology.Point b = loop.get((i + 1) % loop.size());
         perimeter += Math.hypot(b.x() - a.x(), b.z() - a.z());
      }

      double repeats = Math.max(1.0, Math.rint(perimeter / 0.64F));
      double density = repeats / perimeter;
      double[] offsets = new double[]{-0.21149999439716338, -0.1259999966621399, -0.07199999809265137, 0.07199999809265137, 0.19799999475479127};
      double[] heights = new double[]{-0.1098F, 0.1, 0.155F, 0.155F, 0.034};
      double[] crossUv = new double[offsets.length];

      for (int i = 1; i < offsets.length; i++) {
         crossUv[i] = crossUv[i - 1] + Math.hypot(offsets[i] - offsets[i - 1], heights[i] - heights[i - 1]) / 0.64F;
      }

      List<BorderSurface.Vertex> previous = null;

      for (int level = 0; level < offsets.length; level++) {
         ArrayList<BorderSurface.Vertex> ring = new ArrayList<>();
         double along = 0.0;

         for (int i = 0; i < loop.size(); i++) {
            MatTopology.Point a = loop.get((i + loop.size() - 1) % loop.size());
            MatTopology.Point b = loop.get(i);
            MatTopology.Point c = loop.get((i + 1) % loop.size());
            double ab = Math.hypot(b.x() - a.x(), b.z() - a.z());
            double bc = Math.hypot(c.x() - b.x(), c.z() - b.z());
            double ux = (b.x() - a.x()) / ab;
            double uz = (b.z() - a.z()) / ab;
            double vx = (c.x() - b.x()) / bc;
            double vz = (c.z() - b.z()) / bc;
            double scale = offsets[level] / (1.0 + ux * vx + uz * vz);
            double x = b.x() + (-uz - vz) * scale;
            double z = b.z() + (ux + vx) * scale;
            double turn = Math.signum(ux * vz - uz * vx);
            double baseBevel = Math.min(0.14, Math.min(ab, bc) * 0.2);
            double bevel = Math.clamp(baseBevel - (2.0 - Math.sqrt(2.0)) * offsets[level] * turn, 0.008, Math.min(ab, bc) * 0.4);
            double beforeX = x - ux * bevel;
            double beforeZ = z - uz * bevel;
            double afterX = x + vx * bevel;
            double afterZ = z + vz * bevel;
            ring.add(
               new BorderSurface.Vertex(beforeX, heights[level], beforeZ, (along + (beforeX - b.x()) * ux + (beforeZ - b.z()) * uz) * density, crossUv[level])
            );
            ring.add(
               new BorderSurface.Vertex(afterX, heights[level], afterZ, (along + (afterX - b.x()) * vx + (afterZ - b.z()) * vz) * density, crossUv[level])
            );
            along += bc;
         }

         if (previous != null) {
            for (int i = 0; i < ring.size(); i++) {
               int j = (i + 1) % ring.size();
               BorderSurface.Vertex a = previous.get(i);
               BorderSurface.Vertex b = ring.get(i);
               BorderSurface.Vertex c = ring.get(j);
               BorderSurface.Vertex d = previous.get(j);
               if (j == 0) {
                  c = new BorderSurface.Vertex(c.x, c.y, c.z, c.u + repeats, c.v);
                  d = new BorderSurface.Vertex(d.x, d.y, d.z, d.u + repeats, d.v);
               }

               if ((i & 1) == 0) {
                  double tx = d.x - a.x;
                  double tz = d.z - a.z;
                  double n = Math.hypot(tx, tz);
                  tx /= n;
                  tz /= n;
                  double shift = (a.u + d.u) * 0.5 - ((a.x + d.x) * tx + (a.z + d.z) * tz) / 1.28F;
                  a = cornerUv(a, tx, tz, shift);
                  b = cornerUv(b, tx, tz, shift);
                  c = cornerUv(c, tx, tz, shift);
                  d = cornerUv(d, tx, tz, shift);
               }

               add(faces, a, b, c);
               add(faces, a, c, d);
            }
         }

         previous = ring;
      }

      return faces;
   }

   private static BorderSurface.Vertex cornerUv(BorderSurface.Vertex v, double x, double z, double shift) {
      return new BorderSurface.Vertex(v.x, v.y, v.z, (v.x * x + v.z * z) / 0.64F + shift, v.v);
   }

   private static void add(List<BorderSurface.Face> faces, BorderSurface.Vertex a, BorderSurface.Vertex b, BorderSurface.Vertex c) {
      if (Math.abs(cross(a, b, c)) > 1.0E-9) {
         faces.add(new BorderSurface.Face(a, b, c));
      }
   }

   private BorderSurface() {
   }

   @Environment(EnvType.CLIENT)
   public static final class Face {
      public final BorderSurface.Vertex a;
      public final BorderSurface.Vertex b;
      public final BorderSurface.Vertex c;
      public final double sx;
      public final double sz;
      public final double offset;
      public final double x0;
      public final double x1;
      public final double z0;
      public final double z1;

      public Face(BorderSurface.Vertex a, BorderSurface.Vertex b, BorderSurface.Vertex c) {
         this.a = a;
         this.b = b;
         this.c = c;
         double determinant = (b.x - a.x) * (c.z - a.z) - (c.x - a.x) * (b.z - a.z);
         if (Math.abs(determinant) < 1.0E-9) {
            throw new IllegalArgumentException("Vertical/degenerate border face");
         }

         this.sx = ((b.y - a.y) * (c.z - a.z) - (c.y - a.y) * (b.z - a.z)) / determinant;
         this.sz = ((b.x - a.x) * (c.y - a.y) - (c.x - a.x) * (b.y - a.y)) / determinant;
         this.offset = a.y - this.sx * a.x - this.sz * a.z;
         this.x0 = Math.min(a.x, Math.min(b.x, c.x));
         this.x1 = Math.max(a.x, Math.max(b.x, c.x));
         this.z0 = Math.min(a.z, Math.min(b.z, c.z));
         this.z1 = Math.max(a.z, Math.max(b.z, c.z));
      }

      public double height(double x, double z) {
         return this.sx * x + this.sz * z + this.offset;
      }

      public double area() {
         return Math.abs(BorderSurface.cross(this.a, this.b, this.c)) * 0.5;
      }

      boolean overlaps(BorderSurface.Face b) {
         return this.x1 > b.x0 + 1.0E-9 && b.x1 > this.x0 + 1.0E-9 && this.z1 > b.z0 + 1.0E-9 && b.z1 > this.z0 + 1.0E-9;
      }
   }

   @Environment(EnvType.CLIENT)
   public record Vertex(double x, double y, double z, double u, double v) {
      BorderSurface.Vertex mix(BorderSurface.Vertex b, double t) {
         return new BorderSurface.Vertex(
            this.x + (b.x - this.x) * t, this.y + (b.y - this.y) * t, this.z + (b.z - this.z) * t, this.u + (b.u - this.u) * t, this.v + (b.v - this.v) * t
         );
      }
   }
}
