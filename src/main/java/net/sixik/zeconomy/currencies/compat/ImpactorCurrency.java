package net.sixik.zeconomy.currencies.compat;

import java.util.UUID;
import net.sixik.zeconomy.currencies.BaseCurrency;
import net.sixik.zeconomy.currencies.CurrencySymbol;
import net.sixik.zeconomy.utils.CurrencyHelper;

public class ImpactorCurrency extends BaseCurrency {
    private static final String ID = "impactor_energy";

    public static boolean isLoaded() {
        return false;
    }

    public static String getCurrencyID() {
        return ID;
    }

    public ImpactorCurrency() {
        super(ID, new CurrencySymbol("E"), 0.0);
    }

    public void addCurrency(UUID playerId, double value) {
        CurrencyHelper.getPlayerCurrencyServerData().addCurrencyValue(playerId, ID, value);
    }

    public void setCurrency(UUID playerId, double value) {
        CurrencyHelper.getPlayerCurrencyServerData().setCurrencyValue(playerId, ID, value);
    }

    public double getCurrency(UUID playerId) {
        return CurrencyHelper.getPlayerCurrencyServerData().getBalance(playerId, ID).value;
    }
}
