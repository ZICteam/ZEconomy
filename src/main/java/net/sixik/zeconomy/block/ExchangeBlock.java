package net.sixik.zeconomy.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import net.sixik.zeconomy.block.entity.ExchangeBlockEntity;
import net.sixik.zeconomy.menu.ExchangeMenu;

public class ExchangeBlock extends BaseEntityBlock implements EntityBlock {
    public ExchangeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ExchangeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ExchangeBlockEntity exchange)) {
            return InteractionResult.PASS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }

        if (exchange.getOwner() == null) {
            exchange.setOwner(player.getUUID());
            openSetupMenu(serverPlayer, pos);
            return InteractionResult.CONSUME;
        }
        if (exchange.getOwner().equals(player.getUUID())) {
            openSetupMenu(serverPlayer, pos);
            return InteractionResult.CONSUME;
        }
        openSetupMenu(serverPlayer, pos);
        return InteractionResult.CONSUME;
    }

    private void openSetupMenu(ServerPlayer player, BlockPos pos) {
        NetworkHooks.openScreen(
            player,
            new SimpleMenuProvider((id, inv, p) -> new ExchangeMenu(id, inv, pos), Component.translatable("screen.zeconomy.exchange.title")),
            pos
        );
    }
}
