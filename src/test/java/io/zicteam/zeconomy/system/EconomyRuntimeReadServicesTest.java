package io.zicteam.zeconomy.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zicteam.zeconomy.ZEconomy;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EconomyRuntimeReadServicesTest {
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
    void runtimeSnapshotReflectsLogsRatesAndExportTimestamp() {
        ZEconomy.EXTRA_DATA.setRate("z_coin", "b_coin", 4.0D);
        ZEconomy.EXTRA_DATA.setRate("b_coin", "z_coin", 0.25D);
        ZEconomy.EXTRA_DATA.markExported(777L);
        EconomyLogService.record("TRANSFER", UUID.randomUUID(), UUID.randomUUID(), "z_coin", 25.0D, 1.0D, "test");

        EconomySnapshotReadService.RuntimeSnapshot snapshot = EconomySnapshotReadService.runtime();

        assertEquals(1, snapshot.logCount());
        assertEquals(777L, snapshot.lastExportEpochSec());
        assertEquals(2, snapshot.exchangeRateCount());
    }

    @Test
    void recentLogsReturnNewestRecordsWithinLimit() {
        UUID actor = UUID.randomUUID();
        EconomyLogService.record("BANK_DEPOSIT", actor, null, "z_coin", 10.0D, 0.0D, "first");
        EconomyLogService.record("BANK_WITHDRAW", actor, null, "z_coin", 5.0D, 0.0D, "second");
        EconomyLogService.record("EXCHANGE", actor, null, "b_coin", 2.0D, 0.2D, "third");

        List<ExtraEconomyData.TransactionRecord> logs = EconomySnapshotReadService.recentLogs(2);

        assertEquals(2, logs.size());
        assertEquals("BANK_WITHDRAW", logs.get(0).type());
        assertEquals("EXCHANGE", logs.get(1).type());
    }

    @Test
    void exchangeRateReadServiceReturnsConfiguredRates() {
        ZEconomy.EXTRA_DATA.setRate("z_coin", "b_coin", 3.5D);

        assertEquals(3.5D, EconomySnapshotReadService.exchangeRate("z_coin", "b_coin"));
        assertTrue(EconomySnapshotReadService.exchangeRates().containsKey("z_coin->b_coin"));
        assertEquals(1, EconomySnapshotReadService.exchangeRateCount());
    }
}
