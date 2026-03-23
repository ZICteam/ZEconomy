package io.zicteam.zeconomy.system;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

final class DailyRewardState {
    private final Map<UUID, DailyEntry> entries = new HashMap<>();

    int getStreak(UUID playerId) {
        return entries.getOrDefault(playerId, new DailyEntry(-1L, 0)).streak();
    }

    long getLastClaimDay(UUID playerId) {
        return entries.getOrDefault(playerId, new DailyEntry(-1L, 0)).lastClaimDay();
    }

    ExtraEconomyData.DailyClaimResult applyClaim(UUID playerId, long claimDay, int nextStreak, double zReward, double bReward) {
        entries.put(playerId, new DailyEntry(claimDay, nextStreak));
        DataStorageManager.markDirty();
        return new ExtraEconomyData.DailyClaimResult(true, zReward, bReward, nextStreak);
    }

    boolean hasClaimedOnDay(UUID playerId, long claimDay) {
        return getLastClaimDay(playerId) == claimDay;
    }

    void clear() {
        entries.clear();
    }

    void writeTo(CompoundTag root) {
        ListTag daily = new ListTag();
        for (Map.Entry<UUID, DailyEntry> entry : entries.entrySet()) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("uuid", entry.getKey());
            tag.putLong("lastDay", entry.getValue().lastClaimDay());
            tag.putInt("streak", entry.getValue().streak());
            daily.add(tag);
        }
        root.put("daily", daily);
    }

    void readFrom(CompoundTag root) {
        clear();
        for (Tag tag : root.getList("daily", Tag.TAG_COMPOUND)) {
            if (tag instanceof CompoundTag dailyTag) {
                entries.put(
                    dailyTag.getUUID("uuid"),
                    new DailyEntry(dailyTag.getLong("lastDay"), dailyTag.getInt("streak"))
                );
            }
        }
    }

    record DailyEntry(long lastClaimDay, int streak) {
    }
}
