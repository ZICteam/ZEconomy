package io.zicteam.zeconomy.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class EconomyValidationServiceTest {
    @Test
    void requireValidPinAcceptsDigitOnlyLengthRange() {
        assertNull(EconomyValidationService.requireValidPin("1234"));
        assertNull(EconomyValidationService.requireValidPin("123456789012"));
    }

    @Test
    void requireValidPinRejectsInvalidValues() {
        assertEquals(EconomyOperationService.OperationFailure.INVALID_PIN, EconomyValidationService.requireValidPin(null));
        assertEquals(EconomyOperationService.OperationFailure.INVALID_PIN, EconomyValidationService.requireValidPin("123"));
        assertEquals(EconomyOperationService.OperationFailure.INVALID_PIN, EconomyValidationService.requireValidPin("1234567890123"));
        assertEquals(EconomyOperationService.OperationFailure.INVALID_PIN, EconomyValidationService.requireValidPin("12a4"));
    }

    @Test
    void mailSendValidationRejectsDisabledMailboxBeforeOtherChecks() {
        assertEquals(
            EconomyOperationService.OperationFailure.FEATURE_DISABLED,
            EconomyValidationService.evaluateMailSendValidation(false, false, true)
        );
    }

    @Test
    void mailSendValidationRejectsSelfTarget() {
        assertEquals(
            EconomyOperationService.OperationFailure.SELF_TARGET,
            EconomyValidationService.evaluateMailSendValidation(true, true, true)
        );
    }

    @Test
    void mailSendValidationRejectsEmptyHand() {
        assertEquals(
            EconomyOperationService.OperationFailure.EMPTY_HAND,
            EconomyValidationService.evaluateMailSendValidation(true, false, false)
        );
    }

    @Test
    void mailSendValidationPassesForEnabledMailboxDifferentTargetAndHeldItem() {
        assertNull(EconomyValidationService.evaluateMailSendValidation(true, false, true));
    }
}
