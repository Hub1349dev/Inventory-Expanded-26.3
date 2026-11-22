package com.example.matinventory.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface NativePreviewAccess {
   NativeMenuPreview matinventory$nativePreview();

   void matinventory$refreshNativePreview();
}
