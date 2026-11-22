package com.example.matinventory.screen;

import com.example.matinventory.MatInventoryClient;
import com.example.matinventory.compat.LegacyGui;
import com.example.matinventory.compat.LegacyScreen;
import com.example.matinventory.config.InventorySound;
import com.example.matinventory.config.MatConfig;
import com.example.matinventory.render.UiButtonTextures;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

@Environment(EnvType.CLIENT)
public final class InventorySettingsScreen extends LegacyScreen {
   private final List<InventorySettingsScreen.Option> options = new ArrayList<>();
   private final List<InventorySettingsScreen.Option> visible = new ArrayList<>();
   private final Map<String, Button> toggles = new HashMap<>();
   private EditBox search;
   private int filter;
   private int scroll;
   private int contentHeight;
   private int left;
   private int right;
   private int top;
   private int bottom;
   private int controlWidth;
   private boolean draggingScroll;

   public InventorySettingsScreen() {
      super(tr("title"));
      MatConfig c = MatInventoryClient.config;
      this.options.add(new InventorySettingsScreen.Option("enabled", false, () -> c.enabled, v -> c.enabled = v));
      this.options.add(new InventorySettingsScreen.Option("crafting_table", false, () -> c.craftingTable, v -> c.craftingTable = v));
      this.options.add(new InventorySettingsScreen.Option("dark_buttons", false, () -> c.darkUiButtons, v -> c.darkUiButtons = v));
      this.options
         .add(
            new InventorySettingsScreen.Option(
               "craft_effect",
               false,
               () -> tr("craft_effect." + c.craftFocusEffect.name().toLowerCase(Locale.ROOT))
                  .copy()
                  .withStyle(c.craftFocusEffect == MatConfig.CraftFocusEffect.OFF ? ChatFormatting.GRAY : ChatFormatting.GREEN),
               () -> c.craftFocusEffect = c.craftFocusEffect.next()
            )
         );

      for (InventorySound sound : InventorySound.values()) {
         this.options.add(new InventorySettingsScreen.Option("sound." + sound.id(), true, sound::enabled, sound::set));
      }
   }

   private static Component tr(String key) {
      return Component.translatable("matinventory.settings." + key);
   }

   @Override
   protected void init() {
      String query = this.search == null ? "" : this.search.getValue();
      this.clearWidgets();
      this.toggles.clear();
      this.left = Math.max(12, (this.width - 850) / 2);
      this.right = this.width - this.left;
      this.top = 94;
      this.bottom = this.height - 58;
      this.controlWidth = 88;

      for (MatConfig.CraftFocusEffect effect : MatConfig.CraftFocusEffect.values()) {
         this.controlWidth = Math.max(this.controlWidth, this.font.width(tr("craft_effect." + effect.name().toLowerCase(Locale.ROOT))) + 16);
      }

      this.search = this.addRenderableWidget(new EditBox(this.font, this.left, 36, this.right - this.left, 20, tr("search")));
      this.search.setHint(tr("search"));
      this.search.setValue(query);
      this.search.setMaxLength(128);
      this.search.setResponder(v -> {
         this.scroll = 0;
         this.refresh();
      });
      String[] categories = new String[]{"all", "general", "audio"};
      int gap = 6;
      int w = (this.right - this.left - gap * 2) / 3;

      for (int i = 0; i < 3; i++) {
         int category = i;
         this.addRenderableWidget(Button.builder(tr(categories[i]), bx -> {
            this.filter = category;
            this.scroll = 0;
            this.refresh();
         }).bounds(this.left + i * (w + gap), 62, w, 20).build());
      }

      for (InventorySettingsScreen.Option option : this.options) {
         Button b = Button.builder(this.state(option), button -> {
            option.change.run();
            MatInventoryClient.config.save();
            this.refresh();
         }).bounds(this.right - this.controlWidth - 12, this.top, this.controlWidth, 20).build();
         this.toggles.put(option.id, this.addRenderableWidget(b));
      }

      this.addRenderableWidget(
         Button.builder(Component.translatable("gui.done"), bx -> this.onClose()).bounds(this.left, this.height - 30, this.right - this.left, 20).build()
      );
      this.refresh();
   }

   private static Component booleanState(boolean value) {
      return tr(value ? "on" : "off").copy().withStyle(value ? ChatFormatting.GREEN : ChatFormatting.GRAY);
   }

   private Component state(InventorySettingsScreen.Option o) {
      return o.state.get();
   }

   private int textWidth() {
      return Math.max(55, this.right - this.left - this.controlWidth - 32);
   }

   private int rowHeight(InventorySettingsScreen.Option o) {
      return Math.max(46, 14 + this.font.split(tr(o.id), this.textWidth()).size() * 10 + this.font.split(tr(o.id + ".desc"), this.textWidth()).size() * 10)
         + (o.id.equals("dark_buttons") ? 20 : 0);
   }

   private void refresh() {
      this.visible.clear();
      String query = this.search == null ? "" : this.search.getValue().strip().toLowerCase(Locale.ROOT);

      for (InventorySettingsScreen.Option o : this.options) {
         if ((this.filter == 0 || this.filter == 1 && !o.audio || this.filter == 2 && o.audio)
            && (tr(o.id).getString() + " " + tr(o.id + ".desc").getString()).toLowerCase(Locale.ROOT).contains(query)) {
            this.visible.add(o);
         }
      }

      this.contentHeight = this.visible.stream().mapToInt(this::rowHeight).sum();
      this.scroll = Math.clamp(this.scroll, 0, this.maxScroll());
      this.positionButtons();
   }

   private int maxScroll() {
      return Math.max(0, this.contentHeight - Math.max(1, this.bottom - this.top));
   }

   private void positionButtons() {
      for (Button b : this.toggles.values()) {
         b.visible = false;
      }

      int y = this.top - this.scroll;

      for (InventorySettingsScreen.Option o : this.visible) {
         int h = this.rowHeight(o);
         Button b = this.toggles.get(o.id);
         b.setY(y + 8);
         b.setMessage(this.state(o));
         b.visible = b.getY() >= this.top && b.getY() + 20 <= this.bottom;
         y += h;
      }
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) {
      g.centeredText(this.font, this.title, this.width / 2, 10, -1);
      g.centeredText(this.font, Component.literal("Inventory Expanded 0.2.1 Beta"), this.width / 2, 23, -5592406);
      g.fill(this.left, this.top - 3, this.right, this.top - 2, -8947849);
      g.fill(this.left, this.bottom + 1, this.right, this.bottom + 2, -8947849);
      g.enableScissor(this.left, this.top, this.right, this.bottom);
      int y = this.top - this.scroll;

      for (InventorySettingsScreen.Option o : this.visible) {
         int h = this.rowHeight(o);
         if (y + h > this.top && y < this.bottom) {
            g.fill(this.left, y, this.right - 7, y + h - 1, 1375731712);
            int ty = y + 7;

            for (FormattedCharSequence line : this.font.split(tr(o.id), this.textWidth())) {
               g.text(this.font, line, this.left + 8, ty, -1, false);
               ty += 10;
            }

            ty += 3;

            for (FormattedCharSequence line : this.font.split(tr(o.id + ".desc"), this.textWidth())) {
               g.text(this.font, line, this.left + 8, ty, o.id.equals("crafting_table") ? -11894 : -5592406, false);
               ty += 10;
            }

            if (o.id.equals("dark_buttons")) {
               String[] icons = new String[]{"mouse_left", "mouse_right", "mouse_middle", "key_ctrl", "key_e"};
               int ix = this.left + 8;

               for (String icon : icons) {
                  int w = icon.equals("key_ctrl") ? 25 : (icon.equals("key_e") ? 13 : 12);
                  int intHeight = icon.startsWith("mouse") ? 16 : 13;
                  LegacyGui.blit(
                     g,
                     UiButtonTextures.themed(Identifier.fromNamespaceAndPath("matinventory", "textures/gui/" + icon + ".png")),
                     ix,
                     y + h - 21,
                     0.0F,
                     0.0F,
                     w,
                     intHeight,
                     w,
                     intHeight,
                     -1
                  );
                  ix += w + 8;
               }
            }

            g.fill(this.left, y + h - 1, this.right - 7, y + h, -11184811);
         }

         y += h;
      }

      if (this.visible.isEmpty()) {
         g.centeredText(this.font, tr("empty"), this.width / 2, this.top + 18, -5592406);
      }

      g.disableScissor();
      if (this.maxScroll() > 0) {
         int available = this.bottom - this.top;
         int thumb = Math.max(18, available * available / Math.max(1, this.contentHeight));
         int sy = this.top + (available - thumb) * this.scroll / this.maxScroll();
         g.fill(this.right - 5, this.top, this.right, this.bottom, -15198184);
         g.fill(this.right - 5, sy, this.right, sy + thumb, -5592406);
      }

      g.centeredText(this.font, tr("saved"), this.width / 2, this.height - 47, -5592406);

      for (GuiEventListener widget : this.children()) {
         if (widget instanceof Renderable renderable) {
            renderable.extractRenderState(g, mx, my, partial);
         }
      }
   }

   @Override
   public boolean mouseScrolled(double x, double y, double dx, double dy) {
      if (y >= this.top && y < this.bottom) {
         this.scroll = Math.clamp(this.scroll - (int)(dy * 30.0), 0, this.maxScroll());
         this.positionButtons();
         return true;
      } else {
         return super.mouseScrolled(x, y, dx, dy);
      }
   }

   @Override
   public boolean mouseClicked(double x, double y, int button) {
      if (button == 0 && this.maxScroll() > 0 && x >= this.right - 6 && x <= this.right && y >= this.top && y < this.bottom) {
         this.draggingScroll = true;
         this.scrollTo(y);
         return true;
      } else {
         return super.mouseClicked(x, y, button);
      }
   }

   private void scrollTo(double y) {
      int available = this.bottom - this.top;
      int thumb = Math.max(18, available * available / Math.max(1, this.contentHeight));
      this.scroll = Math.clamp((int)((y - this.top - thumb * 0.5) * this.maxScroll() / Math.max(1, available - thumb)), 0, this.maxScroll());
      this.positionButtons();
   }

   @Override
   public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
      if (this.draggingScroll && button == 0) {
         this.scrollTo(y);
         return true;
      } else {
         return super.mouseDragged(x, y, button, dx, dy);
      }
   }

   @Override
   public boolean mouseReleased(double x, double y, int button) {
      this.draggingScroll = false;
      return super.mouseReleased(x, y, button);
   }

   @Override
   public boolean keyPressed(int key, int scan, int mods) {
      if (!this.search.isFocused() && (key == InputConstants.KEY_PAGEDOWN || key == InputConstants.KEY_PAGEUP)) {
         this.scroll = Math.clamp(this.scroll + (key == InputConstants.KEY_PAGEDOWN ? 1 : -1) * Math.max(30, this.bottom - this.top - 20), 0, this.maxScroll());
         this.positionButtons();
         return true;
      } else {
         return super.keyPressed(key, scan, mods);
      }
   }

   @Override
   public void onClose() {
      MatInventoryClient.config.save();
      this.minecraft.gui.setScreen(null);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @Environment(EnvType.CLIENT)
   private record Option(String id, boolean audio, Supplier<Component> state, Runnable change) {
      Option(String id, boolean audio, BooleanSupplier get, Consumer<Boolean> set) {
         this(id, audio, () -> InventorySettingsScreen.booleanState(get.getAsBoolean()), () -> set.accept(!get.getAsBoolean()));
      }
   }
}
