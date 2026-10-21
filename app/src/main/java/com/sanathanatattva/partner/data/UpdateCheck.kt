package com.sanathanatattva.partner.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import com.sanathanatattva.partner.BuildConfig
import com.sanathanatattva.partner.data.api.ApiResult
import com.sanathanatattva.partner.data.api.PartnerApi
import com.sanathanatattva.partner.data.api.apiCall
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull

/** A newer build to offer after the splash. [required]: this build is below the server's minimum. */
data class UpdateOffer(val required: Boolean)

/**
 * Asks the Play Store whether a newer version is live, and our server whether this build may
 * still take orders. Either can fail quietly (a debug or sideloaded build has no Play answer).
 */
suspend fun checkForUpdate(context: Context, api: PartnerApi): UpdateOffer? = coroutineScope {
    val play = async {
        withTimeoutOrNull(6_000) {
            runCatching {
                AppUpdateManagerFactory.create(context).requestAppUpdateInfo()
                    .updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
            }.getOrNull()
        } ?: false
    }
    val minimum = async {
        withTimeoutOrNull(6_000) {
            (apiCall { api.appVersions() } as? ApiResult.Success)?.value?.delivery?.min_version_code
        } ?: 0
    }
    when {
        BuildConfig.VERSION_CODE < minimum.await() -> UpdateOffer(required = true)
        play.await() -> UpdateOffer(required = false)
        else -> null
    }
}

/** This app's Play Store page (the Play app if it's there, else the website). */
fun openPlayStore(context: Context) {
    val id = context.packageName
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, "market://details?id=$id".toUri())
                .setPackage("com.android.vending")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (_: ActivityNotFoundException) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$id".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
