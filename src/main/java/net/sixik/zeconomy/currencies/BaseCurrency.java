package net.sixik.zeconomy.currencies;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
public class BaseCurrency {
    protected final String name;
    protected final double defaultValue;
    public CurrencySymbol symbol;
    public boolean canDelete;

    public BaseCurrency(String name) {
        this(name, new CurrencySymbol("$"), 0.0);
    }

    public BaseCurrency(String name, CurrencySymbol symbol) {
        this(name, symbol, 0.0);
    }

    public BaseCurrency(String name, CurrencySymbol symbol, double defaultValue) {
        this.name = name;
        this.symbol = symbol == null ? new CurrencySymbol("$") : symbol;
        this.defaultValue = defaultValue;
        this.canDelete = true;
    }

    public BaseCurrency canDelete(boolean value) {
        this.canDelete = value;
        return this;
    }

    public BaseCurrency copy() {
        return new BaseCurrency(this.name, this.symbol.copy(), this.defaultValue).canDelete(this.canDelete);
    }

    public String getName() {
        return this.name;
    }

    public double getDefaultValue() {
        return this.defaultValue;
    }

    public Component getTranslation() {
        return Component.literal(this.name);
    }

    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putString("name", this.name);
        tag.putDouble("defaultValue", this.defaultValue);
        tag.putBoolean("canDelete", this.canDelete);
        tag.put("symbol", this.symbol.serialize());
        return tag;
    }

    public static BaseCurrency deserialize(CompoundTag tag) {
        String name = tag.getString("name");
        CurrencySymbol symbol = tag.contains("symbol") ? CurrencySymbol.deserialize(tag.getCompound("symbol")) : new CurrencySymbol("$");
        double defaultValue = tag.contains("defaultValue") ? tag.getDouble("defaultValue") : 0.0;
        BaseCurrency currency = new BaseCurrency(name, symbol, defaultValue);
        if (tag.contains("canDelete")) {
            currency.canDelete(tag.getBoolean("canDelete"));
        }
        return currency;
    }
}
