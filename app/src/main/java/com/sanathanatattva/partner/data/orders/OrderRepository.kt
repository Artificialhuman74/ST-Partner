package com.sanathanatattva.partner.data.orders

import com.sanathanatattva.partner.data.api.CodeBody
import com.sanathanatattva.partner.data.api.FailBody
import com.sanathanatattva.partner.data.api.OtpBody
import com.sanathanatattva.partner.data.api.PartnerApi
import com.sanathanatattva.partner.data.api.apiCall
import com.sanathanatattva.partner.data.signup.prepareForUpload
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

/** Offers and the delivery itself (/api/partner/orders). */
class OrderRepository(private val api: PartnerApi) {
    suspend fun active() = apiCall { api.activeWork() }
    suspend fun offer(id: Long) = apiCall { api.offer(id) }
    suspend fun accept(id: Long) = apiCall { api.acceptOffer(id) }
    suspend fun decline(id: Long) = apiCall { api.declineOffer(id) }
    suspend fun order(id: Long) = apiCall { api.order(id) }
    suspend fun packed(id: Long) = apiCall { api.packed(id) }
    suspend fun start(id: Long) = apiCall { api.startDelivery(id) }
    suspend fun atShop(id: Long) = apiCall { api.atShop(id) }
    suspend fun handover(id: Long, code: String) = apiCall { api.handover(id, CodeBody(code)) }
    suspend fun returned(id: Long, code: String) = apiCall { api.returned(id, CodeBody(code)) }
    suspend fun fail(id: Long, reason: String, note: String?) = apiCall { api.failDelivery(id, FailBody(reason, note)) }
    suspend fun customerPhone(id: Long) = apiCall { api.customerPhone(id) }
    suspend fun checkCode(id: Long, otp: String) = apiCall { api.checkCode(id, OtpBody(otp)) }
    suspend fun resendCode(id: Long) = apiCall { api.resendCode(id) }

    /** The customer's 6-digit code and the photo at the door, both required by the server. */
    suspend fun deliver(id: Long, otp: String, photo: File) = apiCall {
        val upload = prepareForUpload(photo)
        val type = (if (upload.extension == "webp") "image/webp" else "image/jpeg").toMediaType()
        api.deliver(
            id,
            otp.toRequestBody("text/plain".toMediaType()),
            MultipartBody.Part.createFormData("photo", "door.${upload.extension}", upload.asRequestBody(type)),
        )
    }
}
