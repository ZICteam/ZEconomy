package net.sixik.zeconomy.npc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;

public final class BankNpcHelper {
    public static final String BANK_NPC_TAG = "zeconomy_bank_npc";

    private BankNpcHelper() {
    }

    public static boolean isBankNpc(Entity entity) {
        return entity instanceof Villager && entity.getPersistentData().getBoolean(BANK_NPC_TAG);
    }

    public static void markBankNpc(Villager villager) {
        CompoundTag data = villager.getPersistentData();
        data.putBoolean(BANK_NPC_TAG, true);
        villager.setCustomName(net.minecraft.network.chat.Component.literal("Р вЂР В°Р Р…Р С”Р С‘РЎР‚"));
        villager.setCustomNameVisible(true);
        villager.setPersistenceRequired();
        villager.setNoAi(true);
    }
}
