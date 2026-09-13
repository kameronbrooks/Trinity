package com.trinity.network;

import com.trinity.client.ClientFlash;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Network setup. Registered on the mod bus from {@link com.trinity.Trinity}. Only one payload: the
 * server-to-client nuke flash. The handler just stores the peak brightness in {@link ClientFlash}
 * (a plain holder, safe to touch from this common class); the client HUD reads and fades it.
 */
public final class TrinityNetwork {
    private TrinityNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
                NukeFlashPayload.TYPE,
                NukeFlashPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientFlash.trigger(payload.peak())));
    }
}
