package com.trinity.client;

/**
 * Holds the current full-screen nuke-flash brightness on the client. Deliberately a plain holder with
 * no client-only imports, so the network handler ({@link com.trinity.network.TrinityNetwork}) can set
 * it from common code; the actual drawing/fading lives in {@link TrinityHud}.
 */
public final class ClientFlash {
    private ClientFlash() {}

    private static float alpha = 0.0f;

    /** Start (or intensify) a flash at the given peak brightness. */
    public static void trigger(float peak) {
        float clamped = Math.max(0.0f, Math.min(1.0f, peak));
        if (clamped > alpha) {
            alpha = clamped;
        }
    }

    /** Fade the flash out; call once per client tick. */
    public static void tick() {
        if (alpha > 0.0f) {
            alpha *= 0.86f;
            if (alpha < 0.02f) {
                alpha = 0.0f;
            }
        }
    }

    public static float alpha() {
        return alpha;
    }
}
