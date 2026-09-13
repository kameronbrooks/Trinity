package com.trinity.block;

import com.trinity.block.entity.NuclearBombBlockEntity;
import com.trinity.registry.TrinityBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The nuclear bomb. It sits inert until <em>armed</em>, which — like vanilla TNT — happens when a
 * player right-clicks it with flint and steel or a fire charge, or when it receives a redstone
 * signal. Arming lights the block ({@link #ARMED}) and starts the fuse countdown handled by its
 * {@link NuclearBombBlockEntity}; when the fuse expires the block entity hands off to the blast
 * runner and removes the block.
 */
public class NuclearBombBlock extends BaseEntityBlock {
    public static final MapCodec<NuclearBombBlock> CODEC = simpleCodec(NuclearBombBlock::new);
    public static final BooleanProperty ARMED = BlockStateProperties.LIT;

    public NuclearBombBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ARMED, Boolean.FALSE));
    }

    @Override
    protected MapCodec<NuclearBombBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ARMED);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NuclearBombBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, TrinityBlockEntities.NUCLEAR_BOMB.get(), NuclearBombBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE)) {
            if (!level.isClientSide) {
                prime(level, pos, state);
                if (stack.is(Items.FLINT_AND_STEEL)) {
                    stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
                } else {
                    stack.consume(1, player);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(state.getBlock()) && !level.isClientSide && level.hasNeighborSignal(pos)) {
            prime(level, pos, state);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide && level.hasNeighborSignal(pos)) {
            prime(level, pos, state);
        }
    }

    /** Arm the bomb (if not already armed) and kick off its fuse. */
    private static void prime(Level level, BlockPos pos, BlockState state) {
        if (state.getValue(ARMED)) {
            return;
        }
        level.setBlock(pos, state.setValue(ARMED, Boolean.TRUE), Block.UPDATE_CLIENTS);
        if (level.getBlockEntity(pos) instanceof NuclearBombBlockEntity bomb) {
            bomb.arm();
        }
        level.playSound(null, pos, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.5F, 0.6F);
    }
}
