package com.example.matinventory.capacity;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CapacityHello(int storageSlots) implements CustomPacketPayload {
   public static final Type<CapacityHello> TYPE = new Type<>(Identifier.fromNamespaceAndPath("matinventory", "capacity_v4"));
   public static final StreamCodec<RegistryFriendlyByteBuf, CapacityHello> CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, CapacityHello::storageSlots, CapacityHello::new
   );

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
