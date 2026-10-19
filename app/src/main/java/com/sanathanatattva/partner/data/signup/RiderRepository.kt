package com.sanathanatattva.partner.data.signup

import com.sanathanatattva.partner.data.GoogleSignIn
import com.sanathanatattva.partner.data.SessionStore
import com.sanathanatattva.partner.data.api.ApiProblem
import com.sanathanatattva.partner.data.api.ApiResult
import com.sanathanatattva.partner.data.api.PartnerApi
import com.sanathanatattva.partner.data.api.RiderApplication
import com.sanathanatattva.partner.data.api.User
import com.sanathanatattva.partner.data.api.apiCall
import com.sanathanatattva.partner.push.PushRegistrar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/** What an admin can flag (backend riderApplication.REVIEW_ITEMS). */
object ReviewItem {
    const val SELFIE = "selfie"
    const val LICENCE_FRONT = "licence_front"
    const val LICENCE_BACK = "licence_back"
    const val PAN = "pan"
    const val BANK = "bank"
    const val VEHICLE = "vehicle"
    const val EMERGENCY = "emergency"
    const val BASE = "base"
}

class RiderRepository(
    private val api: PartnerApi,
    private val sessions: SessionStore,
    private val google: GoogleSignIn,
    val drafts: SignupDraftStore,
    private val push: PushRegistrar,
) {
    private val text = "text/plain".toMediaType()
    private val jpeg = "image/jpeg".toMediaType()
    private val webp = "image/webp".toMediaType()

    /** Sends a complete application. On success the rider is signed in (pending review). */
    suspend fun submit(draft: SignupDraft): ApiResult<User> {
        val fields = linkedMapOf(
            "name" to draft.name.trim(),
            "phone" to draft.phone,
            "base_label" to draft.baseLabel.wire,
            "base_address" to draft.baseAddress.trim(),
            "base_lat" to draft.baseLat?.toString().orEmpty(),
            "base_lng" to draft.baseLng?.toString().orEmpty(),
            "vehicle_type" to draft.vehicle?.wire.orEmpty(),
            "pan" to draft.pan,
            "bank_account_name" to draft.accountName.trim(),
            "bank_account_number" to draft.accountNumber,
            "bank_ifsc" to draft.ifsc,
            "emergency_name" to draft.emergencyName.trim(),
            "emergency_relation" to draft.emergencyRelation?.wire.orEmpty(),
            "emergency_phone" to draft.emergencyPhone,
        )
        if (draft.motorised) {
            fields["vehicle_plate"] = draft.plate.trim()
            fields["licence_number"] = draft.licenceNumber.trim()
        }
        val kinds = buildList {
            add(KycPhoto.Selfie)
            add(KycPhoto.Pan)
            if (draft.motorised) { add(KycPhoto.LicenceFront); add(KycPhoto.LicenceBack) }
        }
        val photos = photoParts(draft, kinds)

        val result = if (draft.viaGoogle) {
            val idToken = google.currentIdToken()
                ?: return ApiResult.Failure(ApiProblem.Server(401, null))
            fields["id_token"] = idToken
            apiCall { api.registerRiderWithGoogle(fields.toParts(), photos) }
        } else {
            fields["email"] = draft.email.trim()
            fields["password"] = draft.password
            apiCall { api.registerRider(fields.toParts(), photos) }
        }
        return when (result) {
            is ApiResult.Success -> {
                sessions.save(result.value.token, result.value.user)
                drafts.clear()
                push.register()
                ApiResult.Success(result.value.user)
            }
            is ApiResult.Failure -> result
        }
    }

    suspend fun application(): ApiResult<RiderApplication> = apiCall { api.riderApplication() }

    /** Sends only what the admin flagged (plus the fields those fixes depend on). */
    suspend fun resubmit(draft: SignupDraft, flagged: Set<String>): ApiResult<RiderApplication> {
        val fields = linkedMapOf<String, String>()
        val kinds = mutableListOf<KycPhoto>()
        if (ReviewItem.SELFIE in flagged) kinds += KycPhoto.Selfie
        if (ReviewItem.LICENCE_FRONT in flagged) kinds += KycPhoto.LicenceFront
        if (ReviewItem.LICENCE_BACK in flagged) kinds += KycPhoto.LicenceBack
        if (ReviewItem.PAN in flagged) {
            fields["pan"] = draft.pan
            kinds += KycPhoto.Pan
        }
        if (ReviewItem.BANK in flagged) {
            fields["bank_account_name"] = draft.accountName.trim()
            fields["bank_account_number"] = draft.accountNumber
            fields["bank_ifsc"] = draft.ifsc
        }
        if (ReviewItem.VEHICLE in flagged) {
            fields["vehicle_type"] = draft.vehicle?.wire.orEmpty()
            if (draft.motorised) {
                fields["vehicle_plate"] = draft.plate.trim()
                fields["licence_number"] = draft.licenceNumber.trim()
                draft.photo(KycPhoto.LicenceFront)?.let { if (KycPhoto.LicenceFront !in kinds) kinds += KycPhoto.LicenceFront }
                draft.photo(KycPhoto.LicenceBack)?.let { if (KycPhoto.LicenceBack !in kinds) kinds += KycPhoto.LicenceBack }
            }
        }
        if (ReviewItem.LICENCE_FRONT in flagged || ReviewItem.LICENCE_BACK in flagged) {
            if (draft.licenceNumber.isNotBlank()) fields["licence_number"] = draft.licenceNumber.trim()
        }
        if (ReviewItem.EMERGENCY in flagged) {
            fields["emergency_name"] = draft.emergencyName.trim()
            fields["emergency_relation"] = draft.emergencyRelation?.wire.orEmpty()
            fields["emergency_phone"] = draft.emergencyPhone
        }
        if (ReviewItem.BASE in flagged) {
            fields["base_label"] = draft.baseLabel.wire
            fields["base_address"] = draft.baseAddress.trim()
            fields["base_lat"] = draft.baseLat?.toString().orEmpty()
            fields["base_lng"] = draft.baseLng?.toString().orEmpty()
        }
        val photos = photoParts(draft, kinds)
        return apiCall { api.resubmitRiderApplication(fields.toParts(), photos) }
    }

    private fun Map<String, String>.toParts(): Map<String, RequestBody> = mapValues { it.value.toRequestBody(text) }

    private suspend fun photoParts(draft: SignupDraft, kinds: List<KycPhoto>): List<MultipartBody.Part> =
        kinds.mapNotNull { kind ->
            val file = draft.photo(kind) ?: return@mapNotNull null
            val upload = prepareForUpload(file)
            val type = if (upload.extension == "webp") webp else jpeg
            MultipartBody.Part.createFormData(kind.field, "${kind.field}.${upload.extension}", upload.asRequestBody(type))
        }
}

/** "HDFC Bank · Jayanagar, Bengaluru" from an IFSC, via Razorpay's free public IFSC lookup. */
class BankLookup {
    private val client = OkHttpClient.Builder().callTimeout(8, TimeUnit.SECONDS).build()
    private val json = Json { ignoreUnknownKeys = true }

    private val known = java.util.concurrent.ConcurrentHashMap<String, String>()

    /** Already looked up this session (no network). */
    fun cached(ifsc: String): String? = known[ifsc]

    suspend fun describe(ifsc: String): String? = known[ifsc] ?: withContext(Dispatchers.IO) {
        runCatching {
            client.newCall(Request.Builder().url("https://ifsc.razorpay.com/$ifsc").build()).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val obj = json.parseToJsonElement(response.body?.string() ?: return@use null).jsonObject
                val bank = obj["BANK"]?.jsonPrimitive?.content ?: return@use null
                val branch = obj["BRANCH"]?.jsonPrimitive?.content?.titleCase()
                val city = obj["CITY"]?.jsonPrimitive?.content?.titleCase()
                listOfNotNull(bank, listOfNotNull(branch, city).joinToString(", ").ifBlank { null }).joinToString(" · ")
            }
        }.getOrNull()?.also { known[ifsc] = it }
    }

    private fun String.titleCase() = lowercase().split(' ').joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
}
