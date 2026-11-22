package com.example.matinventory.screen;

import com.example.matinventory.MatInventoryClient;
import com.example.matinventory.animation.MatAnimation;
import com.example.matinventory.arrangement.ItemCategoryClassifier;
import com.example.matinventory.arrangement.ItemPresentationData;
import com.example.matinventory.capacity.BlockKinds;
import com.example.matinventory.capacity.ConsumableKinds;
import com.example.matinventory.capacity.MaterialKinds;
import com.example.matinventory.capacity.VehicleKinds;
import com.example.matinventory.config.MatConfig;
import com.example.matinventory.layout.MatLayout;
import com.example.matinventory.mixin.CreativeSlotAccess;
import com.example.matinventory.render.MatItems;
import com.example.matinventory.render.MatOverlay;
import com.example.matinventory.render.MatSceneRenderer;
import com.example.matinventory.render.MatSceneState;
import com.example.matinventory.scene.MatCamera;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

@Environment(EnvType.CLIENT)
public final class MatEmbeddedPreview {
   public final MatLayout layout = MatLayout.inventory();
   public final MatCamera camera = new MatCamera();
   private final MatAnimation animation = new MatAnimation();
   private final AbstractContainerMenu nativeMenu;
   private final AbstractContainerMenu source;
   private final MatItems items;
   private final Map<Integer, Slot> nativeSlots = new HashMap<>();
   private final Map<Slot, Integer> sourceSlots = new IdentityHashMap<>();
   private Slot pickupOrigin;
   private boolean pickupDetached;
   private ItemStack pickupItem = ItemStack.EMPTY;
   private int slotOriginX;
   private int slotOriginY;
   private final float[] frameBounds = new float[4];
   private int framedRevision = -1;
   private final Matrix4f boundsMatrix = new Matrix4f();
   private final Vector3f boundsCorner = new Vector3f();
   private final MatOverlay overlay = new MatOverlay();
   public final int x;
   public final int y;
   public final int width;
   public final int height;

   public float openingProgress() {
      return this.animation.matOpen();
   }

   public void slotOrigin(int x, int y) {
      this.slotOriginX = x;
      this.slotOriginY = y;
   }

   public int sceneHeight() {
      return this.height;
   }

   public float[] centeredBounds() {
      return (float[])this.frameBounds.clone();
   }

   public void toggleGrid() {
      if (this.nativeMenu.getCarried().isEmpty()) {
         MatConfig c = MatInventoryClient.config;
         c.gridEnabled = !c.gridEnabled;
         c.save();
         this.layout.gridChanged();
      }
   }

   public MatEmbeddedPreview(AbstractContainerMenu menu, int x, int y, int width, int height) {
      this.nativeMenu = menu;
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
      this.overlay.clipOrigin(x, y);
      this.source = Minecraft.getInstance().player.inventoryMenu;
      this.items = MatItems.forMenu(this.source);

      for (Slot original : this.source.slots) {
         for (Slot target : menu.slots) {
            if (unwrap(target).container == original.container && unwrap(target).getContainerSlot() == original.getContainerSlot()) {
               this.nativeSlots.put(original.index, target);
               this.sourceSlots.put(target, original.index);
               break;
            }
         }
      }
   }

   public static Slot unwrap(Slot slot) {
      return slot instanceof CreativeSlotAccess access ? access.matinventory$target() : slot;
   }

   public boolean owns(Slot slot) {
      return this.sourceSlots.containsKey(slot);
   }

   public void occludedTop(int height) {
      this.overlay.minimumTop(height);
   }

   public Slot nativeSlot(int sourceSlot) {
      return this.nativeSlots.get(sourceSlot);
   }

   public int sourceSlot(Slot nativeSlot) {
      return this.sourceSlots.getOrDefault(nativeSlot, -1);
   }

   private boolean storedManualPosition(Slot slot) {
      if (slot != null && slot.hasItem() && this.owns(slot)) {
         MatConfig.DetachedItem saved = MatInventoryClient.config.detachedItems.get(MatLayout.storageKey(slot));
         return saved != null && saved.item.equals(ItemPresentationData.id(slot.getItem())) && Boolean.TRUE.equals(saved.independent);
      } else {
         return false;
      }
   }

   public MatEmbeddedPreview.ClickState beforeClick(Slot slot, double x, double y) {
      MatLayout.Place p = slot == null ? null : this.layout.place(this.sourceSlot(slot));
      return new MatEmbeddedPreview.ClickState(
         this.nativeMenu.getCarried().copy(),
         slot == null ? ItemStack.EMPTY : slot.getItem().copy(),
         this.nativeMenu.getCarried().isEmpty() ? null : this.captureDrop(x, y),
         p != null && p.detachedPlacement || this.storedManualPosition(slot)
      );
   }

   public void afterClick(Slot slot, ContainerInput type, MatEmbeddedPreview.ClickState state) {
      if (state != null && type == ContainerInput.PICKUP) {
         ItemStack before = state.cursor;
         ItemStack after = this.nativeMenu.getCarried();
         boolean placed = slot != null
            && !before.isEmpty()
            && ItemStack.isSameItemSameComponents(before, slot.getItem())
            && (after.isEmpty() || !ItemStack.isSameItemSameComponents(before, after) || after.getCount() < before.getCount());
         if (placed) {
            this.applyDrop(slot, before, state.drop);
         }

         if (after.isEmpty()) {
            this.pickupOrigin = null;
            this.pickupDetached = false;
            this.pickupItem = ItemStack.EMPTY;
            this.layout.separatePreview(false);
         } else if (before.isEmpty() || !ItemStack.isSameItemSameComponents(before, after)) {
            int id = slot == null ? -1 : this.sourceSlot(slot);
            MatLayout.Place p = this.layout.place(id);
            this.pickupOrigin = p != null && p.group() == MatLayout.Group.SUPPLIES ? slot : null;
            this.pickupItem = after.copy();
            this.pickupDetached = this.pickupOrigin != null && state.detached;
            if (p != null) {
               this.items.pickup(id);
            }

            this.layout.separatePreview(this.pickupDetached);
         }
      }
   }

   private Slot returnOrigin() {
      ItemStack held = this.nativeMenu.getCarried();
      if (this.pickupOrigin != null
         && ItemStack.isSameItemSameComponents(held, this.pickupItem)
         && this.pickupOrigin.isActive()
         && this.pickupOrigin.mayPlace(held)) {
         ItemStack current = this.pickupOrigin.getItem();
         return !current.isEmpty()
               && (!ItemStack.isSameItemSameComponents(current, held) || current.getCount() + held.getCount() > this.pickupOrigin.getMaxStackSize(held))
            ? null
            : this.pickupOrigin;
      } else {
         return null;
      }
   }

   private MatLayout.SeparateDestination independentDestination(float x, float z) {
      ItemStack saved = this.source.getCarried();
      this.source.setCarried(this.nativeMenu.getCarried());

      try {
         return this.layout
            .separateDestination(
               this.source,
               this.pickupOrigin == null ? -1 : this.sourceSlot(this.pickupOrigin),
               x,
               z,
               this.items.separateFootprint(this.nativeMenu.getCarried())
            );
      } finally {
         this.source.setCarried(saved);
      }
   }

   public MatEmbeddedPreview.DropIntent captureDrop(double mx, double my) {
      if (this.contains(mx, my) && this.camera.raycast(mx - this.x, my - this.y)) {
         float px = this.camera.hit.x;
         float pz = this.camera.hit.z;
         if (this.pickupDetached) {
            MatLayout.SeparateDestination destination = this.independentDestination(px, pz);
            return destination == null ? null : new MatEmbeddedPreview.DropIntent(-1, -1, -1, -1, -1, destination.x(), destination.z(), false);
         }

         MatLayout.Place p = this.layout.pick(px, pz, this.camera);
         MatLayout.ToolCell t = p != null && p.toolCell >= 0 ? this.layout.toolCell(p.toolCell) : this.layout.toolCellAt(px, pz);
         MatLayout.FoodCell f = p != null && p.foodCell >= 0 ? this.layout.foodCell(p.foodCell) : this.layout.foodCellAt(px, pz);
         MatLayout.MaterialCell m = p != null && p.materialCell >= 0 ? this.layout.materialCell(p.materialCell) : this.layout.materialCellAt(px, pz);
         MatLayout.BlockCell b = this.layout.blockCellAt(px, pz);
         if (b == null && p != null && p.blockCell >= 0) {
            b = this.layout.blockCell(p.blockCell);
         }

         MatLayout.VehicleCell v = this.layout
            .vehicleCells()
            .stream()
            .filter(c -> p != null && p.vehicleCell >= 0 ? c.index() == p.vehicleCell : c.contains(px, pz))
            .findFirst()
            .orElse(null);
         return new MatEmbeddedPreview.DropIntent(
            t == null ? -1 : t.index(),
            f == null ? -1 : f.index(),
            m == null ? -1 : m.index(),
            b == null ? -1 : b.index(),
            v == null ? -1 : v.index(),
            px,
            pz,
            p != null && p.occupied && this.source.getSlot(p.slot()).hasItem() && (b == null || p.blockCell == b.index())
         );
      } else {
         return null;
      }
   }

   public void applyDrop(Slot slot, ItemStack stack, MatEmbeddedPreview.DropIntent intent) {
      if (intent == null || this.layout.acceptsStorage(stack, intent.x, intent.z)) {
         int id = this.sourceSlot(slot);
         MatLayout.Place p = this.layout.place(id);
         if (p != null && p.group() == MatLayout.Group.SUPPLIES) {
            String kind = MatLayout.toolKind(stack);
            if (this.pickupDetached && intent != null) {
               this.layout.detach(this.source.getSlot(id), intent.x, intent.z, this.items.separateScale(stack));
            } else if (intent != null && !intent.onModel && !MatInventoryClient.config.gridEnabled && this.layout.manualStorageAt(intent.x, intent.z)) {
               if (kind != null) {
                  float[] point = this.layout.toolPileDestination(this.source, id, intent.x, intent.z, this.items.separateFootprint(stack));
                  if (point != null) {
                     this.layout.placeToolPile(this.source, kind, point[0], point[1]);
                  }
               } else {
                  this.layout.detach(this.source.getSlot(id), intent.x, intent.z);
               }

               if (kind != null) {
                  this.layout.promoteTool(kind, id);
               }
            } else {
               if (intent != null) {
                  if (kind != null && intent.tool >= 0) {
                     int from = this.layout.toolCellFor(stack);
                     if (from >= 0 && from != intent.tool) {
                        MatLayout.ToolCell target = this.layout.toolCell(intent.tool);
                        String other = target == null ? null : target.kind();
                        float[] a = this.toolCenter(from);
                        float[] b = this.toolCenter(intent.tool);
                        this.layout.swapToolCells(from, intent.tool);
                        if (!MatInventoryClient.config.gridEnabled) {
                           this.layout.placeToolPile(this.source, kind, b[0], b[1]);
                           if (other != null) {
                              this.layout.placeToolPile(this.source, other, a[0], a[1]);
                           }
                        }
                     }
                  } else if (ConsumableKinds.isConsumable(stack) && intent.food >= 0) {
                     this.layout.placeFood(this.source.getSlot(id), intent.food);
                  } else if (MaterialKinds.isMaterial(stack) && intent.material >= 0) {
                     this.layout.placeMaterial(this.source.getSlot(id), intent.material);
                  } else if (BlockKinds.isBlock(stack) && intent.block >= 0) {
                     this.layout.placeStorage(this.source, this.source.getSlot(id), intent.block, false);
                  } else if (VehicleKinds.isVehicle(stack) && intent.vehicle >= 0) {
                     this.layout.placeStorage(this.source, this.source.getSlot(id), intent.vehicle, true);
                  } else if (!MatInventoryClient.config.gridEnabled && ItemCategoryClassifier.flexible(stack)) {
                     this.layout.placeFlexible(stack, intent.x, intent.z);
                  }
               }

               if (kind != null) {
                  this.layout.promoteTool(kind, id);
               }
            }
         }
      }
   }

   private float[] toolCenter(int cell) {
      float x0 = Float.POSITIVE_INFINITY;
      float z0 = x0;
      float x1 = -x0;
      float z1 = -x0;

      for (MatLayout.Place p : this.layout.toolsInCell(cell)) {
         if (p.visible) {
            x0 = Math.min(x0, p.x - p.halfWidth);
            x1 = Math.max(x1, p.x + p.halfWidth);
            z0 = Math.min(z0, p.z - p.halfDepth);
            z1 = Math.max(z1, p.z + p.halfDepth);
         }
      }

      MatLayout.ToolCell c = this.layout.toolCell(cell);
      return Float.isFinite(x0) ? new float[]{(x0 + x1) * 0.5F, (z0 + z1) * 0.5F} : new float[]{(c.x0() + c.x1()) * 0.5F, (c.z0() + c.z1()) * 0.5F};
   }

   private Slot availableStorage() {
      ItemStack carried = this.nativeMenu.getCarried();

      for (MatLayout.Place p : this.layout.storagePlaces()) {
         Slot target = this.nativeSlots.get(p.slot());
         if (target != null
            && target.isActive()
            && target.hasItem()
            && target.mayPlace(carried)
            && ItemStack.isSameItemSameComponents(target.getItem(), carried)
            && target.getItem().getCount() < target.getMaxStackSize(carried)) {
            return target;
         }
      }

      for (MatLayout.Place p : this.layout.storagePlaces()) {
         Slot target = this.nativeSlots.get(p.slot());
         if (target != null && target.isActive() && !target.hasItem() && target.mayPlace(carried)) {
            return target;
         }
      }

      return null;
   }

   private int cycleOwner(double mx, double my) {
      int ui = this.overlay.cycleOwnerAt(mx - this.x, my - this.y);
      if (ui >= 0) {
         return ui;
      }

      Slot hit = this.pick(mx, my);
      return hit == null ? -1 : this.sourceSlot(hit);
   }

   public boolean scroll(double mx, double my, double amount) {
      if (this.nativeMenu.getCarried().isEmpty() && amount != 0.0) {
         int owner = this.cycleOwner(mx, my);
         return owner >= 0 && this.layout.cyclePreview(owner).size() > 1 && this.overlay.scrollCycle(this.layout, owner, amount);
      } else {
         return false;
      }
   }

   public boolean confirmCycle(double mx, double my) {
      if (!this.nativeMenu.getCarried().isEmpty()) {
         return false;
      }

      int owner = this.cycleOwner(mx, my);
      if (owner < 0 || this.layout.cyclePreview(owner).size() < 2) {
         return false;
      }

      if (!this.layout.selectCycleItem(owner, this.overlay.cycleSelection(this.layout, owner))) {
         return false;
      }

      this.overlay.cycleConfirmed(this.source);
      return true;
   }

   public boolean contains(double mx, double my) {
      return mx >= this.x && mx < this.x + this.width && my >= this.y && my < this.y + this.height;
   }

   public Slot pick(double mx, double my) {
      if (this.contains(mx, my) && !(my >= this.y + this.sceneHeight()) && this.camera.width != 0 && !(this.animation.matOpen() < 0.999F)) {
         int ui = this.overlay.cycleOwnerAt(mx - this.x, my - this.y);
         if (ui >= 0 && this.nativeMenu.getCarried().isEmpty()) {
            return this.nativeSlots.get(ui);
         }

         if (!this.camera.raycast(mx - this.x, my - this.y)) {
            return null;
         }

         if (!this.nativeMenu.getCarried().isEmpty() && !this.layout.acceptsStorage(this.nativeMenu.getCarried(), this.camera.hit.x, this.camera.hit.z)) {
            return null;
         }

         MatLayout.Place place = this.layout.pick(this.camera.hit.x, this.camera.hit.z, this.camera);
         MatLayout.BlockCell blockCell = this.layout.blockCellAt(this.camera.hit.x, this.camera.hit.z);
         if (this.nativeMenu.getCarried().isEmpty()
            || blockCell == null
            || !BlockKinds.isBlock(this.nativeMenu.getCarried())
            || place != null && place.blockCell == blockCell.index()) {
            boolean actual = place != null && place.occupied && this.source.getSlot(place.slot()).hasItem();
            if (this.nativeMenu.getCarried().isEmpty() || !this.pickupDetached || place != null && place.group() != MatLayout.Group.SUPPLIES) {
               if (this.nativeMenu.getCarried().isEmpty() && actual && place.detachedPlacement) {
                  return this.nativeSlots.get(place.slot());
               }

               if (!this.nativeMenu.getCarried().isEmpty()
                  && !MatInventoryClient.config.gridEnabled
                  && !actual
                  && (place == null || place.group() == MatLayout.Group.SUPPLIES)
                  && this.layout.manualStorageAt(this.camera.hit.x, this.camera.hit.z)) {
                  Slot origin = this.returnOrigin();
                  return origin != null ? origin : this.availableStorage();
               }

               MatLayout.ToolCell cell = place != null && place.toolCell >= 0
                  ? this.layout.toolCell(place.toolCell)
                  : this.layout.toolCellAt(this.camera.hit.x, this.camera.hit.z);
               if (cell != null) {
                  String held = MatLayout.toolKind(this.nativeMenu.getCarried());
                  if (this.nativeMenu.getCarried().isEmpty()) {
                     MatLayout.Place top = this.layout.topTool(cell.index());
                     if (top != null) {
                        return this.nativeSlots.get(top.slot());
                     }
                  } else if (held != null) {
                     Slot origin = this.returnOrigin();
                     if (origin != null) {
                        return origin;
                     }

                     for (MatLayout.Place p : this.layout.toolsInCell(cell.index())) {
                        Slot target = this.nativeSlots.get(p.slot());
                        if (target != null
                           && target.isActive()
                           && target.hasItem()
                           && target.mayPlace(this.nativeMenu.getCarried())
                           && ItemStack.isSameItemSameComponents(target.getItem(), this.nativeMenu.getCarried())
                           && target.getItem().getCount() < target.getMaxStackSize(this.nativeMenu.getCarried())) {
                           return target;
                        }
                     }

                     for (MatLayout.Place p : this.layout.storagePlaces()) {
                        Slot target = this.nativeSlots.get(p.slot());
                        if (target != null && target.isActive() && !target.hasItem() && target.mayPlace(this.nativeMenu.getCarried())) {
                           return target;
                        }
                     }

                     return null;
                  }
               }

               if (this.nativeMenu.getCarried().isEmpty() || place != null && place.group() != MatLayout.Group.SUPPLIES) {
                  if (place != null) {
                     Slot target = this.nativeSlots.get(place.slot());
                     if (target != null && target.isActive()) {
                        return target;
                     }
                  }

                  if (!this.nativeMenu.getCarried().isEmpty()) {
                     ItemStack saved = this.source.getCarried();
                     this.source.setCarried(this.nativeMenu.getCarried());

                     try {
                        Slot target = this.layout.dropTarget(this.source, this.camera.hit.x, this.camera.hit.z);
                        return target == null ? null : this.nativeSlots.get(target.index);
                     } finally {
                        this.source.setCarried(saved);
                     }
                  } else {
                     return null;
                  }
               } else {
                  ItemStack saved = this.source.getCarried();
                  this.source.setCarried(this.nativeMenu.getCarried());

                  try {
                     float px = this.camera.hit.x;
                     float pz = this.camera.hit.z;
                     if (place != null) {
                        if (place.foodCell >= 0) {
                           MatLayout.FoodCell c = this.layout.foodCell(place.foodCell);
                           px = (c.x0() + c.x1()) * 0.5F;
                           pz = (c.z0() + c.z1()) * 0.5F;
                        } else if (place.materialCell >= 0) {
                           MatLayout.MaterialCell c = this.layout.materialCell(place.materialCell);
                           px = (c.x0() + c.x1()) * 0.5F;
                           pz = (c.z0() + c.z1()) * 0.5F;
                        }
                     }

                     Slot destination = this.layout.dropTarget(this.source, px, pz);
                     return destination == null ? null : this.nativeSlots.get(destination.index);
                  } finally {
                     this.source.setCarried(saved);
                  }
               }
            } else {
               MatLayout.SeparateDestination destination = this.independentDestination(this.camera.hit.x, this.camera.hit.z);
               return destination == null ? null : this.nativeSlots.get(destination.slot().index);
            }
         } else {
            Slot origin = this.returnOrigin();
            return origin != null ? origin : this.availableStorage();
         }
      } else {
         return null;
      }
   }

   public void render(GuiGraphicsExtractor g, int mx, int my) {
      this.animation.frame();
      ItemStack saved = this.source.getCarried();
      this.source.setCarried(this.nativeMenu.getCarried());

      try {
         this.items.arrange(this.source, this.layout, this.animation, !this.nativeMenu.getCarried().isEmpty());
         float x0 = Float.POSITIVE_INFINITY;
         float z0 = x0;
         float x1 = -x0;
         float z1 = -x0;

         for (ItemPresentationData.Compartment area : this.layout.solidCompartments()) {
            x0 = Math.min(x0, area.x0());
            z0 = Math.min(z0, area.z0());
            x1 = Math.max(x1, area.x1());
            z1 = Math.max(z1, area.z1());
         }

         if (this.framedRevision != this.layout.revision) {
            this.frameBounds[0] = x0 - 0.25F;
            this.frameBounds[1] = z0 - 0.25F;
            this.frameBounds[2] = x1 + 0.25F;
            this.frameBounds[3] = z1 + 0.25F;
         }

         this.camera.embedded(this.width, this.sceneHeight(), this.frameBounds[0], this.frameBounds[1], this.frameBounds[2], this.frameBounds[3]);
         if (this.framedRevision != this.layout.revision) {
            this.items.extract(this.source, this.layout, this.camera, this.animation, -1, 0.0F, 0.0F, -1000.0, -1000.0);

            for (MatLayout.Place p : this.layout.places()) {
               if (p.visible && p.occupied && this.nativeSlots.containsKey(p.slot())) {
                  for (int i = 0; i < p.pickBoxCount; i++) {
                     p.pickBoxes[i].expandXZ(this.frameBounds, this.boundsMatrix, this.boundsCorner);
                  }
               }
            }

            this.framedRevision = this.layout.revision;
         }

         this.camera.embedded(this.width, this.sceneHeight(), this.frameBounds[0], this.frameBounds[1], this.frameBounds[2], this.frameBounds[3]);
         Slot hovered = this.pick(mx, my);
         int hover = -1;
         if (hovered != null) {
            hover = this.sourceSlot(hovered);
         }

         MatSceneState frame = this.items.extract(this.source, this.layout, this.camera, this.animation, hover, 1.0F, 0.0F, mx - this.x, my - this.y);
         frame.embedded = true;
         frame.recipe = false;
         frame.dropZones = this.gridZones(mx, my);
         int i = 0;

         while (i < frame.itemCount) {
            MatSceneState.Item item = frame.items()[i];
            MatLayout.Place p = this.layout.place(item.slot);
            if (p != null && p.group() != MatLayout.Group.CRAFT && p.group() != MatLayout.Group.RESULT) {
               i++;
            } else {
               MatSceneState.Item last = frame.items()[--frame.itemCount];
               frame.items()[frame.itemCount] = item;
               frame.items()[i] = last;
            }
         }

         for (Entry<Integer, Slot> entry : this.nativeSlots.entrySet()) {
            MatLayout.Place p = this.layout.place(entry.getKey());
            Slot slot = entry.getValue();
            if (p != null && p.visible) {
               Vector4f point = this.camera.project(p.pickX, p.pickY, p.pickZ);
               SlotCoordinates.move(slot, this.x + Math.round(point.x) - 8 - this.slotOriginX, this.y + Math.round(point.y) - 8 - this.slotOriginY);
            } else {
               SlotCoordinates.move(slot, -10000, -10000);
            }
         }

         g.pose().pushMatrix();
         g.pose().translate(this.x, this.y);

         try {
            MatSceneRenderer.draw(g, frame);
            this.renderCounts(g);
            i = this.overlay.cycleOwnerAt(mx - this.x, my - this.y);
            if (i >= 0) {
               hover = i;
            }

            this.overlay
               .render(
                  g,
                  this.source,
                  this.layout,
                  this.camera,
                  hover,
                  mx - this.x,
                  my - this.y,
                  this.width,
                  this.sceneHeight(),
                  this.animation.elapsed,
                  hover >= 0 && this.layout.cyclePreview(hover).size() > 1 ? 1 : 0
               );
         } finally {
            g.pose().popMatrix();
         }
      } finally {
         this.source.setCarried(saved);
      }
   }

   private List<MatLayout.DropZone> gridZones(double mx, double my) {
      if (!MatInventoryClient.config.gridEnabled) {
         return List.of();
      }

      boolean hit = this.contains(mx, my) && my < this.y + this.sceneHeight() && this.camera.raycast(mx - this.x, my - this.y);
      float px = hit ? this.camera.hit.x : Float.NaN;
      float pz = hit ? this.camera.hit.z : Float.NaN;
      if (!this.nativeMenu.getCarried().isEmpty()) {
         return this.layout.dropZones(this.source, px, pz).stream().filter(z -> {
            ItemPresentationData.Compartment cx = this.layout.compartmentAt((z.x0() + z.x1()) * 0.5F, (z.z0() + z.z1()) * 0.5F);
            return cx == null || cx.category() != ItemCategoryClassifier.Category.CRAFT;
         }).toList();
      }

      ArrayList<MatLayout.DropZone> zones = new ArrayList<>();

      for (MatLayout.ToolCell c : this.layout.toolCells()) {
         zones.add(new MatLayout.DropZone(c.dropX0(), c.z0() + 0.13F, c.dropX1(), c.z1() - 0.13F, c.contains(px, pz)));
      }

      for (MatLayout.FoodCell c : this.layout.foodCells()) {
         zones.add(new MatLayout.DropZone(c.x0(), c.z0(), c.x1(), c.z1(), c.contains(px, pz)));
      }

      for (MatLayout.MaterialCell c : this.layout.materialCells()) {
         zones.add(new MatLayout.DropZone(c.x0(), c.z0(), c.x1(), c.z1(), c.contains(px, pz)));
      }

      for (MatLayout.BlockCell c : this.layout.blockCells()) {
         zones.add(new MatLayout.DropZone(c.x0(), c.z0(), c.x1(), c.z1(), c.contains(px, pz)));
      }

      for (MatLayout.VehicleCell c : this.layout.vehicleCells()) {
         zones.add(new MatLayout.DropZone(c.x0(), c.z0(), c.x1(), c.z1(), c.contains(px, pz)));
      }

      for (MatLayout.EquipmentCell c : this.layout.equipmentCells()) {
         zones.add(new MatLayout.DropZone(c.x0(), c.z0(), c.x1(), c.z1(), c.contains(px, pz)));
      }

      return zones;
   }

   private void renderCounts(GuiGraphicsExtractor g) {
      Font font = Minecraft.getInstance().font;

      for (MatLayout.Place p : this.layout.places()) {
         Slot slot = this.nativeSlots.get(p.slot());
         if (slot != null
            && slot.isActive()
            && p.visible
            && slot.hasItem()
            && slot.getItem().getCount() > 1
            && Float.isFinite(p.screenRight)
            && Float.isFinite(p.screenBottom)
            && !(p.screenRight < 0.0F)
            && !(p.screenLeft >= this.width)
            && !(p.screenBottom < 0.0F)
            && !(p.screenTop >= this.height)) {
            String count = Integer.toString(slot.getItem().getCount());
            float right = p.screenRight;
            float bottom = p.screenBottom;
            float cellX = Float.NaN;
            float cellZ = Float.NaN;
            if (p.foodCell >= 0) {
               MatLayout.FoodCell cell = this.layout.foodCell(p.foodCell);
               if (cell != null) {
                  cellX = cell.x1();
                  cellZ = cell.z1();
               }
            } else if (p.materialCell >= 0) {
               MatLayout.MaterialCell cell = this.layout.materialCell(p.materialCell);
               if (cell != null) {
                  cellX = cell.x1();
                  cellZ = cell.z1();
               }
            } else if (p.blockCell >= 0) {
               MatLayout.BlockCell cell = this.layout.blockCell(p.blockCell);
               if (cell != null) {
                  cellX = cell.x1();
                  cellZ = cell.z1();
               }
            }

            if (Float.isFinite(cellX)) {
               Vector4f anchor = this.camera.project(cellX - 0.1F, 0.0F, cellZ - 0.1F);
               right = Math.min(right, anchor.x);
               bottom = Math.min(bottom, anchor.y);
            }

            float scale = Math.min(1.0F, Math.max(0.45F, this.width / 560.0F));
            float textX = Math.round(Math.clamp(right - font.width(count) * scale, 1.0F, Math.max(1.0F, this.width - font.width(count) * scale - 1.0F)));
            float textY = Math.round(Math.clamp(bottom - 9.0F * scale + 1.0F, 1.0F, Math.max(1.0F, this.height - 9.0F * scale - 1.0F)));
            g.pose().pushMatrix();
            g.pose().translate(textX, textY);
            g.pose().scale(scale, scale);
            g.text(font, count, 0, 0, -1, true);
            g.pose().popMatrix();
         }
      }

      this.overlay.equipmentIndicators(g, this.source, this.layout, this.camera);
   }

   @Environment(EnvType.CLIENT)
   public record ClickState(ItemStack cursor, ItemStack atSlot, MatEmbeddedPreview.DropIntent drop, boolean detached) {
   }

   @Environment(EnvType.CLIENT)
   public record DropIntent(int tool, int food, int material, int block, int vehicle, float x, float z, boolean onModel) {
   }
}
