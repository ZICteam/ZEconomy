package io.zicteam.zeconomy.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DailyRewardFacadeTest {
    @Test
    void applyClaimUpdatesStreakAndTodayFlag() {
        DailyRewardFacade facade = new DailyRewardFacade();
        UUID playerId = UUID.randomUUID();
        long today = LocalDate.now(ZoneOffset.UTC).toEpochDay();

        ExtraEconomyData.DailyClaimResult result = facade.applyClaim(playerId, today, 4, 120.0D, 2.0D);

        assertTrue(result.success());
        assertEquals(4, facade.getStreak(playerId));
        assertEquals(today, facade.getLastClaimDay(playerId));
        assertTrue(facade.hasClaimedToday(playerId));
        assertFalse(facade.hasClaimedToday(UUID.randomUUID()));
    }
}
