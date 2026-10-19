package com.sanathanatattva.partner.ui.intro

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.appContainer
import com.sanathanatattva.partner.data.Settings
import com.sanathanatattva.partner.ui.components.OilEase
import com.sanathanatattva.partner.ui.components.PrimaryButton
import com.sanathanatattva.partner.ui.components.StepDots
import com.sanathanatattva.partner.ui.components.VoiceToggle
import com.sanathanatattva.partner.ui.theme.St
import com.sanathanatattva.partner.voice.Narrator
import kotlinx.coroutines.launch

private data class IntroPage(
    @StringRes val title: Int,
    @StringRes val body: Int,
    val art: @Composable (Modifier) -> Unit,
)

private val pages = listOf(
    IntroPage(R.string.intro1_title, R.string.intro1_body) { RingingPhoneArt(it) },
    IntroPage(R.string.intro2_title, R.string.intro2_body) { PackPickupArt(it) },
    IntroPage(R.string.intro3_title, R.string.intro3_body) { CodePhotoArt(it) },
)

/** Three narrated, skippable intro steps (canvas Intro 1–3). */
@Composable
fun IntroScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val settingsStore = context.appContainer.settings
    val settings by settingsStore.settings.collectAsState(initial = Settings())
    val scope = rememberCoroutineScope()
    val pager = rememberPagerState { pages.size }
    val voiceTag = stringResource(R.string.narration_locale)
    val narrator = remember(voiceTag) { Narrator(context, voiceTag) }
    DisposableEffect(narrator) { onDispose { narrator.release() } }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { narrator.stop() }

    val page = pages[pager.settledPage]
    val spoken = stringResource(page.title) + ". " + stringResource(page.body)
    LaunchedEffect(spoken, settings.voiceOn) {
        if (settings.voiceOn) narrator.speak(spoken) else narrator.stop()
    }

    val finish: () -> Unit = {
        narrator.stop()
        scope.launch {
            settingsStore.setIntroSeen()
            onDone()
        }
    }
    BackHandler(enabled = pager.currentPage > 0) {
        scope.launch { pager.animateScrollToPage(pager.currentPage - 1, animationSpec = tween(600, easing = OilEase)) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(top = 12.dp, bottom = 20.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            VoiceToggle(on = settings.voiceOn, onChange = { on -> scope.launch { settingsStore.setVoiceOn(on) } })
            TextButton(onClick = finish, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.intro_skip), style = MaterialTheme.typography.titleMedium, color = St.Green)
            }
        }
        HorizontalPager(
            state = pager,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) { index ->
            val p = pages[index]
            Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(16.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 380.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(32.dp))
                        .background(St.Sand),
                ) {
                    p.art(Modifier.fillMaxSize())
                }
                Spacer(Modifier.height(28.dp))
                Text(
                    stringResource(p.title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = St.Ink,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(12.dp))
                Text(stringResource(p.body), style = MaterialTheme.typography.bodyLarge, color = St.Ink2)
                Spacer(Modifier.height(16.dp))
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            StepDots(
                count = pages.size,
                current = pager.currentPage,
                description = stringResource(R.string.intro_step, pager.currentPage + 1, pages.size),
            )
            val last = pager.currentPage == pages.lastIndex
            PrimaryButton(
                text = stringResource(if (last) R.string.intro_start else R.string.intro_next),
                onClick = {
                    if (last) finish()
                    else scope.launch { pager.animateScrollToPage(pager.currentPage + 1, animationSpec = tween(600, easing = OilEase)) }
                },
            )
        }
    }
}
