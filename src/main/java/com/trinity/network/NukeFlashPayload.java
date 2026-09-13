package com.trinity.network;

import com.trinity.Trinity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server -> client: "a nuke just went off near you — flash the screen white". {@code peak} is the
 * starting brightness (0..1), scaled by how close the viewer was, and fades out client-side.
 */
public record NukeFlashPayload(float peak) implements CustomPacketPayload {
    public static final Type<NukeFlashPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Trinity.MOD_ID, "nuke_flash"));

    public static final StreamCodec<ByteBuf, NukeFlashPayload> STREAM_CODEC =
            ByteBufCodecs.FLOAT.map(NukeFlashPayload::new, NukeFlashPayload::peak);

    @Override
    public Type<NukeFlashPayload> type() {
        return TYPE;
    }
}
