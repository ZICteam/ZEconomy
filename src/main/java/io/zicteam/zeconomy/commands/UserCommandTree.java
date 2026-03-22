package io.zicteam.zeconomy.commands;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.zicteam.zeconomy.permissions.ModPermissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;

final class UserCommandTree {
    private UserCommandTree() {
    }

    static void append(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("help")
            .executes(ctx -> UserEconomyCommands.showHelp(ctx.getSource())));

        root.then(Commands.literal("balance")
            .requires(src -> EconomyCommands.hasUserPermission(src, ModPermissions.COMMAND_BALANCE))
            .executes(ctx -> UserEconomyCommands.showBalance(ctx.getSource(), ctx.getSource().getPlayerOrException()))
            .then(Commands.argument("player", EntityArgument.player())
                .requires(src -> EconomyCommands.hasAdminPermission(src, ModPermissions.ADMIN_SET))
                .executes(ctx -> UserEconomyCommands.showBalance(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))));

        root.then(Commands.literal("pay")
            .requires(src -> EconomyCommands.hasUserPermission(src, ModPermissions.COMMAND_PAY))
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("currency", StringArgumentType.word())
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0001D))
                        .executes(ctx -> UserEconomyCommands.pay(
                            ctx.getSource().getPlayerOrException(),
                            EntityArgument.getPlayer(ctx, "player"),
                            StringArgumentType.getString(ctx, "currency"),
                            DoubleArgumentType.getDouble(ctx, "amount")))))));

        root.then(Commands.literal("bank")
            .requires(src -> EconomyCommands.hasUserPermission(src, ModPermissions.COMMAND_BANK))
            .then(Commands.literal("deposit")
                .then(Commands.argument("currency", StringArgumentType.word())
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0001D))
                        .executes(ctx -> UserEconomyCommands.bankDeposit(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "currency"), DoubleArgumentType.getDouble(ctx, "amount"))))))
            .then(Commands.literal("withdraw")
                .then(Commands.argument("currency", StringArgumentType.word())
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0001D))
                        .executes(ctx -> UserEconomyCommands.bankWithdraw(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "currency"), DoubleArgumentType.getDouble(ctx, "amount"))))))
            .then(Commands.literal("info").executes(ctx -> UserEconomyCommands.bankInfo(ctx.getSource().getPlayerOrException()))));

        LiteralArgumentBuilder<CommandSourceStack> exchangeCmd = Commands.literal("exchange")
            .requires(src -> EconomyCommands.hasUserPermission(src, ModPermissions.COMMAND_EXCHANGE))
            .then(Commands.argument("from", StringArgumentType.word())
                .then(Commands.argument("to", StringArgumentType.word())
                    .executes(ctx -> UserEconomyCommands.showRate(
                        ctx.getSource(),
                        StringArgumentType.getString(ctx, "from"),
                        StringArgumentType.getString(ctx, "to")))
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0001D))
                        .executes(ctx -> UserEconomyCommands.exchangeCurrency(
                            ctx.getSource().getPlayerOrException(),
                            StringArgumentType.getString(ctx, "from"),
                            StringArgumentType.getString(ctx, "to"),
                            DoubleArgumentType.getDouble(ctx, "amount"))))))
            .then(Commands.literal("rates")
                .executes(ctx -> UserEconomyCommands.showRates(ctx.getSource())))
            .then(Commands.literal("audit")
                .requires(src -> EconomyCommands.hasAdminPermission(src, ModPermissions.ADMIN_EXCHANGE_RATE))
                .executes(ctx -> UserEconomyCommands.auditRates(ctx.getSource())))
            .then(Commands.literal("resetrates")
                .requires(src -> EconomyCommands.hasAdminPermission(src, ModPermissions.ADMIN_EXCHANGE_RATE))
                .executes(ctx -> UserEconomyCommands.resetRates(ctx.getSource())))
            .then(Commands.literal("rate")
                .requires(src -> EconomyCommands.hasAdminPermission(src, ModPermissions.ADMIN_EXCHANGE_RATE))
                .then(Commands.argument("from", StringArgumentType.word())
                    .then(Commands.argument("to", StringArgumentType.word())
                        .executes(ctx -> UserEconomyCommands.showRate(
                            ctx.getSource(),
                            StringArgumentType.getString(ctx, "from"),
                            StringArgumentType.getString(ctx, "to")))
                        .then(Commands.argument("rate", DoubleArgumentType.doubleArg(0.0001D))
                            .executes(ctx -> UserEconomyCommands.setRate(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "from"),
                                StringArgumentType.getString(ctx, "to"),
                                DoubleArgumentType.getDouble(ctx, "rate")))))));
        exchangeCmd.then(Commands.literal("ratepair")
            .requires(src -> EconomyCommands.hasAdminPermission(src, ModPermissions.ADMIN_EXCHANGE_RATE))
            .then(Commands.argument("from", StringArgumentType.word())
                .then(Commands.argument("to", StringArgumentType.word())
                    .then(Commands.argument("rate", DoubleArgumentType.doubleArg(0.0001D))
                        .executes(ctx -> UserEconomyCommands.setRatePair(
                            ctx.getSource(),
                            StringArgumentType.getString(ctx, "from"),
                            StringArgumentType.getString(ctx, "to"),
                            DoubleArgumentType.getDouble(ctx, "rate")))))));
        exchangeCmd.then(Commands.literal("rateclear")
            .requires(src -> EconomyCommands.hasAdminPermission(src, ModPermissions.ADMIN_EXCHANGE_RATE))
            .then(Commands.argument("from", StringArgumentType.word())
                .then(Commands.argument("to", StringArgumentType.word())
                    .executes(ctx -> UserEconomyCommands.clearRate(
                        ctx.getSource(),
                        StringArgumentType.getString(ctx, "from"),
                        StringArgumentType.getString(ctx, "to"))))));
        root.then(exchangeCmd);

        root.then(Commands.literal("mail")
            .requires(src -> EconomyCommands.hasUserPermission(src, ModPermissions.COMMAND_MAIL))
            .then(Commands.literal("claim").executes(ctx -> UserEconomyCommands.mailClaim(ctx.getSource().getPlayerOrException())))
            .then(Commands.literal("send")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> UserEconomyCommands.mailSend(
                        ctx.getSource().getPlayerOrException(),
                        EntityArgument.getPlayer(ctx, "player"))))));

        root.then(Commands.literal("vault")
            .requires(src -> EconomyCommands.hasUserPermission(src, ModPermissions.COMMAND_VAULT))
            .then(Commands.literal("setpin")
                .then(Commands.argument("pin", StringArgumentType.word())
                    .executes(ctx -> UserEconomyCommands.vaultSetPin(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "pin")))))
            .then(Commands.literal("balance").executes(ctx -> UserEconomyCommands.vaultBalance(ctx.getSource().getPlayerOrException())))
            .then(Commands.literal("deposit")
                .then(Commands.argument("pin", StringArgumentType.word())
                    .then(Commands.argument("currency", StringArgumentType.word())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0001D))
                            .executes(ctx -> UserEconomyCommands.vaultDeposit(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "pin"), StringArgumentType.getString(ctx, "currency"), DoubleArgumentType.getDouble(ctx, "amount")))))))
            .then(Commands.literal("withdraw")
                .then(Commands.argument("pin", StringArgumentType.word())
                    .then(Commands.argument("currency", StringArgumentType.word())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0001D))
                            .executes(ctx -> UserEconomyCommands.vaultWithdraw(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "pin"), StringArgumentType.getString(ctx, "currency"), DoubleArgumentType.getDouble(ctx, "amount"))))))));

        root.then(Commands.literal("daily")
            .requires(src -> EconomyCommands.hasUserPermission(src, ModPermissions.COMMAND_DAILY))
            .then(Commands.literal("claim").executes(ctx -> UserEconomyCommands.dailyClaim(ctx.getSource().getPlayerOrException()))));

        root.then(Commands.literal("top")
            .requires(src -> EconomyCommands.hasUserPermission(src, ModPermissions.COMMAND_TOP))
            .then(Commands.argument("currency", StringArgumentType.word())
                .executes(ctx -> UserEconomyCommands.showTop(ctx.getSource(), StringArgumentType.getString(ctx, "currency"))))
            .executes(ctx -> UserEconomyCommands.showTop(ctx.getSource(), "z_coin")));

        root.then(Commands.literal("currencies")
            .requires(src -> EconomyCommands.hasUserPermission(src, ModPermissions.COMMAND_BALANCE))
            .executes(ctx -> UserEconomyCommands.showCurrencies(ctx.getSource())));

        root.then(Commands.literal("status")
            .requires(src -> EconomyCommands.hasUserPermission(src, ModPermissions.COMMAND_BALANCE))
            .executes(ctx -> UserEconomyCommands.showStatus(ctx.getSource())));

        root.then(Commands.literal("gui")
            .requires(src -> EconomyCommands.hasAdminPermission(src, ModPermissions.ADMIN_RELOAD))
            .then(Commands.literal("edit")
                .then(Commands.argument("target", StringArgumentType.word())
                    .executes(ctx -> EconomyCommands.openGuiEditor(
                        ctx.getSource(),
                        StringArgumentType.getString(ctx, "target")
                    )))));
    }
}
