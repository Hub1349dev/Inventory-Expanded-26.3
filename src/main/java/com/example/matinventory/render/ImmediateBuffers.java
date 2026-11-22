package com.example.matinventory.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.renderpearl.api.commands.RenderPass;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.client.renderer.rendertype.RenderType;

@Environment(EnvType.CLIENT)
final class ImmediateBuffers implements SceneBuffers, AutoCloseable {
   private final StagedVertexBuffer staged = new StagedVertexBuffer(() -> "Inventory scene", 4194304);
   private final SceneTarget target;
   private final List<Batch> batches = new ArrayList<>();
   private final Map<RenderType, Batch> solid = new IdentityHashMap<>();

   ImmediateBuffers(SceneTarget target) {
      this.target = target;
   }

   @Override
   public VertexConsumer getBuffer(RenderType type) {
      Batch batch;
      if (type.hasBlending()) {
         batch = !this.batches.isEmpty() && this.batches.getLast().type() == type ? this.batches.getLast() : this.append(type);
      } else {
         batch = this.solid.get(type);
         if (batch == null) {
            batch = this.append(type);
            this.solid.put(type, batch);
         }
      }

      return this.staged.getVertexBuilder(batch.draw());
   }

   private Batch append(RenderType type) {
      VertexSorting sorting = type.sortOnUpload() ? RenderSystem.getProjectionType().vertexSorting() : null;
      Batch batch = new Batch(type, this.staged.appendDraw(type.format(), type.primitiveTopology(), sorting));
      this.batches.add(batch);
      return batch;
   }

   @Override
   public void endBatch() {
      if (this.batches.isEmpty()) {
         return;
      }

      try {
         this.batches.sort(Comparator.comparing(batch -> batch.type().hasBlending()));
         this.staged.upload();
         List<PreparedRenderType> types = new ArrayList<>(this.batches.size());

         for (Batch batch : this.batches) {
            this.staged.requestIndexCount(batch.draw());
            types.add(batch.type().prepare());
         }

         RenderSystem.resizeAllAutoStorageIndexBuffers();

         try (RenderPass pass = this.target.open()) {
            for (int i = 0; i < types.size(); i++) {
               StagedVertexBuffer.ExecuteInfo info = this.staged.getExecuteInfo(this.batches.get(i).draw());
               if (info != null) {
                  types.get(i).drawFromBuffer(info, pass);
               }
            }
         }
      } finally {
         this.batches.clear();
         this.solid.clear();
         this.staged.endDraw();
      }
   }

   void endFrame() {
      this.batches.clear();
      this.solid.clear();
      this.staged.endFrame();
   }

   @Override
   public void close() {
      this.staged.close();
   }

   @Environment(EnvType.CLIENT)
   private record Batch(RenderType type, StagedVertexBuffer.Draw draw) {
   }
}
