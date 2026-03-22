package net.sixik.zeconomy.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.LevelResource;
import net.sixik.zeconomy.CustomPlayerData;
import net.sixik.zeconomy.ZEconomyEvents;
import net.sixik.zeconomy.ZEconomy;
import net.sixik.zeconomy.block.entity.ExchangeBlockEntity;
import net.sixik.zeconomy.config.EconomyConfig;
import net.sixik.zeconomy.config.GuiLayoutConfig;
import net.sixik.zeconomy.currencies.BaseCurrency;
import net.sixik.zeconomy.currencies.CurrencySymbol;
import net.sixik.zeconomy.currencies.compat.VaultBridge;
import net.sixik.zeconomy.currencies.data.CurrencyPlayerData;
import net.sixik.zeconomy.permissions.ModPermissions;
import net.sixik.zeconomy.system.DataStorageManager;
import net.sixik.zeconomy.system.ExtraEconomyData;
import net.sixik.zeconomy.network.ZEconomyNetwork;
import net.sixik.zeconomy.utils.CurrencyHelper;
import net.sixik.zeconomy.utils.ErrorCodes;
import net.minecraftforge.server.permission.nodes.PermissionNode;

public final class EconomyCommands {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private EconomyCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("zeco");

        root.then(Commands.literal("help")
            .executes(ctx -> showHelp(ctx.getSource())));

        root.then(Commands.literal("balance")
            .requires(src -> hasUserPermission(src, ModPermissions.COMMAND_BALANCE))
            .executes(ctx -> showBalance(ctx.getSource(), ctx.getSource().getPlayerOrException()))
            .then(Commands.argument("player", EntityArgument.player())
                .requires(src -> hasAdminPermission(src, ModPermissions.ADMIN_SET))
                .executes(ctx -> showBalance(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))));

        root.then(Commands.literal("pay")
            .requires(src -> hasUserPermission(src, ModPermissions.COMMAND_PAY))
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("currency", StringArgumentType.word())
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0001D))
                        .executes(ctx -> pay(
                            ctx.getSource().getPlayerOrException(),
                            EntityArgument.getPlayer(ctx, "player"),
                            StringArgumentType.getString(ctx, "currency"),
                            DoubleArgumentType.getDouble(ctx, "amount")))))));

        root.then(Commands.literal("bank")
            .requires(src -> hasUserPermission(src, ModPermissions.COMMAND_BANK))
            .then(Commands.literal("deposit")
                .then(Commands.argument("currency", StringArgumentType.word())
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0001D))
                        .executes(ctx -> bankDeposit(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "currency"), DoubleArgumentType.getDouble(ctx, "amount"))))))
            .then(Commands.literal("withdraw")
                .then(Commands.argument("currency", StringArgumentType.word())
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0001D))
                        .executes(ctx -> bankWithdraw(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "currency"), DoubleArgumentType.getDouble(ctx, "amount"))))))
            .then(Commands.literal("info").executes(ctx -> bankInfo(ctx.getSource().getPlayerOrException()))));

        LiteralArgumentBuilder<CommandSourceStack> exchangeCmd = Commands.literal("exchange")
            .requires(src -> hasUserPermission(src, ModPermissions.COMMAND_EXCHANGE))
            .then(Commands.argument("from", StringArgumentType.word())
                .then(Commands.argument("to", StringArgumentType.word())
                    .executes(ctx -> showRate(
                        ctx.getSource(),
                        StringArgumentType.getString(ctx, "from"),
                        StringArgumentType.getString(ctx, "to")))
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0001D))
                        .executes(ctx -> exchangeCurrency(
                            ctx.getSource().getPlayerOrException(),
                            StringArgumentType.getString(ctx, "from"),
                            StringArgumentType.getString(ctx, "to"),
                            DoubleArgumentType.getDouble(ctx, "amount"))))))
            .then(Commands.literal("rates")
                .executes(ctx -> showRates(ctx.getSource())))
            .then(Commands.literal("audit")
                .requires(src -> hasAdminPermission(src, ModPermissions.ADMIN_EXCHANGE_RATE))
                .executes(ctx -> auditRates(ctx.getSource())))
            .then(Commands.literal("resetrates")
                .requires(src -> hasAdminPermission(src, ModPermissions.ADMIN_EXCHANGE_RATE))
                .executes(ctx -> resetRates(ctx.getSource())))
            .then(Commands.literal("rate")
                .requires(src -> hasAdminPermission(src, ModPermissions.ADMIN_EXCHANGE_RATE))
                .then(Commands.argument("from", StringArgumentType.word())
                    .then(Commands.argument("to", StringArgumentType.word())
                        .executes(ctx -> showRate(
                            ctx.getSource(),
                            StringArgumentType.getString(ctx, "from"),
                            StringArgumentType.getString(ctx, "to")))
                        .then(Commands.argument("rate", DoubleArgumentType.doubleArg(0.0001D))
                            .executes(ctx -> setRate(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "from"),
                                StringArgumentType.getString(ctx, "to"),
                                DoubleArgumentType.getDouble(ctx, "rate")))))));
            exchangeCmd.then(Commands.literal("ratepair")
                .requires(src -> hasAdminPermission(src, ModPermissions.ADMIN_EXCHANGE_RATE))
                .then(Commands.argument("from", StringArgumentType.word())
                    .then(Commands.argument("to", StringArgumentType.word())
                        .then(Commands.argument("rate", DoubleArgumentType.doubleArg(0.0001D))
                            .executes(ctx -> setRatePair(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "from"),
                                StringArgumentType.getString(ctx, "to"),
                                DoubleArgumentType.getDouble(ctx, "rate")))))));
            exchangeCmd.then(Commands.literal("rateclear")
                .requires(src -> hasAdminPermission(src, ModPermissions.ADMIN_EXCHANGE_RATE))
                .then(Commands.argument("from", StringArgumentType.word())
                    .then(Commands.argument("to", StringArgumentType.word())
                        .executes(ctx -> clearRate(
                            ctx.getSource(),
                            StringArgumentType.getString(ctx, "from"),
                            StringArgumentType.getString(ctx, "to"))))));
        root.then(exchangeCmd);

        root.then(Commands.literal("mail")
            .requires(src -> hasUserPermission(src, ModPermissions.COMMAND_MAIL))
            .then(Commands.literal("claim").executes(ctx -> mailClaim(ctx.getSource().getPlayerOrException())))
            .then(Commands.literal("send")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> mailSend(
                        ctx.getSource().getPlayerOrException(),
                        EntityArgument.getPlayer(ctx, "player"))))));

        root.then(Commands.literal("vault")
            .requires(src -> hasUserPermission(src, ModPermissions.COMMAND_VAULT))
            .then(Commands.literal("setpin")
                .then(Commands.argument("pin", StringArgumentType.word())
                    .executes(ctx -> vaultSetPin(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "pin")))))
            .then(Commands.literal("balance").executes(ctx -> vaultBalance(ctx.getSource().getPlayerOrException())))
            .then(Commands.literal("deposit")
                .then(Commands.argument("pin", StringArgumentType.word())
                    .then(Commands.argument("currency", StringArgumentType.word())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0001D))
                            .executes(ctx -> vaultDeposit(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "pin"), StringArgumentType.getString(ctx, "currency"), DoubleArgumentType.getDouble(ctx, "amount")))))))
            .then(Commands.literal("withdraw")
                .then(Commands.argument("pin", StringArgumentType.word())
                    .then(Commands.argument("currency", StringArgumentType.word())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0001D))
                            .executes(ctx -> vaultWithdraw(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "pin"), StringArgumentType.getString(ctx, "currency"), DoubleArgumentType.getDouble(ctx, "amount"))))))));

        root.then(Commands.literal("daily")
            .requires(src -> hasUserPermission(src, ModPermissions.COMMAND_DAILY))
            .then(Commands.literal("claim").executes(ctx -> dailyClaim(ctx.getSource().getPlayerOrException()))));

        root.then(Commands.literal("top")
            .requires(src -> hasUserPermission(src, ModPermissions.COMMAND_TOP))
            .then(Commands.argument("currency", StringArgumentType.word())
                .executes(ctx -> showTop(ctx.getSource(), StringArgumentType.getString(ctx, "currency"))))
            .executes(ctx -> showTop(ctx.getSource(), "z_coin")));

        root.then(Commands.literal("currencies")
            .requires(src -> hasUserPermission(src, ModPermissions.COMMAND_BALANCE))
            .executes(ctx -> showCurrencies(ctx.getSource())));

        root.then(Commands.literal("status")
            .requires(src -> hasUserPermission(src, ModPermissions.COMMAND_BALANCE))
            .executes(ctx -> showStatus(ctx.getSource())));

        root.then(Commands.literal("logs")
            .requires(src -> hasAdminPermission(src, ModPermissions.ADMIN_LOGS))
            .then(Commands.argument("limit", IntegerArgumentType.integer(1, 100))
                .executes(ctx -> showLogs(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "limit"))))
            .executes(ctx -> showLogs(ctx.getSource(), 20)));

        root.then(Commands.literal("export")
            .requires(src -> hasAdminPermission(src, ModPermissions.ADMIN_EXPORT))
            .then(Commands.literal("now").executes(ctx -> exportNow(ctx.getSource()))));

        root.then(Commands.literal("reload")
            .requires(src -> hasAdminPermission(src, ModPermissions.ADMIN_RELOAD))
            .executes(ctx -> reloadMod(ctx.getSource())));

        root.then(Commands.literal("gui")
            .requires(src -> hasAdminPermission(src, ModPermissions.ADMIN_RELOAD))
            .then(Commands.literal("edit")
                .then(Commands.argument("target", StringArgumentType.word())
                    .executes(ctx -> openGuiEditor(
                        ctx.getSource(),
                        StringArgumentType.getString(ctx, "target")
                    )))));

        root.then(Commands.literal("admin")
            .requires(src -> hasAdminPermission(src, ModPermissions.ADMIN_SET))
            .then(Commands.literal("set")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("currency", StringArgumentType.word())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0D))
                            .executes(ctx -> adminSet(
                                ctx.getSource(),
                                EntityArgument.getPlayer(ctx, "player"),
                                StringArgumentType.getString(ctx, "currency"),
                                DoubleArgumentType.getDouble(ctx, "amount")))))))
            .then(adminCurrencyNode())
            .then(Commands.literal("status")
                .requires(src -> hasAdminPermission(src, ModPermissions.ADMIN_STATUS))
                .executes(ctx -> adminStatus(ctx.getSource())))
            .then(Commands.literal("save")
                .requires(src -> hasAdminPermission(src, ModPermissions.ADMIN_STATUS))
                .executes(ctx -> adminSave(ctx.getSource())))
            .then(Commands.literal("backup")
                .requires(src -> hasAdminPermission(src, ModPermissions.ADMIN_STATUS))
                .executes(ctx -> adminBackup(ctx.getSource())))
            .then(Commands.literal("reloadstorage")
                .requires(src -> hasAdminPermission(src, ModPermissions.ADMIN_RELOAD))
                .executes(ctx -> adminReloadStorage(ctx.getSource())))
            .then(Commands.literal("exportnow")
                .requires(src -> hasAdminPermission(src, ModPermissions.ADMIN_EXPORT))
                .executes(ctx -> adminExportNow(ctx.getSource())))
            .then(Commands.literal("exportstatus")
                .requires(src -> hasAdminPermission(src, ModPermissions.ADMIN_EXPORT))
                .executes(ctx -> adminExportStatus(ctx.getSource())))
            .then(Commands.literal("doctor")
                .requires(src -> hasAdminPermission(src, ModPermissions.ADMIN_STATUS))
                .executes(ctx -> adminDoctor(ctx.getSource())))
            .then(Commands.literal("doctorfix")
                .requires(src -> hasAdminPermission(src, ModPermissions.ADMIN_STATUS))
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> adminDoctorFix(
                        ctx.getSource(),
                        EntityArgument.getPlayer(ctx, "player")))))
            .then(Commands.literal("doctorfixall")
                .requires(src -> hasAdminPermission(src, ModPermissions.ADMIN_STATUS))
                .executes(ctx -> adminDoctorFixAll(ctx.getSource())))
            .then(Commands.literal("inspect")
                .requires(src -> hasAdminPermission(src, ModPermissions.ADMIN_STATUS))
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> adminInspect(
                        ctx.getSource(),
                        EntityArgument.getPlayer(ctx, "player")))))
            .then(Commands.literal("reconcile")
                .requires(src -> hasAdminPermission(src, ModPermissions.ADMIN_STATUS))
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> adminReconcile(
                        ctx.getSource(),
                        EntityArgument.getPlayer(ctx, "player")))))
            .then(Commands.literal("syncvault")
                .requires(src -> hasAdminPermission(src, ModPermissions.ADMIN_STATUS))
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> adminSyncVault(
                        ctx.getSource(),
                        EntityArgument.getPlayer(ctx, "player"))))));

        root.then(Commands.literal("server")
            .requires(src -> hasAdminPermission(src, ModPermissions.ADMIN_SET))
            .then(Commands.literal("balance")
                .executes(ctx -> serverBalance(ctx.getSource()))
                .then(Commands.argument("currency", StringArgumentType.word())
                    .executes(ctx -> serverBalance(ctx.getSource(), StringArgumentType.getString(ctx, "currency")))))
            .then(Commands.literal("set")
                .then(Commands.argument("currency", StringArgumentType.word())
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0D))
                        .executes(ctx -> serverSet(
                            ctx.getSource(),
                            StringArgumentType.getString(ctx, "currency"),
                            DoubleArgumentType.getDouble(ctx, "amount"))))))
            .then(Commands.literal("give")
                .then(Commands.argument("currency", StringArgumentType.word())
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0001D))
                        .executes(ctx -> serverGive(
                            ctx.getSource(),
                            StringArgumentType.getString(ctx, "currency"),
                            DoubleArgumentType.getDouble(ctx, "amount"))))))
            .then(Commands.literal("take")
                .then(Commands.argument("currency", StringArgumentType.word())
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0001D))
                        .executes(ctx -> serverTake(
                            ctx.getSource(),
                            StringArgumentType.getString(ctx, "currency"),
                            DoubleArgumentType.getDouble(ctx, "amount")))))));

        root.then(Commands.literal("exchangeblock")
            .then(Commands.literal("set")
                .requires(src -> hasAdminPermission(src, ModPermissions.ADMIN_EXCHANGEBLOCK_SET))
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                    .then(Commands.argument("input_item", StringArgumentType.word())
                        .then(Commands.argument("input_count", IntegerArgumentType.integer(1))
                            .then(Commands.argument("output_item", StringArgumentType.word())
                                .then(Commands.argument("output_count", IntegerArgumentType.integer(1))
                                    .executes(ctx -> setExchangeBlockOffer(
                                        ctx.getSource().getPlayerOrException(),
                                        BlockPosArgument.getLoadedBlockPos(ctx, "pos"),
                                        StringArgumentType.getString(ctx, "input_item"),
                                        IntegerArgumentType.getInteger(ctx, "input_count"),
                                        StringArgumentType.getString(ctx, "output_item"),
                                        IntegerArgumentType.getInteger(ctx, "output_count"))))))))));

        dispatcher.register(root);
    }

    private static boolean hasUserPermission(CommandSourceStack source, PermissionNode<Boolean> node) {
        return hasPermission(source, node, EconomyConfig.OP_LEVEL_MODERATOR.get());
    }

    private static boolean hasAdminPermission(CommandSourceStack source, PermissionNode<Boolean> node) {
        return hasPermission(source, node, EconomyConfig.OP_LEVEL_ADMIN.get());
    }

    private static boolean hasPermission(CommandSourceStack source, PermissionNode<Boolean> node, int fallbackOpLevel) {
        if (!EconomyConfig.USE_PERMISSION_NODES.get()) {
            return source.hasPermission(fallbackOpLevel);
        }
        if (source.getEntity() instanceof ServerPlayer player && ModPermissions.check(player, node)) {
            return true;
        }
        return EconomyConfig.ALLOW_OP_FALLBACK.get() && source.hasPermission(fallbackOpLevel);
    }

    private static int showBalance(CommandSourceStack source, ServerPlayer player) {
        double sdm = CurrencyHelper.getPlayerCurrencyServerData().getBalance(player, "z_coin").value;
        double b = CurrencyHelper.getPlayerCurrencyServerData().getBalance(player, "b_coin").value;
        source.sendSuccess(() -> Component.literal(player.getName().getString() + " wallet: z_coin=" + String.format("%.2f", sdm) + ", b_coin=" + String.format("%.2f", b)), false);
        source.sendSuccess(() -> Component.literal("bank: z=" + String.format("%.2f", ZEconomy.EXTRA_DATA.getDeposited(player.getUUID(), "z_coin")) + ", b=" + String.format("%.2f", ZEconomy.EXTRA_DATA.getDeposited(player.getUUID(), "b_coin"))), false);
        source.sendSuccess(() -> Component.literal("vault: z=" + String.format("%.2f", ZEconomy.EXTRA_DATA.getVaultBalance(player.getUUID(), "z_coin")) + ", b=" + String.format("%.2f", ZEconomy.EXTRA_DATA.getVaultBalance(player.getUUID(), "b_coin"))), false);
        source.sendSuccess(() -> Component.literal("daily streak: " + ZEconomy.EXTRA_DATA.getDailyStreak(player.getUUID()) + " | mail: " + ZEconomy.EXTRA_DATA.pendingMailCount(player.getUUID())), false);
        return 1;
    }

    private static int pay(ServerPlayer from, ServerPlayer to, String currency, double amount) {
        boolean ok = ZEconomy.EXTRA_DATA.transferWithFee(from, to, currency, amount);
        if (!ok) {
            from.sendSystemMessage(Component.literal("Transfer failed: insufficient funds or invalid params").withStyle(ChatFormatting.RED));
            return 0;
        }
        double fee = amount * EconomyConfig.TRANSFER_FEE_RATE.get();
        from.sendSystemMessage(Component.literal("Sent " + amount + " " + currency + " to " + to.getName().getString() + " (fee " + String.format("%.2f", fee) + ")"));
        to.sendSystemMessage(Component.literal("Received " + amount + " " + currency + " from " + from.getName().getString()).withStyle(ChatFormatting.GREEN));
        CurrencyHelper.syncPlayer(from);
        CurrencyHelper.syncPlayer(to);
        return 1;
    }

    private static int bankDeposit(ServerPlayer player, String currency, double amount) {
        if (!ZEconomy.EXTRA_DATA.depositToBank(player, currency, amount)) {
            player.sendSystemMessage(Component.literal("Bank deposit failed").withStyle(ChatFormatting.RED));
            return 0;
        }
        player.sendSystemMessage(Component.literal("Bank deposit: " + amount + " " + currency));
        CurrencyHelper.syncPlayer(player);
        return 1;
    }

    private static int bankWithdraw(ServerPlayer player, String currency, double amount) {
        if (!ZEconomy.EXTRA_DATA.withdrawFromBank(player, currency, amount)) {
            player.sendSystemMessage(Component.literal("Bank withdraw failed").withStyle(ChatFormatting.RED));
            return 0;
        }
        player.sendSystemMessage(Component.literal("Bank withdraw: " + amount + " " + currency));
        CurrencyHelper.syncPlayer(player);
        return 1;
    }

    private static int bankInfo(ServerPlayer player) {
        MapToLines.print(player, "Bank balances", ZEconomy.EXTRA_DATA.getAllDeposits(player.getUUID()));
        player.sendSystemMessage(Component.literal("Hourly interest: " + (EconomyConfig.HOURLY_INTEREST_RATE.get() * 100.0) + "%"));
        return 1;
    }

    private static int exchangeCurrency(ServerPlayer player, String from, String to, double amount) {
        if (!ZEconomy.EXTRA_DATA.exchangeCurrency(player, from, to, amount)) {
            player.sendSystemMessage(Component.literal("Exchange failed (rate/balance/config)").withStyle(ChatFormatting.RED));
            return 0;
        }
        double rate = ZEconomy.EXTRA_DATA.getRate(from, to);
        player.sendSystemMessage(Component.literal("Exchanged " + amount + " " + from + " -> " + String.format("%.2f", amount * rate) + " " + to + " (before fee)"));
        CurrencyHelper.syncPlayer(player);
        return 1;
    }

    private static int setRate(CommandSourceStack source, String from, String to, double rate) {
        ZEconomy.EXTRA_DATA.setRate(from, to, rate);
        source.sendSuccess(() -> Component.literal("Rate set: " + from + " -> " + to + " = " + rate), true);
        return 1;
    }

    private static int setRatePair(CommandSourceStack source, String from, String to, double rate) {
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

    private static int mailClaim(ServerPlayer player) {
        java.util.List<net.minecraft.world.item.ItemStack> list = ZEconomy.EXTRA_DATA.claimMail(player.getUUID());
        if (list.isEmpty()) {
            player.sendSystemMessage(Component.literal("Mailbox is empty").withStyle(ChatFormatting.GRAY));
            return 1;
        }
        for (net.minecraft.world.item.ItemStack stack : list) {
            net.sixik.zeconomy.util.InventoryUtils.giveItem(player, stack);
        }
        ZEconomy.EXTRA_DATA.syncPlayerMirror(player);
        CurrencyHelper.syncPlayer(player);
        player.sendSystemMessage(Component.literal("Claimed " + list.size() + " mail item stack(s)").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int mailSend(ServerPlayer player, ServerPlayer target) {
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
        ZEconomy.EXTRA_DATA.syncPlayerMirror(target);
        ZEconomy.EXTRA_DATA.syncPlayerMirror(player);
        CurrencyHelper.syncPlayer(target);
        CurrencyHelper.syncPlayer(player);
        player.sendSystemMessage(Component.translatable("message.zeconomy.mailbox.command_sent", mailStack.getCount(), mailStack.getHoverName(), target.getName()).withStyle(ChatFormatting.GREEN));
        target.sendSystemMessage(Component.translatable("message.zeconomy.mailbox.received", player.getName().getString()));
        return 1;
    }

    private static int vaultSetPin(ServerPlayer player, String pin) {
        if (!ZEconomy.EXTRA_DATA.setVaultPin(player.getUUID(), pin)) {
            player.sendSystemMessage(Component.literal("PIN must be 4-12 digits").withStyle(ChatFormatting.RED));
            return 0;
        }
        ZEconomy.EXTRA_DATA.syncPlayerMirror(player);
        CurrencyHelper.syncPlayer(player);
        player.sendSystemMessage(Component.literal("Vault PIN updated").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int vaultDeposit(ServerPlayer player, String pin, String currency, double amount) {
        if (!ZEconomy.EXTRA_DATA.vaultDeposit(player, pin, currency, amount)) {
            player.sendSystemMessage(Component.literal("Vault deposit failed (check PIN/balance)").withStyle(ChatFormatting.RED));
            return 0;
        }
        CurrencyHelper.syncPlayer(player);
        player.sendSystemMessage(Component.literal("Vault deposit: " + amount + " " + currency));
        return 1;
    }

    private static int vaultWithdraw(ServerPlayer player, String pin, String currency, double amount) {
        if (!ZEconomy.EXTRA_DATA.vaultWithdraw(player, pin, currency, amount)) {
            player.sendSystemMessage(Component.literal("Vault withdraw failed (check PIN/balance)").withStyle(ChatFormatting.RED));
            return 0;
        }
        CurrencyHelper.syncPlayer(player);
        player.sendSystemMessage(Component.literal("Vault withdraw: " + amount + " " + currency));
        return 1;
    }

    private static int vaultBalance(ServerPlayer player) {
        MapToLines.print(player, "Vault balances", ZEconomy.EXTRA_DATA.getAllVaultBalances(player.getUUID()));
        return 1;
    }

    private static int dailyClaim(ServerPlayer player) {
        ExtraEconomyData.DailyClaimResult result = ZEconomy.EXTRA_DATA.claimDaily(player);
        if (!result.success()) {
            player.sendSystemMessage(Component.literal("Daily reward already claimed today").withStyle(ChatFormatting.YELLOW));
            return 0;
        }
        CurrencyHelper.syncPlayer(player);
        player.sendSystemMessage(Component.literal("Daily reward: +" + String.format("%.2f", result.zReward()) + " z_coin, +" + String.format("%.2f", result.bReward()) + " b_coin | streak " + result.streak()).withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int showTop(CommandSourceStack source, String currency) {
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

    private static int showCurrencies(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("message.zeconomy.currencies.header"), false);
        if (net.sixik.zeconomy.currencies.data.CurrencyData.SERVER.currencies.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("message.zeconomy.currencies.empty"), false);
            return 1;
        }
        for (net.sixik.zeconomy.currencies.BaseCurrency currency : net.sixik.zeconomy.currencies.data.CurrencyData.SERVER.currencies) {
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

    private static int showRate(CommandSourceStack source, String from, String to) {
        double rate = ZEconomy.EXTRA_DATA.getRate(from, to);
        if (rate <= 0.0D) {
            source.sendFailure(Component.translatable("message.zeconomy.exchange.rate_missing", from, to));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("message.zeconomy.exchange.rate_value", from, to, String.format("%.4f", rate)), false);
        return 1;
    }

    private static int showRates(CommandSourceStack source) {
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

    private static int clearRate(CommandSourceStack source, String from, String to) {
        boolean removed = ZEconomy.EXTRA_DATA.removeRate(from, to);
        if (!removed) {
            source.sendFailure(Component.translatable("message.zeconomy.exchange.rate_missing", from, to));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("message.zeconomy.exchange.rate_cleared", from, to), true);
        return 1;
    }

    private static int auditRates(CommandSourceStack source) {
        java.util.Map<String, Double> rates = ZEconomy.EXTRA_DATA.getAllRates();
        java.util.Set<String> currencies = net.sixik.zeconomy.currencies.data.CurrencyData.SERVER.currencies.stream()
            .map(net.sixik.zeconomy.currencies.BaseCurrency::getName)
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

    private static int resetRates(CommandSourceStack source) {
        ZEconomy.EXTRA_DATA.clearRates();
        ZEconomy.EXTRA_DATA.ensureDefaultRates();
        source.sendSuccess(() -> Component.translatable(
            "message.zeconomy.exchange.rates_reset",
            ZEconomy.EXTRA_DATA.getAllRates().size()
        ), true);
        return 1;
    }

    private static int showHelp(CommandSourceStack source) {
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

    private static LiteralArgumentBuilder<CommandSourceStack> adminCurrencyNode() {
        return Commands.literal("currency")
            .requires(src -> hasAdminPermission(src, ModPermissions.ADMIN_CURRENCY))
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
                            true)))))
            .then(Commands.literal("lockall")
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
                            false)))))
            .then(Commands.literal("unlockall")
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

    private static int showStatus(CommandSourceStack source) {
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

    private static int showLogs(CommandSourceStack source, int limit) {
        java.util.List<ExtraEconomyData.TransactionRecord> records = ZEconomy.EXTRA_DATA.getRecentLogs(limit);
        source.sendSuccess(() -> Component.literal("Recent logs: " + records.size()), false);
        for (ExtraEconomyData.TransactionRecord r : records) {
            String line = TS.format(Instant.ofEpochSecond(r.epochSec())) + " | " + r.type() + " | " + r.currency() + " " + String.format("%.2f", r.amount()) + " fee=" + String.format("%.2f", r.fee()) + " note=" + r.note();
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    private static int exportNow(CommandSourceStack source) {
        if (source.getServer() == null) {
            return 0;
        }
        ZEconomy.EXTRA_DATA.exportJson(source.getServer(), CurrencyHelper.exportJsonPath(source.getServer()));
        source.sendSuccess(() -> Component.literal("Exported: " + CurrencyHelper.exportJsonPath(source.getServer())), true);
        return 1;
    }

    private static int reloadMod(CommandSourceStack source) {
        if (source.getServer() == null) {
            return 0;
        }
        CurrencyHelper.saveAll(source.getServer());
        DataStorageManager.loadAll(source.getServer());
        CurrencyHelper.ensureDefaultCurrency();
        ZEconomy.EXTRA_DATA.ensureDefaultRates();
        GuiLayoutConfig.reload();
        CurrencyHelper.syncCurrencyData(source.getServer());
        source.sendSuccess(() -> Component.literal("ZEconomy reloaded: data + configs + GUI layouts"), true);
        return 1;
    }

    private static int openGuiEditor(CommandSourceStack source, String target) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only player can open GUI editor"));
            return 0;
        }
        if (!GuiLayoutConfig.isValidTarget(target)) {
            source.sendFailure(Component.literal("Unknown target. Use: exchange | atm | mailbox | bank"));
            return 0;
        }
        ZEconomyNetwork.openLayoutEditor(player, target);
        return 1;
    }

    private static int adminSet(CommandSourceStack source, ServerPlayer player, String currency, double amount) {
        CurrencyHelper.getPlayerCurrencyServerData().setCurrencyValue(player, currency, amount);
        ZEconomy.EXTRA_DATA.syncPlayerMirror(player);
        CurrencyHelper.syncPlayer(player);
        source.sendSuccess(() -> Component.literal("Balance set: " + player.getName().getString() + " " + currency + "=" + amount), true);
        return 1;
    }

    private static int serverBalance(CommandSourceStack source) {
        return serverBalance(source, "z_coin");
    }

    private static int serverBalance(CommandSourceStack source, String currency) {
        double balance = CurrencyHelper.getPlayerCurrencyServerData().getBalance(CurrencyHelper.getServerAccountUUID(), currency).value;
        source.sendSuccess(() -> Component.literal(
            "Server treasury " + currency + "=" + String.format("%.2f", balance)
                + " | reserved=" + String.format("%.2f", ZEconomy.EXTRA_DATA.getTotalDeposited(currency))
                + " | spendable=" + String.format("%.2f", ZEconomy.EXTRA_DATA.getServerSpendable(currency))
        ), false);
        return 1;
    }

    private static int serverSet(CommandSourceStack source, String currency, double amount) {
        CurrencyHelper.getPlayerCurrencyServerData().setCurrencyValue(CurrencyHelper.getServerAccountUUID(), currency, amount);
        source.sendSuccess(() -> Component.literal("Server treasury set: " + currency + "=" + String.format("%.2f", amount)), true);
        return 1;
    }

    private static int serverGive(CommandSourceStack source, String currency, double amount) {
        CurrencyHelper.getPlayerCurrencyServerData().addCurrencyValue(CurrencyHelper.getServerAccountUUID(), currency, amount);
        double balance = CurrencyHelper.getPlayerCurrencyServerData().getBalance(CurrencyHelper.getServerAccountUUID(), currency).value;
        source.sendSuccess(() -> Component.literal("Server treasury +" + String.format("%.2f", amount) + " " + currency + " => " + String.format("%.2f", balance)), true);
        return 1;
    }

    private static int serverTake(CommandSourceStack source, String currency, double amount) {
        double spendable = ZEconomy.EXTRA_DATA.getServerSpendable(currency);
        if (spendable < amount) {
            source.sendFailure(Component.literal("Server treasury insufficient spendable balance: " + String.format("%.2f", spendable)));
            return 0;
        }
        CurrencyHelper.getPlayerCurrencyServerData().addCurrencyValue(CurrencyHelper.getServerAccountUUID(), currency, -amount);
        double balance = CurrencyHelper.getPlayerCurrencyServerData().getBalance(CurrencyHelper.getServerAccountUUID(), currency).value;
        source.sendSuccess(() -> Component.literal("Server treasury -" + String.format("%.2f", amount) + " " + currency + " => " + String.format("%.2f", balance)), true);
        return 1;
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
        java.util.Set<java.util.UUID> targetIds = collectCurrencyTargetIds(source);
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
        java.util.Set<String> registryCurrencies = net.sixik.zeconomy.currencies.data.CurrencyData.SERVER.currencies.stream()
            .map(net.sixik.zeconomy.currencies.BaseCurrency::getName)
            .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        int players = 0;
        int missingEntries = 0;
        int danglingEntries = 0;
        for (java.util.Map.Entry<java.util.UUID, java.util.LinkedList<CurrencyPlayerData.PlayerCurrency>> entry : CurrencyPlayerData.SERVER.playersCurrencyMap.entrySet()) {
            players++;
            java.util.Set<String> playerCurrencies = entry.getValue().stream()
                .map(pc -> pc.currency.getName())
                .collect(java.util.stream.Collectors.toSet());
            for (String currencyId : registryCurrencies) {
                if (!playerCurrencies.contains(currencyId)) {
                    missingEntries++;
                }
            }
            for (CurrencyPlayerData.PlayerCurrency playerCurrency : entry.getValue()) {
                if (!registryCurrencies.contains(playerCurrency.currency.getName())) {
                    danglingEntries++;
                }
            }
        }
        final int playerCount = players;
        final int missingCount = missingEntries;
        final int danglingCount = danglingEntries;
        source.sendSuccess(() -> Component.translatable(
            "message.zeconomy.admin.currency.audit",
            registryCurrencies.size(),
            playerCount,
            missingCount,
            danglingCount
        ), false);
        return danglingEntries == 0 && missingEntries == 0 ? 1 : 0;
    }

    private static int adminCurrencyInspect(CommandSourceStack source, ServerPlayer player) {
        CurrencyPlayerData.SERVER.newPlayer(player);
        CurrencyInspectResult result = inspectCurrencyEntries(CurrencyPlayerData.SERVER.getPlayersCurrency(player));
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
        BaseCurrency currency = net.sixik.zeconomy.currencies.data.CurrencyData.SERVER.currencies.stream()
            .filter(entry -> id.equals(entry.getName()))
            .findFirst()
            .orElse(null);
        if (currency == null) {
            source.sendFailure(Component.translatable("message.zeconomy.admin.currency.invalid_id", id));
            return 0;
        }
        java.util.Set<java.util.UUID> targetIds = collectCurrencyTargetIds(source);
        int present = 0;
        int missing = 0;
        int locked = 0;
        source.sendSuccess(() -> Component.translatable(
            "message.zeconomy.admin.currency.players_header",
            id,
            targetIds.size()
        ), false);
        for (java.util.UUID playerId : targetIds) {
            java.util.Optional<CurrencyPlayerData.PlayerCurrency> entry = CurrencyPlayerData.SERVER.getPlayerCurrency(playerId, id);
            String playerName = currencyPlayerName(source, playerId);
            if (entry.isPresent()) {
                present++;
                CurrencyPlayerData.PlayerCurrency playerCurrency = entry.get();
                if (playerCurrency.isLocked) {
                    locked++;
                }
                final String finalPlayerName = playerName;
                final double balance = playerCurrency.balance;
                final String lockStateKey = playerCurrency.isLocked
                    ? "message.zeconomy.admin.currency.players_locked"
                    : "message.zeconomy.admin.currency.players_unlocked";
                source.sendSuccess(() -> Component.translatable(
                    "message.zeconomy.admin.currency.players_line_present",
                    finalPlayerName,
                    String.format("%.2f", balance),
                    Component.translatable(lockStateKey)
                ), false);
            } else {
                missing++;
                final String finalPlayerName = playerName;
                source.sendSuccess(() -> Component.translatable(
                    "message.zeconomy.admin.currency.players_line_missing",
                    finalPlayerName
                ), false);
            }
        }
        final int presentCount = present;
        final int missingCount = missing;
        final int lockedCount = locked;
        source.sendSuccess(() -> Component.translatable(
            "message.zeconomy.admin.currency.players_summary",
            id,
            presentCount,
            missingCount,
            lockedCount
        ), false);
        return missing == 0 ? 1 : 0;
    }

    private static int adminCurrencyRepair(CommandSourceStack source, ServerPlayer player) {
        CurrencyPlayerData.SERVER.newPlayer(player);
        CurrencyRepairResult result = repairCurrencyEntries(player.getUUID(), CurrencyPlayerData.SERVER.getPlayersCurrency(player));
        CurrencyHelper.syncPlayer(player);
        if (source.getServer() != null) {
            CurrencyHelper.saveAll(source.getServer());
        }
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
        java.util.Set<java.util.UUID> targetIds = collectCurrencyTargetIds(source);
        int checked = 0;
        int problematic = 0;
        source.sendSuccess(() -> Component.translatable("message.zeconomy.admin.currency.issues_header"), false);
        for (java.util.UUID playerId : targetIds) {
            CurrencyInspectResult result = inspectCurrencyEntries(CurrencyPlayerData.SERVER.getPlayersCurrency(playerId));
            checked++;
            if (result.missing() == 0 && result.dangling() == 0) {
                continue;
            }
            problematic++;
            String playerName = currencyPlayerName(source, playerId);
            final String finalPlayerName = playerName;
            source.sendSuccess(() -> Component.translatable(
                "message.zeconomy.admin.currency.issues_line",
                finalPlayerName,
                result.entries(),
                result.missing(),
                result.dangling(),
                result.locked()
            ), false);
        }
        final int checkedCount = checked;
        final int problematicCount = problematic;
        final boolean clean = problematic == 0;
        source.sendSuccess(() -> Component.translatable(
            clean
                ? "message.zeconomy.admin.currency.issues_clean"
                : "message.zeconomy.admin.currency.issues_summary",
            checkedCount,
            problematicCount
        ), false);
        return clean ? 1 : 0;
    }

    private static int adminCurrencySummary(CommandSourceStack source) {
        if (source.getServer() == null) {
            return 0;
        }
        java.util.Set<java.util.UUID> targetIds = collectCurrencyTargetIds(source);
        final int playerCount = targetIds.size();
        source.sendSuccess(() -> Component.translatable(
            "message.zeconomy.admin.currency.summary_header",
            playerCount
        ), false);
        for (BaseCurrency currency : net.sixik.zeconomy.currencies.data.CurrencyData.SERVER.currencies) {
            int present = 0;
            int missing = 0;
            int locked = 0;
            for (java.util.UUID playerId : targetIds) {
                java.util.Optional<CurrencyPlayerData.PlayerCurrency> entry = CurrencyPlayerData.SERVER.getPlayerCurrency(playerId, currency.getName());
                if (entry.isPresent()) {
                    present++;
                    if (entry.get().isLocked) {
                        locked++;
                    }
                } else {
                    missing++;
                }
            }
            final String currencyId = currency.getName();
            final int presentCount = present;
            final int missingCount = missing;
            final int lockedCount = locked;
            source.sendSuccess(() -> Component.translatable(
                "message.zeconomy.admin.currency.summary_line",
                currencyId,
                presentCount,
                missingCount,
                lockedCount
            ), false);
        }
        return 1;
    }

    private static int adminCurrencyRepairIssues(CommandSourceStack source) {
        if (source.getServer() == null) {
            return 0;
        }
        java.util.Set<java.util.UUID> targetIds = collectCurrencyTargetIds(source);
        int repairedPlayers = 0;
        int added = 0;
        int removed = 0;
        for (java.util.UUID playerId : targetIds) {
            java.util.LinkedList<CurrencyPlayerData.PlayerCurrency> list = CurrencyPlayerData.SERVER.getPlayersCurrency(playerId);
            CurrencyInspectResult inspect = inspectCurrencyEntries(list);
            if (inspect.missing() == 0 && inspect.dangling() == 0) {
                continue;
            }
            CurrencyRepairResult result = repairCurrencyEntries(playerId, list);
            repairedPlayers++;
            added += result.added();
            removed += result.removed();
            ServerPlayer online = source.getServer().getPlayerList().getPlayer(playerId);
            if (online != null) {
                CurrencyHelper.syncPlayer(online);
            }
        }
        CurrencyHelper.saveAll(source.getServer());
        final int repairedCount = repairedPlayers;
        final int addedCount = added;
        final int removedCount = removed;
        final boolean clean = repairedPlayers == 0;
        source.sendSuccess(() -> Component.translatable(
            clean
                ? "message.zeconomy.admin.currency.repairissues_clean"
                : "message.zeconomy.admin.currency.repairissues",
            repairedCount,
            addedCount,
            removedCount
        ), true);
        return 1;
    }

    private static int adminCurrencyRepairAll(CommandSourceStack source) {
        if (source.getServer() == null) {
            return 0;
        }
        int players = 0;
        int added = 0;
        int removed = 0;
        for (java.util.Map.Entry<java.util.UUID, java.util.LinkedList<CurrencyPlayerData.PlayerCurrency>> entry : CurrencyPlayerData.SERVER.playersCurrencyMap.entrySet()) {
            players++;
            CurrencyRepairResult result = repairCurrencyEntries(entry.getKey(), entry.getValue());
            added += result.added();
            removed += result.removed();
        }
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            CurrencyPlayerData.SERVER.newPlayer(player);
            CurrencyHelper.syncPlayer(player);
        }
        CurrencyHelper.saveAll(source.getServer());
        final int playerCount = players;
        final int addedCount = added;
        final int removedCount = removed;
        source.sendSuccess(() -> Component.translatable(
            "message.zeconomy.admin.currency.repairall",
            playerCount,
            addedCount,
            removedCount
        ), true);
        return 1;
    }

    private static java.util.Set<java.util.UUID> collectCurrencyTargetIds(CommandSourceStack source) {
        java.util.Set<java.util.UUID> targetIds = new java.util.LinkedHashSet<>(CurrencyPlayerData.SERVER.playersCurrencyMap.keySet());
        if (source.getServer() == null) {
            return targetIds;
        }
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            CurrencyPlayerData.SERVER.newPlayer(player);
            targetIds.add(player.getUUID());
        }
        return targetIds;
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

    private static CurrencyInspectResult inspectCurrencyEntries(java.util.LinkedList<CurrencyPlayerData.PlayerCurrency> currencies) {
        java.util.Set<String> registryCurrencies = net.sixik.zeconomy.currencies.data.CurrencyData.SERVER.currencies.stream()
            .map(net.sixik.zeconomy.currencies.BaseCurrency::getName)
            .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        java.util.Set<String> playerCurrencyIds = currencies.stream()
            .map(pc -> pc.currency.getName())
            .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        int lockedCount = 0;
        int missingCount = 0;
        int danglingCount = 0;
        for (CurrencyPlayerData.PlayerCurrency currency : currencies) {
            if (currency.isLocked) {
                lockedCount++;
            }
            if (!registryCurrencies.contains(currency.currency.getName())) {
                danglingCount++;
            }
        }
        for (String currencyId : registryCurrencies) {
            if (!playerCurrencyIds.contains(currencyId)) {
                missingCount++;
            }
        }
        return new CurrencyInspectResult(registryCurrencies.size(), currencies.size(), lockedCount, missingCount, danglingCount);
    }

    private static CurrencyRepairResult repairCurrencyEntries(java.util.UUID playerId, java.util.LinkedList<CurrencyPlayerData.PlayerCurrency> list) {
        java.util.Map<String, net.sixik.zeconomy.currencies.BaseCurrency> registry = net.sixik.zeconomy.currencies.data.CurrencyData.SERVER.currencies.stream()
            .collect(java.util.stream.Collectors.toMap(
                net.sixik.zeconomy.currencies.BaseCurrency::getName,
                net.sixik.zeconomy.currencies.BaseCurrency::copy,
                (a, b) -> a,
                java.util.LinkedHashMap::new
            ));
        int added = 0;
        int removed = 0;
        java.util.Set<String> seen = new java.util.HashSet<>();
        java.util.Iterator<CurrencyPlayerData.PlayerCurrency> iterator = list.iterator();
        while (iterator.hasNext()) {
            CurrencyPlayerData.PlayerCurrency playerCurrency = iterator.next();
            String currencyId = playerCurrency.currency.getName();
            if (!registry.containsKey(currencyId) || !seen.add(currencyId)) {
                iterator.remove();
                removed++;
            }
        }
        for (java.util.Map.Entry<String, net.sixik.zeconomy.currencies.BaseCurrency> currencyEntry : registry.entrySet()) {
            if (!seen.contains(currencyEntry.getKey())) {
                list.add(new CurrencyPlayerData.PlayerCurrency(currencyEntry.getValue().copy(), currencyEntry.getValue().getDefaultValue()));
                added++;
            }
        }
        return new CurrencyRepairResult(playerId, added, removed);
    }

    private static int adminStatus(CommandSourceStack source) {
        if (source.getServer() == null) {
            return 0;
        }
        String mode = EconomyConfig.STORAGE_MODE.get();
        String storageTarget = storageTarget(source.getServer());
        source.sendSuccess(() -> Component.translatable("message.zeconomy.admin.status.header"), false);
        source.sendSuccess(() -> Component.translatable(
            "message.zeconomy.admin.status.storage",
            mode,
            storageTarget
        ), false);
        source.sendSuccess(() -> Component.translatable(
            "message.zeconomy.admin.status.files",
            CurrencyHelper.currencyDataPath(source.getServer()),
            CurrencyHelper.playerDataPath(source.getServer()),
            CurrencyHelper.customDataPath(source.getServer()),
            CurrencyHelper.extraDataPath(source.getServer())
        ), false);
        source.sendSuccess(() -> Component.translatable(
            "message.zeconomy.admin.status.vault",
            EconomyConfig.ENABLE_VAULT_BRIDGE.get() ? "on" : "off",
            VaultBridge.isAvailable() ? "available" : "offline",
            VaultBridge.getProviderName(),
            EconomyConfig.VAULT_SYNC_CURRENCY_ID.get()
        ), false);
        source.sendSuccess(() -> Component.translatable(
            "message.zeconomy.admin.status.runtime",
            source.getServer().getPlayerList().getPlayerCount(),
            ZEconomyEvents.pendingVaultSyncCount(),
            ZEconomy.EXTRA_DATA.getAllRates().size(),
            net.sixik.zeconomy.currencies.data.CurrencyData.SERVER.currencies.size()
        ), false);
        return 1;
    }

    private static int adminSave(CommandSourceStack source) {
        if (source.getServer() == null) {
            return 0;
        }
        CurrencyHelper.saveAll(source.getServer());
        source.sendSuccess(() -> Component.translatable("message.zeconomy.admin.save.success", storageTarget(source.getServer())), true);
        return 1;
    }

    private static int adminBackup(CommandSourceStack source) {
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

    private static int adminReloadStorage(CommandSourceStack source) {
        if (source.getServer() == null) {
            return 0;
        }
        DataStorageManager.loadAll(source.getServer());
        CurrencyHelper.ensureDefaultCurrency();
        ZEconomy.EXTRA_DATA.ensureDefaultRates();
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            CurrencyPlayerData.SERVER.newPlayer(player);
            CustomPlayerData.SERVER.createData(player);
            ZEconomy.EXTRA_DATA.syncPlayerMirror(player);
            CurrencyHelper.syncPlayer(player);
            CurrencyHelper.syncCustomData(player);
        }
        CurrencyHelper.syncCurrencyData(source.getServer());
        source.sendSuccess(() -> Component.translatable(
            "message.zeconomy.admin.reloadstorage.success",
            EconomyConfig.STORAGE_MODE.get(),
            source.getServer().getPlayerList().getPlayerCount()
        ), true);
        return 1;
    }

    private static int adminExportNow(CommandSourceStack source) {
        if (source.getServer() == null) {
            return 0;
        }
        ZEconomy.EXTRA_DATA.exportJson(source.getServer(), CurrencyHelper.exportJsonPath(source.getServer()));
        source.sendSuccess(() -> Component.translatable("message.zeconomy.admin.exportnow.success", CurrencyHelper.exportJsonPath(source.getServer())), true);
        return 1;
    }

    private static int adminExportStatus(CommandSourceStack source) {
        if (source.getServer() == null) {
            return 0;
        }
        Path exportPath = CurrencyHelper.exportJsonPath(source.getServer());
        boolean exists = Files.exists(exportPath);
        String size = exists ? Long.toString(safeFileSize(exportPath)) : "0";
        String modified = exists ? TS.format(Instant.ofEpochMilli(safeLastModified(exportPath))) : "never";
        source.sendSuccess(() -> Component.translatable(
            "message.zeconomy.admin.exportstatus",
            exportPath,
            exists ? "yes" : "no",
            size,
            modified,
            EconomyConfig.EXPORT_INTERVAL_SECONDS.get()
        ), false);
        return 1;
    }

    private static int adminDoctor(CommandSourceStack source) {
        if (source.getServer() == null) {
            return 0;
        }
        boolean hasZ = net.sixik.zeconomy.currencies.data.CurrencyData.SERVER.currencies.stream().anyMatch(c -> "z_coin".equals(c.getName()));
        boolean hasB = net.sixik.zeconomy.currencies.data.CurrencyData.SERVER.currencies.stream().anyMatch(c -> "b_coin".equals(c.getName()));
        boolean currencyFile = java.nio.file.Files.exists(CurrencyHelper.currencyDataPath(source.getServer()));
        boolean playerFile = java.nio.file.Files.exists(CurrencyHelper.playerDataPath(source.getServer()));
        boolean customFile = java.nio.file.Files.exists(CurrencyHelper.customDataPath(source.getServer()));
        boolean extraFile = java.nio.file.Files.exists(CurrencyHelper.extraDataPath(source.getServer()));
        String storageHealth = storageHealth(source.getServer());
        String vaultHealth = !EconomyConfig.ENABLE_VAULT_BRIDGE.get()
            ? "disabled"
            : (VaultBridge.isAvailable() ? "ok" : "offline");
        source.sendSuccess(() -> Component.translatable("message.zeconomy.admin.doctor.header"), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.admin.doctor.currencies", hasZ ? "ok" : "missing", hasB ? "ok" : "missing"), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.admin.doctor.storage", EconomyConfig.STORAGE_MODE.get(), storageHealth), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.admin.doctor.files", yesNo(currencyFile), yesNo(playerFile), yesNo(customFile), yesNo(extraFile)), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.admin.doctor.vault", vaultHealth, ZEconomyEvents.pendingVaultSyncCount(), VaultBridge.getProviderName()), false);
        return 1;
    }

    private static int adminInspect(CommandSourceStack source, ServerPlayer player) {
        String syncCurrency = EconomyConfig.VAULT_SYNC_CURRENCY_ID.get();
        double syncBalance = CurrencyHelper.getPlayerCurrencyServerData().getBalance(player, syncCurrency).value;
        net.minecraft.nbt.CompoundTag custom = CustomPlayerData.SERVER.getPlayerCustomData(player).nbt;
        source.sendSuccess(() -> Component.translatable("message.zeconomy.admin.inspect.header", player.getName().getString(), player.getUUID()), false);
        source.sendSuccess(() -> Component.translatable(
            "message.zeconomy.admin.inspect.summary",
            ZEconomy.EXTRA_DATA.pendingMailCount(player.getUUID()),
            ZEconomy.EXTRA_DATA.getDailyStreak(player.getUUID()),
            custom.getBoolean("vault_has_pin") ? "yes" : "no",
            syncCurrency,
            String.format("%.2f", syncBalance)
        ), false);
        source.sendSuccess(() -> Component.translatable("message.zeconomy.admin.inspect.wallet"), false);
        for (net.sixik.zeconomy.currencies.data.CurrencyPlayerData.PlayerCurrency currency : CurrencyHelper.getPlayerCurrencyServerData().getPlayersCurrency(player)) {
            source.sendSuccess(() -> Component.translatable(
                "message.zeconomy.admin.inspect.wallet_line",
                currency.currency.getName(),
                String.format("%.2f", currency.balance),
                currency.isLocked ? "yes" : "no"
            ), false);
        }
        source.sendSuccess(() -> Component.translatable("message.zeconomy.admin.inspect.bank"), false);
        printSourceMap(source, ZEconomy.EXTRA_DATA.getAllDeposits(player.getUUID()));
        source.sendSuccess(() -> Component.translatable("message.zeconomy.admin.inspect.vault"), false);
        printSourceMap(source, ZEconomy.EXTRA_DATA.getAllVaultBalances(player.getUUID()));
        source.sendSuccess(() -> Component.translatable(
            "message.zeconomy.admin.inspect.mirror",
            String.format("%.2f", custom.getDouble("bank_z_coin")),
            String.format("%.2f", custom.getDouble("bank_b_coin")),
            String.format("%.2f", custom.getDouble("vault_z_coin")),
            String.format("%.2f", custom.getDouble("vault_b_coin")),
            custom.getInt("mail_pending"),
            custom.getInt("daily_streak")
        ), false);
        return 1;
    }

    private static int adminReconcile(CommandSourceStack source, ServerPlayer player) {
        applyReconcile(player, source.getServer() != null);
        String syncCurrency = EconomyConfig.VAULT_SYNC_CURRENCY_ID.get();
        double syncBalance = CurrencyHelper.getPlayerCurrencyServerData().getBalance(player, syncCurrency).value;
        source.sendSuccess(() -> Component.translatable(
            "message.zeconomy.admin.reconcile.success",
            player.getName().getString(),
            CurrencyPlayerData.SERVER.getPlayersCurrency(player).size(),
            ZEconomy.EXTRA_DATA.pendingMailCount(player.getUUID()),
            syncCurrency,
            String.format("%.2f", syncBalance)
        ), true);
        return 1;
    }

    private static int adminDoctorFix(CommandSourceStack source, ServerPlayer player) {
        DoctorFixResult result = applyDoctorFix(player, false);
        if (source.getServer() != null) {
            CurrencyHelper.saveAll(source.getServer());
        }
        String syncCurrency = EconomyConfig.VAULT_SYNC_CURRENCY_ID.get();
        double syncBalance = CurrencyHelper.getPlayerCurrencyServerData().getBalance(player, syncCurrency).value;
        source.sendSuccess(() -> Component.translatable(
            "message.zeconomy.admin.doctorfix.success",
            player.getName().getString(),
            CurrencyPlayerData.SERVER.getPlayersCurrency(player).size(),
            ZEconomy.EXTRA_DATA.pendingMailCount(player.getUUID()),
            result.vaultAttempted() ? "yes" : "no",
            result.vaultOk() ? "ok" : "skip",
            syncCurrency,
            String.format("%.2f", syncBalance)
        ), true);
        return 1;
    }

    private static int adminDoctorFixAll(CommandSourceStack source) {
        if (source.getServer() == null) {
            return 0;
        }
        int players = 0;
        int vaultAttempted = 0;
        int vaultOk = 0;
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            DoctorFixResult result = applyDoctorFix(player, false);
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

    private static int adminSyncVault(CommandSourceStack source, ServerPlayer player) {
        if (!EconomyConfig.ENABLE_VAULT_BRIDGE.get()) {
            source.sendFailure(Component.translatable("message.zeconomy.admin.syncvault.disabled"));
            return 0;
        }
        CurrencyHelper.initVaultBridge(player.server);
        if (!VaultBridge.isAvailable()) {
            source.sendFailure(Component.translatable("message.zeconomy.admin.syncvault.unavailable"));
            return 0;
        }
        boolean ok = CurrencyHelper.syncFromVaultOnJoin(player);
        if (!ok) {
            source.sendFailure(Component.translatable(
                "message.zeconomy.admin.syncvault.failed",
                player.getName().getString(),
                EconomyConfig.VAULT_SYNC_CURRENCY_ID.get()
            ));
            return 0;
        }
        ZEconomy.EXTRA_DATA.syncPlayerMirror(player);
        CurrencyHelper.syncPlayer(player);
        CurrencyHelper.syncCustomData(player);
        source.sendSuccess(() -> Component.translatable(
            "message.zeconomy.admin.syncvault.success",
            player.getName().getString(),
            EconomyConfig.VAULT_SYNC_CURRENCY_ID.get(),
            String.format("%.2f", CurrencyHelper.getPlayerCurrencyServerData().getBalance(player, EconomyConfig.VAULT_SYNC_CURRENCY_ID.get()).value)
        ), true);
        return 1;
    }

    private static String storageTarget(net.minecraft.server.MinecraftServer server) {
        String mode = EconomyConfig.STORAGE_MODE.get();
        if ("json".equalsIgnoreCase(mode)) {
            return server.getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve("zeconomy").resolve(EconomyConfig.JSON_FILE_NAME.get()).toString();
        }
        if ("sqlite".equalsIgnoreCase(mode)) {
            return server.getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve("zeconomy").resolve(EconomyConfig.SQLITE_FILE_NAME.get()).toString();
        }
        if ("mysql".equalsIgnoreCase(mode)) {
            return EconomyConfig.MYSQL_HOST.get() + ":" + EconomyConfig.MYSQL_PORT.get() + "/" + EconomyConfig.MYSQL_DATABASE.get() + " [" + EconomyConfig.MYSQL_TABLE.get() + "]";
        }
        return server.getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve("zeconomy").toString();
    }

    private static String storageHealth(net.minecraft.server.MinecraftServer server) {
        String mode = EconomyConfig.STORAGE_MODE.get();
        if ("json".equalsIgnoreCase(mode)) {
            return java.nio.file.Files.exists(server.getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve("zeconomy").resolve(EconomyConfig.JSON_FILE_NAME.get())) ? "ok" : "pending";
        }
        if ("sqlite".equalsIgnoreCase(mode)) {
            return java.nio.file.Files.exists(server.getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve("zeconomy").resolve(EconomyConfig.SQLITE_FILE_NAME.get())) ? "ok" : "pending";
        }
        if ("mysql".equalsIgnoreCase(mode)) {
            return "external";
        }
        return java.nio.file.Files.exists(CurrencyHelper.currencyDataPath(server)) ? "ok" : "pending";
    }

    private static String yesNo(boolean value) {
        return value ? "yes" : "no";
    }

    private static int copyIfExists(Path source, Path backupRoot) throws java.io.IOException {
        if (source == null || !Files.exists(source)) {
            return 0;
        }
        Path target = backupRoot.resolve(source.getFileName().toString());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        return 1;
    }

    private static long safeFileSize(Path path) {
        try {
            return Files.size(path);
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private static long safeLastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private static void applyReconcile(ServerPlayer player, boolean saveAfter) {
        CurrencyHelper.ensureDefaultCurrency();
        CurrencyPlayerData.SERVER.newPlayer(player);
        CustomPlayerData.SERVER.createData(player);
        ZEconomy.EXTRA_DATA.ensureDefaultRates();
        ZEconomy.EXTRA_DATA.syncPlayerMirror(player);
        CurrencyHelper.syncPlayer(player);
        CurrencyHelper.syncCustomData(player);
        if (saveAfter && player.server != null) {
            CurrencyHelper.saveAll(player.server);
        }
    }

    private static DoctorFixResult applyDoctorFix(ServerPlayer player, boolean saveAfter) {
        applyReconcile(player, false);
        boolean vaultAttempted = false;
        boolean vaultOk = false;
        if (EconomyConfig.ENABLE_VAULT_BRIDGE.get()) {
            vaultAttempted = true;
            CurrencyHelper.initVaultBridge(player.server);
            if (VaultBridge.isAvailable()) {
                vaultOk = CurrencyHelper.syncFromVaultOnJoin(player);
                if (vaultOk) {
                    ZEconomy.EXTRA_DATA.syncPlayerMirror(player);
                    CurrencyHelper.syncPlayer(player);
                    CurrencyHelper.syncCustomData(player);
                }
            }
        }
        if (saveAfter && player.server != null) {
            CurrencyHelper.saveAll(player.server);
        }
        return new DoctorFixResult(vaultAttempted, vaultOk);
    }

    private record DoctorFixResult(boolean vaultAttempted, boolean vaultOk) {
    }

    private record CurrencyInspectResult(int registered, int entries, int locked, int missing, int dangling) {
    }

    private record CurrencyRepairResult(java.util.UUID playerId, int added, int removed) {
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

    private static int setExchangeBlockOffer(ServerPlayer player, BlockPos pos, String inputItem, int inputCount, String outputItem, int outputCount) {
        BlockEntity be = player.serverLevel().getBlockEntity(pos);
        if (!(be instanceof ExchangeBlockEntity exchange)) {
            player.sendSystemMessage(Component.literal("No exchange block at this position").withStyle(ChatFormatting.RED));
            return 0;
        }
        if (exchange.getOwner() != null && !exchange.getOwner().equals(player.getUUID()) && !player.hasPermissions(2)) {
            player.sendSystemMessage(Component.literal("Not owner of exchange block").withStyle(ChatFormatting.RED));
            return 0;
        }
        ResourceLocation inId = ResourceLocation.tryParse(inputItem);
        ResourceLocation outId = ResourceLocation.tryParse(outputItem);
        if (inId == null || outId == null || !BuiltInRegistries.ITEM.containsKey(inId) || !BuiltInRegistries.ITEM.containsKey(outId)) {
            player.sendSystemMessage(Component.literal("Invalid item id").withStyle(ChatFormatting.RED));
            return 0;
        }
        exchange.setOwner(player.getUUID());
        exchange.setOffer(inputItem, inputCount, outputItem, outputCount);
        player.sendSystemMessage(Component.literal("Exchange offer saved").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static class MapToLines {
        static void print(ServerPlayer player, String header, java.util.Map<String, Double> values) {
            if (values.isEmpty()) {
                player.sendSystemMessage(Component.literal(header + ": empty").withStyle(ChatFormatting.GRAY));
                return;
            }
            player.sendSystemMessage(Component.literal(header + ":").withStyle(ChatFormatting.GOLD));
            for (java.util.Map.Entry<String, Double> e : values.entrySet()) {
                player.sendSystemMessage(Component.literal("- " + e.getKey() + ": " + String.format("%.2f", e.getValue())));
            }
        }
    }
}
