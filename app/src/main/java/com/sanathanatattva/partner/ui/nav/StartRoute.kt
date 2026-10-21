package com.sanathanatattva.partner.ui.nav

import com.sanathanatattva.partner.data.Session
import com.sanathanatattva.partner.data.Settings
import kotlinx.serialization.Serializable

@Serializable data object SplashRoute
@Serializable data object LanguageRoute
@Serializable data object IntroRoute
@Serializable data object LoginRoute
@Serializable data class LoggedInRoute(val firstName: String)
@Serializable data object UnderReviewRoute
@Serializable data object HomeRoute
@Serializable data class SignupRoute(val viaGoogle: Boolean = false)
@Serializable data class ApprovedRoute(val firstName: String)
@Serializable data object RejectedRoute
@Serializable data class PermGateRoute(val fromApproval: Boolean = false)
@Serializable data object AutostartGuideRoute
@Serializable data object ProfileRoute
@Serializable data object EarningsRoute
@Serializable data object StartPlacesRoute
@Serializable data object ChangeLanguageRoute
@Serializable data object DeliveryHoursRoute
@Serializable data object PayoutRoute
@Serializable data object RiderJobsRoute
@Serializable data object ContainerPickupsRoute
@Serializable data object EditProfileRoute
@Serializable data class OrderRoute(val orderId: Long)

// My Shop (traders)
@Serializable data object MyShopRoute
@Serializable data object ShelfRoute
@Serializable data class ProductRoute(val productId: Long)
@Serializable data class AskStockRoute(val productId: Long = -1)
@Serializable data object SentRoute
@Serializable data object ShopOrdersRoute
@Serializable data object ShopMoneyRoute
@Serializable data object ShopPeopleRoute
@Serializable data class SubDealerRoute(val id: Long)

/** Where the splash hands off to (handoff §4.2). A saved login skips the first-run screens. */
fun startRoute(settings: Settings, session: Session?): Any = when {
    session != null && session.user.isPendingReview -> UnderReviewRoute
    session != null -> HomeRoute
    !settings.languageChosen -> LanguageRoute
    !settings.introSeen -> IntroRoute
    else -> LoginRoute
}
