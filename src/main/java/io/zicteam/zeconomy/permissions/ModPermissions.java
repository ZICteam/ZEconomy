package io.zicteam.zeconomy.permissions;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;
import io.zicteam.zeconomy.ZEconomy;

public final class ModPermissions {
    public static final PermissionNode<Boolean> COMMAND_BALANCE = boolNode("command.balance");
    public static final PermissionNode<Boolean> COMMAND_PAY = boolNode("command.pay");
    public static final PermissionNode<Boolean> COMMAND_BANK = boolNode("command.bank");
    public static final PermissionNode<Boolean> COMMAND_EXCHANGE = boolNode("command.exchange");
    public static final PermissionNode<Boolean> COMMAND_MAIL = boolNode("command.mail");
    public static final PermissionNode<Boolean> COMMAND_VAULT = boolNode("command.vault");
    public static final PermissionNode<Boolean> COMMAND_DAILY = boolNode("command.daily");
    public static final PermissionNode<Boolean> COMMAND_TOP = boolNode("command.top");

    public static final PermissionNode<Boolean> ADMIN_SET = boolNode("admin.set");
    public static final PermissionNode<Boolean> ADMIN_LOGS = boolNode("admin.logs");
    public static final PermissionNode<Boolean> ADMIN_EXPORT = boolNode("admin.export");
    public static final PermissionNode<Boolean> ADMIN_RELOAD = boolNode("admin.reload");
    public static final PermissionNode<Boolean> ADMIN_EXCHANGE_RATE = boolNode("admin.exchange.rate");
    public static final PermissionNode<Boolean> ADMIN_EXCHANGEBLOCK_SET = boolNode("admin.exchangeblock.set");
    public static final PermissionNode<Boolean> ADMIN_CURRENCY = boolNode("admin.currency");
    public static final PermissionNode<Boolean> ADMIN_STATUS = boolNode("admin.status");

    private ModPermissions() {
    }

    private static PermissionNode<Boolean> boolNode(String path) {
        return new PermissionNode<>(
            ResourceLocation.fromNamespaceAndPath(ZEconomy.MOD_ID, path),
            PermissionTypes.BOOLEAN,
            (player, uuid, contexts) -> false
        );
    }

    public static void register(PermissionGatherEvent.Nodes event) {
        event.addNodes(List.of(
            COMMAND_BALANCE,
            COMMAND_PAY,
            COMMAND_BANK,
            COMMAND_EXCHANGE,
            COMMAND_MAIL,
            COMMAND_VAULT,
            COMMAND_DAILY,
            COMMAND_TOP,
            ADMIN_SET,
            ADMIN_LOGS,
            ADMIN_EXPORT,
            ADMIN_RELOAD,
            ADMIN_EXCHANGE_RATE,
            ADMIN_EXCHANGEBLOCK_SET,
            ADMIN_CURRENCY,
            ADMIN_STATUS
        ));
    }

    public static boolean check(net.minecraft.server.level.ServerPlayer player, PermissionNode<Boolean> node) {
        return PermissionAPI.getPermission(player, node);
    }
}
