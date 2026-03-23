package io.zicteam.zeconomy.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.zicteam.zeconomy.utils.CurrencyHelper;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EconomyOperationDeltaPlansTest {
    @Test
    void transferDeltaPlanIncludesSenderRecipientAndFeeCollector() {
        UUID from = UUID.randomUUID();
        UUID to = UUID.randomUUID();
        EconomyOperationService.TransferQuote quote = EconomyOperationService.computeTransferQuote(100.0D, 0.05D);

        EconomyOperationService.BalanceDelta[] deltas = EconomyOperationService.buildTransferDeltas(from, to, "z_coin", quote);

        assertEquals(from, deltas[0].playerId());
        assertEquals("z_coin", deltas[0].currencyId());
        assertEquals(-105.0D, deltas[0].amount());

        assertEquals(to, deltas[1].playerId());
        assertEquals(100.0D, deltas[1].amount());

        assertEquals(CurrencyHelper.getServerAccountUUID(), deltas[2].playerId());
        assertEquals(5.0D, deltas[2].amount());
    }

    @Test
    void transferDeltaPlanOmitsFeeCollectorWhenFeeIsZero() {
        UUID from = UUID.randomUUID();
        UUID to = UUID.randomUUID();
        EconomyOperationService.TransferQuote quote = EconomyOperationService.computeTransferQuote(100.0D, 0.0D);

        EconomyOperationService.BalanceDelta[] deltas = EconomyOperationService.buildTransferDeltas(from, to, "z_coin", quote);

        assertNull(deltas[2]);
    }

    @Test
    void bankWithdrawDeltaPlanMovesFundsFromServerToPlayer() {
        UUID playerId = UUID.randomUUID();

        EconomyOperationService.BalanceDelta[] deltas = EconomyOperationService.buildBankWithdrawDeltas(playerId, "b_coin", 25.0D);

        assertEquals(CurrencyHelper.getServerAccountUUID(), deltas[0].playerId());
        assertEquals("b_coin", deltas[0].currencyId());
        assertEquals(-25.0D, deltas[0].amount());

        assertEquals(playerId, deltas[1].playerId());
        assertEquals(25.0D, deltas[1].amount());
    }

    @Test
    void exchangeDeltaPlanUsesGrossForTreasuryAndNetForPlayer() {
        UUID playerId = UUID.randomUUID();
        EconomyOperationService.ExchangeQuote quote = EconomyOperationService.computeExchangeQuote(20.0D, 4.0D, 0.10D);

        EconomyOperationService.BalanceDelta[] deltas = EconomyOperationService.buildExchangeDeltas(playerId, "z_coin", "b_coin", quote);

        assertEquals(playerId, deltas[0].playerId());
        assertEquals("z_coin", deltas[0].currencyId());
        assertEquals(-20.0D, deltas[0].amount());

        assertEquals(CurrencyHelper.getServerAccountUUID(), deltas[1].playerId());
        assertEquals("z_coin", deltas[1].currencyId());
        assertEquals(20.0D, deltas[1].amount());

        assertEquals(CurrencyHelper.getServerAccountUUID(), deltas[2].playerId());
        assertEquals("b_coin", deltas[2].currencyId());
        assertEquals(-80.0D, deltas[2].amount());

        assertEquals(playerId, deltas[3].playerId());
        assertEquals("b_coin", deltas[3].currencyId());
        assertEquals(72.0D, deltas[3].amount());
    }
}
