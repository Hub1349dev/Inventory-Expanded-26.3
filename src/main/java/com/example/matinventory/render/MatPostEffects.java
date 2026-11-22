package com.example.matinventory.render;

import com.example.matinventory.layout.MatLayout;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline.Builder;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.lwjgl.system.MemoryStack;

@Environment(EnvType.CLIENT)
final class MatPostEffects implements AutoCloseable {
   private static final BindGroupLayout EFFECT = BindGroupLayout.builder().withUniform("Effect", UniformType.UNIFORM_BUFFER).build();
   private static final RenderPipeline COPY = pipeline("world_copy", false);
   private static final RenderPipeline BLUR = pipeline("contact_blur", false);
   private static final RenderPipeline COMPOSITE = pipeline("contact_blur", true);
   private static final RenderPipeline HALO = pipeline("hover_halo", true);
   private final SceneTarget target;
   private TextureTarget mask;
   private static final Vector4fc CLEAR = new Vector4f(0.0F);
   private TextureTarget soft;
   private final GpuBuffer params = RenderSystem.getDevice().createBuffer(() -> "Inventory effects", 136, 16L);
   static long haloFrames;

   private static Identifier id(String s) {
      return Identifier.fromNamespaceAndPath("matinventory", s);
   }

   private static RenderPipeline pipeline(String fragment, boolean blend) {
      Builder b = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
         .withLocation(id("pipeline/" + fragment))
         .withVertexShader("core/screenquad")
         .withFragmentShader(id("core/" + fragment))
         .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
         .withColorTargetState(blend ? new ColorTargetState(BlendFunction.ENTITY_OUTLINE_BLIT) : ColorTargetState.DEFAULT)
         .withCull(false);
      if (!fragment.equals("world_copy")) {
         b.withBindGroupLayout(EFFECT);
      }

      return b.build();
   }

   MatPostEffects(SceneTarget target) {
      this.target = target;
   }

   void copy(GpuTextureView source) {
      try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Inventory world", this.target.color, Optional.empty())) {
         pass.setPipeline(RenderSystem.getCompiledPipeline(COPY));
         RenderSystem.bindDefaultUniforms(pass);
         pass.setUniform("Sampler0", source, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
         pass.draw(3, 1, 0, 0);
      }
   }

   private GpuTextureView color() {
      return this.target.color;
   }

   private GpuTextureView depth() {
      return this.target.depth;
   }

   private void size(GpuTextureView target) {
      int w = target.getWidth(0);
      int h = target.getHeight(0);
      if (this.mask == null || this.mask.width != w || this.mask.height != h) {
         this.release();
         this.mask = new TextureTarget("Inventory effect mask", w, h, GpuFormat.RGBA8_UNORM, GpuFormat.D32_FLOAT);
         this.soft = new TextureTarget("Inventory soft mask", w, h, GpuFormat.RGBA8_UNORM, null);
      }
   }

   private void mask(GpuTextureView destination, Runnable draw, boolean copyDepth) {
      this.size(destination);
      GpuTextureView oldColor = this.target.color;
      GpuTextureView oldDepth = this.target.depth;
      CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
      encoder.clearColorAndDepthTextures(this.mask.getColorTexture(), CLEAR, this.mask.getDepthTexture(), 0.0);
      GpuTextureView sceneDepth = this.depth();

      try {
         this.target.bind(this.mask.getColorTextureView(), copyDepth ? sceneDepth : this.mask.getDepthTextureView());
         draw.run();
      } finally {
         this.target.bind(oldColor, oldDepth);
      }
   }

   void shadows(Runnable draw) {
      GpuTextureView destination = this.color();
      this.mask(destination, draw, true);
      float radius = Math.max(1.0F, destination.getHeight(0) / 960.0F) * 2.2F;
      this.pass(BLUR, this.mask.getColorTextureView(), this.soft.getColorTextureView(), radius, 0.0F, 1.0F, 0.0F, null);
      this.pass(COMPOSITE, this.soft.getColorTextureView(), destination, 0.0F, radius, 0.85F, 0.0F, null);
   }

   void halo(MatSceneState state, Runnable draw) {
      float left = Float.POSITIVE_INFINITY;
      float top = left;
      float right = Float.NEGATIVE_INFINITY;
      float bottom = right;

      for (int i = 0; i < state.itemCount; i++) {
         MatSceneState.Item item = state.items()[i];
         if (!(item.hover <= 0.015F) && !item.ghost) {
            MatLayout.Place p = state.layout().place(item.slot);
            if (p != null) {
               left = Math.min(left, p.screenLeft);
               top = Math.min(top, p.screenTop);
               right = Math.max(right, p.screenRight);
               bottom = Math.max(bottom, p.screenBottom);
            }
         }
      }

      if (Float.isFinite(left)) {
         GpuTextureView destination = this.color();
         this.mask(destination, draw, false);
         float radius = Math.max(1.0F, destination.getHeight(0) / 960.0F);
         float sx = (float)destination.getWidth(0) / state.x1();
         float sy = (float)destination.getHeight(0) / state.y1();
         float pad = radius * 6.0F;
         int x0 = Math.max(0, (int)(left * sx - pad));
         int x1 = Math.min(destination.getWidth(0), (int)Math.ceil(right * sx + pad));
         int y0 = Math.max(0, (int)((state.y1() - bottom) * sy - pad));
         int y1 = Math.min(destination.getHeight(0), (int)Math.ceil((state.y1() - top) * sy + pad));
         if (x1 <= x0 || y1 <= y0) {
            return;
         }

         this.pass(HALO, this.mask.getColorTextureView(), destination, radius, 0.0F, 1.0F, 0.0F, new int[]{x0, y0, Math.max(0, x1 - x0), Math.max(0, y1 - y0)});
         haloFrames++;
      }
   }

   private void pass(RenderPipeline pipeline, GpuTextureView source, GpuTextureView dest, float x, float y, float strength, float unused, int[] clip) {
      CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();

      try (MemoryStack stack = MemoryStack.stackPush()) {
         encoder.writeToBuffer(this.params.slice(), Std140Builder.onStack(stack, 16).putVec4(x, y, strength, unused).get());
      }

      try (RenderPass pass = encoder.createRenderPass(() -> "Inventory soft effect", dest, Optional.empty())) {
         pass.setPipeline(RenderSystem.getCompiledPipeline(pipeline));
         RenderSystem.bindDefaultUniforms(pass);
         pass.setUniform("Effect", this.params);
         pass.setUniform("Sampler0", source, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
         if (clip != null) {
            pass.enableScissor(clip[0], clip[1], clip[2], clip[3]);
         }

         pass.draw(3, 1, 0, 0);
      }
   }

   private void release() {
      if (this.mask != null) {
         this.mask.destroyBuffers();
         this.soft.destroyBuffers();
         this.mask = this.soft = null;
      }
   }

   @Override
   public void close() {
      this.release();
      this.params.close();
   }
}
