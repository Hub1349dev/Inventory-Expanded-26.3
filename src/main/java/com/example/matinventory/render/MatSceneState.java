package com.example.matinventory.render;

import com.example.matinventory.MatInventoryClient;
import com.example.matinventory.animation.MatFold;
import com.example.matinventory.animation.OpeningMotion;
import com.example.matinventory.arrangement.ItemPresentationData;
import com.example.matinventory.core.BlockPile;
import com.example.matinventory.core.ConsumablePile;
import com.example.matinventory.core.DecorativeBlockPile;
import com.example.matinventory.core.MaterialPile;
import com.example.matinventory.layout.MatLayout;
import com.example.matinventory.mixin.MatGuiScissorAccess;
import com.example.matinventory.scene.MatCamera;
import com.example.matinventory.scene.MatWorldContext;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public final class MatSceneState implements PictureInPictureRenderState {
   private int width;
   private int height;
   private ScreenRectangle bounds;
   private final Matrix3x2f guiPose = new Matrix3x2f();
   private ScreenRectangle guiClip;
   private final Matrix4f projection = new Matrix4f();
   private final Matrix4f view = new Matrix4f();
   private MatLayout layout;
   private float flash;
   public float time;
   public float openingTime = Float.POSITIVE_INFINITY;
   public long animationId;
   public float controllerHover;
   public float bookHover;
   public long contentRevision;
   public boolean foil;
   public final MatFold fold = new MatFold();
   public float deckHalfWidth;
   public float deckHalfDepth;
   public float shake;
   public float groundX;
   public float groundZ;
   public boolean overview;
   public boolean embedded;
   public boolean carrying;
   public boolean recipe;
   public List<MatLayout.DropZone> dropZones = List.of();
   public MatLayout.DropZone capacityZone;
   public float capacityStrength;
   public MatWorldContext world;
   public int itemCount;
   private final MatSceneState.Item[] items = new MatSceneState.Item[512];

   public void captureGui(GuiGraphicsExtractor g) {
      this.guiPose.set(g.pose());
      this.guiClip = ((MatGuiScissorAccess)g).matinventory$scissors().peek();
      this.bounds = new ScreenRectangle(0, 0, this.width, this.height).transformMaxBounds(this.guiPose);
      if (this.guiClip != null) {
         this.bounds = this.bounds.intersection(this.guiClip);
      }
   }

   @Override
   public Matrix3x2f pose() {
      return this.guiPose;
   }

   public boolean actualWorld() {
      return !this.embedded && this.world != null && this.world.placement.world();
   }

   public MatSceneState() {
      for (int i = 0; i < this.items.length; i++) {
         this.items[i] = new MatSceneState.Item();
      }
   }

   public void begin(MatLayout layout, MatCamera camera, float flash, float time, boolean carrying, boolean recipe) {
      this.embedded = false;
      if (this.width != camera.width || this.height != camera.height) {
         this.bounds = new ScreenRectangle(0, 0, camera.width, camera.height);
      }

      this.width = camera.width;
      this.height = camera.height;
      this.projection.set(camera.projection);
      this.view.set(camera.view);
      this.layout = layout;
      this.flash = flash;
      this.time = time;
      this.carrying = carrying;
      this.recipe = recipe;
      this.itemCount = 0;
      this.foil = false;
      this.controllerHover = 0.0F;
      this.bookHover = 0.0F;
      this.groundX = camera.panX;
      this.groundZ = camera.panZ;
      this.overview = camera.overview;
      this.deckHalfWidth = camera.deckHalfWidth();
      this.deckHalfDepth = camera.coverHalfDepth;
   }

   public MatSceneState.Item next() {
      return this.items[this.itemCount++];
   }

   public void discardLast() {
      this.itemCount--;
   }

   public MatSceneState.Item[] items() {
      return this.items;
   }

   public Matrix4f projection() {
      return this.projection;
   }

   public Matrix4f view() {
      return this.view;
   }

   public long visualKey() {
      long h = this.contentRevision;
      h = h * 31L + (this.embedded ? 1 : 0);
      h = h * 31L + this.projection.hashCode();
      h = h * 31L + this.view.hashCode();
      h = h * 31L + this.layout.revision;
      h = h * 31L + this.itemCount;
      h = h * 31L + (this.carrying ? 1 : 0);
      h = h * 31L + (this.recipe ? 1 : 0);
      h = h * 31L + this.world.placement.hashCode();
      h = h * 31L + Float.floatToIntBits(OpeningMotion.active(this.openingTime) ? this.openingTime : -1.0F);
      h = h * 31L + System.identityHashCode(this.world.tiles);
      h = h * 31L + Float.floatToIntBits(this.flash);
      h = h * 31L + Float.floatToIntBits(this.shake);
      h = h * 31L + this.dropZones.hashCode();
      h = h * 31L + Objects.hashCode(this.capacityZone);
      h = h * 31L + Float.floatToIntBits(this.capacityStrength);
      h = h * 31L + MatMaterials.generation;
      h = h * 31L + ItemPresentationData.generation;
      h = h * 31L + Float.floatToIntBits(this.controllerHover);
      h = h * 31L + MatInventoryClient.config.ledColor;
      h = h * 31L + MatInventoryClient.config.matColor;
      h = h * 31L + (MatInventoryClient.config.gridEnabled ? 1 : 0);
      h = h * 31L + Float.floatToIntBits(this.bookHover);
      h = h * 31L + this.world.leds.revision();
      h = h * 31L + Float.floatToIntBits(MatInventoryClient.config.exposure);
      h = h * 31L + Float.floatToIntBits(MatInventoryClient.config.saturation);

      for (int i = 0; i < this.itemCount; i++) {
         MatSceneState.Item v = this.items[i];
         h = h * 31L + v.slot;
         h = h * 31L + v.copies;
         h = h * 31L + Float.floatToIntBits(v.x);
         h = h * 31L + Float.floatToIntBits(v.y);
         h = h * 31L + Float.floatToIntBits(v.z);
         h = h * 31L + Float.floatToIntBits(v.yaw);
         h = h * 31L + Float.floatToIntBits(v.pitch);
         h = h * 31L + Float.floatToIntBits(v.scale);
         h = h * 31L + Float.floatToIntBits(v.hover);
         h = h * 31L + Float.floatToIntBits(v.glow);
         h = h * 31L + Float.floatToIntBits(v.stackShade);
         h = h * 31L + v.pileSeed;
         h = h * 31L + Float.floatToIntBits(v.pileWidth);
         h = h * 31L + Float.floatToIntBits(v.pileHeight);
         h = h * 31L + Float.floatToIntBits(v.pileDepth);
         h = h * 31L + Float.floatToIntBits(v.outlineExpansion);
         h = h * 31L + v.materialStyle;
         h = h * 31L + Float.floatToIntBits(v.roll);
      }

      return h;
   }

   public MatLayout layout() {
      return this.layout;
   }

   public float flash() {
      return this.flash;
   }

   @Override
   public int x0() {
      return 0;
   }

   @Override
   public int y0() {
      return 0;
   }

   @Override
   public int x1() {
      return this.width;
   }

   @Override
   public int y1() {
      return this.height;
   }

   @Override
   public float scale() {
      return 1.0F;
   }

   @Override
   public ScreenRectangle scissorArea() {
      return this.guiClip;
   }

   @Override
   public ScreenRectangle bounds() {
      return this.bounds;
   }

   @Environment(EnvType.CLIENT)
   public static final class Item {
      public ItemStackRenderState model;
      public float x;
      public float y;
      public float z;
      public float yaw;
      public float glow;
      public float scale;
      public float centerX;
      public float centerY;
      public float centerZ;
      public float pitch;
      public float roll;
      public float hover;
      public float shadowRadius;
      public float alpha = 1.0F;
      public float durability = -1.0F;
      public float shadowX;
      public float shadowZ;
      public float shadowYaw;
      public float stackStep;
      public float pileWidth;
      public float pileDepth;
      public float pileHeight;
      public float stackShade = 1.0F;
      public float outlineExpansion = 1.025F;
      public int pileSeed;
      public int materialStyle;
      public float nativeBlockEnlargement = 1.0F;
      public int copies;
      public int slot;
      public int barColor;
      public boolean torch;
      public boolean ghost;
      public boolean decorativePile;
      public boolean blockPile;
      public boolean largeFlatBlockPile;
      public boolean foodPile;
      public boolean materialPile;
      public boolean flatHover;
      public boolean nativeGeometry;
      public boolean resting;
      public float supportX;
      public float supportZ;
      public float contactOffset;
      public float supportY;
      public Identifier nativeTexture;

      private float offset(int copy) {
         return this.copies != 1 && copy != 4 ? (copy % 2 == 0 ? -0.53F : 0.53F) * this.pileWidth : 0.0F;
      }

      private float localX(int copy) {
         return this.decorativePile
            ? DecorativeBlockPile.x(copy, this.copies, this.pileWidth)
            : (
               this.largeFlatBlockPile
                  ? (copy == 0 ? 0.0F : ((copy & 1) == 0 ? -0.035F : 0.035F) * this.pileWidth)
                  : (
                     this.blockPile
                        ? BlockPile.x(copy, this.copies, this.pileWidth, this.pileSeed)
                        : (
                           this.foodPile
                              ? ConsumablePile.x(copy, this.copies, this.pileWidth, this.pileSeed)
                              : (this.materialPile ? MaterialPile.x(copy, this.copies, this.pileWidth, this.pileSeed, this.materialStyle) : this.offset(copy))
                        )
                  )
            );
      }

      private float localZ(int copy) {
         return this.decorativePile
            ? DecorativeBlockPile.z(copy, this.copies, this.pileDepth)
            : (
               this.largeFlatBlockPile
                  ? copy * 0.04F * this.pileDepth
                  : (
                     this.blockPile
                        ? BlockPile.z(copy, this.copies, this.pileDepth, this.pileSeed)
                        : (
                           this.foodPile
                              ? ConsumablePile.z(copy, this.copies, this.pileDepth, this.pileSeed)
                              : (this.materialPile ? MaterialPile.z(copy, this.copies, this.pileDepth, this.pileSeed, this.materialStyle) : 0.0F)
                        )
                  )
            );
      }

      public float copyX(int copy) {
         double a = Math.toRadians(this.shadowYaw);
         return this.localX(copy) * (float)Math.cos(a) + this.localZ(copy) * (float)Math.sin(a);
      }

      public float copyZ(int copy) {
         double a = Math.toRadians(this.shadowYaw);
         return -this.localX(copy) * (float)Math.sin(a) + this.localZ(copy) * (float)Math.cos(a);
      }

      public float copyYaw(int copy) {
         return this.largeFlatBlockPile
            ? (copy == 0 ? 0 : (copy % 2 == 0 ? -2 : 2))
            : (
               this.blockPile
                  ? BlockPile.yaw(copy, this.pileSeed)
                  : (
                     this.foodPile
                        ? ConsumablePile.yaw(copy, this.copies, this.pileSeed)
                        : (this.materialPile ? MaterialPile.yaw(copy, this.copies, this.pileSeed, this.materialStyle) : 0.0F)
                  )
            );
      }

      public float copyScale(int copy) {
         return this.blockPile
            ? BlockPile.scale(copy) / (copy > 0 ? this.nativeBlockEnlargement : 1.0F)
            : (this.materialPile ? MaterialPile.scale(copy, this.materialStyle) : 1.0F);
      }

      public float copyY(int copy) {
         if (this.decorativePile) {
            return 0.0F;
         }

         if (this.largeFlatBlockPile && copy > 0) {
            float h = Math.max(0.0F, this.pileHeight - 0.025F);
            float ratio = 0.88F / this.nativeBlockEnlargement;
            return h * 0.5F + h * ratio * (copy - 0.5F) + 0.012F * copy;
         }

         if (this.blockPile && this.nativeBlockEnlargement > 1.0F && copy > 0) {
            float h = Math.max(0.0F, this.pileHeight - 0.025F);
            float ratio = 0.88F / this.nativeBlockEnlargement;
            if (this.copies == 3 ? copy == 2 : copy >= 4) {
               float support = this.copies != 3 && copy != 4 ? ratio : 1.0F;
               return h * (support - 0.5F + ratio * 0.5F) + 0.025F;
            } else {
               return h * (ratio - 1.0F) * 0.5F;
            }
         } else {
            return this.blockPile
               ? BlockPile.y(copy, this.copies, this.pileHeight)
               : (
                  this.foodPile
                     ? ConsumablePile.y(copy, this.pileHeight)
                     : (this.materialPile ? MaterialPile.y(copy, this.copies, this.pileHeight, this.materialStyle) : copy / 2 * this.pileHeight)
               );
         }
      }

      public float hoverTilt() {
         return this.copies <= 1 && !this.flatHover ? this.hover * 10.0F : 0.0F;
      }

      public ItemStackRenderState model() {
         return this.model;
      }

      public float x() {
         return this.x;
      }

      public float y() {
         return this.y;
      }

      public float z() {
         return this.z;
      }

      public float yaw() {
         return this.yaw;
      }

      public float glow() {
         return this.glow;
      }

      public int copies() {
         return this.copies;
      }
   }
}
