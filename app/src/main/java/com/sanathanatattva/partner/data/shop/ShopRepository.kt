package com.sanathanatattva.partner.data.shop

import com.sanathanatattva.partner.data.api.AdminContact
import com.sanathanatattva.partner.data.api.ApiResult
import com.sanathanatattva.partner.data.api.CatalogProduct
import com.sanathanatattva.partner.data.api.CommissionsResponse
import com.sanathanatattva.partner.data.api.CustomerOrder
import com.sanathanatattva.partner.data.api.Enrollment
import com.sanathanatattva.partner.data.api.InventoryItem
import com.sanathanatattva.partner.data.api.PartnerApi
import com.sanathanatattva.partner.data.api.RestockBody
import com.sanathanatattva.partner.data.api.RestockCreated
import com.sanathanatattva.partner.data.api.RestockLine
import com.sanathanatattva.partner.data.api.RestockOrder
import com.sanathanatattva.partner.data.api.SubDealer
import com.sanathanatattva.partner.data.api.SuccessResponse
import com.sanathanatattva.partner.data.api.ThresholdBody
import com.sanathanatattva.partner.data.api.TraderProfile
import com.sanathanatattva.partner.data.api.apiCall
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** A restock order with its lines (the list endpoint has no lines; the first few are fetched). */
data class Restock(val order: RestockOrder, val productCount: Int?)

/** What a Tier 1 trader sees of one sub-dealer: their card, and their shelf once loaded. */
data class SubShop(val dealer: SubDealer, val inventory: List<InventoryItem>? = null, val failed: Boolean = false)

/** Everything My Shop shows. Fields stay null until their first load; a failed refresh keeps the last copy. */
data class ShopState(
    val profile: TraderProfile? = null,
    val inventory: List<InventoryItem>? = null,
    val restocks: List<Restock>? = null,
    val customers: List<CustomerOrder>? = null,
    val money: CommissionsResponse? = null,
    val subShops: List<SubShop>? = null,
    val referralCode: String? = null,
    val luckyDraw: List<Enrollment>? = null,
    val admin: AdminContact? = null,
    /** When everything last loaded, for "These numbers are from 10:42 AM". */
    val loadedAt: Long? = null,
    /** The last refresh failed for want of a connection. */
    val offline: Boolean = false,
) {
    val attention: List<InventoryItem> get() = inventory.orEmpty().filter { it.needsAttention }.sortedBy { it.quantity }
    val onTheWay: Int get() = restocks.orEmpty().count { it.order.status == "shipped" }
}

/** The order "Ask for stock" just saved, for the "Order saved" screen. */
data class SentRestock(val order: RestockOrder, val message: String, val adminPhone: String?, val total: Double)

class ShopRepository(private val api: PartnerApi) {
    private val _state = MutableStateFlow(ShopState())
    val state: StateFlow<ShopState> = _state.asStateFlow()

    var lastSent: SentRestock? = null
        private set

    private var catalogCache: List<CatalogProduct>? = null

    /** Loads the whole shop at once; [tier1] adds the sub-dealers and their shelves. */
    suspend fun refresh(tier1: Boolean): Boolean = coroutineScope {
        val profile = async { apiCall { api.traderProfile() } }
        val inventory = async { apiCall { api.inventory() } }
        val restocks = async { loadRestocks() }
        val customers = async { apiCall { api.customerOrders() } }
        val money = async { apiCall { api.commissions() } }
        val admin = async { apiCall { api.adminContact() } }
        val referral = async { if (tier1) apiCall { api.referral() } else null }
        val lucky = async { if (tier1) apiCall { api.luckyDraw() } else null }
        val subs = async { if (tier1) loadSubShops() else null }

        val inv = inventory.await()
        val offline = inv is ApiResult.Failure
        _state.update { s ->
            s.copy(
                profile = (profile.await() as? ApiResult.Success)?.value?.user ?: s.profile,
                inventory = (inv as? ApiResult.Success)?.value?.inventory ?: s.inventory,
                restocks = restocks.await() ?: s.restocks,
                customers = (customers.await() as? ApiResult.Success)?.value?.orders ?: s.customers,
                money = (money.await() as? ApiResult.Success)?.value ?: s.money,
                admin = (admin.await() as? ApiResult.Success)?.value ?: s.admin,
                referralCode = (referral.await() as? ApiResult.Success)?.value?.referralCode ?: s.referralCode,
                luckyDraw = (lucky.await() as? ApiResult.Success)?.value?.enrollments ?: s.luckyDraw,
                subShops = subs.await() ?: s.subShops,
                loadedAt = if (offline) s.loadedAt else System.currentTimeMillis(),
                offline = offline,
            )
        }
        !offline
    }

    /** Screens call this on every return; the shop only reloads when its copy is over [maxAgeMs] old. */
    suspend fun refreshIfStale(tier1: Boolean, maxAgeMs: Long = 30_000) {
        val at = _state.value.loadedAt
        if (at == null || System.currentTimeMillis() - at > maxAgeMs || _state.value.offline) refresh(tier1)
    }

    suspend fun refreshInventory() {
        (apiCall { api.inventory() } as? ApiResult.Success)?.value?.inventory?.let { inv ->
            _state.update { it.copy(inventory = inv, offline = false) }
        }
    }

    suspend fun refreshRestocks() {
        loadRestocks()?.let { list -> _state.update { it.copy(restocks = list) } }
    }

    private suspend fun loadRestocks(): List<Restock>? = coroutineScope {
        val orders = (apiCall { api.restockOrders() } as? ApiResult.Success)?.value?.orders ?: return@coroutineScope null
        // Product counts for the ones on screen; the rest show without.
        val counted = orders.take(10).map { o ->
            async { (apiCall { api.restockOrder(o.id) } as? ApiResult.Success)?.value?.items?.size }
        }.awaitAll()
        orders.mapIndexed { i, o -> Restock(o, counted.getOrNull(i)) }
    }

    private suspend fun loadSubShops(): List<SubShop>? = coroutineScope {
        val dealers = (apiCall { api.subDealers() } as? ApiResult.Success)?.value?.subDealers ?: return@coroutineScope null
        dealers.map { d ->
            async {
                when (val r = apiCall { api.subDealerInventory(d.id) }) {
                    is ApiResult.Success -> SubShop(d, r.value.inventory)
                    is ApiResult.Failure -> SubShop(d, failed = true)
                }
            }
        }.awaitAll()
    }

    suspend fun setAlertLevel(productId: Long, level: Int): ApiResult<SuccessResponse> {
        val r = apiCall { api.setAlertLevel(productId, ThresholdBody(level)) }
        if (r is ApiResult.Success) {
            _state.update { s -> s.copy(inventory = s.inventory?.map { if (it.productId == productId) it.copy(alertAt = level) else it }) }
        }
        return r
    }

    suspend fun catalog(): List<CatalogProduct> =
        catalogCache ?: ((apiCall { api.catalog() } as? ApiResult.Success)?.value?.products.orEmpty().also { if (it.isNotEmpty()) catalogCache = it })

    /** Saves a real restock order (the same as the website's cart) and remembers it for "Order saved". */
    suspend fun askForStock(lines: List<RestockLine>, message: (RestockOrder) -> String): ApiResult<RestockCreated> {
        val r = apiCall { api.askForStock(RestockBody(lines, notes = "Asked from the ST Delivery app")) }
        if (r is ApiResult.Success) {
            val order = r.value.order
            lastSent = SentRestock(order, message(order), _state.value.admin?.phone, order.total)
            _state.update { s -> s.copy(restocks = listOf(Restock(order, lines.size)) + s.restocks.orEmpty()) }
        }
        return r
    }

    fun clear() {
        _state.value = ShopState()
        lastSent = null
        catalogCache = null
    }
}
