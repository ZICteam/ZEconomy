package io.zicteam.zeconomy;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import io.zicteam.zeconomy.currencies.data.CurrencyData;
import io.zicteam.zeconomy.currencies.data.CurrencyPlayerData;
import io.zicteam.zeconomy.commands.EconomyCommands;
import io.zicteam.zeconomy.menu.BankMenu;
import io.zicteam.zeconomy.npc.BankNpcHelper;
import io.zicteam.zeconomy.permissions.ModPermissions;
import io.zicteam.zeconomy.system.DataStorageManager;
import io.zicteam.zeconomy.system.ExtraEconomyData;
import io.zicteam.zeconomy.utils.CurrencyHelper;

@Mod.EventBusSubscriber(modid = ZEconomy.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ZEconomyEvents {
    private static final int VAULT_SYNC_RETRY_INTERVAL_TICKS = 20;
    private static final int VAULT_SYNC_RETRY_WINDOW_TICKS = 20 * 30;
    private static int autosaveTicks = 0;
    private static final Map<UUID, Integer> pendingVaultSync = new ConcurrentHashMap<>();

    public static void init() {
        MinecraftForge.EVENT_BUS.register(ZEconomyEvents.class);
    }

    public static int pendingVaultSyncCount() {
        return pendingVaultSync.size();
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        CurrencyData.SERVER.server = server;
        CurrencyPlayerData.SERVER.server = server;
        DataStorageManager.loadAll(server);
        CurrencyHelper.ensureDefaultCurrency();
        CurrencyHelper.ensureServerAccount();
        ZEconomy.EXTRA_DATA.ensureDefaultRates();
        CurrencyHelper.initVaultBridge(server);
        ZEconomy.EXTRA_DATA.exportJson(server, CurrencyHelper.exportJsonPath(server));
        CurrencyHelper.syncCurrencyData(server);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        pendingVaultSync.clear();
        CurrencyHelper.saveAll(event.getServer());
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return;
        }
        CurrencyPlayerData.SERVER.newPlayer(serverPlayer);
        CustomPlayerData.SERVER.createData(serverPlayer);
        pendingVaultSync.put(serverPlayer.getUUID(), VAULT_SYNC_RETRY_WINDOW_TICKS);
        CurrencyHelper.syncFromVaultOnJoin(serverPlayer);
        ZEconomy.EXTRA_DATA.syncPlayerMirror(serverPlayer);
        CurrencyHelper.syncPlayer(serverPlayer);
        CurrencyHelper.syncCustomData(serverPlayer);
    }

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        pendingVaultSync.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        autosaveTicks++;
        if (autosaveTicks >= 20 * 60) {
            autosaveTicks = 0;
            if (event.getServer() != null) {
                DataStorageManager.scheduleSave(event.getServer());
            }
        }
        if (event.getServer() != null) {
            ZEconomy.EXTRA_DATA.tickHourlyInterest(event.getServer());
            ZEconomy.EXTRA_DATA.tickExport(event.getServer(), CurrencyHelper.exportJsonPath(event.getServer()));
            if (!pendingVaultSync.isEmpty() && event.getServer().getTickCount() % VAULT_SYNC_RETRY_INTERVAL_TICKS == 0) {
                for (UUID playerId : pendingVaultSync.keySet().toArray(UUID[]::new)) {
                    Integer remainingTicks = pendingVaultSync.get(playerId);
                    if (remainingTicks == null) {
                        continue;
                    }
                    net.minecraft.server.level.ServerPlayer player = event.getServer().getPlayerList().getPlayer(playerId);
                    if (player == null) {
                        pendingVaultSync.remove(playerId);
                        continue;
                    }
                    if (CurrencyHelper.syncFromVaultOnJoin(player)) {
                        ZEconomy.EXTRA_DATA.syncPlayerMirror(player);
                        CurrencyHelper.syncPlayer(player);
                        CurrencyHelper.syncCustomData(player);
                        pendingVaultSync.remove(playerId);
                        continue;
                    }
                    int remaining = remainingTicks - VAULT_SYNC_RETRY_INTERVAL_TICKS;
                    if (remaining <= 0) {
                        pendingVaultSync.remove(playerId);
                        continue;
                    }
                    pendingVaultSync.put(playerId, remaining);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        EconomyCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onPermissionNodes(PermissionGatherEvent.Nodes event) {
        ModPermissions.register(event);
    }

    @SubscribeEvent
    public static void onInteractBankNpc(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!BankNpcHelper.isBankNpc(event.getTarget())) {
            return;
        }
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (id, inv, p) -> new BankMenu(id, inv),
                net.minecraft.network.chat.Component.translatable("screen.zeconomy.bank.title")
            ));
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.CONSUME);
        }
    }
}
