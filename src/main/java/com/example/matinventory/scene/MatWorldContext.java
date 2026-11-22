package com.example.matinventory.scene;

import com.example.matinventory.MatInventoryClient;
import com.example.matinventory.arrangement.ItemPresentationData;
import com.example.matinventory.config.MatConfig;
import com.example.matinventory.core.LedController;
import com.example.matinventory.core.LedPowerState;
import com.example.matinventory.layout.MatLayout;
import com.example.matinventory.render.MatMaterials;
import com.example.matinventory.render.ShaderSupport;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

@Environment(EnvType.CLIENT)
public final class MatWorldContext {
   public final LedPowerState leds = new LedPowerState();
   public MatWorldContext.Tile[] tiles;
   public int radius;
   public int turns;
   public float brightness = 1.0F;
   public float red = 1.0F;
   public float green = 1.0F;
   public float blue = 1.0F;
   private long updated;
   private int materialGeneration = -1;
   private BlockPos sampledSupport;
   private BlockState sampledSurface;
   public static BlockState previewGround;
   public static String previewLight;
   public MatPlacement.Result placement = new MatPlacement.Result(false, 0.0, 0.0, 0.0, 0.0F);
   private boolean placementFitted;
   private int ledGeneration = -1;
   private List<ItemPresentationData.Compartment> layoutAreas = List.of();
   private float[][] ledChannels = new float[0][];
   private int lightGeneration;
   private long stableRevision = Long.MIN_VALUE;
   private boolean stableLight;
   private final Long2ObjectOpenHashMap<MatWorldContext.LightSample> lightSamples = new Long2ObjectOpenHashMap<>();

   public void beginOpening() {
      Minecraft client = Minecraft.getInstance();
      this.placementFitted = false;
      this.placement = previewGround == null && client.level != null && client.player != null
         ? MatPlacement.find(client.level, client.player.position(), client.player.getYRot(), ItemPresentationData.compartments())
         : new MatPlacement.Result(false, 0.0, 0.0, 0.0, 0.0F);
      this.invalidate();
   }

   public void fitOpening(MatLayout layout) {
      if (!this.placementFitted) {
         this.placementFitted = true;
         Minecraft client = Minecraft.getInstance();
         if (previewGround == null && client.level != null && client.player != null) {
            this.placement = MatPlacement.find(client.level, client.player.position(), client.player.getYRot(), layout.solidCompartments());
         }
      }
   }

   public void invalidate() {
      this.updated = 0L;
   }

   public void update() {
      long now = System.nanoTime();
      Minecraft client = Minecraft.getInstance();
      ClientLevel level = client.level;
      LocalPlayer player = client.player;
      MatConfig config = MatInventoryClient.config;
      BlockPos feet = player == null ? new BlockPos(0, 64, 0) : player.getOnPos();
      BlockState surface = previewGround != null ? previewGround : (level == null ? Blocks.STONE.defaultBlockState() : level.getBlockState(feet));
      if (previewGround == null && level != null && surface.isAir()) {
         for (int d = 1; d <= 16; d++) {
            BlockPos below = feet.below(d);
            BlockState state = level.getBlockState(below);
            if (!state.isAir() && !state.getCollisionShape(level, below).isEmpty()) {
               feet = below;
               surface = state;
               break;
            }
         }
      }

      if (previewGround == null && level != null && !surface.isSolidRender()) {
         boolean found = false;

         for (int d = 1; d <= 16; d++) {
            BlockPos below = feet.below(d);
            BlockState state = level.getBlockState(below);
            if (state.isSolidRender()) {
               feet = below;
               surface = state;
               found = true;
               break;
            }
         }

         if (!found) {
            surface = Blocks.GRASS_BLOCK.defaultBlockState();
         }
      }

      if (surface.isAir()) {
         surface = Blocks.STONE.defaultBlockState();
      }

      if (surface.is(Blocks.SNOW)) {
         surface = Blocks.SNOW_BLOCK.defaultBlockState();
      }

      int facing = config.groundAlignToYaw && player != null ? Math.floorMod(Math.round(player.getYRot() / 90.0F) + 2, 4) : 0;
      if (this.tiles == null
         || now - this.updated >= 1000000000L
         || this.materialGeneration != MatMaterials.generation
         || this.radius != config.groundRadius
         || this.turns != facing
         || !feet.equals(this.sampledSupport)
         || surface != this.sampledSurface) {
         this.updated = now;
         this.materialGeneration = MatMaterials.generation;
         this.sampledSupport = feet.immutable();
         this.sampledSurface = surface;
         this.radius = config.groundRadius;
         this.tiles = new MatWorldContext.Tile[(this.radius * 2 + 1) * (this.radius * 2 + 1)];
         this.turns = facing;
         RandomSource random = RandomSource.create(19L);
         random.setSeed(feet.asLong());
         BlockStateModel model = client.getModelManager().getBlockStateModelSet().get(surface);
         TextureAtlasSprite sprite = model.particleMaterial().sprite();
         List<BlockStateModelPart> parts = new ArrayList<>();
         model.collectParts(random, parts);
         int tint = -1;

         for (BlockStateModelPart part : parts) {
            Iterator block = part.getQuads(Direction.UP).iterator();
            if (block.hasNext()) {
               BakedQuad quad = (BakedQuad)block.next();
               sprite = quad.materialInfo().sprite();
               if (quad.materialInfo().isTinted() && level != null) {
                  BlockTintSource source = client.getBlockColors().getTintSource(surface, quad.materialInfo().tintIndex());
                  if (source != null) {
                     tint = source.colorInWorld(surface, level, feet);
                  }
               }
               break;
            }
         }

         if (previewGround != null && surface.is(Blocks.GRASS_BLOCK) && tint == -1) {
            tint = 7640651;
         }

         for (int z = -this.radius; z <= this.radius; z++) {
            for (int x = -this.radius; x <= this.radius; x++) {
               this.tiles[(z + this.radius) * (2 * this.radius + 1) + x + this.radius] = new MatWorldContext.Tile(sprite, tint, this.turns, 1.0F);
            }
         }

         this.brightness = 1.0F;
         this.red = 1.0F;
         this.green = 1.0F;
         this.blue = 1.0F;
         if (config.worldLighting && level != null && player != null) {
            BlockPos lightPos = BlockPos.containing(player.getEyePosition());
            int sky = level.getBrightness(LightLayer.SKY, lightPos);
            int block = level.getBrightness(LightLayer.BLOCK, lightPos);
            long time = Math.floorMod(level.getDefaultClockTime(), 24000);
            float daylight = time < 12000L
               ? 1.0F
               : (time < 13500L ? 1.0F - (float)(time - 12000L) / 1500.0F : (time < 22500L ? 0.0F : (float)(time - 22500L) / 1500.0F));
            this.brightness = Math.max(config.minBrightness, Math.max(sky / 15.0F * (0.25F + 0.75F * daylight), block / 15.0F * 0.85F));
            this.brightness = Math.max(config.minBrightness, this.brightness * (1.0F - level.getRainLevel(1.0F) * 0.18F));
            String dim = level.dimension().identifier().toString();
            if (dim.contains("nether")) {
               this.brightness = Math.max(0.55F, this.brightness);
               this.red = 1.16F;
               this.green = 0.77F;
               this.blue = 0.68F;
            } else if (dim.contains("end")) {
               this.brightness = Math.max(0.48F, this.brightness);
               this.red = 0.88F;
               this.green = 0.82F;
               this.blue = 1.1F;
            } else if (this.brightness < 0.55F) {
               this.red = 0.88F;
               this.green = 0.93F;
               this.blue = 1.08F;
            }
         }

         if (previewLight != null) {
            switch (previewLight) {
               case "night":
               case "cave":
                  this.brightness = config.minBrightness;
                  this.red = 0.88F;
                  this.green = 0.94F;
                  this.blue = 1.08F;
                  break;
               case "nether":
                  this.brightness = 0.58F;
                  this.red = 1.16F;
                  this.green = 0.77F;
                  this.blue = 0.68F;
                  break;
               default:
                  this.brightness = 1.0F;
            }
         }
      }
   }

   public void layout(MatLayout layout) {
      List<ItemPresentationData.Compartment> areas = layout.fabricCompartments();
      if (!this.layoutAreas.equals(areas)) {
         this.layoutAreas = areas;
         this.ledGeneration = -1;
         this.ensureLedChannels();
      }
   }

   private boolean stableLight() {
      long revision = this.leds.revision();
      if (this.stableRevision != revision) {
         this.stableRevision = revision;
         this.stableLight = this.leds.stable();
      }

      return this.stableLight;
   }

   public MatWorldContext.LightSample lightSample(float x, float z) {
      this.ensureLedChannels();
      long key = (long)Float.floatToIntBits(x) << 32 | Integer.toUnsignedLong(Float.floatToIntBits(z));
      MatWorldContext.LightSample sample = this.lightSamples.get(key);
      if (sample == null) {
         sample = new MatWorldContext.LightSample(x, z);
         this.lightSamples.put(key, sample);
      }

      return sample;
   }

   private void ensureLedChannels() {
      if (this.ledGeneration != ItemPresentationData.generation) {
         this.ledGeneration = ItemPresentationData.generation;
         this.ledChannels = (this.layoutAreas.isEmpty() ? ItemPresentationData.ledChannels() : ItemPresentationData.ledChannels(this.layoutAreas))
            .toArray(float[][]::new);
         this.lightGeneration++;
         this.leds.configure(this.ledChannels);
      }
   }

   public void advanceLeds(float dt, LedPowerState.FlashListener listener) {
      this.ensureLedChannels();
      this.leds.advance(dt, listener);
   }

   public float ledPower() {
      return this.leds.enabled() ? 1.0F : 0.0F;
   }

   public int coloredLight() {
      int color = MatInventoryClient.config.ledColor;
      if (color != 0 && this.leds.enabled()) {
         this.ensureLedChannels();
         if (this.ledChannels.length == 0) {
            return 0;
         }

         float lit = 0.0F;

         for (int i = 0; i < this.ledChannels.length; i++) {
            lit += this.leds.gain(i);
         }

         int amount = Math.round(255.0F * lit / this.ledChannels.length);
         return amount == 0 ? 0 : amount << 24 | LedController.color(color);
      } else {
         return 0;
      }
   }

   public float ledSpill(float x, float z) {
      float power = this.ledPower();
      if (power == 0.0F) {
         return 0.0F;
      }

      if (!(x < -42.0F) && !(x > 47.0F) && !(z < -23.0F) && !(z > 19.0F)) {
         this.ensureLedChannels();
         float nearest = Float.POSITIVE_INFINITY;
         boolean stable = this.stableLight();

         for (int i = 0; i < this.ledChannels.length; i++) {
            if (stable || this.leds.gain(i) > 0.0F) {
               nearest = Math.min(nearest, LedPowerState.distanceSquared(this.ledChannels[i], x, z));
            }
         }

         return this.spillAt(nearest);
      } else {
         return 0.0F;
      }
   }

   public boolean naturalTint() {
      int selected = Math.floorMod(MatInventoryClient.config.ledColor, 5);
      return selected != 0 && selected != 2;
   }

   private float spillAt(float distance) {
      return this.naturalTint()
         ? 0.42F * (float)Math.exp(-distance / 11.0F) + 0.09F * (float)Math.exp(-distance / 38.0F)
         : 0.69F * (float)Math.exp(-distance / 2.5F);
   }

   private int ledTint() {
      return LedController.color(MatInventoryClient.config.ledColor);
   }

   public int ledColor() {
      if (!this.leds.enabled()) {
         return -13354441;
      }

      int tint = this.ledTint();
      int r = Math.min(255, Math.round((tint >> 16 & 0xFF) * 1.15F));
      int g = Math.min(255, Math.round((tint >> 8 & 0xFF) * 1.15F));
      int b = Math.min(255, Math.round((tint & 0xFF) * 1.15F));
      if (MatInventoryClient.config.ledColor != 0) {
         float core = this.naturalTint() ? 0.975F : 0.9F;
         r = Math.round(r + (255 - r) * core);
         g = Math.round(g + (255 - g) * core);
         b = Math.round(b + (255 - b) * core);
      }

      return 0xFF000000 | r << 16 | g << 8 | b;
   }

   public int ledColor(float x, float z) {
      return this.leds.gainAt(x, z) > 0.0F ? this.ledColor() : -13354441;
   }

   public int rubberColor(int argb, float x, float z, float nx, float ny, float nz) {
      return this.rubberColor(argb, x, z, nx, ny, nz, this.ledSpill(x, z));
   }

   public int rubberColor(int argb, float x, float z, float nx, float ny, float nz, float spill) {
      if (ShaderSupport.drawing()) {
         return this.color(argb, x, z, nx, ny, nz, false, spill);
      }

      int base = this.color(argb, x, z, nx, ny, nz, true, spill);
      float spec = (float)Math.pow(Math.max(0.0F, -0.22F * nx + 0.96F * ny - 0.16F * nz), 8.0);
      float shine = spec * 7.25F * (float)Math.sqrt(this.brightness);
      float lamp = spill * 3.5F;
      int tint = this.ledTint();
      return base & 0xFF000000
         | Math.min(255, (int)((base >> 16 & 0xFF) + shine + lamp * (tint >> 16 & 0xFF) / 255.0F)) << 16
         | Math.min(255, (int)((base >> 8 & 0xFF) + shine + lamp * (tint >> 8 & 0xFF) / 255.0F)) << 8
         | Math.min(255, (int)((base & 0xFF) + shine + lamp * (tint & 0xFF) / 255.0F));
   }

   public int color(int argb, float x, float z, float nx, float ny, float nz, boolean normal) {
      return this.color(argb, x, z, nx, ny, nz, normal, this.ledSpill(x, z));
   }

   public int color(int argb, float x, float z, float nx, float ny, float nz, boolean normal, float lamp) {
      if (!ShaderSupport.drawing()) {
         MatConfig c = MatInventoryClient.config;
         float gradient = Math.clamp(1.0F - 0.1F * ((x + 18.0F) / 34.0F + (z + 8.4F) / 16.0F), 0.8F, 1.0F);
         float diffuse = normal ? 0.25F + 0.75F * Math.max(0.0F, -0.5F * nx + 0.77F * ny - 0.39F * nz) : 1.0F;
         float perceptual = (float)Math.sqrt(this.brightness);
         float exposure = c.exposure * gradient * diffuse;
         int tint = this.ledTint();
         float lr = (tint >> 16 & 0xFF) / 255.0F;
         float lg = (tint >> 8 & 0xFF) / 255.0F;
         float lb = (tint & 0xFF) / 255.0F;
         float r = argb >> 16 & 0xFF;
         float g = argb >> 8 & 0xFF;
         float b = argb & 0xFF;
         float gray = 0.2126F * r + 0.7152F * g + 0.0722F * b;
         r = (gray + (r - gray) * c.saturation) * exposure * (perceptual + lamp * lr) * (this.red + lamp * 0.08F * lr);
         g = (gray + (g - gray) * c.saturation) * exposure * (perceptual + lamp * lg) * (this.green + lamp * 0.1F * lg);
         b = (gray + (b - gray) * c.saturation) * exposure * (perceptual + lamp * lb) * (this.blue + lamp * 0.12F * lb);
         if (this.leds.enabled() && Math.floorMod(c.ledColor, 5) == 0) {
            float peak = Math.max(r, Math.max(g, b));
            if (peak > 255.0F) {
               float gain = 255.0F / peak;
               r *= gain;
               g *= gain;
               b *= gain;
            }
         }

         return argb & 0xFF000000 | Math.clamp((int)r, 0, 255) << 16 | Math.clamp((int)g, 0, 255) << 8 | Math.clamp((int)b, 0, 255);
      } else {
         int tint = this.ledTint();
         int result = argb & 0xFF000000;
         if (this.naturalTint()) {
            for (int shift = 16; shift >= 0; shift -= 8) {
               float channel = (tint >> shift & 0xFF) / 255.0F;
               int value = Math.round((argb >> shift & 0xFF) * (1.0F - lamp * 0.75F * (1.0F - channel)));
               result |= Math.clamp(value, 0, 255) << shift;
            }

            return result;
         } else {
            for (int shift = 16; shift >= 0; shift -= 8) {
               float glow = lamp * (tint >> shift & 0xFF) / 255.0F;
               int value = Math.min(255, Math.round((argb >> shift & 0xFF) * (1.0F + glow * 0.65F)));
               result |= value << shift;
            }

            return result;
         }
      }
   }

   @Environment(EnvType.CLIENT)
   public final class LightSample {
      private final float x;
      private final float z;
      private int generation = -1;
      private float[] distances;
      private float nearest;
      private long revision = Long.MIN_VALUE;
      private int color = -1;
      private float spill;

      private LightSample(float x, float z) {
         this.x = x;
         this.z = z;
         this.prepare();
      }

      private void prepare() {
         if (this.generation != MatWorldContext.this.lightGeneration) {
            this.generation = MatWorldContext.this.lightGeneration;
            this.revision = Long.MIN_VALUE;
            boolean outside = this.x < -42.0F || this.x > 47.0F || this.z < -23.0F || this.z > 19.0F;
            this.distances = new float[outside ? 0 : MatWorldContext.this.ledChannels.length];
            this.nearest = Float.POSITIVE_INFINITY;

            for (int i = 0; i < this.distances.length; i++) {
               this.distances[i] = LedPowerState.distanceSquared(MatWorldContext.this.ledChannels[i], this.x, this.z);
               this.nearest = Math.min(this.nearest, this.distances[i]);
            }
         }
      }

      public float spill() {
         if (!MatWorldContext.this.leds.enabled()) {
            return 0.0F;
         }

         this.prepare();
         long current = MatWorldContext.this.leds.revision();
         int selected = MatInventoryClient.config.ledColor;
         if (this.revision == current && this.color == selected) {
            return this.spill;
         }

         this.revision = current;
         this.color = selected;
         float distance = this.nearest;
         if (!MatWorldContext.this.stableLight()) {
            distance = Float.POSITIVE_INFINITY;

            for (int i = 0; i < this.distances.length; i++) {
               if (MatWorldContext.this.leds.gain(i) > 0.0F) {
                  distance = Math.min(distance, this.distances[i]);
               }
            }
         }

         return this.spill = MatWorldContext.this.spillAt(distance);
      }
   }

   @Environment(EnvType.CLIENT)
   public record Tile(TextureAtlasSprite sprite, int tint, int quarterTurn, float variation) {
   }
}
