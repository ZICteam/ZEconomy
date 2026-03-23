package io.zicteam.zeconomy.system;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TransactionLogSubsystemTest {
    @Test
    void transactionLogSubsystemKeepsOnlyRecentEntriesUpToLimit() {
        TransactionLogSubsystem subsystem = new TransactionLogSubsystem();
        UUID actor = UUID.randomUUID();

        for (int i = 0; i < ExtraEconomyData.MAX_LOGS + 5; i++) {
            subsystem.record("TYPE_" + i, actor, null, "z_coin", i, 0.0D, "");
        }

        List<ExtraEconomyData.TransactionRecord> records = subsystem.recent(ExtraEconomyData.MAX_LOGS + 10);

        assertEquals(ExtraEconomyData.MAX_LOGS, subsystem.size());
        assertEquals(ExtraEconomyData.MAX_LOGS, records.size());
        assertEquals("TYPE_5", records.get(0).type());
        assertEquals("TYPE_" + (ExtraEconomyData.MAX_LOGS + 4), records.get(records.size() - 1).type());
    }
}
