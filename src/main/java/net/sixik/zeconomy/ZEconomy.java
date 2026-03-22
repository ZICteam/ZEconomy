package net.sixik.zeconomy;

import com.mojang.logging.LogUtils;
import java.util.UUID;
import net.sixik.zeconomy.api.ZEconomyApi;
import net.sixik.zeconomy.api.ZEconomyApiProvider;
import net.sixik.zeconomy.config.GuiLayoutConfig;
import net.sixik.zeconomy.currencies.BaseCurrency;
import net.sixik.zeconomy.currencies.CurrencySymbol;
import net.sixik.zeconomy.currencies.data.CurrencyData;
import net.sixik.zeconomy.network.ZEconomyNetwork;
import net.sixik.zeconomy.system.ExtraEconomyData;
import org.slf4j.Logger;

public final class ZEconomy {
    public static final String MOD_ID = "zeconomy";
    public static final String PRIMARY_CURRENCY_ID = "z_coin";
    public static final String SECONDARY_CURRENCY_ID = "b_coin";
    public static final String PRIMARY_COIN_ITEM_ID = "z_coin_item";
    public static final UUID SERVER_ACCOUNT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final Logger LOGGER = LogUtils.getLogger();
    public static ExtraEconomyData EXTRA_DATA = new ExtraEconomyData();

    private ZEconomy() {
    }

    public static void init() {
        GuiLayoutConfig.reload();
        ZEconomyNetwork.init();
        ZEconomyEvents.init();
        ensureCoreCurrencies();
    }

    public static void ensureCoreCurrencies() {
        if (CurrencyData.SERVER.currencies.stream().noneMatch(c -> c.getName().equals(PRIMARY_CURRENCY_ID))) {
            CurrencyData.SERVER.currencies.add(new BaseCurrency(PRIMARY_CURRENCY_ID, new CurrencySymbol("\u25ce"), 0.0).canDelete(false));
        }
        if (CurrencyData.SERVER.currencies.stream().noneMatch(c -> c.getName().equals(SECONDARY_CURRENCY_ID))) {
            CurrencyData.SERVER.currencies.add(new BaseCurrency(SECONDARY_CURRENCY_ID, new CurrencySymbol("B"), 0.0).canDelete(false));
        }
    }

    public static ZEconomyApi api() {
        return ZEconomyApiProvider.get();
    }

    public static void printStackTrace(String message, Throwable throwable) {
        if (throwable == null) {
            LOGGER.error(message);
            return;
        }
        LOGGER.error(message, throwable);
    }
}
