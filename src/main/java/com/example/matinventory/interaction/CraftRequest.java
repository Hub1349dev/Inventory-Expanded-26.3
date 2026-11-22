package com.example.matinventory.interaction;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record CraftRequest(int menuId, boolean all, ItemStack expected) implements CustomPacketPayload {
   public static final Type<CraftRequest> TYPE = new Type<>(Identifier.fromNamespaceAndPath("matinventory", "craft_output_v1"));
   public static final StreamCodec<RegistryFriendlyByteBuf, CraftRequest> CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      CraftRequest::menuId,
      ByteBufCodecs.BOOL,
      CraftRequest::all,
      ItemStack.OPTIONAL_STREAM_CODEC,
      CraftRequest::expected,
      CraftRequest::new
   );

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
