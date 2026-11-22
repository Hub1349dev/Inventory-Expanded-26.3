package com.example.matinventory.render;

import com.example.matinventory.MatInventoryClient;
import com.example.matinventory.animation.OpeningMotion;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.commands.RenderPass;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
final class OpeningPlayerRenderer implements AutoCloseable {
   private final SubmitNodeStorage collector = new SubmitNodeStorage();
   private final MatProjectionBuffer projection = new MatProjectionBuffer("Mat opening player");
   private final Matrix4f matrix = new Matrix4f();
   private final CameraRenderState camera = new CameraRenderState();
   private boolean bodyFailed;
   private boolean handFailed;
   private boolean bodySubmitted;
   private boolean handWarmed;
   private long animationId = Long.MIN_VALUE;
   private Object measuredArm;
   private int measuredGeneration = -1;
   private float armShoulderX;
   private float armShoulderY;
   private float armShoulderZ;
   private float armLength;
   private final float[] armBounds = new float[6];
   private final Vector3f corner = new Vector3f();
   private final Vector3f armAxis = new Vector3f();
   private final Vector3f aim = new Vector3f();
   private final Quaternionf armRotation = new Quaternionf();
   static long bodyFrames;
   static long handFrames;

   void draw(MatSceneState scene, SceneTarget target) {
      float time = scene.openingTime;
      Minecraft client = Minecraft.getInstance();
      LocalPlayer player = client.player;
      if (!(time >= 1.02F) && player != null && !player.isInvisible()) {
         boolean warming = time < 0.0F;
         if (scene.animationId != this.animationId) {
            this.bodySubmitted = this.handWarmed = this.bodyFailed = this.handFailed = false;
            this.measuredArm = null;
            this.animationId = scene.animationId;
         }

         this.matrix.identity().perspective((float)Math.toRadians(65.0), (float)scene.x1() / Math.max(1, scene.y1()), 0.05F, 50.0F);
         RenderSystem.setProjectionMatrix(this.projection.getBuffer(this.matrix), ProjectionType.PERSPECTIVE);
         SubmitNodeStorage collector = this.collector;
         float hand = OpeningMotion.hand(time);
         if (!this.bodyFailed && !this.bodySubmitted) {
            try {
               EntityRenderer renderer = client.getEntityRenderDispatcher().getRenderer(player);
               EntityRenderState snapshot = renderer.createRenderState(player, 1.0F);
               snapshot.nameTag = null;
               snapshot.nameTagAttachment = null;
               snapshot.leashStates = null;
               snapshot.shadowPieces.clear();
               snapshot.displayFireAnimation = false;
               snapshot.outlineColor = 0;
               snapshot.lightCoords = 15728880;
               if (snapshot instanceof LivingEntityRenderState living) {
                  living.bodyRot = 180.0F;
                  living.yRot = 0.0F;
                  living.xRot = 0.0F;
                  living.deathTime = 0.0F;
                  living.walkAnimationSpeed = 0.0F;
                  living.pose = Pose.STANDING;
               }

               if (snapshot instanceof ArmedEntityRenderState armed) {
                  armed.rightHandItemState.clear();
                  armed.leftHandItemState.clear();
                  armed.rightHandItemStack = ItemStack.EMPTY;
                  armed.leftHandItemStack = ItemStack.EMPTY;
                  armed.rightArmPose = armed.leftArmPose = ArmPose.EMPTY;
                  armed.swingAnimation = OpeningMotion.handVisible(time) ? 0.2F + hand * 0.55F : 0.0F;
                  armed.currentSwing = new LivingEntity.SwingDescription(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, 6);
               }

               if (snapshot instanceof HumanoidRenderState humanoid) {
                  humanoid.isUsingItem = false;
                  humanoid.isFallFlying = false;
                  humanoid.isVisuallySwimming = false;
                  humanoid.swimAmount = 0.0F;
                  humanoid.mainArm = player.getMainArm();
               }

               if (snapshot instanceof AvatarRenderState avatar) {
                  avatar.scoreText = null;
                  avatar.arrowCount = avatar.stingerCount = 0;
               }

               PoseStack pose = new PoseStack();
               pose.translate(0.0, -1.62, 0.65);
               renderer.submit(snapshot, pose, collector, this.camera);
               this.render(target);
               bodyFrames++;
               this.bodySubmitted = true;
            } catch (RuntimeException ex) {
               this.bodyFailed = true;
               collector.drainPhases(phase -> phase.sortInto((node, ordered) -> {}));
               MatInventoryClient.LOG.warn("Active player renderer cannot draw the opening body; keeping native hand gesture", ex);
            }
         }

         if (!this.handFailed && (warming && !this.handWarmed || OpeningMotion.handVisible(time))) {
            try {
               PoseStack pose = new PoseStack();
               float side = player.getMainArm() == HumanoidArm.RIGHT ? 1.0F : -1.0F;
               float sweep = OpeningMotion.smooth(hand);
               float reach = (float)Math.sin(Math.PI * hand);
               AvatarRenderer<AbstractClientPlayer> renderer = (AvatarRenderer<AbstractClientPlayer>)(Object)client.getEntityRenderDispatcher().getRenderer(player);
               ModelPart arm = side > 0.0F ? renderer.getModel().rightArm : renderer.getModel().leftArm;
               if (this.measuredArm != arm || this.measuredGeneration != MatMaterials.generation) {
                  arm.resetPose();
                  arm.zRot = side * 0.1F;
                  PoseStack bonePose = new PoseStack();
                  arm.translateAndRotate(bonePose);
                  bonePose.last().pose().transformDirection(this.armAxis.set(0.0F, 1.0F, 0.0F)).normalize();
                  float[] along = new float[]{Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY};

                  for (int i = 0; i < 3; i++) {
                     this.armBounds[i] = Float.POSITIVE_INFINITY;
                     this.armBounds[i + 3] = Float.NEGATIVE_INFINITY;
                  }

                  arm.visit(
                     new PoseStack(),
                     (partPose, path, index, cube) -> {
                        for (int i = 0; i < 8; i++) {
                           partPose.pose()
                              .transformPosition(
                                 this.corner
                                    .set(
                                       ((i & 1) == 0 ? cube.minX : cube.maxX) / 16.0F,
                                       ((i & 2) == 0 ? cube.minY : cube.maxY) / 16.0F,
                                       ((i & 4) == 0 ? cube.minZ : cube.maxZ) / 16.0F
                                    )
                              );

                           for (int axis = 0; axis < 3; axis++) {
                              this.armBounds[axis] = Math.min(this.armBounds[axis], this.corner.get(axis));
                              this.armBounds[axis + 3] = Math.max(this.armBounds[axis + 3], this.corner.get(axis));
                           }

                           float distance = this.corner.dot(this.armAxis);
                           along[0] = Math.min(along[0], distance);
                           along[1] = Math.max(along[1], distance);
                        }
                     }
                  );
                  this.armLength = along[1] - along[0];
                  if (!Float.isFinite(this.armLength) || this.armLength < 0.001F) {
                     throw new IllegalStateException("Active arm has no measurable geometry");
                  }

                  this.corner
                     .set(
                        (this.armBounds[0] + this.armBounds[3]) * 0.5F,
                        (this.armBounds[1] + this.armBounds[4]) * 0.5F,
                        (this.armBounds[2] + this.armBounds[5]) * 0.5F
                     );
                  float offset = this.corner.dot(this.armAxis) - along[0];
                  this.corner.fma(-offset, this.armAxis);
                  this.armShoulderX = this.corner.x;
                  this.armShoulderY = this.corner.y;
                  this.armShoulderZ = this.corner.z;
                  this.measuredArm = arm;
                  this.measuredGeneration = MatMaterials.generation;
               }

               float exit = OpeningMotion.smooth((hand - 0.58F) / 0.42F);
               if (warming) {
                  pose.translate(0.0F, -10.0F, 0.0F);
               }

               float shoulderX = 0.85F;
               float shoulderY = -1.2F - 0.35F * (1.0F - reach);
               float shoulderZ = -0.65F;
               float wristX = -1.15F + 2.5F * sweep;
               float wristY = -1.05F + 1.5F * reach - 0.35F * exit;
               float wristZ = -1.45F - 0.35F * (1.0F - reach) - 0.2F * exit;
               this.aim.set(wristX - shoulderX, wristY - shoulderY, wristZ - shoulderZ);
               float scale = this.aim.length() / this.armLength;
               this.aim.normalize();
               this.armRotation.rotationTo(this.armAxis, this.aim);
               pose.translate(shoulderX, shoulderY, shoulderZ);
               pose.rotate(this.armRotation);
               pose.scale(scale, scale, scale);
               pose.translate(-this.armShoulderX, -this.armShoulderY, -this.armShoulderZ);
               Identifier texture = player.getSkin().body().texturePath();
               if (side > 0.0F) {
                  renderer.renderRightHand(pose, collector, 15728880, texture, player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE));
               } else {
                  renderer.renderLeftHand(pose, collector, 15728880, texture, player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE));
               }

               this.render(target);
               handFrames++;
               this.handWarmed = true;
            } catch (RuntimeException ex) {
               this.handFailed = true;
               collector.drainPhases(phase -> phase.sortInto((node, ordered) -> {}));
               MatInventoryClient.LOG.warn("Active first-person renderer cannot draw the opening gesture; inventory remains usable", ex);
            }
         }
      }
   }

   private void render(SceneTarget target) {
      try (
         FeatureRenderDispatcher.PreparedFrame frame = Minecraft.getInstance().gameRenderer.featureRenderDispatcher().prepareFrame(this.collector);
         RenderPass pass = target.open();
      ) {
         FeatureRenderDispatcher.renderAllFeatures(pass, frame);
      }
   }

   @Override
   public void close() {
      this.projection.close();
   }
}
