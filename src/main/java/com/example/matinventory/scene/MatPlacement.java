package com.example.matinventory.scene;

import com.example.matinventory.MatInventoryClient;
import com.example.matinventory.arrangement.ItemPresentationData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

@Environment(EnvType.CLIENT)
public final class MatPlacement {
   public static final double CLEARANCE = 4.0;
   public static final double MAX_SHIFT = 4.0;

   public static float sceneUnit() {
      return 1.0F / (MatInventoryClient.config.groundBlockSizeCm * 0.24F * 1.2F);
   }

   public static MatPlacement.Result find(Level level, Vec3 player, float yaw) {
      return find(level, player, yaw, -4.0, -4.0, 4.0, 4.0, 4.0);
   }

   public static MatPlacement.Result find(Level level, Vec3 player, float yaw, Iterable<ItemPresentationData.Compartment> areas) {
      double x0 = -4.0;
      double z0 = -4.0;
      double x1 = 4.0;
      double z1 = 4.0;
      double angle = Math.toRadians(180.0F - yaw);
      double cos = Math.cos(angle);
      double sin = Math.sin(angle);
      double unit = sceneUnit();

      for (ItemPresentationData.Compartment area : areas) {
         for (double x : new double[]{area.x0() - 0.4, area.x1() + 0.4}) {
            for (double z : new double[]{area.z0() - 0.4, area.z1() + 0.4}) {
               double wx = (x * cos + z * sin) * unit;
               double wz = (-x * sin + z * cos) * unit;
               x0 = Math.min(x0, wx);
               x1 = Math.max(x1, wx);
               z0 = Math.min(z0, wz);
               z1 = Math.max(z1, wz);
            }
         }
      }

      double shift = Math.min(8.0, Math.max(4.0, Math.max(Math.max(-x0, x1), Math.max(-z0, z1))));
      return find(level, player, yaw, x0, z0, x1, z1, shift);
   }

   private static MatPlacement.Result find(Level level, Vec3 player, float yaw, double x0, double z0, double x1, double z1, double maxShift) {
      double y = support(level, player.x, player.y + 0.05, player.z);
      MatPlacement.Result fallback = new MatPlacement.Result(false, player.x, Double.isFinite(y) ? y : player.y, player.z, yaw);
      if (!Double.isFinite(y)) {
         return fallback;
      }

      Map<Long, Boolean> columns = new HashMap<>();
      double angle = Math.toRadians(yaw);
      double backX = Math.sin(angle);
      double backZ = -Math.cos(angle);
      List<MatPlacement.Offset> offsets = new ArrayList<>();
      int range = (int)Math.floor(maxShift * 2.0);

      for (int z = -range; z <= range; z++) {
         for (int x = -range; x <= range; x++) {
            double dx = x * 0.5;
            double dz = z * 0.5;
            double d = dx * dx + dz * dz;
            if (d <= maxShift * maxShift) {
               offsets.add(new MatPlacement.Offset(dx, dz, d, -dx * backX - dz * backZ));
            }
         }
      }

      offsets.sort(Comparator.comparingDouble(MatPlacement.Offset::distance).thenComparingDouble(MatPlacement.Offset::preference));

      for (MatPlacement.Offset offset : offsets) {
         double cx = player.x + offset.x;
         double cz = player.z + offset.z;
         if (clear(level, cx, y, cz, x0, z0, x1, z1, columns)) {
            boolean reachable = true;
            int steps = Math.max(1, (int)Math.ceil(Math.sqrt(offset.distance) * 4.0));

            for (int i = 1; i <= steps; i++) {
               double t = (double)i / steps;
               int x = (int)Math.floor(player.x + offset.x * t);
               int z = (int)Math.floor(player.z + offset.z * t);
               if (!column(level, x, y, z, columns)) {
                  reachable = false;
                  break;
               }
            }

            if (reachable) {
               return new MatPlacement.Result(true, cx, y, cz, yaw);
            }
         }
      }

      return fallback;
   }

   private static boolean clear(Level level, double x, double y, double z, double x0, double z0, double x1, double z1, Map<Long, Boolean> cache) {
      for (int dz = (int)Math.floor(z + z0); dz <= (int)Math.floor(z + z1 - 1.0E-4); dz++) {
         for (int dx = (int)Math.floor(x + x0); dx <= (int)Math.floor(x + x1 - 1.0E-4); dx++) {
            if (!column(level, dx, y, dz, cache)) {
               return false;
            }
         }
      }

      return true;
   }

   private static boolean column(Level level, int x, double y, int z, Map<Long, Boolean> cache) {
      long key = BlockPos.asLong(x, 0, z);
      Boolean known = cache.get(key);
      if (known != null) {
         return known;
      }

      MutableBlockPos p = new MutableBlockPos(x, (int)Math.floor(y), z);
      boolean good = level.hasChunkAt(p) && level.getWorldBorder().isWithinBounds(p);
      if (good) {
         double floor = support(level, x + 0.5, y + 0.05, z + 0.5);
         good = Double.isFinite(floor) && Math.abs(floor - y) < 0.025;
      }

      double height = Math.max(2.5, 14.0F * sceneUnit());
      AABB volume = new AABB(x, y + 0.025, z, x + 1, y + height, z + 1);

      for (int by = (int)Math.floor(y); good && by < Math.ceil(y + height); by++) {
         p.setY(by);
         BlockState state = level.getBlockState(p);
         if (!state.getFluidState().isEmpty()) {
            good = false;
            break;
         }

         if (state.getCollisionShape(level, p).isEmpty() && !state.isAir() && state.getRenderShape() != RenderShape.INVISIBLE) {
            good = false;
            break;
         }

         VoxelShape shape = state.getCollisionShape(level, p);

         for (AABB box : shape.toAabbs()) {
            if (box.move(x, by, z).intersects(volume)) {
               good = false;
               break;
            }
         }
      }

      cache.put(key, good);
      return good;
   }

   private static double support(Level level, double x, double y, double z) {
      MutableBlockPos p = BlockPos.containing(x, y, z).mutable();
      int d = 0;

      while (d <= 16) {
         if (!level.hasChunkAt(p) || p.getY() < level.getMinY()) {
            return Double.NaN;
         }

         BlockState state = level.getBlockState(p);
         if (!state.getFluidState().isEmpty()) {
            return Double.NaN;
         }

         double top = Double.NEGATIVE_INFINITY;

         for (AABB box : state.getCollisionShape(level, p).toAabbs()) {
            if (x >= p.getX() + box.minX
               && x <= p.getX() + box.maxX
               && z >= p.getZ() + box.minZ
               && z <= p.getZ() + box.maxZ
               && p.getY() + box.maxY <= y + 0.025) {
               top = Math.max(top, p.getY() + box.maxY);
            }
         }

         if (Double.isFinite(top)) {
            return top;
         }

         d++;
         p.move(0, -1, 0);
      }

      return Double.NaN;
   }

   private MatPlacement() {
   }

   @Environment(EnvType.CLIENT)
   private record Offset(double x, double z, double distance, double preference) {
   }

   @Environment(EnvType.CLIENT)
   public record Result(boolean world, double x, double y, double z, float yaw) {
   }
}
