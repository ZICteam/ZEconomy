package io.zicteam.zeconomy.currencies.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zicteam.zeconomy.ZEconomy;
import io.zicteam.zeconomy.currencies.BaseCurrency;
import io.zicteam.zeconomy.currencies.CurrencySymbol;
import java.util.LinkedList;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class CurrencyDataTest {
    @Test
    void deserializeAddsCoreCurrenciesWhenMissing() {
        CurrencyData data = CurrencyData.deserialize(new CompoundTag());

        assertTrue(data.currencies.stream().anyMatch(currency -> currency.getName().equals(ZEconomy.PRIMARY_CURRENCY_ID)));
        assertTrue(data.currencies.stream().anyMatch(currency -> currency.getName().equals(ZEconomy.SECONDARY_CURRENCY_ID)));
    }

    @Test
    void serializeRoundTripPreservesCustomCurrencyFields() {
        CurrencyData original = new CurrencyData(new LinkedList<>());
        original.currencies.add(new BaseCurrency("emerald", new CurrencySymbol("E"), 12.5D).canDelete(false));

        CurrencyData restored = CurrencyData.deserialize(original.serialize());

        BaseCurrency emerald = restored.currencies.stream()
            .filter(currency -> currency.getName().equals("emerald"))
            .findFirst()
            .orElseThrow();

        assertEquals(12.5D, emerald.getDefaultValue());
        assertEquals("E", emerald.symbol.value);
        assertFalse(emerald.canDelete);
    }
}
