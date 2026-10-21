package com.sanathanatattva.partner.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// A trader's own shop, from the website's trader routes (backend/src/routes/trader.js). The app's
// login token is the website's, so a shop partner sees the same numbers as on the website.

@Serializable
data class InventoryResponse(val inventory: List<InventoryItem> = emptyList())

@Serializable
data class InventoryItem(
    @SerialName("product_id") val productId: Long,
    val quantity: Int = 0,
    @SerialName("low_stock_threshold") val alertAt: Int = 0,
    @SerialName("product_name") val name: String = "",
    val price: Double = 0.0,
    val unit: String? = null,
    @SerialName("container_type") val containerType: String? = null,
)

@Serializable
data class ThresholdBody(val threshold: Int)

@Serializable
data class RestockOrdersResponse(val orders: List<RestockOrder> = emptyList())

/** A trader's order for stock from the admin's warehouse ("My Orders" on the website). */
@Serializable
data class RestockOrder(
    val id: Long,
    @SerialName("order_number") val orderNumber: String = "",
    val status: String = "pending",
    @SerialName("total_amount") val total: Double = 0.0,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class RestockOrderDetail(val order: RestockOrder, val items: List<RestockItem> = emptyList())

@Serializable
data class RestockItem(
    @SerialName("product_id") val productId: Long = 0,
    val quantity: Int = 0,
    val price: Double = 0.0,
    @SerialName("product_name") val name: String = "",
)

@Serializable
data class RestockBody(val items: List<RestockLine>, val notes: String? = null)

@Serializable
data class RestockLine(@SerialName("product_id") val productId: Long, val quantity: Int)

@Serializable
data class RestockCreated(val order: RestockOrder)

@Serializable
data class CustomerOrdersResponse(val orders: List<CustomerOrder> = emptyList())

@Serializable
data class CustomerOrder(
    val id: Long,
    @SerialName("order_number") val orderNumber: String = "",
    val status: String = "",
    @SerialName("delivery_status") val deliveryStatus: String? = null,
    @SerialName("total_amount") val total: Double = 0.0,
    @SerialName("consumer_name") val customerName: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("delivery_dealer_id") val deliveryDealerId: Long? = null,
    @SerialName("rider_id") val riderId: Long? = null,
    val items: List<CustomerOrderItem> = emptyList(),
)

@Serializable
data class CustomerOrderItem(
    val quantity: Int = 0,
    @SerialName("product_name") val name: String = "",
    @SerialName("container_type") val containerType: String? = null,
)

@Serializable
data class CommissionsResponse(
    val commissions: List<Commission> = emptyList(),
    val summary: CommissionSummary = CommissionSummary(),
    /** The weekday automatic payouts run on (0 = Sunday); null when the admin pays by hand. */
    val payoutDay: Int? = null,
)

@Serializable
data class Commission(
    val id: Long,
    val amount: Double = 0.0,
    val status: String = "pending",
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class CommissionSummary(
    @SerialName("total_amount") val total: Double = 0.0,
    @SerialName("paid_amount") val paid: Double = 0.0,
    @SerialName("pending_amount") val pending: Double = 0.0,
)

@Serializable
data class SubDealersResponse(val subDealers: List<SubDealer> = emptyList())

@Serializable
data class SubDealer(
    val id: Long,
    val name: String = "",
    val phone: String? = null,
    val address: String? = null,
    val status: String = "active",
)

@Serializable
data class ReferralResponse(val referralCode: String? = null, val tier: Int? = null)

@Serializable
data class AdminContact(val name: String? = null, val phone: String? = null)

@Serializable
data class EnrollmentsResponse(val enrollments: List<Enrollment> = emptyList())

/** One customer in the trader's lucky draw: "entered" this month, "not_entered", or "winning". */
@Serializable
data class Enrollment(val id: Long = 0, val standing: String? = null, @SerialName("consumer_name") val customerName: String? = null)

@Serializable
data class TraderProfileResponse(val user: TraderProfile)

@Serializable
data class TraderProfile(val id: Long, val name: String = "", val address: String? = null, val tier: Int? = null)

@Serializable
data class CatalogResponse(val products: List<CatalogProduct> = emptyList())

@Serializable
data class CatalogProduct(
    val id: Long,
    val name: String = "",
    val price: Double = 0.0,
    val unit: String? = null,
    @SerialName("container_type") val containerType: String? = null,
)
