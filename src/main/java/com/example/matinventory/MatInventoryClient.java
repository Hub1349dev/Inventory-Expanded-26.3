package com.example.matinventory;

import com.example.matinventory.arrangement.ItemPresentationData;
import com.example.matinventory.capacity.CapacityHello;
import com.example.matinventory.capacity.ExtraInventory;
import com.example.matinventory.capacity.NativeExtraMenuAccess;
import com.example.matinventory.command.InventoryScaleCommand;
import com.example.matinventory.config.MatConfig;
import com.example.matinventory.config.PlacementProfiles;
import com.example.matinventory.debug.MatVisualDebug;
import com.example.matinventory.interaction.EatItemResult;
import com.example.matinventory.layout.MatLayout;
import com.example.matinventory.render.MatItems;
import com.example.matinventory.render.MatMaterials;
import com.example.matinventory.render.MatSceneRenderer;
import com.example.matinventory.screen.MatBlockInventoryScreen;
import com.example.matinventory.screen.MatInventoryScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.ClientStarted;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Disconnect;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Join;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.BeforeInit;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents.AllowKeyPress;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents.TagsLoaded;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping.Category;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class MatInventoryClient implements ClientModInitializer {
   public static final Logger LOG = LoggerFactory.getLogger("matinventory");
   public static MatConfig config;
   public static KeyMapping toggle;
   public static boolean switching;
   public static boolean renderFailed;

   @Override
   public void onInitializeClient() {
      ClientPlayNetworking.registerGlobalReceiver(EatItemResult.TYPE, (payload, context) -> {
         if (context.client().gui.screen() instanceof MatInventoryScreen mat) {
            mat.eatConfirmed(payload.menuId(), payload.token(), payload.variant());
         }
      });
      ClientPlayNetworking.registerGlobalReceiver(CapacityHello.TYPE, (payload, context) -> {
         ExtraInventory.clientStorageSlots = Math.clamp(payload.storageSlots(), 27, 256);
         ExtraInventory.clientSupported = true;
      });
      ClientPlayConnectionEvents.DISCONNECT.register((Disconnect)(handler, client) -> ExtraInventory.clientSupported = false);
      config = MatConfig.load();
      ClientPlayConnectionEvents.JOIN.register((Join)(handler, sender, client) -> PlacementProfiles.activate(client));
      InventoryScaleCommand.register();
      ItemPresentationData.reload();
      MatVisualDebug.register();
      PictureInPictureRendererRegistry.register(MatSceneRenderer::new);
      CommonLifecycleEvents.TAGS_LOADED.register((TagsLoaded)(registries, client) -> MatItems.invalidateTags());
      ClientLifecycleEvents.CLIENT_STARTED.register((ClientStarted)client -> {
         if (client.getResourceManager() instanceof ReloadableResourceManager resources) {
            resources.registerReloadListener((ResourceManagerReloadListener)manager -> MatMaterials.invalidate());
         }
      });
      ClientLifecycleEvents.CLIENT_STARTED.register((ClientStarted)client -> MatMaterials.ensureLoaded());
      toggle = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.matinventory.toggle", InputConstants.KEY_F8, Category.INVENTORY));
      ScreenEvents.BEFORE_INIT
         .register(
            (BeforeInit)(client, screen, width, height) -> {
               if (screen instanceof MatInventoryScreen
                  || screen instanceof MatBlockInventoryScreen
                  || screen.getClass() == InventoryScreen.class
                  || screen.getClass() == CraftingScreen.class) {
                  ScreenKeyboardEvents.allowKeyPress(screen).register((AllowKeyPress)(current, event) -> {
                     if (toggle.matches(event)) {
                        toggle(client);
                        return false;
                     } else {
                        return true;
                     }
                  });
               }
            }
         );
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)client -> {
         InventoryScaleCommand.ensureChatSuggestions(client);
         PlacementProfiles.prune();

         while (toggle.consumeClick()) {
            toggle(client);
         }

         if (renderFailed && client.gui.screen() instanceof MatInventoryScreen mat) {
            mat.fallback();
         }

         if (config.enabled && client.player != null && !client.player.isCreative() && client.gui.overlay() == null && client.gui.screen() == null) {
            MatItems.forMenu(client.player.inventoryMenu).preload(client.player.inventoryMenu);
         }
      });
      ClientPlayConnectionEvents.DISCONNECT.register((Disconnect)(handler, client) -> MatItems.clearMenus());
      LOG.info("Inventory Expanded 0.2.1 initialized for Fabric Minecraft 26.3");
      if (FabricLoader.getInstance().isDevelopmentEnvironment() && Boolean.getBoolean("matinventory.validation")) {
         try {
            Class.forName(
                  Boolean.getBoolean("matinventory.fabricParity")
                     ? "com.example.matinventory.validation.FabricParity"
                     : "com.example.matinventory.validation.LabValidation"
               )
               .getMethod("register")
               .invoke(null);
         } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Cannot initialize development validation; use -PmatValidation and inspect its cause", ex);
         }
      }
   }

   public static Screen replace(Screen screen) {
      Minecraft client = Minecraft.getInstance();
      if (!switching && config != null && config.enabled && client.player != null) {
         if (!renderFailed && ExtraInventory.enabled(client.player)) {
            if (screen != null && screen.getClass() == ContainerScreen.class) {
               return new MatBlockInventoryScreen(((ContainerScreen)screen).getMenu(), client.player.getInventory(), screen.getTitle());
            }

            if (config.craftingTable && screen != null && screen.getClass() == CraftingScreen.class) {
               return new MatBlockInventoryScreen(((CraftingScreen)screen).getMenu(), client.player.getInventory(), screen.getTitle());
            }
         }

         if (!renderFailed && !client.player.isCreative()) {
            return screen != null && screen.getClass() == InventoryScreen.class
               ? new MatInventoryScreen(client.player.inventoryMenu, client.player.getInventory(), screen.getTitle(), MatLayout.inventory(), screen)
               : screen;
         } else {
            return screen;
         }
      } else {
         return screen;
      }
   }

   public static void toggle(Minecraft client) {
      config.enabled = !config.enabled;
      config.save();
      if (client.gui.screen() instanceof MatInventoryScreen mat) {
         mat.fallback();
      } else if (client.gui.screen() instanceof MatBlockInventoryScreen block) {
         switchScreen(client, block.nativeScreen());
      } else if (client.gui.screen() instanceof AbstractContainerScreen<?> screen
         && screen.getMenu() instanceof NativeExtraMenuAccess access
         && access.matinventory$extraEnd() > 0) {
         client.resizeGui();
      } else if (config.enabled && client.gui.screen() != null) {
         Screen replacement = replace(client.gui.screen());
         if (replacement != client.gui.screen()) {
            switchScreen(client, replacement);
         }
      }
   }

   public static void switchScreen(Minecraft client, Screen screen) {
      switching = true;

      try {
         client.gui.setScreen(screen);
      } finally {
         switching = false;
      }
   }
}
