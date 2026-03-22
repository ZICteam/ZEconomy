package io.zicteam.zeconomy.commands;

import io.zicteam.zeconomy.ZEconomy;
import io.zicteam.zeconomy.config.EconomyConfig;
import io.zicteam.zeconomy.system.EconomyOperationService;
import io.zicteam.zeconomy.system.ExtraEconomyData;
import io.zicteam.zeconomy.utils.CurrencyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

final class UserEconomyCommands {
    private UserEconomyCommands() {
    }

    static int showBalance(CommandSourceStack source, ServerPlayer player) {
        double sdm = CurrencyHelper.getPlayerCurrencyServerData().getBalance(player, "z_coin").value;
        double b = CurrencyHelper.getPlayerCurrencyServerData().getBalance(player, "b_coin").value;
        source.sendSuccess(() -> Component.literal(player.getName().getString() + " wallet: z_coin=" + String.format("%.2f", sdm) + ", b_coin=" + String.format("%.2f", b)), false);
        source.sendSuccess(() -> Component.literal("bank: z=" + String.format("%.2f", ZEconomy.EXTRA_DATA.getDeposited(player.getUUID(), "z_coin")) + ", b=" + String.format("%.2f", ZEconomy.EXTRA_DATA.getDeposited(player.getUUID(), "b_coin"))), false);
        source.sendSuccess(() -> Component.literal("vault: z=" + String.format("%.2f", ZEconomy.EXTRA_DATA.getVaultBalance(player.getUUID(), "z_coin")) + ", b=" + String.format("%.2f", ZEconomy.EXTRA_DATA.getVaultBalance(player.getUUID(), "b_coin"))), false);
        source.sendSuccess(() -> Component.literal("daily streak: " + ZEconomy.EXTRA_DATA.getDailyStreak(player.getUUID()) + " | mail: " + ZEconomy.EXTRA_DATA.pendingMailCount(player.getUUID())), false);
        return 1;
    }

    static int pay(ServerPlayer from, ServerPlayer to, String currency, double amount) {
        boolean ok = EconomyOperationService.transfer(from, to, currency, amount);
        if (!ok) {
            from.sendSystemMessage(Component.literal("Transfer failed: insufficient funds or invalid params").withStyle(ChatFormatting.RED));
            return 0;
        }
        double fee = amount * EconomyConfig.TRANSFER_FEE_RATE.get();
        from.sendSystemMessage(Component.literal("Sent " + amount + " " + currency + " to " + to.getName().getString() + " (fee " + String.format("%.2f", fee) + ")"));
        to.sendSystemMessage(Component.literal("Received " + amount + " " + currency + " from " + from.getName().getString()).withStyle(ChatFormatting.GREEN));
        return 1;
    }

    static int bankDeposit(ServerPlayer player, String currency, double amount) {
        if (!EconomyOperationService.depositBank(player, currency, amount)) {
            player.sendSystemMessage(Component.literal("Bank deposit failed").withStyle(ChatFormatting.RED));
            return 0;
        }
        player.sendSystemMessage(Component.literal("Bank deposit: " + amount + " " + currency));
        return 1;
    }

    static int bankWithdraw(ServerPlayer player, String currency, double amount) {
        if (!EconomyOperationService.withdrawBank(player, currency, amount)) {
            player.sendSystemMessage(Component.literal("Bank withdraw failed").withStyle(ChatFormatting.RED));
            return 0;
        }
        player.sendSystemMessage(Component.literal("Bank withdraw: " + amount + " " + currency));
        return 1;
    }

    static int bankInfo(ServerPlayer player) {
        EconomyCommands.MapToLines.print(player, "Bank balances", ZEconomy.EXTRA_DATA.getAllDeposits(player.getUUID()));
        player.sendSystemMessage(Component.literal("Hourly interest: " + (EconomyConfig.HOURLY_INTEREST_RATE.get() * 100.0) + "%"));
        return 1;
    }

    static int exchangeCurrency(ServerPlayer player, String from, String to, double amount) {
        if (!EconomyOperationService.exchange(player, from, to, amount)) {
            player.sendSystemMessage(Component.literal("Exchange failed (rate/balance/config)").withStyle(ChatFormatting.RED));
            return 0;
        }
        double rate = ZEconomy.EXTRA_DATA.getRate(from, to);
        player.sendSystemMessage(Component.literal("Exchanged " + amount + " " + from + " -> " + String.format("%.2f", amount * rate) + " " + to + " (before fee)"));
        return 1;
    }

    static int setRate(CommandSourceStack source, String from, String to, double rate) {
        ZEconomy.EXTRA_DATA.setRate(from, to, rate);
        source.sendSuccess(() -> Component.literal("Rate set: " + from + " -> " + to + " = " + rate), true);
        return 1;
    }

    static int setRatePair(CommandSourceStack source, String from, String to, double rate) {
        ZEconomy.EXTRA_DATA.setRate(from, to, rate);
        double reverse = 1.0D / rate;
        ZEconomy.EXTRA_DATA.setRate(to, from, reverse);
        source.sendSuccess(() -> Component.translatable(
            "message.zeconomy.exchange.rate_pair_set",
            from,
            to,
            String.format("%.4f", rate),
            to,
            from,
            String.format("%.4f", reverse)
        ), true);
        return 1;
    }

    static int mailClaim(ServerPlayer player) {
        java.util.List<net.minecraft.world.item.ItemStack> list = ZEconomy.EXTRA_DATA.claimMail(player.getUUID());
        if (list.isEmpty()) {
            player.sendSystemMessage(Component.literal("Mailbox is empty").withStyle(ChatFormatting.GRAY));
            return 1;
        }
        for (net.minecraft.world.item.ItemStack stack : list) {
            io.zicteam.zeconomy.util.InventoryUtils.giveItem(player, stack);
        }
        CurrencyHelper.refreshPlayerState(player);
        player.sendSystemMessage(Component.literal("Claimed " + list.size() + " mail item stack(s)").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    static int mailSend(ServerPlayer player, ServerPlayer target) {
        if (!EconomyConfig.ENABLE_MAILBOX.get()) {
            player.sendSystemMessage(Component.translatable("message.zeconomy.mailbox.disabled").withStyle(ChatFormatting.YELLOW));
            return 0;
        }
        if (player.getUUID().equals(target.getUUID())) {
            player.sendSystemMessage(Component.translatable("message.zeconomy.mailbox.self_send").withStyle(ChatFormatting.YELLOW));
            return 0;
        }
        net.minecraft.world.item.ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.zeconomy.mailbox.empty_hand").withStyle(ChatFormatting.RED));
            return 0;
        }
        net.minecraft.world.item.ItemStack mailStack = held.copy();
        ZEconomy.EXTRA_DATA.sendMail(target.getUUID(), mailStack);
        held.setCount(0);
        CurrencyHelper.refreshPlayersState(target, player);
        player.sendSystemMessage(Component.translatable("message.zeconomy.mailbox.command_sent", mailStack.getCount(), mailStack.getHoverName(), target.getName()).withStyle(ChatFormatting.GREEN));
        target.sendSystemMessage(Component.translatable("message.zeconomy.mailbox.received", player.getName().getString()));
        return 1;
    }

    static int vaultSetPin(ServerPlayer player, String pin) {
        if (!ZEconomy.EXTRA_DATA.setVaultPin(player.getUUID(), pin)) {
            player.sendSystemMessage(Component.literal("PIN must be 4-12 digits").withStyle(ChatFormatting.RED));
            return 0;
        }
        CurrencyHelper.refreshPlayerState(player);
        player.sendSystemMessage(Component.literal("Vault PIN updated").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    static int vaultDeposit(ServerPlayer player, String pin, String currency, double amount) {
        if (!EconomyOperationService.depositVault(player, pin, currency, amount)) {
            player.sendSystemMessage(Component.literal("Vault deposit failed (check PIN/balance)").withStyle(ChatFormatting.RED));
            return 0;
        }
        player.sendSystemMessage(Component.literal("Vault deposit: " + amount + " " + currency));
        return 1;
    }

    static int vaultWithdraw(ServerPlayer player, String pin, String currency, double amount) {
        if (!EconomyOperationService.withdrawVault(player, pin, currency, amount)) {
            player.sendSystemMessage(Component.literal("Vault withdraw failed (check PIN/balance)").withStyle(ChatFormatting.RED));
            return 0;
        }
        player.sendSystemMessage(Component.literal("Vault withdraw: " + amount + " " + currency));
        return 1;
    }

    static int vaultBalance(ServerPlayer player) {
        EconomyCommands.MapToLines.print(player, "Vault balances", ZEconomy.EXTRA_DATA.getAllVaultBalances(player.getUUID()));
        return 1;
    }

    static int dailyClaim(ServerPlayer player) {
        ExtraEconomyData.DailyClaimResult result = EconomyOperationService.claimDaily(player);
        if (!result.success()) {
            player.sendSystemMessage(Component.literal("Daily reward already claimed today").withStyle(ChatFormatting.YELLOW));
            return 0;
        }
        player.sendSystemMessage(Component.literal("Daily reward: +" + String.format("%.2f", result.zReward()) + " z_coin, +" + String.format("%.2f", result.bReward()) + " b_coin | streak " + result.streak()).withStyle(ChatFormatting.GREEN));
        return 1;
    }

    static int showTop(CommandSourceStack source, String currency) {
        if (source.getServer() == null) {
            return 0;
        }
        java.util.List<ExtraEconomyData.RichEntry> top = ZEconomy.EXTRA_DATA.getTopRich(source.getServer(), currency, 10, true);
        source.sendSuccess(() -> Component.literal("Top rich (" + currency + "):"), false);
        int i = 1;
        for (ExtraEconomyData.RichEntry e : top) {
            int rank = i++;
            source.sendSuccess(() -> Component.literal(rank + ". " + e.playerName() + " = " + String.format("%.2f", e.amount())), false);
        }
        return 1;
    }

    static int showCurrencies(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("message.zeconomy.currencies.header"), false);
        if (io.zicteam.zeconomy.currencies.data.CurrencyData.SERVER.currencies.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("message.zeconomy.currencies.empty"), false);
            return 1;
        }
        for (io.zicteam.zeconomy.currencies.BaseCurrency currency : io.zicteam.zeconomy.currencies.data.CurrencyData.SERVER.currencies) {
            String symbol = currency.symbol == null ? "?" : currency.symbol.value;
            source.sendSuccess(() -> Component.translatable(
                "message.zeconomy.currencies.line",
                currency.getName(),
                symbol,
                String.format("%.2f", currency.getDefaultValue()),
                currency.canDelete ? "yes" : "no"
            ), false);
        }
        return 1;
    }

    static int showRate(CommandSourceStack source, String from, String to) {
        double rate = ZEconomy.EXTRA_DATA.getRate(from, to);
        if (rate <= 0.0D) {
            source.sendFailure(Component.translatable("message.zeconomy.exchange.rate_missing", from, to));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("message.zeconomy.exchange.rate_value", from, to, String.format("%.4f", rate)), false);
        return 1;
    }

    static int showRates(CommandSourceStack source) {
        java.util.Map<String, Double> rates = ZEconomy.EXTRA_DATA.getAllRates();
        source.sendSuccess(() -> Component.translatable("message.zeconomy.exchange.rates_header"), false);
        if (rates.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("message.zeconomy.exchange.rates_empty"), false);
            return 1;
        }
        rates.entrySet().stream()
            .sorted(java.util.Map.Entry.comparingByKey())
            .forEach(entry -> source.sendSuccess(
                () -> Component.translatable("message.zeconomy.exchange.rates_line", entry.getKey(), String.format("%.4f", entry.getValue())),
                false
            ));
        return 1;
    }

    static int clearRate(CommandSourceStack source, String from, String to) {
        boolean removed = ZEconomy.EXTRA_DATA.removeRate(from, to);
        if (!removed) {
            source.sendFailure(Component.translatable("message.zeconomy.exchange.rate_missing", from, to));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("message.zeconomy.exchange.rate_cleared", from, to), true);
        return 1;
    }

    static int auditRates(CommandSourceStack source) {
        java.util.Map<String, Double> rates = ZEconomy.EXTRA_DATA.getAllRates();
        java.util.Set<String> currencies = io.zicteam.zeconomy.currencies.data.CurrencyData.SERVER.currencies.stream()
            .map(io.zicteam.zeconomy.currencies.BaseCurrency::getName)
            .collect(java.util.stream.Collectors.toSet());
        int valid = 0;
        int invalid = 0;
        source.sendSuccess(() -> Component.translatable("message.zeconomy.exchange.audit_header"), false);
        if (rates.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("message.zeconomy.exchange.rates_empty"), false);
            return 1;
        }
        for (java.util.Map.Entry<String, Double> entry : rates.entrySet()) {
            String key = entry.getKey();
            String[] parts = key.split("->", 2);
            boolean ok = parts.length == 2
                && currencies.contains(parts[0])
                && currencies.contains(parts[1])
                && entry.getValue() > 0.0D;
            if (ok) {
                valid++;
            } else {
                invalid++;
                source.sendSuccess(() -> Component.translatable("message.zeconomy.exchange.audit_invalid", key, String.format("%.4f", entry.getValue())), false);
            }
        }
        final int validCount = valid;
        final int invalidCount = invalid;
        source.sendSuccess(() -> Component.translatable("message.zeconomy.exchange.audit_summary", validCount, invalidCount), false);
        return invalid == 0 ? 1 : 0;
    }

    static int resetRates(CommandSourceStack source) {
        ZEconomy.EXTRA_DATA.clearRates();
        ZEconomy.EXTRA_DATA.ensureDefaultRates();
        source.sendSuccess(() -> Component.translatable(
            "message.zeconomy.exchange.rates_reset",
            ZEconomy.EXTRA_DATA.getAllRates().size()
        ), true);
        return 1;
    }

    static int showHelp(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("message.zeconomy.help.header"), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.help.line.balance"), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.help.line.currencies"), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.help.line.pay"), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.help.line.bank"), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.help.line.exchange"), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.help.line.exchange_admin"), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.help.line.mail"), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.help.line.vault"), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.help.line.daily"), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.help.line.top"), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.help.line.status"), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.help.line.admin_currency"), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.help.line.admin_currency_lock"), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.help.line.admin_currency_lockall"), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.help.line.admin_currency_inspect"), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.help.line.admin_currency_players"), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.help.line.admin_currency_repair"), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.help.line.admin_currency_issues"), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.help.line.admin_currency_summary"), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.help.line.admin_currency_repairissues"), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.help.line.admin_currency_audit"), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.help.line.admin_status"), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.help.line.admin_save"), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.help.line.admin_backup"), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.help.line.admin_reloadstorage"), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.help.line.admin_exportnow"), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.help.line.admin_exportstatus"), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.help.line.admin_doctor"), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.help.line.admin_doctorfix"), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.help.line.admin_doctorfixall"), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.help.line.admin_inspect"), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.help.line.admin_reconcile"), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.help.line.admin_syncvault"), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.help.line.reload"), false);
        return 1;
    }

    static int showStatus(CommandSourceStack source) {
        if (source.getServer() == null) {
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("message.zeconomy.status.header"), false);
        source.sendSuccess(() -> Component.translatable(
            "message.zeconomy.status.general",
            EconomyConfig.STORAGE_MODE.get(),
            EconomyConfig.ENABLE_MAILBOX.get() ? "on" : "off",
            EconomyConfig.ENABLE_PHYSICAL_MONEY.get() ? "on" : "off",
            EconomyConfig.ENABLE_BCOIN_EXCHANGE.get() ? "on" : "off"
        ), false);
        source.sendSuccess(() -> Component.translatable(
            "message.zeconomy.status.rules",
            String.format("%.2f", EconomyConfig.TRANSFER_FEE_RATE.get() * 100.0D),
            String.format("%.2f", EconomyConfig.EXCHANGE_FEE_RATE.get() * 100.0D),
            String.format("%.2f", EconomyConfig.HOURLY_INTEREST_RATE.get() * 100.0D),
            EconomyConfig.EXPORT_INTERVAL_SECONDS.get()
        ), false);
        source.sendSuccess(() -> Component.translatable(
            "message.zeconomy.status.compat",
            EconomyConfig.ENABLE_VAULT_BRIDGE.get() ? "on" : "off",
            EconomyConfig.VAULT_SYNC_CURRENCY_ID.get(),
            EconomyConfig.VAULT_PULL_ON_JOIN.get() ? "on" : "off",
            EconomyConfig.VAULT_PUSH_ON_CHANGE.get() ? "on" : "off"
        ), false);
        source.sendSuccess(() -> Component.translatable(
            "message.zeconomy.status.export",
            CurrencyHelper.exportJsonPath(source.getServer())
        ), false);
        return 1;
    }
}
