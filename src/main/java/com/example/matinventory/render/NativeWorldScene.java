package com.example.matinventory.render;

import com.example.matinventory.MatInventoryClient;
import com.example.matinventory.mixin.MatCameraAccess;
import com.example.matinventory.scene.MatPlacement;
import com.example.matinventory.scene.MatWorldContext;
import com.example.matinventory.screen.MatInventoryScreen;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public final class NativeWorldScene {
   private static MatSceneState scene;
   private static Drawer drawer;
   private static boolean shader;
   private static boolean drawing;
   private static boolean deferred;
   private static Vec3 oldPosition;
   private static float oldYaw;
   private static float oldPitch;
   private static double anchorX;
   private static double anchorY;
   private static double anchorZ;
   private static float yaw;
   private static float unit;
   private static final Matrix4f inversePose = new Matrix4f();
   public static long frames;

   public static boolean active() {
      return scene != null;
   }

   public static boolean drawing() {
      return drawing;
   }

   public static boolean deferred() {
      return deferred;
   }

   public static MatWorldContext world() {
      return drawing ? scene.world : null;
   }

   static void install(Drawer renderer) {
      drawer = renderer;
   }

   static boolean litByLevel(MatSceneState state) {
      return shader && scene == state;
   }

   static void toMat(Vector3f point) {
      point.mulPosition(inversePose);
   }

   public static void beginFrame() {
      Minecraft client = Minecraft.getInstance();
      scene = null;
      shader = false;
      deferred = !MatInventoryClient.renderFailed && client.level != null && client.player != null && client.gui.screen() instanceof MatInventoryScreen;
   }

   static void request(MatSceneState state) {
      if (deferred && !state.embedded) {
         boolean shaders = ShaderSupport.active();
         if (shaders || state.actualWorld()) {
            scene = state;
            shader = shaders;
         }
      }
   }

   public static void finishExtraction() {
      deferred = false;
   }

   public static void camera(Camera camera) {
      if (!active()) {
         return;
      }

      Minecraft client = Minecraft.getInstance();
      MatCameraAccess access = (MatCameraAccess)camera;
      oldPosition = camera.position();
      oldYaw = camera.yRot();
      oldPitch = camera.xRot();
      unit = MatPlacement.sceneUnit();
      yaw = (float)Math.toRadians(180.0F - scene.world.placement.yaw());
      MatPlacement.Result location = scene.world.placement;
      anchorX = location.x();
      anchorZ = location.z();
      anchorY = scene.actualWorld() ? location.y() : Math.max(client.level.getMaxY() + 128, client.player.getY() + 256.0);
      Matrix4f inverse = new Matrix4f(scene.view()).invert();
      Vector3f eye = inverse.getTranslation(new Vector3f());
      Vector3f forward = inverse.transformDirection(new Vector3f(0.0F, 0.0F, -1.0F)).rotateY(yaw).normalize();
      Vector3f offset = new Vector3f(eye.x, eye.y - -0.11F, eye.z).mul(unit).rotateY(yaw);
      access.matinventory$position(anchorX + offset.x, anchorY + offset.y, anchorZ + offset.z);
      access.matinventory$rotation((float)Math.toDegrees(Math.atan2(-forward.x, forward.z)), (float)Math.toDegrees(Math.asin(-forward.y)));
      float fov = 2.0F * (float)Math.atan(1.0F / scene.projection().m11());
      float aspect = scene.projection().m11() / scene.projection().m00();
      float near = 0.05F * unit;
      float far = access.matinventory$depthFar();
      access.matinventory$perspective(near, far, (float)Math.toDegrees(fov), aspect, 1.0F);
      access.matinventory$frustum(
         camera.getViewRotationMatrix(new Matrix4f()),
         new Matrix4f().perspective(fov, aspect, near, far, RenderSystem.getDevice().getDeviceInfo().isZZeroToOne()),
         camera.position()
      );
   }

   public static void restoreCamera(Camera camera) {
      if (oldPosition != null) {
         MatCameraAccess access = (MatCameraAccess)camera;
         access.matinventory$position(oldPosition.x, oldPosition.y, oldPosition.z);
         access.matinventory$rotation(oldYaw, oldPitch);
         oldPosition = null;
      }
   }

   public static void draw(SubmitNodeStorage collector, Vec3 camera) {
      if (!active() || !shader || drawer == null) {
         return;
      }

      PoseStack pose = new PoseStack();
      pose.translate(anchorX - camera.x, anchorY - camera.y, anchorZ - camera.z);
      pose.rotate(new Quaternionf().rotationY(yaw));
      pose.scale(unit, unit, unit);
      pose.translate(0.0F, 0.11F, 0.0F);
      inversePose.set(pose.last().pose()).invert();
      drawing = true;

      try {
         drawer.draw(scene, pose, collector);
         frames++;
         ShaderSupport.frames++;
      } catch (RuntimeException ex) {
         MatInventoryClient.LOG.error("3D inventory level pass failed; returning to vanilla", ex);
         MatInventoryClient.renderFailed = true;
      } finally {
         drawing = false;
      }
   }

   private NativeWorldScene() {
   }

   @Environment(EnvType.CLIENT)
   interface Drawer {
      void draw(MatSceneState state, PoseStack pose, SubmitNodeStorage collector);
   }
}
