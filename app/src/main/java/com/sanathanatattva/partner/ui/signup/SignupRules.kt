package com.sanathanatattva.partner.ui.signup

import androidx.annotation.StringRes
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.data.signup.KycPhoto
import com.sanathanatattva.partner.data.signup.SignupDraft

/** The signup steps in order. Licence is skipped for walking and cycle riders. */
enum class Step { Basics, Base, Selfie, Vehicle, Licence, Bank, Emergency, Review }

fun stepsFor(draft: SignupDraft): List<Step> =
    Step.entries.filter { it != Step.Licence || draft.vehicle == null || draft.motorised }

/** Same rules as the backend (riderApplication.js), checked before leaving each step. */
object SignupRules {
    private val email = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    private val pan = Regex("^[A-Z]{5}\\d{4}[A-Z]$")
    private val ifsc = Regex("^[A-Z]{4}0[A-Z0-9]{6}$")
    private val mobile = Regex("^[6-9]\\d{9}$")

    fun isMobile(digits: String) = mobile.matches(digits)
    fun isPan(value: String) = pan.matches(value)
    fun isIfsc(value: String) = ifsc.matches(value)

    /** Field key → message; empty when the step can be left. */
    fun check(step: Step, d: SignupDraft): Map<String, Int> {
        val e = linkedMapOf<String, Int>()
        fun need(ok: Boolean, field: String, @StringRes message: Int) { if (!ok) e[field] = message }
        when (step) {
            Step.Basics -> {
                need(d.name.trim().length >= 2, "name", R.string.check_name)
                need(isMobile(d.phone), "phone", R.string.check_phone)
                if (!d.viaGoogle) {
                    need(email.matches(d.email.trim()), "email", R.string.login_error_email)
                    need(d.password.length >= 8, "password", R.string.check_password)
                }
            }
            Step.Base -> need(d.baseLat != null && d.baseLng != null && d.baseAddress.isNotBlank(), "base", R.string.check_base)
            Step.Selfie -> need(d.photo(KycPhoto.Selfie) != null, "selfie", R.string.check_selfie)
            Step.Vehicle -> {
                need(d.vehicle != null, "vehicle", R.string.check_vehicle)
                if (d.motorised) need(d.plate.replace(" ", "").length >= 6, "plate", R.string.check_plate)
            }
            Step.Licence -> if (d.motorised) {
                need(d.photo(KycPhoto.LicenceFront) != null && d.photo(KycPhoto.LicenceBack) != null, "licence_photos", R.string.check_licence_photos)
                need(d.licenceNumber.replace(" ", "").length >= 8, "licence_number", R.string.check_licence_number)
            }
            Step.Bank -> {
                need(d.photo(KycPhoto.Pan) != null, "pan_photo", R.string.check_pan_photo)
                need(isPan(d.pan), "pan", R.string.check_pan)
                need(d.accountName.trim().length >= 2, "account_name", R.string.check_account_name)
                need(d.accountNumber.length in 9..18, "account_number", R.string.check_account_number)
                need(d.accountNumber == d.accountNumberAgain, "account_again", R.string.check_account_match)
                need(isIfsc(d.ifsc), "ifsc", R.string.check_ifsc)
            }
            Step.Emergency -> {
                need(d.emergencyName.trim().length >= 2, "emergency_name", R.string.check_emergency_name)
                need(d.emergencyRelation != null, "relation", R.string.check_relation)
                need(isMobile(d.emergencyPhone), "emergency_phone", R.string.check_emergency_phone)
                if (isMobile(d.emergencyPhone)) need(d.emergencyPhone != d.phone, "emergency_phone", R.string.check_emergency_self)
            }
            Step.Review -> Unit
        }
        return e
    }

    /** First step that still has a problem; where "Send" takes the rider if something slipped. */
    fun firstIncomplete(d: SignupDraft): Step? = stepsFor(d).firstOrNull { it != Step.Review && check(it, d).isNotEmpty() }
}

/** Keeps only digits; strips a leading +91 / 0 so "+91 98765 43210" becomes 9876543210. */
fun mobileDigits(raw: String): String {
    val d = raw.filter(Char::isDigit)
    return when {
        d.length > 10 && d.startsWith("91") -> d.drop(2).take(10)
        d.length > 10 && d.startsWith("0") -> d.drop(1).take(10)
        else -> d.take(10)
    }
}

/** "98765 43210" for display. */
fun prettyMobile(digits: String): String = if (digits.length > 5) digits.take(5) + " " + digits.drop(5) else digits
