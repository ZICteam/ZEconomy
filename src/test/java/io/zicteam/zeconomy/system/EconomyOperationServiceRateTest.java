package io.zicteam.zeconomy.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zicteam.zeconomy.ZEconomy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EconomyOperationServiceRateTest {
    private ExtraEconomyData previousData;

    @BeforeEach
    void setUp() {
        previousData = ZEconomy.EXTRA_DATA;
        ZEconomy.EXTRA_DATA = new ExtraEconomyData();
    }

    @AfterEach
    void tearDown() {
        ZEconomy.EXTRA_DATA = previousData;
    }

    @Test
    void setAndClearRateOperateThroughServiceLayer() {
        EconomyOperationService.RateMutationResult setResult = EconomyOperationService.setRate("z_coin", "b_coin", 5.0D);

        assertTrue(setResult.success());
        assertEquals(5.0D, ZEconomy.EXTRA_DATA.getRate("z_coin", "b_coin"));

        EconomyOperationService.RateMutationResult clearResult = EconomyOperationService.clearRate("z_coin", "b_coin");

        assertTrue(clearResult.success());
        assertEquals(0.0D, ZEconomy.EXTRA_DATA.getRate("z_coin", "b_coin"));
    }
}
