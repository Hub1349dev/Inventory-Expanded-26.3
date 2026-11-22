package com.example.matinventory.layout;

import com.example.matinventory.MatInventoryClient;
import com.example.matinventory.arrangement.ItemCategoryClassifier;
import com.example.matinventory.arrangement.ItemFootprint;
import com.example.matinventory.arrangement.ItemPresentationData;
import com.example.matinventory.arrangement.ModelVariant;
import com.example.matinventory.capacity.ConsumableKinds;
import com.example.matinventory.capacity.EquipmentKinds;
import com.example.matinventory.capacity.ExtraInventory;
import com.example.matinventory.capacity.MaterialKinds;
import com.example.matinventory.capacity.ModToolRules;
import com.example.matinventory.capacity.VehicleKinds;
import com.example.matinventory.config.InventoryScales;
import com.example.matinventory.config.MatConfig;
import com.example.matinventory.core.ConsumableOrder;
import com.example.matinventory.core.ConsumablePile;
import com.example.matinventory.core.EquipmentOrder;
import com.example.matinventory.core.FreePositionSearch;
import com.example.matinventory.core.MatTopology;
import com.example.matinventory.core.MaterialOrder;
import com.example.matinventory.core.MaterialPile;
import com.example.matinventory.core.RenderedItemBounds;
import com.example.matinventory.core.ShelfPacker;
import com.example.matinventory.core.StorageOrder;
import com.example.matinventory.core.ToolOrder;
import com.example.matinventory.core.VisualItemKinds;
import com.example.matinventory.interaction.InventoryCrafting;
import com.example.matinventory.scene.MatCamera;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Vector3f;
import org.joml.Vector4f;

@Environment(EnvType.CLIENT)
public final class MatLayout {
   private static final float CRAFT_ITEM_SCALE = 0.775F;
   private static final float CRAFT_ITEM_FIT = 1.675F;
   private final Vector3f pickOrigin = new Vector3f();
   private final Vector3f pickDirection = new Vector3f();
   private final List<MatLayout.Place> places = new ArrayList<>();
   private final List<MatLayout.Place> storage = new ArrayList<>();
   private final List<MatLayout.Place> storageView = Collections.unmodifiableList(this.storage);
   private final MatLayout.Place[] bySlot = new MatLayout.Place[512];
   private final int gridSize;
   private final int base;
   private int selectedPage;
   private int totalPages = 1;
   private final EnumMap<ItemCategoryClassifier.Category, Integer> offsets = new EnumMap<>(ItemCategoryClassifier.Category.class);
   private final EnumMap<ItemCategoryClassifier.Category, Integer> totals = new EnumMap<>(ItemCategoryClassifier.Category.class);
   private final EnumMap<ItemCategoryClassifier.Category, Integer> shown = new EnumMap<>(ItemCategoryClassifier.Category.class);
   public boolean overflowLeaf;
   public float mainBottom = 3.4F;
   public float mainHalfWidth = 7.65F;
   public float hotbarTop = 3.7F;
   private long signature = Long.MIN_VALUE;
   private boolean arranged;
   private final FreePositionSearch freeSearch = new FreePositionSearch();
   private final Map<Integer, MatLayout.FlexiblePosition> lastFlexiblePositions = new HashMap<>();
   private final Map<Integer, MatLayout.FlexiblePosition> looseFlexiblePositions = new HashMap<>();
   private final Map<ItemPresentationData.Compartment, List<MatLayout.Anchor>> anchorCache = new HashMap<>();
   private final ToolOrder toolOrder = new ToolOrder(MatInventoryClient.config == null ? null : MatInventoryClient.config.toolOrder);
   private final Map<String, Integer> toolOffsets = new HashMap<>();
   private final Map<String, Integer> toolFrontSlots = new HashMap<>();
   private final Map<Integer, List<MatLayout.Place>> toolGroups = new HashMap<>();
   private int toolFamilyOffset;
   private int toolFamilyTotal;
   private boolean separatePreview;
   private final int blockSeed = new Random().nextInt();
   private final StorageOrder blockOrder = new StorageOrder(16, MatInventoryClient.config == null ? null : MatInventoryClient.config.blockOrder);
   private final StorageOrder vehicleOrder = new StorageOrder(3, MatInventoryClient.config == null ? null : MatInventoryClient.config.vehicleOrder);
   private final List<MatLayout.BlockCell> blockCells = new ArrayList<>();
   private final ConsumableOrder foodOrder = new ConsumableOrder(MatInventoryClient.config == null ? null : MatInventoryClient.config.consumableOrder);
   private final List<MatLayout.FoodCell> foodCells = new ArrayList<>();
   private final MaterialOrder materialOrder = new MaterialOrder(MatInventoryClient.config == null ? null : MatInventoryClient.config.materialOrder);
   private final List<MatLayout.MaterialCell> materialCells = new ArrayList<>();
   private final EquipmentOrder equipmentOrder = new EquipmentOrder(MatInventoryClient.config == null ? null : MatInventoryClient.config.equipmentOrder);
   private final List<MatLayout.EquipmentCell> equipmentCells = new ArrayList<>();
   private final Map<Integer, List<MatLayout.Place>> equipmentGroups = new HashMap<>();
   private final Map<String, Integer> equipmentFront = new HashMap<>();
   private final Map<String, List<Integer>> equipmentSequence = new HashMap<>();
   private int offhandSlot = -1;
   public static final float OFFHAND_WIDTH_MULTIPLIER = 1.15F;
   public static final float FREE_EDGE_CLEARANCE = 0.36F;
   private List<ItemPresentationData.Compartment> activeCompartments = List.of();
   private final List<MatLayout.VehicleCell> vehicleCells = new ArrayList<>();
   private final Set<String> vehiclePreviews = new HashSet<>();
   public static final float VEHICLE_LENGTH = 5.48F;
   private boolean upperPreview;
   private boolean upperBlocksPreview;
   private boolean lowerBlocksPreview;
   private boolean shieldPreview;
   private boolean elytraPreview;
   private boolean vehiclePreview;
   private boolean tridentPreview;
   private boolean fishingPreview;
   private final List<MatLayout.ToolCell> toolCells = new ArrayList<>();
   public int revision;
   public float hotbarBottom = 5.2F;
   public float equipmentTop = -6.95F;
   public float beltHalfWidth = 7.65F;
   public float equipmentHalfWidth = 7.65F;
   private final float[] beltEdges = new float[10];
   private final float[] beltCells = new float[9];
   private final float[] placementScales = new float[512];
   public float capacityScale = 1.0F;

   public void separatePreview(boolean enabled) {
      if (this.separatePreview != enabled) {
         this.separatePreview = enabled;
         this.signature = Long.MIN_VALUE;
      }
   }

   private static int toolVisibleLimit(String kind) {
      return kind.startsWith("external:") ? 2 : 5;
   }

   public List<MatLayout.BlockCell> blockCells() {
      return List.copyOf(this.blockCells);
   }

   public MatLayout.BlockCell blockCell(int index) {
      return this.blockCells.stream().filter(c -> c.index() == index).findFirst().orElse(null);
   }

   public MatLayout.BlockCell blockCellAt(float x, float z) {
      return this.blockCells.stream().filter(c -> c.contains(x, z)).findFirst().orElse(null);
   }

   public static String storageKey(Slot slot) {
      return (slot.container instanceof ExtraInventory ? "extra:" : "inventory:") + slot.getContainerSlot();
   }

   private static String pileKey(Slot slot, String kind) {
      return kind + "@" + storageKey(slot);
   }

   public void placeStorage(AbstractContainerMenu menu, Slot slot, int cell, boolean vehicle) {
      if (vehicle) {
         this.vehicleOrder.place(storageKey(slot), cell);
      } else {
         this.blockOrder.place(pileKey(slot, ItemPresentationData.id(slot.getItem())), cell);
      }

      this.saveStorageOrders();

      for (MatLayout.Place p : this.storagePlaces()) {
         if (p.vehicleCell >= 0 || p.blockCell >= 0) {
            p.initialized = false;
            p.pickBoxCount = 0;
         }
      }
   }

   private void saveStorageOrders() {
      MatInventoryClient.config.blockOrder = new ArrayList<>(this.blockOrder.keys());
      MatInventoryClient.config.vehicleOrder = new ArrayList<>(this.vehicleOrder.keys());
      MatInventoryClient.config.save();
      this.signature = Long.MIN_VALUE;
   }

   public List<MatLayout.FoodCell> foodCells() {
      return List.copyOf(this.foodCells);
   }

   public MatLayout.FoodCell foodCell(int index) {
      return index >= 0 && index < this.foodCells.size() ? this.foodCells.get(index) : null;
   }

   public MatLayout.FoodCell foodCellAt(float x, float z) {
      return this.foodCells.stream().filter(c -> c.contains(x, z)).findFirst().orElse(null);
   }

   public int foodCellFor(ItemStack stack) {
      String prefix = ConsumableKinds.key(stack) + "@";

      for (int i = 0; i < this.foodOrder.keys().size(); i++) {
         if (this.foodOrder.key(i).startsWith(prefix)) {
            return i;
         }
      }

      return -1;
   }

   public void swapFoodCells(int from, int to) {
      if (from != to) {
         this.foodOrder.swap(from, to);
         this.saveFoodOrder();
      }
   }

   public void placeFood(Slot slot, int cell) {
      String key = pileKey(slot, ConsumableKinds.key(slot.getItem()));
      if (this.foodOrder.cell(key) != cell) {
         this.foodOrder.place(key, cell);
         this.saveFoodOrder();
      }
   }

   private void saveFoodOrder() {
      MatInventoryClient.config.consumableOrder = new ArrayList<>(this.foodOrder.keys());
      MatInventoryClient.config.save();
      this.signature = Long.MIN_VALUE;

      for (MatLayout.Place p : this.places) {
         if (p.foodCell >= 0) {
            p.initialized = false;
         }
      }
   }

   public List<MatLayout.MaterialCell> materialCells() {
      return List.copyOf(this.materialCells);
   }

   public MatLayout.MaterialCell materialCell(int cell) {
      return cell >= 0 && cell < this.materialCells.size() ? this.materialCells.get(cell) : null;
   }

   public MatLayout.MaterialCell materialCellAt(float x, float z) {
      return this.materialCells.stream().filter(c -> c.contains(x, z)).findFirst().orElse(null);
   }

   public int materialCellFor(ItemStack stack) {
      String prefix = MaterialKinds.key(stack) + "@";

      for (int i = 0; i < this.materialOrder.keys().size(); i++) {
         if (this.materialOrder.key(i).startsWith(prefix)) {
            return i;
         }
      }

      return -1;
   }

   public void swapMaterialCells(int from, int to) {
      if (from != to) {
         this.materialOrder.swap(from, to);
         this.saveMaterialOrder();
      }
   }

   public void placeMaterial(Slot slot, int cell) {
      String key = pileKey(slot, MaterialKinds.key(slot.getItem()));
      if (this.materialOrder.cell(key) != cell) {
         this.materialOrder.place(key, cell);
         this.saveMaterialOrder();
      }
   }

   private void saveMaterialOrder() {
      MatInventoryClient.config.materialOrder = new ArrayList<>(this.materialOrder.keys());
      MatInventoryClient.config.save();
      this.signature = Long.MIN_VALUE;

      for (MatLayout.Place p : this.places) {
         if (p.materialCell >= 0) {
            p.initialized = false;
         }
      }
   }

   public static boolean materialCategory(ItemCategoryClassifier.Category category) {
      return category == ItemCategoryClassifier.Category.RESOURCES || category == ItemCategoryClassifier.Category.OTHER;
   }

   public void remapEquipment(int a, int b) {
      for (List<Integer> sequence : this.equipmentSequence.values()) {
         for (int i = 0; i < sequence.size(); i++) {
            int s = sequence.get(i);
            if (s == a) {
               sequence.set(i, b);
            } else if (s == b) {
               sequence.set(i, a);
            }
         }
      }

      this.equipmentFront.replaceAll((k, sx) -> sx == a ? b : (sx == b ? a : sx));
      this.signature = Long.MIN_VALUE;

      for (MatLayout.Place p : this.places) {
         if (p.equipmentCell >= 0 || p.slot == a || p.slot == b) {
            p.initialized = false;
         }
      }
   }

   private float looseHalfWidth(MatLayout.Place p, boolean target) {
      double a = Math.toRadians((target ? p.targetYaw : p.yaw) - this.rackYaw(p));
      return p.halfWidth * (float)Math.abs(Math.cos(a)) + p.halfDepth * (float)Math.abs(Math.sin(a));
   }

   private float looseHalfDepth(MatLayout.Place p, boolean target) {
      double a = Math.toRadians((target ? p.targetYaw : p.yaw) - this.rackYaw(p));
      return p.halfDepth * (float)Math.abs(Math.cos(a)) + p.halfWidth * (float)Math.abs(Math.sin(a));
   }

   private float rackYaw(MatLayout.Place p) {
      MatLayout.ToolCell cell = this.toolCell(p.toolCell);
      return cell == null || !"trident".equals(cell.kind()) && !"fishing_rod".equals(cell.kind()) ? 0.0F : 90.0F;
   }

   public float offhandWidth() {
      return (this.beltEdges[1] - this.beltEdges[0]) * 1.15F;
   }

   public float hotbarCellWidth() {
      return this.beltEdges[1] - this.beltEdges[0];
   }

   public int offhandSlot() {
      return this.offhandSlot;
   }

   public List<MatLayout.EquipmentCell> equipmentCells() {
      return List.copyOf(this.equipmentCells);
   }

   public MatLayout.EquipmentCell equipmentCell(int index) {
      return this.equipmentCells.stream().filter(c -> c.index() == index).findFirst().orElse(null);
   }

   public MatLayout.EquipmentCell equipmentCellAt(float x, float z) {
      return this.equipmentCells.stream().filter(c -> c.contains(x, z)).findFirst().orElse(null);
   }

   public int equipmentCellFor(ItemStack stack) {
      return this.equipmentOrder.cell(EquipmentKinds.kind(stack));
   }

   public static boolean equipmentDestination(int from, int to) {
      return from >= 0 && to >= 0 && (from < 5 && to < 5 || from == to);
   }

   public List<MatLayout.Place> equipmentInCell(int index) {
      return this.equipmentGroups.getOrDefault(index, List.of());
   }

   public void swapEquipmentCells(int from, int to) {
      if (from != to) {
         this.equipmentOrder.swap(from, to);
         MatInventoryClient.config.equipmentOrder = new ArrayList<>(this.equipmentOrder.keys());
         MatInventoryClient.config.save();
         this.signature = Long.MIN_VALUE;

         for (MatLayout.Place p : this.places) {
            if (p.equipmentCell >= 0) {
               p.initialized = false;
            }
         }
      }
   }

   public boolean cycleEquipmentGroup(int index) {
      List<MatLayout.Place> group = this.equipmentInCell(index);
      if (group.size() < 2) {
         return false;
      }

      String key = this.equipmentCell(index).kind();
      this.equipmentFront.put(key, group.get(group.size() - 2).slot());
      this.signature = Long.MIN_VALUE;

      for (MatLayout.Place p : group) {
         p.initialized = false;
         p.pickBoxCount = 0;
      }

      return true;
   }

   public List<MatLayout.VehicleCell> vehicleCells() {
      return List.copyOf(this.vehicleCells);
   }

   public MatLayout.VehicleCell vehicleCellAt(float x, float z) {
      return this.vehicleCells.stream().filter(c -> c.contains(x, z)).findFirst().orElse(null);
   }

   public ItemPresentationData.Compartment vehicleArea() {
      if (this.vehicleCells.isEmpty()) {
         return null;
      }

      MatLayout.VehicleCell first = this.vehicleCells.getFirst();
      MatLayout.VehicleCell last = this.vehicleCells.getLast();
      return new ItemPresentationData.Compartment(
         "vehicles", ItemCategoryClassifier.Category.VEHICLES, first.x0() - 0.2F, first.z0() - 0.2F, last.x1() + 0.2F, last.z1() + 0.2F
      );
   }

   public boolean upperToolsPreview() {
      return this.upperPreview;
   }

   public boolean compartmentPreview(String id) {
      return id.equals("extra_tools") && this.upperPreview
         || id.equals("extra_tools_trident") && this.tridentPreview
         || id.equals("extra_tools_fishing_rod") && this.fishingPreview
         || id.equals("extra_blocks_top") && this.upperBlocksPreview
         || id.equals("extra_blocks_bottom") && this.lowerBlocksPreview
         || id.equals("extra_shield") && this.shieldPreview
         || id.equals("extra_elytra") && this.elytraPreview
         || id.equals("vehicles") && this.vehiclePreview
         || this.vehiclePreviews.contains(id);
   }

   public List<ItemPresentationData.Compartment> fabricCompartments() {
      return this.compartments();
   }

   public List<ItemPresentationData.Compartment> solidCompartments() {
      return this.fabricCompartments().stream().filter(c -> !this.compartmentPreview(c.id())).toList();
   }

   public void syncSlots(AbstractContainerMenu menu) {
      Slot output = InventoryCrafting.output(menu);
      if (output != null && this.place(output.index) == null) {
         this.add(output.index, 2.5F, 0.0F, 0.58F, MatLayout.Group.RESULT);
      }

      for (Slot slot : menu.slots) {
         if (slot.container instanceof ExtraInventory extra && ExtraInventory.enabled(extra.owner) && this.place(slot.index) == null) {
            this.add(slot.index, 0.0F, 0.0F, 0.58F, MatLayout.Group.SUPPLIES);
         }
      }

      for (Slot slot : menu.slots) {
         if (slot.container instanceof Inventory && slot.getContainerSlot() == 40) {
            this.offhandSlot = slot.index;
            if (this.place(slot.index) == null) {
               this.add(slot.index, -4.5F, 5.8F, 0.4F, MatLayout.Group.OFFHAND);
            }
         }
      }

      for (Slot slot : menu.slots) {
         if (slot.container instanceof Inventory && slot.getContainerSlot() >= 36 && slot.getContainerSlot() <= 39 && this.place(slot.index) == null) {
            this.add(slot.index, 0.0F, 0.0F, 0.58F, MatLayout.Group.EQUIPMENT);
         }
      }
   }

   public List<MatLayout.Place> storagePlaces() {
      return this.storageView;
   }

   public List<MatLayout.ToolCell> toolCells() {
      return List.copyOf(this.toolCells);
   }

   public MatLayout.ToolCell toolCellAt(float x, float z) {
      return this.toolCells.stream().filter(c -> c.contains(x, z)).findFirst().orElse(null);
   }

   public MatLayout.ToolCell toolDropCellAt(float x, float z) {
      return this.toolCells.stream().filter(c -> c.dropContains(x, z)).findFirst().orElse(null);
   }

   public MatLayout.ToolCell toolCell(int index) {
      return this.toolCells.stream().filter(c -> c.index == index).findFirst().orElse(null);
   }

   public int toolOffset(String kind) {
      return this.toolOffsets.getOrDefault(kind, 0);
   }

   public List<MatLayout.Place> toolsInCell(int index) {
      return this.toolGroups.getOrDefault(index, List.of());
   }

   public MatLayout.Place topTool(int cell) {
      List<MatLayout.Place> group = this.toolsInCell(cell);
      MatLayout.ToolCell area = this.toolCell(cell);
      return !group.isEmpty() && area != null ? group.get(this.toolFrontIndex(group, area.kind())) : null;
   }

   public void promoteTool(String kind, int slot) {
      if (kind != null) {
         this.toolFrontSlots.put(kind, slot);
         this.signature = Long.MIN_VALUE;

         for (MatLayout.Place p : this.places) {
            if (p.toolCell >= 0) {
               p.initialized = false;
               p.pickBoxCount = 0;
            }
         }
      }
   }

   public boolean scrollTool(int cell, double amount) {
      MatLayout.Place top = this.topTool(cell);
      if (top != null && amount != 0.0) {
         List<MatLayout.Place> order = this.cyclePreview(top.slot());
         if (order.size() < 2) {
            return false;
         }

         int steps = Math.max(1, (int)Math.round(Math.abs(amount)));
         return this.selectCycleItem(top.slot(), order.get(Math.floorMod(amount > 0.0 ? steps : -steps, order.size())).slot());
      } else {
         return false;
      }
   }

   public List<MatLayout.Place> cyclePreview(int slot) {
      MatLayout.Place p = this.place(slot);
      if (p == null) {
         return List.of();
      }

      List<MatLayout.Place> group = p.equipmentCell >= 0 ? this.equipmentInCell(p.equipmentCell) : (p.toolCell >= 0 ? this.toolsInCell(p.toolCell) : List.of());
      if (group.size() < 2) {
         return List.of();
      }

      int front = group.size() - 1;
      if (p.equipmentCell < 0) {
         MatLayout.ToolCell cell = this.toolCell(p.toolCell);
         if (cell == null) {
            return List.of();
         }

         front = this.toolFrontIndex(group, cell.kind());
      }

      ArrayList<MatLayout.Place> preview = new ArrayList<>(group.size());

      for (int i = 0; i < group.size(); i++) {
         preview.add(group.get(Math.floorMod(front - i, group.size())));
      }

      return preview;
   }

   public boolean selectCycleItem(int owner, int target) {
      MatLayout.Place p = this.place(owner);
      List<MatLayout.Place> preview = this.cyclePreview(owner);
      if (p != null && preview.size() >= 2 && !preview.stream().noneMatch(qx -> qx.slot() == target)) {
         if (p.equipmentCell >= 0) {
            this.equipmentFront.put(this.equipmentCell(p.equipmentCell).kind(), target);
         } else {
            MatLayout.ToolCell cell = this.toolCell(p.toolCell);
            this.toolFrontSlots.put(cell.kind(), target);
            this.toolOffsets.put(cell.kind(), Math.max(0, preview.size() - toolVisibleLimit(cell.kind())));
         }

         this.signature = Long.MIN_VALUE;

         for (MatLayout.Place q : preview) {
            q.initialized = false;
            q.pickBoxCount = 0;
         }

         return true;
      } else {
         return false;
      }
   }

   private int toolFrontIndex(List<MatLayout.Place> group, String kind) {
      Integer selected = this.toolFrontSlots.get(kind);
      int front = -1;

      for (int i = 0; i < group.size(); i++) {
         if (selected != null && group.get(i).slot() == selected) {
            front = i;
         }
      }

      if (front < 0) {
         for (int i = 0; i < group.size(); i++) {
            if (group.get(i).visible) {
               front = i;
            }
         }
      }

      return Math.floorMod(front, group.size());
   }

   public boolean cycleToolGroup(int index) {
      List<MatLayout.Place> group = this.toolGroups.get(index);
      MatLayout.ToolCell cell = this.toolCell(index);
      if (cell != null && group != null && group.size() >= 2) {
         int front = this.toolFrontIndex(group, cell.kind());
         this.toolFrontSlots.put(cell.kind(), group.get(Math.floorMod(front - 1, group.size())).slot());
         this.toolOffsets.put(cell.kind(), Math.max(0, group.size() - toolVisibleLimit(cell.kind())));
         this.signature = Long.MIN_VALUE;

         for (MatLayout.Place p : group) {
            p.initialized = false;
            p.pickBoxCount = 0;
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean extraToolsVisible() {
      return this.toolCells.size() > 5;
   }

   public int toolCellFor(ItemStack stack) {
      String kind = toolKind(stack);
      return kind == null ? -1 : this.toolOrder.cell(kind);
   }

   public static String toolKind(ItemStack stack) {
      if (ModToolRules.applies(stack)) {
         String key = "external:" + ItemPresentationData.id(stack);
         return key + ModelVariant.key(stack);
      } else {
         String kind = ItemCategoryClassifier.toolKind(stack);
         if (kind == null && !stack.isEmpty()) {
            ItemCategoryClassifier.Classification classification = ItemCategoryClassifier.classify(stack);
            return classification.category() == ItemCategoryClassifier.Category.LONG_TOOLS ? classification.subtype() : null;
         } else {
            return kind;
         }
      }
   }

   public void swapToolCells(int source, int target) {
      if (source != target) {
         this.toolOrder.swap(source, target);
         MatInventoryClient.config.toolOrder = new ArrayList<>(this.toolOrder.keys());
         MatInventoryClient.config.save();
         this.signature = Long.MIN_VALUE;

         for (MatLayout.Place p : this.places) {
            if (p.toolCell >= 0) {
               p.initialized = false;
            }
         }
      }
   }

   private MatLayout(boolean table) {
      this.gridSize = table ? 3 : 2;
      this.base = table ? 10 : 9;
      float step = 1.35F;
      float r = 0.58F;

      for (int row = 0; row < this.gridSize; row++) {
         for (int col = 0; col < this.gridSize; col++) {
            this.add(
               1 + row * this.gridSize + col,
               (col - (this.gridSize - 1) / 2.0F) * 1.35F - (table ? 0.2F : 0.7F),
               (row - (this.gridSize - 1) / 2.0F) * 1.35F,
               0.58F,
               MatLayout.Group.CRAFT
            );
         }
      }

      this.add(0, 2.5F, 0.0F, 0.58F, MatLayout.Group.RESULT);

      for (int i = 0; i < 15; i++) {
         this.add(this.base + i, -6.75F + i % 3 * 1.35F, -2.7F + i / 3 * 1.35F, 0.58F, MatLayout.Group.SUPPLIES);
      }

      for (int i = 0; i < 12; i++) {
         this.add(this.base + 15 + i, 4.05F + i % 3 * 1.35F, -2.7F + i / 3 * 1.35F, 0.58F, MatLayout.Group.SUPPLIES);
      }

      for (int i = 0; i < 9; i++) {
         this.add(this.base + 27 + i, (i - 4) * 1.35F, 4.25F, 0.58F, MatLayout.Group.HOTBAR);
      }

      if (!table) {
         for (int i = 0; i < 4; i++) {
            this.add(5 + i, -2.7F + i * 1.35F, -4.2F, 0.58F, MatLayout.Group.EQUIPMENT);
         }

         this.add(45, 2.7F, -4.2F, 0.58F, MatLayout.Group.OFFHAND);
         this.offhandSlot = 45;
      }
   }

   private void add(int s, float x, float z, float r, MatLayout.Group g) {
      MatLayout.Place p = new MatLayout.Place(s, x, z, r, g);
      this.places.add(p);
      if (g == MatLayout.Group.SUPPLIES) {
         this.storage.add(p);
      }

      this.bySlot[s] = p;
   }

   public static MatLayout inventory() {
      return new MatLayout(false);
   }

   public static MatLayout workbench() {
      return new MatLayout(true);
   }

   public List<MatLayout.Place> places() {
      return this.places;
   }

   public int gridSize() {
      return this.gridSize;
   }

   public int mainBase() {
      return this.base;
   }

   public MatLayout.Place place(int slot) {
      return slot >= 0 && slot < this.bySlot.length ? this.bySlot[slot] : null;
   }

   public float halfWidth() {
      return 19.0F;
   }

   public float halfDepth() {
      return 9.0F;
   }

   public boolean categorized() {
      return MatInventoryClient.config != null && !"SLOT_ORDER".equals(MatInventoryClient.config.arrangeMode);
   }

   public List<ItemPresentationData.Compartment> compartments() {
      return this.activeCompartments.isEmpty() ? ItemPresentationData.compartments() : this.activeCompartments;
   }

   public ItemPresentationData.Compartment compartmentAt(float x, float z) {
      if (this.overflowLeaf && Math.abs(x) <= this.mainHalfWidth && z >= -3.35F && z <= this.mainBottom) {
         return ItemPresentationData.compartment(ItemCategoryClassifier.Category.OTHER);
      }

      for (ItemPresentationData.Compartment c : this.compartments()) {
         if (c.category() == ItemCategoryClassifier.Category.HOTBAR) {
            if (c.contains(x, z)) {
               return c;
            }
         } else if (c.category() == ItemCategoryClassifier.Category.EQUIPMENT) {
            if (c.contains(x, z)) {
               return c;
            }
         } else if (c.contains(x, z)) {
            return c;
         }
      }

      return null;
   }

   public boolean onMat(float x, float z) {
      return this.compartments().stream().anyMatch(c -> x >= c.x0() - 0.18F && x <= c.x1() + 0.18F && z >= c.z0() - 0.18F && z <= c.z1() + 0.18F);
   }

   public boolean insideEnvelope(float x, float z) {
      float x0 = Float.POSITIVE_INFINITY;
      float z0 = x0;
      float x1 = -x0;
      float z1 = -x0;

      for (ItemPresentationData.Compartment c : this.compartments()) {
         x0 = Math.min(x0, c.x0());
         z0 = Math.min(z0, c.z0());
         x1 = Math.max(x1, c.x1());
         z1 = Math.max(z1, c.z1());
      }

      return x >= x0 - 0.35F && x <= x1 + 0.35F && z >= z0 - 0.35F && z <= z1 + 0.35F;
   }

   public boolean onCloth(float x, float z) {
      if (this.overflowLeaf) {
         return false;
      }

      if (this.compartments().isEmpty()) {
         return Math.abs(x) < 3.14F && z > -3.32F && z < 3.43F;
      }

      ItemPresentationData.Compartment c = ItemPresentationData.compartment(ItemCategoryClassifier.Category.CRAFT);
      return x >= c.x0() + 0.55F && x <= c.x1() - 0.55F && z >= c.z0() + 0.5F && z <= c.z1() - 0.5F;
   }

   public void arrange(AbstractContainerMenu menu, ItemFootprint[] footprints, float dt, boolean freeze) {
      long hash = ItemPresentationData.generation * 31L + MatInventoryClient.config.arrangeMode.hashCode();
      hash = hash * 31L + InventoryScales.revision;
      hash = hash * 31L + (MatInventoryClient.config.gridEnabled ? 1 : 0);

      for (MatLayout.Place p : this.places) {
         ItemFootprint f = footprints[p.slot];
         hash = hash * 31L + (f == null ? 0 : f.hashCode());
      }

      hash = hash * 31L + this.selectedPage;
      hash = hash * 31L + this.toolOrder.keys().hashCode();
      hash = hash * 31L + Objects.hashCode(toolKind(menu.getCarried()));
      hash = hash * 31L + this.foodOrder.keys().hashCode();
      hash = hash * 31L + this.materialOrder.keys().hashCode();
      hash = hash * 31L + this.equipmentOrder.keys().hashCode();
      hash = hash * 31L + this.blockOrder.keys().hashCode();
      hash = hash * 31L + this.vehicleOrder.keys().hashCode();
      hash = hash * 31L + Objects.hashCode(EquipmentKinds.kind(menu.getCarried()));
      hash = hash * 31L + (VehicleKinds.isVehicle(menu.getCarried()) ? 1 : 0);
      boolean blockHeld = !menu.getCarried().isEmpty()
         && ItemCategoryClassifier.classify(menu.getCarried()).category() == ItemCategoryClassifier.Category.BLOCKS;
      hash = hash * 31L + (blockHeld ? ItemPresentationData.id(menu.getCarried()).hashCode() : 0);
      boolean needsUpperPreview = this.needsUpperPreview(menu.getCarried());
      String blockPreview = this.blockPreviewId(menu);
      boolean revealBlocks = blockPreview != null && this.compartments().stream().noneMatch(cx -> cx.id().equals(blockPreview));
      boolean revealEquipment = menu.getCarried().is(Items.SHIELD) && this.equipmentCell(5) == null
         || menu.getCarried().is(Items.ELYTRA) && this.equipmentCell(6) == null;
      boolean revealVehicles = VehicleKinds.isVehicle(menu.getCarried())
         && (
            this.vehicleCells.isEmpty()
               || this.vehicleCells
                  .stream()
                  .anyMatch(
                     cell -> this.compartments()
                        .stream()
                        .noneMatch(
                           cx -> cx.category() == ItemCategoryClassifier.Category.VEHICLES
                              && cx.contains((cell.x0() + cell.x1()) * 0.5F, (cell.z0() + cell.z1()) * 0.5F)
                        )
                  )
         );
      String heldToolKind = toolKind(menu.getCarried());
      boolean revealTool = heldToolKind != null && this.toolCell(this.toolOrder.cell(heldToolKind)) == null;
      if (!freeze
         || !this.arranged
         || (needsUpperPreview || this.separatePreview) && !this.extraToolsVisible()
         || revealBlocks
         || revealEquipment
         || revealVehicles
         || revealTool) {
         if (hash != this.signature) {
            this.signature = hash;
            this.revision++;

            for (MatLayout.Place p : this.places) {
               p.pickBoxCount = 0;
            }

            this.pack(menu, footprints);
         } else if (this.arranged) {
            boolean moving = false;

            for (MatLayout.Place p : this.places) {
               if (Math.abs(p.x - p.targetX) > 1.0E-4F || Math.abs(p.z - p.targetZ) > 1.0E-4F || Math.abs(p.yaw - p.targetYaw) > 1.0E-4F) {
                  moving = true;
                  break;
               }
            }

            if (!moving) {
               return;
            }
         }

         float seconds = MatInventoryClient.config.reflowAnimationSeconds;
         float a = seconds <= 0.0F ? 1.0F : 1.0F - (float)Math.exp(-Math.max(0.0F, dt) * 5.0F / seconds);

         for (MatLayout.Place p : this.places) {
            if (!p.initialized) {
               p.x = p.targetX;
               p.z = p.targetZ;
               p.yaw = p.targetYaw;
               p.initialized = true;
            } else if (!freeze) {
               p.x = p.x + (p.targetX - p.x) * a;
               p.z = p.z + (p.targetZ - p.z) * a;
               p.yaw = p.yaw + (p.targetYaw - p.yaw) * a;
            }
         }

         boolean collision = false;

         for (MatLayout.Place p : this.places) {
            if (p.visible && p.group == MatLayout.Group.SUPPLIES && p.toolCell < 0 && p.equipmentCell < 0) {
               ItemPresentationData.Compartment c = this.compartments()
                  .stream()
                  .filter(b -> b.category().name().equals(p.bin) && (p.category != ItemCategoryClassifier.Category.BLOCKS || b.contains(p.targetX, p.targetZ)))
                  .findFirst()
                  .orElse(null);
               if (c != null
                  && (
                     p.x - p.halfWidth < c.x0() + 0.08F
                        || p.x + p.halfWidth > c.x1() - 0.08F
                        || p.z - p.halfDepth < c.z0() + 0.08F
                        || p.z + p.halfDepth > c.z1() - 0.08F
                  )) {
                  collision = true;
               }

               if (p.bin.equals("LONG_TOOLS") && Math.abs(p.x - p.targetX) > 0.06F) {
                  collision = true;
               }

               for (MatLayout.Place q : this.places) {
                  if (q != p
                     && q.visible
                     && q.group == MatLayout.Group.SUPPLIES
                     && Math.abs(p.x - q.x) < p.halfWidth + q.halfWidth + 0.02F
                     && Math.abs(p.z - q.z) < p.halfDepth + q.halfDepth + 0.02F) {
                     collision = true;
                  }
               }
            }
         }

         if (collision) {
            for (MatLayout.Place p : this.places) {
               p.x = p.targetX;
               p.z = p.targetZ;
               p.yaw = p.targetYaw;
            }
         }

         this.arranged = true;
      }
   }

   private void pack(AbstractContainerMenu menu, ItemFootprint[] f) {
      this.anchorCache.clear();
      this.totalPages = 1;
      this.selectedPage = 0;
      this.overflowLeaf = false;
      this.mainHalfWidth = 12.0F;
      this.mainBottom = 3.55F;
      ItemPresentationData.Compartment belt = ItemPresentationData.compartment(ItemCategoryClassifier.Category.HOTBAR);
      this.hotbarTop = belt.z0();
      this.hotbarBottom = belt.z1();
      this.beltHalfWidth = belt.width() / 2.0F;

      for (int i = 0; i < 10; i++) {
         this.beltEdges[i] = belt.x0() + 0.28F + i * (belt.width() - 0.56F) / 9.0F;
      }

      ItemPresentationData.Compartment gear = ItemPresentationData.compartment(ItemCategoryClassifier.Category.EQUIPMENT);
      this.equipmentTop = gear.z0();
      this.equipmentHalfWidth = gear.width() / 2.0F;
      ArrayList<MatLayout.Place> supplied = new ArrayList<>();
      ArrayList<MatLayout.Place> detached = new ArrayList<>();

      for (MatLayout.Place p : this.places) {
         ItemFootprint fp = f[p.slot];
         if (fp != null && !p.occupied) {
            p.initialized = false;
         }

         p.freePlacement = p.detachedPlacement = false;
         p.occupied = fp != null;
         p.visible = p.group != MatLayout.Group.SUPPLIES || fp != null;
         p.sizeFactor = 1.0F;
         p.toolCell = -1;
         p.toolTotal = 0;
         p.pileLift = 0.0F;
         p.blockTotal = 0;
         p.blockStacks = 0;
         p.foodCell = -1;
         p.foodStacks = 0;
         p.materialCell = -1;
         p.materialStacks = 0;
         p.materialStyle = 0;
         p.equipmentCell = -1;
         p.equipmentTotal = 0;
         p.vehicleCell = -1;
         p.blockCell = -1;
         if (p.group == MatLayout.Group.RESULT) {
            Slot output = InventoryCrafting.output(menu);
            p.visible = p.slot == (output != null && output.hasItem() ? output.index : 0);
         }

         p.freePlacement = p.detachedPlacement = false;
         this.footprint(p, fp);
         p.targetYaw = p.preferredYaw;
         p.bin = "";
         p.stackShade = 1.0F;
         if (fp != null) {
            float jitter = MatInventoryClient.config.itemRotationJitterDeg;
            int seed = p.seed * 31 + p.slot * 1103;
            p.targetYaw = p.targetYaw + (ItemCategoryClassifier.longToolPose(fp.classification()) ? 1.0F * signed(p.seed) : jitter * signed(seed));
         }

         if (p.group == MatLayout.Group.SUPPLIES && fp != null) {
            p.category = fp.classification().category();
            p.freePlacement = ItemCategoryClassifier.flexible(menu.getSlot(p.slot).getItem());
            MatConfig.DetachedItem saved = this.detachedPosition(menu.getSlot(p.slot));
            if (p.category == ItemCategoryClassifier.Category.EQUIPMENT) {
               p.targetYaw = Math.clamp(p.targetYaw, -5.0F, 5.0F);
            }

            if (p.category == ItemCategoryClassifier.Category.BLOCKS) {
               p.sizeFactor = !VisualItemKinds.torch(menu.getSlot(p.slot).getItem()) && !VisualItemKinds.flatStack(menu.getSlot(p.slot).getItem())
                  ? 0.57199997F
                  : 1.0F;
               float side = Math.max(p.halfWidth, p.halfDepth) * p.sizeFactor;
               p.halfWidth = side * 2.16F;
               p.halfDepth = side * 2.16F;
               p.targetYaw = VisualItemKinds.decorative(menu.getSlot(p.slot).getItem()) ? 0.0F : 0.75F * signed(p.seed ^ this.blockSeed);
               if (VisualItemKinds.flatStack(menu.getSlot(p.slot).getItem())) {
                  p.halfWidth = MaterialPile.halfWidth(fp.width(), fp.depth(), 0);
                  p.halfDepth = MaterialPile.halfDepth(fp.width(), fp.depth(), 0);
               }

               if (VisualItemKinds.bundle(menu.getSlot(p.slot).getItem())) {
                  float w = fp.width() * p.sizeFactor;
                  float d = fp.depth() * p.sizeFactor;
                  p.halfWidth = MaterialPile.halfWidth(w, d, 2);
                  p.halfDepth = MaterialPile.halfDepth(w, d, 2);
               }
            } else if (p.category == ItemCategoryClassifier.Category.FOOD) {
               p.sizeFactor = 0.74F;
               float w = p.halfWidth * 2.0F * p.sizeFactor;
               float d = p.halfDepth * 2.0F * p.sizeFactor;
               p.halfWidth = ConsumablePile.halfWidth(w, d);
               p.halfDepth = ConsumablePile.halfDepth(w, d);
               p.targetYaw = 7.0F * signed(p.seed ^ 1327217884);
            } else if (materialCategory(p.category) && !p.freePlacement) {
               ItemStack stack = menu.getSlot(p.slot).getItem();
               p.materialStyle = MaterialKinds.rod(stack) && fp.width() <= fp.depth() * 0.33F ? 2 : (MaterialKinds.mineral(stack) ? 1 : 0);
               p.halfWidth = MaterialPile.halfWidth(fp.width(), fp.depth(), p.materialStyle);
               p.halfDepth = MaterialPile.halfDepth(fp.width(), fp.depth(), p.materialStyle);
               p.targetYaw = (p.materialStyle == 2 ? 3 : 7) * signed(p.seed ^ 1327217884);
            } else if (p.category != ItemCategoryClassifier.Category.LONG_TOOLS && menu.getSlot(p.slot).getItem().getMaxStackSize() > 1) {
               p.halfWidth *= 2.06F;
               p.targetYaw = Math.clamp(p.targetYaw, -3.0F, 3.0F);
            }

            if (saved != null && (p.category != ItemCategoryClassifier.Category.LONG_TOOLS || Boolean.TRUE.equals(saved.independent))) {
               if (menu.getSlot(p.slot).getItem().getCount() == 1) {
                  p.halfWidth = fp.width() * 0.5F * p.sizeFactor;
                  p.halfDepth = fp.depth() * 0.5F * p.sizeFactor;
                  p.targetYaw = this.separateYaw(menu.getSlot(p.slot), fp, menu.getSlot(p.slot).getItem());
               }

               p.freePlacement = p.detachedPlacement = true;
               detached.add(p);
            } else {
               supplied.add(p);
            }
         } else {
            if (fp != null) {
               float limit = Float.POSITIVE_INFINITY;
               if (p.group != MatLayout.Group.HOTBAR && p.group != MatLayout.Group.OFFHAND) {
                  if (p.group == MatLayout.Group.EQUIPMENT) {
                     limit = 1.0F;
                  } else {
                     limit = 1.675F / Math.max(fp.width(), fp.depth());
                  }
               } else {
                  p.targetYaw = ItemCategoryClassifier.longToolPose(fp.classification()) ? 1.0F * signed(p.seed) : Math.clamp(p.targetYaw, -8.0F, 8.0F);
                  double a = Math.toRadians(p.targetYaw);
                  float w = (float)(fp.width() * Math.abs(Math.cos(a)) + fp.depth() * Math.abs(Math.sin(a)));
                  float d = (float)(fp.depth() * Math.abs(Math.cos(a)) + fp.width() * Math.abs(Math.sin(a)));
                  float cellWidth = (belt.width() - 0.56F) / 9.0F * (p.group == MatLayout.Group.OFFHAND ? 1.15F : 1.0F);
                  limit = Math.min(cellWidth * 0.86F / w, (belt.depth() - 0.5F) / d);
               }

               p.sizeFactor = Math.min(1.0F, limit);
               if ((p.group == MatLayout.Group.HOTBAR || p.group == MatLayout.Group.OFFHAND) && VisualItemKinds.torch(menu.getSlot(p.slot).getItem())) {
                  p.sizeFactor *= 0.5F;
               }

               if (p.group == MatLayout.Group.CRAFT || p.group == MatLayout.Group.RESULT) {
                  p.sizeFactor *= 0.775F;
               }

               p.halfWidth = p.halfWidth * p.sizeFactor;
               p.halfDepth = p.halfDepth * p.sizeFactor;
               p.radius = Math.max(p.halfWidth, p.halfDepth);
            }

            this.fixed(p, fp);
            if (fp == null && (p.group == MatLayout.Group.HOTBAR || p.group == MatLayout.Group.OFFHAND)) {
               p.halfWidth = (p.group == MatLayout.Group.OFFHAND ? this.offhandWidth() : this.beltEdges[1] - this.beltEdges[0]) * 0.4F;
               p.halfDepth = belt.depth() * 0.39F;
            }
         }
      }

      if (this.categorized()) {
         supplied.sort(
            Comparator.<MatLayout.Place, String>comparing(px -> f[px.slot].classification().subtype())
               .thenComparing(px -> -f[px.slot].cm())
               .thenComparing(px -> f[px.slot].id())
               .thenComparingInt(px -> f[px.slot].count())
               .thenComparingInt(px -> px.slot)
         );
      } else {
         supplied.sort(Comparator.comparingInt(MatLayout.Place::slot));
      }

      this.packTools(menu, supplied, f);
      this.packEquipment(menu, f);
      this.packBlocks(menu, supplied);
      this.packFoods(menu, supplied);
      this.packMaterials(menu, supplied);
      this.packVehicles(menu, supplied, f);
      ArrayList<ShelfPacker.Box> boxes = new ArrayList<>();

      for (MatLayout.Place p : supplied) {
         if (p.toolCell < 0
            && p.equipmentCell < 0
            && p.category != ItemCategoryClassifier.Category.VEHICLES
            && p.category != ItemCategoryClassifier.Category.BLOCKS
            && p.category != ItemCategoryClassifier.Category.FOOD
            && !materialCategory(p.category)) {
            double a = Math.toRadians(p.targetYaw);
            float co = (float)Math.abs(Math.cos(a));
            float si = (float)Math.abs(Math.sin(a));
            boxes.add(
               new ShelfPacker.Box(
                  p.slot,
                  p.category.name(),
                  2.0F * (p.halfWidth * co + p.halfDepth * si) * 1.08F + 0.12F,
                  2.0F * (p.halfDepth * co + p.halfWidth * si) * 1.08F + 0.3F,
                  24.0F * MatInventoryClient.config.minItemSizeFrac + 0.22F
               )
            );
         }
      }

      ArrayList<ShelfPacker.Rect> bins = new ArrayList<>();

      for (ItemPresentationData.Compartment c : this.compartments()) {
         if (!materialCategory(c.category())
            && c.category() != ItemCategoryClassifier.Category.VEHICLES
            && c.category() != ItemCategoryClassifier.Category.FOOD
            && c.category() != ItemCategoryClassifier.Category.BLOCKS
            && c.category() != ItemCategoryClassifier.Category.LONG_TOOLS
            && c.category() != ItemCategoryClassifier.Category.EQUIPMENT
            && c.category() != ItemCategoryClassifier.Category.CRAFT
            && c.category() != ItemCategoryClassifier.Category.HOTBAR) {
            bins.add(new ShelfPacker.Rect(c.category().name(), c.x0() + 0.2F, c.z0() + 0.2F, c.x1() - 0.2F, c.z1() - 0.2F));
         }
      }

      float padding = MatInventoryClient.config.compartmentPadding + 0.02F;
      float gap = MatInventoryClient.config.itemSpacing * 0.8F;
      ArrayList<ShelfPacker.Position> positions = new ArrayList<>();

      for (MatLayout.Place p : supplied) {
         if (p.toolCell < 0
            && p.equipmentCell < 0
            && p.category != ItemCategoryClassifier.Category.VEHICLES
            && p.category != ItemCategoryClassifier.Category.BLOCKS
            && p.category != ItemCategoryClassifier.Category.FOOD
            && !materialCategory(p.category)) {
            p.visible = false;
         }
      }

      for (ShelfPacker.Rect bin : bins) {
         ItemCategoryClassifier.Category category = ItemCategoryClassifier.Category.valueOf(bin.id());
         List<ShelfPacker.Box> group = boxes.stream().filter(box -> box.category().equals(bin.id())).toList();
         int offset = ShelfPacker.windowOffset(group, bin, this.offsets.getOrDefault(category, 0), padding, gap);
         this.offsets.put(category, offset);
         this.totals.put(category, group.size());
         ShelfPacker.Result window = ShelfPacker.window(group, bin, offset, padding, gap);
         this.shown.put(category, window.positions().size());
         positions.addAll(window.positions());
      }

      ShelfPacker.Result result = new ShelfPacker.Result(positions, 1.0F);
      this.capacityScale = 1.0F;

      for (ShelfPacker.Position q : result.positions()) {
         MatLayout.Place p = this.bySlot[q.id()];
         p.visible = true;
         double a = Math.toRadians(p.targetYaw);
         float w = 2.0F * (p.halfWidth * (float)Math.abs(Math.cos(a)) + p.halfDepth * (float)Math.abs(Math.sin(a)));
         p.sizeFactor = 1.0F;
         p.halfWidth = p.halfWidth * p.sizeFactor;
         p.halfDepth = p.halfDepth * p.sizeFactor;
         p.targetX = q.x() + signed(p.seed + p.slot * 77) * w * p.sizeFactor * 0.025F;
         p.targetZ = q.z() - 0.09F * p.sizeFactor + signed(p.seed * 17 + p.slot) * p.halfDepth * 0.05F;
         p.bin = q.bin();
         p.radius = Math.max(p.halfWidth, p.halfDepth);
      }

      ArrayList<MatLayout.Place> free = new ArrayList<>(supplied);
      free.addAll(detached);
      this.packFlexible(menu, free, f);
   }

   private boolean storageArea(ItemPresentationData.Compartment c) {
      return c.category() != ItemCategoryClassifier.Category.HOTBAR
         && c.category() != ItemCategoryClassifier.Category.CRAFT
         && (!this.compartmentPreview(c.id()) || this.separatePreview && c.category() == ItemCategoryClassifier.Category.LONG_TOOLS);
   }

   private boolean separateArea(ItemPresentationData.Compartment c) {
      return this.storageArea(c) && (c.id().equals("extra_tools") || ItemPresentationData.compartments().stream().anyMatch(base -> base.id().equals(c.id())));
   }

   private MatConfig.DetachedItem detachedPosition(Slot slot) {
      MatConfig.DetachedItem saved = MatInventoryClient.config.detachedItems.get(storageKey(slot));
      return saved != null
            && slot.hasItem()
            && saved.item.equals(ItemPresentationData.id(slot.getItem()))
            && this.acceptsStorage(slot.getItem(), saved.x, saved.z)
         ? saved
         : null;
   }

   public void detach(Slot slot, float x, float z) {
      this.detach(slot, x, z, null);
   }

   public void detach(Slot slot, float x, float z, Float presentationScale) {
      if (this.acceptsStorage(slot.getItem(), x, z)) {
         MatConfig.DetachedItem saved = new MatConfig.DetachedItem(ItemPresentationData.id(slot.getItem()), x, z);
         if (presentationScale != null && Float.isFinite(presentationScale) && presentationScale > 0.0F) {
            saved.presentationScale = presentationScale;
         }

         saved.freePosition = !MatInventoryClient.config.gridEnabled;
         saved.independent = true;
         if (MatInventoryClient.config.gridEnabled) {
            ItemPresentationData.Compartment area = this.compartmentAt(x, z);
            if (area != null) {
               saved.gridCell = this.anchors(area)
                  .stream()
                  .min(Comparator.comparingDouble(a -> (a.x - x) * (a.x - x) + (a.z - z) * (a.z - z)))
                  .map(MatLayout.Anchor::id)
                  .orElse(null);
            }
         }

         MatInventoryClient.config.detachedItems.put(storageKey(slot), saved);
         this.lastFlexiblePositions.remove(slot.index);
         this.looseFlexiblePositions.remove(slot.index);
         MatInventoryClient.config.save();
         this.signature = Long.MIN_VALUE;
         this.arranged = false;
         MatLayout.Place p = this.place(slot.index);
         if (p != null) {
            p.initialized = false;
            p.pickBoxCount = 0;
         }
      }
   }

   public float[] toolPileDestination(AbstractContainerMenu menu, int origin, float x, float z) {
      return this.toolPileDestination(menu, origin, x, z, null);
   }

   public float[] toolPileDestination(AbstractContainerMenu menu, int origin, float x, float z, ItemFootprint inserted) {
      MatLayout.Place source = this.place(origin);
      ItemPresentationData.Compartment area = this.compartmentAt(x, z);
      if (area != null && this.storageArea(area) && area.category() == ItemCategoryClassifier.Category.LONG_TOOLS) {
         List<MatLayout.Place> group = source != null && source.toolCell >= 0 ? this.toolsInCell(source.toolCell) : List.of();
         if (group.isEmpty() && inserted == null) {
            return null;
         }

         float left = Float.POSITIVE_INFINITY;
         float right = -left;
         float top = left;
         float bottom = -left;

         for (MatLayout.Place p : group) {
            float w = this.looseHalfWidth(p, false);
            float d = this.looseHalfDepth(p, false);
            left = Math.min(left, p.x - w);
            right = Math.max(right, p.x + w);
            top = Math.min(top, p.z - d);
            bottom = Math.max(bottom, p.z + d);
         }

         float hw = group.isEmpty() ? inserted.width() * 0.5F : (right - left) * 0.5F;
         float hd = group.isEmpty() ? inserted.depth() * 0.5F : (bottom - top) * 0.5F;
         this.freeSearch.reset(area.x0() + hw + 0.36F, area.z0() + hd + 0.36F, area.x1() - hw - 0.36F, area.z1() - hd - 0.36F, hw, hd);

         for (MatLayout.Place p : this.places) {
            if (p.slot != origin && p.visible && menu.getSlot(p.slot).hasItem() && !group.contains(p)) {
               this.freeSearch.add(p.x, p.z, this.looseHalfWidth(p, false), this.looseHalfDepth(p, false));
            }
         }

         return this.freeSearch.find(x, z) ? new float[]{this.freeSearch.x, this.freeSearch.z} : null;
      } else {
         return null;
      }
   }

   public void placeToolPile(AbstractContainerMenu menu, String kind, float x, float z) {
      String key = "tool-pile:" + kind;
      MatConfig.DetachedItem saved = new MatConfig.DetachedItem(key, x, z);
      saved.freePosition = true;
      MatInventoryClient.config.detachedItems.put(key, saved);

      for (MatLayout.Place p : this.storagePlaces()) {
         if (Objects.equals(kind, toolKind(menu.getSlot(p.slot).getItem()))) {
            MatConfig.DetachedItem previous = this.detachedPosition(menu.getSlot(p.slot));
            if (previous != null && !Boolean.TRUE.equals(previous.independent)) {
               MatInventoryClient.config.detachedItems.remove(storageKey(menu.getSlot(p.slot)));
            }

            p.initialized = false;
            p.pickBoxCount = 0;
         }
      }

      MatInventoryClient.config.save();
      this.signature = Long.MIN_VALUE;
      this.arranged = false;
   }

   public List<MatLayout.Anchor> anchors(ItemPresentationData.Compartment c) {
      return this.anchorCache.computeIfAbsent(c, this::buildAnchors);
   }

   private List<MatLayout.Anchor> buildAnchors(ItemPresentationData.Compartment c) {
      ArrayList<MatLayout.Anchor> result = new ArrayList<>();
      switch (c.category()) {
         case LONG_TOOLS:
            for (MatLayout.ToolCell cell : this.toolCells) {
               if (c.contains((cell.x0() + cell.x1()) * 0.5F, (cell.z0() + cell.z1()) * 0.5F)) {
                  result.add(new MatLayout.Anchor(c.id() + ":tool:" + cell.index(), (cell.x0() + cell.x1()) * 0.5F, (cell.z0() + cell.z1()) * 0.5F));
               }
            }
            break;
         case BLOCKS:
            for (MatLayout.BlockCell cell : this.blockCells) {
               if (c.contains((cell.x0() + cell.x1()) * 0.5F, (cell.z0() + cell.z1()) * 0.5F)) {
                  result.add(new MatLayout.Anchor(c.id() + ":block:" + cell.index(), (cell.x0() + cell.x1()) * 0.5F, (cell.z0() + cell.z1()) * 0.5F));
               }
            }
            break;
         case FOOD:
            for (MatLayout.FoodCell cell : this.foodCells) {
               if (c.contains((cell.x0() + cell.x1()) * 0.5F, (cell.z0() + cell.z1()) * 0.5F)) {
                  result.add(new MatLayout.Anchor(c.id() + ":food:" + cell.index(), (cell.x0() + cell.x1()) * 0.5F, (cell.z0() + cell.z1()) * 0.5F));
               }
            }
            break;
         case RESOURCES:
         case OTHER:
            for (MatLayout.MaterialCell cell : this.materialCells) {
               if (cell.category() == c.category() && c.contains((cell.x0() + cell.x1()) * 0.5F, (cell.z0() + cell.z1()) * 0.5F)) {
                  result.add(new MatLayout.Anchor(c.id() + ":material:" + cell.index(), (cell.x0() + cell.x1()) * 0.5F, (cell.z0() + cell.z1()) * 0.5F));
               }
            }
            break;
         case EQUIPMENT:
            for (MatLayout.EquipmentCell cell : this.equipmentCells) {
               if (c.contains((cell.x0() + cell.x1()) * 0.5F, (cell.z0() + cell.z1()) * 0.5F)) {
                  result.add(new MatLayout.Anchor(c.id() + ":equipment:" + cell.index(), (cell.x0() + cell.x1()) * 0.5F, (cell.z0() + cell.z1()) * 0.5F));
               }
            }
            break;
         case VEHICLES:
            for (MatLayout.VehicleCell cell : this.vehicleCells) {
               if (c.contains((cell.x0() + cell.x1()) * 0.5F, (cell.z0() + cell.z1()) * 0.5F)) {
                  result.add(new MatLayout.Anchor(c.id() + ":vehicle:" + cell.index(), (cell.x0() + cell.x1()) * 0.5F, (cell.z0() + cell.z1()) * 0.5F));
               }
            }
      }

      if (result.isEmpty() && this.storageArea(c)) {
         int cols = Math.max(1, (int)(c.width() / 2.0F));
         int rows = Math.max(1, (int)(c.depth() / 2.0F));

         for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
               result.add(
                  new MatLayout.Anchor(c.id() + ":cell:" + col + ":" + row, c.x0() + (col + 0.5F) * c.width() / cols, c.z0() + (row + 0.5F) * c.depth() / rows)
               );
            }
         }
      }

      return result;
   }

   public boolean acceptsStorage(ItemStack stack, float x, float z) {
      ItemPresentationData.Compartment c = this.compartmentAt(x, z);
      return c != null && ItemCategoryClassifier.acceptsSector(stack, c.category());
   }

   public boolean manualStorageAt(float x, float z) {
      ItemPresentationData.Compartment c = this.compartmentAt(x, z);
      return c != null && this.separateArea(c);
   }

   public void gridChanged() {
      this.signature = Long.MIN_VALUE;
   }

   private float separateYaw(Slot slot, ItemFootprint fp, ItemStack stack) {
      MatLayout.Place p = this.place(slot.index);
      ItemCategoryClassifier.Category category = fp.classification().category();
      float yaw = fp.preferredYaw()
         + (
            ItemCategoryClassifier.longToolPose(fp.classification())
               ? signed(p.seed)
               : MatInventoryClient.config.itemRotationJitterDeg * signed(p.seed * 31 + p.slot * 1103)
         );
      if (category == ItemCategoryClassifier.Category.BLOCKS) {
         return VisualItemKinds.decorative(stack) ? 0.0F : 0.75F * signed(p.seed ^ this.blockSeed);
      } else if (category == ItemCategoryClassifier.Category.FOOD) {
         return 7.0F * signed(p.seed ^ 1327217884);
      } else if (materialCategory(category) && !ItemCategoryClassifier.flexible(stack)) {
         return (MaterialKinds.rod(stack) && fp.width() <= fp.depth() * 0.33F ? 3 : 7) * signed(p.seed ^ 1327217884);
      } else if (category == ItemCategoryClassifier.Category.EQUIPMENT) {
         return Math.clamp(yaw, -5.0F, 5.0F);
      } else {
         return category != ItemCategoryClassifier.Category.LONG_TOOLS && stack.getMaxStackSize() > 1 ? Math.clamp(yaw, -3.0F, 3.0F) : yaw;
      }
   }

   public MatLayout.SeparateDestination separateDestination(AbstractContainerMenu menu, int origin, float x, float z, ItemFootprint fp) {
      return this.separateDestination(menu, origin, x, z, fp, false);
   }

   public MatLayout.SeparateDestination separateDestination(AbstractContainerMenu menu, int origin, float x, float z, ItemFootprint fp, boolean exact) {
      ItemPresentationData.Compartment c = this.compartmentAt(x, z);
      if (c != null && this.separateArea(c) && ItemCategoryClassifier.acceptsSector(menu.getCarried(), c.category())) {
         Slot destination = null;
         MatLayout.Place source = this.place(origin);
         if (source != null && source.group == MatLayout.Group.SUPPLIES && !menu.getSlot(origin).hasItem() && menu.getSlot(origin).mayPlace(menu.getCarried())) {
            destination = menu.getSlot(origin);
         }

         if (destination == null) {
            for (MatLayout.Place p : this.storagePlaces()) {
               Slot s = menu.getSlot(p.slot);
               if (!s.hasItem() && s.mayPlace(menu.getCarried())) {
                  destination = s;
                  break;
               }
            }
         }

         if (destination == null) {
            return null;
         }

         float hw = fp.width() * 0.5F;
         float hd = fp.depth() * 0.5F;
         float best = Float.POSITIVE_INFINITY;
         float bx = 0.0F;
         float bz = 0.0F;
         if (exact) {
            float yaw = this.separateYaw(destination, fp, menu.getCarried());
            double angle = Math.toRadians(yaw);
            float co = (float)Math.abs(Math.cos(angle));
            float si = (float)Math.abs(Math.sin(angle));
            float w = hw;
            hw = w * co + hd * si;
            hd = hd * co + w * si;
         }

         if (!MatInventoryClient.config.gridEnabled) {
            if (!exact && source != null && source.group == MatLayout.Group.SUPPLIES && source.occupied) {
               hw = this.looseHalfWidth(source, false);
               hd = this.looseHalfDepth(source, false);
            }

            this.freeSearch.reset(c.x0() + hw + 0.36F, c.z0() + hd + 0.36F, c.x1() - hw - 0.36F, c.z1() - hd - 0.36F, hw, hd);

            for (MatLayout.Place p : this.places) {
               if (p.visible && menu.getSlot(p.slot).hasItem()) {
                  this.freeSearch.add(p.x, p.z, this.looseHalfWidth(p, false), this.looseHalfDepth(p, false));
               }
            }

            if (exact) {
               return this.freeSearch.freeAt(x, z) ? new MatLayout.SeparateDestination(destination, x, z) : null;
            } else {
               return this.freeSearch.find(x, z) ? new MatLayout.SeparateDestination(destination, this.freeSearch.x, this.freeSearch.z) : null;
            }
         } else {
            MatLayout.Anchor clicked = exact
               ? this.anchors(c).stream().min(Comparator.comparingDouble(a -> (a.x - x) * (a.x - x) + (a.z - z) * (a.z - z))).orElse(null)
               : null;

            for (MatLayout.Anchor anchor : this.anchors(c)) {
               float px = anchor.x;
               float pz = anchor.z;
               if ((!exact || anchor == clicked)
                  && !(px - hw < c.x0() + 0.12F)
                  && !(px + hw > c.x1() - 0.12F)
                  && !(pz - hd < c.z0() + 0.12F)
                  && !(pz + hd > c.z1() - 0.12F)) {
                  boolean collision = false;

                  for (MatLayout.Place p : this.places) {
                     if (p.visible
                        && menu.getSlot(p.slot).hasItem()
                        && Math.abs(px - p.x) < hw + p.halfWidth + 0.1F
                        && Math.abs(pz - p.z) < hd + p.halfDepth + 0.1F) {
                        collision = true;
                        break;
                     }
                  }

                  float distance = (px - x) * (px - x) + (pz - z) * (pz - z);
                  if (!collision && distance < best) {
                     best = distance;
                     bx = px;
                     bz = pz;
                  }
               }
            }

            return Float.isFinite(best) ? new MatLayout.SeparateDestination(destination, bx, bz) : null;
         }
      } else {
         return null;
      }
   }

   public void placeFlexible(ItemStack stack, float x, float z) {
      if (ItemCategoryClassifier.flexible(stack)) {
         MatInventoryClient.config.flexiblePositions.put(ItemPresentationData.id(stack), new float[]{x, z});
         String id = ItemPresentationData.id(stack);
         this.lastFlexiblePositions.values().removeIf(p -> p.item.equals(id));
         this.looseFlexiblePositions.values().removeIf(p -> p.item.equals(id));
         MatInventoryClient.config.save();
         this.signature = Long.MIN_VALUE;
      }
   }

   public Slot flexibleTarget(AbstractContainerMenu menu, float x, float z, Set<Slot> excluded) {
      ItemPresentationData.Compartment c = this.compartmentAt(x, z);
      ItemStack stack = menu.getCarried();
      if (c != null && this.storageArea(c) && ItemCategoryClassifier.flexible(stack) && ItemCategoryClassifier.acceptsSector(stack, c.category())) {
         for (MatLayout.Place p : this.storagePlaces()) {
            Slot s = menu.getSlot(p.slot());
            if (!excluded.contains(s)
               && s.hasItem()
               && ItemStack.isSameItemSameComponents(s.getItem(), stack)
               && s.getItem().getCount() < s.getMaxStackSize(stack)
               && s.mayPlace(stack)) {
               return s;
            }
         }

         for (MatLayout.Place p : this.storagePlaces()) {
            Slot s = menu.getSlot(p.slot());
            if (!excluded.contains(s) && !s.hasItem() && s.mayPlace(stack)) {
               return s;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private boolean hasFreePosition(MatConfig.DetachedItem saved) {
      if (saved.freePosition != null) {
         return saved.freePosition;
      }

      if (saved.gridCell == null) {
         return true;
      }

      ItemPresentationData.Compartment area = this.compartmentAt(saved.x, saved.z);
      if (area != null) {
         for (MatLayout.Anchor anchor : this.anchors(area)) {
            if (Math.abs(anchor.x - saved.x) < 0.002F && Math.abs(anchor.z - saved.z) < 0.002F) {
               return false;
            }
         }
      }

      return true;
   }

   private void packFlexible(AbstractContainerMenu menu, List<MatLayout.Place> supplied, ItemFootprint[] footprints) {
      ArrayList<MatLayout.Place> occupied = new ArrayList<>();
      ArrayList<MatLayout.FlexibleRequest> requests = new ArrayList<>();
      boolean grid = MatInventoryClient.config.gridEnabled;
      boolean gridDirty = false;

      for (MatLayout.Place p : this.places) {
         if (p.visible && p.occupied && !p.freePlacement) {
            occupied.add(p);
            this.placementScales[p.slot] = InventoryScales.factor(menu.getSlot(p.slot).getItem());
         }
      }

      for (MatLayout.Place p : supplied) {
         if (p.freePlacement) {
            ItemFootprint fp = footprints[p.slot];
            MatConfig.DetachedItem saved = this.detachedPosition(menu.getSlot(p.slot));
            MatLayout.FlexiblePosition remembered = grid
               ? this.lastFlexiblePositions.get(p.slot)
               : this.looseFlexiblePositions.getOrDefault(p.slot, this.lastFlexiblePositions.get(p.slot));
            if (remembered != null && !remembered.item.equals(fp.id())) {
               remembered = null;
            }

            boolean savedFree = saved != null && this.hasFreePosition(saved);
            float[] requested = saved == null || !grid && !savedFree
               ? (
                  remembered != null
                     ? new float[]{remembered.x, remembered.z}
                     : (saved != null ? new float[]{saved.x, saved.z} : MatInventoryClient.config.flexiblePositions.get(fp.id()))
               )
               : new float[]{saved.x, saved.z};
            if (!grid && saved != null && !savedFree && remembered == null && saved.gridCell != null) {
               label352:
               for (ItemPresentationData.Compartment area : this.compartments()) {
                  if (this.storageArea(area)) {
                     for (MatLayout.Anchor anchor : this.anchors(area)) {
                        if (anchor.id.equals(saved.gridCell)) {
                           requested = new float[]{anchor.x, anchor.z};
                           break label352;
                        }
                     }
                  }
               }
            }

            ItemPresentationData.Compartment preferred = requested == null
               ? ItemPresentationData.compartment(ItemCategoryClassifier.Category.OTHER)
               : this.compartmentAt(requested[0], requested[1]);
            if (requested == null && fp.depth() > preferred.depth() - 0.48F) {
               preferred = ItemPresentationData.compartment(ItemCategoryClassifier.Category.LONG_TOOLS);
            }

            if (preferred == null
               || !this.storageArea(preferred)
               || !ItemCategoryClassifier.acceptsSector(menu.getSlot(p.slot).getItem(), preferred.category())) {
               requested = null;
               preferred = ItemPresentationData.compartment(fp.classification().category());
            }

            requests.add(
               new MatLayout.FlexibleRequest(
                  p, fp, InventoryScales.factor(menu.getSlot(p.slot).getItem()), saved, requested, preferred, saved != null || remembered != null, savedFree
               )
            );
         }
      }

      requests.sort(Comparator.comparing(r -> !r.stable));

      for (MatLayout.FlexibleRequest request : requests) {
         MatLayout.Place p = request.place;
         ItemFootprint fp = request.footprint;
         MatConfig.DetachedItem saved = request.saved;
         float[] requested = request.requested;
         ItemPresentationData.Compartment preferred = request.preferred;
         float manualScale = request.scale;
         float unitHalfWidth = p.detachedPlacement ? p.halfWidth / p.sizeFactor : fp.width() * 0.5F;
         float unitHalfDepth = p.detachedPlacement ? p.halfDepth / p.sizeFactor : fp.depth() * 0.5F;
         p.visible = true;
         p.materialCell = -1;
         p.materialStacks = 0;
         if (!p.detachedPlacement) {
            p.targetYaw = 0.0F;
         }

         double angle = Math.toRadians(p.targetYaw);
         float co = (float)Math.abs(Math.cos(angle));
         float si = (float)Math.abs(Math.sin(angle));
         float worldUnitWidth = unitHalfWidth * co + unitHalfDepth * si;
         float worldUnitDepth = unitHalfDepth * co + unitHalfWidth * si;
         float inset = grid ? 0.12F : 0.36F;
         ArrayList<ItemPresentationData.Compartment> areas = new ArrayList<>();
         areas.add(preferred);

         for (ItemPresentationData.Compartment c : this.compartments()) {
            if (this.storageArea(c) && !c.equals(preferred) && ItemCategoryClassifier.acceptsSector(menu.getSlot(p.slot).getItem(), c.category())) {
               areas.add(c);
            }
         }

         boolean placed = false;
         float storedScale = saved != null && saved.presentationScale != null && Float.isFinite(saved.presentationScale) && saved.presentationScale > 0.0F
            ? saved.presentationScale
            : fp.storageScale();
         Iterator position = areas.iterator();

         label287:
         while (true) {
            ItemPresentationData.Compartment c;
            float factor;
            while (true) {
               if (!position.hasNext()) {
                  break label287;
               }

               c = (ItemPresentationData.Compartment)position.next();
               factor = Math.min(
                  1.0F,
                  Math.min(
                     (c.width() - inset * 2.0F) / (worldUnitWidth * manualScale * 2.0F), (c.depth() - inset * 2.0F) / (worldUnitDepth * manualScale * 2.0F)
                  )
               );
               if (!p.detachedPlacement) {
                  break;
               }

               if (!(factor < storedScale)) {
                  factor = storedScale;
                  break;
               }
            }

            float hw = worldUnitWidth * factor * manualScale;
            float hd = worldUnitDepth * factor * manualScale;
            float wantX = requested != null && c.equals(preferred) ? requested[0] : (c.x0() + c.x1()) * 0.5F;
            float wantZ = requested != null && c.equals(preferred) ? requested[1] : (c.z0() + c.z1()) * 0.5F;
            float best = Float.POSITIVE_INFINITY;
            float bx = 0.0F;
            float bz = 0.0F;
            String bestCell = null;
            if (!grid) {
               this.freeSearch.reset(c.x0() + hw + inset, c.z0() + hd + inset, c.x1() - hw - inset, c.z1() - hd - inset, hw, hd);

               for (MatLayout.Place q : occupied) {
                  this.freeSearch
                     .add(
                        q.targetX,
                        q.targetZ,
                        this.looseHalfWidth(q, true) * this.placementScales[q.slot],
                        this.looseHalfDepth(q, true) * this.placementScales[q.slot]
                     );
               }

               if (this.freeSearch.find(wantX, wantZ)) {
                  best = 0.0F;
                  bx = this.freeSearch.x;
                  bz = this.freeSearch.z;
               }
            } else {
               for (MatLayout.Anchor anchor : this.anchors(c)) {
                  float x = anchor.x;
                  float z = anchor.z;
                  if (!(x - hw < c.x0() + 0.12F) && !(x + hw > c.x1() - 0.12F) && !(z - hd < c.z0() + 0.12F) && !(z + hd > c.z1() - 0.12F)) {
                     boolean collision = false;

                     for (MatLayout.Place q : occupied) {
                        if (Math.abs(x - q.targetX) < hw + q.halfWidth * this.placementScales[q.slot] + 0.1F
                           && Math.abs(z - q.targetZ) < hd + q.halfDepth * this.placementScales[q.slot] + 0.1F) {
                           collision = true;
                           break;
                        }
                     }

                     float distance = (x - wantX) * (x - wantX) + (z - wantZ) * (z - wantZ);
                     if (saved != null && anchor.id.equals(saved.gridCell)) {
                        distance -= 10000.0F;
                     }

                     if (!collision && distance < best) {
                        best = distance;
                        bx = x;
                        bz = z;
                        bestCell = anchor.id;
                     }
                  }
               }
            }

            if (Float.isFinite(best)) {
               p.targetX = bx;
               p.targetZ = bz;
               p.sizeFactor = factor;
               p.halfWidth = unitHalfWidth * factor;
               p.halfDepth = unitHalfDepth * factor;
               p.bin = c.category().name();
               placed = true;
               if (grid && saved != null && !Objects.equals(saved.gridCell, bestCell)) {
                  saved.gridCell = bestCell;
                  gridDirty = true;
               }
               break;
            }
         }

         if (!placed) {
            p.sizeFactor = p.detachedPlacement ? storedScale : Math.min(1.0F, 0.6F / Math.max(fp.width(), fp.depth()));
            p.halfWidth = unitHalfWidth * p.sizeFactor;
            p.halfDepth = unitHalfDepth * p.sizeFactor;
            if (grid) {
               List<MatLayout.Anchor> available = this.anchors(preferred);
               MatLayout.Anchor anchor = available.get(Math.floorMod(p.slot, available.size()));
               p.targetX = anchor.x;
               p.targetZ = anchor.z;
            } else {
               ItemPresentationData.Compartment fit = preferred;
               float maximum = 0.0F;

               for (ItemPresentationData.Compartment candidate : areas) {
                  float limit = Math.min(
                     (candidate.width() - inset * 2.0F) / (worldUnitWidth * manualScale * 2.0F),
                     (candidate.depth() - inset * 2.0F) / (worldUnitDepth * manualScale * 2.0F)
                  );
                  if (limit >= p.sizeFactor) {
                     fit = candidate;
                     maximum = limit;
                     break;
                  }

                  if (limit > maximum) {
                     fit = candidate;
                     maximum = limit;
                  }
               }

               p.sizeFactor = Math.min(p.sizeFactor, maximum * 0.99999F);
               p.halfWidth = unitHalfWidth * p.sizeFactor;
               p.halfDepth = unitHalfDepth * p.sizeFactor;
               float hw = worldUnitWidth * p.sizeFactor * manualScale;
               float hd = worldUnitDepth * p.sizeFactor * manualScale;
               p.targetX = Math.clamp(requested != null ? requested[0] : (fit.x0() + fit.x1()) * 0.5F, fit.x0() + hw + inset, fit.x1() - hw - inset);
               p.targetZ = Math.clamp(requested != null ? requested[1] : (fit.z0() + fit.z1()) * 0.5F, fit.z0() + hd + inset, fit.z1() - hd - inset);
               preferred = fit;
            }

            p.bin = preferred.category().name();
         }

         if (!grid && saved != null && !request.savedFree) {
            saved.x = p.targetX;
            saved.z = p.targetZ;
            saved.freePosition = true;
            gridDirty = true;
         }

         MatLayout.FlexiblePosition positionx = new MatLayout.FlexiblePosition(fp.id(), p.targetX, p.targetZ, grid);
         this.lastFlexiblePositions.put(p.slot, positionx);
         if (!grid) {
            this.looseFlexiblePositions.put(p.slot, positionx);
         }

         p.radius = Math.max(p.halfWidth, p.halfDepth);
         occupied.add(p);
         this.placementScales[p.slot] = manualScale;
      }

      if (gridDirty) {
         MatInventoryClient.config.save();
      }
   }

   private void packVehicles(AbstractContainerMenu menu, List<MatLayout.Place> supplied, ItemFootprint[] footprints) {
      this.vehicleCells.clear();
      this.vehiclePreviews.clear();
      List<MatLayout.Place> group = supplied.stream()
         .filter(px -> px.category == ItemCategoryClassifier.Category.VEHICLES)
         .sorted(Comparator.comparingInt(MatLayout.Place::slot))
         .toList();
      this.vehiclePreview = group.isEmpty() && VehicleKinds.isVehicle(menu.getCarried());
      this.totals.put(ItemCategoryClassifier.Category.VEHICLES, group.size());
      this.shown.put(ItemCategoryClassifier.Category.VEHICLES, Math.min(3, group.size()));
      int offset = Math.clamp(this.offsets.getOrDefault(ItemCategoryClassifier.Category.VEHICLES, 0).intValue(), 0, Math.max(0, group.size() - 3));
      this.offsets.put(ItemCategoryClassifier.Category.VEHICLES, offset);
      List<MatLayout.Place> selected = group.subList(offset, Math.min(group.size(), offset + 3));
      List<String> old = this.vehicleOrder.keys();
      this.vehicleOrder.ensure(selected.stream().map(px -> storageKey(menu.getSlot(px.slot))).toList(), List.of(0, 1, 2));
      if (!old.equals(this.vehicleOrder.keys())) {
         this.saveStorageOrders();
      }

      if (!group.isEmpty() || this.vehiclePreview) {
         ItemPresentationData.Compartment food = ItemPresentationData.compartment(ItemCategoryClassifier.Category.FOOD);
         float pitch = 5.91F;

         for (MatLayout.Place p : group) {
            ItemFootprint fp = footprints[p.slot];
            if (menu.getSlot(p.slot).getItem().getItem() instanceof BoatItem) {
               pitch = Math.max(pitch, 5.48F * fp.depth() / fp.width() + 0.43F);
            }
         }

         if (this.vehiclePreview && menu.getCarried().getItem() instanceof BoatItem) {
            pitch = 7.9F;
         }

         ItemPresentationData.Compartment blocks = ItemPresentationData.compartment(ItemCategoryClassifier.Category.BLOCKS);
         float blockTop = blocks.z0() - ((blocks.depth() - 0.4F) * 0.5F + 0.4F);
         float right = food.x1();
         float left = right - pitch * 3.0F;
         float bottom = Math.min(food.z0(), blockTop);
         float top = bottom - 5.91F;

         for (int i = 0; i < 3; i++) {
            this.vehicleCells.add(new MatLayout.VehicleCell(i, left + i * pitch + 0.2F, top + 0.2F, left + (i + 1) * pitch - 0.2F, bottom - 0.2F));
         }

         TreeSet<Integer> occupied = new TreeSet<>();

         for (MatLayout.Place p : selected) {
            occupied.add(this.vehicleOrder.cell(storageKey(menu.getSlot(p.slot))));
         }

         ArrayList<ItemPresentationData.Compartment> regions = new ArrayList<>(this.activeCompartments);
         boolean holding = VehicleKinds.isVehicle(menu.getCarried());

         for (int i = 0; i < 3; i++) {
            if (occupied.contains(i) || holding) {
               String id = "vehicles_" + i;
               regions.add(
                  new ItemPresentationData.Compartment(id, ItemCategoryClassifier.Category.VEHICLES, left + i * pitch, top, left + (i + 1) * pitch, bottom)
               );
               if (!occupied.contains(i)) {
                  this.vehiclePreviews.add(id);
               }
            }
         }

         float baseX0 = food.x0();
         float baseX1 = food.x0();
         if (!occupied.isEmpty()) {
            float solidLeft = left + occupied.first().intValue() * pitch;
            float solidRight = left + (occupied.last() + 1) * pitch;
            baseX0 = Math.max(food.x0(), solidLeft);
            baseX1 = Math.min(food.x1(), Math.max(solidRight, food.x0() + 0.35F));
            int previous = occupied.first();

            for (int i : occupied) {
               if (i > previous + 1) {
                  regions.add(
                     new ItemPresentationData.Compartment(
                        "vehicles_join_" + previous,
                        ItemCategoryClassifier.Category.VEHICLES,
                        left + (previous + 1) * pitch,
                        bottom - 0.28F,
                        left + i * pitch,
                        bottom
                     )
                  );
               }

               previous = i;
            }

            if (solidRight < baseX1) {
               regions.add(
                  new ItemPresentationData.Compartment(
                     "vehicles_join_base", ItemCategoryClassifier.Category.VEHICLES, solidRight, bottom - 0.28F, baseX1, bottom
                  )
               );
            }

            if (bottom < food.z0()) {
               regions.add(new ItemPresentationData.Compartment("vehicles_link", ItemCategoryClassifier.Category.VEHICLES, baseX0, bottom, baseX1, food.z0()));
            }
         }

         if (holding && bottom < food.z0()) {
            if (baseX0 > food.x0()) {
               String id = "vehicles_preview_left";
               regions.add(new ItemPresentationData.Compartment(id, ItemCategoryClassifier.Category.VEHICLES, food.x0(), bottom, baseX0, food.z0()));
               this.vehiclePreviews.add(id);
            }

            if (baseX1 < food.x1()) {
               String id = "vehicles_preview_right";
               regions.add(new ItemPresentationData.Compartment(id, ItemCategoryClassifier.Category.VEHICLES, baseX1, bottom, food.x1(), food.z0()));
               this.vehiclePreviews.add(id);
            }
         }

         this.activeCompartments = List.copyOf(regions);

         for (int i = 0; i < group.size(); i++) {
            MatLayout.Place p = group.get(i);
            p.visible = i >= offset && i < offset + 3;
            p.bin = ItemCategoryClassifier.Category.VEHICLES.name();
            if (p.visible) {
               ItemFootprint fp = footprints[p.slot];
               MatLayout.VehicleCell cell = this.vehicleCells.get(this.vehicleOrder.cell(storageKey(menu.getSlot(p.slot))));
               p.vehicleCell = cell.index();
               boolean boat = menu.getSlot(p.slot).getItem().getItem() instanceof BoatItem;
               p.sizeFactor = 5.48F / (boat ? fp.width() : Math.max(fp.width(), fp.depth()));
               p.targetYaw = !boat && !(fp.width() > fp.depth()) ? 0.0F : 90.0F;
               p.targetX = (cell.x0() + cell.x1()) * 0.5F;
               p.targetZ = (cell.z0() + cell.z1()) * 0.5F;
               p.halfWidth = fp.width() * p.sizeFactor * 0.5F;
               p.halfDepth = fp.depth() * p.sizeFactor * 0.5F;
               p.radius = Math.max(p.halfWidth, p.halfDepth);
            }
         }
      }
   }

   private void packFoods(AbstractContainerMenu menu, List<MatLayout.Place> supplied) {
      LinkedHashMap<String, List<MatLayout.Place>> groups = new LinkedHashMap<>();
      List<String> oldOrder = this.foodOrder.keys();

      for (MatLayout.Place p : supplied) {
         if (p.category == ItemCategoryClassifier.Category.FOOD) {
            p.visible = false;
            Slot slot = menu.getSlot(p.slot());
            String kind = ConsumableKinds.key(slot.getItem());
            String key = pileKey(slot, kind);
            this.foodOrder.migrate(kind, key);
            groups.put(key, List.of(p));
         }
      }

      ArrayList<String> keys = new ArrayList<>(groups.keySet());
      keys.sort(Comparator.naturalOrder());
      int capacity = 14;
      int offset = Math.clamp(this.offsets.getOrDefault(ItemCategoryClassifier.Category.FOOD, 0).intValue(), 0, Math.max(0, keys.size() - capacity));
      List<String> selected = keys.subList(offset, Math.min(keys.size(), offset + capacity));
      this.offsets.put(ItemCategoryClassifier.Category.FOOD, offset);
      this.totals.put(ItemCategoryClassifier.Category.FOOD, keys.size());
      this.shown.put(ItemCategoryClassifier.Category.FOOD, selected.size());
      this.foodOrder.ensure(selected);
      if (!oldOrder.equals(this.foodOrder.keys())) {
         this.saveFoodOrder();
      }

      ItemPresentationData.Compartment region = ItemPresentationData.compartment(ItemCategoryClassifier.Category.FOOD);
      int columns = 7;
      int rows = 2;
      float gap = 0.14F;
      float side = Math.min((region.width() - 0.2F - gap * (columns - 1)) / columns, (region.depth() - 0.2F - gap * (rows - 1)) / rows);
      float left = region.x0() + (region.width() - (columns * side + (columns - 1) * gap)) * 0.5F;
      float top = region.z0() + (region.depth() - (rows * side + (rows - 1) * gap)) * 0.5F;
      this.foodCells.clear();

      for (int i = 0; i < capacity; i++) {
         float x = left + i % columns * (side + gap);
         float z = top + i / columns * (side + gap);
         this.foodCells.add(new MatLayout.FoodCell(i, this.foodOrder.key(i), x, z, x + side, z + side));
      }

      for (String key : selected) {
         List<MatLayout.Place> group = groups.get(key);
         MatLayout.Place p = group.getFirst();
         MatLayout.FoodCell cell = this.foodCell(this.foodOrder.cell(key));
         p.visible = true;
         p.foodCell = cell.index();
         p.foodStacks = group.size();
         p.bin = ItemCategoryClassifier.Category.FOOD.name();
         p.targetX = (cell.x0() + cell.x1()) * 0.5F;
         p.targetZ = (cell.z0() + cell.z1()) * 0.5F;
         p.radius = Math.max(p.halfWidth, p.halfDepth);
      }
   }

   private void packMaterials(AbstractContainerMenu menu, List<MatLayout.Place> supplied) {
      TreeMap<String, List<MatLayout.Place>> groups = new TreeMap<>();
      HashSet<String> resources = new HashSet<>();
      List<String> previous = this.materialOrder.keys();

      for (MatLayout.Place p : supplied) {
         if (materialCategory(p.category)) {
            p.visible = false;
            Slot slot = menu.getSlot(p.slot);
            String kind = MaterialKinds.key(slot.getItem());
            if (kind != null) {
               String key = pileKey(slot, kind);
               this.materialOrder.migrate(kind, key);
               groups.put(key, List.of(p));
               if (p.category == ItemCategoryClassifier.Category.RESOURCES) {
                  resources.add(key);
               }
            }
         }
      }

      ArrayList<String> keys = new ArrayList<>(groups.keySet());
      int capacity = 25;
      int offset = Math.clamp(this.offsets.getOrDefault(ItemCategoryClassifier.Category.RESOURCES, 0).intValue(), 0, Math.max(0, keys.size() - capacity));
      List<String> selected = keys.subList(offset, Math.min(keys.size(), offset + capacity));

      for (ItemCategoryClassifier.Category category : List.of(ItemCategoryClassifier.Category.RESOURCES, ItemCategoryClassifier.Category.OTHER)) {
         this.offsets.put(category, offset);
         this.totals.put(category, keys.size());
         this.shown.put(category, selected.size());
      }

      this.materialOrder.ensure(selected, resources);
      if (!previous.equals(this.materialOrder.keys())) {
         this.saveMaterialOrder();
      }

      this.materialCells.clear();
      this.materialGrid(ItemCategoryClassifier.Category.RESOURCES, 3, 5, 0);
      this.materialGrid(ItemCategoryClassifier.Category.OTHER, 5, 2, 15);

      for (String key : selected) {
         List<MatLayout.Place> group = groups.get(key);
         MatLayout.Place p = group.getFirst();
         MatLayout.MaterialCell cell = this.materialCell(this.materialOrder.cell(key));
         p.visible = true;
         p.materialCell = cell.index();
         p.materialStacks = 1;
         p.bin = cell.category().name();
         p.targetX = (cell.x0() + cell.x1()) * 0.5F;
         p.targetZ = (cell.z0() + cell.z1()) * 0.5F;
         p.radius = Math.max(p.halfWidth, p.halfDepth);
      }
   }

   private void materialGrid(ItemCategoryClassifier.Category category, int columns, int rows, int first) {
      ItemPresentationData.Compartment region = ItemPresentationData.compartment(category);
      float gap = 0.14F;
      float side = Math.min((region.width() - 0.2F - gap * (columns - 1)) / columns, (region.depth() - 0.2F - gap * (rows - 1)) / rows);
      float left = region.x0() + (region.width() - (columns * side + (columns - 1) * gap)) * 0.5F;
      float top = region.z0() + (region.depth() - (rows * side + (rows - 1) * gap)) * 0.5F;

      for (int i = 0; i < columns * rows; i++) {
         float x = left + i % columns * (side + gap);
         float z = top + i / columns * (side + gap);
         this.materialCells.add(new MatLayout.MaterialCell(first + i, this.materialOrder.key(first + i), category, x, z, x + side, z + side));
      }
   }

   private void packBlocks(AbstractContainerMenu menu, List<MatLayout.Place> supplied) {
      ItemPresentationData.Compartment region = ItemPresentationData.compartment(ItemCategoryClassifier.Category.BLOCKS);
      float pitchX = (region.width() - 0.4F) / 2.0F;
      float pitchZ = (region.depth() - 0.4F) / 4.0F;
      LinkedHashMap<String, List<MatLayout.Place>> groups = new LinkedHashMap<>();
      List<String> oldOrder = this.blockOrder.keys();

      for (MatLayout.Place p : supplied) {
         if (p.category == ItemCategoryClassifier.Category.BLOCKS) {
            Slot slot = menu.getSlot(p.slot());
            String kind = ItemPresentationData.id(slot.getItem());
            String key = pileKey(slot, kind);
            this.blockOrder.migrate(kind, key);
            p.visible = false;
            groups.put(key, List.of(p));
         }
      }

      ArrayList<String> keys = new ArrayList<>(groups.keySet());
      keys.sort(Comparator.comparingInt(keyx -> keyx.hashCode() ^ this.blockSeed));
      int offset = Math.clamp(this.offsets.getOrDefault(ItemCategoryClassifier.Category.BLOCKS, 0).intValue(), 0, Math.max(0, keys.size() - 16));
      this.offsets.put(ItemCategoryClassifier.Category.BLOCKS, offset);
      this.totals.put(ItemCategoryClassifier.Category.BLOCKS, keys.size());
      this.shown.put(ItemCategoryClassifier.Category.BLOCKS, Math.min(16, keys.size()));
      List<String> selected = keys.subList(offset, Math.min(keys.size(), offset + 16));
      ArrayList<Integer> preference = new ArrayList<>();

      for (int tier = 0; tier < 3; tier++) {
         ArrayList<Integer> tierCells = new ArrayList<>();

         for (int i = tier == 0 ? 0 : (tier == 1 ? 8 : 12); i < (tier == 0 ? 8 : (tier == 1 ? 12 : 16)); i++) {
            tierCells.add(i);
         }

         Collections.shuffle(tierCells, new Random(this.blockSeed + tier));
         preference.addAll(tierCells);
      }

      this.blockOrder.ensure(selected, preference);
      if (!oldOrder.equals(this.blockOrder.keys())) {
         this.saveStorageOrders();
      }

      String preview = this.blockPreviewId(menu);
      boolean upperPresent = selected.stream().anyMatch(k -> this.blockOrder.cell(k) >= 8 && this.blockOrder.cell(k) < 12);
      boolean lowerPresent = selected.stream().anyMatch(k -> this.blockOrder.cell(k) >= 12);
      this.upperBlocksPreview = !upperPresent && "extra_blocks_top".equals(preview);
      this.lowerBlocksPreview = !lowerPresent && "extra_blocks_bottom".equals(preview);
      float extensionDepth = 2.0F * pitchZ + 0.4F;
      ItemPresentationData.Compartment upper = new ItemPresentationData.Compartment(
         "extra_blocks_top", ItemCategoryClassifier.Category.BLOCKS, region.x0(), region.z0() - extensionDepth, region.x1(), region.z0()
      );
      ItemPresentationData.Compartment lower = new ItemPresentationData.Compartment(
         "extra_blocks_bottom", ItemCategoryClassifier.Category.BLOCKS, region.x0(), region.z1(), region.x1(), region.z1() + extensionDepth
      );
      ArrayList<ItemPresentationData.Compartment> regions = new ArrayList<>(this.activeCompartments);
      if (upperPresent || this.upperBlocksPreview) {
         regions.add(upper);
      }

      if (lowerPresent || this.lowerBlocksPreview) {
         regions.add(lower);
      }

      this.activeCompartments = List.copyOf(regions);
      this.blockCells.clear();

      for (int i = 0; i < 16; i++) {
         if ((i < 8 || i >= 12 || upperPresent || this.upperBlocksPreview) && (i < 12 || lowerPresent || this.lowerBlocksPreview)) {
            ItemPresentationData.Compartment area = i < 8 ? region : (i < 12 ? upper : lower);
            int local = i < 8 ? i : (i < 12 ? i - 8 : i - 12);
            float x = area.x0() + 0.2F + local % 2 * pitchX;
            float z = area.z0() + 0.2F + local / 2 * pitchZ;
            this.blockCells.add(new MatLayout.BlockCell(i, x + 0.06F, z + 0.06F, x + pitchX - 0.06F, z + pitchZ - 0.06F));
         }
      }

      for (String key : selected) {
         int cell = this.blockOrder.cell(key);
         ItemPresentationData.Compartment area = cell < 8 ? region : (cell < 12 ? upper : lower);
         int local = cell < 8 ? cell : (cell < 12 ? cell - 8 : cell - 12);
         List<MatLayout.Place> group = groups.get(key);
         MatLayout.Place p = group.getFirst();
         if (VisualItemKinds.bundle(menu.getSlot(p.slot).getItem())) {
            float fit = Math.min(1.0F, Math.min((pitchX - 0.25F) / (2.0F * p.halfWidth), (pitchZ - 0.25F) / (2.0F * p.halfDepth)));
            p.sizeFactor *= fit;
            p.halfWidth *= fit;
            p.halfDepth *= fit;
         }

         p.visible = true;
         p.blockCell = cell;
         p.blockTotal = group.stream().mapToInt(q -> menu.getSlot(q.slot()).getItem().getCount()).sum();
         p.blockStacks = group.size();
         p.bin = ItemCategoryClassifier.Category.BLOCKS.name();
         p.targetX = area.x0() + 0.2F + (local % 2 + 0.5F) * pitchX + 0.025F * signed(p.seed ^ this.blockSeed);
         p.targetZ = area.z0() + 0.2F + (local / 2 + 0.5F) * pitchZ + 0.035F * signed(p.seed * 17 ^ this.blockSeed);
         p.radius = Math.max(p.halfWidth, p.halfDepth);
      }
   }

   private String blockPreviewId(AbstractContainerMenu menu) {
      if (!menu.getCarried().isEmpty() && ItemCategoryClassifier.classify(menu.getCarried()).category() == ItemCategoryClassifier.Category.BLOCKS) {
         int stacks = 0;

         for (MatLayout.Place p : this.storagePlaces()) {
            ItemStack stack = menu.getSlot(p.slot()).getItem();
            if (!stack.isEmpty() && ItemCategoryClassifier.classify(stack).category() == ItemCategoryClassifier.Category.BLOCKS) {
               if (ItemStack.isSameItemSameComponents(stack, menu.getCarried()) && stack.getCount() < menu.getSlot(p.slot()).getMaxStackSize(menu.getCarried())
                  )
                {
                  return null;
               }

               stacks++;
            }
         }

         return stacks == 8 ? "extra_blocks_top" : (stacks == 12 ? "extra_blocks_bottom" : null);
      } else {
         return null;
      }
   }

   private void packTools(AbstractContainerMenu menu, List<MatLayout.Place> supplied, ItemFootprint[] footprints) {
      boolean manualDirty = false;
      this.toolGroups.clear();
      this.toolCells.clear();

      for (MatLayout.Place p : supplied) {
         if (p.category == ItemCategoryClassifier.Category.LONG_TOOLS) {
            p.toolCell = this.toolCellFor(menu.getSlot(p.slot).getItem());
            this.toolGroups.computeIfAbsent(p.toolCell, k -> new ArrayList<>()).add(p);
         }
      }

      String heldKind = toolKind(menu.getCarried());
      int carried = this.toolCellFor(menu.getCarried());
      ArrayList<Integer> regularIndices = new ArrayList<>();

      for (int i = 0; i < this.toolOrder.keys().size(); i++) {
         String kind = this.toolOrder.key(i);
         if (!"trident".equals(kind) && !"fishing_rod".equals(kind) && !kind.startsWith("empty_cell_")) {
            regularIndices.add(i);
         }
      }

      boolean modded = this.toolGroups.keySet().stream().anyMatch(i -> this.toolOrder.key(i).startsWith("external:"))
         || heldKind != null && heldKind.startsWith("external:");
      this.toolFamilyTotal = 0;
      if (modded) {
         ArrayList<Integer> extras = new ArrayList<>();

         for (int index : regularIndices.subList(Math.min(5, regularIndices.size()), regularIndices.size())) {
            if (this.toolGroups.containsKey(index) || index == carried) {
               extras.add(index);
            }
         }

         this.toolFamilyTotal = extras.size();
         this.toolFamilyOffset = Math.clamp(this.toolFamilyOffset, 0, Math.max(0, extras.size() - 5));
         int held = extras.indexOf(carried);
         if (held >= 0 && (held < this.toolFamilyOffset || held >= this.toolFamilyOffset + 5)) {
            this.toolFamilyOffset = Math.min(held, Math.max(0, extras.size() - 5));
         }

         ArrayList<Integer> visible = new ArrayList<>(regularIndices.subList(0, Math.min(5, regularIndices.size())));
         visible.addAll(extras.subList(this.toolFamilyOffset, Math.min(extras.size(), this.toolFamilyOffset + 5)));
         if (visible.size() > 5 || this.separatePreview) {
            for (int index : regularIndices) {
               if (visible.size() < 10 && !visible.contains(index) && !this.toolGroups.containsKey(index) && index != carried) {
                  visible.add(index);
               }
            }
         }

         regularIndices = visible;
      }

      int rows = 0;

      for (int index : this.toolGroups.keySet()) {
         String kind = this.toolOrder.key(index);
         if (!"trident".equals(kind) && !"fishing_rod".equals(kind)) {
            rows = Math.max(rows, regularIndices.indexOf(index) / 5);
         } else {
            rows = Math.max(rows, 1);
         }
      }

      ItemPresentationData.Compartment baseTools = ItemPresentationData.compartment(ItemCategoryClassifier.Category.LONG_TOOLS);
      boolean manualUpper = this.storagePlaces().stream().anyMatch(px -> {
         MatConfig.DetachedItem saved = this.detachedPosition(menu.getSlot(px.slot));
         return saved != null && saved.x >= baseTools.x0() && saved.x <= baseTools.x1() && saved.z < baseTools.z0() && saved.z >= baseTools.z0() - 6.1F;
      });
      if (manualUpper) {
         rows = Math.max(rows, 1);
      }

      int actualRows = rows;
      if (carried >= 0) {
         rows = Math.max(rows, !"trident".equals(heldKind) && !"fishing_rod".equals(heldKind) ? regularIndices.indexOf(carried) / 5 : 1);
      }

      if (this.needsUpperPreview(menu.getCarried()) || this.separatePreview) {
         rows = Math.max(rows, 1);
      }

      this.upperPreview = actualRows == 0 && rows > 0;
      ItemPresentationData.Compartment original = ItemPresentationData.compartment(ItemCategoryClassifier.Category.LONG_TOOLS);
      float[] widths = new float[5];
      Arrays.fill(widths, 2.2F);

      for (Entry<Integer, List<MatLayout.Place>> entry : this.toolGroups.entrySet()) {
         if (regularIndices.contains(entry.getKey())) {
            for (MatLayout.Place p : entry.getValue()) {
               float w = this.axisWidth(p) * 2.0F;
               if (!"trident".equals(toolKind(menu.getSlot(p.slot).getItem()))) {
                  widths[entry.getKey() % 5] = Math.max(widths[entry.getKey() % 5], w + 0.48F + 0.56F);
               }
            }
         }
      }

      float total = 0.0F;

      for (float w : widths) {
         total += w;
      }

      float equipmentRight = original.x0() + Math.max(original.width(), total);
      float right = equipmentRight;
      ArrayList<ItemPresentationData.Compartment> regions = new ArrayList<>();

      for (ItemPresentationData.Compartment c : ItemPresentationData.compartments()) {
         if (c.category() == ItemCategoryClassifier.Category.LONG_TOOLS) {
            regions.add(new ItemPresentationData.Compartment(c.id(), c.category(), c.x0(), c.z0(), right, c.z1()));
         } else if (c.category() == ItemCategoryClassifier.Category.EQUIPMENT) {
            regions.add(new ItemPresentationData.Compartment(c.id(), c.category(), c.x0(), c.z0(), equipmentRight, c.z1()));
         } else if (c.category() == ItemCategoryClassifier.Category.HOTBAR) {
            regions.add(new ItemPresentationData.Compartment(c.id(), c.category(), c.x0() - (c.width() - 0.56F) / 9.0F * 1.15F, c.z0(), c.x1(), c.z1()));
         } else {
            regions.add(c);
         }
      }

      float upperDepth = 6.1F;
      if (rows > 0) {
         regions.add(
            new ItemPresentationData.Compartment(
               "extra_tools", ItemCategoryClassifier.Category.LONG_TOOLS, original.x0(), original.z0() - rows * 6.1F, right, original.z0()
            )
         );
      }

      int trident = this.toolOrder.cell("trident");
      boolean tridentVisible = this.toolGroups.containsKey(trident) || "trident".equals(toolKind(menu.getCarried()));
      int fishing = this.toolOrder.cell("fishing_rod");
      boolean fishingVisible = this.toolGroups.containsKey(fishing) || "fishing_rod".equals(toolKind(menu.getCarried()));
      this.tridentPreview = !this.toolGroups.containsKey(trident) && "trident".equals(toolKind(menu.getCarried()));
      this.fishingPreview = !this.toolGroups.containsKey(fishing) && "fishing_rod".equals(toolKind(menu.getCarried()));
      if (tridentVisible) {
         regions.add(
            new ItemPresentationData.Compartment(
               "extra_tools_trident",
               ItemCategoryClassifier.Category.LONG_TOOLS,
               original.x0(),
               original.z0() - rows * 6.1F - 3.15F,
               original.x0() + 7.5F,
               original.z0() - rows * 6.1F
            )
         );
      }

      if (fishingVisible) {
         regions.add(
            new ItemPresentationData.Compartment(
               "extra_tools_fishing_rod",
               ItemCategoryClassifier.Category.LONG_TOOLS,
               original.x0() + 7.5F,
               original.z0() - rows * 6.1F - 3.15F,
               original.x0() + 13.4F,
               original.z0() - rows * 6.1F
            )
         );
      }

      this.activeCompartments = List.copyOf(regions);

      for (int row = 0; row <= rows; row++) {
         ArrayList<Integer> indices = new ArrayList<>();
         float units = 0.0F;

         for (int col = row * 5; col < Math.min(regularIndices.size(), row * 5 + 5); col++) {
            int index = regularIndices.get(col);
            indices.add(index);
            units += "crossbow".equals(this.toolOrder.key(index)) ? 2.0F : 1.0F;
         }

         if (!indices.isEmpty()) {
            float unit = (right - original.x0()) / units;
            float left = original.x0();

            for (int index : indices) {
               String kind = index < this.toolOrder.keys().size() ? this.toolOrder.key(index) : "";
               float next = left + unit * ("crossbow".equals(kind) ? 2 : 1);
               float z0 = row == 0 ? original.z0() : original.z0() - row * 6.1F;
               float z1 = row == 0 ? original.z1() : z0 + 6.1F;
               this.toolCells.add(new MatLayout.ToolCell(index, kind, left, z0, next, z1, (left + next) * 0.5F, next - left - 0.2F));
               left = next;
            }
         }
      }

      if (tridentVisible) {
         float top = original.z0() - rows * 6.1F;
         this.toolCells
            .add(new MatLayout.ToolCell(trident, "trident", original.x0() + 0.1F, top - 3.05F, original.x0() + 7.4F, top - 0.1F, original.x0() + 3.75F, 7.3F));
      }

      if (fishingVisible) {
         float top = original.z0() - rows * 6.1F;
         this.toolCells
            .add(
               new MatLayout.ToolCell(
                  fishing, "fishing_rod", original.x0() + 7.6F, top - 3.05F, original.x0() + 13.3F, top - 0.1F, original.x0() + 10.45F, 5.7F
               )
            );
      }

      this.totals.put(ItemCategoryClassifier.Category.LONG_TOOLS, this.toolGroups.values().stream().mapToInt(List::size).sum());
      this.shown.put(ItemCategoryClassifier.Category.LONG_TOOLS, this.totals.get(ItemCategoryClassifier.Category.LONG_TOOLS));
      this.offsets.put(ItemCategoryClassifier.Category.LONG_TOOLS, 0);

      for (Entry<Integer, List<MatLayout.Place>> entry : this.toolGroups.entrySet()) {
         List<MatLayout.Place> group = entry.getValue();
         MatLayout.ToolCell cell = this.toolCell(entry.getKey());
         if (cell == null) {
            for (MatLayout.Place p : group) {
               p.visible = false;
            }
         } else {
            String kind = cell.kind();
            Integer selected = this.toolFrontSlots.get(kind);
            if (selected != null) {
               for (int i = 0; i < group.size(); i++) {
                  if (group.get(i).slot() == selected) {
                     Collections.rotate(group, group.size() - 1 - i);
                     break;
                  }
               }
            }

            int visible = Math.min(toolVisibleLimit(kind), group.size());
            int offset = Math.clamp(this.toolOffsets.getOrDefault(kind, 0).intValue(), 0, Math.max(0, group.size() - visible));
            this.toolOffsets.put(kind, offset);
            boolean horizontal = "trident".equals(kind) || "fishing_rod".equals(kind);

            for (MatLayout.Place p : group) {
               if (horizontal) {
                  float w = p.halfWidth;
                  p.halfWidth = p.halfDepth;
                  p.halfDepth = w;
               }
            }

            for (int i = 0; i < group.size(); i++) {
               MatLayout.Place p = group.get(i);
               p.visible = i >= offset && i < offset + visible;
               p.toolTotal = group.size();
               p.toolOrdinal = i - offset;
               p.bin = ItemCategoryClassifier.Category.LONG_TOOLS.name();
               p.sizeFactor = 1.0F;
               p.radius = Math.max(p.halfWidth, p.halfDepth);
            }

            stackGroup(
               group.stream().filter(px -> px.visible).toList(),
               (cell.x0() + cell.x1()) * 0.5F,
               (cell.z0() + cell.z1()) * 0.5F,
               cell.dropX1() - cell.dropX0(),
               cell.z1() - cell.z0(),
               horizontal,
               "trident".equals(kind),
               "crossbow".equals(kind)
            );
            MatConfig.DetachedItem manual = MatInventoryClient.config.detachedItems.get("tool-pile:" + kind);
            if (!MatInventoryClient.config.gridEnabled && manual != null) {
               float left = Float.POSITIVE_INFINITY;
               float groupRight = -left;
               float top = left;
               float bottom = -left;

               for (MatLayout.Place p : group) {
                  if (p.visible) {
                     float scale = InventoryScales.factor(menu.getSlot(p.slot).getItem());
                     float w = this.looseHalfWidth(p, true) * scale;
                     float d = this.looseHalfDepth(p, true) * scale;
                     left = Math.min(left, p.targetX - w);
                     groupRight = Math.max(groupRight, p.targetX + w);
                     top = Math.min(top, p.targetZ - d);
                     bottom = Math.max(bottom, p.targetZ + d);
                  }
               }

               float hw = (groupRight - left) * 0.5F;
               float hd = (bottom - top) * 0.5F;
               ItemPresentationData.Compartment area = this.compartmentAt(manual.x, manual.z);
               if (area == null || area.category() != ItemCategoryClassifier.Category.LONG_TOOLS) {
                  area = this.compartmentAt((left + groupRight) * 0.5F, (top + bottom) * 0.5F);
               }

               if (area != null && area.width() >= 2.0F * (hw + 0.36F) && area.depth() >= 2.0F * (hd + 0.36F)) {
                  float x = Math.clamp(manual.x, area.x0() + hw + 0.36F, area.x1() - hw - 0.36F);
                  float z = Math.clamp(manual.z, area.z0() + hd + 0.36F, area.z1() - hd - 0.36F);
                  if (x != manual.x || z != manual.z) {
                     manual.x = x;
                     manual.z = z;
                     manualDirty = true;
                  }

                  float dx = x - (left + groupRight) * 0.5F;
                  float dz = z - (top + bottom) * 0.5F;

                  for (MatLayout.Place p : group) {
                     p.targetX += dx;
                     p.targetZ += dz;
                  }
               }
            }
         }
      }

      if (manualDirty) {
         MatInventoryClient.config.save();
      }
   }

   private static void stackGroup(List<MatLayout.Place> group, float x, float z, float width, float depth, boolean horizontal, boolean rods, boolean crossbow) {
      if (!group.isEmpty()) {
         float hw = 0.0F;
         float hd = 0.0F;

         for (MatLayout.Place p : group) {
            hw = Math.max(hw, p.halfWidth);
            hd = Math.max(hd, p.halfDepth);
         }

         float spread = Math.max(0.08F, Math.min(crossbow ? 0.68F : 0.5F, (width - hw * 2.0F - 0.14F) * 0.5F));
         float rear = -Math.min(0.3F, Math.max(0.08F, (depth - hd * 2.0F - 0.14F) * 0.3F));

         for (int i = 0; i < group.size(); i++) {
            MatLayout.Place p = group.get(i);
            boolean top = i == group.size() - 1;
            float dx = 0.0F;
            float dz = 0.0F;
            float angle = 0.0F;
            if (group.size() > 1) {
               if (rods) {
                  float pitch = Math.max(0.12F, Math.min(0.6F, (depth - hd * 2.0F - 0.14F) / Math.max(1, group.size() - 1)));
                  dz = (i - (group.size() - 1) * 0.5F) * pitch;
                  dx = i % 2 == 0 ? -0.04F : 0.04F;
                  angle = i == 0 ? -1.5F : (i == 1 ? 0.8F : 1.5F);
               } else {
                  dx = top ? 0.0F : (i % 2 == 0 ? -spread : spread);
                  dz = !top || !crossbow && group.size() <= 3 ? rear : 0.03F;
                  angle = top ? 0.0F : (i % 2 == 0 ? -1 : 1) * (crossbow ? 9 : 3);
               }
            }

            p.targetX = x + dx;
            p.targetZ = z + dz;
            p.targetYaw = (horizontal ? 90 : 0) + angle;
            p.pileLift = rods ? 0.0F : (top && group.size() > 1 ? 0.08F : 0.0F);
            p.stackShade = !rods && !top ? 0.9F : 1.0F;
         }
      }
   }

   private void packEquipment(AbstractContainerMenu menu, ItemFootprint[] footprints) {
      this.equipmentCells.clear();
      this.equipmentGroups.clear();
      ItemPresentationData.Compartment area = this.compartments()
         .stream()
         .filter(c -> c.category() == ItemCategoryClassifier.Category.EQUIPMENT)
         .findFirst()
         .orElseThrow();
      float gap = 0.16F;
      float width = (area.width() - 0.4F - gap * 4.0F) / 5.0F;

      for (int i = 0; i < 5; i++) {
         float x = area.x0() + 0.2F + i * (width + gap);
         this.equipmentCells.add(new MatLayout.EquipmentCell(i, this.equipmentOrder.key(i), x, area.z0() + 0.2F, x + width, area.z1() - 0.2F));
      }

      boolean storedShield = this.storagePlaces().stream().anyMatch(px -> !px.detachedPlacement && menu.getSlot(px.slot).getItem().is(Items.SHIELD));
      boolean storedElytra = this.places
         .stream()
         .anyMatch(
            px -> !px.detachedPlacement
               && (px.group == MatLayout.Group.SUPPLIES || px.group == MatLayout.Group.EQUIPMENT)
               && menu.getSlot(px.slot).getItem().is(Items.ELYTRA)
         );
      this.shieldPreview = !storedShield && menu.getCarried().is(Items.SHIELD);
      this.elytraPreview = !storedElytra && menu.getCarried().is(Items.ELYTRA);
      float top = area.z1();
      float bottom = top + 5.05F;
      float split = area.x0() + 6.2F;
      ArrayList<ItemPresentationData.Compartment> regions = new ArrayList<>(this.activeCompartments);
      if (storedShield || this.shieldPreview) {
         this.equipmentCells.add(new MatLayout.EquipmentCell(5, "shield", area.x0() + 0.2F, top + 0.2F, split - 0.2F, bottom - 0.2F));
         regions.add(new ItemPresentationData.Compartment("extra_shield", ItemCategoryClassifier.Category.EQUIPMENT, area.x0(), top, split, bottom));
      }

      if (storedElytra || this.elytraPreview) {
         this.equipmentCells.add(new MatLayout.EquipmentCell(6, "elytra", split + 0.2F, top + 0.2F, area.x1() - 0.2F, bottom - 0.2F));
         regions.add(new ItemPresentationData.Compartment("extra_elytra", ItemCategoryClassifier.Category.EQUIPMENT, split, top, area.x1(), bottom));
      }

      this.activeCompartments = List.copyOf(regions);

      for (MatLayout.Place p : this.places) {
         if (!p.detachedPlacement && (p.group == MatLayout.Group.SUPPLIES || p.group == MatLayout.Group.EQUIPMENT)) {
            ItemFootprint fp = footprints[p.slot];
            String kind = EquipmentKinds.kind(menu.getSlot(p.slot).getItem());
            if (kind == null && fp != null && p.category == ItemCategoryClassifier.Category.EQUIPMENT) {
               kind = "chestplate";
            }

            if (p.group == MatLayout.Group.EQUIPMENT && fp == null) {
               kind = EquipmentKinds.DEFAULT.get(39 - menu.getSlot(p.slot).getContainerSlot());
               p.visible = false;
            }

            if (kind != null) {
               p.equipmentCell = this.equipmentOrder.cell(kind);
               p.category = ItemCategoryClassifier.Category.EQUIPMENT;
               p.bin = ItemCategoryClassifier.Category.EQUIPMENT.name();
               MatLayout.EquipmentCell cell = this.equipmentCell(p.equipmentCell);
               if (cell == null) {
                  p.visible = false;
               } else {
                  p.targetX = (cell.x0() + cell.x1()) * 0.5F;
                  p.targetZ = (cell.z0() + cell.z1()) * 0.5F;
                  p.targetYaw = 0.0F;
                  if (fp != null) {
                     if (p.equipmentCell == 5) {
                        p.targetYaw = 90.0F;
                        p.sizeFactor = Math.min(
                           (cell.x1() - cell.x0() - 0.64F) / fp.depth(), Math.min((cell.z1() - cell.z0() - 0.55F) / fp.width(), 5.2F / fp.depth())
                        );
                     } else if (p.equipmentCell == 6) {
                        p.targetYaw = 90.0F;
                        p.sizeFactor = Math.min(
                           (cell.x1() - cell.x0() - 0.64F) / fp.depth(), Math.min((cell.z1() - cell.z0() - 0.55F) / fp.width(), 6.05F / fp.depth())
                        );
                     } else {
                        p.sizeFactor = Math.min((cell.x1() - cell.x0() - 0.42F) / fp.width(), (cell.z1() - cell.z0() - 0.54F) / fp.depth());
                     }

                     p.halfWidth = fp.width() * p.sizeFactor * 0.5F;
                     p.halfDepth = fp.depth() * p.sizeFactor * 0.5F;
                     p.radius = Math.max(p.halfWidth, p.halfDepth);
                     this.equipmentGroups.computeIfAbsent(p.equipmentCell, k -> new ArrayList<>()).add(p);
                  }
               }
            }
         }
      }

      for (Entry<Integer, List<MatLayout.Place>> entry : this.equipmentGroups.entrySet()) {
         List<MatLayout.Place> group = entry.getValue();
         MatLayout.EquipmentCell cell = this.equipmentCell(entry.getKey());
         List<String> saved = MatInventoryClient.config.equipmentPresentation.getOrDefault(cell.kind(), List.of());
         List<Integer> sequence = this.equipmentSequence
            .getOrDefault(
               cell.kind(),
               menu.slots
                  .stream()
                  .filter(s -> (s.container instanceof Inventory || s.container instanceof ExtraInventory) && saved.contains(storageKey(s)))
                  .sorted(Comparator.comparingInt(s -> saved.indexOf(storageKey(s))))
                  .map(s -> s.index)
                  .toList()
            );
         group.sort(Comparator.comparingInt(px -> sequence.contains(px.slot) ? sequence.indexOf(px.slot) : 1000 + px.slot));
         Integer front = this.equipmentFront.get(cell.kind());
         if (front != null) {
            for (int i = 0; i < group.size(); i++) {
               if (group.get(i).slot() == front) {
                  Collections.rotate(group, group.size() - 1 - i);
                  break;
               }
            }
         }

         this.equipmentSequence.put(cell.kind(), new ArrayList<>(group.stream().map(MatLayout.Place::slot).toList()));
         List<String> addresses = group.stream().map(px -> storageKey(menu.getSlot(px.slot))).toList();
         if (!addresses.equals(saved)) {
            MatInventoryClient.config.equipmentPresentation.put(cell.kind(), new ArrayList<>(addresses));
            MatInventoryClient.config.save();
         }

         int visible = Math.min(cell.index() >= 5 ? 1 : 3, group.size());
         int start = group.size() - visible;

         for (int i = 0; i < group.size(); i++) {
            MatLayout.Place p = group.get(i);
            p.visible = i >= start;
            p.equipmentTotal = group.size();
            p.equipmentOrdinal = i - start;
         }

         stackGroup(
            group.stream().filter(px -> px.visible).toList(),
            (cell.x0() + cell.x1()) * 0.5F,
            (cell.z0() + cell.z1()) * 0.5F,
            cell.x1() - cell.x0(),
            cell.z1() - cell.z0(),
            cell.index() >= 5,
            false,
            false
         );
      }

      int count = this.equipmentGroups.values().stream().mapToInt(List::size).sum();
      this.totals.put(ItemCategoryClassifier.Category.EQUIPMENT, count);
      this.shown.put(ItemCategoryClassifier.Category.EQUIPMENT, count);
      this.offsets.put(ItemCategoryClassifier.Category.EQUIPMENT, 0);
   }

   private boolean needsUpperPreview(ItemStack stack) {
      String kind = toolKind(stack);
      if (kind == null) {
         return false;
      }

      int defaultCell = ToolOrder.DEFAULT.indexOf(kind);
      return defaultCell >= 5 || this.toolOrder.cell(kind) >= 5;
   }

   public List<MatLayout.Rail> rails() {
      ArrayList<MatLayout.Rail> out = new ArrayList<>();
      MatTopology.Shape shape = MatTopology.build(this.fabricCompartments().stream().map(c -> new MatTopology.Rect(c.x0(), c.z0(), c.x1(), c.z1())).toList());

      for (MatTopology.Segment seam : shape.seams()) {
         MatTopology.Point a = seam.a();
         MatTopology.Point b = seam.b();
         if (a.x() == b.x()) {
            out.add(new MatLayout.Rail(a.x() - 0.14F, a.z(), b.x() + 0.14F, b.z(), false));
         } else {
            out.add(new MatLayout.Rail(a.x(), a.z() - 0.14F, b.x(), b.z() + 0.14F, false));
         }
      }

      for (int i = 0; i < shape.perimeter().size(); i++) {
         MatTopology.Point a = shape.perimeter().get(i);
         MatTopology.Point b = shape.perimeter().get((i + 1) % shape.perimeter().size());
         if (a.x() == b.x()) {
            out.add(new MatLayout.Rail(a.x() - 0.22F, Math.min(a.z(), b.z()), a.x() + 0.22F, Math.max(a.z(), b.z()), false));
         } else {
            out.add(new MatLayout.Rail(Math.min(a.x(), b.x()), a.z() - 0.22F, Math.max(a.x(), b.x()), a.z() + 0.22F, false));
         }
      }

      return out;
   }

   private float axisWidth(MatLayout.Place p) {
      double a = Math.toRadians(p.targetYaw);
      return p.halfWidth * (float)Math.abs(Math.cos(a)) + p.halfDepth * (float)Math.abs(Math.sin(a));
   }

   private static float signed(int seed) {
      seed ^= seed >>> 16;
      seed *= 73244475;
      seed ^= seed >>> 16;
      return Math.floorMod(seed, 2001) / 1000.0F - 1.0F;
   }

   private void footprint(MatLayout.Place p, ItemFootprint f) {
      p.halfWidth = f == null ? 0.48F : f.width() * 0.5F;
      p.halfDepth = f == null ? 0.48F : f.depth() * 0.5F;
      p.preferredYaw = f == null ? 0.0F : f.preferredYaw();
      p.seed = f == null ? 0 : f.id().hashCode();
      p.radius = Math.max(p.halfWidth, p.halfDepth);
   }

   private void fixed(MatLayout.Place p, ItemFootprint f) {
      if (p.group == MatLayout.Group.HOTBAR) {
         int i = p.slot - this.base - 27;
         p.targetX = (this.beltEdges[i] + this.beltEdges[i + 1]) / 2.0F;
         p.targetZ = (this.hotbarTop + this.hotbarBottom) / 2.0F;
      } else if (p.group == MatLayout.Group.OFFHAND) {
         p.targetX = this.beltEdges[0] - this.offhandWidth() * 0.5F;
         p.targetZ = (this.hotbarTop + this.hotbarBottom) / 2.0F;
      } else if (p.group == MatLayout.Group.EQUIPMENT) {
         ItemPresentationData.Compartment c = ItemPresentationData.compartment(ItemCategoryClassifier.Category.EQUIPMENT);
         int i = p.slot == 45 ? 4 : p.slot - 5;
         p.targetX = c.x0() + 0.28F + (i + 0.5F) * 5.84F / 5.0F;
         p.targetZ = (c.z0() + c.z1()) / 2.0F;
      } else if (p.group == MatLayout.Group.CRAFT || p.group == MatLayout.Group.RESULT) {
         p.targetYaw = 0.0F;
         ItemPresentationData.Compartment c = ItemPresentationData.compartment(ItemCategoryClassifier.Category.CRAFT);
         float cx = (c.x0() + c.x1()) / 2.0F;
         float cz = (c.z0() + c.z1()) / 2.0F;
         if (p.group == MatLayout.Group.RESULT) {
            p.targetX = c.x1() - 1.85F;
            p.targetZ = cz;
         } else {
            int i = p.slot - 1;
            float step = this.gridSize == 2 ? 2.0F : 1.55F;
            p.targetX = cx - 0.95F + (i % this.gridSize - (this.gridSize - 1) / 2.0F) * step;
            p.targetZ = cz + (i / this.gridSize - (this.gridSize - 1) / 2.0F) * step;
         }
      }
   }

   public int pageCount(ItemCategoryClassifier.Category c) {
      return 1;
   }

   public int page(ItemCategoryClassifier.Category c) {
      return 0;
   }

   public int pageCount() {
      return 1;
   }

   public int page() {
      return 0;
   }

   public int scrollOffset(ItemCategoryClassifier.Category c) {
      return this.offsets.getOrDefault(c, 0);
   }

   public int scrollTotal(ItemCategoryClassifier.Category c) {
      return this.totals.getOrDefault(c, 0);
   }

   public int scrollShown(ItemCategoryClassifier.Category c) {
      return this.shown.getOrDefault(c, 0);
   }

   public boolean scroll(float x, float z, int direction) {
      MatLayout.ToolCell cell = this.toolCellAt(x, z);
      if (cell != null && this.toolsInCell(cell.index()).size() > toolVisibleLimit(cell.kind())) {
         List<MatLayout.Place> group = this.toolGroups.get(cell.index());
         int current = this.toolOffsets.getOrDefault(cell.kind(), 0);
         int next = Math.clamp(current + direction, 0, group.size() - toolVisibleLimit(cell.kind()));
         if (current != next) {
            this.toolOffsets.put(cell.kind(), next);
            this.signature = Long.MIN_VALUE;

            for (MatLayout.Place p : group) {
               p.initialized = false;
            }
         }

         return true;
      } else {
         ItemPresentationData.Compartment c = this.compartmentAt(x, z);
         if (c == null) {
            return false;
         }

         if (c.category() == ItemCategoryClassifier.Category.LONG_TOOLS && this.toolFamilyTotal > 5) {
            this.toolFamilyOffset = Math.clamp(this.toolFamilyOffset + direction * 5, 0, this.toolFamilyTotal - 5);
            this.signature = Long.MIN_VALUE;

            for (MatLayout.Place p : this.places) {
               if (p.toolCell >= 0) {
                  p.initialized = false;
                  p.pickBoxCount = 0;
               }
            }

            return true;
         } else {
            ItemCategoryClassifier.Category cat = materialCategory(c.category()) ? ItemCategoryClassifier.Category.RESOURCES : c.category();
            int current = this.scrollOffset(cat);
            int total = this.scrollTotal(cat);
            if (total <= this.scrollShown(cat) && current == 0) {
               return false;
            }

            if (direction > 0 && current + this.scrollShown(cat) >= total) {
               return true;
            }

            int next = Math.clamp(current + direction, 0, Math.max(0, total - 1));
            if (next != current) {
               this.offsets.put(cat, next);
               this.signature = Long.MIN_VALUE;

               for (MatLayout.Place p : this.places) {
                  if (p.group == MatLayout.Group.SUPPLIES) {
                     p.initialized = false;
                  }
               }
            }

            return true;
         }
      }
   }

   public MatLayout.Place pick(float x, float z, MatCamera camera) {
      MatLayout.Place best = null;
      float score = Float.POSITIVE_INFINITY;
      float screenX = 0.0F;
      float screenY = 0.0F;
      if (camera != null) {
         Vector4f q = camera.project(x, 0.0F, z);
         screenX = q.x;
         screenY = q.y;
         camera.screenRay(screenX, screenY, this.pickOrigin, this.pickDirection);
      }

      for (MatLayout.Place p : this.places) {
         if (p.visible && (p.group != MatLayout.Group.SUPPLIES || p.occupied || !this.arranged)) {
            if (camera != null && p.occupied && p.solidPick && p.pickBoxCount > 0) {
               for (int copy = 0; copy < p.pickBoxCount; copy++) {
                  float distance = p.pickBoxes[copy].intersect(this.pickOrigin, this.pickDirection);
                  if (distance < score) {
                     best = p;
                     score = distance;
                  }
               }
            } else {
               float hitX = x;
               float hitZ = z;
               float distance = 0.0F;
               float pad = 0.0F;
               if (camera != null) {
                  if (!camera.raycastAt(screenX, screenY, p.occupied ? p.surfaceHeight : 0.0F)) {
                     continue;
                  }

                  hitX = camera.hit.x;
                  hitZ = camera.hit.z;
                  distance = this.pickOrigin.distance(camera.hit);
                  Vector4f center = camera.project(p.x, p.surfaceHeight, p.z);
                  float sx = center.x;
                  Vector4f edge = camera.project(p.x + 1.0F, p.surfaceHeight, p.z);
                  pad = 12.0F / Math.max(1, Minecraft.getInstance().getWindow().getGuiScale()) / Math.max(1.0F, Math.abs(edge.x - sx));
               }

               double a = Math.toRadians(p.yaw);
               float dx = hitX - p.x;
               float dz = hitZ - p.z;
               float ox = (float)(dx * Math.cos(a) - dz * Math.sin(a));
               float oz = (float)(dx * Math.sin(a) + dz * Math.cos(a));
               if (!(Math.abs(ox) > Math.max(p.halfWidth, pad) + 0.035F) && !(Math.abs(oz) > Math.max(p.halfDepth, pad) + 0.035F)) {
                  if (camera == null) {
                     distance = dx * dx + dz * dz;
                  }

                  if (distance < score) {
                     best = p;
                     score = distance;
                  }
               }
            }
         }
      }

      if (camera != null) {
         camera.hit.set(x, 0.0F, z);
      }

      return best;
   }

   public MatLayout.Place pick(float x, float z) {
      return this.pick(x, z, null);
   }

   public Slot dropTarget(AbstractContainerMenu menu, float x, float z) {
      return this.dropTarget(menu, x, z, Set.of());
   }

   public Slot dropTarget(AbstractContainerMenu menu, float x, float z, Set<Slot> excluded) {
      ItemStack carried = menu.getCarried();
      if (carried.isEmpty()) {
         return null;
      }

      ItemPresentationData.Compartment c = this.compartmentAt(x, z);
      if (c != null && ItemCategoryClassifier.acceptsSector(carried, c.category())) {
         if (c.category() == ItemCategoryClassifier.Category.HOTBAR) {
            if (x < this.beltEdges[0] && this.offhandSlot >= 0) {
               float center = this.beltEdges[0] - this.offhandWidth() * 0.5F;
               return Math.abs(x - center) < this.offhandWidth() * 0.44F
                     && Math.abs(z - (this.hotbarTop + this.hotbarBottom) * 0.5F) < (this.hotbarBottom - this.hotbarTop) * 0.39F
                  ? this.valid(menu.getSlot(this.offhandSlot), carried)
                  : null;
            }

            int i = 0;

            while (i < 8 && x > this.beltEdges[i + 1]) {
               i++;
            }

            return !(x < this.beltEdges[i] + 0.04F)
                  && !(x > this.beltEdges[i + 1] - 0.04F)
                  && !(Math.abs(z - (this.hotbarTop + this.hotbarBottom) * 0.5F) > (this.hotbarBottom - this.hotbarTop) * 0.39F)
               ? this.valid(menu.getSlot(this.base + 27 + i), carried)
               : null;
         } else {
            if (ItemCategoryClassifier.flexible(carried) && this.storageArea(c)) {
               return this.flexibleTarget(menu, x, z, excluded);
            }

            if (c.category() == ItemCategoryClassifier.Category.EQUIPMENT) {
               MatLayout.EquipmentCell cell = this.equipmentCellAt(x, z);
               if (cell == null || this.equipmentCellFor(carried) < 0) {
                  return null;
               }

               if (!equipmentDestination(this.equipmentCellFor(carried), cell.index())) {
                  return null;
               }
            }

            if (c.category() == ItemCategoryClassifier.Category.CRAFT) {
               MatLayout.Place nearest = null;
               float best = Float.POSITIVE_INFINITY;

               for (MatLayout.Place p : this.places) {
                  if (p.group == MatLayout.Group.CRAFT) {
                     float d = (x - p.x) * (x - p.x) + (z - p.z) * (z - p.z);
                     if (d < best) {
                        best = d;
                        nearest = p;
                     }
                  }
               }

               float half = this.gridSize == 2 ? 0.82F : 0.64F;
               return nearest != null && !(Math.abs(x - nearest.x) > half) && !(Math.abs(z - nearest.z) > half)
                  ? this.valid(menu.getSlot(nearest.slot), carried)
                  : null;
            } else {
               if (ItemCategoryClassifier.flexible(carried)) {
                  return this.flexibleTarget(menu, x, z, excluded);
               }

               if (c.category() == ItemCategoryClassifier.Category.EQUIPMENT) {
                  if (this.equipmentCellAt(x, z) == null || this.equipmentCellFor(carried) < 0) {
                     return null;
                  }
               } else if (c.category() == ItemCategoryClassifier.Category.VEHICLES) {
                  if (this.vehicleCellAt(x, z) == null || !VehicleKinds.isVehicle(carried)) {
                     return null;
                  }
               } else if (c.category() == ItemCategoryClassifier.Category.BLOCKS) {
                  if (this.blockCellAt(x, z) == null || ItemCategoryClassifier.classify(carried).category() != ItemCategoryClassifier.Category.BLOCKS) {
                     return null;
                  }
               } else if (c.category() == ItemCategoryClassifier.Category.LONG_TOOLS) {
                  if (this.toolDropCellAt(x, z) == null || toolKind(carried) == null) {
                     return null;
                  }
               } else if (c.category() == ItemCategoryClassifier.Category.FOOD) {
                  if (this.foodCellAt(x, z) == null || ItemCategoryClassifier.classify(carried).category() != ItemCategoryClassifier.Category.FOOD) {
                     return null;
                  }
               } else if (materialCategory(c.category())) {
                  if (this.materialCellAt(x, z) == null || !MaterialKinds.isMaterial(carried)) {
                     return null;
                  }
               } else if (c.category() != ItemCategoryClassifier.classify(carried).category()) {
                  return null;
               }

               for (MatLayout.Place p : this.storagePlaces()) {
                  Slot s = menu.getSlot(p.slot());
                  ItemStack t = s.getItem();
                  if (!excluded.contains(s)
                     && !t.isEmpty()
                     && ItemStack.isSameItemSameComponents(t, carried)
                     && t.getCount() < s.getMaxStackSize(carried)
                     && s.mayPlace(carried)) {
                     return s;
                  }
               }

               for (MatLayout.Place p : this.storagePlaces()) {
                  Slot slot = menu.getSlot(p.slot());
                  if (!excluded.contains(slot) && !slot.hasItem() && slot.mayPlace(carried)) {
                     return slot;
                  }
               }

               return null;
            }
         }
      } else {
         return null;
      }
   }

   public MatLayout.DropZone capacityZone(String sector) {
      List<ItemPresentationData.Compartment> areas = this.compartments()
         .stream()
         .filter(
            cx -> "blocks".equals(sector)
               ? cx.category() == ItemCategoryClassifier.Category.BLOCKS
               : (
                  "vehicles".equals(sector)
                     ? cx.category() == ItemCategoryClassifier.Category.VEHICLES
                     : ("extra_tools".equals(sector) ? cx.id().startsWith("extra_tools") : cx.id().equals(sector))
               )
         )
         .toList();
      if (areas.isEmpty()) {
         return null;
      }

      float x0 = Float.POSITIVE_INFINITY;
      float z0 = x0;
      float x1 = -x0;
      float z1 = -x0;

      for (ItemPresentationData.Compartment c : areas) {
         x0 = Math.min(x0, c.x0());
         z0 = Math.min(z0, c.z0());
         x1 = Math.max(x1, c.x1());
         z1 = Math.max(z1, c.z1());
      }

      return new MatLayout.DropZone(x0 + 0.18F, z0 + 0.18F, x1 - 0.18F, z1 - 0.18F, true);
   }

   public List<MatLayout.DropZone> dropZones(AbstractContainerMenu menu, float x, float z) {
      return this.dropZones(menu, x, z, -1);
   }

   public List<MatLayout.DropZone> dropZones(AbstractContainerMenu menu, float x, float z, int hoveredToolCell) {
      if (menu.getCarried().isEmpty()) {
         return List.of();
      }

      ArrayList<MatLayout.DropZone> zones = new ArrayList<>();

      for (ItemPresentationData.Compartment c : this.compartments()) {
         if (ItemCategoryClassifier.acceptsSector(menu.getCarried(), c.category())
            && (MatInventoryClient.config.gridEnabled || c.category() == ItemCategoryClassifier.Category.CRAFT)) {
            boolean independent = this.separatePreview && this.separateArea(c);
            if (ItemCategoryClassifier.flexible(menu.getCarried()) && this.storageArea(c)) {
               zones.add(new MatLayout.DropZone(c.x0() + 0.2F, c.z0() + 0.2F, c.x1() - 0.2F, c.z1() - 0.2F, c.contains(x, z)));
            } else if (c.category() == ItemCategoryClassifier.Category.BLOCKS) {
               if (independent || ItemCategoryClassifier.classify(menu.getCarried()).category() == ItemCategoryClassifier.Category.BLOCKS) {
                  for (MatLayout.BlockCell cell : this.blockCells) {
                     if (c.contains((cell.x0() + cell.x1()) * 0.5F, (cell.z0() + cell.z1()) * 0.5F)) {
                        zones.add(new MatLayout.DropZone(cell.x0(), cell.z0(), cell.x1(), cell.z1(), cell.contains(x, z)));
                     }
                  }
               }
            } else if (c.category() == ItemCategoryClassifier.Category.VEHICLES) {
               if (independent || VehicleKinds.isVehicle(menu.getCarried())) {
                  for (MatLayout.VehicleCell cell : this.vehicleCells) {
                     if (c.contains((cell.x0() + cell.x1()) * 0.5F, (cell.z0() + cell.z1()) * 0.5F)) {
                        zones.add(new MatLayout.DropZone(cell.x0(), cell.z0(), cell.x1(), cell.z1(), cell.contains(x, z)));
                     }
                  }
               }
            } else if (c.category() == ItemCategoryClassifier.Category.LONG_TOOLS) {
               if (independent || toolKind(menu.getCarried()) != null) {
                  for (MatLayout.ToolCell cell : this.toolCells) {
                     if (cell.x0() >= c.x0()
                        && cell.x1() <= c.x1()
                        && cell.z0() >= c.z0()
                        && cell.z1() <= c.z1()
                        && (!this.toolsInCell(cell.index()).isEmpty() || this.dropTarget(menu, cell.dropCenterX(), (cell.z0() + cell.z1()) * 0.5F) != null)) {
                        zones.add(
                           new MatLayout.DropZone(
                              cell.dropX0(),
                              cell.z0() + 0.13F,
                              cell.dropX1(),
                              cell.z1() - 0.13F,
                              hoveredToolCell >= 0 ? hoveredToolCell == cell.index() : cell.dropContains(x, z)
                           )
                        );
                     }
                  }
               }
            } else if (c.category() == ItemCategoryClassifier.Category.FOOD) {
               if (independent || ItemCategoryClassifier.classify(menu.getCarried()).category() == ItemCategoryClassifier.Category.FOOD) {
                  for (MatLayout.FoodCell cell : this.foodCells) {
                     if (independent
                        || this.places.stream().anyMatch(px -> px.visible && px.foodCell == cell.index())
                        || this.dropTarget(menu, (cell.x0() + cell.x1()) * 0.5F, (cell.z0() + cell.z1()) * 0.5F) != null) {
                        zones.add(new MatLayout.DropZone(cell.x0(), cell.z0(), cell.x1(), cell.z1(), cell.contains(x, z)));
                     }
                  }
               }
            } else if (materialCategory(c.category())) {
               if (independent || MaterialKinds.isMaterial(menu.getCarried())) {
                  for (MatLayout.MaterialCell cell : this.materialCells) {
                     if (cell.category() == c.category()
                        && (
                           independent
                              || this.places.stream().anyMatch(px -> px.visible && px.materialCell == cell.index())
                              || this.dropTarget(menu, (cell.x0() + cell.x1()) * 0.5F, (cell.z0() + cell.z1()) * 0.5F) != null
                        )) {
                        zones.add(new MatLayout.DropZone(cell.x0(), cell.z0(), cell.x1(), cell.z1(), cell.contains(x, z)));
                     }
                  }
               }
            } else if (c.category() == ItemCategoryClassifier.Category.EQUIPMENT) {
               if (independent || this.equipmentCellFor(menu.getCarried()) >= 0) {
                  for (MatLayout.EquipmentCell cell : this.equipmentCells) {
                     if ((independent || equipmentDestination(this.equipmentCellFor(menu.getCarried()), cell.index()))
                        && c.contains((cell.x0() + cell.x1()) * 0.5F, (cell.z0() + cell.z1()) * 0.5F)) {
                        zones.add(new MatLayout.DropZone(cell.x0(), cell.z0(), cell.x1(), cell.z1(), cell.contains(x, z)));
                     }
                  }
               }
            } else if (c.category() != ItemCategoryClassifier.Category.CRAFT && c.category() != ItemCategoryClassifier.Category.HOTBAR) {
               if (c.category() == ItemCategoryClassifier.classify(menu.getCarried()).category()
                  && (
                     this.dropTarget(menu, (c.x0() + c.x1()) * 0.5F, (c.z0() + c.z1()) * 0.5F) != null
                        || this.places.stream().anyMatch(px -> px.visible && px.occupied && c.contains(px.x, px.z))
                  )) {
                  zones.add(new MatLayout.DropZone(c.x0() + 0.18F, c.z0() + 0.18F, c.x1() - 0.18F, c.z1() - 0.18F, c.contains(x, z)));
               }
            } else {
               for (MatLayout.Place p : this.places) {
                  if ((
                        c.category() == ItemCategoryClassifier.Category.CRAFT && p.group == MatLayout.Group.CRAFT
                           || c.category() == ItemCategoryClassifier.Category.HOTBAR
                              && (p.group == MatLayout.Group.HOTBAR || p.group == MatLayout.Group.OFFHAND)
                     )
                     && menu.getSlot(p.slot).mayPlace(menu.getCarried())) {
                     float rx = p.group == MatLayout.Group.OFFHAND
                        ? this.offhandWidth() * 0.44F
                        : (p.group == MatLayout.Group.HOTBAR ? (this.beltEdges[1] - this.beltEdges[0]) * 0.44F : (this.gridSize == 2 ? 0.82F : 0.64F));
                     float rz = p.group != MatLayout.Group.HOTBAR && p.group != MatLayout.Group.OFFHAND ? rx : c.depth() * 0.39F;
                     zones.add(new MatLayout.DropZone(p.x - rx, p.z - rz, p.x + rx, p.z + rz, Math.abs(x - p.x) <= rx && Math.abs(z - p.z) <= rz));
                  }
               }
            }
         }
      }

      return zones;
   }

   private Slot valid(Slot slot, ItemStack carried) {
      return slot.mayPlace(carried) ? slot : null;
   }

   @Environment(EnvType.CLIENT)
   public record Anchor(String id, float x, float z) {
   }

   @Environment(EnvType.CLIENT)
   public record BlockCell(int index, float x0, float z0, float x1, float z1) {
      public boolean contains(float x, float z) {
         return x >= this.x0 && x <= this.x1 && z >= this.z0 && z <= this.z1;
      }
   }

   @Environment(EnvType.CLIENT)
   public record DropZone(float x0, float z0, float x1, float z1, boolean hovered) {
   }

   @Environment(EnvType.CLIENT)
   public record EquipmentCell(int index, String kind, float x0, float z0, float x1, float z1) {
      public boolean contains(float x, float z) {
         return x >= this.x0 && x <= this.x1 && z >= this.z0 && z <= this.z1;
      }
   }

   @Environment(EnvType.CLIENT)
   private record FlexiblePosition(String item, float x, float z, boolean grid) {
   }

   @Environment(EnvType.CLIENT)
   private record FlexibleRequest(
      MatLayout.Place place,
      ItemFootprint footprint,
      float scale,
      MatConfig.DetachedItem saved,
      float[] requested,
      ItemPresentationData.Compartment preferred,
      boolean stable,
      boolean savedFree
   ) {
   }

   @Environment(EnvType.CLIENT)
   public record FoodCell(int index, String key, float x0, float z0, float x1, float z1) {
      public boolean contains(float x, float z) {
         return x >= this.x0 && x <= this.x1 && z >= this.z0 && z <= this.z1;
      }
   }

   @Environment(EnvType.CLIENT)
   public enum Group {
      SUPPLIES,
      HOTBAR,
      OFFHAND,
      EQUIPMENT,
      CRAFT,
      RESULT;
   }

   @Environment(EnvType.CLIENT)
   public record MaterialCell(int index, String key, ItemCategoryClassifier.Category category, float x0, float z0, float x1, float z1) {
      public boolean contains(float x, float z) {
         return x >= this.x0 && x <= this.x1 && z >= this.z0 && z <= this.z1;
      }
   }

   @Environment(EnvType.CLIENT)
   public static final class Place {
      private final int slot;
      private final MatLayout.Group group;
      public float x;
      public float z;
      public float radius;
      public float halfWidth = 0.5F;
      public float halfDepth = 0.5F;
      public float yaw;
      public float targetX;
      public float targetZ;
      public float targetYaw;
      public float preferredYaw;
      public float sizeFactor = 1.0F;
      public boolean freePlacement;
      public boolean detachedPlacement;
      public float screenLeft;
      public float screenTop;
      public float screenRight;
      public float screenBottom;
      public float countX = -1.0F;
      public float countY = -1.0F;
      public float pickHeight;
      public float pickX;
      public float pickY;
      public float pickZ;
      public float surfaceHeight;
      public float labelLeft;
      public float labelTop;
      public float labelRight;
      public float labelBottom;
      public float stackShade = 1.0F;
      public boolean solidPick;
      public int pickBoxCount;
      public int blockTotal;
      public int blockStacks;
      public final RenderedItemBounds[] pickBoxes = new RenderedItemBounds[]{
         new RenderedItemBounds(),
         new RenderedItemBounds(),
         new RenderedItemBounds(),
         new RenderedItemBounds(),
         new RenderedItemBounds(),
         new RenderedItemBounds()
      };
      public String bin = "";
      public int seed;
      public int toolCell = -1;
      public int toolTotal;
      public int toolOrdinal;
      public int foodCell = -1;
      public int foodStacks;
      public int materialCell = -1;
      public int materialStacks;
      public int materialStyle;
      public int equipmentCell = -1;
      public int equipmentTotal;
      public int equipmentOrdinal;
      public int vehicleCell = -1;
      public int blockCell = -1;
      public float pileLift;
      public boolean visible = true;
      public boolean occupied;
      public boolean initialized;
      public ItemCategoryClassifier.Category category = ItemCategoryClassifier.Category.OTHER;

      private Place(int slot, float x, float z, float radius, MatLayout.Group group) {
         this.slot = slot;
         this.x = this.targetX = x;
         this.z = this.targetZ = z;
         this.radius = radius;
         this.group = group;
      }

      public int slot() {
         return this.slot;
      }

      public float x() {
         return this.x;
      }

      public float z() {
         return this.z;
      }

      public float radius() {
         return this.radius;
      }

      public MatLayout.Group group() {
         return this.group;
      }
   }

   @Environment(EnvType.CLIENT)
   public record Rail(float x0, float z0, float x1, float z1, boolean lane) {
   }

   @Environment(EnvType.CLIENT)
   public record SeparateDestination(Slot slot, float x, float z) {
   }

   @Environment(EnvType.CLIENT)
   public record ToolCell(int index, String kind, float x0, float z0, float x1, float z1, float dropCenterX, float dropWidth) {
      public boolean contains(float x, float z) {
         return x >= this.x0 && x <= this.x1 && z >= this.z0 && z <= this.z1;
      }

      public float dropX0() {
         return this.dropCenterX - this.dropWidth * 0.5F;
      }

      public float dropX1() {
         return this.dropCenterX + this.dropWidth * 0.5F;
      }

      public boolean dropContains(float x, float z) {
         return x >= this.dropX0() && x <= this.dropX1() && z >= this.z0 + 0.13F && z <= this.z1 - 0.13F;
      }
   }

   @Environment(EnvType.CLIENT)
   public record VehicleCell(int index, float x0, float z0, float x1, float z1) {
      public boolean contains(float x, float z) {
         return x >= this.x0 && x <= this.x1 && z >= this.z0 && z <= this.z1;
      }
   }
}
