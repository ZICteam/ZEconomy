package example.integration;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.sixik.zeconomy.api.ZEconomyApi;

public final class ExampleEconomyCommands {
    private ExampleEconomyCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, ZEconomyApi api) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("zexample");

        root.then(Commands.literal("snapshot")
            .then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> showSnapshot(
                    ctx.getSource(),
                    EntityArgument.getPlayer(ctx, "player"),
                    api
                ))));

        root.then(Commands.literal("givez")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0001D))
                    .executes(ctx -> giveZ(
                        ctx.getSource(),
                        EntityArgument.getPlayer(ctx, "player"),
                        DoubleArgumentType.getDouble(ctx, "amount"),
                        api
                    )))));

        root.then(Commands.literal("treasury")
            .executes(ctx -> showTreasury(ctx.getSource(), api)));

        root.then(Commands.literal("status")
            .executes(ctx -> showStatus(ctx.getSource(), api)));

        dispatcher.register(root);
    }

    private static int showSnapshot(CommandSourceStack source, ServerPlayer player, ZEconomyApi api) {
        var result = api.getPlayerSnapshot(player.getUUID());
        if (!result.success()) {
            source.sendFailure(Component.literal("Snapshot failed: " + result.errorCode() + " | " + result.message()));
            return 0;
        }
        var snapshot = result.value();
        source.sendSuccess(() -> Component.literal("Snapshot for " + snapshot.playerName() + " [" + snapshot.playerId() + "]"), false);
        source.sendSuccess(() -> Component.literal("Wallet z_coin: " + snapshot.walletBalances().getOrDefault("z_coin", 0.0D)), false);
        source.sendSuccess(() -> Component.literal("Bank z_coin: " + snapshot.bankBalances().getOrDefault("z_coin", 0.0D)), false);
        source.sendSuccess(() -> Component.literal("Vault z_coin: " + snapshot.vaultBalances().getOrDefault("z_coin", 0.0D)), false);
        source.sendSuccess(() -> Component.literal("Mail: " + snapshot.pendingMail() + " | Streak: " + snapshot.dailyStreak() + " | Claimed today: " + snapshot.claimedDailyToday()), false);
        return 1;
    }

    private static int giveZ(CommandSourceStack source, ServerPlayer player, double amount, ZEconomyApi api) {
        var result = api.addBalance(player.getUUID(), "z_coin", amount);
        if (!result.success()) {
            source.sendFailure(Component.literal("Add failed: " + result.errorCode() + " | " + result.message()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("New z_coin balance for " + player.getName().getString() + ": " + result.value()), true);
        return 1;
    }

    private static int showTreasury(CommandSourceStack source, ZEconomyApi api) {
        var result = api.getTreasurySnapshot("z_coin");
        if (!result.success()) {
            source.sendFailure(Component.literal("Treasury failed: " + result.errorCode() + " | " + result.message()));
            return 0;
        }
        var treasury = result.value();
        source.sendSuccess(() -> Component.literal(
            "Treasury z_coin | total=" + treasury.totalBalance()
                + " reserved=" + treasury.reservedBalance()
                + " spendable=" + treasury.spendableBalance()
        ), false);
        return 1;
    }

    private static int showStatus(CommandSourceStack source, ZEconomyApi api) {
        var result = api.getRuntimeStatus();
        if (!result.success()) {
            source.sendFailure(Component.literal("Status failed: " + result.errorCode() + " | " + result.message()));
            return 0;
        }
        var status = result.value();
        source.sendSuccess(() -> Component.literal(
            "ZEconomy status | storage=" + status.storageMode()
                + " vaultEnabled=" + status.vaultBridgeEnabled()
                + " vaultAvailable=" + status.vaultBridgeAvailable()
                + " provider=" + status.vaultProvider()
        ), false);
        return 1;
    }
}
