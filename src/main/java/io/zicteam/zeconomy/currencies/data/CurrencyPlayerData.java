package io.zicteam.zeconomy.currencies.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import io.zicteam.zeconomy.ZEconomy;
import io.zicteam.zeconomy.api.event.BalanceChangeEvent;
import io.zicteam.zeconomy.api.event.ZEconomyApiEvents;
import io.zicteam.zeconomy.currencies.BaseCurrency;
import io.zicteam.zeconomy.system.DataStorageManager;
import io.zicteam.zeconomy.utils.CurrencyHelper;
import io.zicteam.zeconomy.utils.ErrorCodeStruct;
import io.zicteam.zeconomy.utils.ErrorCodes;

public class CurrencyPlayerData {
    public static CurrencyPlayerData.Server SERVER = new CurrencyPlayerData.Server();
    public static CurrencyPlayerData.Client CLIENT = new CurrencyPlayerData.Client();

    public CurrencyPlayerData() {
    }

    public static void load(MinecraftServer server) {
        Path path = CurrencyHelper.playerDataPath(server);
        if (!Files.exists(path)) {
            SERVER = new CurrencyPlayerData.Server();
            SERVER.server = server;
            return;
        }
        try {
            CompoundTag root = NbtIo.readCompressed(path.toFile());
            if (root == null || !root.contains("players", Tag.TAG_LIST)) {
                SERVER = new CurrencyPlayerData.Server();
                SERVER.server = server;
                return;
            }
            SERVER = Server.deserialize(server, root);
        } catch (IOException e) {
            ZEconomy.printStackTrace("Failed to load player currency data", e);
            SERVER = new CurrencyPlayerData.Server();
            SERVER.server = server;
        }
    }

    public static void save(MinecraftServer server) {
        if (server == null) {
            return;
        }
        Path path = CurrencyHelper.playerDataPath(server);
        try {
            Files.createDirectories(path.getParent());
            NbtIo.writeCompressed(SERVER.serialize(), path.toFile());
        } catch (IOException e) {
            ZEconomy.printStackTrace("Failed to save player currency data", e);
        }
    }

    public static class Server {
        public MinecraftServer server;
        public Map<UUID, LinkedList<PlayerCurrency>> playersCurrencyMap;
        private int bulkUpdateDepth;
        private final Set<UUID> pendingPlayerSyncs;
        private final Map<UUID, Set<String>> pendingVaultSyncs;

        public Server() {
            this.playersCurrencyMap = new ConcurrentHashMap<>();
            this.pendingPlayerSyncs = ConcurrentHashMap.newKeySet();
            this.pendingVaultSyncs = new ConcurrentHashMap<>();
        }

        public CompoundTag serialize() {
            CompoundTag root = new CompoundTag();
            ListTag players = new ListTag();
            for (Map.Entry<UUID, LinkedList<PlayerCurrency>> entry : playersCurrencyMap.entrySet()) {
                CompoundTag playerTag = new CompoundTag();
                playerTag.putUUID("uuid", entry.getKey());
                ListTag currencies = new ListTag();
                for (PlayerCurrency currency : entry.getValue()) {
                    currencies.add(currency.serialize());
                }
                playerTag.put("currencies", currencies);
                players.add(playerTag);
            }
            root.put("players", players);
            return root;
        }

        public static Server deserialize(MinecraftServer server, CompoundTag root) {
            Server data = new Server();
            data.server = server;
            if (root == null || !root.contains("players", Tag.TAG_LIST)) {
                return data;
            }
            ListTag players = root.getList("players", Tag.TAG_COMPOUND);
            for (Tag tag : players) {
                if (!(tag instanceof CompoundTag playerTag)) {
                    continue;
                }
                UUID playerId = playerTag.getUUID("uuid");
                LinkedList<PlayerCurrency> list = new LinkedList<>();
                for (Tag item : playerTag.getList("currencies", Tag.TAG_COMPOUND)) {
                    if (item instanceof CompoundTag currencyTag) {
                        list.add(PlayerCurrency.deserialize(currencyTag));
                    }
                }
                data.playersCurrencyMap.put(playerId, list);
            }
            return data;
        }

        public ErrorCodes addCurrencyValue(Player player, String currencyId, double value) {
            return this.addCurrencyValue(player.getUUID(), currencyId, value);
        }

        public ErrorCodes addCurrencyValue(UUID playerId, String currencyId, double value) {
            final String normalizedCurrencyId = currencyId;
            LinkedList<PlayerCurrency> list = getPlayersCurrency(playerId);
            Optional<PlayerCurrency> currency = list.stream().filter(c -> c.currency.getName().equals(normalizedCurrencyId)).findFirst();
            if (currency.isEmpty()) {
                return ErrorCodes.NOT_FOUND;
            }
            PlayerCurrency playerCurrency = currency.get();
            double oldBalance = playerCurrency.balance;
            playerCurrency.balance += value;
            if (playerCurrency.balance < 0.0) {
                playerCurrency.balance = 0.0;
            }
            ZEconomyApiEvents.post(new BalanceChangeEvent(playerId, currencyId, oldBalance, playerCurrency.balance, "ADD"));
            DataStorageManager.markDirty();
            queueBalanceSideEffects(playerId, currencyId);
            return ErrorCodes.SUCCESS;
        }

        public ErrorCodes setCurrencyValue(Player player, String currencyId, double value) {
            return this.setCurrencyValue(player.getUUID(), currencyId, value);
        }

        public ErrorCodes setCurrencyValue(UUID playerId, String currencyId, double value) {
            final String normalizedCurrencyId = currencyId;
            LinkedList<PlayerCurrency> list = getPlayersCurrency(playerId);
            Optional<PlayerCurrency> currency = list.stream().filter(c -> c.currency.getName().equals(normalizedCurrencyId)).findFirst();
            if (currency.isEmpty()) {
                return ErrorCodes.NOT_FOUND;
            }
            PlayerCurrency playerCurrency = currency.get();
            double oldBalance = playerCurrency.balance;
            playerCurrency.balance = Math.max(0.0, value);
            ZEconomyApiEvents.post(new BalanceChangeEvent(playerId, currencyId, oldBalance, playerCurrency.balance, "SET"));
            DataStorageManager.markDirty();
            queueBalanceSideEffects(playerId, currencyId);
            return ErrorCodes.SUCCESS;
        }

        public ErrorCodeStruct<Double> getBalance(Player player, String currencyId) {
            return getBalance(player.getUUID(), currencyId);
        }

        public ErrorCodeStruct<Double> getBalance(UUID playerId, String currencyId) {
            Optional<PlayerCurrency> currency = getPlayerCurrency(playerId, currencyId);
            if (currency.isEmpty()) {
                return new ErrorCodeStruct<>(0.0, ErrorCodes.NOT_FOUND);
            }
            return new ErrorCodeStruct<>(currency.get().balance, ErrorCodes.SUCCESS);
        }

        public LinkedList<PlayerCurrency> getPlayersCurrency(Player player) {
            return getPlayersCurrency(player.getUUID());
        }

        public LinkedList<PlayerCurrency> getPlayersCurrency(UUID playerId) {
            newPlayer(playerId);
            return playersCurrencyMap.get(playerId);
        }

        public Optional<PlayerCurrency> getPlayerCurrency(Player player, String currencyId) {
            return getPlayerCurrency(player.getUUID(), currencyId);
        }

        public Optional<PlayerCurrency> getPlayerCurrency(UUID playerId, String currencyId) {
            final String normalizedCurrencyId = currencyId;
            return getPlayersCurrency(playerId).stream().filter(c -> c.currency.getName().equals(normalizedCurrencyId)).findFirst();
        }

        public LinkedList<PlayerCurrency> getPlayerUnlockedCurrency(Player player) {
            return getPlayerUnlockedCurrency(player.getUUID());
        }

        public LinkedList<PlayerCurrency> getPlayerUnlockedCurrency(UUID playerId) {
            LinkedList<PlayerCurrency> result = new LinkedList<>();
            for (PlayerCurrency currency : getPlayersCurrency(playerId)) {
                if (!currency.isLocked) {
                    result.add(currency);
                }
            }
            return result;
        }

        public LinkedList<PlayerCurrency> getPlayerLockedCurrency(Player player) {
            return getPlayerLockedCurrency(player.getUUID());
        }

        public LinkedList<PlayerCurrency> getPlayerLockedCurrency(UUID playerId) {
            LinkedList<PlayerCurrency> result = new LinkedList<>();
            for (PlayerCurrency currency : getPlayersCurrency(playerId)) {
                if (currency.isLocked) {
                    result.add(currency);
                }
            }
            return result;
        }

        public ErrorCodes lockCurrency(Player player, String currencyId, boolean value) {
            return lockCurrency(player.getUUID(), currencyId, value);
        }

        public ErrorCodes lockCurrency(UUID playerId, String currencyId, boolean value) {
            Optional<PlayerCurrency> currency = getPlayerCurrency(playerId, currencyId);
            if (currency.isEmpty()) {
                return ErrorCodes.NOT_FOUND;
            }
            currency.get().isLocked = value;
            DataStorageManager.markDirty();
            queuePlayerSync(playerId);
            return ErrorCodes.SUCCESS;
        }

        public void runBulkUpdate(Runnable action) {
            bulkUpdateDepth++;
            try {
                action.run();
            } finally {
                bulkUpdateDepth = Math.max(0, bulkUpdateDepth - 1);
                if (bulkUpdateDepth == 0) {
                    flushPendingSideEffects();
                }
            }
        }

        private void queueBalanceSideEffects(UUID playerId, String currencyId) {
            if (bulkUpdateDepth > 0) {
                pendingPlayerSyncs.add(playerId);
                pendingVaultSyncs.computeIfAbsent(playerId, ignored -> ConcurrentHashMap.newKeySet()).add(currencyId);
                return;
            }
            CurrencyHelper.syncToVaultOnBalanceChange(playerId, currencyId);
            CurrencyHelper.syncPlayer(playerId);
        }

        private void queuePlayerSync(UUID playerId) {
            if (bulkUpdateDepth > 0) {
                pendingPlayerSyncs.add(playerId);
                return;
            }
            CurrencyHelper.syncPlayer(playerId);
        }

        private void flushPendingSideEffects() {
            for (Map.Entry<UUID, Set<String>> entry : pendingVaultSyncs.entrySet()) {
                for (String currencyId : entry.getValue()) {
                    CurrencyHelper.syncToVaultOnBalanceChange(entry.getKey(), currencyId);
                }
            }
            pendingVaultSyncs.clear();
            for (UUID playerId : pendingPlayerSyncs) {
                CurrencyHelper.syncPlayer(playerId);
            }
            pendingPlayerSyncs.clear();
        }

        public void newPlayer(Player player) {
            newPlayer(player.getUUID());
        }

        public void newPlayer(UUID playerId) {
            playersCurrencyMap.computeIfAbsent(playerId, ignored -> {
                LinkedList<PlayerCurrency> list = new LinkedList<>();
                for (BaseCurrency currency : CurrencyData.SERVER.currencies) {
                    list.add(new PlayerCurrency(currency.copy(), currency.getDefaultValue()));
                }
                DataStorageManager.markDirty();
                return list;
            });
        }
    }

    public static class Client {
        public LinkedList<PlayerCurrency> currencies;

        public Client() {
            this.currencies = new LinkedList<>();
        }

        public Optional<PlayerCurrency> getCurrency(String currencyId) {
            return currencies.stream().filter(c -> c.currency.getName().equals(currencyId)).findFirst();
        }

        public double getBalance(String currencyId) {
            return getCurrency(currencyId).map(c -> c.balance).orElse(0.0);
        }

        public boolean hasCurrency(String currencyId) {
            return getCurrency(currencyId).isPresent();
        }

        public ErrorCodeStruct<Boolean> isCurrencyLocked(String currencyId) {
            return getCurrency(currencyId)
                .map(c -> new ErrorCodeStruct<>(c.isLocked, ErrorCodes.SUCCESS))
                .orElseGet(() -> new ErrorCodeStruct<>(false, ErrorCodes.NOT_FOUND));
        }

        public LinkedList<PlayerCurrency> getAllLockedCurrency() {
            LinkedList<PlayerCurrency> result = new LinkedList<>();
            for (PlayerCurrency currency : currencies) {
                if (currency.isLocked) {
                    result.add(currency);
                }
            }
            return result;
        }

        public LinkedList<PlayerCurrency> getAllUnlockedCurrency() {
            LinkedList<PlayerCurrency> result = new LinkedList<>();
            for (PlayerCurrency currency : currencies) {
                if (!currency.isLocked) {
                    result.add(currency);
                }
            }
            return result;
        }

        public ErrorCodeStruct<Boolean> updateCurrency(PlayerCurrency updated) {
            if (updated == null || updated.currency == null) {
                return new ErrorCodeStruct<>(false, ErrorCodes.FAIL);
            }
            for (int i = 0; i < currencies.size(); i++) {
                if (currencies.get(i).currency.getName().equals(updated.currency.getName())) {
                    currencies.set(i, updated);
                    return new ErrorCodeStruct<>(true, ErrorCodes.SUCCESS);
                }
            }
            return new ErrorCodeStruct<>(false, ErrorCodes.NOT_FOUND);
        }

        public ErrorCodeStruct<Boolean> updateCurrency(String currencyId, double balance, boolean isLocked) {
            Optional<PlayerCurrency> currency = getCurrency(currencyId);
            if (currency.isEmpty()) {
                return new ErrorCodeStruct<>(false, ErrorCodes.NOT_FOUND);
            }
            PlayerCurrency playerCurrency = currency.get();
            playerCurrency.balance = balance;
            playerCurrency.isLocked = isLocked;
            return new ErrorCodeStruct<>(true, ErrorCodes.SUCCESS);
        }

        public ErrorCodes updateCurrencyForce(PlayerCurrency updated) {
            ErrorCodeStruct<Boolean> result = updateCurrency(updated);
            if (result.codes == ErrorCodes.NOT_FOUND && updated != null) {
                currencies.add(updated);
                return ErrorCodes.SUCCESS;
            }
            return result.codes;
        }

        public ErrorCodes updateCurrencyForce(String currencyId, double balance, boolean isLocked) {
            Optional<PlayerCurrency> currency = getCurrency(currencyId);
            if (currency.isPresent()) {
                PlayerCurrency existing = currency.get();
                existing.balance = balance;
                existing.isLocked = isLocked;
                return ErrorCodes.SUCCESS;
            }
            Optional<BaseCurrency> base = CurrencyData.CLIENT.currencies.stream().filter(c -> c.getName().equals(currencyId)).findFirst();
            if (base.isEmpty()) {
                return ErrorCodes.NOT_FOUND;
            }
            currencies.add(new PlayerCurrency(base.get().copy(), balance).setLocked(isLocked));
            return ErrorCodes.SUCCESS;
        }

        public void load(CompoundTag root) {
            currencies.clear();
            for (Tag tag : root.getList("currencies", Tag.TAG_COMPOUND)) {
                if (tag instanceof CompoundTag compound) {
                    currencies.add(PlayerCurrency.deserialize(compound));
                }
            }
        }

        public CompoundTag serialize() {
            CompoundTag root = new CompoundTag();
            ListTag list = new ListTag();
            for (PlayerCurrency currency : currencies) {
                list.add(currency.serialize());
            }
            root.put("currencies", list);
            return root;
        }
    }

    public static class PlayerCurrency {
        public BaseCurrency currency;
        public double balance;
        public boolean isLocked;

        public PlayerCurrency(BaseCurrency currency, double balance) {
            this.currency = currency;
            this.balance = balance;
            this.isLocked = false;
        }

        public PlayerCurrency setLocked(boolean value) {
            this.isLocked = value;
            return this;
        }

        public static PlayerCurrency deserialize(CompoundTag tag) {
            BaseCurrency currency = BaseCurrency.deserialize(tag.getCompound("currency"));
            double balance = tag.getDouble("balance");
            boolean isLocked = tag.getBoolean("locked");
            return new PlayerCurrency(currency, balance).setLocked(isLocked);
        }

        public CompoundTag serialize() {
            CompoundTag tag = new CompoundTag();
            tag.put("currency", currency.serialize());
            tag.putDouble("balance", balance);
            tag.putBoolean("locked", isLocked);
            return tag;
        }
    }
}
