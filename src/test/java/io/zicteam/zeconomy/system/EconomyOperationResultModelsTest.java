package io.zicteam.zeconomy.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class EconomyOperationResultModelsTest {
    @Test
    void dailyOperationResultExposesFailureFromWrappedOperation() {
        EconomyOperationService.DailyOperationResult result = new EconomyOperationService.DailyOperationResult(
            EconomyOperationService.OperationResult.failed(EconomyOperationService.OperationFailure.ALREADY_CLAIMED),
            new ExtraEconomyData.DailyClaimResult(false, 0.0D, 0.0D, 4)
        );

        assertFalse(result.success());
        assertEquals(EconomyOperationService.OperationFailure.ALREADY_CLAIMED, result.failure());
        assertEquals(4, result.reward().streak());
    }

    @Test
    void mailSendResultSuccessPreservesPayloadMetadata() {
        EconomyOperationService.MailSendResult result = EconomyOperationService.MailSendResult.succeeded(
            null,
            2,
            EconomyOperationEffects.none()
        );

        assertTrue(result.success());
        assertEquals(2, result.targetPendingMail());
        assertNull(result.sentStack());
    }

    @Test
    void mailClaimResultFailureReturnsEmptyItemsAndZeroCount() {
        EconomyOperationService.MailClaimResult result = EconomyOperationService.MailClaimResult.failed(
            EconomyOperationService.OperationFailure.EMPTY_MAILBOX
        );

        assertFalse(result.success());
        assertEquals(EconomyOperationService.OperationFailure.EMPTY_MAILBOX, result.failure());
        assertTrue(result.items().isEmpty());
        assertEquals(0, result.itemCount());
    }

    @Test
    void mailClaimResultSuccessReportsItemCountFromClaimedPayload() {
        EconomyOperationService.MailClaimResult result = EconomyOperationService.MailClaimResult.succeeded(
            Arrays.asList(null, null),
            EconomyOperationEffects.none()
        );

        assertTrue(result.success());
        assertEquals(2, result.itemCount());
    }
}
