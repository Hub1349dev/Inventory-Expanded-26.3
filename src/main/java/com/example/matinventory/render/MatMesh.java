package com.example.matinventory.render;

import com.example.matinventory.MatInventoryClient;
import com.example.matinventory.animation.MatFold;
import com.example.matinventory.arrangement.ItemCategoryClassifier;
import com.example.matinventory.arrangement.ItemPresentationData;
import com.example.matinventory.capacity.EquipmentKinds;
import com.example.matinventory.config.MatConfig;
import com.example.matinventory.core.BorderSurface;
import com.example.matinventory.core.GearPadSurface;
import com.example.matinventory.core.GridController;
import com.example.matinventory.core.LedController;
import com.example.matinventory.core.MatPalette;
import com.example.matinventory.core.MatTopology;
import com.example.matinventory.core.RecipeBookProp;
import com.example.matinventory.layout.MatLayout;
import com.example.matinventory.scene.MatWorldContext;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public final class MatMesh {
   private static final Vector3f vertexPoint = new Vector3f();
   private static final Vector3f vertexNormal = new Vector3f();
   private static final Matrix4f groundClip = new Matrix4f();
   private static final FrustumIntersection groundFrustum = new FrustumIntersection();
   public static long groundSubmitted;
   public static long groundCulled;
   private static final Map<Integer, MatMesh.Geometry> geometries = new LinkedHashMap<Integer, MatMesh.Geometry>(8, 0.75F, true) {
      @Override
      protected boolean removeEldestEntry(Entry<Integer, MatMesh.Geometry> entry) {
         return this.size() > 8;
      }
   };

   private static void emit(Pose pose, VertexConsumer out, float x, float y, float z, int color, float u, float v, float nx, float ny, float nz) {
      pose.pose().transformPosition(x, y, z, vertexPoint);
      pose.normal().transform(nx, ny, nz, vertexNormal);
      out.addVertex(
         vertexPoint.x, vertexPoint.y, vertexPoint.z, color, u, v, OverlayTexture.NO_OVERLAY, 15728880, vertexNormal.x, vertexNormal.y, vertexNormal.z
      );
   }

   public static float padHeight(MatLayout layout, float x, float z) {
      ItemPresentationData.Compartment c = ItemPresentationData.compartment(ItemCategoryClassifier.Category.CRAFT);
      return GearPadSurface.height((x - c.x0() - 0.24F) / (c.width() - 0.48F), (z - c.z0() - 0.23F) / (c.depth() - 0.46F));
   }

   public static float itemSupport(MatLayout layout, float x, float z, float halfWidth, float halfDepth) {
      if (!layout.onCloth(x, z)) {
         return 0.095F;
      }

      float height = 0.074F;

      for (int i = -1; i <= 1; i++) {
         for (int j = -1; j <= 1; j++) {
            height = Math.max(height, padHeight(layout, x + i * halfWidth, z + j * halfDepth));
         }
      }

      return height + 0.022F;
   }

   public static float equipmentSupport(MatLayout layout, float x, float z, float halfWidth, float halfDepth) {
      if (!layout.onCloth(x, z)) {
         return layout.compartmentAt(x, z) == null ? 0.05F : 0.042000003F;
      }

      float height = 0.074F;

      for (int i = -2; i <= 2; i++) {
         for (int j = -2; j <= 2; j++) {
            height = Math.max(height, padHeight(layout, x + i * halfWidth * 0.5F, z + j * halfDepth * 0.5F));
         }
      }

      return height + 0.008F;
   }

   private static void flush(SceneBuffers buffers) {
      buffers.endBatch();
   }

   public static void draw(PoseStack pose, SceneBuffers buffers, MatSceneState state) {
      MatMaterials.ensureLoaded();
      if (!state.embedded && !state.actualWorld()) {
         drawGround(pose, buffers, state);
      }

      MatLayout layout = state.layout();
      float width = 12.0F;
      float depth = 6.857F;
      float flash = state.flash();
      int key = (
               ((ItemPresentationData.generation * 997 + Float.floatToIntBits(MatInventoryClient.config.railHeightCm)) * 31 + layout.compartments().hashCode())
                     * 31
                  + layout.solidCompartments().hashCode()
            )
            * 31
         + layout.equipmentCells().hashCode();
      key = key * 31 + layout.gridSize();
      key = key * 31 + (MatInventoryClient.config.gridEnabled ? 1 : 0);
      MatMesh.Geometry geometry = geometries.get(key);
      if (geometry == null) {
         geometry = new MatMesh.Geometry(layout, width, depth, key);
         geometries.put(key, geometry);
      }

      state.fold.surfaceBounds(geometry.surfaceBottom, geometry.surfaceTop);
      flush(buffers);
      if (!state.embedded && !state.fold.active()) {
         geometry.groundShadow.draw(pose, buffers, state.world, null);
      }

      flush(buffers);

      for (MatMesh.Part part : geometry.parts) {
         if (part != geometry.shadow && part != geometry.groundShadow && part != geometry.metal && part != geometry.led) {
            if (part == geometry.cloth && state.shake != 0.0F) {
               pose.pushPose();
               pose.translate(state.shake, 0.0F, 0.0F);
               part.draw(pose, buffers, state.world, state.fold);
               pose.popPose();
            } else {
               part.draw(pose, buffers, state.world, state.fold);
            }
         }
      }

      flush(buffers);
      geometry.shadow.draw(pose, buffers, state.world, state.fold);
      flush(buffers);
      geometry.metal.draw(pose, buffers, state.world, state.fold);
      geometry.led.draw(pose, buffers, state.world, state.fold);
      if (!state.fold.active()) {
         if (state.controllerHover > 0.01F) {
            controllerOutline(pose, buffers, state.controllerHover);
         }

         for (ItemPresentationData.Compartment c : layout.compartments()) {
            if (layout.compartmentPreview(c.id())) {
               VertexConsumer v = buffers.getBuffer(MatRenderTypes.decal(MatMaterials.HOTBAR));
               vertex(pose, v, c.x0(), 0.045F, c.z0(), c.x0() / 8.0F, c.z0() / 8.0F, 1224736767);
               vertex(pose, v, c.x0(), 0.045F, c.z1(), c.x0() / 8.0F, c.z1() / 8.0F, 1224736767);
               vertex(pose, v, c.x1(), 0.045F, c.z1(), c.x1() / 8.0F, c.z1() / 8.0F, 1224736767);
               vertex(pose, v, c.x1(), 0.045F, c.z0(), c.x1() / 8.0F, c.z0() / 8.0F, 1224736767);
               outline(pose, buffers, layout, new MatLayout.DropZone(c.x0(), c.z0(), c.x1(), c.z1(), true), 16777215, 1.0F);
            }
         }

         for (MatLayout.DropZone zone : state.dropZones) {
            if (state.capacityZone == null
               || zone.x0() < state.capacityZone.x0() - 0.01F
               || zone.z0() < state.capacityZone.z0() - 0.01F
               || zone.x1() > state.capacityZone.x1() + 0.01F
               || zone.z1() > state.capacityZone.z1() + 0.01F) {
               outline(pose, buffers, layout, zone, 16777215, 1.0F);
            }
         }

         if (state.capacityZone != null) {
            outline(pose, buffers, layout, state.capacityZone, 16721189, state.capacityStrength);
         }
      }
   }

   private static void controllerOutline(PoseStack pose, SceneBuffers buffers, float alpha) {
      VertexConsumer out = buffers.getBuffer(MatRenderTypes.decal(MatMaterials.GLOW));
      float x = LedController.buttonX();
      float z = LedController.z();
      float y = 0.231F;
      int color = (int)(alpha * 220.0F) << 24 | 16777215;
      float[][] inner = LedController.buttonShape(-0.01F);
      float[][] outer = LedController.buttonShape(0.004F);

      for (int i = 0; i < 8; i++) {
         int j = (i + 1) % 8;

         for (float[] p : new float[][]{inner[i], outer[i], outer[j], inner[j]}) {
            vertex(pose, out, x + p[0], y, z + p[1], 0.5F, 0.5F, color);
         }
      }
   }

   private static void outline(PoseStack pose, SceneBuffers buffers, MatLayout layout, MatLayout.DropZone zone, int rgb, float strength) {
      float x0 = zone.x0();
      float x1 = zone.x1();
      float z0 = zone.z0();
      float z1 = zone.z1();
      float cx = (x0 + x1) * 0.5F;
      float cz = (z0 + z1) * 0.5F;
      float y = layout.onCloth(cx, cz) ? 0.083000004F : 0.1F;
      if (rgb != 16777215) {
         y = Math.max(y, 0.24F);
      }

      for (int layer = 0; layer < 2; layer++) {
         float edge = layer == 0 ? 0.055F : 0.12F;
         int alpha = zone.hovered() ? (layer == 0 ? 235 : 100) : (layer == 0 ? 130 : 45);
         int color = (int)(alpha * strength) << 24 | rgb;
         decal(pose, buffers, MatMaterials.GLOW, cx, y, z0, (x1 - x0) * 0.5F, edge, color);
         decal(pose, buffers, MatMaterials.GLOW, cx, y, z1, (x1 - x0) * 0.5F, edge, color);
         decal(pose, buffers, MatMaterials.GLOW, x0, y, cz, edge, (z1 - z0) * 0.5F, color);
         decal(pose, buffers, MatMaterials.GLOW, x1, y, cz, edge, (z1 - z0) * 0.5F, color);
      }

      int core = (int)((zone.hovered() ? 245 : 180) * strength) << 24 | rgb;
      whiteStrip(pose, buffers, cx, y + 0.002F, z0, (x1 - x0) * 0.5F, 0.014F, core);
      whiteStrip(pose, buffers, cx, y + 0.002F, z1, (x1 - x0) * 0.5F, 0.014F, core);
      whiteStrip(pose, buffers, x0, y + 0.002F, cz, 0.014F, (z1 - z0) * 0.5F, core);
      whiteStrip(pose, buffers, x1, y + 0.002F, cz, 0.014F, (z1 - z0) * 0.5F, core);
   }

   private static void drawGround(PoseStack pose, SceneBuffers buffers, MatSceneState state) {
      MatWorldContext world = state.world;
      MatConfig config = MatInventoryClient.config;
      float size = config.groundBlockSizeCm * 0.24F;
      int rangeX = (int)Math.ceil(Math.max(state.overview ? 75.0F : 30.0F, state.deckHalfWidth) / size) + 2;
      int rangeZ = (int)Math.ceil(Math.max(state.overview ? 75.0F : 30.0F, state.deckHalfDepth) / size) + 2;
      state.projection().mul(state.view(), groundClip);
      groundFrustum.set(groundClip);

      for (int z = -rangeZ; z <= rangeZ; z++) {
         for (int x = -rangeX; x <= rangeX; x++) {
            if (!groundFrustum.testAab(
               (x - 0.5F) * size - 0.01F, -0.12F, (z - 0.5F) * size - 0.01F, (x + 0.5F) * size + 0.01F, -0.1F, (z + 0.5F) * size + 0.01F
            )) {
               groundCulled++;
            } else {
               groundSubmitted++;
               if (!config.groundEnabled) {
                  decal(pose, buffers, MatMaterials.FLOOR, x * size, -0.11F, z * size, size / 2.0F, size / 2.0F, -8880505);
               } else {
                  int sx = Math.clamp(x, -world.radius, world.radius);
                  int sz = Math.clamp(z, -world.radius, world.radius);
                  MatWorldContext.Tile tile = world.tiles[(sz + world.radius) * (world.radius * 2 + 1) + sx + world.radius];
                  TextureAtlasSprite sprite = tile.sprite();
                  VertexConsumer out = buffers.getBuffer(MatRenderTypes.ground(sprite.atlasLocation()));
                  if (out == DiscardVertices.INSTANCE) {
                     return;
                  }

                  int raw = tile.tint() == -1 ? -1 : 0xFF000000 | tile.tint();

                  for (int i = 0; i < 4; i++) {
                     int uv = Math.floorMod(i + tile.quarterTurn(), 4);
                     float px = (x + (i != 0 && i != 1 ? 0.5F : -0.5F)) * size;
                     float pz = (z + (i != 0 && i != 3 ? 0.5F : -0.5F)) * size;
                     int color = world.color(raw, px, pz, 0.0F, 1.0F, 0.0F, true);
                     float factor = tile.variation() * 0.64F;
                     int r = (int)((color >> 16 & 0xFF) * factor);
                     int g = (int)((color >> 8 & 0xFF) * factor);
                     int b = (int)((color & 0xFF) * factor);
                     color = 0xFF000000 | r << 16 | g << 8 | b;
                     vertex(pose, out, px, -0.11F, pz, uv < 2 ? sprite.getU0() : sprite.getU1(), uv != 0 && uv != 3 ? sprite.getV1() : sprite.getV0(), color);
                  }
               }
            }
         }
      }
   }

   public static void decal(PoseStack p, SceneBuffers buffers, Identifier texture, float x, float y, float z, float rx, float rz, int color) {
      VertexConsumer v = buffers.getBuffer(MatRenderTypes.decal(texture));
      vertex(p, v, x - rx, y, z - rz, 0.0F, 0.0F, color);
      vertex(p, v, x - rx, y, z + rz, 0.0F, 1.0F, color);
      vertex(p, v, x + rx, y, z + rz, 1.0F, 1.0F, color);
      vertex(p, v, x + rx, y, z - rz, 1.0F, 0.0F, color);
   }

   private static void whiteStrip(PoseStack p, SceneBuffers buffers, float x, float y, float z, float rx, float rz, int color) {
      VertexConsumer v = buffers.getBuffer(MatRenderTypes.decal(MatMaterials.GLOW));
      vertex(p, v, x - rx, y, z - rz, 0.5F, 0.5F, color);
      vertex(p, v, x - rx, y, z + rz, 0.5F, 0.5F, color);
      vertex(p, v, x + rx, y, z + rz, 0.5F, 0.5F, color);
      vertex(p, v, x + rx, y, z - rz, 0.5F, 0.5F, color);
   }

   public static void halo(PoseStack p, SceneBuffers buffers, float x, float z, float r, float strength) {
      decal(p, buffers, MatMaterials.GLOW, x, 0.078F, z, r, r, (int)(strength * 110.0F) << 24 | 16777215);
   }

   private static void vertex(PoseStack p, VertexConsumer v, float x, float y, float z, float u, float t, int color) {
      emit(p.last(), v, x, y, z, color, u, t, 0.0F, 1.0F, 0.0F);
   }

   private static void top(MatMesh.Part p, float x0, float y, float z0, float x1, float z1, float u0, float v0, float u1, float v1, int c) {
      face(p, x0, y, z0, x0, y, z1, x1, y, z1, x1, y, z0, u0, v0, u1, v1, c, 0.0F, 1.0F, 0.0F);
   }

   private static void face(
      MatMesh.Part p,
      float ax,
      float ay,
      float az,
      float bx,
      float by,
      float bz,
      float cx,
      float cy,
      float cz,
      float dx,
      float dy,
      float dz,
      float u0,
      float v0,
      float u1,
      float v1,
      int c,
      float nx,
      float ny,
      float nz
   ) {
      p.build.add(new MatMesh.Vertex(ax, ay, az, u0, v0, c, nx, ny, nz));
      p.build.add(new MatMesh.Vertex(bx, by, bz, u0, v1, c, nx, ny, nz));
      p.build.add(new MatMesh.Vertex(cx, cy, cz, u1, v1, c, nx, ny, nz));
      p.build.add(new MatMesh.Vertex(dx, dy, dz, u1, v0, c, nx, ny, nz));
   }

   private static void box(MatMesh.Part p, float x0, float y0, float z0, float x1, float y1, float z1, int c) {
      if (p.type == MatRenderTypes.material(MatMaterials.CLOTH)) {
         float w = x1 - x0 - 0.08F;
         float d = z1 - z0 - 0.08F;
         float columns = Math.max(1, Math.round(w));
         float rows = Math.max(1, Math.round(d));
         gridTop(
            p, x0, y1, z0, x1, z1, -0.04F / w * columns * 0.5F, -0.04F / d * rows * 0.5F, (w + 0.04F) / w * columns * 0.5F, (d + 0.04F) / d * rows * 0.5F, c
         );
      } else {
         top(p, x0, y1, z0, x1, z1, x0 / 2.0F, z0 / 2.0F, x1 / 2.0F, z1 / 2.0F, c);
      }

      float undersideUv = p.type == MatRenderTypes.material(MatMaterials.TARP) ? 8.0F : 2.0F;
      p.build.add(new MatMesh.Vertex(x0, y0, z0, x0 / undersideUv, z0 / undersideUv, c, 0.0F, -1.0F, 0.0F));
      p.build.add(new MatMesh.Vertex(x1, y0, z0, x1 / undersideUv, z0 / undersideUv, c, 0.0F, -1.0F, 0.0F));
      p.build.add(new MatMesh.Vertex(x1, y0, z1, x1 / undersideUv, z1 / undersideUv, c, 0.0F, -1.0F, 0.0F));
      p.build.add(new MatMesh.Vertex(x0, y0, z1, x0 / undersideUv, z1 / undersideUv, c, 0.0F, -1.0F, 0.0F));
      face(p, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0 / 2.0F, -y0 / 2.0F, x1 / 2.0F, -y1 / 2.0F, c, 0.0F, 0.0F, 1.0F);
      face(p, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, x0 / 2.0F, -y0 / 2.0F, x1 / 2.0F, -y1 / 2.0F, c, 0.0F, 0.0F, -1.0F);
      face(p, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, z0 / 2.0F, -y0 / 2.0F, z1 / 2.0F, -y1 / 2.0F, c, -1.0F, 0.0F, 0.0F);
      face(p, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, z0 / 2.0F, -y0 / 2.0F, z1 / 2.0F, -y1 / 2.0F, c, 1.0F, 0.0F, 0.0F);
   }

   private static void gridTop(MatMesh.Part p, float x0, float y, float z0, float x1, float z1, float u0, float v0, float u1, float v1, int color) {
      float x = x0;

      while (x < x1) {
         float nextX = Math.min(x1, (float)Math.floor(x + 1.0E-5F) + 1.0F);
         float z = z0;

         while (z < z1) {
            float nextZ = Math.min(z1, (float)Math.floor(z + 1.0E-5F) + 1.0F);
            top(
               p,
               x,
               y,
               z,
               nextX,
               nextZ,
               u0 + (u1 - u0) * (x - x0) / (x1 - x0),
               v0 + (v1 - v0) * (z - z0) / (z1 - z0),
               u0 + (u1 - u0) * (nextX - x0) / (x1 - x0),
               v0 + (v1 - v0) * (nextZ - z0) / (z1 - z0),
               color
            );
            z = nextZ;
         }

         x = nextX;
      }
   }

   private MatMesh() {
   }

   @Environment(EnvType.CLIENT)
   private static final class Geometry {
      final MatMesh.Part floor = new MatMesh.Part(MatRenderTypes.material(MatMaterials.FLOOR));
      final MatMesh.Part felt = new MatMesh.Part(MatRenderTypes.material(MatMaterials.LEATHER));
      final MatMesh.Part belt = new MatMesh.Part(MatRenderTypes.material(MatMaterials.HOTBAR));
      final MatMesh.Part border = new MatMesh.Part(MatRenderTypes.material(MatMaterials.BORDER));
      final MatMesh.Part cloth = new MatMesh.Part(MatRenderTypes.material(MatMaterials.CLOTH));
      final MatMesh.Part cuttingEdge = new MatMesh.Part(MatRenderTypes.material(MatMaterials.CUTTING_EDGE));
      final MatMesh.Part metal = new MatMesh.Part(MatRenderTypes.material(MatMaterials.METAL));
      final MatMesh.Part thread = new MatMesh.Part(MatRenderTypes.material(MatMaterials.THREAD));
      final MatMesh.Part shadow = new MatMesh.Part(MatRenderTypes.decal(MatMaterials.CONTACT));
      final MatMesh.Part groundShadow = new MatMesh.Part(MatRenderTypes.decal(MatMaterials.CONTACT));
      final MatMesh.Part tarp = new MatMesh.Part(MatRenderTypes.material(MatMaterials.TARP));
      final MatMesh.Part wood = new MatMesh.Part(MatRenderTypes.material(MatMaterials.WOOD));
      final MatMesh.Part led = new MatMesh.Part(MatRenderTypes.material(MatMaterials.LED));
      final MatMesh.Part controller = new MatMesh.Part(MatRenderTypes.material(MatMaterials.CONTROLLER));
      final MatMesh.Part controllerDark = new MatMesh.Part(MatRenderTypes.material(MatMaterials.CONTROL_DARK));
      final MatMesh.Part controllerButton = new MatMesh.Part(MatRenderTypes.material(MatMaterials.CONTROL_BUTTON));
      final MatMesh.Part controllerBlue = new MatMesh.Part(MatRenderTypes.material(MatMaterials.CONTROL_BLUE));
      final MatMesh.Part[] digits = IntStream.range(0, 10)
         .mapToObj(i -> new MatMesh.Part(MatRenderTypes.material(MatMaterials.DIGITS[i])))
         .toArray(MatMesh.Part[]::new);
      final MatMesh.Part[] parts;
      float surfaceBottom = Float.POSITIVE_INFINITY;
      float surfaceTop = Float.NEGATIVE_INFINITY;
      final float width;
      final float depth;
      final int shapeKey;

      Geometry(MatLayout layout, float width, float depth, int shapeKey) {
         this.width = width;
         this.depth = depth;
         this.shapeKey = shapeKey;
         this.parts = Stream.concat(
               Stream.of(
                  this.tarp,
                  this.felt,
                  this.belt,
                  this.cloth,
                  this.cuttingEdge,
                  this.shadow,
                  this.groundShadow,
                  this.wood,
                  this.metal,
                  this.border,
                  this.thread,
                  this.led,
                  this.controller,
                  this.controllerDark,
                  this.controllerButton,
                  this.controllerBlue
               ),
               Arrays.stream(this.digits)
            )
            .toArray(MatMesh.Part[]::new);

         for (ItemPresentationData.Compartment c : layout.solidCompartments()) {
            MatMesh.box(this.tarp, c.x0(), -0.1098F, c.z0(), c.x1(), 0.005F, c.z1(), -987931);
            this.shadow(c.x0() - 0.24F, -0.107F, c.z0() - 0.24F, c.x1() + 0.24F, c.z1() + 0.24F, 0.48F);
         }

         MatTopology.Shape shape = MatTopology.build(
            layout.solidCompartments().stream().map(cx -> new MatTopology.Rect(cx.x0(), cx.z0(), cx.x1(), cx.z1())).toList()
         );

         for (ItemPresentationData.Compartment c : layout.solidCompartments()) {
            float x0 = c.x0();
            float x1 = c.x1();
            float z0 = c.z0();
            float z1 = c.z1();
            int tint = -987931;
            MatMesh.Part surface = c.category() != ItemCategoryClassifier.Category.HOTBAR
                  && c.category() != ItemCategoryClassifier.Category.VEHICLES
                  && !c.id().startsWith("extra_tools")
                  && !c.id().equals("extra_shield")
                  && !c.id().equals("extra_elytra")
                  && !c.id().startsWith("extra_blocks_")
               ? this.felt
               : this.belt;
            float tile = 0.125F;
            MatMesh.gridTop(surface, x0, 0.034F, z0, x1, z1, x0 / tile, z0 / tile, x1 / tile, z1 / tile, tint);
            if (c.category() == ItemCategoryClassifier.Category.CRAFT) {
               MatMesh.box(this.cloth, x0 + 0.1F, 0.034F, z0 + 0.1F, x1 - 0.1F, 0.074F, z1 - 0.1F, -921363);
               float a = x0 + 0.14F;
               float b = x1 - 0.14F;
               float t = z0 + 0.14F;
               float d = z1 - 0.14F;
               float y = 0.0755F;
               float wx = (b - a) / (Math.max(1, Math.round(b - a)) * 8);
               float wz = (d - t) / (Math.max(1, Math.round(d - t)) * 8);
               MatMesh.top(this.cuttingEdge, a, y, t, b, t + wz, 0.0F, 0.0F, 1.0F, 1.0F, -921363);
               MatMesh.top(this.cuttingEdge, a, y, d - wz, b, d, 0.0F, 0.0F, 1.0F, 1.0F, -921363);
               MatMesh.top(this.cuttingEdge, a, y, t + wz, a + wx, d - wz, 0.0F, 0.0F, 1.0F, 1.0F, -921363);
               MatMesh.top(this.cuttingEdge, b - wx, y, t + wz, b, d - wz, 0.0F, 0.0F, 1.0F, 1.0F, -921363);
            }

            float inset = c.category() == ItemCategoryClassifier.Category.CRAFT ? 0.1F : 0.0F;
            float sy = c.category() == ItemCategoryClassifier.Category.CRAFT ? 0.0748F : 0.0348F;
            float a = x0 + inset;
            float b = x1 - inset;
            float t = z0 + inset;
            float d = z1 - inset;
            float w = Math.min(0.34F, Math.min(b - a, d - t) * 0.25F);
            MatMesh.top(this.shadow, a, sy, t, b, t + w, 0.5F, 0.0F, 0.5F, 1.0F, 620756991);
            MatMesh.top(this.shadow, a, sy, d - w, b, d, 0.5F, 0.0F, 0.5F, 1.0F, 620756991);
            MatMesh.top(this.shadow, a, sy, t, a + w, d, 0.0F, 0.5F, 1.0F, 0.5F, 620756991);
            MatMesh.top(this.shadow, b - w, sy, t, b, d, 0.0F, 0.5F, 1.0F, 0.5F, 620756991);
         }

         this.joinedBorder(layout, shape);
         ItemPresentationData.Compartment hotbar = ItemPresentationData.compartment(ItemCategoryClassifier.Category.HOTBAR);
         float slotWidth = (hotbar.width() - 0.56F) / 9.0F;

         for (int i = 0; i < 9; i++) {
            float x = hotbar.x0() + 0.28F + (i + 0.5F) * slotWidth;
            MatMesh.top(this.digits[i], x - 0.36F, 0.035F, hotbar.z1() - 0.82F, x + 0.36F, hotbar.z1() - 0.1F, 0.0F, 0.0F, 1.0F, 1.0F, -1);
         }

         if (layout.offhandSlot() >= 0) {
            float x = hotbar.x0() + 0.28F - layout.offhandWidth() * 0.5F;
            MatMesh.top(this.digits[9], x - 0.36F, 0.035F, hotbar.z1() - 0.82F, x + 0.36F, hotbar.z1() - 0.1F, 0.0F, 0.0F, 1.0F, 1.0F, -1);
         }

         for (MatLayout.EquipmentCell cell : layout.equipmentCells()) {
            int icon = EquipmentKinds.DEFAULT.indexOf(cell.kind());
            if (icon >= 0 && icon < 4) {
               this.etching((cell.x0() + cell.x1()) * 0.5F, (cell.z0() + cell.z1()) * 0.5F, icon);
            }
         }

         for (float[] channel : ItemPresentationData.ledChannels(layout.fabricCompartments())) {
            this.ledStrip(channel[0], channel[1], channel[2], channel[3], 0.17F);
         }

         this.controller();
         this.gridController();
         float bw = 0.31599998F;
         float bd = 0.5F;
         float[] xx = new float[4];
         float[] zz = new float[4];
         int bi = 0;

         for (float[] p : new float[][]{{-bw, -bd}, {-bw, bd}, {bw, bd}, {bw, -bd}}) {
            xx[bi] = RecipeBookProp.worldX(p[0], p[1]);
            zz[bi] = RecipeBookProp.worldZ(p[0], p[1]);
            bi++;
         }

         MatMesh.face(
            this.shadow,
            xx[0],
            0.0747F,
            zz[0],
            xx[1],
            0.0747F,
            zz[1],
            xx[2],
            0.0747F,
            zz[2],
            xx[3],
            0.0747F,
            zz[3],
            0.0F,
            0.0F,
            1.0F,
            1.0F,
            1207959551,
            0.0F,
            1.0F,
            0.0F
         );
         float left = Float.POSITIVE_INFINITY;
         float right = -left;

         for (ItemPresentationData.Compartment c : layout.solidCompartments()) {
            left = Math.min(left, c.x0());
            right = Math.max(right, c.x1());
         }

         for (MatMesh.Part p : this.parts) {
            if (p != this.shadow && p != this.groundShadow) {
               for (MatMesh.Vertex v : p.build) {
                  this.surfaceBottom = Math.min(this.surfaceBottom, v.y);
                  this.surfaceTop = Math.max(this.surfaceTop, v.y);
               }
            }

            p.finish(left, right);
         }

         this.surfaceTop = Math.max(this.surfaceTop, 0.258F);
      }

      private void controller() {
         float x = LedController.x();
         float z = LedController.z();
         float h = 0.3F;
         float d = 0.23F;
         this.shadow(x - h - 0.04F, 0.0747F, z - d - 0.04F, x + h + 0.04F, z + d + 0.04F, 0.24F);
         this.controlHousing(x, z, -1);

         for (float[] lead : LedController.leads(false)) {
            this.controlCable(lead);
         }

         this.controlButton(x - 0.135F, z, false);
         this.controlButton(x + 0.135F, z, true);
      }

      private void gridController() {
         float x = GridController.x();
         float z = GridController.z();
         float h = 0.3F;
         float d = 0.23F;
         this.shadow(x - h - 0.04F, 0.0747F, z - d - 0.04F, x + h + 0.04F, z + d + 0.04F, 0.24F);
         this.controlHousing(x, z, -11510684);

         for (float[] lead : LedController.leads(true)) {
            this.controlCable(lead);
         }

         this.controlButton(x, z, false, -10786960, true);
      }

      private static float[][] chamfer(float x, float z, float cut) {
         return new float[][]{{-x + cut, -z}, {x - cut, -z}, {x, -z + cut}, {x, z - cut}, {x - cut, z}, {-x + cut, z}, {-x, z - cut}, {-x, -z + cut}};
      }

      private void controlHousing(float x, float z, int color) {
         float[][] outer = chamfer(0.3F, 0.23F, 0.035F);
         this.controlProfile(this.controllerDark, x, z, new float[][][]{outer, outer}, new float[]{0.074F, 0.086F}, -9142653);
         this.controlProfile(
            this.controller, x, z, new float[][][]{outer, outer, chamfer(0.282F, 0.21200001F, 0.03F)}, new float[]{0.086F, 0.175F, 0.194F}, color
         );

         for (float sx : new float[]{-0.252F, 0.252F}) {
            for (float sz : new float[]{-0.172F, 0.172F}) {
               float[][] head = chamfer(0.013F, 0.013F, 0.004F);
               this.controlProfile(this.controllerDark, x + sx, z + sz, new float[][][]{head, head}, new float[]{0.1945F, 0.19600001F}, -5656141);
               MatMesh.top(this.controller, x + sx - 0.008F, 0.1965F, z + sz - 0.002F, x + sx + 0.008F, z + sz + 0.002F, 0.0F, 0.0F, 1.0F, 1.0F, -1841179);
            }
         }
      }

      private void controlProfile(MatMesh.Part part, float x, float z, float[][][] rings, float[] heights, int color) {
         for (int level = 1; level < rings.length; level++) {
            for (int i = 0; i < rings[level].length; i++) {
               int j = (i + 1) % rings[level].length;
               float[] a = rings[level - 1][i];
               float[] b = rings[level][i];
               float[] c = rings[level][j];
               float[] d = rings[level - 1][j];
               this.modelFace(
                  part,
                  new float[]{x + a[0], heights[level - 1], z + a[1]},
                  new float[]{x + b[0], heights[level], z + b[1]},
                  new float[]{x + c[0], heights[level], z + c[1]},
                  new float[]{x + d[0], heights[level - 1], z + d[1]},
                  color,
                  (float)i / rings[level].length,
                  (heights[level - 1] - heights[0]) / (heights[heights.length - 1] - heights[0]),
                  (float)(i + 1) / rings[level].length,
                  (heights[level] - heights[0]) / (heights[heights.length - 1] - heights[0])
               );
            }
         }

         for (int cap : new int[]{0, rings.length - 1}) {
            float[][] ring = rings[cap];
            float w = 0.0F;
            float d = 0.0F;

            for (float[] p : ring) {
               w = Math.max(w, Math.abs(p[0]));
               d = Math.max(d, Math.abs(p[1]));
            }

            for (int i = 0; i < ring.length; i++) {
               int j = (i + 1) % ring.length;
               float[] a = ring[cap == 0 ? i : j];
               float[] b = ring[cap == 0 ? j : i];

               for (float[] p : new float[][]{{0.0F, 0.0F}, a, b, b}) {
                  part.build
                     .add(
                        new MatMesh.Vertex(
                           x + p[0], heights[cap], z + p[1], p[0] / (2.0F * w) + 0.5F, p[1] / (2.0F * d) + 0.5F, color, 0.0F, cap == 0 ? -1.0F : 1.0F, 0.0F
                        )
                     );
               }
            }
         }
      }

      private void modelFace(MatMesh.Part part, float[] a, float[] b, float[] c, float[] d, int color, float u0, float v0, float u1, float v1) {
         float bx = b[0] - a[0];
         float by = b[1] - a[1];
         float bz = b[2] - a[2];
         float cx = c[0] - a[0];
         float cy = c[1] - a[1];
         float cz = c[2] - a[2];
         float nx = by * cz - bz * cy;
         float ny = bz * cx - bx * cz;
         float nz = bx * cy - by * cx;
         float n = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
         if (!(n < 1.0E-7F)) {
            MatMesh.face(part, a[0], a[1], a[2], b[0], b[1], b[2], c[0], c[1], c[2], d[0], d[1], d[2], u0, v0, u1, v1, color, nx / n, ny / n, nz / n);
         }
      }

      private void controlCable(float[] lead) {
         float length = (float)Math.hypot(lead[2] - lead[0], lead[3] - lead[1]);
         float dx = (lead[2] - lead[0]) / length;
         float dz = (lead[3] - lead[1]) / length;
         float[] steps = new float[]{0.0F, 0.06F, 0.085F, length};
         float[] radii = new float[]{0.032F, 0.032F, 0.026F, 0.026F};
         float[][] previous = null;

         for (int level = 0; level < steps.length; level++) {
            float[][] ring = new float[8][3];

            for (int i = 0; i < 8; i++) {
               double angle = i * Math.PI / 4.0;
               float side = radii[level] * (float)Math.cos(angle);
               ring[i] = new float[]{
                  lead[0] + dx * steps[level] - dz * side, 0.101F + radii[level] * (float)Math.sin(angle), lead[1] + dz * steps[level] + dx * side
               };
            }

            if (previous != null) {
               for (int i = 0; i < 8; i++) {
                  int j = (i + 1) % 8;
                  this.modelFace(
                     this.controllerDark,
                     previous[i],
                     ring[i],
                     ring[j],
                     previous[j],
                     -6248278,
                     steps[level - 1] / 0.15F,
                     i / 8.0F,
                     steps[level] / 0.15F,
                     (i + 1) / 8.0F
                  );
               }
            }

            if (level == 0 || level == steps.length - 1) {
               for (int i = 0; i < 8; i++) {
                  int j = (i + 1) % 8;
                  float[] centre = new float[]{lead[0] + dx * steps[level], 0.101F, lead[1] + dz * steps[level]};
                  this.modelFace(
                     this.controllerDark,
                     centre,
                     ring[level == 0 ? j : i],
                     ring[level == 0 ? i : j],
                     ring[level == 0 ? i : j],
                     -6248278,
                     0.0F,
                     0.0F,
                     1.0F,
                     1.0F
                  );
               }
            }

            previous = ring;
         }

         this.shadow(
            Math.min(lead[0], lead[2]) - 0.035F,
            0.0747F,
            Math.min(lead[1], lead[3]) - 0.035F,
            Math.max(lead[0], lead[2]) + 0.035F,
            Math.max(lead[1], lead[3]) + 0.035F,
            0.18F
         );
      }

      private void controlButton(float x, float z, boolean ledButton) {
         this.controlButton(x, z, ledButton, -1, false);
      }

      private void controlButton(float x, float z, boolean ledButton, int capColor, boolean gridIcon) {
         float[][] well = LedController.buttonShape(0.01F);
         float[][] path = LedController.buttonShape(0.0F);
         this.controlProfile(this.controllerDark, x, z, new float[][][]{well, well}, new float[]{0.1945F, 0.19600001F}, -9142653);
         this.controlProfile(
            this.controllerButton,
            x,
            z,
            new float[][][]{path, path, LedController.buttonShape(-0.008F)},
            new float[]{0.19600001F, 0.21900001F, 0.23F},
            capColor
         );
         float y = 0.2306F;
         if (gridIcon) {
            int color = MatInventoryClient.config.gridEnabled ? -2038041 : -5722191;

            for (int i = -1; i <= 1; i++) {
               float p = i * 0.043F;
               MatMesh.top(this.controllerButton, x - 0.065F, y, z + p - 0.005F, x + 0.065F, z + p + 0.005F, 0.0F, 0.0F, 1.0F, 1.0F, color);
               MatMesh.top(this.controllerButton, x + p - 0.005F, y, z - 0.065F, x + p + 0.005F, z + 0.065F, 0.0F, 0.0F, 1.0F, 1.0F, color);
            }
         } else if (ledButton) {
            for (int i = 0; i < 10; i++) {
               double a = Math.toRadians(-55 + i * 29);
               double b = a + Math.toRadians(29.0);
               float r = 0.059F;
               float inner = 0.041F;
               MatMesh.face(
                  this.controllerBlue,
                  x + inner * (float)Math.cos(a),
                  y,
                  z + inner * (float)Math.sin(a),
                  x + r * (float)Math.cos(a),
                  y,
                  z + r * (float)Math.sin(a),
                  x + r * (float)Math.cos(b),
                  y,
                  z + r * (float)Math.sin(b),
                  x + inner * (float)Math.cos(b),
                  y,
                  z + inner * (float)Math.sin(b),
                  0.0F,
                  0.0F,
                  1.0F,
                  1.0F,
                  -1,
                  0.0F,
                  1.0F,
                  0.0F
               );
            }

            MatMesh.top(this.controllerBlue, x - 0.007F, y + 2.0E-4F, z - 0.067F, x + 0.007F, z - 0.012F, 0.0F, 0.0F, 1.0F, 1.0F, -1);
         } else {
            MatMesh.top(this.controllerDark, x - 0.038F, y, z - 0.038F, x + 0.038F, z + 0.038F, 0.0F, 0.0F, 1.0F, 1.0F, -5787470);
            MatMesh.top(this.controller, x - 0.029F, y + 2.0E-4F, z - 0.029F, x + 0.029F, z + 0.029F, 0.0F, 0.0F, 1.0F, 1.0F, -1775391);
         }
      }

      private void ledStrip(float x0, float z0, float x1, float z1, float support) {
         float length = (float)Math.hypot(x1 - x0, z1 - z0);
         float nx = -(z1 - z0) / length;
         float nz = (x1 - x0) / length;
         this.stripProfile(
            this.metal,
            x0,
            z0,
            x1,
            z1,
            new float[]{-0.09F, -0.065F, 0.065F, 0.09F},
            new float[]{support - 0.025F, support + 0.025F, support + 0.025F, support - 0.025F},
            -2039584
         );
         this.stripProfile(
            this.led,
            x0,
            z0,
            x1,
            z1,
            new float[]{-0.045F, -0.032F, 0.032F, 0.045F},
            new float[]{support + 0.026F, support + 0.04F, support + 0.04F, support + 0.026F},
            -1,
            support + 0.025F
         );

         for (float t : new float[]{0.0F, length - 0.1F}) {
            float ax = x0 + (x1 - x0) * t / length;
            float az = z0 + (z1 - z0) * t / length;
            float bx = ax + (x1 - x0) * 0.1F / length;
            float bz = az + (z1 - z0) * 0.1F / length;
            MatMesh.box(
               this.metal,
               Math.min(ax, bx) - Math.abs(nx) * 0.09F,
               support - 0.025F,
               Math.min(az, bz) - Math.abs(nz) * 0.09F,
               Math.max(ax, bx) + Math.abs(nx) * 0.09F,
               support + 0.045F,
               Math.max(az, bz) + Math.abs(nz) * 0.09F,
               -3618616
            );
         }
      }

      private void stripProfile(MatMesh.Part part, float x0, float z0, float x1, float z1, float[] offsets, float[] heights, int color) {
         this.stripProfile(part, x0, z0, x1, z1, offsets, heights, color, Float.NaN);
      }

      private void stripProfile(MatMesh.Part part, float x0, float z0, float x1, float z1, float[] offsets, float[] heights, int color, float contact) {
         float length = (float)Math.hypot(x1 - x0, z1 - z0);
         float nx = -(z1 - z0) / length;
         float nz = (x1 - x0) / length;

         for (int i = 0; i < offsets.length - 1; i++) {
            float a = offsets[i];
            float b = offsets[i + 1];
            float ya = heights[i];
            float yb = heights[i + 1];
            float normalX = -nx * (yb - ya);
            float normalY = b - a;
            float normalZ = -nz * (yb - ya);
            float inv = 1.0F / (float)Math.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
            float first = offsets[0];
            float span = offsets[offsets.length - 1] - first;
            MatMesh.face(
               part,
               x0 + nx * a,
               ya,
               z0 + nz * a,
               x0 + nx * b,
               yb,
               z0 + nz * b,
               x1 + nx * b,
               yb,
               z1 + nz * b,
               x1 + nx * a,
               ya,
               z1 + nz * a,
               0.0F,
               (a - first) / span,
               length / 0.8F,
               (b - first) / span,
               color,
               normalX * inv,
               normalY * inv,
               normalZ * inv
            );
         }

         if (Float.isFinite(contact)) {
            for (int side : new int[]{0, offsets.length - 1}) {
               float offset = offsets[side];
               float sign = side == 0 ? -1.0F : 1.0F;
               float v = side == 0 ? 0.0F : 1.0F;
               MatMesh.face(
                  part,
                  x0 + nx * offset,
                  contact,
                  z0 + nz * offset,
                  x0 + nx * offset,
                  heights[side],
                  z0 + nz * offset,
                  x1 + nx * offset,
                  heights[side],
                  z1 + nz * offset,
                  x1 + nx * offset,
                  contact,
                  z1 + nz * offset,
                  0.0F,
                  v,
                  length / 0.8F,
                  v,
                  color,
                  nx * sign,
                  0.0F,
                  nz * sign
               );
            }
         }
      }

      private void joinedBorder(MatLayout layout, MatTopology.Shape shape) {
         ArrayList<MatMesh.Geometry.BorderRun> runs = new ArrayList<>();

         for (MatTopology.Segment seam : shape.seams()) {
            runs.add(new MatMesh.Geometry.BorderRun(seam.a().x(), seam.a().z(), seam.b().x(), seam.b().z(), false));
         }

         List<MatTopology.Point> outline = shape.perimeter();

         for (int i = 0; i < outline.size(); i++) {
            MatTopology.Point a = outline.get(i);
            MatTopology.Point b = outline.get((i + 1) % outline.size());
            runs.add(new MatMesh.Geometry.BorderRun(a.x(), a.z(), b.x(), b.z(), true));
         }

         ArrayList<BorderSurface.Face> faces = new ArrayList<>(BorderSurface.outline(outline));

         for (MatMesh.Geometry.BorderRun run : runs) {
            if (!run.exterior) {
               this.borderTube(run, layout, faces);
            }
         }

         for (BorderSurface.Face face : BorderSurface.weld(faces)) {
            float inv = 1.0F / (float)Math.sqrt(1.0 + face.sx * face.sx + face.sz * face.sz);

            for (BorderSurface.Vertex v : new BorderSurface.Vertex[]{face.a, face.b, face.c, face.c}) {
               this.border
                  .build
                  .add(
                     new MatMesh.Vertex(
                        (float)v.x(), (float)v.y(), (float)v.z(), (float)v.u(), (float)v.v(), -723724, (float)(-face.sx) * inv, inv, (float)(-face.sz) * inv
                     )
                  );
            }
         }

         for (MatMesh.Geometry.BorderRun run : runs) {
            if (!(Math.abs(run.z1 - run.z0) < 1.0E-5F)) {
               float length = (float)Math.hypot(run.x1 - run.x0, run.z1 - run.z0);
               float dx = (run.x1 - run.x0) / length;
               float dz = (run.z1 - run.z0) / length;
               float along = run.x0 * dx + run.z0 * dz;

               for (float t = (float)Math.ceil(along / 0.4F) * 0.4F + 0.075F; t < along + length - 0.25F; t += 0.4F) {
                  if (!(t < along + 0.25F)) {
                     float x = run.x0 + dx * (t - along);
                     float z = run.z0 + dz * (t - along);
                     boolean junction = false;

                     for (MatMesh.Geometry.BorderRun other : runs) {
                        if (other != run) {
                           float vx = other.x1 - other.x0;
                           float vz = other.z1 - other.z0;
                           float u = Math.clamp(((x - other.x0) * vx + (z - other.z0) * vz) / (vx * vx + vz * vz), 0.0F, 1.0F);
                           if (Math.hypot(x - other.x0 - u * vx, z - other.z0 - u * vz) < 0.25) {
                              junction = true;
                              break;
                           }
                        }
                     }

                     if (!junction) {
                        boolean flat = true;

                        for (float a : new float[]{-0.025F, 0.025F}) {
                           for (float b : new float[]{-0.068399996F, 0.068399996F}) {
                              if (Math.abs(BorderSurface.heightAt(faces, x + dx * a - dz * b, z + dz * a + dx * b) - 0.155F) > 1.0E-5) {
                                 flat = false;
                              }
                           }
                        }

                        if (flat) {
                           this.stitch(x, z, dx, dz, 0.068399996F, 0.155F);
                        }
                     }
                  }
               }
            }
         }
      }

      private void borderTube(MatMesh.Geometry.BorderRun run, MatLayout layout, ArrayList<BorderSurface.Face> faces) {
         double length = Math.hypot(run.x1 - run.x0, run.z1 - run.z0);
         double dx = (run.x1 - run.x0) / length;
         double dz = (run.z1 - run.z0) / length;
         float[] radii = new float[]{0.072F, 0.126F};
         BorderSurface.Vertex[] previous = null;

         for (int level = 0; level < radii.length; level++) {
            double r = radii[level];
            double span = radii[radii.length - 1];
            double[][] points = new double[][]{{0.0, r}, {length, r}, {length, -r}, {0.0, -r}};
            BorderSurface.Vertex[] ring = new BorderSurface.Vertex[points.length];

            for (int i = 0; i < points.length; i++) {
               double x = run.x0 + dx * points[i][0] - dz * points[i][1];
               double z = run.z0 + dz * points[i][0] + dx * points[i][1];
               boolean inside = false;

               for (ItemPresentationData.Compartment c : layout.solidCompartments()) {
                  if (c.contains((float)x, (float)z)) {
                     inside = true;
                     break;
                  }
               }

               double y = 0.155F;
               if (level > 0) {
                  if (run.exterior && (level <= 1 || !inside)) {
                     if (level == 1) {
                        y = 0.12;
                     } else {
                        y = level == 2 ? 0.015 : -0.10499999940395355;
                     }
                  } else {
                     y = 0.034;
                  }
               }

               double cross = level == 0 ? points[i][1] : Math.signum(points[i][1]) * (radii[0] + Math.hypot(r - radii[0], 0.155F - y));
               ring[i] = new BorderSurface.Vertex(x, y, z, (x * dx + z * dz) / 0.64F, cross / 0.64F);
            }

            if (previous == null) {
               BorderSurface.Vertex centre = new BorderSurface.Vertex(
                  (run.x0 + run.x1) * 0.5, 0.155F, (run.z0 + run.z1) * 0.5, ((run.x0 + run.x1) * dx + (run.z0 + run.z1) * dz) / 1.28F, 0.0
               );

               for (int i = 0; i < points.length; i++) {
                  faces.add(new BorderSurface.Face(centre, ring[i], ring[(i + 1) % points.length]));
               }
            } else {
               for (int i = 0; i < points.length; i++) {
                  int j = (i + 1) % points.length;
                  this.borderFace(faces, previous[i], ring[i], ring[j]);
                  this.borderFace(faces, previous[i], ring[j], previous[j]);
               }
            }

            previous = ring;
         }
      }

      private void borderFace(ArrayList<BorderSurface.Face> faces, BorderSurface.Vertex a, BorderSurface.Vertex b, BorderSurface.Vertex c) {
         if (!(Math.abs(a.y() - 0.034) < 1.0E-6) || !(Math.abs(b.y() - 0.034) < 1.0E-6) || !(Math.abs(c.y() - 0.034) < 1.0E-6)) {
            if (!(Math.abs((b.x() - a.x()) * (c.z() - a.z()) - (b.z() - a.z()) * (c.x() - a.x())) < 1.0E-9)) {
               faces.add(new BorderSurface.Face(a, b, c));
            }
         }
      }

      private void stitch(float x, float z, float dx, float dz, float halfWidth, float y) {
         float nx = -dz;
         float nz = dx;
         float h = 0.004F;
         float l = 0.021F;
         float ax = x - dx * l - nx * halfWidth;
         float az = z - dz * l - nz * halfWidth;
         float bx = x - dx * l + nx * halfWidth;
         float bz = z - dz * l + nz * halfWidth;
         float cx = x + dx * l + nx * halfWidth;
         float cz = z + dz * l + nz * halfWidth;
         float ex = x + dx * l - nx * halfWidth;
         float ez = z + dz * l - nz * halfWidth;
         MatMesh.face(this.thread, ax, y + h, az, bx, y + h, bz, cx, y + h, cz, ex, y + h, ez, 0.0F, 0.0F, 1.0F, 1.0F, -1, 0.0F, 1.0F, 0.0F);
         MatMesh.face(this.thread, ax, y, az, bx, y, bz, bx, y + h, bz, ax, y + h, az, 0.0F, 0.0F, 1.0F, 1.0F, -1, -dx, 0.0F, -dz);
         MatMesh.face(this.thread, cx, y, cz, ex, y, ez, ex, y + h, ez, cx, y + h, cz, 0.0F, 0.0F, 1.0F, 1.0F, -1, dx, 0.0F, dz);
         MatMesh.face(this.thread, bx, y, bz, cx, y, cz, cx, y + h, cz, bx, y + h, bz, 0.0F, 0.0F, 1.0F, 1.0F, -1, nx, 0.0F, nz);
         MatMesh.face(this.thread, ex, y, ez, ax, y, az, ax, y + h, az, ex, y + h, ez, 0.0F, 0.0F, 1.0F, 1.0F, -1, -nx, 0.0F, -nz);
      }

      private void etching(float x, float z, int shape) {
         float[][] points = switch (shape) {
            case 0 -> new float[][]{
               {-0.38F, 0.35F},
               {-0.4F, -0.08F},
               {-0.26F, -0.4F},
               {0.26F, -0.4F},
               {0.4F, -0.08F},
               {0.38F, 0.35F},
               {0.18F, 0.35F},
               {0.18F, 0.12F},
               {-0.18F, 0.12F},
               {-0.18F, 0.35F},
               {-0.38F, 0.35F}
            };
            case 1 -> new float[][]{
               {-0.2F, -0.42F},
               {-0.48F, -0.22F},
               {-0.38F, 0.03F},
               {-0.25F, -0.04F},
               {-0.25F, 0.44F},
               {0.25F, 0.44F},
               {0.25F, -0.04F},
               {0.38F, 0.03F},
               {0.48F, -0.22F},
               {0.2F, -0.42F},
               {0.15F, -0.25F},
               {-0.15F, -0.25F},
               {-0.2F, -0.42F}
            };
            case 2 -> new float[][]{
               {-0.34F, -0.43F}, {0.34F, -0.43F}, {0.32F, 0.45F}, {0.06F, 0.45F}, {0.0F, -0.03F}, {-0.06F, 0.45F}, {-0.32F, 0.45F}, {-0.34F, -0.43F}
            };
            case 3 -> new float[][]{{-0.34F, -0.4F}, {-0.06F, -0.4F}, {-0.06F, 0.4F}, {-0.43F, 0.4F}, {-0.43F, 0.2F}, {-0.34F, 0.15F}, {-0.34F, -0.4F}};
            default -> new float[][]{{0.0F, -0.48F}, {0.39F, -0.3F}, {0.31F, 0.2F}, {0.0F, 0.48F}, {-0.31F, 0.2F}, {-0.39F, -0.3F}, {0.0F, -0.48F}};
         };

         for (int i = 1; i < points.length; i++) {
            this.line(x + points[i - 1][0], z + points[i - 1][1], x + points[i][0], z + points[i][1]);
         }

         if (shape == 3) {
            for (int i = 1; i < points.length; i++) {
               this.line(x - points[i - 1][0], z + points[i - 1][1], x - points[i][0], z + points[i][1]);
            }
         }
      }

      private void line(float x0, float z0, float x1, float z1) {
         float dx = x1 - x0;
         float dz = z1 - z0;
         float inv = 0.013F / Math.max(0.001F, (float)Math.sqrt(dx * dx + dz * dz));
         float nx = -dz * inv;
         float nz = dx * inv;
         MatMesh.face(
            this.felt,
            x0 + nx,
            0.012F,
            z0 + nz,
            x0 - nx,
            0.012F,
            z0 - nz,
            x1 - nx,
            0.012F,
            z1 - nz,
            x1 + nx,
            0.012F,
            z1 + nz,
            0.0F,
            0.0F,
            1.0F,
            1.0F,
            -6582162,
            0.0F,
            1.0F,
            0.0F
         );
      }

      private void shadow(float x0, float y, float z0, float x1, float z1, float opacity) {
         MatMesh.top(y < 0.0F ? this.groundShadow : this.shadow, x0, y, z0, x1, z1, 0.0F, 0.0F, 1.0F, 1.0F, (int)(opacity * 255.0F) << 24 | 16777215);
      }

      @Environment(EnvType.CLIENT)
      private record BorderRun(float x0, float z0, float x1, float z1, boolean exterior) {
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Part {
      final RenderType type;
      final int shading;
      final ArrayList<MatMesh.Vertex> build = new ArrayList<>();
      MatMesh.Vertex[] vertices;
      MatMesh.Vertex[] bridges;
      int[] colors;
      int[] bridgeColors;
      long lightingKey = Long.MIN_VALUE;
      private MatWorldContext lightOwner;
      private MatWorldContext.LightSample[] lights;
      private MatWorldContext.LightSample[] bridgeLights;
      private final Matrix4f cachedPose = new Matrix4f();
      private float[] projected;

      Part(RenderType type) {
         this.type = type;
         this.shading = type == MatRenderTypes.material(MatMaterials.LED)
            ? 1
            : (
               type == MatRenderTypes.decal(MatMaterials.SHADOW) || type == MatRenderTypes.decal(MatMaterials.CONTACT)
                  ? 2
                  : (
                     type == MatRenderTypes.material(MatMaterials.LEATHER)
                        ? 4
                        : (
                           type != MatRenderTypes.material(MatMaterials.CLOTH)
                                 && type != MatRenderTypes.material(MatMaterials.CUTTING_EDGE)
                                 && type != MatRenderTypes.material(MatMaterials.METAL)
                                 && type != MatRenderTypes.material(MatMaterials.HOTBAR)
                                 && type != MatRenderTypes.material(MatMaterials.BORDER)
                              ? 0
                              : 3
                        )
                  )
            );
      }

      private static float mix(float a, float b, float t) {
         return a + (b - a) * t;
      }

      private static int colorMix(int a, int b, float t) {
         int c = 0;

         for (int shift = 0; shift <= 24; shift += 8) {
            c |= (Math.round(mix(a >>> shift & 0xFF, b >>> shift & 0xFF, t)) & 0xFF) << shift;
         }

         return c;
      }

      private static MatMesh.Vertex lerp(MatMesh.Vertex a, MatMesh.Vertex b, float t) {
         return new MatMesh.Vertex(
            mix(a.x, b.x, t),
            mix(a.y, b.y, t),
            mix(a.z, b.z, t),
            mix(a.u, b.u, t),
            mix(a.v, b.v, t),
            colorMix(a.color, b.color, t),
            mix(a.nx, b.nx, t),
            mix(a.ny, b.ny, t),
            mix(a.nz, b.nz, t)
         );
      }

      private static MatMesh.Vertex atCut(MatMesh.Vertex a, MatMesh.Vertex b, float cut) {
         MatMesh.Vertex v;
         if (a.x == cut) {
            v = a;
         } else if (b.x == cut) {
            v = b;
         } else {
            if (a.x > b.x) {
               MatMesh.Vertex swap = a;
               a = b;
               b = swap;
            }

            v = lerp(a, b, (cut - a.x) / (b.x - a.x));
         }

         return new MatMesh.Vertex(cut, v.y, v.z, v.u, v.v, v.color, v.nx, v.ny, v.nz);
      }

      private static MatMesh.Vertex sharedCut(MatMesh.Vertex a, MatMesh.Vertex b, float cut, List<MatMesh.Vertex> canonical) {
         MatMesh.Vertex point = atCut(a, b, cut);
         if (canonical.isEmpty()) {
            return point;
         }

         MatMesh.Vertex nearest = point;
         double distance = Double.POSITIVE_INFINITY;

         for (MatMesh.Vertex v : canonical) {
            double dy = (double)v.y - point.y;
            double dz = (double)v.z - point.z;
            double d = dy * dy + dz * dz;
            if (d < distance) {
               distance = d;
               nearest = v;
            }
         }

         return nearest;
      }

      private static List<MatMesh.Vertex> clip(List<MatMesh.Vertex> input, float cut, boolean keepRight, List<MatMesh.Vertex> canonical) {
         ArrayList<MatMesh.Vertex> out = new ArrayList<>();
         if (input.isEmpty()) {
            return out;
         }

         MatMesh.Vertex previous = input.getLast();
         boolean was = keepRight ? previous.x >= cut : previous.x <= cut;

         for (MatMesh.Vertex v : input) {
            boolean inside = keepRight ? v.x >= cut : v.x <= cut;
            if (inside != was) {
               out.add(sharedCut(previous, v, cut, canonical));
            }

            if (inside) {
               out.add(v.x == cut ? sharedCut(v, v, cut, canonical) : v);
            }

            previous = v;
            was = inside;
         }

         return out;
      }

      void finish(float left, float right) {
         float[] cuts = new float[]{MatFold.crease(left, right, 0), MatFold.crease(left, right, 1)};
         ArrayList<MatMesh.Vertex> mesh = new ArrayList<>();
         ArrayList<MatMesh.Vertex> joins = new ArrayList<>();

         for (int i = 0; i < this.build.size(); i += 4) {
            List<MatMesh.Vertex> quad = List.of(this.build.get(i), this.build.get(i + 1), this.build.get(i + 2), this.build.get(i + 3));
            float lo = Float.POSITIVE_INFINITY;
            float hi = -lo;

            for (MatMesh.Vertex v : quad) {
               lo = Math.min(lo, v.x);
               hi = Math.max(hi, v.x);
            }

            int whole = hi == lo
               ? (lo <= cuts[0] ? 0 : (lo <= cuts[1] ? 1 : 2))
               : (hi <= cuts[0] ? 0 : (lo >= cuts[1] ? 2 : (lo >= cuts[0] && hi <= cuts[1] ? 1 : -1)));
            if (whole >= 0) {
               for (MatMesh.Vertex v : quad) {
                  mesh.add(v.panel(whole));
               }
            } else {
               ArrayList<List<MatMesh.Vertex>> intersections = new ArrayList<>(cuts.length);

               for (float cut : cuts) {
                  ArrayList<MatMesh.Vertex> edge = new ArrayList<>(2);
                  if (lo < cut && hi > cut) {
                     for (int j = 0; j < 4; j++) {
                        MatMesh.Vertex a = quad.get(j);
                        MatMesh.Vertex b = quad.get((j + 1) % 4);
                        if (a.x < cut != b.x < cut) {
                           edge.add(atCut(a, b, cut));
                        }
                     }
                  }

                  intersections.add(edge);
               }

               for (int panel = 0; panel < 3; panel++) {
                  if ((!(hi - lo < 1.0E-6F) || panel == (lo <= cuts[0] ? 0 : (lo <= cuts[1] ? 1 : 2)))
                     && (panel <= 0 || !(hi < cuts[panel - 1]))
                     && (panel >= cuts.length || !(lo > cuts[panel]))) {
                     List<MatMesh.Vertex> polygon = quad;
                     if (panel > 0) {
                        polygon = clip(polygon, cuts[panel - 1], true, intersections.get(panel - 1));
                     }

                     if (panel < cuts.length) {
                        polygon = clip(polygon, cuts[panel], false, intersections.get(panel));
                     }

                     if (polygon.size() >= 3) {
                        if (polygon.size() == 4) {
                           for (MatMesh.Vertex v : polygon) {
                              mesh.add(v.panel(panel));
                           }
                        } else {
                           for (int j = 1; j < polygon.size() - 1; j++) {
                              mesh.add(polygon.getFirst().panel(panel));
                              mesh.add(polygon.get(j).panel(panel));
                              mesh.add(polygon.get(j + 1).panel(panel));
                              mesh.add(polygon.get(j + 1).panel(panel));
                           }
                        }
                     }
                  }
               }

               for (int cut = 0; cut < cuts.length; cut++) {
                  if (lo < cuts[cut] && hi > cuts[cut]) {
                     List<MatMesh.Vertex> edge = intersections.get(cut);
                     if (edge.size() == 2) {
                        joins.add(edge.get(0).panel(cut));
                        joins.add(edge.get(1).panel(cut));
                        joins.add(edge.get(1).panel(cut + 1));
                        joins.add(edge.get(0).panel(cut + 1));
                     }
                  }
               }
            }
         }

         this.vertices = mesh.toArray(MatMesh.Vertex[]::new);
         this.bridges = joins.toArray(MatMesh.Vertex[]::new);
         this.colors = new int[this.vertices.length];
         this.bridgeColors = new int[this.bridges.length];
         this.build.clear();
      }

      private int shade(MatMesh.Vertex v, MatWorldContext world, MatWorldContext.LightSample sample) {
         if (this.shading == 1) {
            return world.ledColor(v.x, v.z);
         } else if (this.shading == 2) {
            return v.color;
         } else {
            float spill = sample.spill();
            if (this.shading == 4 && MatInventoryClient.config.matColor != 0) {
               return world.rubberColor(MatPalette.tint(v.color, MatInventoryClient.config.matColor), v.x, v.z, v.nx, v.ny, v.nz, spill);
            } else {
               return this.shading != 3 && this.shading != 4
                  ? world.color(v.color, v.x, v.z, v.nx, v.ny, v.nz, true, spill)
                  : world.rubberColor(v.color, v.x, v.z, v.nx, v.ny, v.nz, spill);
            }
         }
      }

      void draw(PoseStack pose, SceneBuffers buffers, MatWorldContext world, MatFold fold) {
         MatConfig config = MatInventoryClient.config;
         VertexConsumer out = buffers.getBuffer(this.shading == 4 && config.matColor != 0 ? MatRenderTypes.dyedCanvas() : this.type);
         if (out != DiscardVertices.INSTANCE) {
            long key = Float.floatToIntBits(world.brightness);
            key = key * 31L + Float.floatToIntBits(world.red);
            key = key * 31L + Float.floatToIntBits(world.green);
            key = key * 31L + Float.floatToIntBits(world.blue);
            key = key * 31L + Float.floatToIntBits(config.exposure);
            key = key * 31L + Float.floatToIntBits(config.saturation);
            key = key * 31L + config.ledColor;
            key = key * 31L + config.matColor;
            key = key * 31L + (ShaderSupport.drawing() ? 1 : 0);
            key = key * 31L + world.leds.revision();
            if (this.lightOwner != world) {
               this.lightOwner = world;
               this.lightingKey = Long.MIN_VALUE;
               this.lights = new MatWorldContext.LightSample[this.vertices.length];
               this.bridgeLights = new MatWorldContext.LightSample[this.bridges.length];
               if (this.shading != 1 && this.shading != 2) {
                  for (int i = 0; i < this.vertices.length; i++) {
                     this.lights[i] = world.lightSample(this.vertices[i].x, this.vertices[i].z);
                  }

                  for (int i = 0; i < this.bridges.length; i++) {
                     this.bridgeLights[i] = world.lightSample(this.bridges[i].x, this.bridges[i].z);
                  }
               }
            }

            if (key != this.lightingKey) {
               this.lightingKey = key;

               for (int i = 0; i < this.vertices.length; i++) {
                  this.colors[i] = this.shade(this.vertices[i], world, this.lights[i]);
               }

               for (int i = 0; i < this.bridges.length; i++) {
                  this.bridgeColors[i] = this.shade(this.bridges[i], world, this.bridgeLights[i]);
               }
            }

            boolean active = fold != null && fold.active();
            if (!active) {
               if (this.projected == null || !this.cachedPose.equals(pose.last().pose())) {
                  if (this.projected == null) {
                     this.projected = new float[this.vertices.length * 6];
                  }

                  this.cachedPose.set(pose.last().pose());

                  for (int i = 0; i < this.vertices.length; i++) {
                     MatMesh.Vertex v = this.vertices[i];
                     int at = i * 6;
                     pose.last().pose().transformPosition(v.x, v.y, v.z, MatMesh.vertexPoint);
                     pose.last().transformNormal(v.nx, v.ny, v.nz, MatMesh.vertexNormal);
                     this.projected[at] = MatMesh.vertexPoint.x;
                     this.projected[at + 1] = MatMesh.vertexPoint.y;
                     this.projected[at + 2] = MatMesh.vertexPoint.z;
                     this.projected[at + 3] = MatMesh.vertexNormal.x;
                     this.projected[at + 4] = MatMesh.vertexNormal.y;
                     this.projected[at + 5] = MatMesh.vertexNormal.z;
                  }
               }

               for (int i = 0; i < this.vertices.length; i++) {
                  MatMesh.Vertex v = this.vertices[i];
                  int at = i * 6;
                  out.addVertex(
                     this.projected[at],
                     this.projected[at + 1],
                     this.projected[at + 2],
                     this.colors[i],
                     v.u,
                     v.v,
                     OverlayTexture.NO_OVERLAY,
                     15728880,
                     this.projected[at + 3],
                     this.projected[at + 4],
                     this.projected[at + 5]
                  );
               }
            } else {
               for (int i = 0; i < this.vertices.length; i++) {
                  MatMesh.Vertex v = this.vertices[i];
                  float x = v.x;
                  float y = v.y;
                  float z = v.z;
                  float nx = v.nx;
                  float ny = v.ny;
                  float nz = v.nz;
                  if (active) {
                     fold.mapPanel(v.panel, x, y * fold.relief(), z, nx, ny, nz);
                     x = fold.x;
                     y = fold.y;
                     z = fold.z;
                     nx = fold.nx;
                     ny = fold.ny;
                     nz = fold.nz;
                  }

                  MatMesh.emit(pose.last(), out, x, y, z, this.colors[i], v.u, v.v, nx, ny, nz);
               }

               if (active) {
                  for (int i = 0; i < this.bridges.length; i += 4) {
                     MatMesh.Vertex va = this.bridges[i];
                     MatMesh.Vertex vb = this.bridges[i + 1];
                     MatMesh.Vertex vc = this.bridges[i + 2];
                     fold.mapPanel(va.panel, va.x, va.y * fold.relief(), va.z, 0.0F, 1.0F, 0.0F);
                     float ax = fold.x;
                     float ay = fold.y;
                     float az = fold.z;
                     fold.mapPanel(vb.panel, vb.x, vb.y * fold.relief(), vb.z, 0.0F, 1.0F, 0.0F);
                     float bx = fold.x - ax;
                     float by = fold.y - ay;
                     float bz = fold.z - az;
                     fold.mapPanel(vc.panel, vc.x, vc.y * fold.relief(), vc.z, 0.0F, 1.0F, 0.0F);
                     float cx = fold.x - ax;
                     float cy = fold.y - ay;
                     float cz = fold.z - az;
                     float nx = by * cz - bz * cy;
                     float ny = bz * cx - bx * cz;
                     float nz = bx * cy - by * cx;
                     float n = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
                     if (!(n < 1.0E-6F)) {
                        nx /= n;
                        ny /= n;
                        nz /= n;

                        for (int j = 0; j < 4; j++) {
                           MatMesh.Vertex v = this.bridges[i + j];
                           fold.mapPanel(v.panel, v.x, v.y * fold.relief(), v.z, 0.0F, 1.0F, 0.0F);
                           MatMesh.emit(pose.last(), out, fold.x, fold.y, fold.z, this.bridgeColors[i + j], v.u, v.v, nx, ny, nz);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @Environment(EnvType.CLIENT)
   private record Vertex(float x, float y, float z, float u, float v, int color, float nx, float ny, float nz, int panel) {
      Vertex(float x, float y, float z, float u, float v, int color, float nx, float ny, float nz) {
         this(x, y, z, u, v, color, nx, ny, nz, -1);
      }

      MatMesh.Vertex panel(int p) {
         return new MatMesh.Vertex(this.x, this.y, this.z, this.u, this.v, this.color, this.nx, this.ny, this.nz, p);
      }
   }
}
