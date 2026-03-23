package io.zicteam.zeconomy.currencies.compat;

import java.util.UUID;
import io.zicteam.zeconomy.currencies.BaseCurrency;
import io.zicteam.zeconomy.currencies.CurrencySymbol;
import io.zicteam.zeconomy.system.EconomyOperationService;
import io.zicteam.zeconomy.utils.CurrencyHelper;

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
        EconomyOperationService.addBalance(playerId, ID, value);
    }

    public void setCurrency(UUID playerId, double value) {
        EconomyOperationService.setBalance(playerId, ID, value);
    }

    public double getCurrency(UUID playerId) {
        return CurrencyHelper.getPlayerCurrencyServerData().getBalance(playerId, ID).value;
    }
}
