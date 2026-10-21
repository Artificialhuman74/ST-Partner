package com.sanathanatattva.partner.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A row of the website's `users` table as the `/api/auth` routes return it (password stripped). */
@Serializable
data class User(
    val id: Long,
    val name: String,
    val email: String,
    val role: String,
    val tier: Int? = null,
    @SerialName("referral_code") val referralCode: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val pincode: String? = null,
    @SerialName("will_deliver") val willDeliver: Int = 0,
    @SerialName("delivery_enabled") val deliveryEnabled: Int = 0,
    val status: String = "active",
    // Riders only, once the backend adds review (handoff §6.2).
    @SerialName("review_status") val reviewStatus: String? = null,
    @SerialName("vehicle_plate") val vehiclePlate: String? = null,
) {
    val firstName: String get() = name.trim().substringBefore(' ').ifEmpty { name }
    val isRider: Boolean get() = role == "rider"
    /** Outside riders can't take orders until an admin approves them (pending or sent back). */
    val isPendingReview: Boolean get() = isRider && reviewStatus != "approved"
}

/** GET/PUT /api/rider/application: where a rider's signup stands. */
@Serializable
data class RiderApplication(
    @SerialName("review_status") val reviewStatus: String? = null,
    @SerialName("review_issues") val reviewIssues: List<ReviewIssue> = emptyList(),
    @SerialName("submitted_at") val submittedAt: String? = null,
    @SerialName("reviewed_at") val reviewedAt: String? = null,
    val application: RiderDetails? = null,
)

@Serializable
data class ReviewIssue(val item: String, val reason: String)

@Serializable
data class RiderBase(val label: String = "home", val address: String = "", val latitude: Double = 0.0, val longitude: Double = 0.0)

@Serializable
data class RiderDetails(
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val base: RiderBase? = null,
    @SerialName("vehicle_type") val vehicleType: String? = null,
    @SerialName("vehicle_plate") val vehiclePlate: String? = null,
    @SerialName("licence_number") val licenceNumber: String? = null,
    val pan: String? = null,
    @SerialName("bank_account_name") val bankAccountName: String? = null,
    @SerialName("bank_account_last4") val bankAccountLast4: String? = null,
    @SerialName("bank_ifsc") val bankIfsc: String? = null,
    @SerialName("emergency_name") val emergencyName: String? = null,
    @SerialName("emergency_relation") val emergencyRelation: String? = null,
    @SerialName("emergency_phone") val emergencyPhone: String? = null,
)

/** GET /api/partner/home */
@Serializable
data class PartnerHome(
    val online: Boolean = false,
    @SerialName("online_since") val onlineSince: String? = null,
    @SerialName("started_from") val startedFrom: StartedFrom? = null,
    val places: List<Place> = emptyList(),
    val live: LivePoint? = null,
    val today: Today = Today(),
    @SerialName("missed_offer_streak") val missedOfferStreak: Int = 0,
    @SerialName("auto_offline_at") val autoOfflineAt: String? = null,
    @SerialName("takes_rider_jobs") val takesRiderJobs: Boolean = false,
    @SerialName("rider_fee") val riderFee: Double = 45.0,
)

/** GET /api/partner/earnings — [EarningDay.date] is a local date, yyyy-MM-dd. */
@Serializable
data class Earnings(
    val from: String = "",
    val to: String = "",
    val days: List<EarningDay> = emptyList(),
    val total: Double = 0.0,
    val deliveries: Int = 0,
    val paid: Double = 0.0,
    val pending: Double = 0.0,
    val bank: BankTail? = null,
)

@Serializable
data class EarningDay(val date: String, val deliveries: Int = 0, val amount: Double = 0.0)

@Serializable
data class BankTail(val ifsc: String? = null, val last4: String? = null)

/** GET /api/partner/payout. [step]: 0 none … 4 payouts active. */
@Serializable
data class Payout(
    val holder: String? = null,
    val ifsc: String? = null,
    val last4: String? = null,
    @SerialName("pan_masked") val panMasked: String? = null,
    @SerialName("pan_verified") val panVerified: Boolean = false,
    @SerialName("account_status") val accountStatus: String? = null,
    val step: Int = 0,
    @SerialName("needs_clarification") val needsClarification: Boolean = false,
    val recent: List<PayoutRow> = emptyList(),
)

@Serializable
data class PayoutRow(val date: String, val amount: Double = 0.0, val status: String = "on_the_way")

/** Delivery hours: minutes from local midnight; index 0 = Monday. */
@Serializable
data class DayHours(val weekday: Int = 0, val enabled: Boolean = true, val start: Int = 480, val end: Int = 1200)

@Serializable
data class Hours(val days: List<DayHours> = emptyList(), val saved: Boolean = false)

@Serializable
data class HoursBody(val days: List<DayHours>)

@Serializable
data class RiderJobsBody(val on: Boolean)

@Serializable
data class RiderJobsResponse(@SerialName("takes_rider_jobs") val takesRiderJobs: Boolean = false)

@Serializable
data class ProfileBody(val name: String, val phone: String)

@Serializable
data class ProfileResponse(val name: String = "", val phone: String = "")

/** GET /api/delivery/container-pickups (the website's existing endpoint). */
@Serializable
data class PickupsResponse(val pickups: List<Pickup> = emptyList())

@Serializable
data class Pickup(
    val id: Long,
    @SerialName("container_type") val containerType: String = "",
    @SerialName("deposit_amount") val deposit: Double = 0.0,
    /** store_credit | manual_upi | manual_bank — what the customer asked for. */
    @SerialName("refund_destination") val destination: String? = null,
    @SerialName("consumer_name") val customerName: String = "",
    @SerialName("consumer_address") val customerAddress: String? = null,
    @SerialName("current_product_name") val productName: String? = null,
)

@Serializable
data class ResolveResponse(val photoUrl: String? = null)

/** An order as the ring and order screens show it (backend dispatch.orderCard + order detail). */
@Serializable
data class OrderCard(
    @SerialName("order_id") val orderId: Long = 0,
    @SerialName("order_number") val orderNumber: String = "",
    val lines: List<OrderLine> = emptyList(),
    val load: Load = Load(),
    val total: Double = 0.0,
    @SerialName("paid_online") val paidOnline: Boolean = true,
    val customer: Customer = Customer(),
    val shop: Shop? = null,
    val earn: Double = 0.0,
    @SerialName("distance_km") val distanceKm: Double? = null,
    @SerialName("to_shop_km") val toShopKm: Double? = null,
    @SerialName("shop_to_customer_km") val shopToCustomerKm: Double? = null,
    // Order screens only:
    val role: String? = null,
    val stage: String? = null,
    @SerialName("delivery_status") val deliveryStatus: String? = null,
    val packed: Boolean = false,
    val attempt: Int = 0,
    @SerialName("fail_reason") val failReason: String? = null,
    val rider: RiderInfo? = null,
    @SerialName("pickup_code") val pickupCode: String? = null,
    @SerialName("return_code") val returnCode: String? = null,
    @SerialName("share_amount") val shareAmount: Double? = null,
)

@Serializable
data class OrderLine(
    val name: String = "",
    val quantity: Int = 1,
    val size: String? = null,
    val litres: Double? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    /** refill | new | standard */
    @SerialName("line_type") val lineType: String = "standard",
)

@Serializable
data class Load(val cans: Int = 0, val litres: Double = 0.0)

@Serializable
data class Customer(
    @SerialName("first_name") val firstName: String = "",
    val area: String = "",
    val address: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    @SerialName("can_call") val canCall: Boolean = false,
)

@Serializable
data class Shop(
    val name: String = "",
    val area: String = "",
    val address: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val phone: String? = null,
)

@Serializable
data class RiderInfo(
    val id: Long = 0,
    val name: String = "",
    @SerialName("first_name") val firstName: String = "",
    val phone: String? = null,
    @SerialName("vehicle_type") val vehicleType: String? = null,
    @SerialName("vehicle_plate") val vehiclePlate: String? = null,
    val live: LivePoint? = null,
    @SerialName("has_photo") val hasPhoto: Boolean = false,
)

@Serializable
data class OfferDetail(
    @SerialName("offer_id") val offerId: Long,
    val kind: String,
    @SerialName("expires_at") val expiresAt: String,
    val open: Boolean = true,
    val response: String? = null,
    val card: OrderCard = OrderCard(),
    val summary: String = "",
)

@Serializable
data class OfferAnswer(val ok: Boolean = true, @SerialName("order_id") val orderId: Long? = null)

@Serializable
data class ActiveWork(val orders: List<OrderCard> = emptyList(), val offers: List<ActiveOffer> = emptyList())

@Serializable
data class ActiveOffer(@SerialName("offer_id") val offerId: Long, val kind: String, @SerialName("expires_at") val expiresAt: String, val card: OrderCard = OrderCard())

@Serializable
data class CodeBody(val code: String)

@Serializable
data class OtpBody(val otp: String)

@Serializable
data class FailBody(val reason: String, val note: String? = null)

@Serializable
data class PhoneResponse(val phone: String)

@Serializable
data class DeliveredResponse(val ok: Boolean = true, val earned: Double = 0.0)

@Serializable
data class LivePoint(val lat: Double, val lng: Double, val at: String? = null)

@Serializable
data class StartedFrom(val kind: String, val place: Place? = null)

@Serializable
data class Today(val deliveries: Int = 0, val earned: Double = 0.0)

@Serializable
data class Place(
    val id: Long,
    val label: String = "home",
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    @SerialName("is_default") val isDefault: Boolean = false,
)

@Serializable
data class PlacesResponse(val places: List<Place> = emptyList())

@Serializable
data class PlaceResponse(val place: Place)

@Serializable
data class PlaceBody(val label: String, val address: String, val lat: Double, val lng: Double, @SerialName("is_default") val isDefault: Boolean = false)

@Serializable
data class OnlineBody(
    val online: Boolean,
    @SerialName("place_id") val placeId: Long? = null,
    val lat: Double? = null,
    val lng: Double? = null,
)

@Serializable
data class OnlineResponse(val online: Boolean = false, @SerialName("place_id") val placeId: Long? = null)

@Serializable
data class LocationBody(val lat: Double, val lng: Double, val accuracy: Float? = null)

@Serializable
data class OkResponse(val ok: Boolean = true)

@Serializable
data class TestAlertResponse(val sent: Int = 0, val failed: Int = 0)

@Serializable
data class DeviceRegistration(
    val token: String,
    val platform: String = "android",
    @SerialName("app_version") val appVersion: String? = null,
)

@Serializable
data class DeviceToken(val token: String)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class EmailRequest(val email: String)

@Serializable
data class AuthResponse(val token: String, val user: User)

@Serializable
data class MeResponse(val user: User)

@Serializable
data class GoogleTokenRequest(@SerialName("id_token") val idToken: String)

/** `exists: false` means this Google account has no partner account yet (email/name prefilled for signup). */
@Serializable
data class PartnerGoogleResponse(
    val exists: Boolean = false,
    val token: String? = null,
    val user: User? = null,
    val email: String? = null,
    val name: String? = null,
)

@Serializable
data class SuccessResponse(val success: Boolean = false)

/** Every error the website returns looks like `{ "error": "..." }`; form errors add `fields`. */
@Serializable
data class ErrorBody(val error: String? = null, val fields: Map<String, String> = emptyMap())

/** GET api/public/app-version: builds below min_version_code must update before they open. */
@Serializable
data class AppVersionRule(val min_version_code: Int = 0)

@Serializable
data class AppVersions(val delivery: AppVersionRule = AppVersionRule())
