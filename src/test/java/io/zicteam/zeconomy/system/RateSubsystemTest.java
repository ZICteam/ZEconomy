package io.zicteam.zeconomy.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class RateSubsystemTest {
    @Test
    void rateSubsystemStoresAndRemovesPairs() {
        RateSubsystem subsystem = new RateSubsystem();

        subsystem.setRate("z_coin", "b_coin", 2.5D);
        subsystem.setRate("b_coin", "z_coin", 0.4D);

        assertEquals(2.5D, subsystem.getRate("z_coin", "b_coin"));
        assertEquals(0.4D, subsystem.getRate("b_coin", "z_coin"));
        assertEquals(Map.of("z_coin->b_coin", 2.5D, "b_coin->z_coin", 0.4D), subsystem.getAllRates());

        assertTrue(subsystem.removeRate("z_coin", "b_coin"));
        assertEquals(0.0D, subsystem.getRate("z_coin", "b_coin"));
        assertFalse(subsystem.removeRate("z_coin", "b_coin"));
    }
}
