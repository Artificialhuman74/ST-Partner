package com.sanathanatattva.partner.ui.nav

import androidx.lifecycle.Lifecycle
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.sanathanatattva.partner.data.UpdateOffer
import com.sanathanatattva.partner.data.checkForUpdate
import com.sanathanatattva.partner.ui.update.UpdateScreen
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.ui.components.DockItem
import com.sanathanatattva.partner.ui.components.DockState
import com.sanathanatattva.partner.ui.components.LocalDockState
import com.sanathanatattva.partner.ui.components.LiquidDock
import com.sanathanatattva.partner.ui.components.StIcons
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.sanathanatattva.partner.ui.components.OilEase
import com.sanathanatattva.partner.ui.earnings.EarningsScreen
import com.sanathanatattva.partner.ui.home.HomeScreen
import com.sanathanatattva.partner.ui.home.HomeTab
import com.sanathanatattva.partner.ui.intro.IntroScreen
import com.sanathanatattva.partner.ui.language.LanguageScreen
import com.sanathanatattva.partner.ui.loggedin.LoggedInScreen
import com.sanathanatattva.partner.ui.login.LoginScreen
import com.sanathanatattva.partner.ui.permissions.AutostartGuideScreen
import com.sanathanatattva.partner.ui.permissions.PermGateScreen
import com.sanathanatattva.partner.ui.profile.ContainerPickupsScreen
import com.sanathanatattva.partner.ui.profile.DeliveryHoursScreen
import com.sanathanatattva.partner.ui.profile.EditProfileScreen
import com.sanathanatattva.partner.ui.profile.PayoutScreen
import com.sanathanatattva.partner.ui.profile.ProfileLink
import com.sanathanatattva.partner.ui.profile.ProfileScreen
import com.sanathanatattva.partner.ui.profile.RiderJobsScreen
import com.sanathanatattva.partner.ui.profile.StartPlacesScreen
import com.sanathanatattva.partner.ui.review.ApprovedScreen
import com.sanathanatattva.partner.ui.review.RejectedScreen
import com.sanathanatattva.partner.ui.review.UnderReviewScreen
import com.sanathanatattva.partner.ui.signup.SignupScreen
import com.sanathanatattva.partner.ui.splash.SplashScreen
import androidx.compose.ui.platform.LocalContext
import com.sanathanatattva.partner.appContainer
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.sanathanatattva.partner.alerts.OpenOrder
import com.sanathanatattva.partner.ui.order.OrderScreen
import com.sanathanatattva.partner.ui.shop.AskStockScreen
import com.sanathanatattva.partner.ui.shop.MyShopScreen
import com.sanathanatattva.partner.ui.shop.ProductScreen
import com.sanathanatattva.partner.ui.shop.SentScreen
import com.sanathanatattva.partner.ui.shop.ShelfScreen
import com.sanathanatattva.partner.ui.shop.ShopLink
import com.sanathanatattva.partner.ui.shop.ShopMoneyScreen
import com.sanathanatattva.partner.ui.shop.ShopOrdersScreen
import com.sanathanatattva.partner.ui.shop.ShopPeopleScreen
import com.sanathanatattva.partner.ui.shop.ShopPlace
import com.sanathanatattva.partner.ui.shop.ShopTarget
import com.sanathanatattva.partner.ui.shop.SubDealerScreen
import com.sanathanatattva.partner.ui.tour.LocalTour
import com.sanathanatattva.partner.ui.tour.TourOverlay
import com.sanathanatattva.partner.ui.tour.TourScreen
import com.sanathanatattva.partner.ui.tour.TourState
import com.sanathanatattva.partner.ui.tour.TourStep
import com.sanathanatattva.partner.alerts.AlertReadiness
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StNavHost(modifier: Modifier = Modifier, nav: NavHostController = rememberNavController()) {
    val container = LocalContext.current.appContainer
    val entry by nav.currentBackStackEntryAsState()
    val tab = entry?.destination?.let { d ->
        when {
            d.hasRoute<HomeRoute>() -> HomeTab.Home
            d.hasRoute<MyShopRoute>() -> HomeTab.MyShop
            d.hasRoute<EarningsRoute>() -> HomeTab.Earnings
            d.hasRoute<ProfileRoute>() -> HomeTab.Profile
            else -> null
        }
    }
    // As in the consumer app, the dock floats over the tab screens (they pad their
    // scrolling content by DockContentClearance) and lives outside them, so the pill
    // glides from tab to tab instead of being redrawn on each screen.
    // An accepted ring (or a tapped order notification) opens that order, once signed in.
    val openOrder by OpenOrder.requests.collectAsState()
    LaunchedEffect(openOrder, entry?.destination) {
        val id = openOrder ?: return@LaunchedEffect
        val dest = entry?.destination ?: return@LaunchedEffect
        if (dest.hasRoute<SplashRoute>() || dest.hasRoute<LoginRoute>() || dest.hasRoute<LanguageRoute>() || dest.hasRoute<IntroRoute>()) return@LaunchedEffect
        OpenOrder.consumed()
        // Already looking at this order: stay. Another order's screen: open this one over it
        // (single-top would reuse that screen, still showing the other order).
        val showing = dest.hasRoute<OrderRoute>() && entry?.toRoute<OrderRoute>()?.orderId == id
        if (!showing) nav.navigate(OrderRoute(id))
    }
    // A tapped shop alert ("Ask for stock", "See money"…) opens that part of My Shop.
    val shopLink by ShopLink.requests.collectAsState()
    LaunchedEffect(shopLink, entry?.destination) {
        val target = shopLink ?: return@LaunchedEffect
        val dest = entry?.destination ?: return@LaunchedEffect
        if (dest.hasRoute<SplashRoute>() || dest.hasRoute<LoginRoute>() || dest.hasRoute<LanguageRoute>() || dest.hasRoute<IntroRoute>()) return@LaunchedEffect
        ShopLink.consumed()
        if (container.sessions.latest?.user?.isRider != false) return@LaunchedEffect
        nav.toTab(HomeTab.MyShop)
        when (target) {
            ShopTarget.AskStock -> nav.navigate(AskStockRoute())
            ShopTarget.Money -> nav.navigate(ShopMoneyRoute)
            ShopTarget.Orders -> nav.navigate(ShopOrdersRoute)
            is ShopTarget.SubDealer -> nav.navigate(SubDealerRoute(target.id))
        }
    }
    val session by container.sessions.session.collectAsState(initial = container.sessions.latest)
    val rider = session?.user?.isRider != false
    val shopState by container.shop.state.collectAsState()
    // Shop partners get My Shop between Home and Earnings; riders keep three tabs.
    val tabs = if (rider) listOf(HomeTab.Home, HomeTab.Earnings, HomeTab.Profile) else HomeTab.entries
    val tabLabels = mapOf(
        HomeTab.Home to stringResource(R.string.home_nav_home),
        HomeTab.MyShop to stringResource(R.string.shop_tab),
        HomeTab.Earnings to stringResource(R.string.home_nav_earnings),
        HomeTab.Profile to stringResource(R.string.home_nav_profile),
    )

    // The welcome tour: once per account on this phone, on the first Home after logging in.
    val tour = remember { TourState() }
    val tourScope = rememberCoroutineScope()
    val tourContext = LocalContext.current
    val tourScreen = when {
        entry?.destination?.hasRoute<HomeRoute>() == true -> TourScreen.Home
        entry?.destination?.hasRoute<PermGateRoute>() == true -> TourScreen.Alerts
        else -> TourScreen.Other
    }
    val userId = session?.user?.id
    LaunchedEffect(userId) {
        tour.onDone = { userId?.let { id -> tourScope.launch { container.settings.setTourSeen(id) } } }
    }
    LaunchedEffect(tourScreen, userId) {
        val id = userId ?: return@LaunchedEffect
        if (tourScreen != TourScreen.Home || tour.step != null) return@LaunchedEffect
        if (container.settings.tourSeen(id)) return@LaunchedEffect
        // Let Home settle first (its numbers in and, for a shop, its shelf) so nothing shifts
        // under the spotlight; give up waiting after a few seconds on a slow connection.
        val since = System.currentTimeMillis()
        while (System.currentTimeMillis() - since < 6_000) {
            val shop = container.shop.state.value
            val homeIn = container.partner.lastHome != null
            val shopIn = rider || shop.inventory != null || shop.offline
            if (homeIn && shopIn) break
            delay(100)
        }
        delay(700)
        if (tour.step == null && tourScreen == TourScreen.Home) tour.start()
    }
    // Tapping the yellow bar opens the checklist: the tour follows. Coming back early sends it back.
    LaunchedEffect(tourScreen, tour.step) {
        when {
            tour.step == TourStep.Bar && tourScreen == TourScreen.Alerts -> tour.go(TourStep.Checklist)
            tour.step == TourStep.Checklist && tourScreen == TourScreen.Home ->
                tour.go(if (AlertReadiness.check(tourContext).ready) TourStep.Online else TourStep.Bar)
        }
    }
    LaunchedEffect(tour.checklistKey, tour.checklistReported, tour.step) {
        if (tour.step == TourStep.Checklist && tour.checklistReported && tour.checklistKey == null) {
            delay(750)
            if (tour.step == TourStep.Checklist && tour.checklistKey == null) tour.go(TourStep.AllSet)
        }
    }
    // A newer version on the Play Store is offered once the splash hands off, once per launch.
    // An optional one that's slow to be known isn't sprung on someone already working;
    // a required one (below the server's minimum) always shows.
    val context = LocalContext.current
    val splashing = entry?.destination?.hasRoute<SplashRoute>() != false
    var update by remember { mutableStateOf<UpdateOffer?>(null) }
    var updateDone by rememberSaveable { mutableStateOf(false) }
    var splashEndedAt by remember { mutableLongStateOf(0L) }
    LaunchedEffect(splashing) { if (!splashing && splashEndedAt == 0L) splashEndedAt = System.currentTimeMillis() }
    LaunchedEffect(Unit) {
        if (updateDone) return@LaunchedEffect
        val offer = checkForUpdate(context.applicationContext, container.api)
        val late = splashEndedAt > 0 && System.currentTimeMillis() - splashEndedAt > 2_000
        if (offer != null && (offer.required || !late)) update = offer else updateDone = true
    }

    val dock = remember { DockState() }
    CompositionLocalProvider(LocalDockState provides dock, LocalTour provides tour) {
    Box(modifier.fillMaxSize()) {
    NavHost(
        navController = nav,
        startDestination = SplashRoute,
        modifier = Modifier.fillMaxSize(),
        // One move for every page, forward and back (arrow or the phone's Back alike): the old
        // page fades out, then the new one fades in, so the two never smear over each other.
        enterTransition = { PageIn },
        exitTransition = { PageOut },
        popEnterTransition = { PageIn },
        popExitTransition = { PageOut },
    ) {
        composable<SplashRoute>(enterTransition = { fadeIn(tween(1)) }) {
            SplashScreen(onDone = { route -> nav.replaceAll(route) })
        }
        composable<LanguageRoute>(enterTransition = { fadeIn(tween(500)) }) {
            LanguageScreen(onDone = { nav.replaceAll(IntroRoute) })
        }
        composable<IntroRoute> {
            IntroScreen(onDone = { nav.replaceAll(LoginRoute) })
        }
        composable<LoginRoute> {
            LoginScreen(
                onLoggedIn = { user ->
                    nav.replaceAll(if (user.isPendingReview) UnderReviewRoute else LoggedInRoute(user.firstName))
                },
                onJoin = { viaGoogle -> nav.go(SignupRoute(viaGoogle)) },
            )
        }
        composable<SignupRoute> { entry ->
            SignupScreen(
                viaGoogle = entry.toRoute<SignupRoute>().viaGoogle,
                onLeave = { if (!nav.back()) nav.replaceAll(LoginRoute) },
                onSubmitted = { nav.replaceAll(UnderReviewRoute) },
            )
        }
        composable<LoggedInRoute>(enterTransition = { fadeIn(tween(350)) }) { entry ->
            LoggedInScreen(firstName = entry.toRoute<LoggedInRoute>().firstName, onDone = { nav.replaceAll(HomeRoute) })
        }
        composable<UnderReviewRoute> {
            UnderReviewScreen(
                onLoggedOut = { nav.replaceAll(LoginRoute) },
                onApproved = { firstName -> nav.replaceAll(ApprovedRoute(firstName)) },
                onSentBack = { nav.replaceAll(RejectedRoute) },
            )
        }
        composable<ApprovedRoute>(enterTransition = { fadeIn(tween(400)) }) { entry ->
            // Approval leads straight into the alert setup (canvas: Approved → "Set up alerts").
            ApprovedScreen(firstName = entry.toRoute<ApprovedRoute>().firstName, onContinue = { nav.replaceAll(PermGateRoute(fromApproval = true)) })
        }
        composable<RejectedRoute> {
            RejectedScreen(
                onResent = { nav.replaceAll(UnderReviewRoute) },
                onLoggedOut = { nav.replaceAll(LoginRoute) },
            )
        }
        composable<HomeRoute>(enterTransition = { fadeIn(tween(500)) }) {
            HomeScreen(
                onSignedOut = { nav.replaceAll(LoginRoute) },
                onPermissions = { nav.go(PermGateRoute()) },
                onTab = { nav.toTab(it) },
                onOpenOrder = { nav.go(OrderRoute(it)) },
                onAskStock = { nav.go(AskStockRoute()) },
            )
        }
        composable<MyShopRoute> {
            MyShopScreen(onOpen = { place ->
                when (place) {
                    ShopPlace.Shelf -> nav.go(ShelfRoute)
                    ShopPlace.Money -> nav.go(ShopMoneyRoute)
                    ShopPlace.Orders -> nav.go(ShopOrdersRoute)
                    ShopPlace.People -> nav.go(ShopPeopleRoute)
                    ShopPlace.AskStock -> nav.go(AskStockRoute())
                }
            })
        }
        composable<ShelfRoute> {
            ShelfScreen(onBack = { nav.back() }, onProduct = { nav.go(ProductRoute(it)) }, onAsk = { nav.go(AskStockRoute()) })
        }
        composable<ProductRoute> { entry ->
            ProductScreen(entry.toRoute<ProductRoute>().productId, onBack = { nav.back() }, onAsk = { nav.go(AskStockRoute(it)) })
        }
        composable<AskStockRoute> { entry ->
            AskStockScreen(
                preselect = entry.toRoute<AskStockRoute>().productId.takeIf { it > 0 },
                onBack = { nav.back() },
                onSent = { nav.navigate(SentRoute) { popUpTo<AskStockRoute> { inclusive = true } } },
            )
        }
        composable<SentRoute> {
            SentScreen(onSeeOrder = { nav.navigate(ShopOrdersRoute) { popUpTo<SentRoute> { inclusive = true } } })
        }
        composable<ShopOrdersRoute> { ShopOrdersScreen(onBack = { nav.back() }) }
        composable<ShopMoneyRoute> { ShopMoneyScreen(onBack = { nav.back() }) }
        composable<ShopPeopleRoute> { ShopPeopleScreen(onBack = { nav.back() }, onSubDealer = { nav.go(SubDealerRoute(it)) }) }
        composable<SubDealerRoute> { entry -> SubDealerScreen(entry.toRoute<SubDealerRoute>().id, onBack = { nav.back() }) }
        composable<PermGateRoute> { entry ->
            val fromApproval = entry.toRoute<PermGateRoute>().fromApproval
            PermGateScreen(
                onBack = { if (fromApproval) nav.replaceAll(HomeRoute) else nav.back() },
                onReady = { alreadyOnline ->
                    when {
                        alreadyOnline && !fromApproval -> nav.back()
                        else -> {
                            // "Go online" lands on Home with the start-place picker already up.
                            if (!alreadyOnline) container.partner.wantsToGoOnline = true
                            if (fromApproval) nav.replaceAll(HomeRoute) else nav.toTab(HomeTab.Home)
                        }
                    }
                },
                onAutostartGuide = { nav.go(AutostartGuideRoute) },
            )
        }
        composable<AutostartGuideRoute> {
            AutostartGuideScreen(onBack = { nav.back() }, onDone = { nav.back() })
        }
        composable<ProfileRoute> {
            ProfileScreen(
                onOpen = { link ->
                    when (link) {
                        ProfileLink.StartPlaces -> nav.go(StartPlacesRoute)
                        ProfileLink.Alerts -> nav.go(PermGateRoute())
                        ProfileLink.Language -> nav.go(ChangeLanguageRoute)
                        ProfileLink.DeliveryHours -> nav.go(DeliveryHoursRoute)
                        ProfileLink.Payout -> nav.go(PayoutRoute)
                        ProfileLink.RiderJobs -> nav.go(RiderJobsRoute)
                        ProfileLink.ContainerPickups -> nav.go(ContainerPickupsRoute)
                        // Vehicle changes need the team (licence/plate are re-checked), so they share Edit profile's note.
                        ProfileLink.Edit, ProfileLink.Vehicle -> nav.go(EditProfileRoute)
                        ProfileLink.Tour -> {
                            nav.toTab(HomeTab.Home)
                            // Once Home has faded in, so the tour doesn't start over a moving page.
                            tourScope.launch { delay(600); tour.start() }
                        }
                    }
                },
                onLoggedOut = { nav.replaceAll(LoginRoute) },
            )
        }
        composable<EarningsRoute> {
            EarningsScreen(onPayout = { nav.go(PayoutRoute) })
        }
        composable<StartPlacesRoute> {
            StartPlacesScreen(onBack = { nav.back() })
        }
        composable<ChangeLanguageRoute> {
            LanguageScreen(onDone = { nav.back() })
        }
        composable<DeliveryHoursRoute> { DeliveryHoursScreen(onBack = { nav.back() }) }
        composable<PayoutRoute> { PayoutScreen(onBack = { nav.back() }) }
        composable<RiderJobsRoute> { RiderJobsScreen(onBack = { nav.back() }) }
        composable<ContainerPickupsRoute> { ContainerPickupsScreen(onBack = { nav.back() }) }
        composable<EditProfileRoute> { EditProfileScreen(onBack = { nav.back() }) }
        composable<OrderRoute> { OrderScreen(onBack = { nav.toTab(HomeTab.Home) }) }
    }
    AnimatedVisibility(
        visible = tab != null && dock.hiddenBy == 0,
        enter = slideInVertically(tween(320, easing = OilEase)) { it } + fadeIn(tween(200)),
        exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(160)),
        modifier = Modifier.align(Alignment.BottomCenter),
    ) {
        val current = tab ?: HomeTab.Home
        val items = tabs.map { t ->
            val label = tabLabels.getValue(t)
            when (t) {
                HomeTab.Home -> DockItem(label, StIcons.Home, StIcons.Home)
                HomeTab.MyShop -> DockItem(label, StIcons.Store, StIcons.Store, badge = shopState.attention.size)
                HomeTab.Earnings -> DockItem(label, StIcons.Rupee, StIcons.Rupee)
                HomeTab.Profile -> DockItem(label, StIcons.Person, StIcons.Person)
            }
        }
        LiquidDock(
            items = items,
            selectedIndex = tabs.indexOf(current).coerceAtLeast(0),
            onSelect = { nav.toTab(tabs[it]) },
            onItemBounds = { i, bounds -> if (tour.targets["dock_$i"] != bounds) tour.targets["dock_$i"] = bounds },
        )
    }
    TourOverlay(
        tour = tour,
        screen = tourScreen,
        firstName = session?.user?.firstName.orEmpty(),
        tabs = tabs,
        tabLabel = { tabLabels.getValue(it) },
        alertsReady = { AlertReadiness.check(context).ready },
        onBackHome = { nav.toTab(HomeTab.Home) },
    )
    val offer = update
    if (!splashing && offer != null && !updateDone) UpdateScreen(offer, onLater = { updateDone = true })
    }
    }
}

private val PageOut = fadeOut(tween(140))
private val PageIn = fadeIn(tween(260, delayMillis = 100, easing = OilEase))

/** Bottom tabs swap in place over Home, so Back from any tab lands on Home. */
private fun NavHostController.toTab(tab: HomeTab) {
    val route: Any = when (tab) {
        HomeTab.Home -> HomeRoute
        HomeTab.MyShop -> MyShopRoute
        HomeTab.Earnings -> EarningsRoute
        HomeTab.Profile -> ProfileRoute
    }
    // Already there: leave the screen be. Navigating would pop and rebuild it (Home's
    // pop finds nothing to pop and falls through to replaceAll), reloading it each tap.
    val here = currentDestination
    if (here != null && when (tab) {
            HomeTab.Home -> here.hasRoute<HomeRoute>()
            HomeTab.MyShop -> here.hasRoute<MyShopRoute>()
            HomeTab.Earnings -> here.hasRoute<EarningsRoute>()
            HomeTab.Profile -> here.hasRoute<ProfileRoute>()
        }
    ) return
    if (tab == HomeTab.Home) {
        if (!popBackStack(HomeRoute, inclusive = false)) replaceAll(HomeRoute)
        return
    }
    navigate(route) {
        popUpTo<HomeRoute> { inclusive = false }
        launchSingleTop = true
    }
}

/** Forward-only steps (splash → language → intro → login → home) leave nothing to go back to. */
private fun NavHostController.replaceAll(route: Any) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

/**
 * Taps that land while a screen is still sliding in or out are ignored, so a quick double tap
 * never opens two copies of a screen or goes back two screens at once.
 */
private val NavHostController.settled: Boolean
    get() = currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED

private fun NavHostController.go(route: Any) {
    if (!settled) return
    navigate(route) { launchSingleTop = true }
}

/** Back from a screen's own arrow. True when handled (or ignored mid-transition). */
private fun NavHostController.back(): Boolean = if (!settled) true else popBackStack()
