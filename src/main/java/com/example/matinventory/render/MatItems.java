package com.example.matinventory.render;

import com.example.matinventory.MatInventoryClient;
import com.example.matinventory.animation.CarriedMotion;
import com.example.matinventory.animation.MatAnimation;
import com.example.matinventory.arrangement.ItemCategoryClassifier;
import com.example.matinventory.arrangement.ItemFootprint;
import com.example.matinventory.arrangement.ItemPresentationData;
import com.example.matinventory.capacity.EquipmentKinds;
import com.example.matinventory.capacity.MaterialKinds;
import com.example.matinventory.capacity.ModItemSemantics;
import com.example.matinventory.capacity.ModToolRules;
import com.example.matinventory.capacity.ToolKinds;
import com.example.matinventory.config.InventoryScales;
import com.example.matinventory.config.MatConfig;
import com.example.matinventory.core.BlockPile;
import com.example.matinventory.core.ConsumablePile;
import com.example.matinventory.core.DecorativeBlockPile;
import com.example.matinventory.core.ItemSizeResolver;
import com.example.matinventory.core.MaterialPile;
import com.example.matinventory.core.ModToolSize;
import com.example.matinventory.core.VisualItemKinds;
import com.example.matinventory.layout.MatLayout;
import com.example.matinventory.scene.MatCamera;
import com.example.matinventory.scene.MatWorldContext;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.item.ItemStackRenderState.FoilType;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

@Environment(EnvType.CLIENT)
public final class MatItems {
   private static final int CARRIED = 511;
   private static final int BOOT_REFERENCE = 510;
   private static final int SWORD_REFERENCE = 509;
   private static final int STICK_REFERENCE = 508;
   private static final int FRAME_REFERENCE = 507;
   private final ItemStack referenceFrame = new ItemStack(Items.ITEM_FRAME);
   private final ItemStack referenceStick = new ItemStack(Items.STICK);
   private final ItemStack referenceSword = new ItemStack(Items.IRON_SWORD);
   private final ItemStack referenceBoot = new ItemStack(Items.DIAMOND_BOOTS);
   private final ItemStackRenderState[] models = new ItemStackRenderState[512];
   private final ItemStack[] previous = new ItemStack[512];
   private final ItemFootprint[] footprints = new ItemFootprint[512];
   private final MatSceneState frame = new MatSceneState();
   private final CarriedMotion carriedMotion = new CarriedMotion();
   private ItemStack pickedPresentation = ItemStack.EMPTY;
   private float pickedScale = 1.0F;
   private List<Integer> movingToolSlots = List.of();
   private int movingToolOrigin = -1;
   public final MatWorldContext world = new MatWorldContext();
   private final float[] scales = new float[512];
   private final float[] cx = new float[512];
   private final float[] cy = new float[512];
   private final float[] cz = new float[512];
   private final float[] heights = new float[512];
   private final float[] pitches = new float[512];
   private final float[] rolls = new float[512];
   private final AABB[] modelBounds = new AABB[512];
   private final boolean[] customGeometry = new boolean[512];
   private final MatItems.SpriteShape[] labelShapes = new MatItems.SpriteShape[512];
   private final boolean[] geometryPending = new boolean[512];
   private final boolean[] staleModels = new boolean[512];
   private final AABB[] declaredBounds = new AABB[512];
   private final long[] geometryRetryAt = new long[512];
   private final Matrix4f modelTransform = new Matrix4f();
   private final Matrix4f renderedTransform = new Matrix4f();
   private final Matrix4f pickTransform = new Matrix4f();
   private final Vector3f boundsCorner = new Vector3f();
   private final Vector3f beltEye = new Vector3f();
   private final ModelGeometry geometry = new ModelGeometry();
   private final Set<String> unmeasurable = new HashSet<>();
   private final boolean[] materialPose = new boolean[512];
   private final boolean[] resolvedStorage = new boolean[512];
   private final boolean[] resolvedEquipped = new boolean[512];
   private final ItemFootprint[] normalizedPrimary = new ItemFootprint[512];
   private final float[] normalizedTarget = new float[512];
   private final MatItems.ProjectionEntry[] projections = new MatItems.ProjectionEntry[512];
   private final float[] projectionGeometry = new float[23];
   private final Matrix4f projectionView = new Matrix4f();
   private final Matrix4f projectionMatrix = new Matrix4f();
   private int projectionWidth = -1;
   private int projectionHeight = -1;
   private long projectionRevision;
   public long projectionHits;
   public long projectionMisses;
   private final boolean[] wingsPose = new boolean[512];
   private final boolean[] groundedPose = new boolean[512];
   private NativeElytraModel wings;
   private NativeMatModels nativeModels = new NativeMatModels();
   private final boolean[] nativeGeometry = new boolean[512];
   private final Identifier[] nativeTextures = new Identifier[512];
   private final float[] nativeEnlargement = new float[512];
   private final boolean[] decorativeBlocks = new boolean[512];
   private static final Map<Identifier, MatItems.SpriteShape> sprites = new HashMap<>();
   private static final Map<String, MatItems.SpriteShape> composites = new HashMap<>();
   private static int shapeGeneration = -1;
   private static final Map<AbstractContainerMenu, MatItems> pooled = new WeakHashMap<>();
   private final RandomSource random = RandomSource.create(0L);
   private int generation = -1;
   private int dataGeneration = -1;
   private int tagGeneration = -1;
   private static volatile int semanticGeneration;
   private long modelRevision;
   private long animatedRevision;
   private final List<MatItems.AppliedScale> appliedScales = new ArrayList<>();
   private final PoseStack probePose = new PoseStack();
   private final MatItems.SpriteProbe probe = new MatItems.SpriteProbe();
   private int preparationCursor;
   private int preloadCursor;

   public void pickupToolPile(List<MatLayout.Place> group, int origin) {
      this.movingToolSlots = group.stream().map(MatLayout.Place::slot).toList();
      this.movingToolOrigin = origin;
   }

   public void pickup(int slot) {
      this.pickedPresentation = ItemStack.EMPTY;
      this.pickedScale = 1.0F;

      for (int i = 0; i < this.frame.itemCount; i++) {
         MatSceneState.Item v = this.frame.items()[i];
         if (v.slot == slot) {
            MatLayout.Place place = this.frame.layout().place(slot);
            if (place != null && place.group() == MatLayout.Group.SUPPLIES && this.previous[slot] != null) {
               this.pickedPresentation = this.previous[slot].copy();
               this.pickedScale = place.sizeFactor / InventoryScales.factor(this.pickedPresentation);
            }

            this.carriedMotion.begin(v.x, v.y, v.z, v.yaw, v.pitch, v.roll, v.scale);
            return;
         }
      }

      this.carriedMotion.reset();
   }

   public static MatItems forMenu(AbstractContainerMenu menu) {
      return pooled.computeIfAbsent(menu, k -> new MatItems());
   }

   public static void clearMenus() {
      pooled.clear();
   }

   public static void invalidateTags() {
      semanticGeneration++;
   }

   private void animationRevision(ItemStackRenderState state) {
      if (state.isAnimated()) {
         this.animatedRevision = System.nanoTime() / 50000000L;
      }
   }

   private void restoreUserScales() {
      for (MatItems.AppliedScale a : this.appliedScales) {
         a.place.sizeFactor = a.size;
         a.place.halfWidth = a.width;
         a.place.halfDepth = a.depth;
         a.place.radius = a.radius;
      }

      this.appliedScales.clear();
   }

   public MatItems() {
      for (int i = 0; i < this.models.length; i++) {
         this.models[i] = new ItemStackRenderState();
      }
   }

   private static List<MatItems.Pixel> hull(List<MatItems.Pixel> pixels, float pad) {
      ArrayList<MatItems.Pixel> points = new ArrayList<>(pixels.size() * 4);

      for (MatItems.Pixel p : pixels) {
         for (int c = 0; c < 4; c++) {
            points.add(new MatItems.Pixel(p.x + ((c & 1) == 0 ? -pad : pad), p.y + ((c & 2) == 0 ? -pad : pad)));
         }
      }

      List<MatItems.Pixel> sorted = points.stream()
         .distinct()
         .sorted(Comparator.comparingDouble(MatItems.Pixel::x).thenComparingDouble(MatItems.Pixel::y))
         .toList();
      if (sorted.size() < 3) {
         return sorted;
      }

      ArrayList<MatItems.Pixel> result = new ArrayList<>();

      for (MatItems.Pixel p : sorted) {
         while (result.size() > 1 && turn(result.get(result.size() - 2), result.getLast(), p) <= 0.0F) {
            result.removeLast();
         }

         result.add(p);
      }

      int lower = result.size() + 1;

      for (int i = sorted.size() - 2; i >= 0; i--) {
         MatItems.Pixel p = sorted.get(i);

         while (result.size() >= lower && turn(result.get(result.size() - 2), result.getLast(), p) <= 0.0F) {
            result.removeLast();
         }

         result.add(p);
      }

      result.removeLast();
      return List.copyOf(result);
   }

   private static float turn(MatItems.Pixel a, MatItems.Pixel b, MatItems.Pixel c) {
      return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
   }

   private MatItems.SpriteShape shape(ItemStackRenderState model) {
      this.probe.sprites.clear();
      this.probe.clear();
      this.probePose.setIdentity();
      model.submit(this.probePose, this.probe, 15728880, 0, 0);
      if (this.probe.sprites.isEmpty()) {
         Material.Baked particle = model.pickParticleMaterial(this.random);
         return particle == null ? null : this.readShape(particle.sprite());
      }

      String key = this.probe.sprites.stream().map(s -> s.contents().name().toString()).sorted().collect(Collectors.joining("|"));
      if (composites.containsKey(key)) {
         return composites.get(key);
      }

      ArrayList<MatItems.Pixel> all = new ArrayList<>();
      float padding = 0.0F;

      for (TextureAtlasSprite sprite : this.probe.sprites) {
         MatItems.SpriteShape shape = this.readShape(sprite);
         if (shape != null) {
            all.addAll(shape.pixels);
            padding = Math.max(padding, shape.pixelSize);
         }
      }

      if (all.isEmpty()) {
         composites.put(key, null);
         return null;
      }

      float sx = 0.0F;
      float sy = 0.0F;

      for (MatItems.Pixel p : all) {
         sx += p.x;
         sy += p.y;
      }

      sx /= all.size();
      sy /= all.size();
      float xx = 0.0F;
      float yy = 0.0F;
      float xy = 0.0F;

      for (MatItems.Pixel p : all) {
         float x = p.x - sx;
         float y = p.y - sy;
         xx += x * x;
         yy += y * y;
         xy += x * y;
      }

      float axis = 90.0F - (float)Math.toDegrees(0.5 * Math.atan2(2.0F * xy, xx - yy));
      MatItems.SpriteShape result = new MatItems.SpriteShape(List.copyOf(all), axis, padding, hull(all, padding));
      composites.put(key, result);
      return result;
   }

   private MatItems.SpriteShape readShape(TextureAtlasSprite sprite) {
      Identifier id = sprite.contents().name();
      if (sprites.containsKey(id)) {
         return sprites.get(id);
      }

      MatItems.SpriteShape shape = null;

      try {
         int w = sprite.contents().width();
         int h = sprite.contents().height();
         ArrayList<MatItems.Pixel> pixels = new ArrayList<>();
         float sx = 0.0F;
         float sy = 0.0F;

         for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
               if (!sprite.contents().isTransparent(0, x, y)) {
                  MatItems.Pixel p = new MatItems.Pixel((x + 0.5F) / w - 0.5F, 0.5F - (y + 0.5F) / h);
                  pixels.add(p);
                  sx += p.x;
                  sy += p.y;
               }
            }
         }

         if (!pixels.isEmpty()) {
            sx /= pixels.size();
            sy /= pixels.size();
            float xx = 0.0F;
            float yy = 0.0F;
            float xy = 0.0F;

            for (MatItems.Pixel p : pixels) {
               float x = p.x - sx;
               float y = p.y - sy;
               xx += x * x;
               yy += y * y;
               xy += x * y;
            }

            float axis = (float)Math.toDegrees(0.5 * Math.atan2(2.0F * xy, xx - yy));
            shape = new MatItems.SpriteShape(List.copyOf(pixels), 90.0F - axis, 0.5F / w, hull(pixels, 0.5F / w));
         }
      } catch (Exception var16) {
      }

      sprites.put(id, shape);
      return shape;
   }

   private void resolve(int slot, ItemStack stack, boolean storage, boolean equipped) {
      ItemStackRenderState model = this.models[slot];
      if (this.previous[slot] == null
         || this.resolvedStorage[slot] != storage
         || this.resolvedEquipped[slot] != equipped
         || this.staleModels[slot]
         || this.geometryPending[slot]
         || model.isAnimated()
         || !ItemStack.matches(this.previous[slot], stack)) {
         this.resolvedStorage[slot] = storage;
         this.resolvedEquipped[slot] = equipped;
         boolean external = ModItemSemantics.external(stack);
         boolean material = storage && MaterialKinds.isMaterial(stack);
         boolean nativeWings = (storage || equipped) && stack.is(Items.ELYTRA);
         boolean grounded = nativeWings || stack.is(Items.SHIELD) && (storage || equipped);
         boolean changed = this.previous[slot] == null
            || !ItemStack.matches(this.previous[slot], stack)
            || this.materialPose[slot] != material
            || this.wingsPose[slot] != nativeWings
            || this.groundedPose[slot] != grounded;
         boolean refresh = this.staleModels[slot];
         boolean retry = this.geometryPending[slot] && System.nanoTime() >= this.geometryRetryAt[slot];
         if (changed || refresh || model.isAnimated() || retry) {
            Minecraft client = Minecraft.getInstance();
            boolean externalObject = external && !(stack.getItem() instanceof BlockItem);
            ModItemSemantics.Role externalRole = ModItemSemantics.role(stack);
            boolean authoredGround = externalObject && (externalRole == ModItemSemantics.Role.AMMUNITION || externalRole == ModItemSemantics.Role.ATTACHMENT);
            client.getItemModelResolver()
               .updateForTopItem(
                  model, stack, externalObject && !authoredGround ? ItemDisplayContext.NONE : ItemDisplayContext.GROUND, client.level, client.player, slot
               );
            this.staleModels[slot] = false;
            if (!changed && !external) {
               if (nativeWings) {
                  if (this.wings == null) {
                     this.wings = new NativeElytraModel();
                  }

                  this.wings.resolve(model, stack);
               } else if (this.nativeGeometry[slot]) {
                  this.nativeModels.resolve(model, stack, slot);
               }
            } else {
               if (nativeWings) {
                  if (this.wings == null) {
                     this.wings = new NativeElytraModel();
                  }

                  this.wings.resolve(model, stack);
               }

               NativeMatModels.Presentation nativeModel = nativeWings
                  ? new NativeMatModels.Presentation(NativeMatModels.Pose.ORIGINAL, NativeElytraModel.TEXTURE)
                  : this.nativeModels.resolve(model, stack, slot);
               this.nativeGeometry[slot] = nativeModel.pose() != NativeMatModels.Pose.ORIGINAL;
               this.nativeTextures[slot] = nativeModel.texture();
               if (changed) {
                  this.previous[slot] = stack.copy();
                  this.materialPose[slot] = material;
                  this.wingsPose[slot] = nativeWings;
                  this.groundedPose[slot] = grounded;
               }

               ItemCategoryClassifier.Classification classification = ItemCategoryClassifier.classify(stack);
               ItemCategoryClassifier.Category category = classification.category();
               if ("brush".equals(classification.subtype()) || "shears".equals(classification.subtype())) {
                  category = ItemCategoryClassifier.Category.OTHER;
               }

               ItemPresentationData.Size size = ItemPresentationData.size(stack);
               AABB box = model.getModelBoundingBox();
               if (!changed && this.footprints[slot] != null && (!refresh || !external) && !retry && Objects.equals(box, this.declaredBounds[slot])) {
                  this.animationRevision(model);
               } else {
                  this.declaredBounds[slot] = box;
                  this.geometryPending[slot] = false;
                  if (external || this.nativeGeometry[slot] || !ModelGeometry.valid(box)) {
                     try {
                        AABB measured = this.geometry.measure(model);
                        this.customGeometry[slot] = this.geometry.special;
                        if (measured != null) {
                           box = measured;
                        } else {
                           this.geometryPending[slot] = external && !ModelGeometry.valid(box);
                        }
                     } catch (RuntimeException ex) {
                        this.geometryPending[slot] = external;
                        if (this.unmeasurable.add(ItemPresentationData.id(stack))) {
                           MatInventoryClient.LOG.warn("Cannot measure custom item {}; using safe declared bounds", ItemPresentationData.id(stack), ex);
                        }
                     }
                  }

                  if (this.geometryPending[slot]) {
                     this.geometryRetryAt[slot] = System.nanoTime() + 250000000L;
                  }

                  if (this.geometryPending[slot] && !changed && this.modelBounds[slot] != null) {
                     box = this.modelBounds[slot];
                  }

                  box = ModelGeometry.sanitize(box);
                  if (changed || !box.equals(this.modelBounds[slot])) {
                     this.modelRevision++;
                  }

                  this.animationRevision(model);
                  this.modelBounds[slot] = box;
                  float dx = (float)box.getXsize();
                  float dy = (float)box.getYsize();
                  float dz = (float)box.getZsize();
                  if (Float.isFinite(dx + dy + dz) && !(Math.max(dx, Math.max(dy, dz)) < 0.001F)) {
                     this.cx[slot] = (float)((box.minX + box.maxX) * 0.5);
                     this.cy[slot] = (float)((box.minY + box.maxY) * 0.5);
                     this.cz[slot] = (float)((box.minZ + box.maxZ) * 0.5);
                  } else {
                     dz = 0.5F;
                     dy = 0.5F;
                     dx = 0.5F;
                     this.cx[slot] = this.cy[slot] = this.cz[slot] = 0.0F;
                  }

                  boolean decorative = category == ItemCategoryClassifier.Category.BLOCKS
                     && (VisualItemKinds.decorative(stack) || VisualItemKinds.statue(stack));
                  this.decorativeBlocks[slot] = decorative;
                  boolean flat = !this.nativeGeometry[slot] && dz < Math.max(dx, dy) * 0.36F;
                  boolean lay = this.nativeGeometry[slot]
                     ? nativeModel.pose() == NativeMatModels.Pose.FLAT
                     : grounded
                        || flat
                        || category == ItemCategoryClassifier.Category.LONG_TOOLS && dy > Math.max(dx, dz) * 1.2F
                        || category == ItemCategoryClassifier.Category.EQUIPMENT && dy > dz * 1.4F;
                  float fw = dx;
                  float fd = lay ? dy : dz;
                  float fh = lay ? dz : dy;
                  float modelYaw = 0.0F;
                  this.pitches[slot] = nativeWings ? -90.0F : (lay ? (!this.nativeGeometry[slot] && !grounded ? -90 : 90) : 0.0F);
                  this.rolls[slot] = 0.0F;
                  if (decorative && nativeModel.pose() != NativeMatModels.Pose.FLAT) {
                     this.pitches[slot] = 0.0F;
                     fw = dx;
                     fd = dz;
                     fh = dy;
                     flat = false;
                  }

                  if (decorative && VisualItemKinds.bundle(stack)) {
                     this.pitches[slot] = 90.0F;
                     this.rolls[slot] = 0.0F;
                     fw = dx;
                     fd = dy;
                     fh = dz;
                     flat = false;
                  }

                  if (VisualItemKinds.torch(stack) || VisualItemKinds.plant(stack)) {
                     this.pitches[slot] = -90.0F;
                     this.rolls[slot] = 0.0F;
                     fw = dx;
                     fd = dy;
                     fh = dz;
                     flat = true;
                  }

                  boolean flexible = ItemCategoryClassifier.flexible(stack);
                  boolean customRest = externalObject && !this.nativeGeometry[slot];
                  if (authoredGround) {
                     this.pitches[slot] = 0.0F;
                     this.rolls[slot] = 0.0F;
                     fw = dx;
                     fd = dz;
                     fh = dy;
                     flat = false;
                  } else if (customRest) {
                     if (dx < dy && dx < dz) {
                        this.pitches[slot] = 0.0F;
                        this.rolls[slot] = 90.0F;
                        fw = dy;
                        fd = dz;
                        fh = dx;
                     } else if (dz < dy) {
                        this.pitches[slot] = -90.0F;
                        fw = dx;
                        fd = dy;
                        fh = dz;
                     } else {
                        this.pitches[slot] = 0.0F;
                        fw = dx;
                        fd = dz;
                        fh = dy;
                     }

                     if (fw > fd) {
                        modelYaw = 90.0F;
                        float swap = fw;
                        fw = fd;
                        fd = swap;
                     }

                     flat = false;
                  }

                  this.labelShapes[slot] = null;
                  if (flat) {
                     MatItems.SpriteShape shape = this.shape(model);
                     if (shape != null) {
                        this.labelShapes[slot] = shape;
                        if (category == ItemCategoryClassifier.Category.LONG_TOOLS || VisualItemKinds.torch(stack) || material && MaterialKinds.rod(stack)) {
                           modelYaw = shape.axis;
                        }

                        float co = (float)Math.cos(Math.toRadians(modelYaw));
                        float si = (float)Math.sin(Math.toRadians(modelYaw));
                        float minX = Float.POSITIVE_INFINITY;
                        float minZ = minX;
                        float maxX = Float.NEGATIVE_INFINITY;
                        float maxZ = maxX;
                        float pixel = shape.pixelSize;

                        for (MatItems.Pixel p : shape.pixels) {
                           for (int corner = 0; corner < 4; corner++) {
                              float x = (p.x + ((corner & 1) == 0 ? -pixel : pixel)) * dx;
                              float z = -(p.y + ((corner & 2) == 0 ? -pixel : pixel)) * dy;
                              float rx = x * co + z * si;
                              float rz = -x * si + z * co;
                              minX = Math.min(minX, rx);
                              maxX = Math.max(maxX, rx);
                              minZ = Math.min(minZ, rz);
                              maxZ = Math.max(maxZ, rz);
                           }
                        }

                        fw = maxX - minX;
                        fd = maxZ - minZ;
                        float mx = (minX + maxX) * 0.5F;
                        float mz = (minZ + maxZ) * 0.5F;
                        this.cx[slot] = this.cx[slot] + (mx * co - mz * si);
                        this.cy[slot] = this.cy[slot] - (mx * si + mz * co);
                     }
                  }

                  float cm;
                  if (size != null) {
                     cm = size.cm();
                  } else {
                     cm = switch (category) {
                        case LONG_TOOLS -> 24.0F;
                        case FOOD -> 7.0F;
                        case BLOCKS -> 11.0F;
                        case EQUIPMENT -> 18.0F;
                        case RESOURCES -> {
                           if (!classification.subtype().contains("nugget") && !classification.subtype().contains("dust")) {
                              yield 5.0F;
                           }

                           yield 2.5F;
                        }
                        default -> Math.clamp(Math.max(dx, Math.max(dy, dz)) * 32.0F, 3.0F, 70.0F);
                     };
                  }

                  boolean crossbow = "crossbow".equals(classification.subtype());
                  if (crossbow) {
                     ItemPresentationData.Size bowSize = ItemPresentationData.size(new ItemStack(Items.BOW));
                     cm = bowSize == null ? 42.0F : bowSize.cm();
                  }

                  MatConfig config = MatInventoryClient.config;
                  float physical = 24.0F
                     * ItemSizeResolver.resolve(
                        cm, category.name(), classification.subtype(), config.scaleGamma, config.minItemSizeFrac, config.itemSizeMultiplier
                     );

                  physical = Math.min(physical, switch (category) {
                     case LONG_TOOLS -> 4.6F;
                     default -> 1.6F;
                     case BLOCKS -> 1.56F;
                     case EQUIPMENT -> 2.0F;
                     case RESOURCES -> 0.95F;
                  });
                  if (crossbow) {
                     physical *= 0.94F;
                  }

                  if (category == ItemCategoryClassifier.Category.BLOCKS) {
                     physical *= 0.96F;
                  }

                  boolean tinyMaterial = material
                     && (
                        "tiny".equals(classification.subtype())
                           || classification.subtype().contains("nugget")
                           || stack.getItem() instanceof BlockItem b && b.getBlock() instanceof ButtonBlock
                     );
                  if (material) {
                     physical = tinyMaterial ? 0.64F : (MaterialKinds.rod(stack) && fw <= fd * 0.33F ? 1.32F : 1.16F);
                  }

                  float originalPhysical = physical;
                  boolean modelledObject = this.nativeGeometry[slot]
                     && (
                        !(stack.getItem() instanceof BlockItem) || Math.abs(dx - 1.0F) > 0.001F || Math.abs(dy - 1.0F) > 0.001F || Math.abs(dz - 1.0F) > 0.001F
                     );
                  if (modelledObject) {
                     physical *= 1.3F;
                     if (material) {
                        physical = Math.min(physical, 1.38F);
                     }

                     if (category == ItemCategoryClassifier.Category.LONG_TOOLS) {
                        physical = Math.min(physical, 5.2F);
                     }
                  }

                  if (storage
                     && this.nativeGeometry[slot]
                     && category == ItemCategoryClassifier.Category.BLOCKS
                     && stack.getItem() instanceof BlockItem block
                     && (block.getBlock() instanceof BedBlock || block.getBlock() instanceof DoorBlock)) {
                     physical = 2.8846154F;
                  }

                  if (category == ItemCategoryClassifier.Category.BLOCKS && VisualItemKinds.torch(stack)) {
                     physical = Math.max(physical, 2.8F * config.itemSizeMultiplier);
                  }

                  this.nativeEnlargement[slot] = physical / originalPhysical;
                  if (tinyMaterial) {
                     physical = 0.64F * (storage ? 1.05F : 1.0F);
                     this.nativeEnlargement[slot] = 1.0F;
                  } else if (storage && (material || category == ItemCategoryClassifier.Category.FOOD)) {
                     physical *= 1.05F;
                  }

                  if (flexible && size == null) {
                     physical = Math.clamp(Math.max(dx, Math.max(dy, dz)) * 2.0F, 0.9F, 4.6F) * config.itemSizeMultiplier;
                  }

                  if (external && size == null && (flexible || category == ItemCategoryClassifier.Category.LONG_TOOLS)) {
                     ModItemSemantics.Role role = ModItemSemantics.role(stack);
                     if (role == ModItemSemantics.Role.ATTACHMENT) {
                        physical = 0.85F * config.itemSizeMultiplier;
                     } else if (role == ModItemSemantics.Role.AMMUNITION) {
                        physical = 0.5F * config.itemSizeMultiplier;
                     }
                  }

                  boolean externalTool = ModToolRules.applies(stack);
                  if (externalTool && size == null) {
                     physical = ModToolSize.length(fw, fh, fd, config.itemSizeMultiplier);
                  }

                  this.scales[slot] = physical / Math.max(0.001F, Math.max(Math.max(fw, fd), !this.nativeGeometry[slot] && !decorative ? 0.0F : fh));
                  if (category == ItemCategoryClassifier.Category.LONG_TOOLS && !crossbow && !externalTool) {
                     this.scales[slot] = Math.min(this.scales[slot], 2.6F / Math.max(0.001F, fw));
                  }

                  if ("mace".equals(classification.subtype()) || "brush".equals(classification.subtype()) || "shears".equals(classification.subtype())) {
                     this.scales[slot] = this.scales[slot] * 1.1F;
                  }

                  if (storage && stack.is(Items.TRIDENT)) {
                     this.scales[slot] = this.scales[slot] * 1.2F;
                  }

                  if (storage && !externalTool && stack.is(ItemTags.SWORDS)) {
                     this.scales[slot] = this.scales[slot] * 1.06F;
                  }

                  this.heights[slot] = fh * this.scales[slot];
                  float preferred = size != null && size.angle() != null ? size.angle() : 0.0F;
                  this.footprints[slot] = new ItemFootprint(
                     fw * this.scales[slot], fd * this.scales[slot], cm, modelYaw, preferred, classification, ItemPresentationData.id(stack), stack.getCount()
                  );
               }
            }
         }
      }
   }

   public void arrange(AbstractContainerMenu menu, MatLayout layout, MatAnimation animation, boolean freeze) {
      this.restoreUserScales();
      layout.syncSlots(menu);
      this.refreshResources();

      for (MatLayout.Place place : layout.places()) {
         int slot = place.slot();
         ItemStack stack = menu.getSlot(slot).getItem();
         if (stack.isEmpty()) {
            this.previous[slot] = null;
            this.footprints[slot] = null;
         } else {
            this.resolve(
               slot,
               stack,
               place.group() == MatLayout.Group.SUPPLIES,
               place.group() == MatLayout.Group.EQUIPMENT || EquipmentKinds.equipped(menu.getSlot(slot))
            );
         }
      }

      this.normalizePrimaryTools(menu, layout);
      this.normalizeTorches(menu, layout);
      layout.arrange(menu, this.footprints, animation.dt, freeze);
      this.normalizePlayerArmor(menu, layout);

      for (MatLayout.Place p : layout.places()) {
         float factor = InventoryScales.factor(menu.getSlot(p.slot()).getItem());
         if (factor != 1.0F) {
            this.appliedScales.add(new MatItems.AppliedScale(p, p.sizeFactor, p.halfWidth, p.halfDepth, p.radius));
            p.sizeFactor *= factor;
            p.halfWidth *= factor;
            p.halfDepth *= factor;
            p.radius *= factor;
         }
      }

      for (MatLayout.ToolCell cell : layout.toolCells()) {
         this.stackRack(layout, layout.toolsInCell(cell.index()));
      }

      for (MatLayout.EquipmentCell cell : layout.equipmentCells()) {
         if (cell.index() < 5) {
            this.stackRack(layout, layout.equipmentInCell(cell.index()));
         }
      }
   }

   private void fitBelt(MatLayout.Place p, ItemFootprint fp, MatLayout layout, MatCamera camera) {
      int slot = p.slot();
      AABB box = this.modelBounds[slot];
      if (box != null) {
         float halfX = (p.group() == MatLayout.Group.OFFHAND ? layout.offhandWidth() : layout.hotbarCellWidth()) * 0.42F;
         float halfZ = Math.max(0.05F, (layout.hotbarBottom - layout.hotbarTop - 0.5F) * 0.5F);
         float support = MatMesh.equipmentSupport(layout, p.x(), p.z(), halfX, halfZ) - 0.006F;
         this.pickTransform.set(camera.view).invert().getTranslation(this.beltEye);
         this.beltEye.sub(p.x(), support, p.z());
         this.modelTransform
            .rotationY((float)Math.toRadians(p.yaw + fp.modelYaw()))
            .rotateX((float)Math.toRadians(this.pitches[slot]))
            .rotateZ((float)Math.toRadians(this.rolls[slot]))
            .scale(this.scales[slot] * p.sizeFactor)
            .translate(-this.cx[slot], -this.cy[slot], -this.cz[slot]);
         float fit = 1.0F;
         float maxX = 0.0F;
         float maxZ = 0.0F;

         for (int c = 0; c < 8; c++) {
            this.boundsCorner
               .set((float)((c & 1) == 0 ? box.minX : box.maxX), (float)((c & 2) == 0 ? box.minY : box.maxY), (float)((c & 4) == 0 ? box.minZ : box.maxZ))
               .mulPosition(this.modelTransform);
            float x = this.boundsCorner.x;
            float z = this.boundsCorner.z;
            float y = Math.max(0.0F, this.boundsCorner.y + this.heights[slot] * p.sizeFactor * 0.5F);
            maxX = Math.max(maxX, Math.abs(x));
            maxZ = Math.max(maxZ, Math.abs(z));
            fit = Math.min(fit, Math.min(halfX / Math.max(1.0E-4F, Math.abs(x)), halfZ / Math.max(1.0E-4F, Math.abs(z))));
            if (this.beltEye.y > 0.05F) {
               float depth = y / this.beltEye.y;
               fit = Math.min(fit, halfX / Math.max(1.0E-4F, Math.abs(x - this.beltEye.x * depth) + halfX * depth));
               fit = Math.min(fit, halfZ / Math.max(1.0E-4F, Math.abs(z - this.beltEye.z * depth) + halfZ * depth));
            }
         }

         p.sizeFactor *= fit;
         p.halfWidth = maxX * fit;
         p.halfDepth = maxZ * fit;
         p.radius = Math.max(p.halfWidth, p.halfDepth);
      }
   }

   private void refreshResources() {
      MatMaterials.ensureLoaded();
      if (shapeGeneration != MatMaterials.generation) {
         shapeGeneration = MatMaterials.generation;
         sprites.clear();
         composites.clear();
      }

      if (this.generation != MatMaterials.generation || this.dataGeneration != ItemPresentationData.generation || this.tagGeneration != semanticGeneration) {
         Arrays.fill(this.previous, null);
         this.wings = null;
         this.nativeModels = new NativeMatModels();
         this.generation = MatMaterials.generation;
         this.dataGeneration = ItemPresentationData.generation;
         this.tagGeneration = semanticGeneration;
      }
   }

   public void beginOpening() {
      this.restoreUserScales();
      this.preparationCursor = 0;

      for (int i = 0; i < this.models.length; i++) {
         if (this.previous[i] != null) {
            if (ModItemSemantics.external(this.previous[i])) {
               this.models[i] = new ItemStackRenderState();
            } else {
               this.models[i].clear();
            }

            this.staleModels[i] = true;
         }
      }

      this.frame.itemCount = 0;
      this.modelRevision++;
      this.carriedMotion.reset();
   }

   private void resolveCarried(ItemStack stack) {
      this.resolve(511, stack, true, false);
      this.resolve(509, this.referenceSword, true, false);
      ItemFootprint sword = this.footprints[509];
      this.normalizePrimaryTool(511, stack, Math.max(sword.width(), sword.depth()));
      this.normalizeSmallPresentation(511, stack);
   }

   public float separateScale(ItemStack stack) {
      this.resolveCarried(stack);
      return ItemStack.isSameItemSameComponents(stack, this.pickedPresentation) && Float.isFinite(this.pickedScale) && this.pickedScale > 0.0F
         ? this.pickedScale
         : this.footprints[511].storageScale();
   }

   public ItemFootprint separateFootprint(ItemStack stack) {
      float factor = this.separateScale(stack) * InventoryScales.factor(stack);
      ItemFootprint fp = this.footprints[511];
      return new ItemFootprint(fp.width() * factor, fp.depth() * factor, fp.cm(), fp.modelYaw(), fp.preferredYaw(), fp.classification(), fp.id(), fp.count());
   }

   public boolean prepareOpening(AbstractContainerMenu menu, MatLayout layout, MatAnimation animation) {
      layout.syncSlots(menu);
      this.refreshResources();
      long deadline = System.nanoTime() + 3000000L;
      List<MatLayout.Place> places = layout.places();

      while (this.preparationCursor < places.size()) {
         MatLayout.Place place = places.get(this.preparationCursor++);
         int slot = place.slot();
         ItemStack stack = menu.getSlot(slot).getItem();
         if (stack.isEmpty()) {
            this.previous[slot] = null;
            this.footprints[slot] = null;
         } else {
            this.resolve(
               slot,
               stack,
               place.group() == MatLayout.Group.SUPPLIES,
               place.group() == MatLayout.Group.EQUIPMENT || EquipmentKinds.equipped(menu.getSlot(slot))
            );
            if (System.nanoTime() > deadline) {
               return false;
            }
         }
      }

      this.preparationCursor = 0;
      this.arrange(menu, layout, animation, false);
      this.world.fitOpening(layout);
      return true;
   }

   public void preload(AbstractContainerMenu menu) {
      this.refreshResources();
      long deadline = System.nanoTime() + 2000000L;

      for (int visited = 0; visited < menu.slots.size(); visited++) {
         this.preloadCursor = this.preloadCursor % menu.slots.size();
         Slot slot = menu.slots.get(this.preloadCursor);
         this.preloadCursor = (this.preloadCursor + 1) % menu.slots.size();
         if (slot.index < 509 && slot.hasItem()) {
            this.resolve(slot.index, slot.getItem(), slot.index >= 9 && slot.index != 45, slot.index >= 5 && slot.index <= 8);
            if (System.nanoTime() > deadline) {
               break;
            }
         }
      }
   }

   private void stackRack(MatLayout layout, List<MatLayout.Place> group) {
      ArrayList<MatLayout.Place> layers = new ArrayList<>();

      for (MatLayout.Place p : group) {
         if (p.visible && this.footprints[p.slot()] != null) {
            p.pileLift = 0.0F;
            float support = this.rackSupport(layout, p);
            double angle = Math.toRadians(p.yaw);
            float co = (float)Math.abs(Math.cos(angle));
            float si = (float)Math.abs(Math.sin(angle));
            float halfX = p.halfWidth * co + p.halfDepth * si;
            float halfZ = p.halfDepth * co + p.halfWidth * si;

            for (MatLayout.Place below : layers) {
               double a = Math.toRadians(below.yaw);
               float c = (float)Math.abs(Math.cos(a));
               float s = (float)Math.abs(Math.sin(a));
               float bx = below.halfWidth * c + below.halfDepth * s;
               float bz = below.halfDepth * c + below.halfWidth * s;
               if (!(Math.abs(p.x() - below.x()) >= halfX + bx) && !(Math.abs(p.z() - below.z()) >= halfZ + bz)) {
                  float top = this.rackSupport(layout, below) + below.pileLift + this.heights[below.slot()] * below.sizeFactor;
                  p.pileLift = Math.max(p.pileLift, top + 0.004F - support);
               }
            }

            layers.add(p);
         }
      }
   }

   private float rackSupport(MatLayout layout, MatLayout.Place p) {
      ItemFootprint fp = this.footprints[p.slot()];
      double angle = Math.toRadians(p.yaw);
      float co = (float)Math.abs(Math.cos(angle));
      float si = (float)Math.abs(Math.sin(angle));
      return MatMesh.equipmentSupport(
         layout, p.x(), p.z(), (fp.width() * co + fp.depth() * si) * p.sizeFactor * 0.5F, (fp.depth() * co + fp.width() * si) * p.sizeFactor * 0.5F
      );
   }

   private void normalizeTorches(AbstractContainerMenu menu, MatLayout layout) {
      for (MatLayout.Place place : layout.storagePlaces()) {
         this.normalizeSmallPresentation(place.slot(), menu.getSlot(place.slot()).getItem());
      }
   }

   private void normalizeSmallPresentation(int slot, ItemStack stack) {
      boolean torch = VisualItemKinds.torch(stack);
      boolean flat = VisualItemKinds.flatStack(stack);
      if (torch || flat) {
         int ref = torch ? 508 : 507;
         this.resolve(ref, torch ? this.referenceStick : this.referenceFrame, true, false);
         ItemFootprint reference = this.footprints[ref];
         ItemFootprint fp = this.footprints[slot];
         float length = Math.max(reference.width(), reference.depth());
         if (torch) {
            length *= 0.70710677F;
         }

         if (flat && stack.getItem() instanceof BlockItem block && block.getBlock() instanceof ButtonBlock) {
            length = 0.64F;
         }

         float ratio = length / Math.max(fp.width(), fp.depth());
         if (!(Math.abs(ratio - 1.0F) < 1.0E-5F)) {
            this.scales[slot] = this.scales[slot] * ratio;
            this.heights[slot] = this.heights[slot] * ratio;
            this.footprints[slot] = new ItemFootprint(
               fp.width() * ratio, fp.depth() * ratio, fp.cm(), fp.modelYaw(), fp.preferredYaw(), fp.classification(), fp.id(), fp.count()
            );
         }
      }
   }

   private void normalizePrimaryTools(AbstractContainerMenu menu, MatLayout layout) {
      this.resolve(509, this.referenceSword, true, false);
      ItemFootprint sword = this.footprints[509];
      float target = Math.max(sword.width(), sword.depth());

      for (MatLayout.Place p : layout.storagePlaces()) {
         this.normalizePrimaryTool(p.slot(), menu.getSlot(p.slot()).getItem(), target);
      }
   }

   private void normalizePrimaryTool(int slot, ItemStack stack, float target) {
      ItemFootprint fp = this.footprints[slot];
      if (fp != null && (this.normalizedPrimary[slot] != fp || this.normalizedTarget[slot] != target)) {
         this.normalizedPrimary[slot] = fp;
         this.normalizedTarget[slot] = target;
         if (!ModToolRules.applies(stack)) {
            String kind = ToolKinds.kind(stack);
            if (kind != null && Set.of("axe", "pickaxe", "shovel", "sword", "hoe").contains(kind)) {
               float factor = target / Math.max(fp.width(), fp.depth());
               if (!(Math.abs(factor - 1.0F) < 1.0E-5F)) {
                  this.scales[slot] = this.scales[slot] * factor;
                  this.heights[slot] = this.heights[slot] * factor;
                  this.footprints[slot] = new ItemFootprint(
                     fp.width() * factor, fp.depth() * factor, fp.cm(), fp.modelYaw(), fp.preferredYaw(), fp.classification(), fp.id(), fp.count()
                  );
                  this.normalizedPrimary[slot] = this.footprints[slot];
               }
            }
         }
      }
   }

   private void normalizePlayerArmor(AbstractContainerMenu menu, MatLayout layout) {
      MatLayout.EquipmentCell cell = layout.equipmentCell(layout.equipmentCellFor(this.referenceBoot));
      if (cell != null) {
         this.resolve(510, this.referenceBoot, false, true);
         ItemFootprint boot = this.footprints[510];
         float bootFit = Math.min((cell.x1() - cell.x0() - 0.42F) / boot.width(), (cell.z1() - cell.z0() - 0.54F) / boot.depth());
         float target = this.renderedExtent(510) * bootFit;

         for (MatLayout.Place p : layout.places()) {
            if (p.equipmentCell >= 0 && this.footprints[p.slot()] != null) {
               String kind = EquipmentKinds.kind(menu.getSlot(p.slot()).getItem());
               if ("helmet".equals(kind) || "chestplate".equals(kind) || "leggings".equals(kind) || "boots".equals(kind)) {
                  float gearTarget = menu.getSlot(p.slot()).getItem().is(Items.CARVED_PUMPKIN) ? target * 0.82F : target;
                  ItemFootprint fp = this.footprints[p.slot()];
                  p.sizeFactor = gearTarget / Math.max(0.001F, this.renderedExtent(p.slot()));
                  p.halfWidth = fp.width() * p.sizeFactor * 0.5F;
                  p.halfDepth = fp.depth() * p.sizeFactor * 0.5F;
                  p.radius = Math.max(p.halfWidth, p.halfDepth);
               }
            }
         }
      }
   }

   private float renderedExtent(int slot) {
      AABB b = this.models[slot].getModelBoundingBox();
      double a = Math.toRadians(this.pitches[slot]);
      return (float)Math.max(b.getXsize(), b.getYsize() * Math.abs(Math.sin(a)) + b.getZsize() * Math.abs(Math.cos(a))) * this.scales[slot];
   }

   public MatSceneState empty(MatLayout layout, MatCamera camera, MatAnimation animation) {
      this.world.layout(layout);
      this.world.update();
      this.frame.world = this.world;
      this.frame.begin(layout, camera, 0.0F, animation.elapsed, false, false);
      this.frame.contentRevision = this.modelRevision * 31L + this.animatedRevision;
      this.frame.fold.set(layout, animation.matOpen(), animation.matFall());
      this.frame.openingTime = animation.openingClock();
      this.frame.animationId = animation.cycleId();
      this.frame.dropZones = List.of();
      this.frame.capacityZone = null;
      this.frame.capacityStrength = 0.0F;

      for (MatLayout.Place p : layout.places()) {
         p.pickBoxCount = 0;
         p.countX = p.countY = -1.0F;
      }

      return this.frame;
   }

   public MatSceneState extract(
      AbstractContainerMenu menu,
      MatLayout layout,
      MatCamera camera,
      MatAnimation animation,
      int hovered,
      float glow,
      float flash,
      double mouseX,
      double mouseY
   ) {
      if (!animation.itemsVisible()) {
         return this.empty(layout, camera, animation);
      }

      this.world.layout(layout);
      this.world.update();
      this.frame.world = this.world;
      boolean recipe = menu.getSlot(0).hasItem();
      this.frame.begin(layout, camera, flash, animation.elapsed, !menu.getCarried().isEmpty(), recipe);
      this.frame.fold.set(layout, animation.matOpen(), animation.matFall());
      this.frame.openingTime = animation.openingClock();
      this.frame.animationId = animation.cycleId();
      if (this.projectionWidth != camera.width
         || this.projectionHeight != camera.height
         || !this.projectionView.equals(camera.view)
         || !this.projectionMatrix.equals(camera.projection)) {
         this.projectionWidth = camera.width;
         this.projectionHeight = camera.height;
         this.projectionView.set(camera.view);
         this.projectionMatrix.set(camera.projection);
         this.projectionRevision++;
      }

      boolean onPlane = camera.raycast(mouseX, mouseY);
      MatLayout.Place hoveredPlace = layout.place(hovered);
      int hoveredToolCell = hoveredPlace != null && menu.getSlot(hovered).hasItem() ? hoveredPlace.toolCell : -1;
      this.frame.dropZones = animation.closing()
         ? List.of()
         : layout.dropZones(menu, onPlane ? camera.hit.x : Float.NaN, onPlane ? camera.hit.z : Float.NaN, hoveredToolCell);

      for (MatLayout.Place place : layout.places()) {
         int slot = place.slot();
         float lift = animation.lift(slot, slot == hovered);
         ItemStack stack = menu.getSlot(slot).getItem();
         if (!stack.isEmpty() && place.visible && animation.itemVisible(place.x())) {
            if (this.footprints[slot] == null) {
               this.resolve(slot, stack, place.group() == MatLayout.Group.SUPPLIES, place.group() == MatLayout.Group.EQUIPMENT);
            }

            ItemFootprint fp = this.footprints[slot];
            if (place.group() == MatLayout.Group.HOTBAR || place.group() == MatLayout.Group.OFFHAND) {
               this.fitBelt(place, fp, layout, camera);
            }

            float length = Math.max(fp.width(), fp.depth()) * place.sizeFactor;
            MatSceneState.Item item = this.frame.next();
            item.model = this.models[slot];
            item.slot = slot;
            item.torch = VisualItemKinds.torch(stack);
            item.stackShade = place.stackShade;
            this.frame.foil = this.frame.foil | stack.hasFoil();
            boolean belt = place.group() == MatLayout.Group.HOTBAR || place.group() == MatLayout.Group.OFFHAND;
            boolean rack = place.toolCell >= 0 || place.equipmentCell >= 0 && place.equipmentCell < 5;
            boolean flatRest = !this.nativeGeometry[slot]
               && (Math.abs(this.pitches[slot]) == 90.0F || this.rolls[slot] != 0.0F || ItemCategoryClassifier.flexible(stack))
               && place.group() != MatLayout.Group.RESULT;
            item.flatHover = true;
            item.nativeGeometry = this.nativeGeometry[slot];
            item.nativeTexture = this.nativeTextures[slot];
            float upX = camera.view.m01();
            float upZ = camera.view.m21();
            float upLength = (float)Math.sqrt(upX * upX + upZ * upZ);
            float nudge = place.group() == MatLayout.Group.RESULT ? 0.0F : 0.08F * lift / Math.max(0.001F, upLength);
            item.x = place.x() + upX * nudge;
            item.z = place.z() + upZ * nudge;
            float support = MatMesh.itemSupport(layout, place.x(), place.z(), fp.width() * place.sizeFactor * 0.5F, fp.depth() * place.sizeFactor * 0.5F);
            boolean resting = belt
               || rack
               || flatRest
               || place.equipmentCell >= 5 && this.groundedPose[slot]
               || (this.nativeGeometry[slot] || this.decorativeBlocks[slot]) && place.group() == MatLayout.Group.SUPPLIES;
            item.resting = resting;
            item.contactOffset = !belt && place.vehicleCell < 0 ? 0.0F : -0.006F;
            double yaw = Math.toRadians(place.yaw);
            float co = (float)Math.abs(Math.cos(yaw));
            float si = (float)Math.abs(Math.sin(yaw));
            item.supportX = (fp.width() * co + fp.depth() * si) * place.sizeFactor * 0.5F;
            item.supportZ = (fp.depth() * co + fp.width() * si) * place.sizeFactor * 0.5F;
            if (resting) {
               support = MatMesh.equipmentSupport(layout, place.x(), place.z(), item.supportX, item.supportZ) + item.contactOffset;
            }

            item.supportY = support;
            item.y = support + this.heights[slot] * place.sizeFactor * 0.5F + place.pileLift + animation.dropAt(place.x());
            item.yaw = place.yaw + fp.modelYaw();
            item.pitch = this.pitches[slot];
            item.roll = this.rolls[slot];
            item.hover = lift;
            item.scale = this.scales[slot] * place.sizeFactor;
            item.centerX = this.cx[slot];
            item.centerY = this.cy[slot];
            item.centerZ = this.cz[slot];
            item.pileWidth = fp.width() * place.sizeFactor;
            item.pileDepth = fp.depth() * place.sizeFactor;
            item.pileHeight = this.heights[slot] * place.sizeFactor + 0.025F;
            item.stackStep = 0.0F;
            boolean statuePile = place.group() == MatLayout.Group.SUPPLIES && VisualItemKinds.statue(stack);
            boolean bundlePile = place.group() == MatLayout.Group.SUPPLIES && VisualItemKinds.bundle(stack);
            boolean flatStackPile = place.group() == MatLayout.Group.SUPPLIES && VisualItemKinds.flatStack(stack);
            item.decorativePile = !flatStackPile && place.group() == MatLayout.Group.SUPPLIES && this.decorativeBlocks[slot] && !bundlePile;
            item.blockPile = !flatStackPile
               && !item.decorativePile
               && !bundlePile
               && place.group() == MatLayout.Group.SUPPLIES
               && !rack
               && !statuePile
               && fp.classification().category() == ItemCategoryClassifier.Category.BLOCKS;
            item.largeFlatBlockPile = item.blockPile
               && stack.getItem() instanceof BlockItem block
               && (block.getBlock() instanceof BedBlock || block.getBlock() instanceof DoorBlock);
            item.nativeBlockEnlargement = item.blockPile ? this.nativeEnlargement[slot] : 1.0F;
            if (item.nativeBlockEnlargement > 1.0F) {
               item.pileWidth = item.pileWidth / item.nativeBlockEnlargement;
               item.pileDepth = item.pileDepth / item.nativeBlockEnlargement;
            }

            item.foodPile = place.group() == MatLayout.Group.SUPPLIES && fp.classification().category() == ItemCategoryClassifier.Category.FOOD;
            item.materialPile = place.materialCell >= 0
               || place.detachedPlacement && MaterialKinds.isMaterial(stack) && !ItemCategoryClassifier.flexible(stack)
               || statuePile && !item.decorativePile
               || bundlePile
               || flatStackPile;
            item.materialStyle = flatStackPile ? 0 : (!statuePile && !bundlePile ? place.materialStyle : 2);
            item.pileSeed = place.seed;
            int count = stack.getCount();
            item.copies = place.group() != MatLayout.Group.SUPPLIES ? 1 : (count == 1 ? 1 : (count <= 16 ? 2 : (count <= 48 ? 3 : (count < 64 ? 4 : 5))));
            if (item.decorativePile) {
               item.copies = DecorativeBlockPile.copies(count);
            } else if (item.blockPile) {
               item.copies = BlockPile.copies(count);
            } else if (item.foodPile) {
               item.copies = ConsumablePile.copies(count);
            } else if (item.materialPile) {
               item.copies = MaterialPile.copies(count);
               if (flatStackPile && stack.getItem() instanceof BlockItem block && block.getBlock() instanceof ButtonBlock) {
                  item.copies = Math.min(3, item.copies);
               }
            } else if (this.pitches[slot] == 0.0F) {
               item.copies = Math.min(3, item.copies);
            }

            if (rack) {
               item.copies = Math.min(2, count);
            }

            item.glow = 0.0F;
            item.shadowX = fp.width() * place.sizeFactor * 0.56F;
            item.shadowZ = fp.depth() * place.sizeFactor * 0.56F;
            item.shadowYaw = place.yaw;
            item.shadowRadius = Math.max(item.shadowX, item.shadowZ);
            item.ghost = slot == 0;
            item.alpha = 1.0F;
            item.durability = -1.0F;
            if (item.ghost) {
               item.y += 0.08F;
               item.copies = 1;
            }

            if (place.vehicleCell >= 0) {
               item.copies = 1;
            }

            if (place.freePlacement && count == 1) {
               item.copies = 1;
            }

            if (!this.reuseProjection(slot, place, item, fp, layout)) {
               place.screenRight = place.screenBottom = -Float.MAX_VALUE;
               float screenLeft = Float.MAX_VALUE;
               float screenTop = Float.MAX_VALUE;
               place.labelLeft = place.labelTop = Float.POSITIVE_INFINITY;
               place.labelRight = place.labelBottom = Float.NEGATIVE_INFINITY;
               if (place.vehicleCell >= 0) {
                  item.copies = 1;
               }

               if (place.freePlacement && count == 1) {
                  item.copies = 1;
               }

               place.pickHeight = item.y + item.copyY(item.copies - 1) + this.heights[slot] * place.sizeFactor * item.copyScale(item.copies - 1) * 0.5F;
               place.surfaceHeight = item.y;
               place.solidPick = ModItemSemantics.external(stack)
                  || this.nativeGeometry[slot]
                  || place.equipmentCell >= 5
                  || this.pitches[slot] == 0.0F
                  || item.blockPile
                  || item.foodPile
                  || item.materialPile;
               place.pickX = item.x;
               place.pickY = item.y;
               place.pickZ = item.z;
               place.pickBoxCount = place.solidPick ? item.copies : 0;
               AABB bounds = this.modelBounds[slot];
               Matrix4f transform = this.modelTransform
                  .translation(item.x, item.y, item.z)
                  .rotateY((float)Math.toRadians(item.yaw))
                  .rotateX((float)Math.toRadians(item.pitch + item.hoverTilt()))
                  .rotateZ((float)Math.toRadians(item.roll))
                  .scale(item.scale)
                  .translate(-item.centerX, -item.centerY, -item.centerZ);
               Vector3f corner = this.boundsCorner;

               for (int copy = 0; copy < item.copies; copy++) {
                  Matrix4f rendered = !item.blockPile && !item.foodPile && !item.materialPile && !item.decorativePile
                     ? this.renderedTransform
                        .set(transform)
                        .m30(transform.m30() + item.copyX(copy))
                        .m31(transform.m31() + item.copyY(copy))
                        .m32(transform.m32() + item.copyZ(copy))
                     : copyTransform(this.renderedTransform, item, copy);
                  if (copy < place.pickBoxCount) {
                     if (item.materialPile && this.pitches[slot] != 0.0F) {
                        float copySize = place.sizeFactor * item.copyScale(copy);
                        float w = fp.width() * copySize * 0.5F;
                        float d = fp.depth() * copySize * 0.5F;
                        float h = this.heights[slot] * copySize * 0.5F;
                        Matrix4f pickPose = this.pickTransform
                           .translation(item.x + item.copyX(copy), item.y + item.copyY(copy), item.z + item.copyZ(copy))
                           .rotateY((float)Math.toRadians(place.yaw + item.copyYaw(copy)));
                        place.pickBoxes[copy].set(pickPose, -w, -h, -d, w, h, d);
                     } else {
                        place.pickBoxes[copy]
                           .set(
                              rendered, (float)bounds.minX, (float)bounds.minY, (float)bounds.minZ, (float)bounds.maxX, (float)bounds.maxY, (float)bounds.maxZ
                           );
                     }
                  }

                  for (int c = 0; c < 8; c++) {
                     corner.set(
                           (float)((c & 1) == 0 ? bounds.minX : bounds.maxX),
                           (float)((c & 2) == 0 ? bounds.minY : bounds.maxY),
                           (float)((c & 4) == 0 ? bounds.minZ : bounds.maxZ)
                        )
                        .mulPosition(rendered);
                     Vector4f point = camera.project(corner.x, corner.y, corner.z);
                     place.screenRight = Math.max(place.screenRight, point.x);
                     place.screenBottom = Math.max(place.screenBottom, point.y);
                     screenLeft = Math.min(screenLeft, point.x);
                     screenTop = Math.min(screenTop, point.y);
                     if (this.labelShapes[slot] == null) {
                        this.labelPoint(place, item, camera, corner);
                     }
                  }

                  MatItems.SpriteShape labelShape = this.labelShapes[slot];
                  if (labelShape != null) {
                     for (MatItems.Pixel pixel : labelShape.hull) {
                        for (int side = 0; side < 2; side++) {
                           corner.set(
                                 (float)((bounds.minX + bounds.maxX) * 0.5 + pixel.x * bounds.getXsize()),
                                 (float)((bounds.minY + bounds.maxY) * 0.5 + pixel.y * bounds.getYsize()),
                                 (float)(side == 0 ? bounds.minZ : bounds.maxZ)
                              )
                              .mulPosition(rendered);
                           this.labelPoint(place, item, camera, corner);
                        }
                     }
                  }
               }

               if (place.pickBoxCount > 0) {
                  Vector3f center = place.pickBoxes[place.pickBoxCount - 1].center;
                  place.pickX = center.x;
                  place.pickY = center.y;
                  place.pickZ = center.z;
               }

               place.screenLeft = screenLeft;
               place.screenTop = screenTop;
               item.outlineExpansion = 1.0F
                  + Math.clamp(0.8F / Math.max(1.0F, Math.max(place.screenRight - screenLeft, place.screenBottom - screenTop)), 0.012F, 0.05F);
            } else {
               item.outlineExpansion = 1.0F
                  + Math.clamp(0.8F / Math.max(1.0F, Math.max(place.screenRight - place.screenLeft, place.screenBottom - place.screenTop)), 0.012F, 0.05F);
            }

            if (!this.frame.fold.active()
               && (place.screenRight < -30.0F || place.screenLeft > camera.width + 30 || place.screenBottom < -30.0F || place.screenTop > camera.height + 30)) {
               this.frame.discardLast();
            }
         } else {
            place.pickBoxCount = 0;
         }
      }

      if (!menu.getCarried().isEmpty() && camera.raycast(mouseX, mouseY) && (!animation.closing() || animation.itemVisible(camera.hit.x))) {
         ItemStack stack = menu.getCarried();
         this.resolveCarried(stack);
         ItemFootprint fp = this.footprints[511];
         MatSceneState.Item item = this.frame.next();
         item.model = this.models[511];
         item.slot = 511;
         item.torch = VisualItemKinds.torch(stack);
         item.stackShade = 1.0F;
         item.flatHover = true;
         item.nativeGeometry = this.nativeGeometry[511];
         item.nativeTexture = this.nativeTextures[511];
         item.resting = false;
         item.contactOffset = 0.0F;
         float tx = camera.hit.x;
         float tz = camera.hit.z;
         float factor = this.separateScale(stack) * InventoryScales.factor(stack);
         item.supportY = MatMesh.itemSupport(layout, tx, tz, fp.width() * factor * 0.5F, fp.depth() * factor * 0.5F);
         if (!animation.closing() || !this.carriedMotion.active()) {
            this.carriedMotion
               .follow(
                  tx,
                  item.supportY + 0.16F + this.heights[511] * factor * 0.5F,
                  tz,
                  fp.modelYaw(),
                  this.pitches[511],
                  this.rolls[511],
                  this.scales[511] * factor,
                  animation.dt
               );
         }

         item.x = this.carriedMotion.x;
         item.y = this.carriedMotion.y + (animation.closing() ? animation.dropAt(tx) : 0.0F);
         item.z = this.carriedMotion.z;
         item.pitch = this.carriedMotion.pitch;
         item.roll = this.carriedMotion.roll;
         item.scale = this.carriedMotion.scale;
         item.centerX = this.cx[511];
         item.centerY = this.cy[511];
         item.centerZ = this.cz[511];
         item.yaw = this.carriedMotion.yaw;
         item.copies = MatLayout.toolKind(stack) != null ? Math.min(2, stack.getCount()) : 1;
         item.blockPile = false;
         item.decorativePile = false;
         item.largeFlatBlockPile = false;
         item.foodPile = false;
         item.materialPile = false;
         item.hover = 0.65F;
         item.glow = 0.3F * glow;
         item.shadowX = fp.width() * 0.54F;
         item.shadowZ = fp.depth() * 0.54F;
         item.shadowYaw = 0.0F;
         item.shadowRadius = Math.max(item.shadowX, item.shadowZ);
         item.stackStep = 0.01F;
         item.ghost = false;
         item.alpha = 1.0F;
         item.durability = -1.0F;
         item.pileWidth = fp.width() * factor;
         item.pileDepth = fp.depth() * factor;
         item.pileHeight = this.heights[511] * factor + 0.025F;
         MatLayout.Place origin = layout.place(this.movingToolOrigin);
         if (origin != null && !this.movingToolSlots.isEmpty()) {
            for (int j = 0; j < this.frame.itemCount; j++) {
               MatSceneState.Item companion = this.frame.items()[j];
               if (companion != item && this.movingToolSlots.contains(companion.slot)) {
                  MatLayout.Place p = layout.place(companion.slot);
                  if (p != null) {
                     companion.x = item.x + p.x() - origin.x();
                     companion.z = item.z + p.z() - origin.z();
                     companion.y = item.y + (this.heights[companion.slot] * p.sizeFactor - this.heights[511] * factor) * 0.5F + p.pileLift;
                     companion.resting = false;
                     companion.hover = 0.0F;
                     companion.glow = 0.0F;
                  }
               }
            }
         }
      } else if (menu.getCarried().isEmpty()) {
         this.carriedMotion.reset();
         this.pickedPresentation = ItemStack.EMPTY;
         this.pickedScale = 1.0F;
         this.movingToolSlots = List.of();
         this.movingToolOrigin = -1;
      }

      this.frame.contentRevision = this.modelRevision * 31L + this.animatedRevision;
      return this.frame;
   }

   private boolean reuseProjection(int slot, MatLayout.Place place, MatSceneState.Item item, ItemFootprint fp, MatLayout layout) {
      float[] g = this.projectionGeometry;
      int n = 0;
      g[n++] = item.x;
      g[n++] = item.y;
      g[n++] = item.z;
      g[n++] = item.yaw;
      g[n++] = item.pitch;
      g[n++] = item.roll;
      g[n++] = item.hoverTilt();
      g[n++] = item.scale;
      g[n++] = item.centerX;
      g[n++] = item.centerY;
      g[n++] = item.centerZ;
      g[n++] = item.pileWidth;
      g[n++] = item.pileDepth;
      g[n++] = item.pileHeight;
      g[n++] = item.shadowYaw;
      g[n++] = item.nativeBlockEnlargement;
      g[n++] = fp.width();
      g[n++] = fp.depth();
      g[n++] = this.heights[slot];
      g[n++] = place.sizeFactor;
      g[n++] = place.x();
      g[n++] = place.z();
      g[n++] = item.copies;
      int flags = (item.blockPile ? 1 : 0)
         | (item.foodPile ? 2 : 0)
         | (item.materialPile ? 4 : 0)
         | (item.largeFlatBlockPile ? 8 : 0)
         | (place.equipmentCell >= 5 ? 16 : 0)
         | (item.decorativePile ? 32 : 0);
      MatItems.ProjectionEntry c = this.projections[slot];
      if (c != null
         && c.layout == layout
         && c.layoutRevision == layout.revision
         && c.camera == this.projectionRevision
         && c.model == this.modelRevision
         && c.bounds == this.modelBounds[slot]
         && c.shape == this.labelShapes[slot]
         && c.seed == item.pileSeed
         && c.style == item.materialStyle
         && c.flags == flags
         && Arrays.equals(c.geometry, g)) {
         this.projectionHits++;
         return true;
      }

      if (c == null) {
         this.projections[slot] = c = new MatItems.ProjectionEntry();
      }

      System.arraycopy(g, 0, c.geometry, 0, n);
      c.layout = layout;
      c.layoutRevision = layout.revision;
      c.camera = this.projectionRevision;
      c.model = this.modelRevision;
      c.bounds = this.modelBounds[slot];
      c.shape = this.labelShapes[slot];
      c.seed = item.pileSeed;
      c.style = item.materialStyle;
      c.flags = flags;
      this.projectionMisses++;
      return false;
   }

   private void labelPoint(MatLayout.Place place, MatSceneState.Item item, MatCamera camera, Vector3f corner) {
      Vector4f rest = camera.project(corner.x + place.x() - item.x, corner.y, corner.z + place.z() - item.z);
      place.labelLeft = Math.min(place.labelLeft, rest.x);
      place.labelTop = Math.min(place.labelTop, rest.y);
      place.labelRight = Math.max(place.labelRight, rest.x);
      place.labelBottom = Math.max(place.labelBottom, rest.y);
   }

   private static Matrix4f copyTransform(Matrix4f out, MatSceneState.Item item, int copy) {
      return out.translation(item.x + item.copyX(copy), item.y + item.copyY(copy), item.z + item.copyZ(copy))
         .rotateY((float)Math.toRadians(item.yaw + item.copyYaw(copy)))
         .rotateX((float)Math.toRadians(item.pitch + item.hoverTilt()))
         .rotateZ((float)Math.toRadians(item.roll))
         .scale(item.scale * item.copyScale(copy))
         .translate(-item.centerX, -item.centerY, -item.centerZ);
   }

   @Environment(EnvType.CLIENT)
   private record AppliedScale(MatLayout.Place place, float size, float width, float depth, float radius) {
   }

   @Environment(EnvType.CLIENT)
   private record Pixel(float x, float y) {
   }

   @Environment(EnvType.CLIENT)
   private static final class ProjectionEntry {
      final float[] geometry = new float[23];
      Object layout;
      Object bounds;
      Object shape;
      int layoutRevision;
      int seed;
      int style;
      int flags;
      long camera;
      long model;
   }

   @Environment(EnvType.CLIENT)
   private static final class SpriteProbe extends SubmitNodeStorage {
      void clear() {
         this.drainPhases(phase -> phase.sortInto((node, ordered) -> {}));
      }

      final Set<TextureAtlasSprite> sprites = new LinkedHashSet<>();

      @Override
      public void submitItem(
         PoseStack pose, ItemDisplayContext context, int light, int overlay, int outline, int[] tints, ItemQuads quads, FoilType foil
      ) {
         for (BakedQuad q : quads.all()) {
            this.sprites.add(q.materialInfo().sprite());
         }
      }
   }

   @Environment(EnvType.CLIENT)
   private record SpriteShape(List<MatItems.Pixel> pixels, float axis, float pixelSize, List<MatItems.Pixel> hull) {
   }
}
