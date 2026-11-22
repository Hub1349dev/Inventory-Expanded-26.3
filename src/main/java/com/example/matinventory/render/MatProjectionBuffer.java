package com.example.matinventory.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
final class MatProjectionBuffer implements AutoCloseable {
   private final ProjectionMatrixBuffer buffer;
   private final Matrix4f previous = new Matrix4f();
   private final Matrix4f reversed = new Matrix4f();
   private GpuBufferSlice slice;

   MatProjectionBuffer(String name) {
      this.buffer = new ProjectionMatrixBuffer(name);
   }

   GpuBufferSlice getBuffer(Matrix4f matrix) {
      if (this.slice == null || !this.previous.equals(matrix)) {
         boolean zeroToOne = RenderSystem.getDevice().getDeviceInfo().isZZeroToOne();
         this.reversed.translation(0.0F, 0.0F, zeroToOne ? 0.5F : 0.0F).scale(1.0F, 1.0F, zeroToOne ? -0.5F : -1.0F).mul(matrix);
         this.slice = this.buffer.getBuffer(this.reversed);
         this.previous.set(matrix);
      }

      return this.slice;
   }

   @Override
   public void close() {
      this.buffer.close();
   }
}
