package io.zicteam.zeconomy.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VaultStateFacadeTest {
    @Test
    void vaultFacadeTracksBalancesAndPins() {
        VaultStateFacade facade = new VaultStateFacade();
        UUID playerId = UUID.randomUUID();

        assertTrue(facade.setPin(playerId, "1234"));
        assertTrue(facade.hasPin(playerId));
        assertTrue(facade.matchesPin(playerId, "1234"));
        assertFalse(facade.matchesPin(playerId, "9999"));

        facade.addBalance(playerId, "z_coin", 12.5D);
        facade.addBalance(playerId, "z_coin", 7.5D);
        facade.setBalance(playerId, "b_coin", 3.0D);

        assertEquals(20.0D, facade.getBalance(playerId, "z_coin"));
        assertEquals(3.0D, facade.getBalance(playerId, "b_coin"));
        assertEquals(Map.of("z_coin", 20.0D, "b_coin", 3.0D), facade.getAllBalances(playerId));
    }
}
