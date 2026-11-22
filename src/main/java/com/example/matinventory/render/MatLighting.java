package com.example.matinventory.render;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import java.nio.ByteBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

@Environment(EnvType.CLIENT)
final class MatLighting implements AutoCloseable {
   private final GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> "Mat Inventory key light", 136, Lighting.UBO_SIZE);
   private final Vector3f direction = new Vector3f();
   private final Vector3f zero = new Vector3f();
   private final Vector3f uploaded = new Vector3f(Float.NaN);

   void apply(Matrix4f view) {
      view.transformDirection(this.direction.set(-0.48F, 1.0F, -0.38F).normalize());
      if (!this.direction.equals(this.uploaded)) {
         try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer bytes = Std140Builder.onStack(stack, Lighting.UBO_SIZE).putVec3(this.direction).putVec3(this.zero).get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.buffer.slice(), bytes);
            this.uploaded.set(this.direction);
         }
      }

      RenderSystem.setShaderLights(this.buffer.slice());
   }

   @Override
   public void close() {
      this.buffer.close();
   }
}
