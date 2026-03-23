package io.zicteam.zeconomy.system;

import io.zicteam.zeconomy.ZEconomy;
import io.zicteam.zeconomy.config.EconomyConfig;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;

final class RateSubsystem {
    private final ExchangeRateState state = new ExchangeRateState();

    void ensureDefaultRates() {
        state.ensureDefaultRate(rateKey(ZEconomy.SECONDARY_CURRENCY_ID, ZEconomy.PRIMARY_CURRENCY_ID), EconomyConfig.DEFAULT_BCOIN_TO_Z_RATE.get());
    }

    double getRate(String from, String to) {
        return state.getRate(rateKey(from, to));
    }

    void setRate(String from, String to, double rate) {
        state.setRate(rateKey(from, to), rate);
    }

    boolean removeRate(String from, String to) {
        return state.removeRate(rateKey(from, to));
    }

    Map<String, Double> getAllRates() {
        return state.getAllRates();
    }

    void clearRates() {
        state.clearRates();
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

    private String rateKey(String from, String to) {
        return from + "->" + to;
    }
}
