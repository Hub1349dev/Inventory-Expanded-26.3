package com.example.matinventory.render;

import java.lang.reflect.Method;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

@Environment(EnvType.CLIENT)
public final class ShaderSupport {
   private static Object api;
   private static Method enabled;
   private static Method shadow;
   public static long frames;
   public static long shadowFrames;

   public static boolean active() {
      if (!FabricLoader.getInstance().isModLoaded("iris")) {
         return false;
      }

      try {
         if (api == null) {
            Class<?> type = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            api = type.getMethod("getInstance").invoke(null);
            enabled = type.getMethod("isShaderPackInUse");
            shadow = type.getMethod("isRenderingShadowPass");
         }

         return (Boolean)enabled.invoke(api);
      } catch (ReflectiveOperationException error) {
         throw new IllegalStateException("Cannot query the installed Iris API", error);
      }
   }

   public static boolean drawing() {
      return NativeWorldScene.drawing();
   }

   public static boolean shadowPass() {
      try {
         return api != null && (Boolean)shadow.invoke(api);
      } catch (ReflectiveOperationException e) {
         throw new IllegalStateException(e);
      }
   }

   private ShaderSupport() {
   }
}
