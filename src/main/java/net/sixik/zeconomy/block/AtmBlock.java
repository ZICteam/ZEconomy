package net.sixik.zeconomy.block;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.sixik.zeconomy.config.EconomyConfig;
import net.sixik.zeconomy.menu.AtmMenu;

public class AtmBlock extends Block {
    public AtmBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, net.minecraft.core.BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!EconomyConfig.ENABLE_PHYSICAL_MONEY.get()) {
            player.sendSystemMessage(Component.translatable("message.zeconomy.atm.disabled").withStyle(ChatFormatting.YELLOW));
            return InteractionResult.CONSUME;
        }
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new AtmMenu(id, inv),
                Component.translatable("screen.zeconomy.atm.title")
            ));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.SUCCESS;
    }
}
