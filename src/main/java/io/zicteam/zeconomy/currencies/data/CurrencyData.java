package io.zicteam.zeconomy.currencies.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import io.zicteam.zeconomy.ZEconomy;
import io.zicteam.zeconomy.currencies.BaseCurrency;
import io.zicteam.zeconomy.currencies.CurrencySymbol;

public class CurrencyData {
    public static CurrencyData EMPTY = new CurrencyData(new LinkedList<>());
    public static CurrencyData SERVER = new CurrencyData(new LinkedList<>());
    public static CurrencyData CLIENT = new CurrencyData(new LinkedList<>());
    public MinecraftServer server;
    public LinkedList<BaseCurrency> currencies;

    public CurrencyData(LinkedList<BaseCurrency> currencies) {
        this.currencies = currencies == null ? new LinkedList<>() : currencies;
    }

    public void reloadCurrenciesFromFile(Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try {
            CompoundTag tag = NbtIo.readCompressed(path.toFile());
            if (tag != null) {
                CurrencyData loaded = deserialize(tag);
                this.currencies = loaded.currencies;
            }
        } catch (IOException e) {
            ZEconomy.printStackTrace("Failed to load currency data", e);
        }
    }

    public void reloadCurrenciesFromNetwork(CompoundTag tag) {
        CurrencyData loaded = deserialize(tag);
        this.currencies = loaded.currencies;
    }

    public CompoundTag serialize() {
        CompoundTag root = new CompoundTag();
        ListTag list = new ListTag();
        for (BaseCurrency currency : this.currencies) {
            list.add(currency.serialize());
        }
        root.put("currencies", list);
        return root;
    }

    public static CurrencyData deserialize(CompoundTag tag) {
        LinkedList<BaseCurrency> result = new LinkedList<>();
        if (tag == null || !tag.contains("currencies", Tag.TAG_LIST)) {
            return new CurrencyData(result);
        }
        ListTag list = tag.getList("currencies", Tag.TAG_COMPOUND);
        for (Tag element : list) {
            if (element instanceof CompoundTag compound) {
                result.add(BaseCurrency.deserialize(compound));
            }
        }
        if (result.stream().noneMatch(c -> c.getName().equals(ZEconomy.PRIMARY_CURRENCY_ID))) {
            result.add(new BaseCurrency(ZEconomy.PRIMARY_CURRENCY_ID, new CurrencySymbol("\u25ce"), 0.0).canDelete(false));
        }
        if (result.stream().noneMatch(c -> c.getName().equals(ZEconomy.SECONDARY_CURRENCY_ID))) {
            result.add(new BaseCurrency(ZEconomy.SECONDARY_CURRENCY_ID, new CurrencySymbol("B"), 0.0).canDelete(false));
        }
        return new CurrencyData(result);
    }

    public CurrencyData copy() {
        LinkedList<BaseCurrency> copy = new LinkedList<>();
        for (BaseCurrency currency : this.currencies) {
            copy.add(currency.copy());
        }
        return new CurrencyData(copy);
    }
}
