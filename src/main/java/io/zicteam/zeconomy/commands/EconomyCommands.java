package io.zicteam.zeconomy.commands;

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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.LevelResource;
import io.zicteam.zeconomy.CustomPlayerData;
import io.zicteam.zeconomy.ZEconomyEvents;
import io.zicteam.zeconomy.ZEconomy;
import io.zicteam.zeconomy.block.entity.ExchangeBlockEntity;
import io.zicteam.zeconomy.config.EconomyConfig;
import io.zicteam.zeconomy.config.GuiLayoutConfig;
import io.zicteam.zeconomy.currencies.BaseCurrency;
import io.zicteam.zeconomy.currencies.CurrencySymbol;
import io.zicteam.zeconomy.currencies.compat.VaultBridge;
import io.zicteam.zeconomy.currencies.data.CurrencyPlayerData;
import io.zicteam.zeconomy.permissions.ModPermissions;
import io.zicteam.zeconomy.system.AdminOperationService;
import io.zicteam.zeconomy.system.AdminReportService;
import io.zicteam.zeconomy.system.CurrencyAdminService;
import io.zicteam.zeconomy.system.DataStorageManager;
import io.zicteam.zeconomy.system.EconomyOperationService;
import io.zicteam.zeconomy.system.ExtraEconomyData;
import io.zicteam.zeconomy.network.ZEconomyNetwork;
import io.zicteam.zeconomy.utils.CurrencyHelper;
import io.zicteam.zeconomy.utils.ErrorCodes;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.permission.nodes.PermissionNode;

public final class EconomyCommands {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private EconomyCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("zeco");
        UserCommandTree.append(root);
        AdminUtilityCommandTree.append(root);
        AdminCommandTree.append(root);
        appendExchangeBlockCommands(root);
        dispatcher.register(root);
    }

    private static void appendExchangeBlockCommands(LiteralArgumentBuilder<CommandSourceStack> root) {
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
    }

    static boolean hasUserPermission(CommandSourceStack source, PermissionNode<Boolean> node) {
        return hasPermission(source, node, EconomyConfig.OP_LEVEL_MODERATOR.get());
    }

    static boolean hasAdminPermission(CommandSourceStack source, PermissionNode<Boolean> node) {
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

    static int showLogs(CommandSourceStack source, int limit) {
        java.util.List<ExtraEconomyData.TransactionRecord> records = ZEconomy.EXTRA_DATA.getRecentLogs(limit);
        source.sendSuccess(() -> Component.literal("Recent logs: " + records.size()), false);
        for (ExtraEconomyData.TransactionRecord r : records) {
            String line = TS.format(Instant.ofEpochSecond(r.epochSec())) + " | " + r.type() + " | " + r.currency() + " " + String.format("%.2f", r.amount()) + " fee=" + String.format("%.2f", r.fee()) + " note=" + r.note();
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    static int exportNow(CommandSourceStack source) {
        if (source.getServer() == null) {
            return 0;
        }
        ZEconomy.EXTRA_DATA.exportJson(source.getServer(), CurrencyHelper.exportJsonPath(source.getServer()));
        source.sendSuccess(() -> Component.literal("Exported: " + CurrencyHelper.exportJsonPath(source.getServer())), true);
        return 1;
    }

    static int reloadMod(CommandSourceStack source) {
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

    static int openGuiEditor(CommandSourceStack source, String target) {
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

    static int adminSet(CommandSourceStack source, ServerPlayer player, String currency, double amount) {
        AdminOperationService.setPlayerBalance(player, currency, amount);
        source.sendSuccess(() -> Component.literal("Balance set: " + player.getName().getString() + " " + currency + "=" + amount), true);
        return 1;
    }

    static int serverBalance(CommandSourceStack source) {
        return serverBalance(source, "z_coin");
    }

    static int serverBalance(CommandSourceStack source, String currency) {
        double balance = CurrencyHelper.getPlayerCurrencyServerData().getBalance(CurrencyHelper.getServerAccountUUID(), currency).value;
        source.sendSuccess(() -> Component.literal(
            "Server treasury " + currency + "=" + String.format("%.2f", balance)
                + " | reserved=" + String.format("%.2f", ZEconomy.EXTRA_DATA.getTotalDeposited(currency))
                + " | spendable=" + String.format("%.2f", ZEconomy.EXTRA_DATA.getServerSpendable(currency))
        ), false);
        return 1;
    }

    static int serverSet(CommandSourceStack source, String currency, double amount) {
        AdminOperationService.setServerBalance(currency, amount);
        source.sendSuccess(() -> Component.literal("Server treasury set: " + currency + "=" + String.format("%.2f", amount)), true);
        return 1;
    }

    static int serverGive(CommandSourceStack source, String currency, double amount) {
        double balance = AdminOperationService.giveServerBalance(currency, amount);
        source.sendSuccess(() -> Component.literal("Server treasury +" + String.format("%.2f", amount) + " " + currency + " => " + String.format("%.2f", balance)), true);
        return 1;
    }

    static int serverTake(CommandSourceStack source, String currency, double amount) {
        AdminOperationService.TreasuryTakeResult result = AdminOperationService.takeServerBalance(currency, amount);
        if (!result.success()) {
            source.sendFailure(Component.literal("Server treasury insufficient spendable balance: " + String.format("%.2f", result.spendableBefore())));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Server treasury -" + String.format("%.2f", amount) + " " + currency + " => " + String.format("%.2f", result.balanceAfter())), true);
        return 1;
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
        if (inId == null || outId == null || !ForgeRegistries.ITEMS.containsKey(inId) || !ForgeRegistries.ITEMS.containsKey(outId)) {
            player.sendSystemMessage(Component.literal("Invalid item id").withStyle(ChatFormatting.RED));
            return 0;
        }
        exchange.setOwner(player.getUUID());
        exchange.setOffer(inputItem, inputCount, outputItem, outputCount);
        player.sendSystemMessage(Component.literal("Exchange offer saved").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    static class MapToLines {
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
