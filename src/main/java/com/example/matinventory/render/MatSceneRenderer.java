package com.example.matinventory.render;

import com.example.matinventory.MatInventoryClient;
import com.example.matinventory.animation.MatFold;
import com.example.matinventory.mixin.PipTargetAccessor;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.platform.Lighting.Entry;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry.Context;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;

@Environment(EnvType.CLIENT)
public final class MatSceneRenderer extends PictureInPictureRenderer<MatSceneState> {
   private final MatProjectionBuffer projection = new MatProjectionBuffer("Mat Inventory");
   private final WhiteSilhouetteCollector silhouette;
   private final MatLighting lighting = new MatLighting();
   private final GhostCollector ghosts;
   private final LitItemCollector lit;
   private final ItemShadowCollector shadows;
   private final ShaderSceneBuffers shaderBuffers = new ShaderSceneBuffers();
   private final SceneTarget target = new SceneTarget();
   private final ImmediateBuffers bufferSource;
   private final SubmitNodeStorage features = new SubmitNodeStorage();
   private final MatPostEffects effects;
   private final NativeRecipeBook recipeBook = new NativeRecipeBook();
   private final OpeningPlayerRenderer openingPlayer = new OpeningPlayerRenderer();
   private final Quaternionf rotation = new Quaternionf();
   public static long renderedFrames;
   public static long hiddenWorldFrames;
   public static long cachedFrames;
   private MatSceneState cachedState;
   private long cachedKey;
   private long lastDraw;
   public static double cpuMillis;

   public static void draw(GuiGraphicsExtractor g, MatSceneState state) {
      state.captureGui(g);
      NativeWorldScene.request(state);
      g.guiRenderState.addPicturesInPictureState(state);
   }

   public MatSceneRenderer(Context context) {
      super();
      this.bufferSource = new ImmediateBuffers(this.target);
      this.effects = new MatPostEffects(this.target);
      NativeWorldScene.install(this::drawInLevel);
      SceneBuffers routed = type -> ShaderSupport.drawing() ? this.shaderBuffers.getBuffer(type) : this.bufferSource.getBuffer(type);
      this.silhouette = new WhiteSilhouetteCollector(routed);
      this.ghosts = new GhostCollector(routed);
      this.lit = new LitItemCollector(routed);
      this.shadows = new ItemShadowCollector(routed);
   }

   @Override
   public Class<MatSceneState> getRenderStateClass() {
      return MatSceneState.class;
   }

   @Override
   protected String getTextureLabel() {
      return "matinventory scene";
   }

   protected boolean textureIsReadyToBlit(MatSceneState state) {
      boolean ready = !state.actualWorld()
         && !ShaderSupport.active()
         && !state.fold.active()
         && state == this.cachedState
         && this.cachedKey == state.visualKey()
         && System.nanoTime() - this.lastDraw < (state.foil ? 16000000L : 50000000L);
      if (ready) {
         cachedFrames++;
      }

      return ready;
   }

   public void prepare(MatSceneState state, GuiRenderState gui, FeatureRenderDispatcher dispatcher, int scale) {
      RenderSystem.backupProjectionMatrix();
      long started = System.nanoTime();

      try {
         super.prepare(state, gui, dispatcher, scale * MatInventoryClient.config.supersampling);
      } catch (RuntimeException ex) {
         MatInventoryClient.LOG.error("3D inventory rendering failed; returning to vanilla", ex);
         MatInventoryClient.renderFailed = true;
         this.features.drainPhases(phase -> phase.sortInto((node, ordered) -> {}));
      } finally {
         this.bufferSource.endFrame();
         RenderSystem.restoreProjectionMatrix();
         Minecraft.getInstance().gameRenderer.lighting().setupFor(Entry.ITEMS_3D);
         renderedFrames++;
         cpuMillis = cpuMillis + (System.nanoTime() - started) / 1000000.0;
      }
   }

   protected void blitTexture(MatSceneState state, GuiRenderState gui) {
      GpuTextureView texture = ((PipTargetAccessor)(Object)this).matinventory$colorTarget();
      int colored = state.world.coloredLight();
      gui.addBlitToCurrentLayer(
         new BlitRenderState(
            MatRenderTypes.colored(state.world.naturalTint(), state.embedded),
            TextureSetup.singleTexture(texture, RenderSystem.getSamplerCache().getClampToEdge(state.embedded ? FilterMode.LINEAR : FilterMode.NEAREST)),
            state.pose(),
            0,
            0,
            state.x1(),
            state.y1(),
            0.0F,
            1.0F,
            1.0F,
            0.0F,
            colored == 0 ? 16777215 : colored,
            state.scissorArea(),
            null
         )
      );
   }

   protected void renderToTexture(MatSceneState state, PoseStack pose, SubmitNodeCollector collector) {
      PipTargetAccessor access = (PipTargetAccessor)(Object)this;
      this.target.bind(access.matinventory$colorTarget(), access.matinventory$depthTarget());
      this.lighting.apply(state.view());
      boolean litByLevel = NativeWorldScene.litByLevel(state);
      if (litByLevel || state.actualWorld()) {
         this.effects.copy(Minecraft.getInstance().gameRenderer.mainRenderTarget().getColorTextureView());
      }

      if (!litByLevel) {
         RenderSystem.setProjectionMatrix(this.projection.getBuffer(state.projection()), ProjectionType.PERSPECTIVE);
         Matrix4fStack modelView = RenderSystem.getModelViewStack();
         modelView.pushMatrix();
         modelView.identity();

         try {
            pose.setIdentity();
            pose.mulPose(state.view());
            this.drawScene(state, pose, this.features);
         } finally {
            modelView.popMatrix();
         }
      }

      RenderSystem.setProjectionMatrix(this.projection.getBuffer(state.projection()), ProjectionType.PERSPECTIVE);
      Matrix4fStack haloView = RenderSystem.getModelViewStack();
      haloView.pushMatrix();
      haloView.identity();

      try {
         pose.setIdentity();
         pose.mulPose(state.view());
         if (litByLevel) {
            this.lighting.apply(state.view());
            this.openingPlayer.draw(state, this.target);
            RenderSystem.setProjectionMatrix(this.projection.getBuffer(state.projection()), ProjectionType.PERSPECTIVE);
         }

         this.effects.halo(state, () -> this.drawHoverMask(state, pose));
      } finally {
         haloView.popMatrix();
      }

      this.cachedState = state;
      this.cachedKey = state.visualKey();
      this.lastDraw = System.nanoTime();
   }

   private void drawInLevel(MatSceneState state, PoseStack pose, SubmitNodeStorage collector) {
      WorldBuffers world = new WorldBuffers();
      this.shaderBuffers.source = world;
      this.drawScene(state, pose, collector);
      world.submit(collector);
   }

   private void renderFeatures() {
      if (ShaderSupport.drawing()) {
         return;
      }

      try (FeatureRenderDispatcher.PreparedFrame frame = Minecraft.getInstance().gameRenderer.featureRenderDispatcher().prepareFrame(this.features)) {
         if (!frame.isEmpty()) {
            try (RenderPass pass = this.target.open()) {
               FeatureRenderDispatcher.renderAllFeatures(pass, frame);
            }
         }
      }
   }

   private void drawScene(MatSceneState state, PoseStack pose, SubmitNodeStorage specials) {
      SceneBuffers sceneBuffers = ShaderSupport.drawing() ? this.shaderBuffers : this.bufferSource;
      MatMesh.draw(pose, sceneBuffers, state);
      this.recipeBook.draw(pose, sceneBuffers, state);
      this.bufferSource.endBatch();
      if (!ShaderSupport.drawing() && !state.fold.active() && state.itemCount > 0) {
         this.effects.shadows(() -> this.drawContactShadows(state, pose));
      }

      this.lit.specials = specials;
      this.ghosts.specials = specials;

      for (int i = 0; i < state.itemCount; i++) {
         MatSceneState.Item item = state.items()[i];
         if (!item.ghost) {
            for (int copy = 0; copy < item.copies(); copy++) {
               this.transform(pose, state, item, copy, 1.0F);
               this.lit.rendered = false;
               this.lit.tint(state.world.color(-1, item.x, item.z, 0.0F, 1.0F, 0.0F, false));
               this.lit.shade(item.stackShade);
               item.model().submit(pose, this.lit, ShaderSupport.drawing() ? ShaderSceneBuffers.light(item.x, item.z) : 15728880, OverlayTexture.NO_OVERLAY, 0);
               if (!this.lit.rendered) {
                  item.model()
                     .submit(
                        pose,
                        specials,
                        ShaderSupport.drawing() ? ShaderSceneBuffers.light(item.x, item.z) : 15728880,
                        OverlayTexture.NO_OVERLAY,
                        0
                     );
               }

               pose.popPose();
            }
         }
      }

      this.lit.clear();
      this.renderFeatures();
      this.bufferSource.endBatch();

      for (int i = 0; i < state.itemCount; i++) {
         MatSceneState.Item item = state.items()[i];
         if (item.ghost) {
            this.transform(pose, state, item, 0, 1.0F);
            this.ghosts.rendered = false;
            item.model.submit(pose, this.ghosts, 15728880, OverlayTexture.NO_OVERLAY, 0);
            if (!this.ghosts.rendered) {
               item.model.submit(pose, specials, 15728880, OverlayTexture.NO_OVERLAY, 0);
            }

            pose.popPose();
         }
      }

      this.ghosts.clear();
      this.renderFeatures();
      this.bufferSource.endBatch();
      if (!ShaderSupport.drawing()) {
         this.openingPlayer.draw(state, this.target);
      }

      this.cachedState = state;
      this.cachedKey = state.visualKey();
      this.lastDraw = System.nanoTime();
   }

   private void drawContactShadows(MatSceneState state, PoseStack pose) {
      state.view().transformDirection(this.shadows.up.set(0.0F, 1.0F, 0.0F));

      for (int i = 0; !state.fold.active() && i < state.itemCount; i++) {
         MatSceneState.Item item = state.items()[i];
         if (!(item.scale < 1.0E-5F)) {
            if (item.torch) {
               for (int copy = 0; copy < Math.min(3, item.copies); copy++) {
                  pose.pushPose();
                  pose.translate(item.x + item.copyX(copy), item.supportY + 0.001F, item.z + item.copyZ(copy));
                  pose.rotate(this.rotation.rotationY((float)Math.toRadians(item.yaw + item.copyYaw(copy))));
                  MatMesh.decal(pose, this.bufferSource, MatMaterials.SHADOW, 0.0F, 0.0F, 0.0F, item.pileWidth * 0.55F, item.pileDepth * 0.55F, 1358954495);
                  pose.popPose();
               }
            } else {
               for (int copy = 0; copy < Math.min(item.decorativePile ? 4 : (item.materialPile && item.materialStyle == 2 ? 3 : 2), item.copies); copy++) {
                  for (int layer = 0; layer < 2; layer++) {
                     pose.pushPose();
                     float support = item.resting
                        ? MatMesh.equipmentSupport(state.layout(), item.x, item.z, item.supportX, item.supportZ)
                           + item.contactOffset
                           - (item.contactOffset < 0.0F ? 0.001F : 0.003F)
                        : (
                           state.layout().onCloth(item.x, item.z)
                              ? MatMesh.itemSupport(state.layout(), item.x, item.z, item.shadowX, item.shadowZ) - 0.018F
                              : 0.059F
                        );
                     pose.translate(
                        item.x + item.copyX(copy) + (item.resting ? 0.0F : 0.035F), support, item.z + item.copyZ(copy) + (item.resting ? 0.0F : 0.04F)
                     );
                     float expand = 1.0F + layer * 0.04F;
                     this.shadows.project(state.view(), support + layer * 0.001F);
                     pose.scale(expand, 1.0F, expand);
                     pose.rotate(this.rotation.rotationY((float)Math.toRadians(item.yaw + item.copyYaw(copy))));
                     pose.rotate(this.rotation.rotationX((float)Math.toRadians(item.pitch)));
                     pose.rotate(this.rotation.rotationZ((float)Math.toRadians(item.roll)));
                     float copyScale = item.scale * item.copyScale(copy);
                     pose.scale(copyScale, copyScale, copyScale);
                     pose.translate(-item.centerX, -item.centerY, -item.centerZ);
                     this.shadows.opacity = layer == 0 ? 0.52F : 0.15F;
                     this.shadows.rendered = false;
                     this.shadows.equipment = item.flatHover || item.nativeGeometry;
                     this.shadows.texture = item.nativeTexture;
                     item.model.submit(pose, this.shadows, 15728880, OverlayTexture.NO_OVERLAY, 0);
                     pose.popPose();
                     if (layer == 0 && !this.shadows.rendered) {
                        MatMesh.decal(
                           pose, this.bufferSource, MatMaterials.SHADOW, item.x + 0.04F, support, item.z + 0.04F, item.shadowX, item.shadowZ, 1895825407
                        );
                     }
                  }
               }
            }
         }
      }

      this.shadows.clear();
      this.bufferSource.endBatch();
   }

   private void drawHoverMask(MatSceneState state, PoseStack pose) {
      for (int i = 0; i < state.itemCount; i++) {
         MatSceneState.Item item = state.items()[i];
         if (!(item.hover <= 0.015F) && !item.ghost && !(item.scale < 1.0E-5F)) {
            for (int copy = 0; copy < item.copies; copy++) {
               this.transform(pose, state, item, copy, 1.0F);
               this.silhouette.opacity = item.hover;
               this.silhouette.texture = item.nativeTexture;
               item.model.submit(pose, this.silhouette, 15728880, OverlayTexture.NO_OVERLAY, 0);
               pose.popPose();
            }
         }
      }

      this.silhouette.clear();
      this.bufferSource.endBatch();
   }

   private void transform(PoseStack pose, MatSceneState state, MatSceneState.Item item, int copy, float expansion) {
      pose.pushPose();
      MatFold fold = state.fold;
      fold.map(item.x() + item.copyX(copy), item.y() + item.copyY(copy) - item.supportY * (1.0F - fold.relief()), item.z() + item.copyZ(copy), 0.0F, 1.0F, 0.0F);
      pose.translate(fold.x, fold.y, fold.z);
      if (fold.active()) {
         pose.rotate(this.rotation.rotationZ(fold.bend));
      }

      pose.rotate(this.rotation.rotationY((float)Math.toRadians(item.yaw() + item.copyYaw(copy))));
      pose.rotate(this.rotation.rotationX((float)Math.toRadians(item.pitch + item.hoverTilt())));
      pose.rotate(this.rotation.rotationZ((float)Math.toRadians(item.roll)));
      float copyScale = item.scale * item.copyScale(copy) * expansion;
      pose.scale(copyScale, copyScale, copyScale);
      pose.translate(-item.centerX, -item.centerY, -item.centerZ);
   }

   @Override
   public void close() {
      super.close();
      this.projection.close();
      this.effects.close();
      this.lighting.close();
      this.openingPlayer.close();
      this.bufferSource.close();
   }
}
