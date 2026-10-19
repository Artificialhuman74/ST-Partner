package com.sanathanatattva.partner

import com.sanathanatattva.partner.data.Session
import com.sanathanatattva.partner.data.Settings
import com.sanathanatattva.partner.data.api.User
import com.sanathanatattva.partner.ui.nav.HomeRoute
import com.sanathanatattva.partner.ui.nav.IntroRoute
import com.sanathanatattva.partner.ui.nav.LanguageRoute
import com.sanathanatattva.partner.ui.nav.LoginRoute
import com.sanathanatattva.partner.ui.nav.UnderReviewRoute
import com.sanathanatattva.partner.ui.nav.startRoute
import org.junit.Assert.assertEquals
import org.junit.Test

class StartRouteTest {
    private val trader = User(id = 7, name = "Lakshmi Devi", email = "l@example.com", role = "trader")

    @Test fun firstRunAsksForLanguage() =
        assertEquals(LanguageRoute, startRoute(Settings(), null))

    @Test fun languageChosenShowsIntro() =
        assertEquals(IntroRoute, startRoute(Settings(languageChosen = true), null))

    @Test fun introSeenGoesToLogin() =
        assertEquals(LoginRoute, startRoute(Settings(languageChosen = true, introSeen = true), null))

    @Test fun savedLoginSkipsFirstRunScreens() =
        assertEquals(HomeRoute, startRoute(Settings(), Session("t", trader)))

    @Test fun riderAwaitingReviewSeesUnderReview() =
        assertEquals(UnderReviewRoute, startRoute(Settings(true, true), Session("t", trader.copy(role = "rider", reviewStatus = "pending_review"))))
}
