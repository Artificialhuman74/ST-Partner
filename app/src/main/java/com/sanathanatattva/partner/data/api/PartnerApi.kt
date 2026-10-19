package com.sanathanatattva.partner.data.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

/** The website backend (`backend/src/routes/auth.js`). Paths are relative to BuildConfig.BASE_URL. */
interface PartnerApi {
    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @POST("api/auth/partner/google")
    suspend fun partnerGoogle(@Body body: GoogleTokenRequest): PartnerGoogleResponse

    // Push notifications (backend/src/routes/devices.js)
    @POST("api/devices")
    suspend fun registerDevice(@Body body: DeviceRegistration): SuccessResponse

    @HTTP(method = "DELETE", path = "api/devices", hasBody = true)
    suspend fun unregisterDevice(@Body body: DeviceToken): SuccessResponse

    // A partner's own status (backend/src/routes/partner.js)
    @GET("api/partner/home")
    suspend fun partnerHome(): PartnerHome

    @PUT("api/partner/online")
    suspend fun setOnline(@Body body: OnlineBody): OnlineResponse

    @POST("api/partner/location")
    suspend fun sendLocation(@Body body: LocationBody): OkResponse

    @GET("api/partner/places")
    suspend fun places(): PlacesResponse

    @POST("api/partner/places")
    suspend fun addPlace(@Body body: PlaceBody): PlaceResponse

    @PUT("api/partner/places/{id}")
    suspend fun updatePlace(@Path("id") id: Long, @Body body: PlaceBody): PlaceResponse

    @POST("api/partner/places/{id}/default")
    suspend fun makeDefaultPlace(@Path("id") id: Long): OkResponse

    @DELETE("api/partner/places/{id}")
    suspend fun deletePlace(@Path("id") id: Long): OkResponse

    @POST("api/partner/test-alert")
    suspend fun testAlert(): TestAlertResponse

    @GET("api/partner/earnings")
    suspend fun earnings(@Query("days") days: Int = 7): Earnings

    @GET("api/partner/payout")
    suspend fun payout(): Payout

    @GET("api/partner/hours")
    suspend fun hours(): Hours

    @PUT("api/partner/hours")
    suspend fun saveHours(@Body body: HoursBody): Hours

    @PUT("api/partner/rider-jobs")
    suspend fun setRiderJobs(@Body body: RiderJobsBody): RiderJobsResponse

    @PUT("api/partner/profile")
    suspend fun saveProfile(@Body body: ProfileBody): ProfileResponse

    @GET("api/partner/orders/active")
    suspend fun activeWork(): ActiveWork

    @GET("api/partner/orders/offers/{id}")
    suspend fun offer(@Path("id") id: Long): OfferDetail

    @POST("api/partner/orders/offers/{id}/accept")
    suspend fun acceptOffer(@Path("id") id: Long): OfferAnswer

    @POST("api/partner/orders/offers/{id}/decline")
    suspend fun declineOffer(@Path("id") id: Long): OfferAnswer

    @GET("api/partner/orders/{id}")
    suspend fun order(@Path("id") id: Long): OrderCard

    @POST("api/partner/orders/{id}/packed")
    suspend fun packed(@Path("id") id: Long): OkResponse

    @POST("api/partner/orders/{id}/start")
    suspend fun startDelivery(@Path("id") id: Long): OkResponse

    @POST("api/partner/orders/{id}/at-shop")
    suspend fun atShop(@Path("id") id: Long): OkResponse

    @POST("api/partner/orders/{id}/handover")
    suspend fun handover(@Path("id") id: Long, @Body body: CodeBody): OkResponse

    @POST("api/partner/orders/{id}/check-code")
    suspend fun checkCode(@Path("id") id: Long, @Body body: OtpBody): OkResponse

    @POST("api/partner/orders/{id}/resend-code")
    suspend fun resendCode(@Path("id") id: Long): OkResponse

    @Multipart
    @POST("api/partner/orders/{id}/deliver")
    suspend fun deliver(@Path("id") id: Long, @Part("otp") otp: RequestBody, @Part photo: MultipartBody.Part): DeliveredResponse

    @POST("api/partner/orders/{id}/fail")
    suspend fun failDelivery(@Path("id") id: Long, @Body body: FailBody): OkResponse

    @POST("api/partner/orders/{id}/returned")
    suspend fun returned(@Path("id") id: Long, @Body body: CodeBody): OkResponse

    @Streaming
    @GET("api/partner/orders/{id}/rider-photo")
    suspend fun riderPhoto(@Path("id") id: Long): ResponseBody

    @GET("api/partner/orders/{id}/customer-phone")
    suspend fun customerPhone(@Path("id") id: Long): PhoneResponse

    @GET("api/delivery/container-pickups")
    suspend fun containerPickups(): PickupsResponse

    @Multipart
    @POST("api/delivery/container-pickups/{id}/resolve")
    suspend fun resolvePickup(
        @Path("id") id: Long,
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part photo: MultipartBody.Part?,
    ): ResolveResponse

    @GET("api/auth/me")
    suspend fun me(): MeResponse

    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body body: EmailRequest): SuccessResponse

    // Rider signup (backend/src/routes/rider.js). Fields go as text parts, photos as files.
    @Multipart
    @POST("api/rider/register")
    suspend fun registerRider(
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part photos: List<MultipartBody.Part>,
    ): AuthResponse

    @Multipart
    @POST("api/rider/register/google")
    suspend fun registerRiderWithGoogle(
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part photos: List<MultipartBody.Part>,
    ): AuthResponse

    @GET("api/rider/application")
    suspend fun riderApplication(): RiderApplication

    @Multipart
    @PUT("api/rider/application")
    suspend fun resubmitRiderApplication(
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part photos: List<MultipartBody.Part>,
    ): RiderApplication
}
