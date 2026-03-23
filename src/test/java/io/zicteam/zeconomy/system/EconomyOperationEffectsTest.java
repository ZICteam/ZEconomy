package io.zicteam.zeconomy.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class EconomyOperationEffectsTest {
    @Test
    void effectsTrackDistinctTouchedPlayersAndFlags() {
        UUID playerA = UUID.randomUUID();
        UUID playerB = UUID.randomUUID();

        EconomyOperationEffects effects = EconomyOperationEffects.none()
            .touchPlayer(playerA)
            .touchPlayer(playerA)
            .touchPlayer(playerB)
            .requestSave()
            .requestCurrencyDataSync();

        assertEquals(2, effects.touchedPlayers().size());
        assertTrue(effects.touchedPlayers().contains(playerA));
        assertTrue(effects.touchedPlayers().contains(playerB));
        assertTrue(effects.saveRequested());
        assertTrue(effects.currencyDataSyncRequested());
    }

    @Test
    void effectsIgnoreNullPlayersAndNullActions() {
        EconomyOperationEffects effects = EconomyOperationEffects.none()
            .touchPlayer((UUID) null)
            .afterCommit(null);

        assertTrue(effects.touchedPlayers().isEmpty());
        assertTrue(effects.postCommitActions().isEmpty());
        assertFalse(effects.saveRequested());
        assertFalse(effects.currencyDataSyncRequested());
    }

    @Test
    void effectsPreservePostCommitActionOrder() {
        AtomicInteger order = new AtomicInteger();
        AtomicInteger first = new AtomicInteger();
        AtomicInteger second = new AtomicInteger();

        EconomyOperationEffects effects = EconomyOperationEffects.none()
            .afterCommit(() -> first.set(order.incrementAndGet()))
            .afterCommit(() -> second.set(order.incrementAndGet()));

        effects.postCommitActions().forEach(Runnable::run);

        assertEquals(1, first.get());
        assertEquals(2, second.get());
    }
}
