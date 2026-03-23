package io.zicteam.zeconomy.system;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

final class TransactionLogState {
    private final LinkedList<ExtraEconomyData.TransactionRecord> records = new LinkedList<>();

    void record(String type, UUID actor, UUID target, String currency, double amount, double fee, String note) {
        records.add(new ExtraEconomyData.TransactionRecord(Instant.now().getEpochSecond(), type, actor, target, currency, amount, fee, note));
        while (records.size() > ExtraEconomyData.MAX_LOGS) {
            records.removeFirst();
        }
    }

    List<ExtraEconomyData.TransactionRecord> recent(int limit) {
        int n = Math.max(1, limit);
        int start = Math.max(0, records.size() - n);
        return new ArrayList<>(records.subList(start, records.size()));
    }

    int size() {
        return records.size();
    }

    void clear() {
        records.clear();
    }

    void writeTo(CompoundTag root) {
        ListTag logsTag = new ListTag();
        for (ExtraEconomyData.TransactionRecord record : records) {
            CompoundTag tag = new CompoundTag();
            tag.putLong("time", record.epochSec());
            tag.putString("type", record.type());
            if (record.actor() != null) {
                tag.putUUID("actor", record.actor());
            }
            if (record.target() != null) {
                tag.putUUID("target", record.target());
            }
            tag.putString("currency", record.currency());
            tag.putDouble("amount", record.amount());
            tag.putDouble("fee", record.fee());
            tag.putString("note", record.note());
            logsTag.add(tag);
        }
        root.put("logs", logsTag);
    }

    void readFrom(CompoundTag root) {
        clear();
        for (Tag tag : root.getList("logs", Tag.TAG_COMPOUND)) {
            if (!(tag instanceof CompoundTag logTag)) {
                continue;
            }
            records.add(new ExtraEconomyData.TransactionRecord(
                logTag.getLong("time"),
                logTag.getString("type"),
                logTag.hasUUID("actor") ? logTag.getUUID("actor") : null,
                logTag.hasUUID("target") ? logTag.getUUID("target") : null,
                logTag.getString("currency"),
                logTag.getDouble("amount"),
                logTag.getDouble("fee"),
                logTag.getString("note")
            ));
        }
    }
}
