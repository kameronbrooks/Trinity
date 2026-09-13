package com.trinity.blast;

import com.trinity.registry.TrinityDamageTypes;
import com.trinity.registry.TrinityEffects;
import com.trinity.registry.TrinityParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import com.trinity.network.NukeFlashPayload;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * A single detonation, run in bounded slices by {@link BlastManager} across many server ticks.
 *
 * <p>The world edits are the part that would normally freeze or crash the game, so they are the part
 * that is throttled: {@link BlastShape} hands us every block offset in the sphere pre-sorted from the
 * centre outward, and each tick we process only {@link #BLOCKS_PER_TICK} of them — producing an
 * expanding shockwave — with block edits made using {@link Block#UPDATE_CLIENTS} |
 * {@link Block#UPDATE_KNOWN_SHAPE} so neighbour updates, block-physics cascades, and item drops (the
 * real cost of a big TNT stack) never fire. Meanwhile the flash, the mushroom cloud (built from a
 * custom, oversized smoke particle), the entity kill, and the lingering radiation cloud play out on
 * their own schedule.
 *
 * <p>Because neighbour updates are suppressed, adjacent fluids would never learn the crater opened
 * next to them. So air carves on the sphere's boundary remember any untouched fluid they expose
 * ({@link #fluidWakes}); once the dig finishes those fluids are given a scheduled tick, and vanilla
 * fluid spreading floods the crater from there.
 */
public final class Blast {
    /** Destruction radius, in blocks. The single knob for how big the bomb is. */
    public static final int RADIUS = 48;

    /** How many block positions to process per server tick. Lower = smoother, slower shockwave. */
    private static final int BLOCKS_PER_TICK = 3500;

    /** How long the mushroom-cloud animation runs, in ticks. */
    private static final int ANIMATION_TICKS = 200;

    /** Block edits: tell clients, but skip neighbour updates, physics, and drops. */
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private static final double RISE_PER_TICK = 1.3;

    private final ServerLevel level;
    private final BlockPos center;
    private final int radius;
    private final int[] offsets;
    private final RandomSource rand;

    private final double craterR2;
    private final double rubbleR2;
    private final double boundaryR2;
    private final int r2;
    private final double capHeight;
    private final double capMaxR;
    private final double capStartAge;

    /** Untouched fluids exposed at the crater boundary, to be woken once the dig completes. */
    private final Set<Long> fluidWakes = new HashSet<>();

    private int cursor = 0;
    private int age = 0;
    private boolean finished = false;

    public Blast(ServerLevel level, BlockPos center, int radius) {
        this.level = level;
        this.center = center;
        this.radius = radius;
        this.offsets = BlastShape.offsetsFor(radius);
        this.rand = level.getRandom();
        this.r2 = radius * radius;
        double craterR = radius * 0.32; // the open pit — kept tighter than the (full-radius) blast wave
        this.craterR2 = craterR * craterR;
        double rubbleR = radius * 0.60;
        this.rubbleR2 = rubbleR * rubbleR;
        double boundaryR = radius - 2;
        this.boundaryR2 = boundaryR * boundaryR;
        this.capHeight = radius * 1.6;
        this.capMaxR = radius * 0.95;
        this.capStartAge = (capHeight * 0.72) / RISE_PER_TICK;
    }

    /** Advance one server tick. Returns {@code true} when the blast is completely finished. */
    public boolean tick() {
        if (age == 0) {
            initialBurst();
        }

        if (cursor < offsets.length) {
            int budget = BLOCKS_PER_TICK;
            while (budget-- > 0 && cursor < offsets.length) {
                int packed = offsets[cursor++];
                handle(BlastShape.ox(packed), BlastShape.oy(packed), BlastShape.oz(packed));
            }
            if (cursor >= offsets.length && !finished) {
                igniteSurfaces();
                wakeFluids();
                finished = true;
            }
        }

        if (age <= ANIMATION_TICKS) {
            mushroomStep(age);
        }

        age++;
        return cursor >= offsets.length && age > ANIMATION_TICKS;
    }

    // ------------------------------------------------------------------ destruction

    private void handle(int ox, int oy, int oz) {
        int d2 = ox * ox + oy * oy + oz * oz;
        BlockPos pos = center.offset(ox, oy, oz);
        BlockState s = level.getBlockState(pos);
        if (s.isAir()) {
            return;
        }
        if (s.getDestroySpeed(level, pos) < 0) {
            return; // unbreakable (bedrock, barrier, ...)
        }
        boolean fluid = !s.getFluidState().isEmpty();

        if (d2 <= craterR2) {
            carveAir(pos, ox, oy, oz, d2); // crater core: total annihilation
            return;
        }
        if (fluid || s.is(BlockTags.LEAVES) || s.canBeReplaced()) {
            carveAir(pos, ox, oy, oz, d2); // foliage, fluids, snow, etc. are vapourised
            return;
        }

        BlockState scorched = scorch(s);
        if (scorched != null) {
            set(pos, scorched); // burned in place: wood->coal, sand->glass, stone->blackstone, ...
            return;
        }
        if (d2 <= rubbleR2 && rand.nextFloat() < 0.25F) {
            carveAir(pos, ox, oy, oz, d2); // partial cratering of whatever's left
        }
        // otherwise the block is left standing but scorched around (fires are seeded separately)
    }

    private void set(BlockPos pos, BlockState state) {
        level.setBlock(pos, state, FLAGS);
    }

    /**
     * Broadcast a particle to every player in the level with the long-distance flag set. The plain
     * {@code ServerLevel.sendParticles(...)} broadcast only reaches — and only renders on — clients
     * within 32 blocks; the per-player overload with {@code overrideLimiter = true} pushes that to
     * 512, which is what makes the flash and mushroom cloud visible from a safe viewing distance.
     */
    private void spawn(ParticleOptions particle, double x, double y, double z,
                       int count, double dx, double dy, double dz, double speed) {
        for (ServerPlayer player : level.players()) {
            level.sendParticles(player, particle, true, x, y, z, count, dx, dy, dz, speed);
        }
    }

    /** Set a block to air, and — near the boundary — remember any untouched fluid it just exposed. */
    private void carveAir(BlockPos pos, int ox, int oy, int oz, int d2) {
        set(pos, Blocks.AIR.defaultBlockState());
        if (d2 < boundaryR2) {
            return; // interior air is surrounded by our own carves; only the rim meets the world
        }
        for (Direction dir : Direction.values()) {
            int nx = ox + dir.getStepX();
            int ny = oy + dir.getStepY();
            int nz = oz + dir.getStepZ();
            if ((long) nx * nx + ny * ny + nz * nz > r2) { // neighbour lies outside the blast = untouched
                BlockPos np = pos.relative(dir);
                if (!level.getFluidState(np).isEmpty()) {
                    fluidWakes.add(np.asLong());
                }
            }
        }
    }

    /** Map a solid block to its burned equivalent, or {@code null} to leave it standing. */
    private BlockState scorch(BlockState s) {
        if (isWood(s)) {
            return Blocks.COAL_BLOCK.defaultBlockState(); // charred timber / ash
        }
        if (s.is(BlockTags.SAND)) {
            // Trinitite: desert sand fused into green glass, exactly like the real Trinity test.
            return (rand.nextInt(4) == 0 ? Blocks.LIME_STAINED_GLASS : Blocks.GREEN_STAINED_GLASS).defaultBlockState();
        }
        if (s.is(BlockTags.DIRT)) {
            return Blocks.COARSE_DIRT.defaultBlockState(); // grass/dirt/podzol -> scorched earth
        }
        if (isStoneLike(s)) {
            return (rand.nextInt(3) == 0 ? Blocks.BASALT : Blocks.BLACKSTONE).defaultBlockState(); // blackened rock
        }
        return null;
    }

    private static boolean isWood(BlockState s) {
        return s.is(BlockTags.LOGS)
                || s.is(BlockTags.PLANKS)
                || s.is(BlockTags.WOODEN_STAIRS)
                || s.is(BlockTags.WOODEN_SLABS)
                || s.is(BlockTags.WOODEN_FENCES)
                || s.is(BlockTags.FENCE_GATES)
                || s.is(BlockTags.WOODEN_DOORS)
                || s.is(BlockTags.WOODEN_TRAPDOORS)
                || s.is(BlockTags.WOODEN_PRESSURE_PLATES)
                || s.is(BlockTags.WOODEN_BUTTONS)
                || s.is(BlockTags.BAMBOO_BLOCKS);
    }

    private static boolean isStoneLike(BlockState s) {
        return s.is(BlockTags.BASE_STONE_OVERWORLD) // stone, granite, diorite, andesite, deepslate, tuff
                || s.is(BlockTags.STONE_BRICKS)
                || s.is(Blocks.COBBLESTONE) || s.is(Blocks.MOSSY_COBBLESTONE)
                || s.is(Blocks.GRAVEL) || s.is(Blocks.CLAY)
                || s.is(Blocks.SANDSTONE) || s.is(Blocks.RED_SANDSTONE)
                || s.is(BlockTags.TERRACOTTA);
    }

    /** Scatter fires on flammable/solid surfaces across the blast zone; count scales with radius. */
    private void igniteSurfaces() {
        int fires = radius * 6;
        for (int i = 0; i < fires; i++) {
            double ang = rand.nextDouble() * Math.PI * 2;
            double rr = Math.sqrt(rand.nextDouble()) * radius;
            int dx = (int) Math.round(Math.cos(ang) * rr);
            int dz = (int) Math.round(Math.sin(ang) * rr);
            int dyMax = (int) Math.sqrt(Math.max(0, r2 - dx * dx - dz * dz));
            for (int dy = dyMax; dy >= -dyMax; dy--) {
                BlockPos ground = center.offset(dx, dy, dz);
                BlockState s = level.getBlockState(ground);
                if (s.isAir() || !s.getFluidState().isEmpty()) {
                    continue;
                }
                BlockPos above = ground.above();
                if (level.getBlockState(above).isAir()
                        && (s.isFlammable(level, ground, Direction.UP) || rand.nextFloat() < 0.5F)) {
                    set(above, Blocks.FIRE.defaultBlockState());
                }
                break; // first solid from the top only
            }
        }
    }

    /** Wake the fluids the crater exposed so vanilla spreading floods it back in. */
    private void wakeFluids() {
        for (long packed : fluidWakes) {
            BlockPos p = BlockPos.of(packed);
            FluidState fs = level.getFluidState(p);
            if (!fs.isEmpty()) {
                level.scheduleTick(p, fs.getType(), 5 + rand.nextInt(20));
            }
        }
        fluidWakes.clear();
    }

    // ---------------------------------------------------------------------- effects

    /** Tell every nearby player's client to flash their screen white, brighter the closer they are. */
    private void sendFlash() {
        double cx = center.getX() + 0.5;
        double cy = center.getY() + 0.5;
        double cz = center.getZ() + 0.5;
        double flashDist = 260.0;        // outer edge where the flash fades to nothing
        double fullDist = radius * 1.3;  // pure-white plateau — covers a normal viewing distance
        for (ServerPlayer player : level.players()) {
            double dist = Math.sqrt(player.distanceToSqr(cx, cy, cz));
            if (dist <= flashDist) {
                float peak = (float) Mth.clamp((flashDist - dist) / (flashDist - fullDist), 0.0, 1.0);
                PacketDistributor.sendToPlayer(player, new NukeFlashPayload(peak));
            }
        }
    }

    private void initialBurst() {
        double x = center.getX() + 0.5;
        double y = center.getY() + 0.5;
        double z = center.getZ() + 0.5;

        // Blinding flash + core fireball, broadcast long-distance so they show from a safe distance.
        spawn(ParticleTypes.FLASH, x, y + 2.0, z, 6, 0.0, 0.0, 0.0, 0.0);
        spawn(ParticleTypes.EXPLOSION_EMITTER, x, y + 1.0, z, 3, radius * 0.15, radius * 0.1, radius * 0.15, 0.0);
        spawn(ParticleTypes.EXPLOSION, x, y + 1.0, z, 24, radius * 0.3, radius * 0.2, radius * 0.3, 0.0);

        // A huge, low boom plus a rolling rumble.
        level.playSound(null, center, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 16.0F, 0.35F);
        level.playSound(null, center, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 10.0F, 0.15F);

        sendFlash();
        killEntities();
        spawnRadiationCloud();
    }

    private void killEntities() {
        AABB box = new AABB(center).inflate(radius + 1);
        Vec3 c = center.getCenter();
        double lethal2 = (double) radius * radius;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (e.distanceToSqr(c) <= lethal2) {
                applyRadiation(e, 1200);
                e.setRemainingFireTicks(300);
                e.hurt(TrinityDamageTypes.nuclearBlast(level), 100000.0F);
            }
        }
    }

    private void spawnRadiationCloud() {
        AreaEffectCloud cloud = new AreaEffectCloud(level,
                center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5);
        float cr = Math.min(radius * 0.6F, 30.0F);
        cloud.setRadius(cr);
        cloud.setDuration(6000); // five minutes
        cloud.setRadiusPerTick(-cr / 6000.0F);
        cloud.setWaitTime(0);
        cloud.setParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0x4CDD3A));
        cloud.addEffect(new MobEffectInstance(TrinityEffects.RADIATION, 200, 0));
        cloud.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
        level.addFreshEntity(cloud);
    }

    /** Apply full radiation sickness — the custom effect plus the vanilla "feeling awful" stack. */
    public static void applyRadiation(LivingEntity e, int duration) {
        e.addEffect(new MobEffectInstance(TrinityEffects.RADIATION, duration, 0));
        e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, Math.min(duration, 300), 0));
        e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0));
        e.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, duration, 1));
        e.addEffect(new MobEffectInstance(MobEffects.HUNGER, duration, 0));
    }

    // -------------------------------------------------------------- mushroom cloud

    private void mushroomStep(int age) {
        double x0 = center.getX() + 0.5;
        double y0 = center.getY() + 1.0;
        double z0 = center.getZ() + 0.5;
        double top = Math.min(y0 + age * RISE_PER_TICK, y0 + capHeight);
        double stemH = top - y0;
        SimpleParticleType smoke = TrinityParticles.BIG_SMOKE.get();

        // Sustained blinding flash across the first few ticks.
        if (age < 6) {
            spawn(ParticleTypes.FLASH, x0, y0 + 2.0, z0, 2, 0.5, 0.5, 0.5, 0.0);
        }

        if (age < 50) {
            spawn(ParticleTypes.FLAME, x0, y0 + 0.5, z0, 12, radius * 0.12, 1.2, radius * 0.12, 0.03);
            spawn(ParticleTypes.LAVA, x0, y0 + 0.5, z0, 5, radius * 0.1, 0.6, radius * 0.1, 0.0);
        }

        // Boiling base and the rising stem.
        spawn(smoke, x0, y0 + 0.5, z0, 5, radius * 0.12, 0.5, radius * 0.12, 0.02);
        double midY = y0 + stemH * 0.5;
        double stemJitter = 1.5 + stemH * 0.03;
        spawn(smoke, x0, midY, z0, 5, stemJitter, stemH * 0.25, stemJitter, 0.01);
        spawn(smoke, x0, top, z0, 6, 2.0, 1.0, 2.0, 0.02);

        // Billowing cap.
        double capAge = age - capStartAge;
        if (capAge > 0) {
            double capR = Math.min(capAge * 0.9, capMaxR);
            double capY = y0 + capHeight;
            int arms = 12;
            for (int i = 0; i < arms; i++) {
                double a = (i / (double) arms) * Math.PI * 2 + capAge * 0.04;
                double px = x0 + Math.cos(a) * capR;
                double pz = z0 + Math.sin(a) * capR;
                double dome = Math.sqrt(Math.max(0.0, capMaxR * capMaxR - capR * capR)) * 0.5;
                spawn(smoke, px, capY + dome * 0.35, pz, 2, 1.5, 1.2, 1.5, 0.01);
            }
            spawn(smoke, x0, capY - 1.5, z0, 6, capR * 0.5, 1.2, capR * 0.5, 0.01);
            spawn(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, x0, capY + 3.0, z0, 5, capR * 0.3, 1.0, capR * 0.3, 0.0);
        }
    }
}
