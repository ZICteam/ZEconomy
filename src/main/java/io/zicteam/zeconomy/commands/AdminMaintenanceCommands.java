package io.zicteam.zeconomy.commands;

import io.zicteam.zeconomy.ZEconomy;
import io.zicteam.zeconomy.config.EconomyConfig;
import io.zicteam.zeconomy.system.AdminOperationService;
import io.zicteam.zeconomy.system.AdminReportService;
import io.zicteam.zeconomy.utils.CurrencyHelper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

final class AdminMaintenanceCommands {
    private AdminMaintenanceCommands() {
    }

    static int adminStatus(CommandSourceStack source) {
        if (source.getServer() == null) {
            return 0;
        }
        AdminReportService.StatusReport report = AdminReportService.status(source.getServer());
        source.sendSuccess(() -> Component.translatable("message.zeconomy.admin.status.header"), false);
        source.sendSuccess(() -> Component.translatable(
            "message.zeconomy.admin.status.storage",
            report.storageMode(),
            report.storageTarget()
        ), false);
        source.sendSuccess(() -> Component.translatable(
            "message.zeconomy.admin.status.files",
            report.currencyPath(),
            report.playerPath(),
            report.customPath(),
            report.extraPath()
        ), false);
        source.sendSuccess(() -> Component.translatable(
            "message.zeconomy.admin.status.vault",
            report.vaultToggle(),
            report.vaultAvailability(),
            report.vaultProvider(),
            report.vaultSyncCurrency()
        ), false);
        source.sendSuccess(() -> Component.translatable(
            "message.zeconomy.admin.status.runtime",
            report.onlinePlayers(),
            report.pendingVaultSyncs(),
            report.exchangeRates(),
            report.currencyCount()
        ), false);
        return 1;
    }

    static int adminSave(CommandSourceStack source) {
        if (source.getServer() == null) {
            return 0;
        }
        CurrencyHelper.saveAll(source.getServer());
        source.sendSuccess(() -> Component.translatable("message.zeconomy.admin.save.success", AdminReportService.storageTarget(source.getServer())), true);
        return 1;
    }

    static int adminBackup(CommandSourceStack source) {
        if (source.getServer() == null) {
            return 0;
        }
        CurrencyHelper.saveAll(source.getServer());
        String mode = EconomyConfig.STORAGE_MODE.get();
        if ("mysql".equalsIgnoreCase(mode)) {
            source.sendFailure(Component.translatable(
                "message.zeconomy.admin.backup.mysql",
                EconomyConfig.MYSQL_HOST.get(),
                EconomyConfig.MYSQL_PORT.get(),
                EconomyConfig.MYSQL_DATABASE.get(),
                EconomyConfig.MYSQL_TABLE.get()
            ));
            return 0;
        }
        String stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(java.time.ZonedDateTime.now());
        Path backupRoot = source.getServer().getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve("zeconomy").resolve("backups").resolve(stamp);
        try {
            Files.createDirectories(backupRoot);
            int copied = 0;
            if ("json".equalsIgnoreCase(mode)) {
                copied += copyIfExists(source.getServer().getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve("zeconomy").resolve(EconomyConfig.JSON_FILE_NAME.get()), backupRoot);
            } else if ("sqlite".equalsIgnoreCase(mode)) {
                copied += copyIfExists(source.getServer().getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve("zeconomy").resolve(EconomyConfig.SQLITE_FILE_NAME.get()), backupRoot);
            } else {
                copied += copyIfExists(CurrencyHelper.currencyDataPath(source.getServer()), backupRoot);
                copied += copyIfExists(CurrencyHelper.playerDataPath(source.getServer()), backupRoot);
                copied += copyIfExists(CurrencyHelper.customDataPath(source.getServer()), backupRoot);
                copied += copyIfExists(CurrencyHelper.extraDataPath(source.getServer()), backupRoot);
            }
            final int copiedFiles = copied;
            source.sendSuccess(() -> Component.translatable("message.zeconomy.admin.backup.success", backupRoot, copiedFiles), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("message.zeconomy.admin.backup.failed", e.getClass().getSimpleName(), e.getMessage() == null ? "" : e.getMessage()));
            return 0;
        }
    }

    static int adminReloadStorage(CommandSourceStack source) {
        if (source.getServer() == null) {
            return 0;
        }
        int onlinePlayers = AdminOperationService.reloadStorage(source.getServer());
        source.sendSuccess(() -> Component.translatable(
            "message.zeconomy.admin.reloadstorage.success",
            EconomyConfig.STORAGE_MODE.get(),
            onlinePlayers
        ), true);
        return 1;
    }

    static int adminExportNow(CommandSourceStack source) {
        if (source.getServer() == null) {
            return 0;
        }
        ZEconomy.EXTRA_DATA.exportJson(source.getServer(), CurrencyHelper.exportJsonPath(source.getServer()));
        source.sendSuccess(() -> Component.translatable("message.zeconomy.admin.exportnow.success", CurrencyHelper.exportJsonPath(source.getServer())), true);
        return 1;
    }

    static int adminExportStatus(CommandSourceStack source) {
        if (source.getServer() == null) {
            return 0;
        }
        AdminReportService.ExportStatusReport report = AdminReportService.exportStatus(source.getServer());
        source.sendSuccess(() -> Component.translatable(
            "message.zeconomy.admin.exportstatus",
            report.exportPath(),
            report.exists(),
            report.sizeBytes(),
            report.modifiedAt(),
            report.intervalSeconds()
        ), false);
        return 1;
    }

    static int adminDoctor(CommandSourceStack source) {
        if (source.getServer() == null) {
            return 0;
        }
        AdminReportService.DoctorReport report = AdminReportService.doctor(source.getServer());
        source.sendSuccess(() -> Component.translatable("message.zeconomy.admin.doctor.header"), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.admin.doctor.currencies", report.zCoinStatus(), report.bCoinStatus()), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.admin.doctor.storage", report.storageMode(), report.storageHealth()), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.admin.doctor.files", report.currencyFile(), report.playerFile(), report.customFile(), report.extraFile()), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.admin.doctor.vault", report.vaultHealth(), report.pendingVaultSyncs(), report.vaultProvider()), false);
        return 1;
    }

    static int adminInspect(CommandSourceStack source, ServerPlayer player) {
        AdminReportService.PlayerInspectReport report = AdminReportService.inspect(player);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.admin.inspect.header", report.playerName(), report.playerId()), false);
        source.sendSuccess(() -> Component.translatable(
            "message.zeconomy.admin.inspect.summary",
            report.pendingMail(),
            report.dailyStreak(),
            report.hasVaultPin(),
            report.syncCurrency(),
            String.format("%.2f", report.syncBalance())
        ), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.admin.inspect.wallet"), false);
        for (AdminReportService.PlayerWalletLine currency : report.wallet()) {
            source.sendSuccess(() -> Component.translatable(
                "message.zeconomy.admin.inspect.wallet_line",
                currency.currencyId(),
                String.format("%.2f", currency.balance()),
                currency.locked()
            ), false);
        }
        source.sendSuccess(() -> Component.translatable("message.zeconomy.admin.inspect.bank"), false);
        printSourceMap(source, report.bankBalances());
        source.sendSuccess(() -> Component.translatable("message.zeconomy.admin.inspect.vault"), false);
        printSourceMap(source, report.vaultBalances());
        source.sendSuccess(() -> Component.translatable(
            "message.zeconomy.admin.inspect.mirror",
            String.format("%.2f", report.mirrorBankZ()),
            String.format("%.2f", report.mirrorBankB()),
            String.format("%.2f", report.mirrorVaultZ()),
            String.format("%.2f", report.mirrorVaultB()),
            report.mirrorMailPending(),
            report.mirrorDailyStreak()
        ), false);
        return 1;
    }

    static int adminReconcile(CommandSourceStack source, ServerPlayer player) {
        AdminOperationService.reconcilePlayer(player, source.getServer() != null);
        String syncCurrency = EconomyConfig.VAULT_SYNC_CURRENCY_ID.get();
        double syncBalance = CurrencyHelper.getPlayerCurrencyServerData().getBalance(player, syncCurrency).value;
        source.sendSuccess(() -> Component.translatable(
            "message.zeconomy.admin.reconcile.success",
            player.getName().getString(),
            io.zicteam.zeconomy.currencies.data.CurrencyPlayerData.SERVER.getPlayersCurrency(player).size(),
            ZEconomy.EXTRA_DATA.pendingMailCount(player.getUUID()),
            syncCurrency,
            String.format("%.2f", syncBalance)
        ), true);
        return 1;
    }

    static int adminDoctorFix(CommandSourceStack source, ServerPlayer player) {
        AdminOperationService.DoctorFixResult result = AdminOperationService.doctorFixPlayer(player, false);
        if (source.getServer() != null) {
            CurrencyHelper.saveAll(source.getServer());
        }
        String syncCurrency = EconomyConfig.VAULT_SYNC_CURRENCY_ID.get();
        double syncBalance = CurrencyHelper.getPlayerCurrencyServerData().getBalance(player, syncCurrency).value;
        source.sendSuccess(() -> Component.translatable(
            "message.zeconomy.admin.doctorfix.success",
            player.getName().getString(),
            io.zicteam.zeconomy.currencies.data.CurrencyPlayerData.SERVER.getPlayersCurrency(player).size(),
            ZEconomy.EXTRA_DATA.pendingMailCount(player.getUUID()),
            result.vaultAttempted() ? "yes" : "no",
            result.vaultOk() ? "ok" : "skip",
            syncCurrency,
            String.format("%.2f", syncBalance)
        ), true);
        return 1;
    }

    static int adminDoctorFixAll(CommandSourceStack source) {
        if (source.getServer() == null) {
            return 0;
        }
        int players = 0;
        int vaultAttempted = 0;
        int vaultOk = 0;
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            AdminOperationService.DoctorFixResult result = AdminOperationService.doctorFixPlayer(player, false);
            players++;
            if (result.vaultAttempted()) {
                vaultAttempted++;
            }
            if (result.vaultOk()) {
                vaultOk++;
            }
        }
        CurrencyHelper.saveAll(source.getServer());
        final int totalPlayers = players;
        final int totalVaultAttempted = vaultAttempted;
        final int totalVaultOk = vaultOk;
        source.sendSuccess(() -> Component.translatable(
            "message.zeconomy.admin.doctorfixall.success",
            totalPlayers,
            totalVaultAttempted,
            totalVaultOk
        ), true);
        return 1;
    }

    static int adminSyncVault(CommandSourceStack source, ServerPlayer player) {
        if (!EconomyConfig.ENABLE_VAULT_BRIDGE.get()) {
            source.sendFailure(Component.translatable("message.zeconomy.admin.syncvault.disabled"));
            return 0;
        }
        AdminOperationService.VaultSyncResult result = AdminOperationService.syncVault(player);
        if (!result.available()) {
            source.sendFailure(Component.translatable("message.zeconomy.admin.syncvault.unavailable"));
            return 0;
        }
        if (!result.success()) {
            source.sendFailure(Component.translatable(
                "message.zeconomy.admin.syncvault.failed",
                player.getName().getString(),
                EconomyConfig.VAULT_SYNC_CURRENCY_ID.get()
            ));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable(
            "message.zeconomy.admin.syncvault.success",
            player.getName().getString(),
            EconomyConfig.VAULT_SYNC_CURRENCY_ID.get(),
            String.format("%.2f", CurrencyHelper.getPlayerCurrencyServerData().getBalance(player, EconomyConfig.VAULT_SYNC_CURRENCY_ID.get()).value)
        ), true);
        return 1;
    }

    private static int copyIfExists(Path source, Path backupRoot) throws java.io.IOException {
        if (source == null || !Files.exists(source)) {
            return 0;
        }
        Path target = backupRoot.resolve(source.getFileName().toString());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        return 1;
    }

    private static void printSourceMap(CommandSourceStack source, java.util.Map<String, Double> values) {
        if (values.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("message.zeconomy.admin.inspect.empty"), false);
            return;
        }
        values.entrySet().stream()
            .sorted(java.util.Map.Entry.comparingByKey())
            .forEach(entry -> source.sendSuccess(
                () -> Component.translatable("message.zeconomy.admin.inspect.map_line", entry.getKey(), String.format("%.2f", entry.getValue())),
                false
            ));
    }
}
