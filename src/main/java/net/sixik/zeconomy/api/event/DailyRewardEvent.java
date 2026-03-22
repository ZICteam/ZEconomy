package net.sixik.zeconomy.api.event;

import java.util.UUID;

public final class DailyRewardEvent extends ZEconomyApiEvent {
    private final UUID playerId;
    private final double zReward;
    private final double bReward;
    private final int streak;

    public DailyRewardEvent(UUID playerId, double zReward, double bReward, int streak) {
        this.playerId = playerId;
        this.zReward = zReward;
        this.bReward = bReward;
        this.streak = streak;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public double getZReward() {
        return zReward;
    }

    public double getBReward() {
        return bReward;
    }

    public int getStreak() {
        return streak;
    }
}
