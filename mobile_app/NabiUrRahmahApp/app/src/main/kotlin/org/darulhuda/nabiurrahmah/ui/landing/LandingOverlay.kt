package org.darulhuda.nabiurrahmah.ui.landing

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.darulhuda.nabiurrahmah.R
import org.darulhuda.nabiurrahmah.ui.common.ImmersiveSystemBars
import org.darulhuda.nabiurrahmah.ui.common.islamicPattern
import org.darulhuda.nabiurrahmah.ui.theme.NurColors
import org.darulhuda.nabiurrahmah.ui.theme.VerseTextStyle

private const val HOLD_MS = 2_600L

/**
 * Opens the app with the Durood: the emblem, then the salawat in Arabic and its
 * meaning, and the whole screen fades into the home screen. A tap skips it.
 */
@Composable
fun LandingOverlay(onFinished: () -> Unit) {
    val finish by rememberUpdatedState(onFinished)
    val emblem = remember { Animatable(0f) }
    val durood = remember { Animatable(0f) }
    val meaning = remember { Animatable(0f) }
    val screen = remember { Animatable(1f) }
    val skip = remember { kotlinx.coroutines.channels.Channel<Unit>(1) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { emblem.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
            launch { delay(350); durood.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
            launch { delay(1_000); meaning.animateTo(1f, tween(700)) }
            launch {
                kotlinx.coroutines.withTimeoutOrNull(HOLD_MS) { skip.receive() }
                screen.animateTo(0f, tween(550, easing = FastOutSlowInEasing))
                finish()
            }
        }
    }

    // Read only inside graphicsLayer, so the animation never recomposes or redraws the lattice.
    val shimmer = rememberInfiniteTransition(label = "landing").animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2_400, easing = LinearEasing), RepeatMode.Reverse),
        label = "pattern",
    )
    ImmersiveSystemBars(barsVisible = true)

    val duroodText = stringResource(R.string.durood_arabic)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = screen.value }
            .background(Brush.verticalGradient(listOf(NurColors.Maroon, NurColors.MaroonDeep)))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { skip.trySend(Unit) }
            .semantics { contentDescription = duroodText },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { alpha = shimmer.value }
                .islamicPattern(NurColors.Gold.copy(alpha = 0.14f), cell = 56.dp),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.logo_nabi_ur_rahmah),
                contentDescription = null,
                modifier = Modifier
                    .size(132.dp)
                    .graphicsLayer {
                        alpha = emblem.value
                        scaleX = 0.8f + 0.2f * emblem.value
                        scaleY = scaleX
                    }
                    .shadow(24.dp, CircleShape)
                    .clip(CircleShape),
            )
            Spacer(Modifier.height(40.dp))
            Text(
                text = duroodText,
                style = VerseTextStyle.copy(fontSize = 30.sp, lineHeight = 56.sp, textDirection = TextDirection.Rtl),
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    alpha = durood.value
                    translationY = (1 - durood.value) * 24.dp.toPx()
                },
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.durood_meaning),
                style = MaterialTheme.typography.bodyLarge,
                color = NurColors.GoldSoft,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { alpha = meaning.value },
            )
        }
    }
}
