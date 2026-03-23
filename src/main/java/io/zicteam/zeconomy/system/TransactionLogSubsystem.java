package io.zicteam.zeconomy.system;

import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

final class TransactionLogSubsystem {
    private final TransactionLogState state = new TransactionLogState();

    void record(String type, UUID actor, UUID target, String currency, double amount, double fee, String note) {
        state.record(type, actor, target, currency, amount, fee, note);
    }

    List<ExtraEconomyData.TransactionRecord> recent(int limit) {
        return state.recent(limit);
    }

    int size() {
        return state.size();
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
