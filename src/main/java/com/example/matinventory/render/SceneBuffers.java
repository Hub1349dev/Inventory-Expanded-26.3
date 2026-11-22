package com.example.matinventory.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.rendertype.RenderType;

@Environment(EnvType.CLIENT)
interface SceneBuffers {
   VertexConsumer getBuffer(RenderType type);

   default void endBatch() {
   }
}
