package com.example.matinventory.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline.Builder;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.FilterMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
final class MatRenderTypes {
   private static final RenderPipeline[] COLORED = new RenderPipeline[]{
      colored("", false, false), colored("_natural", true, false), colored("_embedded", false, true), colored("_embedded_natural", true, true)
   };
   private static final RenderPipeline SOFT = RenderPipeline.builder(RenderPipelines.ENTITY_EMISSIVE_SNIPPET)
      .withLocation(Identifier.fromNamespaceAndPath("matinventory", "pipeline/soft_decal"))
      .withShaderDefine("ALPHA_CUTOUT", 0.001F)
      .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
      .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
      .withCull(false)
      .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
      .build();
   private static final RenderPipeline SILHOUETTE = RenderPipeline.builder(RenderPipelines.OUTLINE_SNIPPET)
      .withLocation(Identifier.fromNamespaceAndPath("matinventory", "pipeline/soft_silhouette"))
      .withFragmentShader(Identifier.fromNamespaceAndPath("matinventory", "core/alpha_mask"))
      .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
      .withCull(false)
      .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
      .build();
   private static final RenderPipeline MATERIAL = RenderPipeline.builder(RenderPipelines.ENTITY_EMISSIVE_SNIPPET)
      .withLocation(Identifier.fromNamespaceAndPath("matinventory", "pipeline/baked_material"))
      .withShaderDefine("NO_CARDINAL_LIGHTING")
      .withShaderDefine("ALPHA_CUTOUT", 0.001F)
      .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
      .withCull(false)
      .withColorTargetState(ColorTargetState.DEFAULT)
      .build();
   private static final RenderPipeline DYED = RenderPipeline.builder(RenderPipelines.ENTITY_EMISSIVE_SNIPPET)
      .withLocation(Identifier.fromNamespaceAndPath("matinventory", "pipeline/dyed_canvas"))
      .withFragmentShader(Identifier.fromNamespaceAndPath("matinventory", "core/canvas_tint"))
      .withShaderDefine("NO_CARDINAL_LIGHTING")
      .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
      .withCull(false)
      .withColorTargetState(ColorTargetState.DEFAULT)
      .build();
   private static RenderType dyed;
   private static final Map<Identifier, RenderType> materials = new HashMap<>();
   private static final Map<Identifier, RenderType> grounds = new HashMap<>();
   private static final Map<Identifier, RenderType> decals = new HashMap<>();
   private static final Map<Identifier, RenderType> outlines = new HashMap<>();

   private static RenderPipeline colored(String suffix, boolean natural, boolean embedded) {
      Builder builder = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
         .withLocation(Identifier.fromNamespaceAndPath("matinventory", "pipeline/colored_light" + suffix))
         .withFragmentShader(Identifier.fromNamespaceAndPath("matinventory", "core/colored_light"))
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA));
      if (natural) {
         builder.withShaderDefine("NATURAL_TINT");
      }

      if (embedded) {
         builder.withShaderDefine("PRESERVE_ALPHA");
      }

      return builder.build();
   }

   static RenderPipeline colored(boolean natural, boolean embedded) {
      return COLORED[(natural ? 1 : 0) + (embedded ? 2 : 0)];
   }

   static RenderType dyedCanvas() {
      if (dyed == null) {
         dyed = RenderType.create(
            "matinventory dyed canvas", RenderSetup.builder(DYED).withTexture("Sampler0", MatMaterials.LEATHER).useOverlay().createRenderSetup()
         );
      }

      return dyed;
   }

   static RenderType material(Identifier texture) {
      return materials.computeIfAbsent(
         texture,
         id -> RenderType.create("matinventory pixel material", RenderSetup.builder(MATERIAL).withTexture("Sampler0", id).useOverlay().createRenderSetup())
      );
   }

   static RenderType ground(Identifier texture) {
      return grounds.computeIfAbsent(
         texture,
         id -> RenderType.create(
            "matinventory ground nearest",
            RenderSetup.builder(MATERIAL)
               .withTexture("Sampler0", id, () -> RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST))
               .useOverlay()
               .createRenderSetup()
         )
      );
   }

   static Identifier texture(RenderType type) {
      if (type == dyed) {
         return ShaderCanvas.texture();
      }

      for (Map<Identifier, RenderType> map : List.of(materials, grounds, decals, outlines)) {
         for (Entry<Identifier, RenderType> entry : map.entrySet()) {
            if (entry.getValue() == type) {
               return entry.getKey();
            }
         }
      }

      return null;
   }

   static boolean soft(RenderType type) {
      return decals.containsValue(type) || outlines.containsValue(type);
   }

   static RenderType decal(Identifier texture) {
      return decals.computeIfAbsent(
         texture, id -> RenderType.create("matinventory soft decal", RenderSetup.builder(SOFT).withTexture("Sampler0", id).useOverlay().createRenderSetup())
      );
   }

   static RenderType silhouette(Identifier atlas) {
      return outlines.computeIfAbsent(
         atlas, id -> RenderType.create("matinventory soft silhouette", RenderSetup.builder(SILHOUETTE).withTexture("Sampler0", id).createRenderSetup())
      );
   }
}
