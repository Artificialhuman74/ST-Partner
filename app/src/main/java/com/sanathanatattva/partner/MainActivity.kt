package com.sanathanatattva.partner

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.sanathanatattva.partner.alerts.OpenOrder
import com.sanathanatattva.partner.ui.shop.ShopLink
import com.sanathanatattva.partner.ui.nav.StNavHost
import com.sanathanatattva.partner.ui.theme.St
import com.sanathanatattva.partner.ui.theme.StTheme

// AppCompatActivity (not plain ComponentActivity) so the per-app language picker
// works on Android 12 and below via AppCompatDelegate.setApplicationLocales.
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        OpenOrder.from(intent)
        ShopLink.from(intent)
        setContent {
            StTheme {
                StNavHost(Modifier.fillMaxSize().background(St.Cream))
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        OpenOrder.from(intent)
        ShopLink.from(intent)
    }
}
