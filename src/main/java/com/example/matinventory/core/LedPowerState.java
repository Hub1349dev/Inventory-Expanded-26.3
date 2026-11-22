package com.example.matinventory.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class LedPowerState {
   private final long seed;
   private long activation;
   private long revision;
   private double clock;
   private boolean enabled;
   private boolean startupFlicker;
   private List<LedPowerState.Lamp> lamps = List.of();

   public LedPowerState() {
      this(System.nanoTime());
   }

   public LedPowerState(long seed) {
      this.seed = seed;
   }

   public void configure(float[][] paths) {
      boolean starting = this.startupFlicker && (this.lamps.isEmpty() || !this.stable());
      HashMap<LedPowerState.Key, LedPowerState.Lamp> old = new HashMap<>();

      for (LedPowerState.Lamp lamp : this.lamps) {
         old.put(lamp.key, lamp);
      }

      ArrayList<LedPowerState.Lamp> next = new ArrayList<>();

      for (float[] path : paths) {
         LedPowerState.Key key = new LedPowerState.Key(
            Float.floatToIntBits(path[0]), Float.floatToIntBits(path[1]), Float.floatToIntBits(path[2]), Float.floatToIntBits(path[3])
         );
         LedPowerState.Lamp lamp = old.get(key);
         if (lamp == null) {
            lamp = new LedPowerState.Lamp(key, path);
            if (this.enabled && starting) {
               this.start(lamp);
            } else {
               lamp.lit = this.enabled;
            }
         }

         next.add(lamp);
      }

      if (!next.equals(this.lamps)) {
         this.lamps = next;
         this.revision++;
      }
   }

   private void start(LedPowerState.Lamp lamp) {
      long channelSeed = this.seed ^ this.activation * -7046029254386353131L;

      for (int bits : new int[]{lamp.key.x0, lamp.key.z0, lamp.key.x1, lamp.key.z1}) {
         channelSeed = (channelSeed ^ Integer.toUnsignedLong(bits)) * 1099511628211L;
      }

      lamp.random = new Random(channelSeed);
      lamp.lit = false;
      lamp.pulses = 3 + lamp.random.nextInt(3);
      lamp.next = this.clock + 0.03 + lamp.random.nextDouble() * 0.22;
   }

   public void toggle() {
      this.enabled = !this.enabled;
      this.activation++;
      this.revision++;
      this.startupFlicker = this.enabled && new Random(this.seed ^ this.activation * -7046029254386353131L).nextDouble() < 0.35;

      for (LedPowerState.Lamp lamp : this.lamps) {
         if (this.startupFlicker) {
            this.start(lamp);
         } else {
            lamp.lit = this.enabled;
            lamp.pulses = 0;
            lamp.next = Double.POSITIVE_INFINITY;
         }
      }
   }

   public void restore(boolean on) {
      boolean changed = this.enabled != on || !this.stable();
      this.enabled = on;
      this.startupFlicker = false;

      for (LedPowerState.Lamp lamp : this.lamps) {
         lamp.lit = on;
         lamp.pulses = 0;
         lamp.next = Double.POSITIVE_INFINITY;
      }

      if (changed) {
         this.revision++;
      }
   }

   public void reset() {
      this.enabled = false;
      this.startupFlicker = false;
      this.activation++;
      this.revision++;

      for (LedPowerState.Lamp lamp : this.lamps) {
         lamp.lit = false;
         lamp.next = Double.POSITIVE_INFINITY;
      }
   }

   public void advance(float dt, LedPowerState.FlashListener listener) {
      this.clock = this.clock + Math.clamp(dt, 0.0F, 0.25F);
      if (this.enabled) {
         for (LedPowerState.Lamp lamp : this.lamps) {
            while (this.clock >= lamp.next) {
               lamp.lit = !lamp.lit;
               this.revision++;
               if (lamp.lit) {
                  lamp.pulses--;
                  listener.flash((lamp.path[0] + lamp.path[2]) * 0.5F, (lamp.path[1] + lamp.path[3]) * 0.5F);
                  lamp.next = lamp.pulses == 0 ? Double.POSITIVE_INFINITY : this.clock + 0.055 + lamp.random.nextDouble() * 0.12;
               } else {
                  lamp.next = this.clock + 0.035 + lamp.random.nextDouble() * 0.12;
               }
            }
         }
      }
   }

   public boolean enabled() {
      return this.enabled;
   }

   public long revision() {
      return this.revision;
   }

   public float gain(int index) {
      return this.enabled && index >= 0 && index < this.lamps.size() && this.lamps.get(index).lit ? 1.0F : 0.0F;
   }

   public boolean stable() {
      for (LedPowerState.Lamp lamp : this.lamps) {
         if (Double.isFinite(lamp.next)) {
            return false;
         }
      }

      return true;
   }

   public float gainAt(float x, float z) {
      if (!this.enabled) {
         return 0.0F;
      }

      float nearest = Float.POSITIVE_INFINITY;
      float gain = 0.0F;

      for (LedPowerState.Lamp lamp : this.lamps) {
         float distance = distanceSquared(lamp.path, x, z);
         if (distance < nearest) {
            nearest = distance;
            gain = lamp.lit ? 1.0F : 0.0F;
         }
      }

      return gain;
   }

   public static float distanceSquared(float[] path, float x, float z) {
      float vx = path[2] - path[0];
      float vz = path[3] - path[1];
      float length = vx * vx + vz * vz;
      float t = length < 1.0E-6F ? 0.0F : Math.clamp(((x - path[0]) * vx + (z - path[1]) * vz) / length, 0.0F, 1.0F);
      float dx = x - path[0] - t * vx;
      float dz = z - path[1] - t * vz;
      return dx * dx + dz * dz;
   }

   @Environment(EnvType.CLIENT)
   public interface FlashListener {
      void flash(float var1, float var2);
   }

   @Environment(EnvType.CLIENT)
   private record Key(int x0, int z0, int x1, int z1) {
   }

   @Environment(EnvType.CLIENT)
   private static final class Lamp {
      final LedPowerState.Key key;
      final float[] path;
      Random random;
      boolean lit;
      int pulses;
      double next = Double.POSITIVE_INFINITY;

      Lamp(LedPowerState.Key key, float[] path) {
         this.key = key;
         this.path = (float[])path.clone();
      }
   }
}
