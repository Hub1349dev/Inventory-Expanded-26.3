package com.example.matinventory.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Environment(EnvType.CLIENT)
@Mixin(Camera.class)
public interface MatCameraAccess {
   @Invoker("setPosition")
   void matinventory$position(double x, double y, double z);

   @Invoker("setRotation")
   void matinventory$rotation(float yaw, float pitch);

   @Invoker("setupPerspective")
   void matinventory$perspective(float near, float far, float fov, float width, float height);

   @Invoker("prepareCullFrustum")
   void matinventory$frustum(Matrix4fc view, Matrix4f projection, Vec3 position);

   @Accessor("depthFar")
   float matinventory$depthFar();
}
