package io.zicteam.zeconomy.currencies.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zicteam.zeconomy.currencies.BaseCurrency;
import io.zicteam.zeconomy.currencies.CurrencySymbol;
import java.util.LinkedList;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CurrencyPlayerDataServerTest {
    private CurrencyData previousCurrencyData;
    private CurrencyPlayerData.Server previousPlayerData;

    @BeforeEach
    void setUp() {
        previousCurrencyData = CurrencyData.SERVER;
        previousPlayerData = CurrencyPlayerData.SERVER;

        CurrencyData.SERVER = new CurrencyData(new LinkedList<>());
        CurrencyData.SERVER.currencies.add(new BaseCurrency("z_coin", new CurrencySymbol("Z"), 10.0D));
        CurrencyData.SERVER.currencies.add(new BaseCurrency("b_coin", new CurrencySymbol("B"), 1.0D));
        CurrencyPlayerData.SERVER = new CurrencyPlayerData.Server();
    }

    @AfterEach
    void tearDown() {
        CurrencyData.SERVER = previousCurrencyData;
        CurrencyPlayerData.SERVER = previousPlayerData;
    }

    @Test
    void newPlayerInitializesEntriesFromServerCurrencyDefaults() {
        UUID playerId = UUID.randomUUID();

        CurrencyPlayerData.SERVER.newPlayer(playerId);

        assertEquals(10.0D, CurrencyPlayerData.SERVER.getBalance(playerId, "z_coin").value);
        assertEquals(1.0D, CurrencyPlayerData.SERVER.getBalance(playerId, "b_coin").value);
    }

    @Test
    void serverSerializeRoundTripPreservesBalancesAndLockFlags() {
        UUID playerId = UUID.randomUUID();
        LinkedList<CurrencyPlayerData.PlayerCurrency> currencies = new LinkedList<>();
        currencies.add(new CurrencyPlayerData.PlayerCurrency(new BaseCurrency("z_coin", new CurrencySymbol("Z"), 0.0D), 42.0D).setLocked(true));
        currencies.add(new CurrencyPlayerData.PlayerCurrency(new BaseCurrency("b_coin", new CurrencySymbol("B"), 0.0D), 7.0D).setLocked(false));
        CurrencyPlayerData.SERVER.playersCurrencyMap.put(playerId, currencies);

        CurrencyPlayerData.Server restored = CurrencyPlayerData.Server.deserialize(null, CurrencyPlayerData.SERVER.serialize());

        assertEquals(42.0D, restored.getBalance(playerId, "z_coin").value);
        assertEquals(7.0D, restored.getBalance(playerId, "b_coin").value);
        assertTrue(restored.getPlayerCurrency(playerId, "z_coin").orElseThrow().isLocked);
    }
}
