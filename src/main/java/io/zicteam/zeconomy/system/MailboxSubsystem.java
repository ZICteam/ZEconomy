package io.zicteam.zeconomy.system;

import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

final class MailboxSubsystem {
    private final MailboxState state = new MailboxState();

    void send(UUID target, ItemStack stack) {
        state.send(target, stack);
    }

    List<ItemStack> claim(UUID playerId) {
        return state.claim(playerId);
    }

    int pendingCount(UUID playerId) {
        return state.pendingCount(playerId);
    }

    void clear() {
        state.clear();
    }

    void writeTo(CompoundTag root) {
        state.writeTo(root);
    }

    void readFrom(CompoundTag root) {
        state.readFrom(root);
    }
}
