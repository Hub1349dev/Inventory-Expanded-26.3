package com.example.matinventory.scene;

import com.example.matinventory.animation.OpeningMotion;
import com.example.matinventory.arrangement.ItemPresentationData;
import com.example.matinventory.layout.MatLayout;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

@Environment(EnvType.CLIENT)
public final class MatCamera {
   public static final float GROUND_Y = -0.11F;
   public final Matrix4f projection = new Matrix4f();
   public final Matrix4f view = new Matrix4f();
   private final Matrix4f combined = new Matrix4f();
   private final Matrix4f inverse = new Matrix4f();
   private final Vector3f near = new Vector3f();
   private final Vector3f far = new Vector3f();
   private final Vector4f point = new Vector4f();
   public final Vector3f hit = new Vector3f();
   public int width;
   public int height;
   public float coverHalfWidth = 8.0F;
   public float coverHalfDepth = 5.1F;
   public float panX;
   public float panZ = 2.25F;
   public boolean overview;
   public boolean locked;
   private long lastUpdate;
   private float minPanX = -14.0F;
   private float minPanZ = -6.0F;
   private float maxPanX = 12.0F;
   private float maxPanZ = 6.0F;
   private float velocityX;
   private float velocityZ;
   private float aimX;
   private float aimZ = 2.25F;
   private float lastMouseX;
   private float lastMouseZ;
   private boolean pointerReady;
   private float upperRight;
   public float craftFocus;
   public float craftPreview;
   private float craftAnchorX;
   private float craftAnchorZ;
   private static final float EYE_HEIGHT = 11.2F;
   private static final float HOME_Z = 2.25F;

   public void focus(float x, float z) {
      this.panX = this.aimX = x;
      this.panZ = this.aimZ = z;
      this.locked = true;
      this.velocityX = this.velocityZ = 0.0F;
      this.pointerReady = false;
   }

   public void anchorPointer() {
      this.pointerReady = false;
   }

   public void embedded(int width, int height, float x0, float z0, float x1, float z1) {
      this.width = width;
      this.height = height;
      this.locked = true;
      this.overview = false;
      this.panX = (x0 + x1) * 0.5F;
      this.panZ = (z0 + z1) * 0.5F;
      float aspect = (float)width / height;
      float halfDepth = Math.max((z1 - z0) * 0.5F, (x1 - x0) * 0.5F / aspect) * 1.08F;
      this.projection.identity().ortho(-halfDepth * aspect, halfDepth * aspect, -halfDepth, halfDepth, 0.1F, 200.0F);
      this.view.identity().lookAt(this.panX, 60.0F, this.panZ, this.panX, 0.0F, this.panZ, 0.0F, 0.0F, -1.0F);
      this.projection.mul(this.view, this.combined);
      this.combined.invert(this.inverse);
      this.coverHalfWidth = halfDepth * aspect;
      this.coverHalfDepth = halfDepth;
   }

   public void hold() {
      this.aimX = this.panX;
      this.aimZ = this.panZ;
      this.velocityX = this.velocityZ = 0.0F;
      this.pointerReady = false;
   }

   public void frame(MatLayout layout) {
      this.minPanX = -14.0F;
      this.minPanZ = -6.0F;
      this.maxPanX = 12.0F;
      this.maxPanZ = 6.0F;

      for (ItemPresentationData.Compartment c : layout.compartments()) {
         this.minPanX = Math.min(this.minPanX, c.x0() + 2.0F);
         this.minPanZ = Math.min(this.minPanZ, c.z0() + 2.0F);
         this.maxPanX = Math.max(this.maxPanX, c.x1() - 2.0F);
         this.maxPanZ = Math.max(this.maxPanZ, c.z1() - 2.0F);
      }
   }

   public void update(int w, int h, float mouseX, float mouseY, float pitch, float parallax) {
      this.update(w, h, mouseX, mouseY, pitch, 45.0F, parallax);
   }

   public void update(int w, int h, float mouseX, float mouseY, float pitch, float fov, float parallax) {
      this.width = Math.max(1, w);
      this.height = Math.max(1, h);
      float aspect = (float)this.width / this.height;
      long now = System.nanoTime();
      float dt = this.lastUpdate == 0L ? 0.0F : Math.min(0.05F, (float)(now - this.lastUpdate) / 1.0E9F);
      this.lastUpdate = now;
      float mx = mouseX / this.width;
      float mz = mouseY / this.height;
      this.navigate(mx, mz, dt);
      float craftZoom = this.craftFocus * 0.3F;
      float sceneFov = fov - (!this.overview && !this.locked ? 1.5F * this.upperRight : 0.0F) - 0.65F * this.craftPreview - 3.0F * craftZoom;
      float angle = (float)Math.toRadians(pitch);
      float tan = (float)Math.tan(Math.toRadians(sceneFov) * 0.5);
      float distance = this.overview ? Math.max(20.0F / (tan * aspect), 11.0F / tan) : 13.0F;
      float px = this.overview ? 1.0F : this.panX;
      float pz = this.overview ? -2.0F : this.panZ;
      float yaw = this.overview ? 0.0F : Math.clamp(this.panX / 16.0F, -1.0F, 1.0F) * (float)Math.toRadians(6.0);
      this.projection.identity().perspective((float)Math.toRadians(sceneFov), aspect, 0.1F, 250.0F);
      if (!this.overview && !this.locked) {
         float eyeZ = 2.25F + 11.2F / (float)Math.tan(angle);
         float targetX = this.panX + (this.craftAnchorX + (this.panX - this.craftAnchorX) * 0.38F - this.panX) * this.craftFocus;
         float targetZ = this.panZ + (this.craftAnchorZ + (this.panZ - this.craftAnchorZ) * 0.38F - this.panZ) * this.craftFocus;
         float eyeX = targetX * 0.72F * craftZoom;
         float focusedZ = targetZ + 3.0F;
         eyeZ += (focusedZ - eyeZ) * craftZoom;
         float eyeY = 11.2F - 3.0F * craftZoom;
         float dx = eyeX - targetX;
         float dz = eyeZ - targetZ;
         float horizontal = (float)Math.sqrt(dx * dx + dz * dz);
         float radius = (float)Math.sqrt(horizontal * horizontal + eyeY * eyeY);
         float elevation = (float)Math.atan2(eyeY, horizontal) + (float)Math.toRadians(4.0) * this.craftFocus;
         float overheadScale = horizontal > 1.0E-4F ? radius * (float)Math.cos(elevation) / horizontal : 1.0F;
         if (this.craftFocus > 0.0F) {
            eyeX = targetX + dx * overheadScale;
            eyeZ = targetZ + dz * overheadScale;
            eyeY = radius * (float)Math.sin(elevation);
         }

         this.view.identity().lookAt(eyeX, eyeY, eyeZ, targetX, 0.0F, targetZ, 0.0F, 1.0F, 0.0F);
         distance = 11.2F / (float)Math.sin(angle);
      } else {
         this.view
            .identity()
            .lookAt(
               px + distance * (float)Math.cos(angle) * (float)Math.sin(yaw),
               distance * (float)Math.sin(angle),
               pz + distance * (float)Math.cos(angle) * (float)Math.cos(yaw),
               px,
               0.0F,
               pz,
               0.0F,
               1.0F,
               0.0F
            );
      }

      this.projection.mul(this.view, this.combined);
      this.combined.invert(this.inverse);
      this.coverHalfWidth = Math.abs(px) + distance * tan * aspect + 12.0F;
      this.coverHalfDepth = Math.abs(pz) + distance * tan / Math.max(0.7F, (float)Math.sin(angle)) + 12.0F;

      for (int i = 0; i < 4; i++) {
         if (this.raycastAt((i & 1) == 0 ? 0.0 : this.width, (i & 2) == 0 ? 0.0 : this.height, -0.11F)) {
            this.coverHalfWidth = Math.max(this.coverHalfWidth, Math.abs(this.hit.x) + 3.0F);
            this.coverHalfDepth = Math.max(this.coverHalfDepth, Math.abs(this.hit.z) + 3.0F);
         }
      }
   }

   public void craftAnchor(float x, float z) {
      this.craftAnchorX = x;
      this.craftAnchorZ = z;
   }

   public void navigate(float mx, float mz, float dt) {
      if (!this.locked && !this.overview) {
         dt = Math.clamp(dt, 0.0F, 0.05F);
         if (dt != 0.0F) {
            boolean inside = mx >= 0.0F && mx <= 1.0F && mz >= 0.0F && mz <= 1.0F;
            float aspect = this.width > 0 && this.height > 0 ? (float)this.width / this.height : 1.7777778F;
            float screenFit = Math.clamp(1.7777778F / aspect, 1.0F, 1.8F);
            if (inside) {
               if (this.pointerReady) {
                  this.aimX = this.aimX + (pointerLookX(mx) - pointerLookX(this.lastMouseX)) * screenFit;
                  this.aimZ = this.aimZ + (pointerLookZ(mx, mz) - pointerLookZ(this.lastMouseX, this.lastMouseZ));
               }

               this.lastMouseX = mx;
               this.lastMouseZ = mz;
               this.pointerReady = true;
            } else {
               this.pointerReady = false;
            }

            float side = 6.5F * screenFit;
            float front = aspect < 1.3F ? 4.0F : 2.2F;
            float sideLook = Math.clamp(Math.abs(this.aimX) / side, 0.0F, 1.0F);
            float rightLook = Math.clamp(this.aimX / side, 0.0F, 1.0F);
            float top = Math.max(this.minPanZ, -front - 4.0F * rightLook);
            float bottom = Math.min(this.maxPanZ, 4.7F + 0.7F * sideLook + 5.0F * rightLook + 2.0F * (screenFit - 1.0F));
            this.aimZ = Math.clamp(this.aimZ, top, bottom);
            float upper = Math.clamp((2.25F - this.aimZ) / (2.25F - top), 0.0F, 1.0F);
            float left = Math.max(this.minPanX, -side * (1.0F - 0.15F * upper));
            float right = Math.min(this.maxPanX, side * (1.0F - 0.08F * upper) + (6.5F - 1.5F * upper) * rightLook * screenFit);
            this.aimX = Math.clamp(this.aimX, left, right);
            float omega = 3.0F;
            float e = (float)Math.exp(-omega * dt);
            float dx = this.panX - this.aimX;
            float dz = this.panZ - this.aimZ;
            float jx = this.velocityX + omega * dx;
            float jz = this.velocityZ + omega * dz;
            this.panX = this.aimX + (dx + jx * dt) * e;
            this.panZ = this.aimZ + (dz + jz * dt) * e;
            this.velocityX = (this.velocityX - omega * jx * dt) * e;
            this.velocityZ = (this.velocityZ - omega * jz * dt) * e;
            if (Math.abs(this.panX - this.aimX) < 1.0E-4F && Math.abs(this.velocityX) < 0.001F) {
               this.panX = this.aimX;
               this.velocityX = 0.0F;
            }

            if (Math.abs(this.panZ - this.aimZ) < 1.0E-4F && Math.abs(this.velocityZ) < 0.001F) {
               this.panZ = this.aimZ;
               this.velocityZ = 0.0F;
            }

            this.upperRight = Math.clamp((2.25F - this.panZ) / (2.25F + front + 4.0F), 0.0F, 1.0F) * Math.clamp(this.panX / side, 0.0F, 1.0F);
         }
      } else {
         this.velocityX = this.velocityZ = 0.0F;
         this.pointerReady = false;
      }
   }

   public void opening(MatLayout layout, float time, float pitch, float fov) {
      if (!(time >= 1.02F) && !this.locked && !this.overview) {
         float t = Math.max(0.0F, time);
         float left = Float.POSITIVE_INFINITY;
         float right = -left;

         for (ItemPresentationData.Compartment c : layout.solidCompartments()) {
            left = Math.min(left, c.x0());
            right = Math.max(right, c.x1());
         }

         if (Float.isFinite(left + right)) {
            float follow = OpeningMotion.follow(t);
            float home = OpeningMotion.returnHome(t);
            float start = left + (right - left) / 6.0F;
            float middle = (left + right) * 0.5F;
            float targetX = (start + (middle - start) * follow) * (1.0F - home) + this.panX * home;
            float targetZ = (-0.8F + (this.panZ + 0.8F) * follow) * (1.0F - home) + this.panZ * home;
            float eyeHeight = 7.8F + 3.3999996F * home;
            float eyeX = targetX * 0.2F * (1.0F - home);
            float eyeZ = 2.25F + 11.2F / (float)Math.tan(Math.toRadians(pitch));
            float shake = OpeningMotion.shake(t);
            float introFov = fov + 5.0F * (1.0F - home);
            this.projection.identity().perspective((float)Math.toRadians(introFov), (float)this.width / this.height, 0.1F, 250.0F);
            this.view
               .identity()
               .lookAt(eyeX + shake * 0.5F, eyeHeight + shake, eyeZ + shake * 0.3F, targetX, 0.0F, targetZ, 0.0F, 1.0F, 0.0F)
               .rotateZ(shake * 0.08F);
            this.projection.mul(this.view, this.combined);
            this.combined.invert(this.inverse);

            for (int i = 0; i < 4; i++) {
               if (this.raycastAt((i & 1) == 0 ? 0.0 : this.width, (i & 2) == 0 ? 0.0 : this.height, -0.11F)) {
                  this.coverHalfWidth = Math.max(this.coverHalfWidth, Math.min(96.0F, Math.abs(this.hit.x) + 3.0F));
                  this.coverHalfDepth = Math.max(this.coverHalfDepth, Math.min(96.0F, Math.abs(this.hit.z) + 3.0F));
               }
            }
         }
      }
   }

   private static float pointerLookX(float x) {
      return (x - 0.5F) * 20.0F + Math.max(0.0F, x - 0.65F) * 12.0F;
   }

   private static float pointerLookZ(float x, float z) {
      return (z - 0.5F) * (12.0F + 12.0F * Math.clamp((x - 0.65F) / 0.35F, 0.0F, 1.0F));
   }

   public void screenRay(double mouseX, double mouseY, Vector3f origin, Vector3f direction) {
      float x = (float)mouseX / this.width * 2.0F - 1.0F;
      float y = 1.0F - (float)mouseY / this.height * 2.0F;
      this.inverse.transformProject(origin.set(x, y, -1.0F));
      this.inverse.transformProject(direction.set(x, y, 1.0F));
      direction.sub(origin).normalize();
   }

   public boolean raycast(double mouseX, double mouseY) {
      return this.raycastAt(mouseX, mouseY, 0.0F);
   }

   public boolean raycastAt(double mouseX, double mouseY, float planeHeight) {
      float x = (float)mouseX / this.width * 2.0F - 1.0F;
      float y = 1.0F - (float)mouseY / this.height * 2.0F;
      this.inverse.transformProject(this.near.set(x, y, -1.0F));
      this.inverse.transformProject(this.far.set(x, y, 1.0F));
      float dy = this.far.y - this.near.y;
      if (Math.abs(dy) < 1.0E-5F) {
         return false;
      }

      float t = (planeHeight - this.near.y) / dy;
      if (t < 0.0F) {
         return false;
      }

      this.hit.set(this.near.x + (this.far.x - this.near.x) * t, planeHeight, this.near.z + (this.far.z - this.near.z) * t);
      return true;
   }

   public float deckHalfWidth() {
      return this.coverHalfWidth;
   }

   public Vector4f project(float x, float y, float z) {
      this.combined.transform(this.point.set(x, y, z, 1.0F));
      this.point.div(this.point.w);
      this.point.x = (this.point.x + 1.0F) * 0.5F * this.width;
      this.point.y = (1.0F - this.point.y) * 0.5F * this.height;
      return this.point;
   }
}
