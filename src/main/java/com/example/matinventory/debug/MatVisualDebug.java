package com.example.matinventory.debug;

import com.example.matinventory.MatInventoryClient;
import com.example.matinventory.animation.MatAnimation;
import com.example.matinventory.arrangement.ItemPresentationData;
import com.example.matinventory.config.MatConfig;
import com.example.matinventory.layout.MatLayout;
import com.example.matinventory.render.MatItems;
import com.example.matinventory.render.MatOverlay;
import com.example.matinventory.render.MatSceneRenderer;
import com.example.matinventory.scene.MatCamera;
import com.example.matinventory.scene.MatWorldContext;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.InactivityFpsLimit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Vector4f;
import org.lwjgl.sdl.SDLVideo;

@Environment(EnvType.CLIENT)
public final class MatVisualDebug {
   private static int ticks;
   private static int shotIndex;
   private static MatVisualDebug.Preview preview;
   private static boolean automated;
   private static int[][] views = new int[][]{{1280, 720, 2}, {1920, 1080, 3}};
   private static final StringBuilder report = new StringBuilder();
   private static String pendingCapture;

   public static void register() {
      ClientCommandRegistrationCallback.EVENT
         .register(
            (ClientCommandRegistrationCallback)(dispatcher, access) -> dispatcher.register(
               (LiteralArgumentBuilder)((LiteralArgumentBuilder)ClientCommands.literal("matinv")
                     .then(ClientCommands.literal("debug").then(ClientCommands.literal("screenshot").executes(context -> {
                        open();
                        return 1;
                     }))))
                  .then(ClientCommands.literal("reload").executes(context -> {
                     ItemPresentationData.reload();
                     return 1;
                  }))
            )
         );
      automated = Boolean.getBoolean("matinventory.visual");
      if (automated && System.getProperty("matinventory.visual.stage", "").equals("Arrange")
         || automated && System.getProperty("matinventory.visual.stage", "").equals("Tarp")) {
         views = new int[][]{{1920, 1080, 2}};
      }

      if (automated) {
         try {
            Path path = Minecraft.getInstance().gameDirectory.toPath().resolve("matinv-debug");
            Files.createDirectories(path);
            Files.writeString(path.resolve(System.getProperty("matinventory.visual.stage", "preview") + "-metrics.txt"), "RUNNING\n");
         } catch (Exception ex) {
            throw new IllegalStateException(ex);
         }
      }

      if (automated && System.getProperty("matinventory.visual.stage", "").startsWith("M7")) {
         views = new int[][]{
            {1280, 720, 2},
            {1280, 720, 3},
            {1280, 720, 4},
            {1280, 720, 0},
            {1920, 1080, 2},
            {1920, 1080, 3},
            {1920, 1080, 4},
            {1920, 1080, 0},
            {2560, 1440, 2},
            {2560, 1440, 3},
            {2560, 1440, 4},
            {2560, 1440, 0},
            {1920, 1200, 3},
            {1280, 960, 2},
            {2560, 1080, 3}
         };
      }

      ClientTickEvents.END_CLIENT_TICK
         .register(
            (EndTick)client -> {
               if (automated) {
                  if (client.gui.overlay() == null && client.gui.screen() != null) {
                     ticks++;
                     if (preview == null && ticks > 30) {
                        open();
                        client.options.inactivityFpsLimit().set(InactivityFpsLimit.MINIMIZED);
                        if (System.getProperty("matinventory.visual.stage", "").startsWith("M7steady")) {
                           client.options.enableVsync().set(false);
                           client.options.framerateLimit().set(240);
                        }

                        applyView(client, views[0]);
                        ticks = 0;
                     } else if (preview != null && ticks == 35) {
                        MatSceneRenderer.cpuMillis = 0.0;
                        MatSceneRenderer.renderedFrames = 0L;
                     } else if (preview != null && ticks == (System.getProperty("matinventory.visual.stage", "").startsWith("M7steady") ? 90 : 30)) {
                        String name = System.getProperty("matinventory.visual.stage", "preview")
                           + "-"
                           + views[shotIndex][0]
                           + "x"
                           + views[shotIndex][1]
                           + "-gui"
                           + views[shotIndex][2];
                        preview.capture(name);
                        double cpu = MatSceneRenderer.cpuMillis / Math.max(1L, MatSceneRenderer.renderedFrames);
                        String line = name
                           + " FPS="
                           + client.getFps()
                           + " PIP_CPU_ms="
                           + String.format(Locale.ROOT, "%.2f", cpu)
                           + " fallback="
                           + MatInventoryClient.renderFailed;
                        MatInventoryClient.LOG.info("MAT_VISUAL {}", line);
                        report.append(line).append('\n');
                     } else if (preview != null && ticks > (System.getProperty("matinventory.visual.stage", "").startsWith("M7steady") ? 102 : 42)) {
                        if (++shotIndex < views.length) {
                           applyView(client, views[shotIndex]);
                           ticks = 0;
                        } else {
                           try {
                              Files.writeString(
                                 client.gameDirectory
                                    .toPath()
                                    .resolve("matinv-debug/" + System.getProperty("matinventory.visual.stage", "preview") + "-metrics.txt"),
                                 report
                              );
                           } catch (Exception ex) {
                              MatInventoryClient.LOG.error("Cannot write visual report", ex);
                           }

                           automated = false;
                           client.stop();
                        }
                     }
                  }
               }
            }
         );
   }

   private static void applyView(Minecraft client, int[] view) {
      client.options.guiScale().set(view[2]);
      SDLVideo.SDL_SetWindowSize(client.getWindow().handle(), view[0], view[1]);
      client.resizeGui();
      MatSceneRenderer.cpuMillis = 0.0;
      MatSceneRenderer.renderedFrames = 0L;
   }

   public static void open() {
      Minecraft client = Minecraft.getInstance();
      preview = new MatVisualDebug.Preview(client.gui.screen());
      MatInventoryClient.switchScreen(client, preview);
      if (!automated && !Boolean.getBoolean("matinventory.scenery")) {
         preview.captureAfter = 12;
      }
   }

   public static MatVisualDebug.Preview currentPreview() {
      return preview;
   }

   public static void flushCapture() {
      if (pendingCapture != null) {
         Minecraft client = Minecraft.getInstance();
         String name = pendingCapture;
         pendingCapture = null;
         Path directory = client.gameDirectory.toPath().resolve("matinv-debug");
         Screenshot.takeScreenshot(client.gameRenderer.mainRenderTarget(), image -> CompletableFuture.runAsync(() -> {
            try {
               Files.createDirectories(directory);
               image.writeToFile(directory.resolve(name + ".png"));
               MatInventoryClient.LOG.info("MAT_VISUAL screenshot {}", name);
            } catch (Exception ex) {
               MatInventoryClient.LOG.error("Cannot save visual fixture", ex);
            } finally {
               image.close();
            }
         }));
      }
   }

   @Environment(EnvType.CLIENT)
   public static final class Fixture extends AbstractContainerMenu {
      private final SimpleContainer contents = new SimpleContainer(46);
      int preset;

      Fixture() {
         super(null, -1);

         for (int i = 0; i < 46; i++) {
            this.addSlot(new Slot(this.contents, i, 0, 0));
         }

         this.populate(0);
      }

      public void populate(int preset) {
         this.preset = preset;
         this.contents.clearContent();

         Item[] varied = switch (preset) {
            case 1 -> new Item[]{
               Items.WOODEN_SWORD,
               Items.STONE_SWORD,
               Items.COPPER_SWORD,
               Items.IRON_SWORD,
               Items.GOLDEN_SWORD,
               Items.DIAMOND_SWORD,
               Items.NETHERITE_SWORD,
               Items.WOODEN_PICKAXE,
               Items.STONE_PICKAXE,
               Items.IRON_PICKAXE,
               Items.DIAMOND_PICKAXE,
               Items.IRON_AXE,
               Items.DIAMOND_AXE,
               Items.IRON_SHOVEL,
               Items.DIAMOND_SHOVEL,
               Items.IRON_HOE,
               Items.BOW,
               Items.CROSSBOW,
               Items.TRIDENT,
               Items.FISHING_ROD,
               Items.APPLE,
               Items.IRON_INGOT,
               Items.GOLD_NUGGET,
               Items.DIAMOND,
               Items.LEATHER,
               Items.STICK,
               Items.ARROW
            };
            case 2 -> new Item[]{
               Items.APPLE,
               Items.GOLDEN_APPLE,
               Items.COOKED_BEEF,
               Items.BEEF,
               Items.COOKED_PORKCHOP,
               Items.BREAD,
               Items.CARROT,
               Items.POTATO,
               Items.POTION,
               Items.SPLASH_POTION,
               Items.LINGERING_POTION,
               Items.STONE,
               Items.DIRT,
               Items.OAK_PLANKS,
               Items.GLASS,
               Items.IRON_INGOT,
               Items.GOLD_INGOT,
               Items.DIAMOND,
               Items.COAL,
               Items.REDSTONE,
               Items.LEATHER,
               Items.STICK,
               Items.ARROW,
               Items.PAPER,
               Items.AMETHYST_SHARD,
               Items.IRON_NUGGET,
               Items.WHEAT_SEEDS
            };
            case 3 -> new Item[]{Items.BOW, Items.IRON_PICKAXE, Items.DIAMOND_SWORD};
            case 4 -> new Item[]{Items.STONE, Items.DIRT, Items.GLASS, Items.OAK_PLANKS};
            case 5 -> new Item[]{Items.GOLD_NUGGET, Items.IRON_INGOT, Items.DIAMOND, Items.REDSTONE, Items.WHEAT_SEEDS};
            case 6 -> new Item[0];
            case 7 -> new Item[]{Items.BOW};
            case 8 -> new Item[]{Items.DIAMOND_CHESTPLATE, Items.ELYTRA, Items.DIAMOND_LEGGINGS, Items.SHIELD};
            case 9 -> new Item[]{Items.WATER_BUCKET, Items.COMPASS, Items.CLOCK, Items.PAPER};
            default -> new Item[]{
               Items.IRON_SWORD,
               Items.IRON_PICKAXE,
               Items.IRON_AXE,
               Items.BOW,
               Items.TRIDENT,
               Items.FISHING_ROD,
               Items.APPLE,
               Items.COOKED_BEEF,
               Items.BREAD,
               Items.CARROT,
               Items.POTION,
               Items.STONE,
               Items.DIRT,
               Items.OAK_PLANKS,
               Items.GLASS,
               Items.IRON_INGOT,
               Items.DIAMOND,
               Items.GOLD_NUGGET,
               Items.COAL,
               Items.REDSTONE,
               Items.LEATHER,
               Items.STICK,
               Items.ARROW,
               Items.COMPASS,
               Items.CLOCK,
               Items.WATER_BUCKET,
               Items.PAPER
            };
         };
         if (preset != 6) {
            for (int i = 9; i < 36; i++) {
               ItemStack stack = new ItemStack(varied[(i - 9) % varied.length]);
               stack.setCount(Math.min(stack.getMaxStackSize(), new int[]{1, 16, 64}[(i - 9) % 3]));
               if (stack.isDamageableItem()) {
                  stack.setDamageValue(stack.getMaxDamage() / 3);
               }

               if (stack.is(Items.DIAMOND_SWORD)) {
                  stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
               }

               this.contents.setItem(i, stack);
            }

            if (preset == 2) {
               for (int i : new int[]{32, 33}) {
                  ItemStack stack = this.contents.getItem(i);
                  ItemPresentationData.debugUnknown(stack, "fictionalmod:unknown_" + i);
                  stack.set(DataComponents.CUSTOM_NAME, Component.literal("Unknown mod component " + i));
               }
            }

            Item[] belt = new Item[]{
               Items.DIAMOND_SWORD, Items.APPLE, Items.GOLD_INGOT, Items.GOLD_NUGGET, Items.STONE, Items.IRON_PICKAXE, Items.TORCH, Items.POTION, Items.BREAD
            };

            for (int i = 0; i < 9; i++) {
               Item item = preset == 3 ? new Item[]{Items.BOW, Items.IRON_PICKAXE, Items.DIAMOND_SWORD}[i % 3] : belt[i];
               this.contents.setItem(36 + i, new ItemStack(item, Math.min(item.getDefaultMaxStackSize(), i % 3 == 0 ? 1 : 16)));
            }

            this.contents.setItem(0, new ItemStack(Items.CRAFTING_TABLE));

            for (int i = 1; i <= 4; i++) {
               this.contents.setItem(i, new ItemStack(Items.OAK_PLANKS, i == 1 ? 16 : 1));
            }

            this.contents.setItem(5, new ItemStack(Items.DIAMOND_HELMET));
            this.contents.setItem(6, new ItemStack(Items.DIAMOND_CHESTPLATE));
            this.contents.setItem(7, new ItemStack(Items.DIAMOND_LEGGINGS));
            this.contents.setItem(8, new ItemStack(Items.DIAMOND_BOOTS));
            this.contents.setItem(45, new ItemStack(Items.SHIELD));
         }
      }

      @Override
      public ItemStack quickMoveStack(Player player, int index) {
         return ItemStack.EMPTY;
      }

      @Override
      public boolean stillValid(Player player) {
         return true;
      }
   }

   @Environment(EnvType.CLIENT)
   public static final class Preview extends Screen {
      private final Screen parent;
      public final MatLayout layout = MatLayout.inventory();
      public final MatCamera camera = new MatCamera();
      public final MatVisualDebug.Fixture menu = new MatVisualDebug.Fixture();
      private final MatItems items = new MatItems();
      private final MatAnimation animation = new MatAnimation();
      private final MatOverlay overlay = new MatOverlay();
      int captureAfter;
      public int hoverOverride = -1;
      public float timeOverride = Float.NaN;

      Preview(Screen parent) {
         super(Component.literal("Mat Inventory visual fixture"));
         this.parent = parent;
      }

      @Override
      public void extractBackground(GuiGraphicsExtractor g, int x, int y, float delta) {
         g.fill(0, 0, this.width, this.height, -15262953);
      }

      @Override
      public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float delta) {
         this.animation.frame();
         if (Float.isFinite(this.timeOverride)) {
            this.animation.elapsed = this.timeOverride;
         }

         if (MatInventoryClient.renderFailed) {
            this.onClose();
         } else {
            MatConfig config = MatInventoryClient.config;
            this.items.arrange(this.menu, this.layout, this.animation, false);
            this.camera.frame(this.layout);
            this.camera.update(this.width, this.height, x, y, config.cameraPitch, config.cameraFov, config.parallax);
            if (MatVisualDebug.automated
               && (System.getProperty("matinventory.visual.stage", "").equals("M5") || System.getProperty("matinventory.visual.stage", "").equals("Final"))) {
               MatLayout.Place p = this.layout
                  .places()
                  .stream()
                  .filter(place -> place.slot() == (MatVisualDebug.shotIndex == 0 ? 9 : 10))
                  .findFirst()
                  .orElseThrow();
               Vector4f point = this.camera.project(p.x(), 0.0F, p.z());
               x = (int)point.x;
               y = (int)point.y;
            }

            if (this.hoverOverride >= 0) {
               MatLayout.Place p = this.layout.place(this.hoverOverride);
               Vector4f point = this.camera.project(p.x(), 0.0F, p.z());
               x = (int)point.x;
               y = (int)point.y;
            } else if (this.hoverOverride == -2) {
               x = -100;
               y = -100;
            }

            int hovered = -1;
            if (x >= 0 && y >= 0 && this.camera.raycast(x, y)) {
               MatLayout.Place p = this.layout.pick(this.camera.hit.x, this.camera.hit.z, this.camera);
               if (p != null) {
                  hovered = p.slot();
               }
            }

            g.guiRenderState
               .addPicturesInPictureState(this.items.extract(this.menu, this.layout, this.camera, this.animation, hovered, config.glow, 0.0F, x, y));
            this.overlay.effects(g, this.width, this.height);
            this.overlay.counts(g, this.menu, this.layout, this.camera, this.width, this.height);
            this.overlay.render(g, this.menu, this.layout, this.camera, hovered, x, y, this.width, this.height, this.animation.elapsed);
            if (config.debugOverlay) {
               g.text(this.minecraft.font, Component.literal("TAB: preset " + (this.menu.preset + 1) + "   F9: PNG"), 7, 7, -1712593453, true);
            }

            if (this.captureAfter > 0 && --this.captureAfter == 0) {
               this.capture(
                  "fixture-" + this.minecraft.getWindow().getWidth() + "x" + this.minecraft.getWindow().getHeight() + "-" + System.currentTimeMillis()
               );
            }
         }
      }

      public void capture(String name) {
         MatVisualDebug.pendingCapture = name;
      }

      @Override
      public boolean keyPressed(KeyEvent key) {
         if (key.key() == InputConstants.KEY_F9) {
            this.captureAfter = 2;
            return true;
         } else if (key.key() == InputConstants.KEY_TAB) {
            this.menu.populate((this.menu.preset + 1) % 7);
            return true;
         } else {
            return super.keyPressed(key);
         }
      }

      @Override
      public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
         return vertical != 0.0 && this.camera.raycast(x, y) && this.layout.scroll(this.camera.hit.x, this.camera.hit.z, vertical > 0.0 ? -1 : 1)
            || super.mouseScrolled(x, y, horizontal, vertical);
      }

      public void invalidateWorld() {
         this.items.world.invalidate();
      }

      public MatWorldContext world() {
         return this.items.world;
      }

      @Override
      public boolean isPauseScreen() {
         return false;
      }

      @Override
      public void onClose() {
         MatInventoryClient.switchScreen(this.minecraft, this.parent);
      }
   }
}
