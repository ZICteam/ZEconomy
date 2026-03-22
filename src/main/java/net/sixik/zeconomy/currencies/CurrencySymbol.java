package net.sixik.zeconomy.currencies;

import net.minecraft.nbt.CompoundTag;

public class CurrencySymbol {
    public String value;
    public Type type;

    public CurrencySymbol(String value) {
        this(value, Type.CHAR);
    }

    public CurrencySymbol(String value, Type type) {
        this.value = value == null ? "" : value;
        this.type = type == null ? Type.CHAR : type;
    }

    public CurrencySymbol copy() {
        return new CurrencySymbol(this.value, this.type);
    }

    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putString("value", this.value);
        tag.putString("type", this.type.name());
        return tag;
    }

    public static CurrencySymbol deserialize(CompoundTag tag) {
        if (tag == null) {
            return new CurrencySymbol("");
        }
        String value = tag.getString("value");
        Type type = Type.CHAR;
        if (tag.contains("type")) {
            try {
                type = Type.valueOf(tag.getString("type"));
            } catch (IllegalArgumentException ignored) {
                type = Type.CHAR;
            }
        }
        return new CurrencySymbol(value, type);
    }

    public enum Type {
        CHAR,
        ICON
    }
}
