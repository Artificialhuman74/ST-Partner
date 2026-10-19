package com.sanathanatattva.partner

import com.sanathanatattva.partner.data.signup.Relation
import com.sanathanatattva.partner.data.signup.SignupDraft
import com.sanathanatattva.partner.data.signup.Vehicle
import com.sanathanatattva.partner.ui.review.RejectedViewModel
import com.sanathanatattva.partner.ui.signup.SignupRules
import com.sanathanatattva.partner.ui.signup.SignupViewModel
import com.sanathanatattva.partner.ui.signup.Step
import com.sanathanatattva.partner.ui.signup.mobileDigits
import com.sanathanatattva.partner.ui.signup.stepsFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SignupRulesTest {
    @Test fun walkingAndCycleRidersSkipTheLicenceStep() {
        assertEquals(8, stepsFor(SignupDraft(vehicle = Vehicle.Scooter)).size)
        assertFalse(Step.Licence in stepsFor(SignupDraft(vehicle = Vehicle.Walk)))
        assertFalse(Step.Licence in stepsFor(SignupDraft(vehicle = Vehicle.Cycle)))
        // Before choosing, show the longer path so "Step n of 8" doesn't jump backwards later.
        assertTrue(Step.Licence in stepsFor(SignupDraft()))
    }

    @Test fun mobileNumbersAreNormalised() {
        assertEquals("9876543210", mobileDigits("+91 98765 43210"))
        assertEquals("9876543210", mobileDigits("098765 43210"))
        assertEquals("98765", mobileDigits("98765"))
        assertTrue(SignupRules.isMobile("9876543210"))
        assertFalse(SignupRules.isMobile("1234567890"))
    }

    @Test fun basicsNeedAPasswordOnlyWithoutGoogle() {
        val base = SignupDraft(name = "Ravi Kumar", phone = "9876543210", email = "ravi@example.com")
        assertEquals(setOf("password"), SignupRules.check(Step.Basics, base).keys)
        assertTrue(SignupRules.check(Step.Basics, base.copy(viaGoogle = true)).isEmpty())
    }

    @Test fun bankDetailsMustMatchAndLookRight() {
        val d = SignupDraft(pan = "ABCDE1234F", accountName = "Ravi Kumar", accountNumber = "50100238148210", accountNumberAgain = "5010023814821", ifsc = "HDFC0001234")
        val keys = SignupRules.check(Step.Bank, d).keys
        assertTrue("account_again" in keys)
        assertTrue("pan_photo" in keys)
        assertFalse("ifsc" in keys)
        assertFalse(SignupRules.isIfsc("HDFC1234567"))
        assertFalse(SignupRules.isPan("ABCD1234FF"))
    }

    @Test fun emergencyContactCannotBeYourOwnNumber() {
        val d = SignupDraft(phone = "9876543210", emergencyName = "Savitha", emergencyRelation = Relation.Mother, emergencyPhone = "9876543210")
        assertEquals(R.string.check_emergency_self, SignupRules.check(Step.Emergency, d)["emergency_phone"])
    }

    @Test fun serverFieldErrorsLandOnTheirStep() {
        assertEquals(Step.Bank, SignupViewModel.stepOfField("bank_ifsc"))
        assertEquals("ifsc", SignupViewModel.localField("bank_ifsc"))
        assertEquals(Step.Licence, SignupViewModel.stepOfField("licence_back"))
        assertEquals(Step.Base, SignupViewModel.stepOfField("base_lat"))
    }

    @Test fun fixingPanDoesNotRequireReTypingTheBankAccount() {
        val d = SignupDraft(pan = "ABCDE1234F", accountName = "Ravi")
        val problems = RejectedViewModel.problemsFor("pan", d).keys
        assertEquals(setOf("pan_photo"), problems)
    }
}
