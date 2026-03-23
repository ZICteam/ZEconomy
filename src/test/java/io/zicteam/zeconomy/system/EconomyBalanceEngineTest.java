package io.zicteam.zeconomy.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zicteam.zeconomy.currencies.BaseCurrency;
import io.zicteam.zeconomy.currencies.CurrencySymbol;
import io.zicteam.zeconomy.currencies.data.CurrencyData;
import io.zicteam.zeconomy.currencies.data.CurrencyPlayerData;
import java.util.LinkedList;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EconomyBalanceEngineTest {
    private CurrencyData previousCurrencyData;
    private CurrencyPlayerData.Server previousPlayerData;

    @BeforeEach
    void setUp() {
        previousCurrencyData = CurrencyData.SERVER;
        previousPlayerData = CurrencyPlayerData.SERVER;

        CurrencyData.SERVER = new CurrencyData(new LinkedList<>());
        CurrencyData.SERVER.currencies.add(new BaseCurrency("z_coin", new CurrencySymbol("Z"), 100.0D));
        CurrencyData.SERVER.currencies.add(new BaseCurrency("b_coin", new CurrencySymbol("B"), 50.0D));
        CurrencyPlayerData.SERVER = new CurrencyPlayerData.Server();
    }

    @AfterEach
    void tearDown() {
        CurrencyData.SERVER = previousCurrencyData;
        CurrencyPlayerData.SERVER = previousPlayerData;
    }

    @Test
    void applyBalanceDeltasAppliesValidGroupedChanges() {
        UUID playerA = UUID.randomUUID();
        UUID playerB = UUID.randomUUID();

        boolean ok = EconomyOperationService.applyBalanceDeltas(
            new EconomyOperationService.BalanceDelta(playerA, "z_coin", -25.0D),
            new EconomyOperationService.BalanceDelta(playerB, "z_coin", 25.0D),
            new EconomyOperationService.BalanceDelta(playerA, "b_coin", 10.0D)
        );

        assertTrue(ok);
        assertEquals(75.0D, CurrencyPlayerData.SERVER.getBalance(playerA, "z_coin").value);
        assertEquals(125.0D, CurrencyPlayerData.SERVER.getBalance(playerB, "z_coin").value);
        assertEquals(60.0D, CurrencyPlayerData.SERVER.getBalance(playerA, "b_coin").value);
    }

    @Test
    void applyBalanceDeltasRejectsOverdraftWithoutPartialMutation() {
        UUID playerA = UUID.randomUUID();
        UUID playerB = UUID.randomUUID();

        CurrencyPlayerData.SERVER.newPlayer(playerA);
        CurrencyPlayerData.SERVER.newPlayer(playerB);

        boolean ok = EconomyOperationService.applyBalanceDeltas(
            new EconomyOperationService.BalanceDelta(playerA, "z_coin", -25.0D),
            new EconomyOperationService.BalanceDelta(playerB, "z_coin", 25.0D),
            new EconomyOperationService.BalanceDelta(playerA, "b_coin", -75.0D)
        );

        assertFalse(ok);
        assertEquals(100.0D, CurrencyPlayerData.SERVER.getBalance(playerA, "z_coin").value);
        assertEquals(100.0D, CurrencyPlayerData.SERVER.getBalance(playerB, "z_coin").value);
        assertEquals(50.0D, CurrencyPlayerData.SERVER.getBalance(playerA, "b_coin").value);
    }

    @Test
    void applyBalanceDeltasIgnoresNoOpEntries() {
        UUID playerId = UUID.randomUUID();

        boolean ok = EconomyOperationService.applyBalanceDeltas(
            null,
            new EconomyOperationService.BalanceDelta(playerId, "z_coin", 0.0D),
            new EconomyOperationService.BalanceDelta(null, "z_coin", 10.0D),
            new EconomyOperationService.BalanceDelta(playerId, "", 10.0D)
        );

        assertTrue(ok);
        assertEquals(0.0D, CurrencyPlayerData.SERVER.playersCurrencyMap.size());
    }
}
