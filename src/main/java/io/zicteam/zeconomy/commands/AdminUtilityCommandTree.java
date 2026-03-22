package io.zicteam.zeconomy.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.zicteam.zeconomy.permissions.ModPermissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

final class AdminUtilityCommandTree {
    private AdminUtilityCommandTree() {
    }

    static void append(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("logs")
            .requires(src -> EconomyCommands.hasAdminPermission(src, ModPermissions.ADMIN_LOGS))
            .then(Commands.argument("limit", IntegerArgumentType.integer(1, 100))
                .executes(ctx -> EconomyCommands.showLogs(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "limit"))))
            .executes(ctx -> EconomyCommands.showLogs(ctx.getSource(), 20)));

        root.then(Commands.literal("export")
            .requires(src -> EconomyCommands.hasAdminPermission(src, ModPermissions.ADMIN_EXPORT))
            .then(Commands.literal("now").executes(ctx -> EconomyCommands.exportNow(ctx.getSource()))));

        root.then(Commands.literal("reload")
            .requires(src -> EconomyCommands.hasAdminPermission(src, ModPermissions.ADMIN_RELOAD))
            .executes(ctx -> EconomyCommands.reloadMod(ctx.getSource())));
    }
}
