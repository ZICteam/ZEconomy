package io.zicteam.zeconomy.system;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import io.zicteam.zeconomy.CustomPlayerData;
import io.zicteam.zeconomy.ZEconomy;
import io.zicteam.zeconomy.api.event.BankTransactionEvent;
import io.zicteam.zeconomy.api.event.DailyRewardEvent;
import io.zicteam.zeconomy.api.event.ExchangeEvent;
import io.zicteam.zeconomy.api.event.RateChangeEvent;
import io.zicteam.zeconomy.api.event.VaultTransactionEvent;
import io.zicteam.zeconomy.api.event.ZEconomyApiEvents;
import io.zicteam.zeconomy.config.EconomyConfig;
import io.zicteam.zeconomy.currencies.data.CurrencyPlayerData;
import io.zicteam.zeconomy.utils.CurrencyHelper;

public class ExtraEconomyData {
    public static final int MAX_LOGS = 500;

    private final Map<UUID, Map<String, Double>> bankDeposits = new HashMap<>();
    private final Map<String, Double> bankTotalsByCurrency = new HashMap<>();
    private final Map<UUID, Long> lastHourlyPayoutEpochSec = new HashMap<>();
    private final Map<UUID, List<ItemStack>> mailbox = new HashMap<>();
    private final Map<String, Double> exchangeRates = new HashMap<>();
    private final Map<UUID, Map<String, Double>> vaultDeposits = new HashMap<>();
    private final Map<UUID, String> vaultPins = new HashMap<>();
    private final Map<UUID, DailyState> dailyRewards = new HashMap<>();
    private final LinkedList<TransactionRecord> logs = new LinkedList<>();
    private long lastExportEpochSec = 0L;
    private long nextInterestSweepEpochSec = 0L;

    public static ExtraEconomyData load(Path path) {
        ExtraEconomyData data = new ExtraEconomyData();
        if (path == null || !Files.exists(path)) {
            return data;
        }
        try {
            CompoundTag root = NbtIo.readCompressed(path.toFile());
            if (root != null) {
                data.read(root);
            }
        } catch (IOException e) {
            ZEconomy.printStackTrace("Failed to load extra economy data", e);
        }
        return data;
    }

    public void save(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.createDirectories(path.getParent());
            NbtIo.writeCompressed(write(), path.toFile());
        } catch (IOException e) {
            ZEconomy.printStackTrace("Failed to save extra economy data", e);
        }
    }

    public CompoundTag serialize() {
        return write();
    }

    public static ExtraEconomyData deserialize(CompoundTag root) {
        ExtraEconomyData data = new ExtraEconomyData();
        if (root != null) {
            data.read(root);
        }
        data.rebuildCaches();
        return data;
    }

    public void ensureDefaultRates() {
        exchangeRates.putIfAbsent(rateKey(ZEconomy.SECONDARY_CURRENCY_ID, ZEconomy.PRIMARY_CURRENCY_ID), EconomyConfig.DEFAULT_BCOIN_TO_Z_RATE.get());
    }

    public double getRate(String from, String to) {
        return exchangeRates.getOrDefault(rateKey(from, to), 0.0);
    }

    public void setRate(String from, String to, double rate) {
        exchangeRates.put(rateKey(from, to), Math.max(0.0, rate));
        ZEconomyApiEvents.post(new RateChangeEvent(RateChangeEvent.Action.SET, from, to, Math.max(0.0, rate)));
    }

    public boolean removeRate(String from, String to) {
        boolean removed = exchangeRates.remove(rateKey(from, to)) != null;
        if (removed) {
            ZEconomyApiEvents.post(new RateChangeEvent(RateChangeEvent.Action.REMOVE, from, to, 0.0D));
        }
        return removed;
    }

    public Map<String, Double> getAllRates() {
        return new HashMap<>(exchangeRates);
    }

    public void clearRates() {
        exchangeRates.clear();
        ZEconomyApiEvents.post(new RateChangeEvent(RateChangeEvent.Action.RESET, "", "", 0.0D));
    }

    public boolean exchangeCurrency(ServerPlayer player, String from, String to, double amount) {
        if (amount <= 0 || !EconomyConfig.ENABLE_BCOIN_EXCHANGE.get()) {
            return false;
        }
        double rate = getRate(from, to);
        if (rate <= 0.0) {
            return false;
        }
        double fromBalance = CurrencyHelper.getPlayerCurrencyServerData().getBalance(player, from).value;
        if (fromBalance < amount) {
            return false;
        }
        double gross = amount * rate;
        double fee = gross * EconomyConfig.EXCHANGE_FEE_RATE.get();
        double net = gross - fee;
        double serverTargetBalance = CurrencyHelper.getPlayerCurrencyServerData().getBalance(CurrencyHelper.getServerAccountUUID(), to).value;
        if (serverTargetBalance < gross) {
            return false;
        }
        CurrencyHelper.getPlayerCurrencyServerData().runBulkUpdate(() -> {
            CurrencyHelper.getPlayerCurrencyServerData().addCurrencyValue(player, from, -amount);
            CurrencyHelper.getPlayerCurrencyServerData().addCurrencyValue(CurrencyHelper.getServerAccountUUID(), from, amount);
            CurrencyHelper.getPlayerCurrencyServerData().addCurrencyValue(CurrencyHelper.getServerAccountUUID(), to, -gross);
            CurrencyHelper.getPlayerCurrencyServerData().addCurrencyValue(player, to, net);
        });
        log("EXCHANGE", player.getUUID(), null, to, net, fee, from + "->" + to + " amount=" + amount);
        ZEconomyApiEvents.post(new ExchangeEvent(player.getUUID(), from, to, amount, net, fee));
        syncPlayerMirror(player);
        return true;
    }

    public boolean transferWithFee(ServerPlayer from, ServerPlayer to, String currency, double amount) {
        if (amount <= 0) {
            return false;
        }
        double fee = amount * EconomyConfig.TRANSFER_FEE_RATE.get();
        double total = amount + fee;
        double balance = CurrencyHelper.getPlayerCurrencyServerData().getBalance(from, currency).value;
        if (balance < total) {
            return false;
        }
        CurrencyHelper.getPlayerCurrencyServerData().runBulkUpdate(() -> {
            CurrencyHelper.getPlayerCurrencyServerData().addCurrencyValue(from, currency, -total);
            CurrencyHelper.getPlayerCurrencyServerData().addCurrencyValue(to, currency, amount);
            if (fee > 0.0D) {
                CurrencyHelper.getPlayerCurrencyServerData().addCurrencyValue(CurrencyHelper.getServerAccountUUID(), currency, fee);
            }
        });
        log("TRANSFER", from.getUUID(), to.getUUID(), currency, amount, fee, "pay");
        syncPlayerMirror(from);
        syncPlayerMirror(to);
        return true;
    }

    public boolean depositToBank(ServerPlayer player, String currency, double amount) {
        if (amount <= 0) {
            return false;
        }
        double balance = CurrencyHelper.getPlayerCurrencyServerData().getBalance(player, currency).value;
        if (balance < amount) {
            return false;
        }
        CurrencyHelper.getPlayerCurrencyServerData().runBulkUpdate(() -> {
            CurrencyHelper.getPlayerCurrencyServerData().addCurrencyValue(player, currency, -amount);
            CurrencyHelper.getPlayerCurrencyServerData().addCurrencyValue(CurrencyHelper.getServerAccountUUID(), currency, amount);
        });
        bankDeposits.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>()).merge(currency, amount, Double::sum);
        adjustBankTotal(currency, amount);
        lastHourlyPayoutEpochSec.putIfAbsent(player.getUUID(), Instant.now().getEpochSecond());
        log("BANK_DEPOSIT", player.getUUID(), null, currency, amount, 0.0, "");
        ZEconomyApiEvents.post(new BankTransactionEvent(BankTransactionEvent.Action.DEPOSIT, player.getUUID(), currency, amount, getDeposited(player.getUUID(), currency)));
        syncPlayerMirror(player);
        return true;
    }

    public boolean withdrawFromBank(ServerPlayer player, String currency, double amount) {
        if (amount <= 0) {
            return false;
        }
        Map<String, Double> account = bankDeposits.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>());
        double deposited = account.getOrDefault(currency, 0.0);
        if (deposited < amount) {
            return false;
        }
        double reservedBefore = getTotalBankDeposits(currency);
        double serverBalance = CurrencyHelper.getPlayerCurrencyServerData().getBalance(CurrencyHelper.getServerAccountUUID(), currency).value;
        if (serverBalance < amount || serverBalance - (reservedBefore - amount) < 0.0D) {
            return false;
        }
        account.put(currency, deposited - amount);
        adjustBankTotal(currency, -amount);
        CurrencyHelper.getPlayerCurrencyServerData().runBulkUpdate(() -> {
            CurrencyHelper.getPlayerCurrencyServerData().addCurrencyValue(CurrencyHelper.getServerAccountUUID(), currency, -amount);
            CurrencyHelper.getPlayerCurrencyServerData().addCurrencyValue(player, currency, amount);
        });
        log("BANK_WITHDRAW", player.getUUID(), null, currency, amount, 0.0, "");
        ZEconomyApiEvents.post(new BankTransactionEvent(BankTransactionEvent.Action.WITHDRAW, player.getUUID(), currency, amount, getDeposited(player.getUUID(), currency)));
        syncPlayerMirror(player);
        return true;
    }

    public boolean setVaultPin(UUID playerId, String pin) {
        if (pin == null || pin.length() < 4 || pin.length() > 12) {
            return false;
        }
        for (char c : pin.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        vaultPins.put(playerId, pin);
        ZEconomyApiEvents.post(new VaultTransactionEvent(VaultTransactionEvent.Action.PIN_SET, playerId, "", 0.0D, 0.0D));
        return true;
    }

    public boolean hasVaultPin(UUID playerId) {
        return vaultPins.containsKey(playerId);
    }

    public boolean vaultDeposit(ServerPlayer player, String pin, String currency, double amount) {
        if (!checkPin(player.getUUID(), pin) || amount <= 0) {
            return false;
        }
        double balance = CurrencyHelper.getPlayerCurrencyServerData().getBalance(player, currency).value;
        if (balance < amount) {
            return false;
        }
        CurrencyHelper.getPlayerCurrencyServerData().runBulkUpdate(() ->
            CurrencyHelper.getPlayerCurrencyServerData().addCurrencyValue(player, currency, -amount)
        );
        vaultDeposits.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>()).merge(currency, amount, Double::sum);
        log("VAULT_DEPOSIT", player.getUUID(), null, currency, amount, 0.0, "");
        ZEconomyApiEvents.post(new VaultTransactionEvent(VaultTransactionEvent.Action.DEPOSIT, player.getUUID(), currency, amount, getVaultBalance(player.getUUID(), currency)));
        syncPlayerMirror(player);
        return true;
    }

    public boolean vaultWithdraw(ServerPlayer player, String pin, String currency, double amount) {
        if (!checkPin(player.getUUID(), pin) || amount <= 0) {
            return false;
        }
        Map<String, Double> account = vaultDeposits.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>());
        double current = account.getOrDefault(currency, 0.0);
        if (current < amount) {
            return false;
        }
        account.put(currency, current - amount);
        CurrencyHelper.getPlayerCurrencyServerData().runBulkUpdate(() ->
            CurrencyHelper.getPlayerCurrencyServerData().addCurrencyValue(player, currency, amount)
        );
        log("VAULT_WITHDRAW", player.getUUID(), null, currency, amount, 0.0, "");
        ZEconomyApiEvents.post(new VaultTransactionEvent(VaultTransactionEvent.Action.WITHDRAW, player.getUUID(), currency, amount, getVaultBalance(player.getUUID(), currency)));
        syncPlayerMirror(player);
        return true;
    }

    public double getVaultBalance(UUID playerId, String currency) {
        return vaultDeposits.getOrDefault(playerId, Map.of()).getOrDefault(currency, 0.0);
    }

    public Map<String, Double> getAllVaultBalances(UUID playerId) {
        return new HashMap<>(vaultDeposits.getOrDefault(playerId, Map.of()));
    }

    public DailyClaimResult claimDaily(ServerPlayer player) {
        long today = LocalDate.now(ZoneOffset.UTC).toEpochDay();
        DailyState state = dailyRewards.computeIfAbsent(player.getUUID(), ignored -> new DailyState(-1, 0));
        if (state.lastClaimDay == today) {
            return new DailyClaimResult(false, 0, 0, state.streak);
        }
        int nextStreak = state.lastClaimDay == today - 1 ? state.streak + 1 : 1;
        double zReward = EconomyConfig.DAILY_REWARD_Z.get() + Math.min(7, nextStreak) * 10.0;
        double bReward = EconomyConfig.DAILY_REWARD_B.get() + (nextStreak >= 7 ? 1.0 : 0.0);
        if (getServerSpendableBalance(ZEconomy.PRIMARY_CURRENCY_ID) < zReward) {
            return new DailyClaimResult(false, 0, 0, state.streak);
        }
        if (getServerSpendableBalance(ZEconomy.SECONDARY_CURRENCY_ID) < bReward) {
            return new DailyClaimResult(false, 0, 0, state.streak);
        }
        if (state.lastClaimDay == today - 1) {
            state.streak += 1;
        } else {
            state.streak = 1;
        }
        state.lastClaimDay = today;
        CurrencyHelper.getPlayerCurrencyServerData().runBulkUpdate(() -> {
            CurrencyHelper.getPlayerCurrencyServerData().addCurrencyValue(CurrencyHelper.getServerAccountUUID(), ZEconomy.PRIMARY_CURRENCY_ID, -zReward);
            CurrencyHelper.getPlayerCurrencyServerData().addCurrencyValue(CurrencyHelper.getServerAccountUUID(), ZEconomy.SECONDARY_CURRENCY_ID, -bReward);
            CurrencyHelper.getPlayerCurrencyServerData().addCurrencyValue(player, ZEconomy.PRIMARY_CURRENCY_ID, zReward);
            CurrencyHelper.getPlayerCurrencyServerData().addCurrencyValue(player, ZEconomy.SECONDARY_CURRENCY_ID, bReward);
        });
        log("DAILY_REWARD", player.getUUID(), null, "mixed", zReward, 0.0, ZEconomy.SECONDARY_CURRENCY_ID + "=" + bReward + " streak=" + state.streak);
        ZEconomyApiEvents.post(new DailyRewardEvent(player.getUUID(), zReward, bReward, state.streak));
        syncPlayerMirror(player);
        return new DailyClaimResult(true, zReward, bReward, state.streak);
    }

    public int getDailyStreak(UUID playerId) {
        return dailyRewards.getOrDefault(playerId, new DailyState(-1, 0)).streak;
    }

    public void tickHourlyInterest(MinecraftServer server) {
        long now = Instant.now().getEpochSecond();
        if (now < nextInterestSweepEpochSec) {
            return;
        }
        nextInterestSweepEpochSec = now + 30L;
        long interval = 3600L;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID id = player.getUUID();
            long last = lastHourlyPayoutEpochSec.getOrDefault(id, now);
            if (now - last < interval) {
                continue;
            }
            Map<String, Double> account = bankDeposits.getOrDefault(id, Map.of());
            double rate = EconomyConfig.HOURLY_INTEREST_RATE.get();
            for (Map.Entry<String, Double> entry : account.entrySet()) {
                if (entry.getValue() <= 0) {
                    continue;
                }
                double payout = entry.getValue() * rate;
                if (getServerSpendableBalance(entry.getKey()) < payout) {
                    continue;
                }
                CurrencyHelper.getPlayerCurrencyServerData().runBulkUpdate(() -> {
                    CurrencyHelper.getPlayerCurrencyServerData().addCurrencyValue(CurrencyHelper.getServerAccountUUID(), entry.getKey(), -payout);
                    CurrencyHelper.getPlayerCurrencyServerData().addCurrencyValue(player, entry.getKey(), payout);
                });
                log("BANK_INTEREST", id, null, entry.getKey(), payout, 0.0, "");
                ZEconomyApiEvents.post(new BankTransactionEvent(BankTransactionEvent.Action.INTEREST, id, entry.getKey(), payout, getDeposited(id, entry.getKey())));
            }
            lastHourlyPayoutEpochSec.put(id, now);
            syncPlayerMirror(player);
        }
    }

    public void tickExport(MinecraftServer server, Path exportPath) {
        long now = Instant.now().getEpochSecond();
        if (now - lastExportEpochSec < EconomyConfig.EXPORT_INTERVAL_SECONDS.get()) {
            return;
        }
        exportJson(server, exportPath);
        lastExportEpochSec = now;
    }

    public void exportJson(MinecraftServer server, Path exportPath) {
        try {
            Files.createDirectories(exportPath.getParent());
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonObject root = new JsonObject();
            root.addProperty("generated_at_epoch", Instant.now().getEpochSecond());
            root.addProperty("server_players_online", server.getPlayerList().getPlayerCount());

            JsonArray tops = new JsonArray();
            for (RichEntry e : getTopRich(server, ZEconomy.PRIMARY_CURRENCY_ID, 20, true)) {
                JsonObject obj = new JsonObject();
                obj.addProperty("uuid", e.playerId.toString());
                obj.addProperty("name", e.playerName);
                obj.addProperty("currency", e.currency);
                obj.addProperty("amount", e.amount);
                tops.add(obj);
            }
            root.add("top_" + ZEconomy.PRIMARY_CURRENCY_ID, tops);

            JsonArray logsJson = new JsonArray();
            for (TransactionRecord r : getRecentLogs(100)) {
                JsonObject o = new JsonObject();
                o.addProperty("time", r.epochSec);
                o.addProperty("type", r.type);
                o.addProperty("actor", r.actor == null ? "" : r.actor.toString());
                o.addProperty("target", r.target == null ? "" : r.target.toString());
                o.addProperty("currency", r.currency);
                o.addProperty("amount", r.amount);
                o.addProperty("fee", r.fee);
                o.addProperty("note", r.note);
                logsJson.add(o);
            }
            root.add("recent_logs", logsJson);

            Files.writeString(exportPath, gson.toJson(root), StandardCharsets.UTF_8);
        } catch (Exception e) {
            ZEconomy.printStackTrace("Failed to export economy JSON", e);
        }
    }

    public void sendMail(UUID target, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        mailbox.computeIfAbsent(target, ignored -> new ArrayList<>()).add(stack.copy());
        log("MAIL_SEND", null, target, "item", stack.getCount(), 0.0, stack.getItem().toString());
    }

    public List<ItemStack> claimMail(UUID playerId) {
        List<ItemStack> items = mailbox.remove(playerId);
        int count = items == null ? 0 : items.size();
        if (count > 0) {
            log("MAIL_CLAIM", playerId, null, "item", count, 0.0, "");
        }
        return items == null ? List.of() : items;
    }

    public int pendingMailCount(UUID playerId) {
        return mailbox.getOrDefault(playerId, List.of()).size();
    }

    public boolean hasClaimedDailyToday(UUID playerId) {
        DailyState state = dailyRewards.get(playerId);
        if (state == null) {
            return false;
        }
        return state.lastClaimDay == LocalDate.now(ZoneOffset.UTC).toEpochDay();
    }

    public long getLastExportEpochSec() {
        return lastExportEpochSec;
    }

    public int getLogCount() {
        return logs.size();
    }

    public void syncPlayerMirror(ServerPlayer player) {
        CustomPlayerData.Data data = CustomPlayerData.SERVER.getPlayerCustomData(player);
        data.nbt.putDouble("bank_z_coin", getDeposited(player.getUUID(), ZEconomy.PRIMARY_CURRENCY_ID));
        data.nbt.putDouble("bank_b_coin", getDeposited(player.getUUID(), ZEconomy.SECONDARY_CURRENCY_ID));
        data.nbt.putDouble("vault_z_coin", getVaultBalance(player.getUUID(), ZEconomy.PRIMARY_CURRENCY_ID));
        data.nbt.putDouble("vault_b_coin", getVaultBalance(player.getUUID(), ZEconomy.SECONDARY_CURRENCY_ID));
        data.nbt.putInt("mail_pending", pendingMailCount(player.getUUID()));
        data.nbt.putInt("daily_streak", getDailyStreak(player.getUUID()));
        data.nbt.putBoolean("vault_has_pin", hasVaultPin(player.getUUID()));
    }

    public List<TransactionRecord> getRecentLogs(int limit) {
        int n = Math.max(1, limit);
        int start = Math.max(0, logs.size() - n);
        return new ArrayList<>(logs.subList(start, logs.size()));
    }

    public List<RichEntry> getTopRich(MinecraftServer server, String currency, int limit, boolean includeVault) {
        List<RichEntry> entries = new ArrayList<>();
        for (Map.Entry<UUID, LinkedList<CurrencyPlayerData.PlayerCurrency>> e : CurrencyHelper.getPlayerCurrencyServerData().playersCurrencyMap.entrySet()) {
            UUID id = e.getKey();
            if (CurrencyHelper.isServerAccount(id)) {
                continue;
            }
            double wallet = CurrencyHelper.getPlayerCurrencyServerData().getBalance(id, currency).value;
            double vault = includeVault ? getVaultBalance(id, currency) : 0.0;
            double total = wallet + vault;
            String name = id.toString();
            ServerPlayer online = server.getPlayerList().getPlayer(id);
            if (online != null) {
                name = online.getName().getString();
            }
            entries.add(new RichEntry(id, name, currency, total));
        }
        entries.sort(Comparator.comparingDouble((RichEntry r) -> r.amount).reversed());
        return entries.subList(0, Math.min(limit, entries.size()));
    }

    public double getDeposited(UUID playerId, String currency) {
        return bankDeposits.getOrDefault(playerId, Map.of()).getOrDefault(currency, 0.0);
    }

    public Map<String, Double> getAllDeposits(UUID playerId) {
        return new HashMap<>(bankDeposits.getOrDefault(playerId, Map.of()));
    }

    public double getTotalDeposited(String currency) {
        return getTotalBankDeposits(currency);
    }

    public double getServerSpendable(String currency) {
        return getServerSpendableBalance(currency);
    }

    private double getTotalBankDeposits(String currency) {
        return bankTotalsByCurrency.getOrDefault(currency, 0.0D);
    }

    private double getServerSpendableBalance(String currency) {
        double serverBalance = CurrencyHelper.getPlayerCurrencyServerData().getBalance(CurrencyHelper.getServerAccountUUID(), currency).value;
        double reserved = getTotalBankDeposits(currency);
        return Math.max(0.0D, serverBalance - reserved);
    }

    private boolean checkPin(UUID playerId, String pin) {
        String current = vaultPins.get(playerId);
        return current != null && current.equals(pin);
    }

    private void adjustBankTotal(String currency, double delta) {
        if (currency == null || currency.isBlank() || delta == 0.0D) {
            return;
        }
        double next = bankTotalsByCurrency.getOrDefault(currency, 0.0D) + delta;
        if (next <= 0.0D) {
            bankTotalsByCurrency.remove(currency);
            return;
        }
        bankTotalsByCurrency.put(currency, next);
    }

    private void rebuildCaches() {
        bankTotalsByCurrency.clear();
        for (Map<String, Double> account : bankDeposits.values()) {
            for (Map.Entry<String, Double> entry : account.entrySet()) {
                adjustBankTotal(entry.getKey(), entry.getValue());
            }
        }
    }

    private void log(String type, UUID actor, UUID target, String currency, double amount, double fee, String note) {
        logs.add(new TransactionRecord(Instant.now().getEpochSecond(), type, actor, target, currency, amount, fee, note));
        while (logs.size() > MAX_LOGS) {
            logs.removeFirst();
        }
    }

    private String rateKey(String from, String to) {
        return from + "->" + to;
    }

    private CompoundTag write() {
        CompoundTag root = new CompoundTag();
        root.putLong("lastExportEpochSec", lastExportEpochSec);

        ListTag bank = new ListTag();
        for (Map.Entry<UUID, Map<String, Double>> playerEntry : bankDeposits.entrySet()) {
            CompoundTag player = new CompoundTag();
            player.putUUID("uuid", playerEntry.getKey());
            ListTag deposits = new ListTag();
            for (Map.Entry<String, Double> d : playerEntry.getValue().entrySet()) {
                CompoundTag dep = new CompoundTag();
                dep.putString("currency", d.getKey());
                dep.putDouble("amount", d.getValue());
                deposits.add(dep);
            }
            player.put("deposits", deposits);
            player.putLong("lastPayout", lastHourlyPayoutEpochSec.getOrDefault(playerEntry.getKey(), 0L));
            bank.add(player);
        }
        root.put("bank", bank);

        ListTag vault = new ListTag();
        for (Map.Entry<UUID, Map<String, Double>> playerEntry : vaultDeposits.entrySet()) {
            CompoundTag player = new CompoundTag();
            player.putUUID("uuid", playerEntry.getKey());
            ListTag deposits = new ListTag();
            for (Map.Entry<String, Double> d : playerEntry.getValue().entrySet()) {
                CompoundTag dep = new CompoundTag();
                dep.putString("currency", d.getKey());
                dep.putDouble("amount", d.getValue());
                deposits.add(dep);
            }
            player.put("deposits", deposits);
            vault.add(player);
        }
        root.put("vault", vault);

        ListTag pins = new ListTag();
        for (Map.Entry<UUID, String> pin : vaultPins.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("uuid", pin.getKey());
            t.putString("pin", pin.getValue());
            pins.add(t);
        }
        root.put("vaultPins", pins);

        ListTag rates = new ListTag();
        for (Map.Entry<String, Double> entry : exchangeRates.entrySet()) {
            CompoundTag tag = new CompoundTag();
            tag.putString("key", entry.getKey());
            tag.putDouble("rate", entry.getValue());
            rates.add(tag);
        }
        root.put("rates", rates);

        ListTag mails = new ListTag();
        for (Map.Entry<UUID, List<ItemStack>> mail : mailbox.entrySet()) {
            CompoundTag playerMail = new CompoundTag();
            playerMail.putUUID("uuid", mail.getKey());
            ListTag items = new ListTag();
            for (ItemStack stack : mail.getValue()) {
                items.add(stack.save(new CompoundTag()));
            }
            playerMail.put("items", items);
            mails.add(playerMail);
        }
        root.put("mail", mails);

        ListTag logsTag = new ListTag();
        for (TransactionRecord r : logs) {
            CompoundTag t = new CompoundTag();
            t.putLong("time", r.epochSec);
            t.putString("type", r.type);
            if (r.actor != null) {
                t.putUUID("actor", r.actor);
            }
            if (r.target != null) {
                t.putUUID("target", r.target);
            }
            t.putString("currency", r.currency);
            t.putDouble("amount", r.amount);
            t.putDouble("fee", r.fee);
            t.putString("note", r.note);
            logsTag.add(t);
        }
        root.put("logs", logsTag);

        ListTag daily = new ListTag();
        for (Map.Entry<UUID, DailyState> e : dailyRewards.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("uuid", e.getKey());
            t.putLong("lastDay", e.getValue().lastClaimDay);
            t.putInt("streak", e.getValue().streak);
            daily.add(t);
        }
        root.put("daily", daily);
        return root;
    }

    private void read(CompoundTag root) {
        bankDeposits.clear();
        bankTotalsByCurrency.clear();
        lastHourlyPayoutEpochSec.clear();
        mailbox.clear();
        exchangeRates.clear();
        vaultDeposits.clear();
        vaultPins.clear();
        logs.clear();
        dailyRewards.clear();
        lastExportEpochSec = root.getLong("lastExportEpochSec");

        for (Tag tag : root.getList("bank", Tag.TAG_COMPOUND)) {
            if (!(tag instanceof CompoundTag player)) {
                continue;
            }
            UUID id = player.getUUID("uuid");
            Map<String, Double> deposits = new HashMap<>();
            for (Tag depositTag : player.getList("deposits", Tag.TAG_COMPOUND)) {
                if (depositTag instanceof CompoundTag dep) {
                    deposits.put(dep.getString("currency"), dep.getDouble("amount"));
                }
            }
            bankDeposits.put(id, deposits);
            lastHourlyPayoutEpochSec.put(id, player.getLong("lastPayout"));
        }

        for (Tag tag : root.getList("vault", Tag.TAG_COMPOUND)) {
            if (!(tag instanceof CompoundTag player)) {
                continue;
            }
            UUID id = player.getUUID("uuid");
            Map<String, Double> deposits = new HashMap<>();
            for (Tag depositTag : player.getList("deposits", Tag.TAG_COMPOUND)) {
                if (depositTag instanceof CompoundTag dep) {
                    deposits.put(dep.getString("currency"), dep.getDouble("amount"));
                }
            }
            vaultDeposits.put(id, deposits);
        }

        for (Tag tag : root.getList("vaultPins", Tag.TAG_COMPOUND)) {
            if (tag instanceof CompoundTag p) {
                vaultPins.put(p.getUUID("uuid"), p.getString("pin"));
            }
        }

        for (Tag tag : root.getList("rates", Tag.TAG_COMPOUND)) {
            if (tag instanceof CompoundTag rate) {
                String[] parts = rate.getString("key").split("->", 2);
                if (parts.length == 2) {
                    exchangeRates.put(rateKey(parts[0], parts[1]), rate.getDouble("rate"));
                }
            }
        }

        for (Tag tag : root.getList("mail", Tag.TAG_COMPOUND)) {
            if (!(tag instanceof CompoundTag mailTag)) {
                continue;
            }
            UUID id = mailTag.getUUID("uuid");
            List<ItemStack> items = new ArrayList<>();
            for (Tag itemTag : mailTag.getList("items", Tag.TAG_COMPOUND)) {
                if (itemTag instanceof CompoundTag stackTag) {
                    items.add(ItemStack.of(stackTag));
                }
            }
            mailbox.put(id, items);
        }

        for (Tag tag : root.getList("logs", Tag.TAG_COMPOUND)) {
            if (!(tag instanceof CompoundTag l)) {
                continue;
            }
            TransactionRecord r = new TransactionRecord(
                l.getLong("time"),
                l.getString("type"),
                l.hasUUID("actor") ? l.getUUID("actor") : null,
                l.hasUUID("target") ? l.getUUID("target") : null,
                l.getString("currency"),
                l.getDouble("amount"),
                l.getDouble("fee"),
                l.getString("note")
            );
            logs.add(r);
        }

        for (Tag tag : root.getList("daily", Tag.TAG_COMPOUND)) {
            if (tag instanceof CompoundTag d) {
                dailyRewards.put(d.getUUID("uuid"), new DailyState(d.getLong("lastDay"), d.getInt("streak")));
            }
        }
        rebuildCaches();
    }

    public record DailyClaimResult(boolean success, double zReward, double bReward, int streak) {
    }

    public record RichEntry(UUID playerId, String playerName, String currency, double amount) {
    }

    public record TransactionRecord(long epochSec, String type, UUID actor, UUID target, String currency, double amount, double fee, String note) {
    }

    private static class DailyState {
        long lastClaimDay;
        int streak;

        DailyState(long lastClaimDay, int streak) {
            this.lastClaimDay = lastClaimDay;
            this.streak = streak;
        }
    }
}
