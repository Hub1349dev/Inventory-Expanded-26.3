package com.example.matinventory.render;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.equipment.ElytraModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState.LayerRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@Environment(EnvType.CLIENT)
final class NativeElytraModel implements SpecialModelRenderer<Boolean> {
   static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/equipment/wings/elytra.png");
   private final ElytraModel model = new ElytraModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.ELYTRA));
   private final Vector3fc[] extents;

   NativeElytraModel() {
      this.model.root().getChild("left_wing").zRot = -0.08726647F;
      this.model.root().getChild("right_wing").zRot = 0.08726647F;
      this.model.root().getChild("left_wing").xRot = 0.0F;
      this.model.root().getChild("right_wing").xRot = 0.0F;
      this.model.root().getChild("right_wing").z = -0.25F;
      ArrayList<Vector3fc> points = new ArrayList<>();
      this.model.root().getExtentsForGui(new PoseStack(), p -> points.add(new Vector3f(p)));
      this.extents = points.toArray(Vector3fc[]::new);
   }

   void resolve(ItemStackRenderState state, ItemStack stack) {
      state.clear();
      LayerRenderState layer = state.newLayer();
      layer.setupSpecialModel(this, stack.hasFoil());
      layer.setExtents(() -> this.extents);
      layer.setUsesBlockLight(true);
   }

   public void submit(
      Boolean foil, PoseStack pose, SubmitNodeCollector collector, int light, int overlay, boolean glint, int outline
   ) {
      RenderType type = Boolean.TRUE.equals(foil) ? RenderTypes.armorCutoutNoCullGlint(TEXTURE) : RenderTypes.armorCutoutNoCull(TEXTURE);
      collector.submitModelPart(this.model.root(), pose, type, light, overlay, null, -1, outline);
   }

   @Override
   public void getExtents(Consumer<Vector3fc> consumer) {
      for (Vector3fc p : this.extents) {
         consumer.accept(p);
      }
   }

   public Boolean extractArgument(ItemStack stack) {
      return stack.hasFoil();
   }
}
