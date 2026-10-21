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

@Composable
fun StNavHost(modifier: Modifier = Modifier, nav: NavHostController = rememberNavController()) {
    val container = LocalContext.current.appContainer
    val entry by nav.currentBackStackEntryAsState()
    val tab = entry?.destination?.let { d ->
        when {
            d.hasRoute<HomeRoute>() -> HomeTab.Home
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
    CompositionLocalProvider(LocalDockState provides dock) {
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
            )
        }
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
        val labels = listOf(R.string.home_nav_home, R.string.home_nav_earnings, R.string.home_nav_profile).map { stringResource(it) }
        val icons = listOf(StIcons.Home, StIcons.Rupee, StIcons.Person)
        LiquidDock(
            items = labels.zip(icons) { label, icon -> DockItem(label, icon, icon) },
            selectedIndex = current.ordinal,
            onSelect = { nav.toTab(HomeTab.entries[it]) },
        )
    }
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
        HomeTab.Earnings -> EarningsRoute
        HomeTab.Profile -> ProfileRoute
    }
    // Already there: leave the screen be. Navigating would pop and rebuild it (Home's
    // pop finds nothing to pop and falls through to replaceAll), reloading it each tap.
    val here = currentDestination
    if (here != null && when (tab) {
            HomeTab.Home -> here.hasRoute<HomeRoute>()
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
