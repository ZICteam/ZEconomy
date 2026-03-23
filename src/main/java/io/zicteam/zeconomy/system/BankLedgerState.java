package io.zicteam.zeconomy.system;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

final class BankLedgerState {
    private final Map<UUID, Map<String, Double>> depositsByPlayer = new HashMap<>();
    private final Map<String, Double> totalsByCurrency = new HashMap<>();
    private final Map<UUID, Long> lastHourlyPayoutEpochSec = new HashMap<>();

    double getDeposited(UUID playerId, String currency) {
        return depositsByPlayer.getOrDefault(playerId, Map.of()).getOrDefault(currency, 0.0D);
    }

    Map<String, Double> getAllDeposits(UUID playerId) {
        return new HashMap<>(depositsByPlayer.getOrDefault(playerId, Map.of()));
    }

    double getTotalDeposited(String currency) {
        return totalsByCurrency.getOrDefault(currency, 0.0D);
    }

    double getServerSpendable(double serverBalance, String currency) {
        return Math.max(0.0D, serverBalance - getTotalDeposited(currency));
    }

    long getLastHourlyPayoutEpochSec(UUID playerId, long defaultValue) {
        return lastHourlyPayoutEpochSec.getOrDefault(playerId, defaultValue);
    }

    void setLastHourlyPayoutEpochSec(UUID playerId, long epochSec) {
        if (playerId == null) {
            return;
        }
        lastHourlyPayoutEpochSec.put(playerId, epochSec);
    }

    Map<String, Double> getAccountDeposits(UUID playerId) {
        return depositsByPlayer.getOrDefault(playerId, Map.of());
    }

    void addDeposit(UUID playerId, String currency, double amount) {
        if (playerId == null || currency == null || currency.isBlank() || amount == 0.0D) {
            return;
        }
        setDeposit(playerId, currency, getDeposited(playerId, currency) + amount);
    }

    void setDeposit(UUID playerId, String currency, double amount) {
        if (playerId == null || currency == null || currency.isBlank()) {
            return;
        }
        Map<String, Double> account = depositsByPlayer.get(playerId);
        double previous = account == null ? 0.0D : account.getOrDefault(currency, 0.0D);
        double next = Math.max(0.0D, amount);
        if (Double.compare(previous, next) == 0) {
            return;
        }
        if (account == null && next > 0.0D) {
            account = new HashMap<>();
            depositsByPlayer.put(playerId, account);
        }
        if (next <= 0.0D) {
            if (account == null) {
                return;
            }
            account.remove(currency);
            if (account.isEmpty()) {
                depositsByPlayer.remove(playerId);
            }
        } else {
            account.put(currency, next);
        }
        adjustTotal(currency, next - previous);
        DataStorageManager.markDirty();
    }

    void clear() {
        depositsByPlayer.clear();
        totalsByCurrency.clear();
        lastHourlyPayoutEpochSec.clear();
    }

    void readFrom(CompoundTag root) {
        clear();
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
            depositsByPlayer.put(id, deposits);
            lastHourlyPayoutEpochSec.put(id, player.getLong("lastPayout"));
        }
        rebuildCaches();
    }

    void writeTo(CompoundTag root) {
        ListTag bank = new ListTag();
        for (Map.Entry<UUID, Map<String, Double>> playerEntry : depositsByPlayer.entrySet()) {
            CompoundTag player = new CompoundTag();
            player.putUUID("uuid", playerEntry.getKey());
            ListTag deposits = new ListTag();
            for (Map.Entry<String, Double> depositEntry : playerEntry.getValue().entrySet()) {
                CompoundTag dep = new CompoundTag();
                dep.putString("currency", depositEntry.getKey());
                dep.putDouble("amount", depositEntry.getValue());
                deposits.add(dep);
            }
            player.put("deposits", deposits);
            player.putLong("lastPayout", lastHourlyPayoutEpochSec.getOrDefault(playerEntry.getKey(), 0L));
            bank.add(player);
        }
        root.put("bank", bank);
    }

    private void rebuildCaches() {
        totalsByCurrency.clear();
        for (Map<String, Double> account : depositsByPlayer.values()) {
            for (Map.Entry<String, Double> entry : account.entrySet()) {
                adjustTotal(entry.getKey(), entry.getValue());
            }
        }
    }

    private void adjustTotal(String currency, double delta) {
        if (currency == null || currency.isBlank() || delta == 0.0D) {
            return;
        }
        double next = totalsByCurrency.getOrDefault(currency, 0.0D) + delta;
        if (next <= 0.0D) {
            totalsByCurrency.remove(currency);
            return;
        }
        totalsByCurrency.put(currency, next);
    }
}
