package com.sanathanatattva.partner

import com.sanathanatattva.partner.data.api.AuthResponse
import com.sanathanatattva.partner.data.api.apiJson
import com.sanathanatattva.partner.data.api.parseErrorMessage
import com.sanathanatattva.partner.ui.components.kf
import com.sanathanatattva.partner.ui.login.isPlausibleEmail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiParsingTest {
    // Shape of POST /api/auth/login from the website backend (users row, password removed).
    private val loginBody = """
        {"token":"abc.def","user":{"id":12,"name":"Ravi Kumar","email":"ravi@example.com","role":"trader",
        "tier":2,"referral_code":"A0003","referred_by_id":1,"phone":"9800000001","address":"Jayanagar",
        "pincode":"560041","will_deliver":1,"delivery_enabled":1,"commission_rate":10.0,"status":"active",
        "google_uid":null,"created_at":"2026-01-02 10:00:00","availability_status":"available"}}
    """.trimIndent()

    @Test fun parsesLoginResponseAndIgnoresExtraColumns() {
        val response = apiJson.decodeFromString(AuthResponse.serializer(), loginBody)
        assertEquals("abc.def", response.token)
        assertEquals("Ravi", response.user.firstName)
        assertEquals("A0003", response.user.referralCode)
        assertEquals(1, response.user.willDeliver)
        assertFalse(response.user.isPendingReview)
    }

    @Test fun readsServerErrorText() {
        assertEquals("Invalid email or password", parseErrorMessage("""{"error":"Invalid email or password"}"""))
        assertNull(parseErrorMessage("<html>Bad gateway</html>"))
        assertNull(parseErrorMessage(""))
    }

    @Test fun emailCheckIsForgivingButCatchesTypos() {
        assertTrue(isPlausibleEmail(" ravi.kumar@example.com "))
        assertFalse(isPlausibleEmail("ravi.kumar@example"))
        assertFalse(isPlausibleEmail("ravi kumar@example.com"))
    }

    @Test fun keyframesHoldOutsideAndInterpolateInside() {
        assertEquals(0f, kf(-1f, 0f to 0f, 1f to 10f), 0f)
        assertEquals(5f, kf(0.5f, 0f to 0f, 1f to 10f), 0.001f)
        assertEquals(10f, kf(2f, 0f to 0f, 1f to 10f), 0f)
        assertEquals(-4f, kf(0.05f, 0f to 0f, 0.05f to -4f, 0.10f to 4f), 0.001f)
    }
}
