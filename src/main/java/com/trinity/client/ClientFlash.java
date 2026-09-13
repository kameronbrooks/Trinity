package com.trinity.client;

/**
 * Holds the current full-screen nuke-flash brightness on the client. Deliberately a plain holder with
 * no client-only imports, so the network handler ({@link com.trinity.network.TrinityNetwork}) can set
 * it from common code; the actual drawing/fading lives in {@link TrinityHud}.
 */
public final class ClientFlash {
    private ClientFlash() {}

    /** Ticks held at full brightness before the fade begins (the "blinding" window). */
    private static final int HOLD_TICKS = 6;
    /** Per-tick fade multiplier once fading — closer to 1.0 lingers longer. */
    private static final float FADE = 0.90f;

    private static float alpha = 0.0f;
    private static int hold = 0;

    /** Start (or intensify) a flash at the given peak brightness. */
    public static void trigger(float peak) {
        float clamped = Math.max(0.0f, Math.min(1.0f, peak));
        if (clamped > alpha) {
            alpha = clamped;
            hold = HOLD_TICKS;
        }
    }

    /** Hold, then fade the flash out; call once per client tick. */
    public static void tick() {
        if (alpha <= 0.0f) {
            return;
        }
        if (hold > 0) {
            hold--;
            return;
        }
        alpha *= FADE;
        if (alpha < 0.02f) {
            alpha = 0.0f;
        }
    }

    public static float alpha() {
        return alpha;
    }
}
