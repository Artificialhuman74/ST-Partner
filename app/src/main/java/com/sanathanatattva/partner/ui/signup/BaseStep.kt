package com.sanathanatattva.partner.ui.signup

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.data.signup.BaseLabel
import com.sanathanatattva.partner.data.signup.SignupDraft
import com.sanathanatattva.partner.ui.components.StIcons
import com.sanathanatattva.partner.ui.components.StTextField
import com.sanathanatattva.partner.ui.theme.St
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import java.util.Locale
import kotlin.coroutines.resume

/** Free OpenStreetMap vector tiles, no key (handoff §10: OSM/OSRM maps, styled later to look like Google's). */
const val MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private val Bengaluru = LatLng(12.9716, 77.5946)

// ── 2 · Where do you start? ──────────────────────────────────────────

@Composable
fun BaseStep(draft: SignupDraft, errors: Map<String, String>, onChange: Edit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var moving by remember { mutableStateOf(false) }
    var finding by remember { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var noLocation by remember { mutableStateOf(false) }
    var geocodeJob by remember { mutableStateOf<Job?>(null) }

    fun moveTo(lat: Double, lng: Double) {
        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 17.0), 700)
    }

    fun locate() {
        scope.launch {
            val here = currentLocation(context)
            if (here == null) noLocation = true else { noLocation = false; moveTo(here.first, here.second) }
        }
    }

    val askLocation = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) locate() else noLocation = true
    }

    fun locateOrAsk() {
        if (hasLocationPermission(context)) locate()
        else askLocation.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    // First visit with no pin yet: start from where the rider is standing.
    LaunchedEffect(map) {
        if (map != null && draft.baseLat == null) locateOrAsk()
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(24.dp)).background(St.SandDeep)) {
            LiveMap(
                start = draft.baseLat?.let { LatLng(it, draft.baseLng ?: Bengaluru.longitude) } ?: Bengaluru,
                onReady = { map = it },
                onMoveStarted = { moving = true },
                onIdle = { target ->
                    moving = false
                    onChange { it.copy(baseLat = target.latitude, baseLng = target.longitude) }
                    geocodeJob?.cancel()
                    geocodeJob = scope.launch {
                        delay(350)
                        finding = true
                        val address = reverseGeocode(context, target.latitude, target.longitude)
                        finding = false
                        if (address != null && !editing) onChange { it.copy(baseAddress = address) }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
            // The pin stays in the middle; the map moves under it. It lifts while dragging.
            val lift by animateDpAsState(if (moving) (-10).dp else 0.dp, label = "pin")
            Icon(
                StIcons.Pin, contentDescription = null, tint = St.Green,
                modifier = Modifier.align(Alignment.Center).offset(y = (-20).dp + lift).size(44.dp),
            )
            Box(Modifier.align(Alignment.Center).size(8.dp).clip(CircleShape).background(St.Ink.copy(alpha = 0.35f)))
            Row(
                Modifier.align(Alignment.TopCenter).padding(top = 10.dp)
                    .clip(RoundedCornerShape(16.dp)).background(St.Paper.copy(alpha = 0.94f)).padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(StIcons.Move, contentDescription = null, tint = St.Green, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.signup_base_move), style = MaterialTheme.typography.labelMedium.copy(fontSize = 15.sp), color = St.Ink)
            }
            val locateLabel = stringResource(R.string.signup_base_my_location)
            Box(
                Modifier.align(Alignment.BottomEnd).padding(12.dp).size(52.dp)
                    .shadow(6.dp, CircleShape).clip(CircleShape).background(St.Paper)
                    .semantics { contentDescription = locateLabel }
                    .selectable(selected = false, role = Role.Button, onClick = ::locateOrAsk),
                contentAlignment = Alignment.Center,
            ) {
                Icon(StIcons.Locate, contentDescription = null, tint = St.Green, modifier = Modifier.size(26.dp))
            }
        }
        if (noLocation) Text(stringResource(R.string.signup_base_no_location), style = MaterialTheme.typography.bodySmall, color = St.Ink2)

        Text(stringResource(R.string.signup_base_save_as), style = MaterialTheme.typography.labelMedium, color = St.Ink)
        Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(
                Triple(BaseLabel.Home, StIcons.Home, R.string.signup_base_home),
                Triple(BaseLabel.Shop, StIcons.Shop, R.string.signup_base_shop),
                Triple(BaseLabel.Other, StIcons.Pin, R.string.signup_base_other),
            ).forEach { (label, icon, text) ->
                val on = draft.baseLabel == label
                Row(
                    Modifier.weight(1f).heightIn(min = 52.dp).clip(RoundedCornerShape(26.dp))
                        .background(if (on) St.GreenWash else St.Paper)
                        .border(if (on) 2.5.dp else 2.dp, if (on) St.Green else St.Line, RoundedCornerShape(26.dp))
                        .selectable(selected = on, role = Role.RadioButton, onClick = { onChange { it.copy(baseLabel = label) } })
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(icon, contentDescription = null, tint = if (on) St.Green else St.Ink2, modifier = Modifier.size(20.dp))
                    Text(stringResource(text), style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp), color = if (on) St.Green else St.Ink)
                }
            }
        }

        if (editing) {
            StTextField(
                value = draft.baseAddress,
                onValueChange = { v -> onChange { it.copy(baseAddress = v) } },
                label = stringResource(R.string.signup_base_address),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                labelEnd = {
                    TextButton(onClick = { editing = false }) {
                        Text(stringResource(R.string.signup_looks_good), color = St.Green, style = MaterialTheme.typography.labelMedium)
                    }
                },
            )
        } else {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(St.Beige).border(1.5.dp, St.BeigeLine, RoundedCornerShape(20.dp))
                    .padding(start = 16.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    val line1 = draft.baseAddress.substringBefore(", ").ifBlank { null }
                    val rest = draft.baseAddress.substringAfter(", ", "").ifBlank { null }
                    Text(
                        when {
                            finding -> stringResource(R.string.signup_base_finding)
                            line1 != null -> line1
                            else -> stringResource(R.string.signup_base_move)
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = St.Ink,
                    )
                    if (!finding && rest != null) Text(rest, style = MaterialTheme.typography.bodySmall, color = St.Ink2)
                }
                TextButton(onClick = { editing = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.signup_base_edit), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = St.Green)
                }
            }
        }
        FieldError(errors["base"])
        Text(stringResource(R.string.signup_base_more_later), style = MaterialTheme.typography.bodySmall, color = St.Ink2)
    }
}

/** MapLibre map with OpenFreeMap tiles; gestures limited to pan and zoom (no tilt/rotate). */
@Composable
fun LiveMap(
    start: LatLng,
    onReady: (MapLibreMap) -> Unit,
    onMoveStarted: () -> Unit,
    onIdle: (LatLng) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
            getMapAsync { map ->
                map.uiSettings.isRotateGesturesEnabled = false
                map.uiSettings.isTiltGesturesEnabled = false
                map.uiSettings.isCompassEnabled = false
                map.cameraPosition = CameraPosition.Builder().target(start).zoom(16.5).build()
                map.setStyle(Style.Builder().fromUri(MAP_STYLE_URL)) {
                    map.addOnCameraMoveStartedListener { onMoveStarted() }
                    map.addOnCameraIdleListener { onIdle(map.cameraPosition.target ?: start) }
                    onReady(map)
                }
            }
        }
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }
    AndroidView(factory = { mapView }, modifier = modifier)
}

fun hasLocationPermission(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

@android.annotation.SuppressLint("MissingPermission")
suspend fun currentLocation(context: Context): Pair<Double, Double>? {
    if (!hasLocationPermission(context)) return null
    return runCatching {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val fix = client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token).await()
            ?: client.lastLocation.await()
        fix?.let { it.latitude to it.longitude }
    }.getOrNull()
}

/** "12, 3rd Cross, Jayanagar 4th Block, Bengaluru 560011" from the phone's own geocoder. */
suspend fun reverseGeocode(context: Context, lat: Double, lng: Double): String? {
    if (!Geocoder.isPresent()) return null
    val geocoder = Geocoder(context, Locale.forLanguageTag("en-IN"))
    val address: Address? = runCatching {
        if (Build.VERSION.SDK_INT >= 33) {
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) = cont.resume(addresses.firstOrNull())
                    override fun onError(errorMessage: String?) = cont.resume(null)
                })
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(lat, lng, 1)?.firstOrNull()
            }
        }
    }.getOrNull()
    return address?.let { a ->
        val line = a.getAddressLine(0)
        line?.removeSuffix(", India")?.removeSuffix(", Karnataka")
    }
}
