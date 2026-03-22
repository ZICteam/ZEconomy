package io.zicteam.zeconomy.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import io.zicteam.zeconomy.npc.BankNpcHelper;

public class BankNpcSpawnerItem extends Item {
    public BankNpcSpawnerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }

        BlockPos spawnPos = context.getClickedPos().relative(context.getClickedFace());
        Villager villager = EntityType.VILLAGER.create(serverLevel);
        if (villager == null) {
            return InteractionResult.FAIL;
        }

        villager.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, 0.0F, 0.0F);
        BankNpcHelper.markBankNpc(villager);
        serverLevel.addFreshEntity(villager);

        if (!player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        player.sendSystemMessage(Component.literal("Р‘Р°РЅРєРёСЂ СѓСЃС‚Р°РЅРѕРІР»РµРЅ").withStyle(ChatFormatting.GREEN));
        return InteractionResult.CONSUME;
    }
}
