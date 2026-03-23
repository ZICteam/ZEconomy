package io.zicteam.zeconomy.system;

import io.zicteam.zeconomy.ZEconomy;
import java.util.UUID;

public final class EconomyLogService {
    private EconomyLogService() {
    }

    public static void record(String type, UUID actor, UUID target, String currency, double amount, double fee, String note) {
        ZEconomy.EXTRA_DATA.recordTransaction(type, actor, target, currency, amount, fee, note);
    }
}
