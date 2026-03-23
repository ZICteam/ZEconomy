package io.zicteam.zeconomy.system;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

final class ExchangeRateState {
    private final Map<String, Double> rates = new HashMap<>();

    void ensureDefaultRate(String key, double rate) {
        rates.putIfAbsent(key, rate);
    }

    double getRate(String key) {
        return rates.getOrDefault(key, 0.0D);
    }

    void setRate(String key, double rate) {
        if (key == null || key.isBlank()) {
            return;
        }
        rates.put(key, Math.max(0.0D, rate));
        DataStorageManager.markDirty();
    }

    boolean removeRate(String key) {
        boolean removed = rates.remove(key) != null;
        if (removed) {
            DataStorageManager.markDirty();
        }
        return removed;
    }

    Map<String, Double> getAllRates() {
        return new HashMap<>(rates);
    }

    void clearRates() {
        if (rates.isEmpty()) {
            return;
        }
        rates.clear();
        DataStorageManager.markDirty();
    }

    void clear() {
        rates.clear();
    }

    void writeTo(CompoundTag root) {
        ListTag ratesTag = new ListTag();
        for (Map.Entry<String, Double> entry : rates.entrySet()) {
            CompoundTag tag = new CompoundTag();
            tag.putString("key", entry.getKey());
            tag.putDouble("rate", entry.getValue());
            ratesTag.add(tag);
        }
        root.put("rates", ratesTag);
    }

    void readFrom(CompoundTag root) {
        clear();
        for (Tag tag : root.getList("rates", Tag.TAG_COMPOUND)) {
            if (tag instanceof CompoundTag rateTag) {
                rates.put(rateTag.getString("key"), rateTag.getDouble("rate"));
            }
        }
    }
}
