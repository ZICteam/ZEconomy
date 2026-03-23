package io.zicteam.zeconomy.system;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EconomyOperationCalculationsTest {
    @Test
    void transferQuoteCalculatesFeeAndTotal() {
        EconomyOperationService.TransferQuote quote = EconomyOperationService.computeTransferQuote(100.0D, 0.05D);

        assertEquals(100.0D, quote.amount());
        assertEquals(5.0D, quote.fee());
        assertEquals(105.0D, quote.total());
    }

    @Test
    void exchangeQuoteCalculatesGrossFeeAndNet() {
        EconomyOperationService.ExchangeQuote quote = EconomyOperationService.computeExchangeQuote(20.0D, 4.0D, 0.10D);

        assertEquals(20.0D, quote.amount());
        assertEquals(4.0D, quote.rate());
        assertEquals(80.0D, quote.gross());
        assertEquals(8.0D, quote.fee());
        assertEquals(72.0D, quote.net());
    }

    @Test
    void remainingDepositDoesNotGoBelowZero() {
        assertEquals(25.0D, EconomyOperationService.computeRemainingDeposit(50.0D, 25.0D));
        assertEquals(0.0D, EconomyOperationService.computeRemainingDeposit(10.0D, 25.0D));
    }
}
