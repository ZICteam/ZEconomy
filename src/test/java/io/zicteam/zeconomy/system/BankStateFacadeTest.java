package io.zicteam.zeconomy.system;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BankStateFacadeTest {
    @Test
    void depositStateTracksTotalsAndRemovesZeroEntries() {
        BankStateFacade facade = new BankStateFacade();
        UUID playerId = UUID.randomUUID();

        facade.addDepositState(playerId, "z_coin", 50.0D);
        facade.addDepositState(playerId, "z_coin", 25.0D);

        assertEquals(75.0D, facade.getDeposited(playerId, "z_coin"));
        assertEquals(75.0D, facade.getTotalDeposited("z_coin"));
        assertEquals(Map.of("z_coin", 75.0D), facade.getAllDeposits(playerId));

        facade.setDepositState(playerId, "z_coin", 0.0D);

        assertEquals(0.0D, facade.getDeposited(playerId, "z_coin"));
        assertEquals(0.0D, facade.getTotalDeposited("z_coin"));
        assertEquals(Map.of(), facade.getAllDeposits(playerId));
    }
}
