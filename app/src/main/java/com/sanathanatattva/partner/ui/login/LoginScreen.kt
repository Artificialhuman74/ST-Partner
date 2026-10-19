package com.sanathanatattva.partner.ui.login

import android.app.Activity
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.appContainer
import com.sanathanatattva.partner.data.api.User
import com.sanathanatattva.partner.ui.components.Emblem
import com.sanathanatattva.partner.ui.components.PrimaryButton
import com.sanathanatattva.partner.ui.components.SecondaryButton
import com.sanathanatattva.partner.ui.components.StIcons
import com.sanathanatattva.partner.ui.components.StTextField
import com.sanathanatattva.partner.ui.components.fill
import com.sanathanatattva.partner.ui.components.kf
import com.sanathanatattva.partner.ui.components.svgPath
import com.sanathanatattva.partner.ui.components.viewBox
import com.sanathanatattva.partner.ui.theme.LocalReduceMotion
import com.sanathanatattva.partner.ui.theme.St
import kotlinx.coroutines.launch

/** Login (canvas "Login"): oil waves on deep green, a cream sheet with the form. */
@Composable
fun LoginScreen(onLoggedIn: (User) -> Unit, onJoin: (viaGoogle: Boolean) -> Unit) {
    val context = LocalContext.current
    val vm: LoginViewModel = viewModel { LoginViewModel(context.appContainer.auth) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var sheet by rememberSaveable { mutableStateOf(LoginSheet.None) }
    val resources = LocalResources.current

    LaunchedEffect(vm) { vm.loggedInEvents.collect(onLoggedIn) }
    LaunchedEffect(vm) {
        vm.noticeEvents.collect { notice ->
            snackbar.showSnackbar(
                when (notice) {
                    is UiText.Res -> resources.getString(notice.id)
                    is UiText.Raw -> notice.text
                },
            )
        }
    }
    LightStatusIcons(false)

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    BoxWithConstraints(Modifier.fillMaxSize().background(St.Green)) {
        // The canvas is 844dp tall; most phones are shorter once bars are drawn, so the
        // header tightens to keep the whole form (and "Join") on screen without scrolling.
        val compact = maxHeight < 830.dp
        val sheetTop = topInset + if (compact) 172.dp else 206.dp
        OilWaves(sheetTop, depth = if (compact) 0.62f else 1f, modifier = Modifier.fillMaxWidth().height(sheetTop + 24.dp))
        Column(
            Modifier.fillMaxWidth().padding(top = topInset + 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Emblem(
                if (compact) 56.dp else 68.dp,
                describe = true,
                modifier = Modifier
                    .shadow(10.dp, CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
                    .border(3.dp, St.Gold, CircleShape),
            )
            Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = if (compact) 28.sp else 32.sp, lineHeight = 1.em),
                color = St.Cream,
            )
            Spacer(Modifier.height(if (compact) 6.dp else 10.dp))
            Text(
                stringResource(R.string.login_tagline),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = if (compact) 16.sp else 17.sp),
                color = St.OnGreenMuted,
            )
        }

        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(top = sheetTop)
                .shadow(18.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), spotColor = St.GreenDeep)
                .background(St.Cream, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .imePadding(),
        ) {
            val sheetHeight = maxHeight
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                LoginForm(
                    vm = vm,
                    minHeight = sheetHeight,
                    compact = compact,
                    onForgot = { vm.clearReset(); sheet = LoginSheet.Forgot },
                    onGoogle = { vm.logInWithGoogle { context.appContainer.google.signIn(context) } },
                    onJoin = { onJoin(false) },
                )
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().imePadding().padding(16.dp))
    }

    when (sheet) {
        LoginSheet.Forgot -> ForgotPasswordSheet(vm, prefill = vm.email, onClose = { sheet = LoginSheet.None })
        LoginSheet.None -> Unit
    }
    vm.googleNoAccount?.let { email ->
        NoPartnerAccountSheet(
            email,
            onJoin = {
                vm.dismissGoogleNoAccount(keepGoogle = true)
                onJoin(true)
            },
            onClose = { vm.dismissGoogleNoAccount(keepGoogle = false) },
        )
    }
}

private enum class LoginSheet { None, Forgot }

@Composable
private fun LoginForm(
    vm: LoginViewModel,
    minHeight: Dp,
    compact: Boolean,
    onForgot: () -> Unit,
    onGoogle: () -> Unit,
    onJoin: () -> Unit,
) {
    val focus = LocalFocusManager.current
    var shown by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .padding(start = 20.dp, end = 20.dp, top = if (compact) 20.dp else 26.dp, bottom = if (compact) 4.dp else 12.dp)
            .navigationBarsPadding(),
    ) {
        Text(
            stringResource(R.string.login_title),
            style = MaterialTheme.typography.headlineMedium,
            color = St.Ink,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(if (compact) 10.dp else 16.dp))
        StTextField(
            value = vm.email,
            onValueChange = vm::onEmail,
            label = stringResource(R.string.login_email),
            icon = StIcons.Mail,
            error = vm.emailError?.let { text(it) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next, autoCorrectEnabled = false),
            keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
            contentType = ContentType.EmailAddress + ContentType.Username,
        )
        Spacer(Modifier.height(if (compact) 8.dp else 14.dp))
        StTextField(
            value = vm.password,
            onValueChange = vm::onPassword,
            label = stringResource(R.string.login_password),
            icon = StIcons.Lock,
            error = vm.passwordError?.let { text(it) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done, autoCorrectEnabled = false),
            keyboardActions = KeyboardActions(onDone = { focus.clearFocus(); vm.logIn() }),
            visualTransformation = if (shown) VisualTransformation.None else PasswordVisualTransformation(),
            contentType = ContentType.Password,
            labelEnd = {
                TextButton(onClick = onForgot, modifier = Modifier.heightIn(min = 44.dp)) {
                    Text(stringResource(R.string.login_forgot), style = MaterialTheme.typography.labelMedium.copy(fontSize = 16.sp), color = St.Green)
                }
            },
            trailing = {
                IconButton(onClick = { shown = !shown }, modifier = Modifier.size(48.dp)) {
                    Icon(
                        if (shown) StIcons.EyeOff else StIcons.Eye,
                        contentDescription = stringResource(if (shown) R.string.login_hide_password else R.string.login_show_password),
                        tint = St.Ink2,
                    )
                }
            },
        )
        vm.formError?.let {
            Text(
                text(it),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = St.Brick,
                modifier = Modifier.padding(top = 14.dp).semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        Spacer(Modifier.height(if (compact) 18.dp else 22.dp))
        PrimaryButton(
            text = stringResource(R.string.login_button),
            arrow = false,
            loading = vm.loading,
            onClick = { focus.clearFocus(); vm.logIn() },
        )
        Spacer(Modifier.height(if (compact) 10.dp else 14.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f).height(1.5.dp).background(St.Divider))
            Text(stringResource(R.string.login_or), style = MaterialTheme.typography.bodyMedium, color = St.Ink2)
            Box(Modifier.weight(1f).height(1.5.dp).background(St.Divider))
        }
        Spacer(Modifier.height(if (compact) 10.dp else 14.dp))
        SecondaryButton(
            text = stringResource(R.string.login_google),
            onClick = onGoogle,
            loading = vm.googleBusy,
            leading = { GoogleG(Modifier.size(22.dp)) },
        )
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(if (compact) 4.dp else 12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clickable(onClick = onJoin),
        ) {
            val newHere = stringResource(R.string.login_new_here)
            val join = stringResource(R.string.login_join)
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = St.Ink2)) { append(newHere) }
                    append(' ')
                    withStyle(SpanStyle(color = St.Green, fontWeight = FontWeight.Bold)) { append(join) }
                },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = 19.sp),
                modifier = Modifier.weight(1f, fill = false),
            )
            Icon(StIcons.ArrowRight, contentDescription = null, tint = St.Green, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
internal fun text(value: UiText): String = when (value) {
    is UiText.Res -> stringResource(value.id)
    is UiText.Raw -> value.text
}

/** Status-bar icon colour for screens whose top is dark green. Restored on leave. */
@Composable
internal fun LightStatusIcons(light: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(light) {
        val window = (view.context as? Activity)?.window ?: return@DisposableEffect onDispose { }
        val controller = WindowCompat.getInsetsController(window, view)
        val before = controller.isAppearanceLightStatusBars
        controller.isAppearanceLightStatusBars = light
        onDispose { controller.isAppearanceLightStatusBars = before }
    }
}

private object Waves {
    val back = svgPath("M0 34Q48.75 10 97.5 34T195 34T292.5 34T390 34T487.5 34T585 34T682.5 34T780 34V90H0Z")
    val middle = svgPath("M0 30Q65 6 130 30T260 30T390 30T520 30T650 30T780 30V90H0Z")
    val front = svgPath("M0 26Q48.75 4 97.5 26T195 26T292.5 26T390 26T487.5 26T585 26T682.5 26T780 26V90H0Z")

    /** x, y, diameter, seconds per rise, start offset in seconds (canvas values). */
    val bubbles = listOf(
        floatArrayOf(46f, 230f, 8f, 8f, -1f), floatArrayOf(98f, 236f, 5f, 11f, -6f),
        floatArrayOf(150f, 232f, 11f, 10f, -3f), floatArrayOf(236f, 236f, 6f, 9f, -7.5f),
        floatArrayOf(290f, 230f, 9f, 12f, -2f), floatArrayOf(338f, 238f, 5f, 8.5f, -5f),
        floatArrayOf(20f, 238f, 6f, 10.5f, -8.5f),
    )
}

/** Three layers of oil drifting at different speeds, with bubbles rising off the surface. */
@Composable
private fun OilWaves(sheetTop: Dp, depth: Float, modifier: Modifier = Modifier) {
    val seconds by rememberSeconds()
    val rise = CubicBezierEasing(0.33f, 0f, 0.4f, 1f)
    Canvas(modifier) {
        val unit = size.width / 390f
        // Design y values are measured from the sheet edge (250); [depth] < 1 tucks them closer.
        fun above(designY: Float) = (designY - 250f) * depth
        val edge = sheetTop.toPx()
        withTransform({
            translate(top = edge)
            scale(unit, unit, Offset.Zero)
        }) {
            fun drift(period: Float, leftward: Boolean): Float {
                val p = (seconds % period) / period
                return if (leftward) -390f * p else -390f + 390f * p
            }
            Waves.bubbles.forEach { (x, y, d, period, offset) ->
                val p = (((seconds - offset) % period) + period) % period / period
                val a = kf(p, 0f to 0f, 0.18f to 0.6f, 0.75f to 0.3f, 1f to 0f)
                val dy = -190f * depth * rise.transform(p)
                drawCircle(St.GoldPale, d / 2f, Offset(x + d / 2f, above(y) + d / 2f + dy), alpha = a)
            }
            translate(drift(22f, true), above(176f)) { fill(Waves.back, St.GoldLight, alpha = 0.28f) }
            translate(drift(14f, false), above(188f)) { fill(Waves.middle, St.GoldDeep) }
            translate(drift(9f, true), above(204f)) { fill(Waves.front, St.Gold) }
        }
    }
}

@Composable
private fun rememberSeconds(): State<Float> {
    val still = LocalReduceMotion.current
    return produceState(initialValue = 0f, still) {
        if (still) return@produceState
        val start = withFrameNanos { it }
        while (true) {
            withInfiniteAnimationFrameNanos { now -> value = (now - start) / 1_000_000_000f }
        }
    }
}

/** Google's four-colour "G", as required next to "Continue with Google". */
@Composable
private fun GoogleG(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        viewBox(48f, 48f) {
            fill(GooglePaths.red, Color(0xFFEA4335))
            fill(GooglePaths.blue, Color(0xFF4285F4))
            fill(GooglePaths.yellow, Color(0xFFFBBC05))
            fill(GooglePaths.green, Color(0xFF34A853))
        }
    }
}

private object GooglePaths {
    val red = svgPath("M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z")
    val blue = svgPath("M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z")
    val yellow = svgPath("M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z")
    val green = svgPath("M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z")
}
