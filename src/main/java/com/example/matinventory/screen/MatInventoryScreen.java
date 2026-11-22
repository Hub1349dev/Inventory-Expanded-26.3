package com.example.matinventory.screen;

import com.example.matinventory.MatInventoryClient;
import com.example.matinventory.MatSounds;
import com.example.matinventory.animation.CraftCameraMotion;
import com.example.matinventory.animation.MatAnimation;
import com.example.matinventory.arrangement.ItemCategoryClassifier;
import com.example.matinventory.arrangement.ItemPresentationData;
import com.example.matinventory.capacity.BlockKinds;
import com.example.matinventory.capacity.ConsumableKinds;
import com.example.matinventory.capacity.EquipmentKinds;
import com.example.matinventory.capacity.MaterialKinds;
import com.example.matinventory.capacity.VehicleKinds;
import com.example.matinventory.compat.MatInput;
import com.example.matinventory.config.MatConfig;
import com.example.matinventory.core.GridController;
import com.example.matinventory.core.LedController;
import com.example.matinventory.core.MatPalette;
import com.example.matinventory.core.RecipeBookProp;
import com.example.matinventory.interaction.CraftRequest;
import com.example.matinventory.interaction.EatItemRequest;
import com.example.matinventory.interaction.InventoryCrafting;
import com.example.matinventory.interaction.InventoryEating;
import com.example.matinventory.layout.MatLayout;
import com.example.matinventory.mixin.ContainerHoverAccessor;
import com.example.matinventory.platform.BundleClient;
import com.example.matinventory.platform.ClientNetwork;
import com.example.matinventory.render.ItemSoundscape;
import com.example.matinventory.render.LedSoundscape;
import com.example.matinventory.render.MatItems;
import com.example.matinventory.render.MatOverlay;
import com.example.matinventory.render.MatSceneRenderer;
import com.example.matinventory.render.MatSceneState;
import com.example.matinventory.render.MatTransitionAudio;
import com.example.matinventory.scene.MatCamera;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.recipebook.CraftingRecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplay;

@Environment(EnvType.CLIENT)
public final class MatInventoryScreen extends AbstractContainerScreen<AbstractContainerMenu> implements RecipeUpdateListener {
   public final MatLayout layout;
   public final MatCamera camera = new MatCamera();
   public final MatAnimation animation = new MatAnimation();
   private final Screen vanilla;
   private final MatItems items;
   private final MatOverlay overlay = new MatOverlay();
   private final LedSoundscape ledAudio = new LedSoundscape();
   private final ItemSoundscape itemAudio = new ItemSoundscape();
   private final MatTransitionAudio transitionAudio = new MatTransitionAudio();
   private boolean craftRelease;
   private boolean controllerRelease;
   private float controllerHover;
   private float matHover;
   private float gridHover;
   private float bookHover;
   private boolean recipeRelease;
   private final CraftingRecipeBookComponent recipeBook;
   private boolean openedSound;
   private boolean openingBlankShown;
   private boolean openingPrepared;
   private boolean openingSurfaceReady;
   private boolean finishingClose;
   private int closePointerX;
   private int closePointerY;
   private float denied;
   private int toolOrigin = -1;
   private String heldTool;
   private double toolPressX;
   private double toolPressY;
   private boolean toolReleaseHandled;
   private boolean toolMiddleHandled;
   private boolean rejectedRelease;
   private int foodOrigin = -1;
   private int foodDestination = -1;
   private String heldFood;
   private boolean foodReleaseHandled;
   private int materialOrigin = -1;
   private int materialDestination = -1;
   private String heldMaterial;
   private boolean materialReleaseHandled;
   private int equipmentOrigin = -1;
   private String heldEquipment;
   private boolean equipmentReleaseHandled;
   private boolean equipmentToggleRelease;
   private final Map<Integer, Integer> equipmentReturns = new HashMap<>();
   private int storageOrigin = -1;
   private ItemStack heldStorage = ItemStack.EMPTY;
   private boolean storageReleaseHandled;
   private boolean separatePlacement;
   private boolean separateRelease;
   private boolean independentPickup;
   private int separateOrigin = -1;
   private String capacitySector;
   private float capacityTime;
   private SoundInstance craftSound;
   private boolean eatKeyHeld;
   private boolean eatPending;
   private int eatToken;
   private static int nextEatToken;
   private float craftHover;
   private float craftLeave;
   private float craftFocus;
   private float craftPreview;
   private float craftDwell;
   private final CraftCameraMotion craftFocusMotion = new CraftCameraMotion();
   private final CraftCameraMotion craftPreviewMotion = new CraftCameraMotion();

   public String capacityWarningSector() {
      return this.capacityTime > 0.0F ? this.capacitySector : null;
   }

   public MatInventoryScreen(AbstractContainerMenu menu, Inventory inventory, Component title, MatLayout layout, Screen vanilla) {
      super(menu, inventory, title);
      this.layout = layout;
      this.items = MatItems.forMenu(menu);
      this.vanilla = vanilla;
      this.recipeBook = menu instanceof AbstractCraftingMenu crafting ? new CraftingRecipeBookComponent(crafting) : null;
   }

   @Override
   public void added() {
      super.added();
      this.eatPending = this.eatKeyHeld = false;
      this.eatToken = 0;
      if (this.animation.closing()) {
         this.animation.cancelClosing();
      }

      this.finishingClose = false;
      this.items.world.beginOpening();
      this.items.beginOpening();
      this.items.world.leds.restore(MatInventoryClient.config.ledEnabled);
      this.itemAudio.stop();
      this.openingBlankShown = false;
      this.openingPrepared = false;
      this.openingSurfaceReady = false;
      this.hoveredSlot = null;
      this.isQuickCrafting = false;
      this.quickCraftSlots.clear();
      this.setDragging(false);
      this.toolOrigin = this.foodOrigin = this.foodDestination = this.materialOrigin = this.materialDestination = this.equipmentOrigin = this.storageOrigin = -1;
      this.heldTool = this.heldFood = this.heldMaterial = this.heldEquipment = null;
      this.heldStorage = ItemStack.EMPTY;
      this.separatePlacement = false;
      this.separateOrigin = -1;
      this.layout.separatePreview(false);
      this.craftRelease = this.controllerRelease = this.recipeRelease = this.toolReleaseHandled = this.toolMiddleHandled = this.rejectedRelease = false;
      this.foodReleaseHandled = this.materialReleaseHandled = this.equipmentReleaseHandled = this.equipmentToggleRelease = this.storageReleaseHandled = this.separateRelease = false;
   }

   @Override
   protected void init() {
      super.init();
      if (this.recipeBook != null) {
         boolean keepOpen = this.recipeBook.isVisible();
         this.recipeBook.init(this.width, 174, this.minecraft, true);
         if (this.recipeBook.isVisible() != keepOpen) {
            this.recipeBook.toggleVisibility();
         }

         this.addWidget(this.recipeBook);
      }

      this.updateCamera(this.width / 2.0, this.height / 2.0);
      this.camera.anchorPointer();
      if (!this.openedSound) {
         this.animation.startOpening();
         this.openedSound = true;
      }
   }

   @Override
   protected void containerTick() {
      if (this.minecraft.player.isCreative()) {
         this.fallback();
      } else if (this.animation.closing()) {
         this.animation.frame();
         this.transitionAudio.update(this, this.animation);
         if (this.animation.closeFinished()) {
            this.finishClosing();
         }
      } else {
         if (this.recipeBook != null) {
            this.recipeBook.tick();
         }
      }
   }

   private boolean recipePanelAt(double x, double y) {
      return this.recipeBook != null
         && this.recipeBook.isVisible()
         && x >= (this.width - 147) / 2.0 - 30.0
         && x < (this.width - 147) / 2.0 + 147.0
         && y >= 4.0
         && y < 170.0;
   }

   private void updateCamera(double x, double y) {
      MatConfig config = MatInventoryClient.config;
      this.camera.frame(this.layout);
      boolean panel = this.recipePanelAt(x, y);
      boolean cinematic = this.animation.cameraOpening();
      boolean ready = this.openingPrepared && this.animation.itemsSettled() && !this.animation.closing() && !cinematic && !panel;
      boolean over = ready && x >= 0.0 && y >= 0.0 && x < this.width && y < this.height && this.craftArea(x, y);
      boolean ingredients = false;
      if (ready) {
         for (MatLayout.Place p : this.layout.places()) {
            if (p.group() == MatLayout.Group.CRAFT && this.menu.getSlot(p.slot()).hasItem()) {
               ingredients = true;
               break;
            }
         }
      }

      float dt = this.animation.dt;
      this.craftLeave = over ? 0.0F : this.craftLeave + dt;
      if (over && !ingredients) {
         this.craftDwell = Math.min(2.0F, this.craftDwell + dt);
      } else if (!ready || ingredients || this.craftLeave >= 0.06F) {
         this.craftDwell = 0.0F;
      }

      boolean active = ready && (over || this.craftLeave < 0.06F && (this.craftFocus > 0.0F || this.craftPreview > 0.0F));
      float focusTarget = active && ingredients ? 1.0F : 0.0F;
      float previewTarget = active && !ingredients && this.craftDwell >= 2.0F ? 1.0F : 0.0F;
      this.craftFocus = this.craftFocusMotion.advance(focusTarget, dt);
      this.craftPreview = this.craftPreviewMotion.advance(previewTarget, dt);
      ItemPresentationData.Compartment region = ItemPresentationData.compartment(ItemCategoryClassifier.Category.CRAFT);
      this.camera.craftAnchor((region.x0() + region.x1()) * 0.5F, (region.z0() + region.z1()) * 0.5F);
      this.camera.craftFocus = this.craftFocus;
      this.camera.craftPreview = this.craftPreview;
      this.camera
         .update(
            this.width,
            this.height,
            !panel && !cinematic ? (float)x : -1000.0F,
            !panel && !cinematic ? (float)y : -1000.0F,
            config.cameraPitch,
            config.cameraFov,
            config.parallax
         );
      if (cinematic) {
         this.camera.opening(this.layout, this.animation.openingClock(), config.cameraPitch, config.cameraFov);
      }
   }

   public Slot pickSlot(double x, double y) {
      if (!this.openingPrepared || !this.animation.itemsSettled()) {
         return null;
      }

      if (this.recipePanelAt(x, y) || RecipeBookProp.hit(this.camera, x, y)) {
         return null;
      }

      if (GridController.hit(this.camera, x, y) || GridController.bodyHit(this.camera, x, y)) {
         return null;
      }

      if (LedController.hit(this.camera, x, y, true) || LedController.hit(this.camera, x, y, false)) {
         return null;
      }

      if (x < 0.0 || y < 0.0 || x >= this.width || y >= this.height) {
         return null;
      }

      if (!this.camera.raycast(x, y)) {
         return null;
      }

      MatLayout.Place place = this.layout.pick(this.camera.hit.x, this.camera.hit.z, this.camera);
      if (place != null && place.toolCell >= 0 && this.menu.getCarried().isEmpty()) {
         MatLayout.Place top = this.layout.topTool(place.toolCell);
         if (top != null) {
            place = top;
         }
      }

      if (place != null) {
         Slot slot = this.menu.getSlot(place.slot());
         if (slot.hasItem() && NativeItemClicks.applies(this.menu.getCarried(), slot.getItem())) {
            return slot;
         }

         if (!this.menu.getCarried().isEmpty() && !this.layout.acceptsStorage(this.menu.getCarried(), this.camera.hit.x, this.camera.hit.z)) {
            return null;
         }

         if (!this.menu.getCarried().isEmpty() && ItemCategoryClassifier.flexible(this.menu.getCarried()) && place.group() == MatLayout.Group.SUPPLIES) {
            return slot.mayPlace(this.menu.getCarried()) ? slot : this.layout.dropTarget(this.menu, this.camera.hit.x, this.camera.hit.z);
         }

         if (!this.menu.getCarried().isEmpty()
            && place.equipmentCell >= 0
            && !MatLayout.equipmentDestination(this.layout.equipmentCellFor(this.menu.getCarried()), place.equipmentCell)) {
            return null;
         }

         if (!this.menu.getCarried().isEmpty() && EquipmentKinds.equipped(slot)) {
            return this.layout.dropTarget(this.menu, this.camera.hit.x, this.camera.hit.z);
         }

         if (!this.menu.getCarried().isEmpty() && place.group() == MatLayout.Group.SUPPLIES) {
            ItemStack carried = this.menu.getCarried();
            if ((place.materialCell < 0 || !MaterialKinds.isMaterial(carried)) && place.category != ItemCategoryClassifier.classify(carried).category()) {
               return null;
            }

            if (place.toolCell >= 0 && !slot.hasItem() && this.layout.toolDropCellAt(this.camera.hit.x, this.camera.hit.z) == null) {
               return null;
            }

            if (place.foodCell >= 0 && !slot.hasItem() && this.layout.foodCellAt(this.camera.hit.x, this.camera.hit.z) == null) {
               return null;
            }

            if (place.materialCell >= 0 && !slot.hasItem() && this.layout.materialCellAt(this.camera.hit.x, this.camera.hit.z) == null) {
               return null;
            }

            if (!slot.mayPlace(carried)) {
               return null;
            }
         }

         return !this.menu.getCarried().isEmpty() && place.group() == MatLayout.Group.EQUIPMENT && !slot.mayPlace(this.menu.getCarried()) ? null : slot;
      } else {
         return this.layout.dropTarget(this.menu, this.camera.hit.x, this.camera.hit.z, this.isQuickCrafting ? this.quickCraftSlots : Set.of());
      }
   }

   @Override
   public void mouseMoved(double x, double y) {
      this.updateHoveredSlot(this.pickSlot(x, y));
      super.mouseMoved(x, y);
   }

   private void updateHoveredSlot(Slot next) {
      if (this.hoveredSlot != next) {
         Slot previous = this.hoveredSlot;
         this.hoveredSlot = next;
         if (previous != null) {
            ((ContainerHoverAccessor)(Object)this).matinventory$stopHovering(previous);
         }
      }
   }

   @Override
   protected boolean hasClickedOutside(double x, double y, int left, int top) {
      if (x < 0.0 || y < 0.0 || x >= this.width || y >= this.height || !this.camera.raycast(x, y)) {
         return true;
      } else {
         return this.layout.pick(this.camera.hit.x, this.camera.hit.z, this.camera) != null
            ? false
            : this.layout
               .fabricCompartments()
               .stream()
               .noneMatch(
                  c -> this.camera.hit.x >= c.x0() - 0.14F
                     && this.camera.hit.x <= c.x1() + 0.14F
                     && this.camera.hit.z >= c.z0() - 0.14F
                     && this.camera.hit.z <= c.z1() + 0.14F
               );
      }
   }

   @Override
   public void extractBackground(GuiGraphicsExtractor g, int x, int y, float delta) {
      g.fill(0, 0, this.width, this.height, -15262953);
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float delta) {
      try {
         this.renderMat(g, x, y, delta);
      } catch (RuntimeException ex) {
         MatInventoryClient.LOG.error("Cannot render inventory screen; returning to vanilla", ex);
         MatInventoryClient.renderFailed = true;
      }
   }

   private void renderMat(GuiGraphicsExtractor g, int x, int y, float delta) {
      this.animation.frame();
      if (this.animation.closing()) {
         x = this.closePointerX;
         y = this.closePointerY;
      }

      this.items.world.advanceLeds(this.animation.dt, (lx, lz) -> this.ledAudio.flash(this.camera, lx, lz));
      if (!this.animation.closing()) {
         this.ledAudio.sync(this, () -> this.items.world.leds.enabled());
      }

      if (!this.openingBlankShown) {
         this.openingBlankShown = true;
         this.updateCamera(x, y);
         this.hoveredSlot = null;
         MatSceneRenderer.draw(g, this.items.empty(this.layout, this.camera, this.animation));
         this.overlay.effects(g, this.width, this.height);
      } else {
         this.denied = Math.max(0.0F, this.denied - this.animation.dt);
         this.capacityTime = Math.max(0.0F, this.capacityTime - this.animation.dt);
         if (!this.openingPrepared && !this.openingSurfaceReady) {
            this.openingSurfaceReady = this.items.prepareOpening(this.menu, this.layout, this.animation);
            this.updateCamera(x, y);
            this.hoveredSlot = null;
            MatSceneRenderer.draw(g, this.items.empty(this.layout, this.camera, this.animation));
            this.overlay.effects(g, this.width, this.height);
         } else {
            if (this.openingPrepared) {
               this.items
                  .arrange(
                     this.menu,
                     this.layout,
                     this.animation,
                     this.animation.closing()
                        || this.isQuickCrafting && !this.menu.getCarried().isEmpty()
                        || this.toolGestureActive()
                        || this.foodGestureActive()
                        || this.materialGestureActive()
                        || this.equipmentGestureActive()
                  );
            }

            if (!this.openingPrepared) {
               float left = Float.POSITIVE_INFINITY;
               float right = Float.NEGATIVE_INFINITY;

               for (MatLayout.Place p : this.layout.places()) {
                  if (p.visible && this.menu.getSlot(p.slot()).hasItem()) {
                     left = Math.min(left, p.x());
                     right = Math.max(right, p.x());
                  }
               }

               if (Float.isFinite(left)) {
                  this.animation.itemRange(left, right);
               }

               this.animation.itemsPrepared(Float.isFinite(left));
               this.openingPrepared = true;
            }

            this.transitionAudio.update(this, this.animation);
            this.updateCamera(x, y);
            this.animation.revealWhenReady();
            this.updateHoveredSlot(this.pickSlot(x, y));
            if (!this.animation.closing()) {
               this.itemAudio.land(this.menu, this.layout, this.animation);
            }

            this.itemAudio
               .hover(this.hoveredSlot == null ? -1 : this.hoveredSlot.index, this.hoveredSlot == null ? ItemStack.EMPTY : this.hoveredSlot.getItem());
            boolean onMatButton = this.animation.itemsSettled() && !this.recipePanelAt(x, y) && LedController.hitMatButton(this.camera, x, y);
            boolean onGrid = this.animation.itemsSettled() && !this.recipePanelAt(x, y) && GridController.hit(this.camera, x, y);
            this.gridHover = this.gridHover + ((onGrid ? 1 : 0) - this.gridHover) * (1.0F - (float)Math.exp(-this.animation.dt * 14.0F));
            if (this.gridHover < 0.001F) {
               this.gridHover = 0.0F;
            }

            this.matHover = this.matHover + ((onMatButton ? 1 : 0) - this.matHover) * (1.0F - (float)Math.exp(-this.animation.dt * 14.0F));
            if (this.matHover < 0.001F) {
               this.matHover = 0.0F;
            }

            boolean onController = this.animation.itemsSettled() && !this.recipePanelAt(x, y) && LedController.hit(this.camera, x, y, true);
            this.controllerHover = this.controllerHover
               + ((onController ? 1 : 0) - this.controllerHover) * (1.0F - (float)Math.exp(-this.animation.dt * 14.0F));
            if (this.controllerHover < 0.001F) {
               this.controllerHover = 0.0F;
            }

            boolean onBook = this.animation.itemsSettled() && !this.recipePanelAt(x, y) && RecipeBookProp.hit(this.camera, x, y);
            this.bookHover = this.bookHover + ((onBook ? 1 : 0) - this.bookHover) * (1.0F - (float)Math.exp(-this.animation.dt * 14.0F));
            if (this.bookHover < 0.001F) {
               this.bookHover = 0.0F;
            }

            try {
               int visualHover = this.hoveredSlot == null ? this.overlay.cycleOwnerAt(x, y) : this.hoveredSlot.index;
               if (visualHover >= 0 && this.menu.getCarried().isEmpty() && this.canCycle(this.layout.place(visualHover))) {
                  visualHover = this.overlay.cycleGlow(this.layout, visualHover);
               }

               MatSceneState scene = this.items
                  .extract(this.menu, this.layout, this.camera, this.animation, visualHover, MatInventoryClient.config.glow, 0.0F, x, y);
               scene.shake = this.denied * (float)Math.sin(this.animation.elapsed * 90.0F) * 0.16F;
               scene.controllerHover = this.controllerHover;
               scene.bookHover = this.bookHover;
               scene.capacityZone = null;
               scene.capacityStrength = 0.0F;
               if (this.capacityTime > 0.0F) {
                  scene.capacityZone = this.layout.capacityZone(this.capacitySector);
                  float wave = (float)Math.cos((1.2F - this.capacityTime) * Math.PI * 3.0);
                  scene.capacityStrength = (0.15F + 0.85F * wave * wave) * Math.min(1.0F, this.capacityTime / 0.15F);
               }

               MatSceneRenderer.draw(g, scene);
               this.overlay.carried(g, this.menu, scene, this.camera, this.width, this.height);
            } catch (RuntimeException ex) {
               MatInventoryClient.LOG.error("Cannot extract inventory scene", ex);
               MatInventoryClient.renderFailed = true;
            }

            this.overlay.effects(g, this.width, this.height);
            float craftEffect = this.craftFocus;
            if (MatInventoryClient.config.craftFocusEffect == MatConfig.CraftFocusEffect.VIGNETTE) {
               this.overlay.craftVignette(g, this.width, this.height, craftEffect);
            }

            if (this.animation.itemsSettled()) {
               this.overlay.counts(g, this.menu, this.layout, this.camera, this.width, this.height);
               this.overlay
                  .render(
                     g,
                     this.menu,
                     this.layout,
                     this.camera,
                     this.hoveredSlot == null ? -1 : this.hoveredSlot.index,
                     x,
                     y,
                     this.width,
                     this.height,
                     this.animation.elapsed,
                     this.hoveredSlot == null ? 0 : this.hoverActions(this.hoveredSlot.index, x, y)
                  );
               this.overlay.controllerLabel(g, this.camera, this.controllerHover, this.width, this.height, this.items.world.leds.enabled());
               this.overlay.matLabel(g, this.camera, this.matHover, this.width, this.height);
               this.overlay.gridLabel(g, this.camera, this.gridHover, this.width, this.height);
               this.overlay.bookLabel(g, this.camera, this.bookHover, this.width, this.height, this.recipeBook != null && this.recipeBook.isVisible());
               MatLayout.Place craftTarget = this.hoveredSlot == null ? null : this.layout.place(this.hoveredSlot.index);
               boolean craftPoint = this.menu.getCarried().isEmpty()
                  && InventoryCrafting.fits(this.menu)
                  && !this.recipePanelAt(x, y)
                  && craftTarget != null
                  && craftTarget.group() == MatLayout.Group.RESULT
                  && this.hoveredSlot.hasItem();
               this.craftHover = craftPoint ? this.craftHover + (1.0F - this.craftHover) * (1.0F - (float)Math.exp(-this.animation.dt * 12.0F)) : 0.0F;
               this.overlay.craftLabel(g, this.layout, this.craftHover, this.width, this.height);
               if (this.recipeBook != null && this.recipeBook.isVisible()) {
                  g.nextStratum();
                  this.recipeBook.extractRenderState(g, x, y, delta);
                  g.nextStratum();
                  this.recipeBook.extractTooltip(g, x, y, null);
               }
            }

            if (MatInventoryClient.config.craftFocusEffect == MatConfig.CraftFocusEffect.CINEMA_BARS) {
               this.overlay.craftCinemaBars(g, this.width, this.height, craftEffect);
            }
         }
      }
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean twice) {
      if (this.openingPrepared && this.animation.itemsSettled()) {
         if (this.recipeBook != null && this.recipeBook.isVisible()) {
            if (this.recipeBook.mouseClicked(event, twice)) {
               this.setFocused(this.recipeBook);
               this.recipeRelease = true;
               return true;
            }

            if (this.recipePanelAt(event.x(), event.y())) {
               this.recipeRelease = true;
               return true;
            }
         }

         if (RecipeBookProp.hit(this.camera, event.x(), event.y())) {
            this.recipeRelease = true;
            if (MatInput.button(event) == 0 && this.recipeBook != null) {
               this.recipeBook.toggleVisibility();
               this.setFocused(this.recipeBook);
               this.overlay.actionConfirmed();
               this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(MatSounds.LED_BUTTON_CLICK, 1.0F, 0.12F));
            }

            return true;
         } else {
            boolean gridButton = GridController.hit(this.camera, event.x(), event.y());
            if (!gridButton && !GridController.bodyHit(this.camera, event.x(), event.y())) {
               boolean matButton = LedController.hitMatButton(this.camera, event.x(), event.y());
               if (matButton) {
                  this.controllerRelease = true;
                  if (MatInput.button(event) == 0) {
                     this.overlay.actionConfirmed();
                     MatInventoryClient.config.matColor = MatPalette.next(MatInventoryClient.config.matColor);
                     MatInventoryClient.config.save();
                     this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(MatSounds.MAT_DROP, 1.0F, 0.7F));
                     this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(MatSounds.LED_BUTTON_CLICK, 1.0F, 0.12F));
                  }

                  return true;
               } else {
                  boolean button = LedController.hit(this.camera, event.x(), event.y(), true);
                  if (button || LedController.hit(this.camera, event.x(), event.y(), false)) {
                     this.controllerRelease = true;
                     if (button && (MatInput.button(event) == 0 || MatInput.button(event) == 1)) {
                        this.overlay.actionConfirmed();
                        if (MatInput.button(event) == 0) {
                           this.items.world.leds.toggle();
                           MatInventoryClient.config.ledEnabled = this.items.world.leds.enabled();
                           MatInventoryClient.config.save();
                           this.ledAudio.sync(this, () -> this.items.world.leds.enabled());
                        } else {
                           MatInventoryClient.config.ledColor = LedController.next(MatInventoryClient.config.ledColor);
                           MatInventoryClient.config.save();
                        }

                        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(MatSounds.LED_BUTTON_CLICK, 1.0F, 0.12F));
                     }

                     return true;
                  } else {
                     if (!this.menu.getCarried().isEmpty()
                        && (MatInput.button(event) == 0 || MatInput.button(event) == 1)
                        && this.hasClickedOutside(event.x(), event.y(), 0, 0)) {
                        this.slotClicked(null, -999, MatInput.button(event), ContainerInput.PICKUP);
                        this.isQuickCrafting = false;
                        this.quickCraftSlots.clear();
                        this.rejectedRelease = true;
                        return true;
                     }

                     if (MatInput.button(event) == 1 && event.hasControlDown()) {
                        Slot slot = this.pickSlot(event.x(), event.y());
                        if (slot != null && EquipmentKinds.playerSlot(slot.getItem()) >= 0) {
                           this.equipmentToggleRelease = true;
                           this.toggleEquipment(slot);
                           return true;
                        }
                     }

                     Slot craftSlot = this.pickSlot(event.x(), event.y());
                     if (!this.menu.getCarried().isEmpty()
                        && !event.hasAltDown()
                        && !this.separatePlacement
                        && craftSlot != null
                        && this.layout.place(craftSlot.index).group() == MatLayout.Group.CRAFT) {
                        this.slotClicked(craftSlot, craftSlot.index, MatInput.button(event), ContainerInput.PICKUP);
                        this.rejectedRelease = true;
                        this.isQuickCrafting = false;
                        this.quickCraftSlots.clear();
                        return true;
                     }

                     if (!this.menu.getCarried().isEmpty()
                        || (MatInput.button(event) != 1 || !this.craftArea(event.x(), event.y()))
                           && (MatInput.button(event) != 0 || craftSlot == null || this.layout.place(craftSlot.index).group() != MatLayout.Group.RESULT)) {
                        if (this.nativeItemClick(event)) {
                           this.rejectedRelease = true;
                           return true;
                        }

                        if (!this.menu.getCarried().isEmpty()
                           && this.camera.raycast(event.x(), event.y())
                           && this.layout.compartmentAt(this.camera.hit.x, this.camera.hit.z) != null
                           && !this.layout.acceptsStorage(this.menu.getCarried(), this.camera.hit.x, this.camera.hit.z)) {
                           this.denied = 0.18F;
                           this.rejectedRelease = true;
                           return true;
                        }

                        if (this.mergeStackClick(event)) {
                           this.rejectedRelease = true;
                           return true;
                        }

                        if (!MatInventoryClient.config.gridEnabled
                           && MatInput.button(event) == 0
                           && !event.hasAltDown()
                           && !this.separatePlacement
                           && this.toolGestureActive()
                           && this.moveToolPile(event)) {
                           this.rejectedRelease = true;
                           return true;
                        }

                        if (!MatInventoryClient.config.gridEnabled
                           && !this.menu.getCarried().isEmpty()
                           && (MatInput.button(event) == 0 || MatInput.button(event) == 1)
                           && this.camera.raycast(event.x(), event.y())
                           && this.layout.manualStorageAt(this.camera.hit.x, this.camera.hit.z)) {
                           MatLayout.Place pointed = this.layout.pick(this.camera.hit.x, this.camera.hit.z, this.camera);
                           if ((pointed == null || !this.menu.getSlot(pointed.slot()).hasItem()) && this.separateClick(event)) {
                              this.separateRelease = true;
                              return true;
                           }
                        }

                        if ((
                              MatInput.button(event) == 1 && (event.hasAltDown() || !this.menu.getCarried().isEmpty())
                                 || this.separatePlacement && (MatInput.button(event) == 0 || MatInput.button(event) == 1)
                           )
                           && this.separateClick(event)) {
                           this.separateRelease = true;
                           return true;
                        }

                        if ((MatInput.button(event) == 0 || MatInput.button(event) == 1) && this.placeFlexible(event)) {
                           this.rejectedRelease = true;
                           return true;
                        }

                        if (MatInput.button(event) == 0 && this.reorderStorage(event)) {
                           this.storageReleaseHandled = true;
                           return true;
                        }

                        if (MatInput.button(event) == 0 && this.layout.equipmentCellFor(this.menu.getCarried()) >= 0 && this.reorderEquipment(event)) {
                           this.equipmentReleaseHandled = true;
                           return true;
                        }

                        this.materialDestination = -1;
                        if (MatInput.button(event) == 0 && MaterialKinds.isMaterial(this.menu.getCarried())) {
                           MatLayout.MaterialCell target = this.materialTarget(event);
                           if (target != null) {
                              this.materialDestination = target.index();
                           }

                           if (this.reorderMaterial(event)) {
                              this.materialReleaseHandled = true;
                              return true;
                           }
                        }

                        this.foodDestination = -1;
                        if (MatInput.button(event) == 0 && ConsumableKinds.isConsumable(this.menu.getCarried()) && this.camera.raycast(event.x(), event.y())) {
                           MatLayout.FoodCell target = this.foodTarget(event);
                           if (target != null) {
                              this.foodDestination = target.index();
                           }

                           if (this.reorderFood(event)) {
                              this.foodReleaseHandled = true;
                              return true;
                           }
                        }

                        if (MatInput.button(event) == 2 && this.overlay.cycleOwnerAt(event.x(), event.y()) >= 0) {
                           MatLayout.Place owner = this.layout.place(this.overlay.cycleOwnerAt(event.x(), event.y()));
                           if (this.canCycle(owner) && this.layout.selectCycleItem(owner.slot(), this.overlay.cycleSelection(this.layout, owner.slot()))) {
                              this.overlay.cycleConfirmed(this.menu);
                           }

                           this.toolMiddleHandled = true;
                           return true;
                        } else {
                           if (MatInput.button(event) == 2 && this.camera.raycast(event.x(), event.y())) {
                              MatLayout.Place tool = this.layout.pick(this.camera.hit.x, this.camera.hit.z, this.camera);
                              if (tool != null && tool.equipmentCell >= 0) {
                                 if (this.canCycle(tool) && this.layout.selectCycleItem(tool.slot(), this.overlay.cycleSelection(this.layout, tool.slot()))) {
                                    this.overlay.cycleConfirmed(this.menu);
                                 }

                                 this.toolMiddleHandled = true;
                                 return true;
                              }

                              if (tool != null && (tool.blockTotal > 0 || tool.foodCell >= 0 || tool.materialCell >= 0)) {
                                 this.toolMiddleHandled = true;
                                 return true;
                              }

                              if (tool != null && tool.toolCell >= 0 && this.menu.getSlot(tool.slot()).hasItem()) {
                                 if (this.canCycle(tool) && this.layout.selectCycleItem(tool.slot(), this.overlay.cycleSelection(this.layout, tool.slot()))) {
                                    this.overlay.cycleConfirmed(this.menu);
                                 }

                                 this.toolMiddleHandled = true;
                                 return true;
                              }
                           }

                           if (MatInput.button(event) == 0 && MatLayout.toolKind(this.menu.getCarried()) != null && this.reorderTool(event)) {
                              this.toolReleaseHandled = true;
                              return true;
                           }

                           this.toolPressX = event.x();
                           this.toolPressY = event.y();
                           if (!this.menu.getCarried().isEmpty()
                              && !this.hasClickedOutside(event.x(), event.y(), 0, 0)
                              && this.pickSlot(event.x(), event.y()) == null) {
                              this.rejectedRelease = true;
                              this.isQuickCrafting = false;
                              this.quickCraftSlots.clear();
                              if (this.camera.raycast(event.x(), event.y())) {
                                 ItemPresentationData.Compartment c = this.layout.compartmentAt(this.camera.hit.x, this.camera.hit.z);
                                 if (c != null
                                    && (
                                       c.category() == ItemCategoryClassifier.classify(this.menu.getCarried()).category()
                                          || MatLayout.materialCategory(c.category()) && MaterialKinds.isMaterial(this.menu.getCarried())
                                    )
                                    && this.storageFull(this.menu.getCarried())) {
                                    this.warnCapacity(c.id());
                                 }
                              }

                              return true;
                           } else {
                              return super.mouseClicked(event, twice);
                           }
                        }
                     } else {
                        this.craftRelease = true;
                        if (MatInput.button(event) == 1) {
                           this.craft(event.hasShiftDown());
                        } else if (craftSlot.hasItem()) {
                           this.slotClicked(craftSlot, craftSlot.index, 0, event.hasShiftDown() ? ContainerInput.QUICK_MOVE : ContainerInput.PICKUP);
                        }

                        return true;
                     }
                  }
               }
            } else {
               this.controllerRelease = true;
               if (gridButton && MatInput.button(event) == 0) {
                  this.overlay.actionConfirmed();
                  MatInventoryClient.config.gridEnabled = !MatInventoryClient.config.gridEnabled;
                  MatInventoryClient.config.save();
                  this.layout.gridChanged();
                  this.isQuickCrafting = false;
                  this.quickCraftSlots.clear();
                  this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(MatSounds.LED_BUTTON_CLICK, 1.0F, 0.12F));
               }

               return true;
            }
         }
      } else {
         return true;
      }
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent event) {
      if (!this.animation.itemsSettled()) {
         return true;
      }

      if (this.separateRelease) {
         this.separateRelease = false;
         return true;
      }

      if (this.recipeRelease) {
         this.recipeRelease = false;
         if (this.recipeBook != null) {
            this.recipeBook.mouseReleased(event);
         }

         return true;
      } else {
         if (this.controllerRelease) {
            this.controllerRelease = false;
            return true;
         }

         if (this.equipmentToggleRelease && MatInput.button(event) == 1) {
            this.equipmentToggleRelease = false;
            return true;
         }

         if (this.storageReleaseHandled && MatInput.button(event) == 0) {
            this.storageReleaseHandled = false;
            return true;
         }

         if (this.equipmentReleaseHandled && MatInput.button(event) == 0) {
            this.equipmentReleaseHandled = false;
            return true;
         }

         if (this.materialReleaseHandled && MatInput.button(event) == 0) {
            this.materialReleaseHandled = false;
            return true;
         }

         if (this.foodReleaseHandled && MatInput.button(event) == 0) {
            this.foodReleaseHandled = false;
            return true;
         }

         if (!this.rejectedRelease || MatInput.button(event) != 0 && MatInput.button(event) != 1) {
            if (this.toolMiddleHandled && MatInput.button(event) == 2) {
               this.toolMiddleHandled = false;
               return true;
            }

            if (!this.craftRelease || MatInput.button(event) != 0 && MatInput.button(event) != 1) {
               if (this.toolReleaseHandled && MatInput.button(event) == 0) {
                  this.toolReleaseHandled = false;
                  return true;
               } else if (MatInput.button(event) == 0
                  && !MatInventoryClient.config.gridEnabled
                  && !this.separatePlacement
                  && this.toolGestureActive()
                  && Math.hypot(event.x() - this.toolPressX, event.y() - this.toolPressY) > 4.0
                  && this.moveToolPile(event)) {
                  super.mouseReleased(event);
                  return true;
               } else if (Math.hypot(event.x() - this.toolPressX, event.y() - this.toolPressY) > 4.0 && this.nativeItemClick(event)) {
                  return true;
               } else if (MatInput.button(event) == 0
                  && this.storageGestureActive()
                  && Math.hypot(event.x() - this.toolPressX, event.y() - this.toolPressY) > 4.0
                  && this.reorderStorage(event)) {
                  super.mouseReleased(event);
                  return true;
               } else if (MatInput.button(event) == 0
                  && this.equipmentGestureActive()
                  && Math.hypot(event.x() - this.toolPressX, event.y() - this.toolPressY) > 4.0
                  && this.reorderEquipment(event)) {
                  super.mouseReleased(event);
                  return true;
               } else if (MatInput.button(event) == 0
                  && this.materialGestureActive()
                  && Math.hypot(event.x() - this.toolPressX, event.y() - this.toolPressY) > 4.0
                  && this.reorderMaterial(event)) {
                  super.mouseReleased(event);
                  return true;
               } else if (MatInput.button(event) == 0
                  && this.foodGestureActive()
                  && Math.hypot(event.x() - this.toolPressX, event.y() - this.toolPressY) > 4.0
                  && this.reorderFood(event)) {
                  super.mouseReleased(event);
                  return true;
               } else if (MatInput.button(event) == 0
                  && this.toolGestureActive()
                  && Math.hypot(event.x() - this.toolPressX, event.y() - this.toolPressY) > 4.0
                  && this.reorderTool(event)) {
                  super.mouseReleased(event);
                  return true;
               } else {
                  return super.mouseReleased(event);
               }
            } else {
               this.craftRelease = false;
               return true;
            }
         } else {
            this.rejectedRelease = false;
            return true;
         }
      }
   }

   private boolean storageGestureActive() {
      return this.storageOrigin >= 0
         && !this.menu.getCarried().isEmpty()
         && ItemStack.isSameItemSameComponents(this.heldStorage, this.menu.getCarried())
         && this.menu.getSlot(this.storageOrigin).getItem().isEmpty();
   }

   private boolean nativeItemClick(MouseButtonEvent event) {
      if ((MatInput.button(event) == 0 || MatInput.button(event) == 1)
         && !event.hasAltDown()
         && !event.hasControlDown()
         && !event.hasShiftDown()
         && !this.separatePlacement
         && !this.menu.getCarried().isEmpty()) {
         Slot slot = this.pickSlot(event.x(), event.y());
         if (slot != null && slot.hasItem() && NativeItemClicks.applies(this.menu.getCarried(), slot.getItem())) {
            this.slotClicked(slot, slot.index, MatInput.button(event), ContainerInput.PICKUP);
            this.toolOrigin = this.foodOrigin = this.materialOrigin = this.equipmentOrigin = this.storageOrigin = -1;
            this.foodDestination = this.materialDestination = -1;
            this.heldStorage = ItemStack.EMPTY;
            this.isQuickCrafting = false;
            this.quickCraftSlots.clear();
            this.setDragging(false);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private boolean separateClick(MouseButtonEvent event) {
      if (this.menu.getCarried().isEmpty()) {
         Slot source = this.pickSlot(event.x(), event.y());
         if (source != null && source.hasItem() && source.mayPickup(this.minecraft.player)) {
            this.separateOrigin = source.index;
            this.independentPickup = true;

            try {
               this.slotClicked(source, source.index, 0, ContainerInput.PICKUP);
            } finally {
               this.independentPickup = false;
            }

            this.separatePlacement = !this.menu.getCarried().isEmpty();
            this.layout.separatePreview(this.separatePlacement);
            this.isQuickCrafting = false;
            this.quickCraftSlots.clear();
            return true;
         } else {
            return true;
         }
      } else {
         boolean single = MatInput.button(event) == 1 && !event.hasAltDown();
         boolean keepSeparate = this.separatePlacement;
         this.separatePlacement = !single || keepSeparate;
         this.layout.separatePreview(true);

         try {
            if (!this.camera.raycast(event.x(), event.y())) {
               return true;
            }

            float x = this.camera.hit.x;
            float z = this.camera.hit.z;
            ItemPresentationData.Compartment area = this.layout.compartmentAt(x, z);
            if (area == null) {
               return true;
            }

            if (area.category() != ItemCategoryClassifier.Category.HOTBAR && area.category() != ItemCategoryClassifier.Category.CRAFT) {
               MatLayout.SeparateDestination destination = this.layout
                  .separateDestination(
                     this.menu,
                     this.separateOrigin,
                     x,
                     z,
                     this.items.separateFootprint(single ? this.menu.getCarried().copyWithCount(1) : this.menu.getCarried()),
                     single
                  );
               if (destination == null) {
                  this.denied = 0.18F;
                  return true;
               }

               Slot slot = destination.slot();
               int before = this.menu.getCarried().getCount();
               float presentationScale = this.items.separateScale(this.menu.getCarried());
               this.slotClicked(slot, slot.index, event.hasAltDown() ? 0 : MatInput.button(event), ContainerInput.PICKUP);
               if (slot.hasItem() && (this.menu.getCarried().isEmpty() || this.menu.getCarried().getCount() < before)) {
                  this.layout.detach(slot, destination.x(), destination.z(), presentationScale);
               }

               this.separatePlacement = !this.menu.getCarried().isEmpty() && (!single || keepSeparate);
               this.layout.separatePreview(this.separatePlacement);
               if (!this.separatePlacement) {
                  this.separateOrigin = -1;
               }

               this.isQuickCrafting = false;
               this.quickCraftSlots.clear();
               return true;
            } else {
               return false;
            }
         } finally {
            this.layout.separatePreview(this.separatePlacement);
         }
      }
   }

   private boolean mergeStackClick(MouseButtonEvent event) {
      if ((MatInput.button(event) == 0 || MatInput.button(event) == 1)
         && !event.hasAltDown()
         && !event.hasShiftDown()
         && !this.menu.getCarried().isEmpty()
         && this.camera.raycast(event.x(), event.y())) {
         MatLayout.Place place = this.layout.pick(this.camera.hit.x, this.camera.hit.z, this.camera);
         if (place != null && place.group() == MatLayout.Group.SUPPLIES) {
            Slot slot = this.menu.getSlot(place.slot());
            if (slot.hasItem() && slot.mayPlace(this.menu.getCarried()) && ItemStack.isSameItemSameComponents(slot.getItem(), this.menu.getCarried())) {
               this.slotClicked(slot, slot.index, MatInput.button(event), ContainerInput.PICKUP);
               this.isQuickCrafting = false;
               this.quickCraftSlots.clear();
               this.setDragging(false);
               return true;
            } else {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private boolean reorderStorage(MouseButtonEvent event) {
      if (this.placeFlexible(event)) {
         return true;
      }

      ItemStack carried = this.menu.getCarried();
      boolean vehicle = VehicleKinds.isVehicle(carried);
      if (!carried.isEmpty() && (vehicle || BlockKinds.isBlock(carried)) && this.camera.raycast(event.x(), event.y())) {
         int target = -1;
         float x = 0.0F;
         float z = 0.0F;
         MatLayout.Place picked = this.layout.pick(this.camera.hit.x, this.camera.hit.z, this.camera);
         if (vehicle) {
            MatLayout.VehicleCell cell = picked != null && picked.vehicleCell >= 0
               ? this.layout.vehicleCells().get(picked.vehicleCell)
               : this.layout.vehicleCellAt(this.camera.hit.x, this.camera.hit.z);
            if (cell != null) {
               target = cell.index();
               x = (cell.x0() + cell.x1()) * 0.5F;
               z = (cell.z0() + cell.z1()) * 0.5F;
            }
         } else {
            MatLayout.BlockCell cell = this.layout.blockCellAt(this.camera.hit.x, this.camera.hit.z);
            if (cell == null && picked != null && picked.blockCell >= 0) {
               cell = this.layout.blockCell(picked.blockCell);
            }

            if (cell != null) {
               target = cell.index();
               x = (cell.x0() + cell.x1()) * 0.5F;
               z = (cell.z0() + cell.z1()) * 0.5F;
            }
         }

         if (target < 0) {
            return false;
         }

         Slot destination = null;

         for (MatLayout.Place p : this.layout.storagePlaces()) {
            if (p.visible && (vehicle ? p.vehicleCell : p.blockCell) == target && this.menu.getSlot(p.slot()).hasItem()) {
               destination = this.menu.getSlot(p.slot());
               break;
            }
         }

         if (!vehicle && destination != null && destination.hasItem() && ItemStack.isSameItemSameComponents(carried, destination.getItem())) {
            this.slotClicked(destination, destination.index, 0, ContainerInput.PICKUP);
            this.isQuickCrafting = false;
            this.quickCraftSlots.clear();
            return true;
         }

         MatLayout.Place source = this.storageGestureActive() ? this.layout.place(this.storageOrigin) : null;
         Slot origin = source != null && source.group() == MatLayout.Group.SUPPLIES ? this.menu.getSlot(this.storageOrigin) : null;
         if (origin == null) {
            for (MatLayout.Place p : this.layout.storagePlaces()) {
               Slot s = this.menu.getSlot(p.slot());
               if (!s.hasItem() && s.mayPlace(carried)) {
                  origin = s;
                  break;
               }
            }
         }

         if (origin != null && origin.mayPlace(carried) && this.layout.dropTarget(this.menu, x, z) != null) {
            this.slotClicked(origin, origin.index, 0, ContainerInput.PICKUP);
            if (!this.menu.getCarried().isEmpty()) {
               this.isQuickCrafting = false;
               this.quickCraftSlots.clear();
               return true;
            } else {
               this.layout.placeStorage(this.menu, origin, target, vehicle);
               this.isQuickCrafting = false;
               this.quickCraftSlots.clear();
               return true;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private boolean placeFlexible(MouseButtonEvent event) {
      ItemStack carried = this.menu.getCarried();
      if (ItemCategoryClassifier.flexible(carried) && this.camera.raycast(event.x(), event.y())) {
         float x = this.camera.hit.x;
         float z = this.camera.hit.z;
         Slot destination = this.layout.flexibleTarget(this.menu, x, z, Set.of());
         if (destination == null) {
            return false;
         }

         ItemStack stack = carried.copy();
         int before = carried.getCount();
         this.slotClicked(destination, destination.index, MatInput.button(event), ContainerInput.PICKUP);
         if (this.menu.getCarried().isEmpty() || this.menu.getCarried().getCount() < before) {
            this.layout.placeFlexible(stack, x, z);
         }

         this.isQuickCrafting = false;
         this.quickCraftSlots.clear();
         return true;
      } else {
         return false;
      }
   }

   private boolean equipmentGestureActive() {
      return this.equipmentOrigin >= 0
         && !this.menu.getCarried().isEmpty()
         && Objects.equals(this.heldEquipment, EquipmentKinds.kind(this.menu.getCarried()))
         && this.menu.getSlot(this.equipmentOrigin).getItem().isEmpty();
   }

   private boolean canCycle(MatLayout.Place place) {
      if (place == null || !this.menu.getCarried().isEmpty() || this.isQuickCrafting) {
         return false;
      } else if (place.equipmentCell >= 0) {
         return this.layout.equipmentInCell(place.equipmentCell).size() > 1;
      } else {
         return place.blockTotal <= 0 && place.foodCell < 0 && place.materialCell < 0
            ? place.toolCell >= 0 && this.menu.getSlot(place.slot()).hasItem() && this.layout.toolsInCell(place.toolCell).size() > 1
            : false;
      }
   }

   public int hoverActions(int slot, double x, double y) {
      int actions = 0;
      if (this.camera.raycast(x, y) && this.canCycle(this.layout.pick(this.camera.hit.x, this.camera.hit.z, this.camera))) {
         actions |= 1;
      }

      Slot source = this.menu.getSlot(slot);
      if (this.canEat(source)) {
         actions |= 8;
      }

      if (this.equipmentTarget(source) != null) {
         actions |= EquipmentKinds.equipped(source) ? 4 : 2;
      }

      return actions;
   }

   private Slot equipmentTarget(Slot source) {
      if (this.menu.getCarried().isEmpty() && !this.isQuickCrafting && source.getItem().getCount() == 1) {
         int nativeIndex = EquipmentKinds.playerSlot(source.getItem());
         if (nativeIndex < 0) {
            return null;
         }

         Slot equipped = null;

         for (Slot s : this.menu.slots) {
            if (s.container == this.minecraft.player.getInventory() && s.getContainerSlot() == nativeIndex) {
               equipped = s;
               break;
            }
         }

         if (equipped != null && source.mayPickup(this.minecraft.player) && equipped.mayPickup(this.minecraft.player)) {
            Slot target = equipped;
            if (source == equipped) {
               Integer previous = this.equipmentReturns.get(equipped.index);
               target = previous == null ? null : this.menu.getSlot(previous);
               if (target == null
                  || this.layout.place(target.index).group() == MatLayout.Group.HOTBAR
                  || target.hasItem()
                  || !target.mayPlace(source.getItem())) {
                  target = null;

                  for (MatLayout.Place p : this.layout.storagePlaces()) {
                     Slot s = this.menu.getSlot(p.slot());
                     if (!s.hasItem() && s.mayPlace(source.getItem())) {
                        target = s;
                        break;
                     }
                  }
               }

               if (target == null) {
                  return null;
               }
            } else if (!target.mayPlace(source.getItem()) || !target.getItem().isEmpty() && !source.mayPlace(target.getItem())) {
               return null;
            }

            return target;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private void toggleEquipment(Slot source) {
      Slot target = this.equipmentTarget(source);
      if (target == null) {
         if (EquipmentKinds.equipped(source)
            && this.menu.getCarried().isEmpty()
            && !this.isQuickCrafting
            && source.getItem().getCount() == 1
            && source.mayPickup(this.minecraft.player)) {
            this.denied = 0.18F;
         }
      } else {
         boolean wasEquipped = EquipmentKinds.equipped(source);
         int a = source.index;
         int b = target.index;
         this.slotClicked(source, a, 0, ContainerInput.PICKUP);
         this.slotClicked(target, b, 0, ContainerInput.PICKUP);
         if (!this.menu.getCarried().isEmpty()) {
            this.slotClicked(source, a, 0, ContainerInput.PICKUP);
         }

         if (this.menu.getCarried().isEmpty()) {
            this.layout.remapEquipment(a, b);
            this.overlay.actionConfirmed();
            if (!wasEquipped) {
               this.equipmentReturns.put(b, a);
            }
         }
      }
   }

   private boolean reorderEquipment(MouseButtonEvent event) {
      if (!this.camera.raycast(event.x(), event.y())) {
         return false;
      }

      MatLayout.EquipmentCell target = this.layout.equipmentCellAt(this.camera.hit.x, this.camera.hit.z);
      MatLayout.Place item = this.layout.pick(this.camera.hit.x, this.camera.hit.z, this.camera);
      if (item != null && this.menu.getSlot(item.slot()).hasItem() && item.equipmentCell >= 0) {
         target = this.layout.equipmentCell(item.equipmentCell);
      }

      if (target == null) {
         return false;
      }

      int from = this.layout.equipmentCellFor(this.menu.getCarried());
      if (from < 0) {
         return false;
      }

      if (!MatLayout.equipmentDestination(from, target.index())) {
         return false;
      }

      MatLayout.Place source = this.equipmentGestureActive() ? this.layout.place(this.equipmentOrigin) : null;
      Slot origin = source == null || source.group() != MatLayout.Group.SUPPLIES && source.group() != MatLayout.Group.EQUIPMENT
         ? this.layout.dropTarget(this.menu, (target.x0() + target.x1()) * 0.5F, (target.z0() + target.z1()) * 0.5F)
         : this.menu.getSlot(this.equipmentOrigin);
      if (origin != null && origin.mayPlace(this.menu.getCarried())) {
         this.slotClicked(origin, origin.index, 0, ContainerInput.PICKUP);
         if (!this.menu.getCarried().isEmpty()) {
            this.isQuickCrafting = false;
            this.quickCraftSlots.clear();
            return true;
         } else {
            this.layout.swapEquipmentCells(from, target.index());
            this.isQuickCrafting = false;
            this.quickCraftSlots.clear();
            return true;
         }
      } else {
         return false;
      }
   }

   private boolean toolGestureActive() {
      return this.toolOrigin >= 0
         && !this.menu.getCarried().isEmpty()
         && Objects.equals(this.heldTool, MatLayout.toolKind(this.menu.getCarried()))
         && this.menu.getSlot(this.toolOrigin).getItem().isEmpty();
   }

   private boolean foodGestureActive() {
      return this.foodOrigin >= 0
         && !this.menu.getCarried().isEmpty()
         && Objects.equals(this.heldFood, ConsumableKinds.key(this.menu.getCarried()))
         && this.menu.getSlot(this.foodOrigin).getItem().isEmpty()
         && this.layout.place(this.foodOrigin).group() == MatLayout.Group.SUPPLIES;
   }

   private boolean materialGestureActive() {
      return this.materialOrigin >= 0
         && !this.menu.getCarried().isEmpty()
         && Objects.equals(this.heldMaterial, MaterialKinds.key(this.menu.getCarried()))
         && this.menu.getSlot(this.materialOrigin).getItem().isEmpty()
         && this.layout.place(this.materialOrigin).group() == MatLayout.Group.SUPPLIES;
   }

   private MatLayout.MaterialCell materialTarget(MouseButtonEvent event) {
      if (!this.camera.raycast(event.x(), event.y())) {
         return null;
      }

      MatLayout.MaterialCell target = this.layout.materialCellAt(this.camera.hit.x, this.camera.hit.z);
      MatLayout.Place item = this.layout.pick(this.camera.hit.x, this.camera.hit.z, this.camera);
      return item != null && this.menu.getSlot(item.slot()).hasItem() && item.materialCell >= 0 ? this.layout.materialCell(item.materialCell) : target;
   }

   private boolean reorderMaterial(MouseButtonEvent event) {
      if (!this.materialGestureActive()) {
         return false;
      }

      MatLayout.MaterialCell target = this.materialTarget(event);
      if (target == null) {
         return false;
      }

      int from = this.layout.place(this.materialOrigin).materialCell;
      if (from < 0) {
         return false;
      }

      Slot origin = this.menu.getSlot(this.materialOrigin);
      if (!origin.mayPlace(this.menu.getCarried())) {
         return false;
      }

      this.materialDestination = -1;
      this.slotClicked(origin, origin.index, 0, ContainerInput.PICKUP);
      if (!this.menu.getCarried().isEmpty()) {
         this.isQuickCrafting = false;
         this.quickCraftSlots.clear();
         return true;
      }

      if (from != target.index()) {
         this.layout.swapMaterialCells(from, target.index());
      }

      this.isQuickCrafting = false;
      this.quickCraftSlots.clear();
      return true;
   }

   private MatLayout.FoodCell foodTarget(MouseButtonEvent event) {
      if (!this.camera.raycast(event.x(), event.y())) {
         return null;
      }

      MatLayout.FoodCell target = this.layout.foodCellAt(this.camera.hit.x, this.camera.hit.z);
      MatLayout.Place item = this.layout.pick(this.camera.hit.x, this.camera.hit.z, this.camera);
      return item != null && this.menu.getSlot(item.slot()).hasItem() && item.foodCell >= 0 ? this.layout.foodCell(item.foodCell) : target;
   }

   private boolean reorderFood(MouseButtonEvent event) {
      if (!this.foodGestureActive()) {
         return false;
      }

      MatLayout.FoodCell target = this.foodTarget(event);
      if (target == null) {
         return false;
      }

      int from = this.layout.place(this.foodOrigin).foodCell;
      if (from < 0) {
         return false;
      }

      Slot origin = this.menu.getSlot(this.foodOrigin);
      if (!origin.mayPlace(this.menu.getCarried())) {
         return false;
      }

      this.foodDestination = -1;
      this.slotClicked(origin, origin.index, 0, ContainerInput.PICKUP);
      if (!this.menu.getCarried().isEmpty()) {
         this.isQuickCrafting = false;
         this.quickCraftSlots.clear();
         return true;
      }

      if (from != target.index()) {
         this.layout.swapFoodCells(from, target.index());
      }

      this.isQuickCrafting = false;
      this.quickCraftSlots.clear();
      return true;
   }

   private boolean moveToolPile(MouseButtonEvent event) {
      if (!this.camera.raycast(event.x(), event.y())) {
         return false;
      }

      MatLayout.Place source = this.layout.place(this.toolOrigin);
      if (source != null && source.group() == MatLayout.Group.SUPPLIES) {
         float[] point = this.layout.toolPileDestination(this.menu, this.toolOrigin, this.camera.hit.x, this.camera.hit.z);
         if (point == null) {
            return false;
         }

         String kind = this.heldTool;
         Slot origin = this.menu.getSlot(this.toolOrigin);
         this.slotClicked(origin, origin.index, 0, ContainerInput.PICKUP);
         if (this.menu.getCarried().isEmpty()) {
            this.layout.placeToolPile(this.menu, kind, point[0], point[1]);
         }

         this.isQuickCrafting = false;
         this.quickCraftSlots.clear();
         this.setDragging(false);
         return true;
      } else {
         return false;
      }
   }

   private boolean reorderTool(MouseButtonEvent event) {
      if (!this.camera.raycast(event.x(), event.y())) {
         return false;
      }

      float x = this.camera.hit.x;
      float z = this.camera.hit.z;
      MatLayout.ToolCell target = this.layout.toolDropCellAt(x, z);
      MatLayout.Place item = this.layout.pick(x, z, this.camera);
      if (item != null && this.menu.getSlot(item.slot()).hasItem() && item.toolCell >= 0) {
         target = this.layout.toolCell(item.toolCell);
      }

      if (target == null) {
         return false;
      }

      int from = this.layout.toolCellFor(this.menu.getCarried());
      int to = target.index();
      Slot origin = this.toolGestureActive() && this.layout.place(this.toolOrigin).group() == MatLayout.Group.SUPPLIES
         ? this.menu.getSlot(this.toolOrigin)
         : this.layout.dropTarget(this.menu, target.dropCenterX(), (target.z0() + target.z1()) * 0.5F);
      if (origin != null && origin.mayPlace(this.menu.getCarried())) {
         this.slotClicked(origin, origin.index, 0, ContainerInput.PICKUP);
         if (!this.menu.getCarried().isEmpty()) {
            this.isQuickCrafting = false;
            this.quickCraftSlots.clear();
            return true;
         } else {
            this.layout.swapToolCells(from, to);
            this.isQuickCrafting = false;
            this.quickCraftSlots.clear();
            return true;
         }
      } else {
         return false;
      }
   }

   @Override
   protected void slotClicked(Slot slot, int index, int button, ContainerInput type) {
      if (!this.animation.closing()) {
         ItemStack original = slot == null ? ItemStack.EMPTY : slot.getItem().copy();
         ItemStack cursor = this.menu.getCarried().copy();
         ItemStack attempted = type == ContainerInput.SWAP && button >= 0 && button < 9 ? this.minecraft.player.getInventory().getItem(button).copy() : original;
         int source = -1;
         int foodSource = -1;
         int materialSource = -1;
         int equipmentSource = -1;
         String kind = null;
         String foodKind = null;
         String materialKind = null;
         String equipmentKind = null;
         MatLayout.Place place = this.layout.place(index);
         List<MatLayout.Place> movingPile = List.of();
         int storageSource = type != ContainerInput.PICKUP
               || button != 0
               || !cursor.isEmpty()
               || slot == null
               || place == null
               || place.group() != MatLayout.Group.SUPPLIES && place.group() != MatLayout.Group.HOTBAR
               || !VehicleKinds.isVehicle(original) && !BlockKinds.isBlock(original) && !ItemCategoryClassifier.flexible(original)
            ? -1
            : index;
         if (type == ContainerInput.PICKUP && button == 0 && cursor.isEmpty() && slot != null && place != null) {
            equipmentKind = EquipmentKinds.kind(original);
            if (equipmentKind != null) {
               equipmentSource = index;
            }
         }

         if (type == ContainerInput.PICKUP
            && button == 0
            && this.menu.getCarried().isEmpty()
            && slot != null
            && place != null
            && (place.group() == MatLayout.Group.SUPPLIES || place.group() == MatLayout.Group.HOTBAR)) {
            kind = MatLayout.toolKind(slot.getItem());
            if (kind != null) {
               source = index;
            }

            foodKind = ConsumableKinds.key(slot.getItem());
            if (foodKind != null) {
               foodSource = index;
            }

            materialKind = MaterialKinds.key(slot.getItem());
            if (materialKind != null) {
               materialSource = index;
            }
         }

         boolean singleTool = type == ContainerInput.PICKUP
            && button == 0
            && cursor.isEmpty()
            && slot != null
            && MatLayout.toolKind(original) != null
            && original.getCount() == 2;
         super.slotClicked(slot, index, singleTool ? 1 : button, type);
         if (type == ContainerInput.PICKUP && !cursor.isEmpty() && this.menu.getCarried().isEmpty() && place != null && place.group() == MatLayout.Group.SUPPLIES) {
            this.layout.promoteTool(MatLayout.toolKind(cursor), index);
         }

         if (cursor.isEmpty() && !this.menu.getCarried().isEmpty() && type != ContainerInput.CLONE) {
            this.items.pickup(index);
            this.items.pickupToolPile(movingPile, index);
            this.itemAudio.pickup(this.menu.getCarried());
         } else if (!cursor.isEmpty()
            && (!ItemStack.isSameItemSameComponents(cursor, this.menu.getCarried()) || this.menu.getCarried().getCount() < cursor.getCount())) {
            this.itemAudio.place(cursor);
         }

         if (type == ContainerInput.PICKUP && cursor.isEmpty() && place != null && place.detachedPlacement && !this.menu.getCarried().isEmpty()) {
            this.separatePlacement = true;
            this.separateOrigin = index;
            this.layout.separatePreview(true);
         }

         if (this.menu.getCarried().isEmpty()) {
            this.separatePlacement = false;
            this.separateOrigin = -1;
            this.layout.separatePreview(false);
         }

         if (this.recipeBook != null) {
            this.recipeBook.slotClicked(slot);
         }

         if (storageSource >= 0 && !this.menu.getCarried().isEmpty()) {
            this.storageOrigin = storageSource;
            this.heldStorage = original.copy();
         }

         if (this.menu.getCarried().isEmpty() || !ItemStack.isSameItemSameComponents(this.heldStorage, this.menu.getCarried())) {
            this.storageOrigin = -1;
            this.heldStorage = ItemStack.EMPTY;
         }

         if (type == ContainerInput.PICKUP
            && button == 0
            && this.materialDestination >= 0
            && !cursor.isEmpty()
            && MaterialKinds.isMaterial(cursor)
            && original.isEmpty()
            && slot != null
            && !slot.getItem().isEmpty()
            && place != null
            && place.group() == MatLayout.Group.SUPPLIES) {
            this.layout.placeMaterial(slot, this.materialDestination);
         }

         if (type == ContainerInput.PICKUP
            && button == 0
            && this.foodDestination >= 0
            && !cursor.isEmpty()
            && ConsumableKinds.isConsumable(cursor)
            && original.isEmpty()
            && slot != null
            && !slot.getItem().isEmpty()
            && place != null
            && place.group() == MatLayout.Group.SUPPLIES) {
            this.layout.placeFood(slot, this.foodDestination);
         }

         if (type == ContainerInput.PICKUP
            && !cursor.isEmpty()
            && place != null
            && place.group() == MatLayout.Group.SUPPLIES
            && ItemStack.matches(cursor, this.menu.getCarried())
            && ItemStack.matches(original, slot.getItem())
            && this.storageFull(cursor)
            && (place.category == ItemCategoryClassifier.classify(cursor).category() || place.materialCell >= 0 && MaterialKinds.isMaterial(cursor))) {
            MatLayout.ToolCell cell = this.layout.toolCell(place.toolCell);
            MatLayout.MaterialCell material = this.layout.materialCell(place.materialCell);
            ItemCategoryClassifier.Category category = material == null ? place.category : material.category();

            for (ItemPresentationData.Compartment c : this.layout.compartments()) {
               if (c.category() == category && (cell == null || cell.z0() >= c.z0() && cell.z1() <= c.z1())) {
                  this.warnCapacity(c.id());
                  break;
               }
            }
         }

         boolean toHotbar = type == ContainerInput.QUICK_MOVE && place != null && place.group() == MatLayout.Group.SUPPLIES;
         if (slot != null
            && (type == ContainerInput.QUICK_MOVE || type == ContainerInput.SWAP)
            && ItemStack.matches(original, slot.getItem())
            && !attempted.isEmpty()
            && (toHotbar ? this.hotbarFull(attempted) : this.storageFull(attempted))) {
            ItemCategoryClassifier.Category category = toHotbar
               ? ItemCategoryClassifier.Category.HOTBAR
               : ItemCategoryClassifier.classify(attempted).category();
            if (!toHotbar && place != null && place.materialCell >= 0 && MaterialKinds.isMaterial(attempted)) {
               category = this.layout.materialCell(place.materialCell).category();
            }

            MatLayout.ToolCell cell = category == ItemCategoryClassifier.Category.LONG_TOOLS ? this.layout.toolCell(this.layout.toolCellFor(attempted)) : null;

            for (ItemPresentationData.Compartment c : this.layout.compartments()) {
               if (c.category() == category && (cell == null || cell.z0() >= c.z0() && cell.z1() <= c.z1())) {
                  this.warnCapacity(c.id());
                  break;
               }
            }
         }

         if (source >= 0 && !this.menu.getCarried().isEmpty() && Objects.equals(kind, MatLayout.toolKind(this.menu.getCarried()))) {
            this.toolOrigin = source;
            this.heldTool = kind;
         }

         if (this.menu.getCarried().isEmpty() || !Objects.equals(this.heldTool, MatLayout.toolKind(this.menu.getCarried()))) {
            this.toolOrigin = -1;
            this.heldTool = null;
         }

         if (foodSource >= 0 && !this.menu.getCarried().isEmpty() && Objects.equals(foodKind, ConsumableKinds.key(this.menu.getCarried()))) {
            this.foodOrigin = foodSource;
            this.heldFood = foodKind;
         }

         if (this.menu.getCarried().isEmpty() || !Objects.equals(this.heldFood, ConsumableKinds.key(this.menu.getCarried()))) {
            this.foodOrigin = -1;
            this.heldFood = null;
         }

         if (materialSource >= 0 && !this.menu.getCarried().isEmpty() && Objects.equals(materialKind, MaterialKinds.key(this.menu.getCarried()))) {
            this.materialOrigin = materialSource;
            this.heldMaterial = materialKind;
         }

         if (this.menu.getCarried().isEmpty() || !Objects.equals(this.heldMaterial, MaterialKinds.key(this.menu.getCarried()))) {
            this.materialOrigin = -1;
            this.heldMaterial = null;
         }

         if (equipmentSource >= 0 && !this.menu.getCarried().isEmpty() && Objects.equals(equipmentKind, EquipmentKinds.kind(this.menu.getCarried()))) {
            this.equipmentOrigin = equipmentSource;
            this.heldEquipment = equipmentKind;
         }

         if (this.menu.getCarried().isEmpty() || !Objects.equals(this.heldEquipment, EquipmentKinds.kind(this.menu.getCarried()))) {
            this.equipmentOrigin = -1;
            this.heldEquipment = null;
         }
      }
   }

   private void warnCapacity(String sector) {
      if (sector.startsWith("extra_tools")) {
         this.capacitySector = "extra_tools";
         this.capacityTime = 1.2F;
      } else if (this.layout.compartments().stream().anyMatch(c -> c.id().equals(sector) && c.category() == ItemCategoryClassifier.Category.VEHICLES)) {
         this.capacitySector = "vehicles";
         this.capacityTime = 1.2F;
      } else {
         this.capacitySector = this.layout
               .compartments()
               .stream()
               .anyMatch(c -> c.id().equals(sector) && c.category() == ItemCategoryClassifier.Category.BLOCKS)
            ? "blocks"
            : sector;
         this.capacityTime = 1.2F;
      }
   }

   private boolean storageFull(ItemStack incoming) {
      for (MatLayout.Place p : this.layout.storagePlaces()) {
         Slot s = this.menu.getSlot(p.slot());
         ItemStack target = s.getItem();
         if (s.mayPlace(incoming)
            && (target.isEmpty() || ItemStack.isSameItemSameComponents(target, incoming) && target.getCount() < s.getMaxStackSize(incoming))) {
            return false;
         }
      }

      return true;
   }

   private boolean hotbarFull(ItemStack incoming) {
      for (MatLayout.Place p : this.layout.places()) {
         if (p.group() == MatLayout.Group.HOTBAR) {
            Slot s = this.menu.getSlot(p.slot());
            ItemStack target = s.getItem();
            if (s.mayPlace(incoming)
               && (target.isEmpty() || ItemStack.isSameItemSameComponents(target, incoming) && target.getCount() < s.getMaxStackSize(incoming))) {
               return false;
            }
         }
      }

      return true;
   }

   @Override
   public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
      if (!this.animation.itemsSettled()) {
         return true;
      } else if (this.recipePanelAt(x, y)) {
         return true;
      } else if (BundleClient.scroll(this.menu, this.pickSlot(x, y), vertical)) {
         return true;
      } else {
         int uiOwner = this.overlay.cycleOwnerAt(x, y);
         MatLayout.Place cycle = uiOwner >= 0
            ? this.layout.place(uiOwner)
            : (this.pickSlot(x, y) == null ? null : this.layout.place(this.pickSlot(x, y).index));
         if (vertical != 0.0 && this.canCycle(cycle) && this.overlay.scrollCycle(this.layout, cycle.slot(), vertical)) {
            return true;
         } else {
            return vertical != 0.0
                  && !this.isQuickCrafting
                  && !this.toolGestureActive()
                  && this.camera.raycast(x, y)
                  && this.layout.scroll(this.camera.hit.x, this.camera.hit.z, vertical > 0.0 ? -1 : 1)
               ? true
               : super.mouseScrolled(x, y, horizontal, vertical);
         }
      }
   }

   private boolean craftArea(double x, double y) {
      if (!this.camera.raycast(x, y)) {
         return false;
      }

      ItemPresentationData.Compartment c = this.layout.compartmentAt(this.camera.hit.x, this.camera.hit.z);
      return c != null && c.category() == ItemCategoryClassifier.Category.CRAFT;
   }

   private void cancelCraft() {
      if (this.craftSound != null) {
         this.minecraft.getSoundManager().stop(this.craftSound);
      }

      this.craftSound = null;
   }

   private void craft(boolean all) {
      if (ClientNetwork.supported() && InventoryCrafting.fits(this.menu) && this.menu.getSlot(0).mayPickup(this.minecraft.player)) {
         ClientNetwork.send(new CraftRequest(this.menu.containerId, all, this.menu.getSlot(0).getItem().copy()));
         this.cancelCraft();
         this.craftSound = SimpleSoundInstance.forUI(MatSounds.CRAFT, 1.0F, 0.55F);
         this.minecraft.getSoundManager().play(this.craftSound);
         this.isQuickCrafting = false;
         this.quickCraftSlots.clear();
         this.setDragging(false);
      }
   }

   @Override
   public boolean keyPressed(KeyEvent event) {
      if (this.animation.closing()) {
         return true;
      }

      if (event.key() == InputConstants.KEY_E) {
         if (this.eatKeyHeld) {
            return true;
         }

         double x = this.minecraft.mouseHandler.xpos() / this.minecraft.getWindow().getGuiScale();
         double y = this.minecraft.mouseHandler.ypos() / this.minecraft.getWindow().getGuiScale();
         Slot food = this.pickSlot(x, y);
         if (food != null && InventoryEating.isFood(food.getItem())) {
            this.eatKeyHeld = true;
            if (!this.recipePanelAt(x, y) && this.canEat(food)) {
               this.eatPending = true;
               this.eatToken = ++nextEatToken;
               ClientNetwork.send(new EatItemRequest(this.menu.containerId, food.index, this.eatToken, food.getItem().copy()));
            }

            return true;
         }
      }

      if (this.recipeBook != null && this.recipeBook.keyPressed(event)) {
         return true;
      } else if (MatInventoryClient.toggle.matches(event)) {
         MatInventoryClient.toggle(this.minecraft);
         return true;
      } else {
         return super.keyPressed(event);
      }
   }

   @Override
   public boolean keyReleased(KeyEvent event) {
      if (event.key() == InputConstants.KEY_E) {
         boolean held = this.eatKeyHeld;
         this.eatKeyHeld = false;
         if (held) {
            return true;
         }
      }

      return super.keyReleased(event);
   }

   private boolean canEat(Slot slot) {
      return !this.eatPending
         && this.menu.getCarried().isEmpty()
         && slot.mayPickup(this.minecraft.player)
         && InventoryEating.isFood(slot.getItem())
         && this.minecraft.player.getFoodData().needsFood()
         && !this.minecraft.player.isUsingItem()
         && !this.minecraft.player.getCooldowns().isOnCooldown(slot.getItem())
         && ClientNetwork.supported();
   }

   public void eatConfirmed(int menuId, int token, int variant) {
      if (menuId == this.menu.containerId && token == this.eatToken && this.eatPending) {
         this.eatPending = false;
         if (variant >= 0 && variant < MatSounds.EAT.length) {
            this.overlay.actionConfirmed();
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(MatSounds.EAT[variant], 1.0F, 0.5F));
         }
      }
   }

   @Override
   public boolean charTyped(CharacterEvent event) {
      return this.animation.closing() || this.recipeBook != null && this.recipeBook.charTyped(event) || super.charTyped(event);
   }

   @Override
   public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
      if (this.animation.closing()) {
         return true;
      } else if (this.recipeBook != null && this.recipeBook.mouseDragged(event, dx, dy)) {
         return true;
      } else {
         return !this.recipeRelease && !this.recipePanelAt(event.x(), event.y()) ? super.mouseDragged(event, dx, dy) : true;
      }
   }

   @Override
   public void recipesUpdated() {
      if (this.recipeBook != null) {
         this.recipeBook.recipesUpdated();
      }
   }

   @Override
   public void fillGhostRecipe(RecipeDisplay display) {
      if (this.recipeBook != null) {
         this.recipeBook.fillGhostRecipe(display);
      }
   }

   public CraftingRecipeBookComponent getRecipeBookComponent() {
      return this.recipeBook;
   }

   public void fallback() {
      MatInventoryClient.switchScreen(this.minecraft, this.vanilla);
   }

   @Override
   public void onClose() {
      if (!this.animation.closing() && !this.finishingClose) {
         this.cancelCraft();
         this.animation.frame();
         boolean populated = !this.menu.getCarried().isEmpty()
            || this.layout.places().stream().anyMatch(p -> p.visible && this.menu.getSlot(p.slot()).hasItem());
         this.animation.startClosing(populated);
         this.camera.hold();
         this.updateHoveredSlot(null);
         this.closePointerX = (int)(this.minecraft.mouseHandler.xpos() / this.minecraft.getWindow().getGuiScale());
         this.closePointerY = (int)(this.minecraft.mouseHandler.ypos() / this.minecraft.getWindow().getGuiScale());
         this.isQuickCrafting = false;
         this.quickCraftSlots.clear();
         this.setDragging(false);
         this.ledAudio.stop();
         this.itemAudio.stop();
         this.transitionAudio.beginClosing();
         if (this.animation.closeFinished()) {
            this.finishClosing();
         }
      }
   }

   private void finishClosing() {
      if (!this.finishingClose && this.minecraft.gui.screen() == this) {
         this.finishingClose = true;
         this.transitionAudio.stop();
         if (this.minecraft.player != null && this.minecraft.player.containerMenu == this.menu) {
            super.onClose();
         } else {
            this.minecraft.gui.setScreen(null);
         }
      }
   }

   @Override
   public void removed() {
      this.eatPending = this.eatKeyHeld = false;
      this.eatToken = 0;
      this.cancelCraft();
      this.updateHoveredSlot(null);
      this.items.world.leds.restore(MatInventoryClient.config.ledEnabled);
      this.ledAudio.stop();
      this.itemAudio.stop();
      this.transitionAudio.stop();
      if (this.animation.closing() && !this.finishingClose) {
         this.animation.cancelClosing();
      }

      super.removed();
   }
}
