package com.example.matinventory.core;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class MatPalette {
   private static final int[] COLORS = new int[]{16777215, 4616094, 7952014, 2369321, 2703155, 7830912};
   private static final String[] NAMES = new String[]{"Laranja", "Azul", "Roxo", "Preto", "Verde escuro", "Cinza"};

   public static int next(int i) {
      return Math.floorMod(i + 1, COLORS.length);
   }

   public static String name(int i) {
      return NAMES[Math.floorMod(i, COLORS.length)];
   }

   public static int tint(int argb, int i) {
      int c = COLORS[Math.floorMod(i, COLORS.length)];
      return argb & 0xFF000000
         | (argb >> 16 & 0xFF) * (c >> 16 & 0xFF) / 255 << 16
         | (argb >> 8 & 0xFF) * (c >> 8 & 0xFF) / 255 << 8
         | (argb & 0xFF) * (c & 0xFF) / 255;
   }

   private MatPalette() {
   }
}
