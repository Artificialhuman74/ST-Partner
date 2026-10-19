package com.sanathanatattva.partner.ui.nav

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
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
        nav.navigate(OrderRoute(id)) { launchSingleTop = true }
    }
    val dock = remember { DockState() }
    CompositionLocalProvider(LocalDockState provides dock) {
    Box(modifier.fillMaxSize()) {
    NavHost(
        navController = nav,
        startDestination = SplashRoute,
        modifier = Modifier.fillMaxSize(),
        // Calm, viscous moves: a short drift plus fade, never a bounce.
        enterTransition = { fadeIn(tween(420, easing = OilEase)) + slideInHorizontally(tween(520, easing = OilEase)) { it / 12 } },
        exitTransition = { fadeOut(tween(220)) },
        popEnterTransition = { fadeIn(tween(420, easing = OilEase)) + slideInHorizontally(tween(520, easing = OilEase)) { -it / 12 } },
        popExitTransition = { fadeOut(tween(220)) + slideOutHorizontally(tween(320)) { it / 12 } },
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
                onJoin = { viaGoogle -> nav.navigate(SignupRoute(viaGoogle)) },
            )
        }
        composable<SignupRoute> { entry ->
            SignupScreen(
                viaGoogle = entry.toRoute<SignupRoute>().viaGoogle,
                onLeave = { if (!nav.popBackStack()) nav.replaceAll(LoginRoute) },
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
        composable<HomeRoute>(enterTransition = { fadeIn(tween(500)) }, popEnterTransition = { fadeIn(tween(220)) }) {
            HomeScreen(
                onSignedOut = { nav.replaceAll(LoginRoute) },
                onPermissions = { nav.navigate(PermGateRoute()) },
                onTab = { nav.toTab(it) },
                onOpenOrder = { nav.navigate(OrderRoute(it)) },
            )
        }
        composable<PermGateRoute> { entry ->
            val fromApproval = entry.toRoute<PermGateRoute>().fromApproval
            PermGateScreen(
                onBack = { if (fromApproval) nav.replaceAll(HomeRoute) else nav.popBackStack() },
                onReady = { alreadyOnline ->
                    when {
                        alreadyOnline && !fromApproval -> nav.popBackStack()
                        else -> {
                            // "Go online" lands on Home with the start-place picker already up.
                            if (!alreadyOnline) container.partner.wantsToGoOnline = true
                            if (fromApproval) nav.replaceAll(HomeRoute) else nav.toTab(HomeTab.Home)
                        }
                    }
                },
                onAutostartGuide = { nav.navigate(AutostartGuideRoute) },
            )
        }
        composable<AutostartGuideRoute> {
            AutostartGuideScreen(onBack = { nav.popBackStack() }, onDone = { nav.popBackStack() })
        }
        composable<ProfileRoute>(enterTransition = { fadeIn(tween(220)) }, exitTransition = { fadeOut(tween(160)) }, popEnterTransition = { fadeIn(tween(220)) }, popExitTransition = { fadeOut(tween(160)) }) {
            ProfileScreen(
                onOpen = { link ->
                    when (link) {
                        ProfileLink.StartPlaces -> nav.navigate(StartPlacesRoute)
                        ProfileLink.Alerts -> nav.navigate(PermGateRoute())
                        ProfileLink.Language -> nav.navigate(ChangeLanguageRoute)
                        ProfileLink.DeliveryHours -> nav.navigate(DeliveryHoursRoute)
                        ProfileLink.Payout -> nav.navigate(PayoutRoute)
                        ProfileLink.RiderJobs -> nav.navigate(RiderJobsRoute)
                        ProfileLink.ContainerPickups -> nav.navigate(ContainerPickupsRoute)
                        // Vehicle changes need the team (licence/plate are re-checked), so they share Edit profile's note.
                        ProfileLink.Edit, ProfileLink.Vehicle -> nav.navigate(EditProfileRoute)
                    }
                },
                onLoggedOut = { nav.replaceAll(LoginRoute) },
            )
        }
        composable<EarningsRoute>(enterTransition = { fadeIn(tween(220)) }, exitTransition = { fadeOut(tween(160)) }, popEnterTransition = { fadeIn(tween(220)) }, popExitTransition = { fadeOut(tween(160)) }) {
            EarningsScreen(onPayout = { nav.navigate(PayoutRoute) })
        }
        composable<StartPlacesRoute> {
            StartPlacesScreen(onBack = { nav.popBackStack() })
        }
        composable<ChangeLanguageRoute> {
            LanguageScreen(onDone = { nav.popBackStack() })
        }
        composable<DeliveryHoursRoute> { DeliveryHoursScreen(onBack = { nav.popBackStack() }) }
        composable<PayoutRoute> { PayoutScreen(onBack = { nav.popBackStack() }) }
        composable<RiderJobsRoute> { RiderJobsScreen(onBack = { nav.popBackStack() }) }
        composable<ContainerPickupsRoute> { ContainerPickupsScreen(onBack = { nav.popBackStack() }) }
        composable<EditProfileRoute> { EditProfileScreen(onBack = { nav.popBackStack() }) }
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
    }
    }
}

/** Bottom tabs swap in place over Home, so Back from any tab lands on Home. */
private fun NavHostController.toTab(tab: HomeTab) {
    val route: Any = when (tab) {
        HomeTab.Home -> HomeRoute
        HomeTab.Earnings -> EarningsRoute
        HomeTab.Profile -> ProfileRoute
    }
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
