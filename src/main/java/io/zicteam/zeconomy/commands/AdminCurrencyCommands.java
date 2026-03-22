package io.zicteam.zeconomy.commands;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.zicteam.zeconomy.ZEconomy;
import io.zicteam.zeconomy.currencies.BaseCurrency;
import io.zicteam.zeconomy.currencies.CurrencySymbol;
import io.zicteam.zeconomy.currencies.data.CurrencyPlayerData;
import io.zicteam.zeconomy.permissions.ModPermissions;
import io.zicteam.zeconomy.system.CurrencyAdminService;
import io.zicteam.zeconomy.utils.CurrencyHelper;
import io.zicteam.zeconomy.utils.ErrorCodes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

final class AdminCurrencyCommands {
    private AdminCurrencyCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> adminCurrencyNode() {
        return Commands.literal("currency")
            .requires(src -> EconomyCommands.hasAdminPermission(src, ModPermissions.ADMIN_CURRENCY))
            .then(Commands.literal("create")
                .then(Commands.argument("id", StringArgumentType.word())
                    .then(Commands.argument("symbol", StringArgumentType.string())
                        .executes(ctx -> adminCreateCurrency(
                            ctx.getSource(),
                            StringArgumentType.getString(ctx, "id"),
                            StringArgumentType.getString(ctx, "symbol"),
                            0.0D))
                        .then(Commands.argument("default", DoubleArgumentType.doubleArg(0.0D))
                            .executes(ctx -> adminCreateCurrency(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "id"),
                                StringArgumentType.getString(ctx, "symbol"),
                                DoubleArgumentType.getDouble(ctx, "default")))))))
            .then(Commands.literal("delete")
                .then(Commands.argument("id", StringArgumentType.word())
                    .executes(ctx -> adminDeleteCurrency(
                        ctx.getSource(),
                        StringArgumentType.getString(ctx, "id")))))
            .then(Commands.literal("lock")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("id", StringArgumentType.word())
                        .executes(ctx -> adminLockCurrency(
                            ctx.getSource(),
                            EntityArgument.getPlayer(ctx, "player"),
                            StringArgumentType.getString(ctx, "id"),
                            true))))
                .then(Commands.argument("id", StringArgumentType.word())
                    .executes(ctx -> adminLockCurrencyAll(
                        ctx.getSource(),
                        StringArgumentType.getString(ctx, "id"),
                        true))))
            .then(Commands.literal("unlock")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("id", StringArgumentType.word())
                        .executes(ctx -> adminLockCurrency(
                            ctx.getSource(),
                            EntityArgument.getPlayer(ctx, "player"),
                            StringArgumentType.getString(ctx, "id"),
                            false))))
                .then(Commands.argument("id", StringArgumentType.word())
                    .executes(ctx -> adminLockCurrencyAll(
                        ctx.getSource(),
                        StringArgumentType.getString(ctx, "id"),
                        false))))
            .then(Commands.literal("inspect")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> adminCurrencyInspect(
                        ctx.getSource(),
                        EntityArgument.getPlayer(ctx, "player")))))
            .then(Commands.literal("players")
                .then(Commands.argument("id", StringArgumentType.word())
                    .executes(ctx -> adminCurrencyPlayers(
                        ctx.getSource(),
                        StringArgumentType.getString(ctx, "id")))))
            .then(Commands.literal("repair")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> adminCurrencyRepair(
                        ctx.getSource(),
                        EntityArgument.getPlayer(ctx, "player")))))
            .then(Commands.literal("issues")
                .executes(ctx -> adminCurrencyIssues(ctx.getSource())))
            .then(Commands.literal("summary")
                .executes(ctx -> adminCurrencySummary(ctx.getSource())))
            .then(Commands.literal("repairissues")
                .executes(ctx -> adminCurrencyRepairIssues(ctx.getSource())))
            .then(Commands.literal("audit")
                .executes(ctx -> adminCurrencyAudit(ctx.getSource())))
            .then(Commands.literal("repairall")
                .executes(ctx -> adminCurrencyRepairAll(ctx.getSource())));
    }

    private static int adminCreateCurrency(CommandSourceStack source, String id, String symbol, double defaultValue) {
        if (id == null || id.isBlank() || !id.matches("[a-z0-9_\\-.]+")) {
            source.sendFailure(Component.translatable("message.zeconomy.admin.currency.invalid_id", id == null ? "" : id));
            return 0;
        }
        BaseCurrency currency = new BaseCurrency(id, new CurrencySymbol(symbol), defaultValue);
        ErrorCodes result = CurrencyHelper.createCurrencyOnServer(currency);
        if (!result.isSuccess()) {
            source.sendFailure(Component.translatable("message.zeconomy.admin.currency.create_failed", id, result.name()));
            return 0;
        }
        if (source.getServer() != null) {
            CurrencyHelper.syncCurrencyData(source.getServer());
        }
        source.sendSuccess(() -> Component.translatable("message.zeconomy.admin.currency.created", id, symbol, String.format("%.2f", defaultValue)), true);
        return 1;
    }

    private static int adminDeleteCurrency(CommandSourceStack source, String id) {
        if (id == null || id.isBlank()) {
            source.sendFailure(Component.translatable("message.zeconomy.admin.currency.invalid_id", ""));
            return 0;
        }
        ErrorCodes result = CurrencyHelper.deleteCurrencyOnServer(new BaseCurrency(id));
        if (!result.isSuccess()) {
            source.sendFailure(Component.translatable("message.zeconomy.admin.currency.delete_failed", id, result.name()));
            return 0;
        }
        if (source.getServer() != null) {
            CurrencyHelper.syncCurrencyData(source.getServer());
        }
        source.sendSuccess(() -> Component.translatable("message.zeconomy.admin.currency.deleted", id), true);
        return 1;
    }

    private static int adminLockCurrency(CommandSourceStack source, ServerPlayer player, String id, boolean locked) {
        if (id == null || id.isBlank()) {
            source.sendFailure(Component.translatable("message.zeconomy.admin.currency.invalid_id", ""));
            return 0;
        }
        ErrorCodes result = CurrencyPlayerData.SERVER.lockCurrency(player, id, locked);
        if (!result.isSuccess()) {
            source.sendFailure(Component.translatable(
                locked ? "message.zeconomy.admin.currency.lock_failed" : "message.zeconomy.admin.currency.unlock_failed",
                player.getName().getString(),
                id,
                result.name()
            ));
            return 0;
        }
        CurrencyHelper.syncPlayer(player);
        source.sendSuccess(() -> Component.translatable(
            locked ? "message.zeconomy.admin.currency.locked" : "message.zeconomy.admin.currency.unlocked",
            player.getName().getString(),
            id
        ), true);
        return 1;
    }

    private static int adminLockCurrencyAll(CommandSourceStack source, String id, boolean locked) {
        if (source.getServer() == null) {
            return 0;
        }
        if (id == null || id.isBlank()) {
            source.sendFailure(Component.translatable("message.zeconomy.admin.currency.invalid_id", ""));
            return 0;
        }
        java.util.Set<java.util.UUID> targetIds = CurrencyAdminService.collectTargetIds(source.getServer());
        int changed = 0;
        int failed = 0;
        for (java.util.UUID playerId : targetIds) {
            ErrorCodes result = CurrencyPlayerData.SERVER.lockCurrency(playerId, id, locked);
            if (result.isSuccess()) {
                changed++;
                ServerPlayer online = source.getServer().getPlayerList().getPlayer(playerId);
                if (online != null) {
                    CurrencyHelper.syncPlayer(online);
                }
            } else {
                failed++;
            }
        }
        CurrencyHelper.saveAll(source.getServer());
        final int changedCount = changed;
        final int failedCount = failed;
        source.sendSuccess(() -> Component.translatable(
            locked ? "message.zeconomy.admin.currency.lockall" : "message.zeconomy.admin.currency.unlockall",
            id,
            changedCount,
            failedCount
        ), true);
        return 1;
    }

    private static int adminCurrencyAudit(CommandSourceStack source) {
        CurrencyAdminService.CurrencyAuditResult result = CurrencyAdminService.auditCurrencies();
        source.sendSuccess(() -> Component.translatable(
            "message.zeconomy.admin.currency.audit",
            result.registered(),
            result.players(),
            result.missingEntries(),
            result.danglingEntries()
        ), false);
        return result.danglingEntries() == 0 && result.missingEntries() == 0 ? 1 : 0;
    }

    private static int adminCurrencyInspect(CommandSourceStack source, ServerPlayer player) {
        CurrencyAdminService.CurrencyInspectResult result = CurrencyAdminService.inspectPlayer(player);
        source.sendSuccess(() -> Component.translatable(
            "message.zeconomy.admin.currency.inspect",
            player.getName().getString(),
            result.registered(),
            result.entries(),
            result.locked(),
            result.missing(),
            result.dangling()
        ), false);
        return result.missing() == 0 && result.dangling() == 0 ? 1 : 0;
    }

    private static int adminCurrencyPlayers(CommandSourceStack source, String id) {
        if (source.getServer() == null) {
            return 0;
        }
        if (id == null || id.isBlank()) {
            source.sendFailure(Component.translatable("message.zeconomy.admin.currency.invalid_id", ""));
            return 0;
        }
        BaseCurrency currency = io.zicteam.zeconomy.currencies.data.CurrencyData.SERVER.currencies.stream()
            .filter(entry -> id.equals(entry.getName()))
            .findFirst()
            .orElse(null);
        if (currency == null) {
            source.sendFailure(Component.translatable("message.zeconomy.admin.currency.invalid_id", id));
            return 0;
        }
        CurrencyAdminService.CurrencyPlayersReport report = CurrencyAdminService.playerCoverage(source.getServer(), id);
        source.sendSuccess(() -> Component.translatable(
            "message.zeconomy.admin.currency.players_header",
            id,
            report.playerCount()
        ), false);
        for (CurrencyAdminService.CurrencyPlayerState state : report.states()) {
            String playerName = currencyPlayerName(source, state.playerId());
            if (state.present()) {
                final String finalPlayerName = playerName;
                final double balance = state.balance();
                final String lockStateKey = state.locked()
                    ? "message.zeconomy.admin.currency.players_locked"
                    : "message.zeconomy.admin.currency.players_unlocked";
                source.sendSuccess(() -> Component.translatable(
                    "message.zeconomy.admin.currency.players_line_present",
                    finalPlayerName,
                    String.format("%.2f", balance),
                    Component.translatable(lockStateKey)
                ), false);
            } else {
                final String finalPlayerName = playerName;
                source.sendSuccess(() -> Component.translatable(
                    "message.zeconomy.admin.currency.players_line_missing",
                    finalPlayerName
                ), false);
            }
        }
        source.sendSuccess(() -> Component.translatable(
            "message.zeconomy.admin.currency.players_summary",
            id,
            report.present(),
            report.missing(),
            report.locked()
        ), false);
        return report.missing() == 0 ? 1 : 0;
    }

    private static int adminCurrencyRepair(CommandSourceStack source, ServerPlayer player) {
        CurrencyAdminService.CurrencyRepairResult result = CurrencyAdminService.repairPlayer(player, source.getServer() != null);
        source.sendSuccess(() -> Component.translatable(
            "message.zeconomy.admin.currency.repair",
            player.getName().getString(),
            result.added(),
            result.removed()
        ), true);
        return 1;
    }

    private static int adminCurrencyIssues(CommandSourceStack source) {
        if (source.getServer() == null) {
            return 0;
        }
        CurrencyAdminService.CurrencyIssuesReport report = CurrencyAdminService.inspectIssues(source.getServer());
        source.sendSuccess(() -> Component.translatable("message.zeconomy.admin.currency.issues_header"), false);
        for (CurrencyAdminService.PlayerIssue issue : report.issues()) {
            String playerName = currencyPlayerName(source, issue.playerId());
            final String finalPlayerName = playerName;
            source.sendSuccess(() -> Component.translatable(
                "message.zeconomy.admin.currency.issues_line",
                finalPlayerName,
                issue.entries(),
                issue.missing(),
                issue.dangling(),
                issue.locked()
            ), false);
        }
        final boolean clean = report.issues().isEmpty();
        source.sendSuccess(() -> Component.translatable(
            clean
                ? "message.zeconomy.admin.currency.issues_clean"
                : "message.zeconomy.admin.currency.issues_summary",
            report.checked(),
            report.issues().size()
        ), false);
        return clean ? 1 : 0;
    }

    private static int adminCurrencySummary(CommandSourceStack source) {
        if (source.getServer() == null) {
            return 0;
        }
        java.util.Set<java.util.UUID> targetIds = CurrencyAdminService.collectTargetIds(source.getServer());
        java.util.LinkedList<CurrencyAdminService.CurrencySummaryLine> lines = CurrencyAdminService.summarizeCurrencies(source.getServer());
        final int playerCount = targetIds.size();
        source.sendSuccess(() -> Component.translatable(
            "message.zeconomy.admin.currency.summary_header",
            playerCount
        ), false);
        for (CurrencyAdminService.CurrencySummaryLine line : lines) {
            source.sendSuccess(() -> Component.translatable(
                "message.zeconomy.admin.currency.summary_line",
                line.currencyId(),
                line.present(),
                line.missing(),
                line.locked()
            ), false);
        }
        return 1;
    }

    private static int adminCurrencyRepairIssues(CommandSourceStack source) {
        if (source.getServer() == null) {
            return 0;
        }
        CurrencyAdminService.CurrencyBatchRepairResult result = CurrencyAdminService.repairIssues(source.getServer());
        final boolean clean = result.players() == 0;
        source.sendSuccess(() -> Component.translatable(
            clean
                ? "message.zeconomy.admin.currency.repairissues_clean"
                : "message.zeconomy.admin.currency.repairissues",
            result.players(),
            result.added(),
            result.removed()
        ), true);
        return 1;
    }

    private static int adminCurrencyRepairAll(CommandSourceStack source) {
        if (source.getServer() == null) {
            return 0;
        }
        CurrencyAdminService.CurrencyBatchRepairResult result = CurrencyAdminService.repairAll(source.getServer());
        source.sendSuccess(() -> Component.translatable(
            "message.zeconomy.admin.currency.repairall",
            result.players(),
            result.added(),
            result.removed()
        ), true);
        return 1;
    }

    private static String currencyPlayerName(CommandSourceStack source, java.util.UUID playerId) {
        if (source.getServer() != null) {
            ServerPlayer online = source.getServer().getPlayerList().getPlayer(playerId);
            if (online != null) {
                return online.getName().getString();
            }
        }
        return playerId.toString();
    }
}
