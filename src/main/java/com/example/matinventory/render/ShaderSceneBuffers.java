package com.example.matinventory.render;

import com.example.matinventory.MatInventoryClient;
import com.example.matinventory.scene.MatWorldContext;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.LightLayer;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
final class ShaderSceneBuffers implements SceneBuffers {
   SceneBuffers source;
   private final Map<RenderType, ShaderSceneBuffers.Consumer> consumers = new WeakHashMap<>();

   static int light(float x, float z) {
      Minecraft c = Minecraft.getInstance();
      if (!MatInventoryClient.config.worldLighting) {
         return 15728880;
      }

      BlockPos pos = c.player.blockPosition();
      int minimum = Math.round(15.0F * (float)Math.sqrt(MatInventoryClient.config.minBrightness));
      int block = Math.max(minimum, c.level.getBrightness(LightLayer.BLOCK, pos));
      int sky = c.level.getBrightness(LightLayer.SKY, pos);
      MatWorldContext world = NativeWorldScene.world();
      if (world != null) {
         block = Math.max(block, Math.min(15, Math.round(block + (15 - block) * world.ledSpill(x, z))));
      }

      return LightCoordsUtil.pack(block, sky);
   }

   private static MatWorldContext world() {
      return NativeWorldScene.world();
   }

   @Override
   public VertexConsumer getBuffer(RenderType type) {
      Identifier texture = MatRenderTypes.texture(type);
      RenderType routed = type;
      boolean emitter = MatMaterials.LED.equals(texture) && world() != null && world().leds.enabled();
      if (!ShaderSupport.shadowPass() || !MatRenderTypes.soft(type) && !emitter) {
         if (texture != null) {
            boolean soft = MatRenderTypes.soft(type);
            routed = emitter ? RenderTypes.eyes(texture) : (soft ? RenderTypes.entityTranslucent(texture) : RenderTypes.entityCutout(texture));
         }

         ShaderSceneBuffers.Consumer out = this.consumers.computeIfAbsent(type, t -> new ShaderSceneBuffers.Consumer());
         out.target = this.source.getBuffer(routed);
         return out;
      } else {
         return DiscardVertices.INSTANCE;
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Consumer implements VertexConsumer {
      VertexConsumer target;
      int light;
      final Vector3f point = new Vector3f();

      @Override
      public VertexConsumer addVertex(float x, float y, float z) {
         NativeWorldScene.toMat(this.point.set(x, y, z));
         this.light = ShaderSceneBuffers.light(this.point.x, this.point.z);
         this.target.addVertex(x, y, z).setUv1(0, 10).setUv2(this.light & 65535, this.light >>> 16).setNormal(0.0F, 1.0F, 0.0F);
         return this;
      }

      @Override
      public VertexConsumer setColor(int r, int g, int b, int a) {
         this.target.setColor(r, g, b, a);
         return this;
      }

      @Override
      public VertexConsumer setColor(int color) {
         this.target.setColor(color);
         return this;
      }

      @Override
      public VertexConsumer setUv(float u, float v) {
         this.target.setUv(u, v);
         return this;
      }

      @Override
      public VertexConsumer setUv1(int u, int v) {
         this.target.setUv1(u, v);
         return this;
      }

      @Override
      public VertexConsumer setUv2(int u, int v) {
         this.target.setUv2(this.light & 65535, this.light >>> 16);
         return this;
      }

      @Override
      public VertexConsumer setNormal(float x, float y, float z) {
         this.target.setNormal(x, y, z);
         return this;
      }

      @Override
      public VertexConsumer setUv3(float u, float v) {
         this.target.setUv3(u, v);
         return this;
      }

      @Override
      public VertexConsumer setLineWidth(float w) {
         this.target.setLineWidth(w);
         return this;
      }
   }
}
