package io.zicteam.zeconomy.commands;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.zicteam.zeconomy.permissions.ModPermissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;

final class AdminCommandTree {
    private AdminCommandTree() {
    }

    static void append(LiteralArgumentBuilder<CommandSourceStack> root) {
        appendAdminCommands(root);
        appendServerCommands(root);
    }

    private static void appendAdminCommands(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("admin")
            .requires(src -> EconomyCommands.hasAdminPermission(src, ModPermissions.ADMIN_SET))
            .then(Commands.literal("set")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("currency", StringArgumentType.word())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0D))
                            .executes(ctx -> EconomyCommands.adminSet(
                                ctx.getSource(),
                                EntityArgument.getPlayer(ctx, "player"),
                                StringArgumentType.getString(ctx, "currency"),
                                DoubleArgumentType.getDouble(ctx, "amount")))))))
            .then(AdminCurrencyCommands.adminCurrencyNode())
            .then(Commands.literal("status")
                .requires(src -> EconomyCommands.hasAdminPermission(src, ModPermissions.ADMIN_STATUS))
                .executes(ctx -> AdminMaintenanceCommands.adminStatus(ctx.getSource())))
            .then(Commands.literal("save")
                .requires(src -> EconomyCommands.hasAdminPermission(src, ModPermissions.ADMIN_STATUS))
                .executes(ctx -> AdminMaintenanceCommands.adminSave(ctx.getSource())))
            .then(Commands.literal("backup")
                .requires(src -> EconomyCommands.hasAdminPermission(src, ModPermissions.ADMIN_STATUS))
                .executes(ctx -> AdminMaintenanceCommands.adminBackup(ctx.getSource())))
            .then(Commands.literal("reloadstorage")
                .requires(src -> EconomyCommands.hasAdminPermission(src, ModPermissions.ADMIN_RELOAD))
                .executes(ctx -> AdminMaintenanceCommands.adminReloadStorage(ctx.getSource())))
            .then(Commands.literal("exportnow")
                .requires(src -> EconomyCommands.hasAdminPermission(src, ModPermissions.ADMIN_EXPORT))
                .executes(ctx -> AdminMaintenanceCommands.adminExportNow(ctx.getSource())))
            .then(Commands.literal("exportstatus")
                .requires(src -> EconomyCommands.hasAdminPermission(src, ModPermissions.ADMIN_EXPORT))
                .executes(ctx -> AdminMaintenanceCommands.adminExportStatus(ctx.getSource())))
            .then(Commands.literal("doctor")
                .requires(src -> EconomyCommands.hasAdminPermission(src, ModPermissions.ADMIN_STATUS))
                .executes(ctx -> AdminMaintenanceCommands.adminDoctor(ctx.getSource())))
            .then(Commands.literal("doctorfix")
                .requires(src -> EconomyCommands.hasAdminPermission(src, ModPermissions.ADMIN_STATUS))
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> AdminMaintenanceCommands.adminDoctorFix(
                        ctx.getSource(),
                        EntityArgument.getPlayer(ctx, "player")))))
            .then(Commands.literal("doctorfixall")
                .requires(src -> EconomyCommands.hasAdminPermission(src, ModPermissions.ADMIN_STATUS))
                .executes(ctx -> AdminMaintenanceCommands.adminDoctorFixAll(ctx.getSource())))
            .then(Commands.literal("inspect")
                .requires(src -> EconomyCommands.hasAdminPermission(src, ModPermissions.ADMIN_STATUS))
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> AdminMaintenanceCommands.adminInspect(
                        ctx.getSource(),
                        EntityArgument.getPlayer(ctx, "player")))))
            .then(Commands.literal("reconcile")
                .requires(src -> EconomyCommands.hasAdminPermission(src, ModPermissions.ADMIN_STATUS))
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> AdminMaintenanceCommands.adminReconcile(
                        ctx.getSource(),
                        EntityArgument.getPlayer(ctx, "player")))))
            .then(Commands.literal("syncvault")
                .requires(src -> EconomyCommands.hasAdminPermission(src, ModPermissions.ADMIN_STATUS))
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> AdminMaintenanceCommands.adminSyncVault(
                        ctx.getSource(),
                        EntityArgument.getPlayer(ctx, "player"))))));
    }

    private static void appendServerCommands(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("server")
            .requires(src -> EconomyCommands.hasAdminPermission(src, ModPermissions.ADMIN_SET))
            .then(Commands.literal("balance")
                .executes(ctx -> EconomyCommands.serverBalance(ctx.getSource()))
                .then(Commands.argument("currency", StringArgumentType.word())
                    .executes(ctx -> EconomyCommands.serverBalance(ctx.getSource(), StringArgumentType.getString(ctx, "currency")))))
            .then(Commands.literal("set")
                .then(Commands.argument("currency", StringArgumentType.word())
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0D))
                        .executes(ctx -> EconomyCommands.serverSet(
                            ctx.getSource(),
                            StringArgumentType.getString(ctx, "currency"),
                            DoubleArgumentType.getDouble(ctx, "amount"))))))
            .then(Commands.literal("give")
                .then(Commands.argument("currency", StringArgumentType.word())
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0001D))
                        .executes(ctx -> EconomyCommands.serverGive(
                            ctx.getSource(),
                            StringArgumentType.getString(ctx, "currency"),
                            DoubleArgumentType.getDouble(ctx, "amount"))))))
            .then(Commands.literal("take")
                .then(Commands.argument("currency", StringArgumentType.word())
                    .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0001D))
                        .executes(ctx -> EconomyCommands.serverTake(
                            ctx.getSource(),
                            StringArgumentType.getString(ctx, "currency"),
                            DoubleArgumentType.getDouble(ctx, "amount")))))));
    }
}
