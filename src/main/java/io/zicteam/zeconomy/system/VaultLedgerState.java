package io.zicteam.zeconomy.system;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

final class VaultLedgerState {
    private final Map<UUID, Map<String, Double>> depositsByPlayer = new HashMap<>();
    private final Map<UUID, String> pinsByPlayer = new HashMap<>();

    boolean setPin(UUID playerId, String pin) {
        if (playerId == null || pin == null || pin.length() < 4 || pin.length() > 12) {
            return false;
        }
        for (char c : pin.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        pinsByPlayer.put(playerId, pin);
        DataStorageManager.markDirty();
        return true;
    }

    boolean hasPin(UUID playerId) {
        return pinsByPlayer.containsKey(playerId);
    }

    boolean matchesPin(UUID playerId, String pin) {
        String current = pinsByPlayer.get(playerId);
        return current != null && current.equals(pin);
    }

    double getBalance(UUID playerId, String currency) {
        return depositsByPlayer.getOrDefault(playerId, Map.of()).getOrDefault(currency, 0.0D);
    }

    Map<String, Double> getAllBalances(UUID playerId) {
        return new HashMap<>(depositsByPlayer.getOrDefault(playerId, Map.of()));
    }

    void addBalance(UUID playerId, String currency, double amount) {
        if (playerId == null || currency == null || currency.isBlank() || amount == 0.0D) {
            return;
        }
        setBalance(playerId, currency, getBalance(playerId, currency) + amount);
    }

    void setBalance(UUID playerId, String currency, double amount) {
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
        DataStorageManager.markDirty();
    }

    void clear() {
        depositsByPlayer.clear();
        pinsByPlayer.clear();
    }

    void writeTo(CompoundTag root) {
        ListTag vault = new ListTag();
        for (Map.Entry<UUID, Map<String, Double>> playerEntry : depositsByPlayer.entrySet()) {
            CompoundTag player = new CompoundTag();
            player.putUUID("uuid", playerEntry.getKey());
            ListTag deposits = new ListTag();
            for (Map.Entry<String, Double> entry : playerEntry.getValue().entrySet()) {
                CompoundTag dep = new CompoundTag();
                dep.putString("currency", entry.getKey());
                dep.putDouble("amount", entry.getValue());
                deposits.add(dep);
            }
            player.put("deposits", deposits);
            vault.add(player);
        }
        root.put("vault", vault);

        ListTag pins = new ListTag();
        for (Map.Entry<UUID, String> pinEntry : pinsByPlayer.entrySet()) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("uuid", pinEntry.getKey());
            tag.putString("pin", pinEntry.getValue());
            pins.add(tag);
        }
        root.put("vaultPins", pins);
    }

    void readFrom(CompoundTag root) {
        clear();
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
            depositsByPlayer.put(id, deposits);
        }
        for (Tag tag : root.getList("vaultPins", Tag.TAG_COMPOUND)) {
            if (tag instanceof CompoundTag pinTag) {
                pinsByPlayer.put(pinTag.getUUID("uuid"), pinTag.getString("pin"));
            }
        }
    }
}
