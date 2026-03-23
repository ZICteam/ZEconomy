package io.zicteam.zeconomy.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.server.ServerLifecycleHooks;
import io.zicteam.zeconomy.CustomPlayerData;
import io.zicteam.zeconomy.ZEconomy;
import io.zicteam.zeconomy.config.EconomyConfig;
import io.zicteam.zeconomy.currencies.BaseCurrency;
import io.zicteam.zeconomy.currencies.CurrencySymbol;
import io.zicteam.zeconomy.currencies.compat.VaultBridge;
import io.zicteam.zeconomy.currencies.data.CurrencyData;
import io.zicteam.zeconomy.currencies.data.CurrencyPlayerData;
import io.zicteam.zeconomy.network.ZEconomyNetwork;
import io.zicteam.zeconomy.system.DataStorageManager;
import io.zicteam.zeconomy.system.EconomyPlayerSyncService;

public class CurrencyHelper {
    public CurrencyHelper() {
    }

    public static ErrorCodeStruct<CurrencyData> getAllCurrency() {
        return getAllCurrency(false);
    }

    public static ErrorCodeStruct<CurrencyData> getAllCurrency(boolean client) {
        return new ErrorCodeStruct<>(client ? CurrencyData.CLIENT : CurrencyData.SERVER);
    }

    public static ErrorCodes syncPlayer(ServerPlayer player) {
        if (player == null) {
            return ErrorCodes.NOT_FOUND;
        }
        ZEconomyNetwork.syncPlayer(player);
        return ErrorCodes.SUCCESS;
    }

    public static ErrorCodes refreshPlayerState(ServerPlayer player) {
        if (player == null) {
            return ErrorCodes.NOT_FOUND;
        }
        EconomyPlayerSyncService.syncPlayerMirror(player);
        return syncPlayer(player);
    }

    public static void refreshPlayersState(ServerPlayer... players) {
        if (players == null) {
            return;
        }
        for (ServerPlayer player : players) {
            refreshPlayerState(player);
        }
    }

    public static ErrorCodeStruct<CurrencyData> getCurrencyData(boolean client) {
        return getAllCurrency(client);
    }

    public static ErrorCodeStruct<LinkedList<CurrencyPlayerData.PlayerCurrency>> getCurrencyPlayerData(Player player) {
        if (player == null) {
            return new ErrorCodeStruct<>(new LinkedList<>(), ErrorCodes.NOT_FOUND);
        }
        if (player.level().isClientSide) {
            return new ErrorCodeStruct<>(CurrencyPlayerData.CLIENT.currencies, ErrorCodes.SUCCESS);
        }
        return new ErrorCodeStruct<>(CurrencyPlayerData.SERVER.getPlayersCurrency(player), ErrorCodes.SUCCESS);
    }

    public static CurrencyPlayerData.Server getPlayerCurrencyServerData() {
        return CurrencyPlayerData.SERVER;
    }

    public static CurrencyPlayerData.Client getPlayerCurrencyClientData() {
        return CurrencyPlayerData.CLIENT;
    }

    public static void syncCurrencyData(ServerPlayer player) {
        ZEconomyNetwork.syncPlayer(player);
    }

    public static void syncCurrencyData(MinecraftServer server) {
        ZEconomyNetwork.syncAll(server);
    }

    public static void createCurrencyOnClient(BaseCurrency currency) {
        if (currency == null) {
            return;
        }
        Optional<BaseCurrency> exists = CurrencyData.CLIENT.currencies.stream().filter(c -> c.getName().equals(currency.getName())).findFirst();
        if (exists.isEmpty()) {
            CurrencyData.CLIENT.currencies.add(currency.copy());
        }
    }

    public static ErrorCodes createCurrencyOnServer(BaseCurrency currency) {
        if (currency == null || currency.getName() == null || currency.getName().isBlank()) {
            return ErrorCodes.FAIL;
        }
        Optional<BaseCurrency> exists = CurrencyData.SERVER.currencies.stream().filter(c -> c.getName().equals(currency.getName())).findFirst();
        if (exists.isPresent()) {
            return ErrorCodes.FAIL;
        }
        CurrencyData.SERVER.currencies.add(currency.copy());
        for (UUID uuid : CurrencyPlayerData.SERVER.playersCurrencyMap.keySet()) {
            CurrencyPlayerData.SERVER.getPlayersCurrency(uuid).add(new CurrencyPlayerData.PlayerCurrency(currency.copy(), currency.getDefaultValue()));
        }
        DataStorageManager.markDirty();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            syncCurrencyData(server);
        }
        return ErrorCodes.SUCCESS;
    }

    public static void deleteCurrencyOnClient(BaseCurrency currency) {
        if (currency == null) {
            return;
        }
        CurrencyData.CLIENT.currencies.removeIf(c -> c.getName().equals(currency.getName()));
        CurrencyPlayerData.CLIENT.currencies.removeIf(c -> c.currency.getName().equals(currency.getName()));
    }

    public static ErrorCodes deleteCurrencyOnServer(BaseCurrency currency) {
        if (currency == null) {
            return ErrorCodes.NOT_FOUND;
        }
        String id = currency.getName();
        Optional<BaseCurrency> exists = CurrencyData.SERVER.currencies.stream().filter(c -> c.getName().equals(id)).findFirst();
        if (exists.isEmpty()) {
            return ErrorCodes.NOT_FOUND;
        }
        if (!exists.get().canDelete) {
            return ErrorCodes.NOT_ACCESS;
        }
        CurrencyData.SERVER.currencies.removeIf(c -> c.getName().equals(id));
        for (UUID uuid : CurrencyPlayerData.SERVER.playersCurrencyMap.keySet()) {
            CurrencyPlayerData.SERVER.getPlayersCurrency(uuid).removeIf(c -> c.currency.getName().equals(id));
        }
        DataStorageManager.markDirty();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            syncCurrencyData(server);
        }
        return ErrorCodes.SUCCESS;
    }

    public static ErrorCodes syncCurrencyFromServerToClient(MinecraftServer server) {
        syncCurrencyData(server);
        return ErrorCodes.SUCCESS;
    }

    public static UUID getPlayerUUID(Player player) {
        return player.getUUID();
    }

    public static boolean isAdmin(Player player) {
        return player != null && player.hasPermissions(2);
    }

    public static void saveAll(MinecraftServer server) {
        if (server == null) {
            return;
        }
        DataStorageManager.saveAll(server);
    }

    public static void scheduleSave(MinecraftServer server) {
        if (server == null) {
            return;
        }
        DataStorageManager.scheduleSave(server);
    }

    public static void saveCurrencyData() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        Path path = currencyDataPath(server);
        try {
            Files.createDirectories(path.getParent());
            net.minecraft.nbt.NbtIo.writeCompressed(CurrencyData.SERVER.serialize(), path.toFile());
        } catch (IOException e) {
            ZEconomy.printStackTrace("Failed to save currencies", e);
        }
    }

    public static void savePlayerData(MinecraftServer server) {
        CurrencyPlayerData.save(server);
    }

    public static ErrorCodeStruct<Boolean> checkNewCurrency() {
        return new ErrorCodeStruct<>(Boolean.TRUE, ErrorCodes.SUCCESS);
    }

    public static CustomPlayerData.Server getCustomServerData() {
        return CustomPlayerData.SERVER;
    }

    public static CustomPlayerData.Client getCustomClientData() {
        return CustomPlayerData.CLIENT;
    }

    public static void syncCustomData(ServerPlayer player) {
        EconomyPlayerSyncService.syncCustomData(player);
    }

    public static ErrorCodes updateCustomData(ServerPlayer player, Consumer<CompoundTag> consumer) {
        if (player == null || consumer == null) {
            return ErrorCodes.FAIL;
        }
        CustomPlayerData.Data data = CustomPlayerData.SERVER.getPlayerCustomData(player);
        consumer.accept(data.nbt);
        DataStorageManager.markDirty();
        syncCustomData(player);
        return ErrorCodes.SUCCESS;
    }

    public static CompoundTag getCustomData(Player player) {
        if (player.level().isClientSide) {
            return CustomPlayerData.CLIENT.data.nbt;
        }
        return CustomPlayerData.SERVER.getPlayerCustomData(player).nbt;
    }

    public static Path currencyDataPath(MinecraftServer server) {
        return rootDataDir(server).resolve("currencies.nbt");
    }

    public static Path playerDataPath(MinecraftServer server) {
        return rootDataDir(server).resolve("players.nbt");
    }

    public static Path customDataPath(MinecraftServer server) {
        return rootDataDir(server).resolve("custom_player_data.nbt");
    }

    public static Path extraDataPath(MinecraftServer server) {
        return rootDataDir(server).resolve("extra_data.nbt");
    }

    public static Path exportJsonPath(MinecraftServer server) {
        return rootDataDir(server).resolve("export").resolve("economy_export.json");
    }

    public static Path resolveDataRoot(MinecraftServer server) {
        Path root = server.getWorldPath(LevelResource.ROOT).resolve("serverconfig");
        return root.resolve(ZEconomy.MOD_ID);
    }

    private static Path rootDataDir(MinecraftServer server) {
        return resolveDataRoot(server);
    }

    public static void syncPlayer(UUID playerId) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player != null) {
            syncPlayer(player);
        }
    }

    public static void ensureDefaultCurrency() {
        if (CurrencyData.SERVER.currencies.stream().noneMatch(c -> c.getName().equals(ZEconomy.PRIMARY_CURRENCY_ID))) {
            CurrencyData.SERVER.currencies.add(new BaseCurrency(ZEconomy.PRIMARY_CURRENCY_ID, new CurrencySymbol("\u25ce"), 0.0).canDelete(false));
        }
        if (CurrencyData.SERVER.currencies.stream().noneMatch(c -> c.getName().equals(ZEconomy.SECONDARY_CURRENCY_ID))) {
            CurrencyData.SERVER.currencies.add(new BaseCurrency(ZEconomy.SECONDARY_CURRENCY_ID, new CurrencySymbol("B"), 0.0).canDelete(false));
        }
    }

    public static UUID getServerAccountUUID() {
        return ZEconomy.SERVER_ACCOUNT_ID;
    }

    public static boolean isServerAccount(UUID playerId) {
        return playerId != null && ZEconomy.SERVER_ACCOUNT_ID.equals(playerId);
    }

    public static void ensureServerAccount() {
        CurrencyPlayerData.SERVER.newPlayer(ZEconomy.SERVER_ACCOUNT_ID);
    }

    public static void initVaultBridge(MinecraftServer server) {
        if (!safeBooleanConfig(EconomyConfig.ENABLE_VAULT_BRIDGE) || server == null) {
            return;
        }
        VaultBridge.init(server);
    }

    public static boolean syncFromVaultOnJoin(ServerPlayer player) {
        if (player == null || !safeBooleanConfig(EconomyConfig.ENABLE_VAULT_BRIDGE) || !safeBooleanConfig(EconomyConfig.VAULT_PULL_ON_JOIN)) {
            return false;
        }
        initVaultBridge(player.server);
        if (!VaultBridge.isAvailable()) {
            return false;
        }
        String currencyId = safeStringConfig(EconomyConfig.VAULT_SYNC_CURRENCY_ID);
        if (currencyId == null || currencyId.isBlank()) {
            return false;
        }
        double vaultBalance = VaultBridge.readBalance(player.getUUID());
        if (Double.isNaN(vaultBalance) || vaultBalance < 0.0) {
            return false;
        }
        double currentBalance = CurrencyHelper.getPlayerCurrencyServerData().getBalance(player, currencyId).value;
        if (vaultBalance == 0.0D && currentBalance > 0.0D) {
            return false;
        }
        CurrencyHelper.getPlayerCurrencyServerData().setCurrencyValue(player, currencyId, vaultBalance);
        return true;
    }

    public static void syncToVaultOnBalanceChange(UUID playerId, String currencyId) {
        if (playerId == null || currencyId == null || !safeBooleanConfig(EconomyConfig.ENABLE_VAULT_BRIDGE) || !safeBooleanConfig(EconomyConfig.VAULT_PUSH_ON_CHANGE)) {
            return;
        }
        if (isServerAccount(playerId)) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        initVaultBridge(server);
        if (!VaultBridge.isAvailable()) {
            return;
        }
        String syncCurrency = safeStringConfig(EconomyConfig.VAULT_SYNC_CURRENCY_ID);
        if (syncCurrency == null || syncCurrency.isBlank() || !syncCurrency.equals(currencyId)) {
            return;
        }
        double balance = CurrencyHelper.getPlayerCurrencyServerData().getBalance(playerId, currencyId).value;
        if (balance < 0.0) {
            balance = 0.0;
        }
        VaultBridge.writeBalance(playerId, balance);
    }

    private static boolean safeBooleanConfig(net.minecraftforge.common.ForgeConfigSpec.BooleanValue configValue) {
        try {
            return configValue != null && configValue.get();
        } catch (IllegalStateException ignored) {
            return false;
        }
    }

    private static String safeStringConfig(net.minecraftforge.common.ForgeConfigSpec.ConfigValue<String> configValue) {
        try {
            return configValue == null ? null : configValue.get();
        } catch (IllegalStateException ignored) {
            return null;
        }
    }
}
