package com.example.matinventory.render;

import com.example.matinventory.core.VisualItemKinds;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.object.bell.BellModel;
import net.minecraft.client.model.object.boat.AbstractBoatModel;
import net.minecraft.client.model.object.boat.BoatModel;
import net.minecraft.client.model.object.boat.RaftModel;
import net.minecraft.client.model.object.cart.MinecartModel;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeCollector.CustomGeometryRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState.LayerRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.DecoratedPotBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.GrowingPlantBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@Environment(EnvType.CLIENT)
final class NativeMatModels {
   private static final NativeMatModels.Presentation ORIGINAL = new NativeMatModels.Presentation(NativeMatModels.Pose.ORIGINAL, null);
   private static final NativeMatModels.Presentation UPRIGHT = new NativeMatModels.Presentation(NativeMatModels.Pose.UPRIGHT, null);
   private static final NativeMatModels.Presentation FLAT = new NativeMatModels.Presentation(NativeMatModels.Pose.FLAT, null);
   private final Map<BlockState, List<BlockStateModelPart>> blocks = new HashMap<>();
   private final Map<Item, NativeMatModels.Mesh> vehicles = new HashMap<>();
   private NativeMatModels.Mesh bell;
   private final NativeMatModels.SpecialProbe specialProbe = new NativeMatModels.SpecialProbe();
   private final PoseStack probePose = new PoseStack();

   NativeMatModels.Presentation resolve(ItemStackRenderState model, ItemStack stack, int seed) {
      Minecraft client = Minecraft.getInstance();
      Item item = stack.getItem();
      if (!stack.is(Items.TRIDENT) && !stack.is(Items.SPYGLASS)) {
         String path = BuiltInRegistries.ITEM.getKey(item).getPath();
         if (item instanceof BoatItem || item instanceof MinecartItem) {
            if (!BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("minecraft")) {
               return ORIGINAL;
            }

            NativeMatModels.Mesh mesh = this.vehicles.computeIfAbsent(item, k -> this.vehicle(stack, path));
            if (mesh != null) {
               model.clear();
               mesh.append(model, stack.hasFoil());
               return new NativeMatModels.Presentation(NativeMatModels.Pose.UPRIGHT, mesh.texture);
            }
         }

         if (!(item instanceof BlockItem blockItem)) {
            return ORIGINAL;
         } else {
            Block block = blockItem.getBlock();
            BlockState state = block.defaultBlockState();
            if (!VisualItemKinds.torch(stack) && !VisualItemKinds.plant(stack)) {
               if (block instanceof ButtonBlock) {
                  state = state.setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR);
               }

               if (!BuiltInRegistries.ITEM.getKey(item).equals(BuiltInRegistries.BLOCK.getKey(block))) {
                  return ORIGINAL;
               }

               if (!stack.is(BlockItemTags.FLOWERS.item())
                  && !(block instanceof VegetationBlock)
                  && !(block instanceof GrowingPlantBlock)
                  && !(block instanceof VineBlock)
                  && !(block instanceof MultifaceBlock)
                  && !(block instanceof BambooStalkBlock)
                  && !(block instanceof SugarCaneBlock)) {
                  this.specialProbe.clear();
                  this.specialProbe.found = false;
                  this.probePose.setIdentity();
                  model.submit(this.probePose, this.specialProbe, 15728880, 0, 0);
                  if (!this.specialProbe.found
                     && !(block instanceof BedBlock)
                     && !(block instanceof AbstractBannerBlock)
                     && !(block instanceof DecoratedPotBlock)
                     && state.getRenderShape() == RenderShape.MODEL) {
                     if (block instanceof CampfireBlock) {
                        state = state.setValue(CampfireBlock.LIT, false);
                     }

                     if (block instanceof FenceBlock) {
                        state = state.setValue(FenceBlock.EAST, true).setValue(FenceBlock.WEST, true);
                     }

                     if (block instanceof WallBlock) {
                        state = state.setValue(WallBlock.EAST, WallSide.LOW).setValue(WallBlock.WEST, WallSide.LOW);
                     }

                     List<BlockStateModelPart> parts = this.parts(state);
                     if (parts.stream()
                        .noneMatch(p -> !p.getQuads(null).isEmpty() || Arrays.stream(Direction.values()).anyMatch(d -> !p.getQuads(d).isEmpty()))) {
                        return ORIGINAL;
                     }

                     model.clear();
                     this.appendBlock(model, state, 0.0F);
                     if (block instanceof DoorBlock || block instanceof DoublePlantBlock) {
                        this.appendBlock(model, state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER), 1.0F);
                     }

                     if (block instanceof BellBlock) {
                        if (this.bell == null) {
                           this.bell = new NativeMatModels.Mesh(
                              new BellModel(client.getEntityModels().bakeLayer(ModelLayers.BELL)).root(),
                              Identifier.withDefaultNamespace("textures/entity/bell/bell_body.png"),
                              false,
                              null,
                              0
                           );
                        }

                        this.bell.append(model, stack.hasFoil());
                        return new NativeMatModels.Presentation(NativeMatModels.Pose.UPRIGHT, this.bell.texture);
                     } else {
                        return this.blockPose(block, stack, model);
                     }
                  } else {
                     client.getItemModelResolver().updateForTopItem(model, stack, ItemDisplayContext.NONE, client.level, client.player, seed);
                     return block instanceof AbstractBannerBlock ? FLAT : UPRIGHT;
                  }
               } else {
                  return ORIGINAL;
               }
            } else {
               return ORIGINAL;
            }
         }
      } else {
         client.getItemModelResolver().updateForTopItem(model, stack, ItemDisplayContext.NONE, client.level, client.player, seed);
         return stack.is(Items.TRIDENT)
            ? new NativeMatModels.Presentation(NativeMatModels.Pose.FLAT, Identifier.withDefaultNamespace("textures/entity/trident/trident.png"))
            : FLAT;
      }
   }

   private NativeMatModels.Presentation blockPose(Block block, ItemStack stack, ItemStackRenderState model) {
      AABB box = model.getModelBoundingBox();
      return block instanceof DoorBlock ? FLAT : UPRIGHT;
   }

   private List<BlockStateModelPart> parts(BlockState state) {
      return this.blocks.computeIfAbsent(state, s -> {
         ArrayList<BlockStateModelPart> out = new ArrayList<>();
         Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(s).collectParts(RandomSource.create(0L), out);
         return List.copyOf(out);
      });
   }

   private void appendBlock(ItemStackRenderState model, BlockState state, float y) {
      for (BlockStateModelPart part : this.parts(state)) {
         LayerRenderState layer = model.newLayer();
         List<BakedQuad> quads = new ArrayList<>();
         quads.addAll(part.getQuads(null));

         for (Direction side : Direction.values()) {
            quads.addAll(part.getQuads(side));
         }

         layer.setQuads(ItemQuads.split(quads));
         layer.setUsesBlockLight(true);
         layer.setParticleMaterial(part.particleMaterial());
         if (y != 0.0F) {
            layer.setItemTransform(new ItemTransform(new Vector3f(), new Vector3f(0.0F, y, 0.0F), new Vector3f(1.0F)));
         }

         ArrayList<Vector3fc> vertices = new ArrayList<>();
         int tint = -1;

         for (BakedQuad quad : quads) {
            for (int i = 0; i < 4; i++) {
               vertices.add(quad.position(i));
            }

            tint = Math.max(tint, quad.materialInfo().tintIndex());
         }

         Vector3fc[] extents = vertices.toArray(Vector3fc[]::new);
         layer.setExtents(() -> extents);
         if (tint >= 0) {
            IntList colors = layer.tintLayers();

            for (int i = 0; i <= tint; i++) {
               BlockTintSource source = Minecraft.getInstance().getBlockColors().getTintSource(state, i);
               colors.add(source == null ? -1 : source.color(state) | 0xFF000000);
            }
         }
      }
   }

   private NativeMatModels.Mesh vehicle(ItemStack stack, String path) {
      Minecraft client = Minecraft.getInstance();
      if (stack.getItem() instanceof BoatItem) {
         boolean chest = path.contains("chest_");
         String wood = path.replace("_chest_boat", "").replace("_chest_raft", "").replace("_boat", "").replace("_raft", "");
         String modelPath = (chest ? "chest_boat/" : "boat/") + wood;
         ModelLayerLocation location = new ModelLayerLocation(Identifier.withDefaultNamespace(modelPath), "main");
         ModelPart root = client.getEntityModels().bakeLayer(location);
         AbstractBoatModel boat = wood.equals("bamboo") ? new RaftModel(root) : new BoatModel(root);
         boat.setupAnim(new BoatRenderState());
         root.getChild("left_paddle").xRot = root.getChild("right_paddle").xRot = (float) (-Math.PI / 8);
         root.getChild("left_paddle").yRot = (float) (Math.PI / 4);
         root.getChild("right_paddle").yRot = (float) (Math.PI * 3.0 / 4.0);
         return new NativeMatModels.Mesh(root, Identifier.withDefaultNamespace("textures/entity/" + modelPath + ".png"), true, null, 0);
      }

      Item content = switch (path) {
         case "chest_minecart" -> Items.CHEST;
         case "hopper_minecart" -> Items.HOPPER;
         case "furnace_minecart" -> Items.FURNACE;
         case "tnt_minecart" -> Items.TNT;
         case "command_block_minecart" -> Items.COMMAND_BLOCK;
         default -> null;
      };
      ItemStackRenderState contents = null;
      if (content != null) {
         contents = new ItemStackRenderState();
         client.getItemModelResolver().updateForTopItem(contents, new ItemStack(content), ItemDisplayContext.NONE, client.level, client.player, 0);
      }

      return new NativeMatModels.Mesh(
         new MinecartModel(client.getEntityModels().bakeLayer(ModelLayers.MINECART)).root(),
         Identifier.withDefaultNamespace("textures/entity/minecart/minecart.png"),
         true,
         contents,
         path.equals("hopper_minecart") ? 1 : (path.equals("chest_minecart") ? 8 : 6)
      );
   }

   @Environment(EnvType.CLIENT)
   private static final class Mesh implements SpecialModelRenderer<Boolean> {
      final ModelPart root;
      final Identifier texture;
      final boolean flip;
      final ItemStackRenderState contents;
      final int offset;
      final Vector3fc[] extents;

      Mesh(ModelPart root, Identifier texture, boolean flip, ItemStackRenderState contents, int offset) {
         this.root = root;
         this.texture = texture;
         this.flip = flip;
         this.contents = contents;
         this.offset = offset;
         ArrayList<Vector3fc> out = new ArrayList<>();
         PoseStack pose = new PoseStack();
         if (flip) {
            pose.scale(-1.0F, -1.0F, 1.0F);
         }

         root.getExtentsForGui(pose, p -> out.add(new Vector3f(p)));
         if (contents != null) {
            Matrix4f transform = new Matrix4f().scaling(0.75F).translate(0.0F, 0.5F + (offset - 8) / 16.0F, 0.0F).rotateY((float) (Math.PI / 2));
            contents.visitExtents(p -> out.add(new Vector3f(p).mulPosition(transform)));
         }

         this.extents = out.toArray(Vector3fc[]::new);
      }

      void append(ItemStackRenderState model, boolean foil) {
         LayerRenderState layer = model.newLayer();
         layer.setupSpecialModel(this, foil);
         layer.setUsesBlockLight(true);
         layer.setExtents(() -> this.extents);
      }

      public void submit(
         Boolean foil, PoseStack pose, SubmitNodeCollector collector, int light, int overlay, boolean glint, int outline
      ) {
         pose.pushPose();
         if (this.flip) {
            pose.scale(-1.0F, -1.0F, 1.0F);
         }

         RenderType type = Boolean.TRUE.equals(foil) ? RenderTypes.entitySolidGlint(this.texture) : RenderTypes.entityCutout(this.texture);
         collector.submitModelPart(this.root, pose, type, light, overlay, null, -1, outline);
         pose.popPose();
         if (this.contents != null) {
            pose.pushPose();
            pose.scale(0.75F, 0.75F, 0.75F);
            pose.translate(0.0F, 0.5F + (this.offset - 8) / 16.0F, 0.0F);
            pose.rotate(Axis.YP.rotationDegrees(90.0F));
            this.contents.submit(pose, collector, light, overlay, outline);
            pose.popPose();
         }
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

   @Environment(EnvType.CLIENT)
   enum Pose {
      ORIGINAL,
      UPRIGHT,
      FLAT;
   }

   @Environment(EnvType.CLIENT)
   record Presentation(NativeMatModels.Pose pose, Identifier texture) {
   }

   @Environment(EnvType.CLIENT)
   private static final class SpecialProbe extends SubmitNodeStorage {
      void clear() {
         this.drainPhases(phase -> phase.sortInto((node, ordered) -> {}));
      }

      boolean found;

      @Override
      public SubmitNodeCollection order(int order) {
         this.found = true;
         return super.order(order);
      }

      @Override
      public void submitCustomGeometry(PoseStack pose, RenderType type, CustomGeometryRenderer renderer) {
         this.found = true;
      }

      @Override
      public <S> void submitModel(
         Model<? super S> model,
         S state,
         PoseStack pose,
         RenderType type,
         int light,
         int overlay,
         int color,
         UvMapping sprite,
         int outline
      ) {
         this.found = true;
      }
   }
}
