package com.trinity.block.entity;

import com.trinity.blast.BlastManager;
import com.trinity.registry.TrinityBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Runs the bomb's fuse. Once {@link #arm() armed}, it counts {@link #FUSE_TICKS} down, beeping and
 * smoking with rising urgency, then detonates: it clears its own block and hands the position off to
 * {@link BlastManager} to run the actual (tick-sliced) explosion.
 */
public class NuclearBombBlockEntity extends BlockEntity {
    /** Length of the fuse, in ticks (four seconds at 20 tps). */
    public static final int FUSE_TICKS = 80;

    /** Ticks remaining on the fuse, or {@code -1} when the bomb is not yet armed. */
    private int fuse = -1;

    public NuclearBombBlockEntity(BlockPos pos, BlockState state) {
        super(TrinityBlockEntities.NUCLEAR_BOMB.get(), pos, state);
    }

    /** Start the fuse if it is not already running. */
    public void arm() {
        if (fuse < 0) {
            fuse = FUSE_TICKS;
            setChanged();
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, NuclearBombBlockEntity be) {
        if (be.fuse < 0) {
            return;
        }

        be.fuse--;
        ServerLevel serverLevel = (ServerLevel) level;

        if (be.fuse <= 0) {
            // Remove ourselves without dropping an item, then let the blast runner take over.
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            BlastManager.detonate(serverLevel, pos);
            return;
        }

        // Warning: beep and smoke with accelerating urgency as the fuse runs out.
        int interval = be.fuse > 40 ? 10 : be.fuse > 20 ? 5 : 2;
        if (be.fuse % interval == 0) {
            float pitch = 1.0F + (FUSE_TICKS - be.fuse) / (float) FUSE_TICKS;
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 1.0F, pitch);
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 4, 0.15, 0.05, 0.15, 0.01);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Fuse", fuse);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fuse = tag.contains("Fuse") ? tag.getInt("Fuse") : -1;
    }
}
