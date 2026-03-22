package io.zicteam.zeconomy.api.model;

public record DailyRewardView(boolean claimed, double zReward, double bReward, int streak) {
}
