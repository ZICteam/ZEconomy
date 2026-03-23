package io.zicteam.zeconomy.system;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

final class DailyRewardFacade {
    private final DailyRewardState state = new DailyRewardState();

    int getStreak(UUID playerId) {
        return state.getStreak(playerId);
    }

    long getLastClaimDay(UUID playerId) {
        return state.getLastClaimDay(playerId);
    }

    ExtraEconomyData.DailyClaimResult applyClaim(UUID playerId, long claimDay, int nextStreak, double zReward, double bReward) {
        return state.applyClaim(playerId, claimDay, nextStreak, zReward, bReward);
    }

    boolean hasClaimedToday(UUID playerId) {
        return state.hasClaimedOnDay(playerId, LocalDate.now(ZoneOffset.UTC).toEpochDay());
    }

    void clear() {
        state.clear();
    }

    void readFrom(net.minecraft.nbt.CompoundTag root) {
        state.readFrom(root);
    }

    void writeTo(net.minecraft.nbt.CompoundTag root) {
        state.writeTo(root);
    }
}
