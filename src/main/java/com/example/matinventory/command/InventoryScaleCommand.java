package com.example.matinventory.command;

import com.example.matinventory.config.InventoryScales;
import com.example.matinventory.render.MatMaterials;
import com.example.matinventory.screen.InventorySettingsScreen;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.CommandNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public final class InventoryScaleCommand {
   private static Object packResources;
   private static int packGeneration = -1;
   private static final Set<String> itemPacks = new TreeSet<>();
   private static final SimpleCommandExceptionType BAD_VALUE = new SimpleCommandExceptionType(Component.translatable("matinventory.scale.bad_value"));
   private static final SimpleCommandExceptionType BAD_TARGET = new SimpleCommandExceptionType(Component.translatable("matinventory.scale.bad_target"));

   public static void register() {
      ClientCommandRegistrationCallback.EVENT
         .register(
            (ClientCommandRegistrationCallback)(dispatcher, access) -> dispatcher.register(
               (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)ClientCommands.literal("inventory")
                        .then(ClientCommands.argument("serverCommand", StringArgumentType.greedyString()).executes(context -> {
                           throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().create();
                        })))
                     .then(ClientCommands.literal("settings").executes(context -> {
                        Minecraft.getInstance().schedule(() -> Minecraft.getInstance().gui.setScreen(new InventorySettingsScreen()));
                        return 1;
                     })))
                  .then(
                     ClientCommands.literal("scale")
                        .then(
                           ClientCommands.argument("target", StringArgumentType.string())
                              .suggests((context, builder) -> {
                                 String prefix = builder.getRemaining().toLowerCase(Locale.ROOT);

                                 for (String name : targets().keySet()) {
                                    String text = StringArgumentType.escapeIfRequired(name);
                                    if (text.toLowerCase(Locale.ROOT).startsWith(prefix) || name.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                                       builder.suggest(text);
                                    }
                                 }

                                 return builder.buildFuture();
                              })
                              .then(
                                 ClientCommands.argument("value", StringArgumentType.greedyString())
                                    .suggests((context, builder) -> {
                                       for (String value : List.of("normal", "-200%", "-100%", "-50%", "0%", "50%", "100%", "200%")) {
                                          if (value.startsWith(builder.getRemaining())) {
                                             builder.suggest(value);
                                          }
                                       }

                                       return builder.buildFuture();
                                    })
                                    .executes(
                                       context -> {
                                          String target = StringArgumentType.getString(context, "target");
                                          String raw = StringArgumentType.getString(context, "value").strip();

                                          float percent;
                                          try {
                                             percent = raw.equalsIgnoreCase("normal")
                                                ? 0.0F
                                                : Float.parseFloat(raw.endsWith("%") ? raw.substring(0, raw.length() - 1) : raw);
                                             if (!Float.isFinite(percent) || percent < -200.0F || percent > 200.0F) {
                                                throw new NumberFormatException();
                                             }
                                          } catch (NumberFormatException ex) {
                                             throw BAD_VALUE.create();
                                          }

                                          String key = targets()
                                             .entrySet()
                                             .stream()
                                             .filter(e -> e.getKey().equalsIgnoreCase(target))
                                             .map(Entry::getValue)
                                             .findFirst()
                                             .orElse(null);
                                          if (key == null) {
                                             throw BAD_TARGET.create();
                                          }

                                          if (key.equals("item")) {
                                             ItemStack held = ((FabricClientCommandSource)context.getSource()).getPlayer().getMainHandItem();
                                             if (held.isEmpty()) {
                                                throw new SimpleCommandExceptionType(Component.translatable("matinventory.scale.no_item")).create();
                                             }

                                             key = InventoryScales.itemKey(held);
                                          }

                                          InventoryScales.set(key, percent);
                                          ((FabricClientCommandSource)context.getSource())
                                             .sendFeedback(
                                                Component.translatable(
                                                   "matinventory.scale.saved",
                                                   key,
                                                   percent == 0.0F ? "normal" : percent + "%",
                                                   String.format(Locale.ROOT, "%.3f", InventoryScales.multiplier(percent))
                                                )
                                             );
                                          return 1;
                                       }
                                    )
                              )
                        )
                  )
            )
         );
   }

   public static Map<String, String> targets() {
      LinkedHashMap<String, String> result = new LinkedHashMap<>();
      result.put("vanilla", "minecraft");
      result.put("item", "item");
      TreeSet<String> namespaces = new TreeSet<>();

      for (Identifier id : BuiltInRegistries.ITEM.keySet()) {
         namespaces.add(id.getNamespace());
      }

      for (String namespace : namespaces) {
         result.put(namespace, namespace);
         FabricLoader.getInstance().getModContainer(namespace).ifPresent(mod -> {
            String name = mod.getMetadata().getName();
            if (!result.containsKey(name)) {
               result.put(name, namespace);
            }
         });
      }

      Minecraft client = Minecraft.getInstance();
      if (client != null) {
         ResourceManager resources = client.getResourceManager();
         int generation = MatMaterials.generation;
         if (resources != packResources || generation != packGeneration) {
            packResources = resources;
            packGeneration = generation;
            itemPacks.clear();
            resources.listResources("items", id -> id.getPath().endsWith(".json")).values().forEach(resource -> itemPacks.add(resource.sourcePackId()));
         }

         for (String pack : itemPacks) {
            result.put("pack:" + pack, "pack:" + pack);
         }
      }

      return result;
   }

   public static void ensureChatSuggestions(Minecraft client) {
      if (client.player != null && client.getConnection() != null) {
         CommandDispatcher<FabricClientCommandSource> local = ClientCommands.getActiveDispatcher();
         if (local != null) {
            CommandNode<FabricClientCommandSource> ownRoot = local.getRoot().getChild("inventory");
            if (ownRoot != null) {
               CommandNode<FabricClientCommandSource> ownScale = ownRoot.getChild("scale");
               if (ownScale != null) {
                  CommandDispatcher<ClientSuggestionProvider> dispatcher = client.getConnection().getCommands();
                  CommandNode<ClientSuggestionProvider> root = dispatcher.getRoot().getChild("inventory");
                  if (root == null) {
                     root = dispatcher.register(LiteralArgumentBuilder.literal("inventory"));
                  }

                  CommandNode<FabricClientCommandSource> ownSettings = ownRoot.getChild("settings");
                  if (ownSettings != null && root.getChild("settings") == null) {
                     root.addChild((CommandNode) suggestionTree(ownSettings));
                  }

                  CommandNode<ClientSuggestionProvider> scale = root.getChild("scale");
                  if (scale == null || scale.getChild("target") == null || scale.getChild("target").getChild("value") == null) {
                     root.addChild((CommandNode) suggestionTree(ownScale));
                  }
               }
            }
         }
      }
   }

   private static CommandNode<FabricClientCommandSource> suggestionTree(CommandNode<FabricClientCommandSource> source) {
      ArgumentBuilder<FabricClientCommandSource, ? extends ArgumentBuilder<FabricClientCommandSource, ?>> builder = (ArgumentBuilder<FabricClientCommandSource, ? extends ArgumentBuilder<FabricClientCommandSource, ?>>)source.createBuilder();
      builder.requires(s -> true);
      if (source.getCommand() != null) {
         builder.executes(c -> 0);
      }

      for (CommandNode<FabricClientCommandSource> child : source.getChildren()) {
         builder.then(suggestionTree(child));
      }

      return builder.build();
   }

   private InventoryScaleCommand() {
   }
}
