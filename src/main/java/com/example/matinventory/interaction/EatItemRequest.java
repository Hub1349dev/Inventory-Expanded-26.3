package com.example.matinventory.interaction;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record EatItemRequest(int menuId, int slot, int token, ItemStack expected) implements CustomPacketPayload {
   public static final Type<EatItemRequest> TYPE = new Type<>(Identifier.fromNamespaceAndPath("matinventory", "eat_item_v1"));
   public static final StreamCodec<RegistryFriendlyByteBuf, EatItemRequest> CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      EatItemRequest::menuId,
      ByteBufCodecs.VAR_INT,
      EatItemRequest::slot,
      ByteBufCodecs.VAR_INT,
      EatItemRequest::token,
      ItemStack.OPTIONAL_STREAM_CODEC,
      EatItemRequest::expected,
      EatItemRequest::new
   );

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
