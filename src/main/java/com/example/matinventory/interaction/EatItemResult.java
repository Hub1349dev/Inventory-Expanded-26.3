package com.example.matinventory.interaction;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record EatItemResult(int menuId, int token, int variant) implements CustomPacketPayload {
   public static final Type<EatItemResult> TYPE = new Type<>(Identifier.fromNamespaceAndPath("matinventory", "eat_result_v1"));
   public static final StreamCodec<RegistryFriendlyByteBuf, EatItemResult> CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      EatItemResult::menuId,
      ByteBufCodecs.VAR_INT,
      EatItemResult::token,
      ByteBufCodecs.VAR_INT,
      EatItemResult::variant,
      EatItemResult::new
   );

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
