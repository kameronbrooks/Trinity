package com.trinity.blast;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Precomputes, once per radius, the set of block offsets inside a sphere, packed into ints and
 * sorted nearest-to-farthest. A {@link Blast} then walks this array outward a fixed budget at a
 * time, which is both cheap (no per-tick geometry) and gives the destruction a visible shockwave
 * that expands from ground zero.
 *
 * <p>Each entry packs the signed offset {@code (x, y, z)} (each biased by {@link #BIAS} into 0..127,
 * seven bits) into the low 21 bits of an int. The sort is done on a temporary {@code long} whose
 * high bits hold the squared distance, so decoding the sorted ints yields distance order for free.
 */
public final class BlastShape {
    private BlastShape() {}

    private static final int BIAS = 64;
    private static final int MASK = 0x7F;
    private static final Map<Integer, int[]> CACHE = new ConcurrentHashMap<>();

    /** Packed, distance-sorted offsets for a sphere of the given radius (cached). */
    public static int[] offsetsFor(int radius) {
        return CACHE.computeIfAbsent(radius, BlastShape::build);
    }

    private static int[] build(int radius) {
        int r2 = radius * radius;
        int side = radius * 2 + 1;
        long[] keys = new long[side * side * side];
        int n = 0;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    int d2 = x * x + y * y + z * z;
                    if (d2 <= r2) {
                        int packed = ((x + BIAS) << 14) | ((y + BIAS) << 7) | (z + BIAS);
                        keys[n++] = ((long) d2 << 21) | packed;
                    }
                }
            }
        }
        keys = Arrays.copyOf(keys, n);
        Arrays.sort(keys);
        int[] out = new int[n];
        for (int i = 0; i < n; i++) {
            out[i] = (int) (keys[i] & 0x1FFFFF);
        }
        return out;
    }

    public static int ox(int packed) {
        return ((packed >> 14) & MASK) - BIAS;
    }

    public static int oy(int packed) {
        return ((packed >> 7) & MASK) - BIAS;
    }

    public static int oz(int packed) {
        return (packed & MASK) - BIAS;
    }
}
