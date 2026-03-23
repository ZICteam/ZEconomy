package io.zicteam.zeconomy.system;

import io.zicteam.zeconomy.ZEconomy;

public final class EconomyRateMutationService {
    private EconomyRateMutationService() {
    }

    public static void ensureDefaultRates() {
        ZEconomy.EXTRA_DATA.ensureDefaultRates();
    }

    public static void setRate(String fromCurrencyId, String toCurrencyId, double rate) {
        ZEconomy.EXTRA_DATA.setRate(fromCurrencyId, toCurrencyId, rate);
    }

    public static boolean removeRate(String fromCurrencyId, String toCurrencyId) {
        return ZEconomy.EXTRA_DATA.removeRate(fromCurrencyId, toCurrencyId);
    }

    public static void clearRates() {
        ZEconomy.EXTRA_DATA.clearRates();
    }
}
