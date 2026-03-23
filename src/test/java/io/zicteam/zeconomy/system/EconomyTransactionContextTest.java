package io.zicteam.zeconomy.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class EconomyTransactionContextTest {
    @Test
    void transactionContextCopiesEffectMetadata() {
        UUID playerA = UUID.randomUUID();
        UUID playerB = UUID.randomUUID();

        EconomyOperationEffects effects = EconomyOperationEffects.none()
            .touchPlayer(playerA)
            .touchPlayer(playerB)
            .requestSave()
            .requestCurrencyDataSync()
            .afterCommit(() -> {
            })
            .afterCommit(() -> {
            });

        EconomyTransactionContext context = new EconomyTransactionContext(effects);

        assertEquals(2, context.touchedPlayers().size());
        assertTrue(context.touchedPlayers().contains(playerA));
        assertTrue(context.touchedPlayers().contains(playerB));
        assertTrue(context.saveRequested());
        assertTrue(context.currencyDataSyncRequested());
        assertEquals(2, context.postCommitActionCount());
    }

    @Test
    void emptyTransactionContextStartsWithoutFlagsOrTouches() {
        EconomyTransactionContext context = new EconomyTransactionContext();

        assertTrue(context.touchedPlayers().isEmpty());
        assertFalse(context.saveRequested());
        assertFalse(context.currencyDataSyncRequested());
        assertEquals(0, context.postCommitActionCount());
    }
}
