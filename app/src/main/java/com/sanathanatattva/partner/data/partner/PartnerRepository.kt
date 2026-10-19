package com.sanathanatattva.partner.data.partner

import com.sanathanatattva.partner.data.api.ApiResult
import com.sanathanatattva.partner.data.api.DayHours
import com.sanathanatattva.partner.data.api.Earnings
import com.sanathanatattva.partner.data.api.HoursBody
import com.sanathanatattva.partner.data.api.LocationBody
import com.sanathanatattva.partner.data.api.OnlineBody
import com.sanathanatattva.partner.data.api.PartnerApi
import com.sanathanatattva.partner.data.api.PartnerHome
import com.sanathanatattva.partner.data.api.Payout
import com.sanathanatattva.partner.data.api.Place
import com.sanathanatattva.partner.data.api.PlaceBody
import com.sanathanatattva.partner.data.api.ProfileBody
import com.sanathanatattva.partner.data.api.RiderJobsBody
import com.sanathanatattva.partner.data.api.apiCall
import com.sanathanatattva.partner.data.signup.prepareForUpload
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

/** Where the partner goes online from: a saved place, or wherever the phone is now. */
sealed interface StartPoint {
    data class Saved(val place: Place) : StartPoint
    data class Here(val lat: Double, val lng: Double) : StartPoint
}

class PartnerRepository(private val api: PartnerApi) {
    // Last answers, so tabs open fully drawn and refresh in place instead of flashing empty.
    @Volatile var lastHome: PartnerHome? = null
        private set
    @Volatile var lastPlaces: List<Place>? = null
        private set
    @Volatile var lastEarnings: Earnings? = null
        private set
    @Volatile var lastPayout: Payout? = null
        private set

    /** Set by "Go online" on the alert checklist; Home opens the start-place picker and clears it. */
    @Volatile var wantsToGoOnline = false

    fun clearCache() {
        lastHome = null
        lastPlaces = null
        lastEarnings = null
        lastPayout = null
    }

    suspend fun home(): ApiResult<PartnerHome> = apiCall { api.partnerHome() }.also { r ->
        if (r is ApiResult.Success) { lastHome = r.value; lastPlaces = r.value.places }
    }

    suspend fun goOnline(from: StartPoint, lat: Double? = null, lng: Double? = null) = apiCall {
        when (from) {
            is StartPoint.Saved -> api.setOnline(OnlineBody(true, placeId = from.place.id, lat = lat, lng = lng))
            is StartPoint.Here -> api.setOnline(OnlineBody(true, lat = from.lat, lng = from.lng))
        }
    }

    suspend fun goOffline() = apiCall { api.setOnline(OnlineBody(false)) }

    suspend fun sendLocation(lat: Double, lng: Double, accuracy: Float?) = apiCall { api.sendLocation(LocationBody(lat, lng, accuracy)) }

    suspend fun places(): ApiResult<List<Place>> = when (val r = apiCall { api.places() }) {
        is ApiResult.Success -> ApiResult.Success(r.value.places).also { lastPlaces = it.value }
        is ApiResult.Failure -> r
    }

    suspend fun addPlace(body: PlaceBody) = apiCall { api.addPlace(body) }
    suspend fun updatePlace(id: Long, body: PlaceBody) = apiCall { api.updatePlace(id, body) }
    suspend fun makeDefault(id: Long) = apiCall { api.makeDefaultPlace(id) }
    suspend fun deletePlace(id: Long) = apiCall { api.deletePlace(id) }
    suspend fun testAlert() = apiCall { api.testAlert() }

    suspend fun earnings(days: Int = 7) = apiCall { api.earnings(days) }.also { r -> if (r is ApiResult.Success) lastEarnings = r.value }
    suspend fun payout() = apiCall { api.payout() }.also { r -> if (r is ApiResult.Success) lastPayout = r.value }
    suspend fun hours() = apiCall { api.hours() }
    suspend fun saveHours(days: List<DayHours>) = apiCall { api.saveHours(HoursBody(days)) }
    suspend fun setRiderJobs(on: Boolean) = apiCall { api.setRiderJobs(RiderJobsBody(on)) }
    suspend fun saveProfile(name: String, phone: String) = apiCall { api.saveProfile(ProfileBody(name, phone)) }

    suspend fun containerPickups() = apiCall { api.containerPickups() }

    /** Closes an empty-can pickup: [outcome] refunded | forfeited; [photo] is the UPI screenshot or the damage photo. */
    suspend fun resolvePickup(id: Long, outcome: String, destination: String?, notes: String?, photo: File?) = apiCall {
        val text = "text/plain".toMediaType()
        val fields = buildMap {
            put("outcome", outcome.toRequestBody(text))
            destination?.let { put("destination", it.toRequestBody(text)) }
            notes?.takeIf { it.isNotBlank() }?.let { put("notes", it.trim().toRequestBody(text)) }
        }
        val part = photo?.let {
            val upload = prepareForUpload(it)
            val type = (if (upload.extension == "webp") "image/webp" else "image/jpeg").toMediaType()
            MultipartBody.Part.createFormData("photo", "photo.${upload.extension}", upload.asRequestBody(type))
        }
        api.resolvePickup(id, fields, part)
    }
}
