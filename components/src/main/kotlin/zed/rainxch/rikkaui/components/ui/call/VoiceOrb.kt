package zed.rainxch.rikkaui.components.ui.call

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import zed.rainxch.rikkaui.components.ui.glass.GlassCapability
import zed.rainxch.rikkaui.components.ui.glass.GlassLevel
import zed.rainxch.rikkaui.components.ui.glass.LocalGlassBackdrop
import zed.rainxch.rikkaui.components.ui.glass.LocalGlassCapability
import zed.rainxch.rikkaui.components.ui.glass.glassSurface
import zed.rainxch.rikkaui.components.ui.glass.rememberGlassStyle
import zed.rainxch.rikkaui.foundation.RikkaTheme
import kotlin.math.max

// ─── Model ──────────────────────────────────────────────────

/**
 * What the agent behind a [VoiceOrb] is currently doing.
 *
 * The orb never stops moving entirely — a frozen orb reads as a crashed agent —
 * but each state has its own tempo and reach.
 */
public enum class VoiceOrbState {
    /** Present but not engaged. Slow, shallow breathing. */
    Idle,

    /** Listening to the user. Amplitude drives the rings. */
    Listening,

    /** Speaking back. Faster breathing, brighter core. */
    Speaking,

    /** Working. Quick rotation, no amplitude response. */
    Thinking,
}

// ─── Component ──────────────────────────────────────────────

/**
 * A breathing sphere of glass that stands in for a voice agent's presence.
 *
 * Built from four stacked layers: an ambient glow that bleeds past the edge, a
 * refracting glass body that samples the same backdrop as the rest of the
 * screen, a coloured core, and a specular highlight that rotates independently
 * of the body — the cue that reads as a solid object turning rather than a
 * gradient cycling.
 *
 * ### Amplitude is a lambda on purpose
 * Microphone amplitude arrives at 30–60 Hz. Passing it as a `Float` parameter
 * would recompose the whole orb on every audio frame; passing it as `() -> Float`
 * means it is read in the draw phase only. The value is also smoothed with a
 * fast attack and slow release, so the orb tracks speech onsets sharply but
 * decays like a VU meter instead of flickering on every consonant.
 *
 * ### Motion budget
 * When [LocalGlassCapability] resolves to [GlassCapability.None] — battery
 * saver, a low-RAM device, or an explicit reduce-transparency override — the
 * infinite animations stop and the orb renders static. A perpetually animating
 * sphere is exactly the wrong thing to keep running on a device that just told
 * you it is trying to save power.
 *
 * ```
 * VoiceOrb(
 *     state = if (isListening) VoiceOrbState.Listening else VoiceOrbState.Idle,
 *     amplitude = { audioEngine.currentAmplitude },
 * )
 * ```
 *
 * @param modifier [Modifier] applied to the root Box.
 * @param state What the agent is doing; drives tempo, reach, and the spoken label.
 * @param amplitude Current input level in `0f..1f`, read once per frame.
 * @param size Diameter of the orb including its glow.
 * @param accent Colour of the core and rings — the agent's identity colour.
 * @param backdrop What shows through the glass body; from [LocalGlassBackdrop] by default.
 * @param label Accessibility description of what the agent is doing, announced
 *   politely so a state change is spoken without interrupting. Empty by default:
 *   an orb next to a status line is decoration, and describing it twice is
 *   noise. Pass a localized string when the orb is the only cue.
 */
@Composable
public fun VoiceOrb(
    modifier: Modifier = Modifier,
    state: VoiceOrbState = VoiceOrbState.Idle,
    amplitude: () -> Float = { 0f },
    size: Dp = 160.dp,
    accent: Color = RikkaTheme.colors.primary,
    backdrop: Backdrop = LocalGlassBackdrop.current,
    label: String = "",
) {
    val motion = RikkaTheme.motion
    val glass = RikkaTheme.glass
    val capability = LocalGlassCapability.current
    val animated = capability != GlassCapability.None

    val style = rememberGlassStyle(level = GlassLevel.Regular, tint = accent)

    val level = rememberSmoothedAmplitude(amplitude, active = state == VoiceOrbState.Listening)

    val transition = rememberInfiniteTransition(label = "voiceOrb")

    // Inhale is slower than exhale, which is what makes it read as breathing
    // rather than pulsing. RepeatMode.Reverse plus an eased tween gets close
    // enough without a second animation.
    val breathDuration = motion.durationPulse * state.breathTempo()
    val breath by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (animated) 1f else 0f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = breathDuration.toInt(), easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "breath",
    )

    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (animated) 360f else 0f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = state.spinDuration(), easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "spin",
    )

    Box(
        modifier =
            modifier
                .size(size)
                .semantics {
                    if (label.isNotEmpty()) {
                        contentDescription = label
                        liveRegion = LiveRegionMode.Polite
                    }
                },
    ) {
        // 1 — ambient glow, drawn past the body so presence bleeds into the scene.
        Canvas(Modifier.fillMaxSize()) {
            val reach = 0.5f + level.floatValue * 0.18f + breath * 0.04f
            drawCircle(
                brush =
                    Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.35f), Color.Transparent),
                        center = center,
                        radius = max(1f, this.size.minDimension * reach),
                    ),
            )
        }

        // 2 — the glass body itself, breathing through the backdrop layer so the
        // scenery behind stays anchored while the sphere swells.
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(size * 0.14f)
                    .glassSurface(
                        backdrop = backdrop,
                        style = style,
                        shape = CircleShape,
                        // Amplitude and breath both push the surface out, so a
                        // loud moment reads as the orb inflating, not flashing.
                        pressFraction = { level.floatValue * 0.5f },
                        layerBlock = {
                            val scale = 1f + breath * 0.05f + level.floatValue * 0.09f
                            scaleX = scale
                            scaleY = scale
                        },
                    ),
        )

        // 3 + 4 — core and the rotating specular, above the glass.
        Canvas(Modifier.fillMaxSize()) {
            val bodyRadius = this.size.minDimension * 0.36f * (1f + breath * 0.05f + level.floatValue * 0.09f)

            drawCircle(
                brush =
                    Brush.radialGradient(
                        colors =
                            listOf(
                                accent.copy(alpha = 0.55f + level.floatValue * 0.3f),
                                accent.copy(alpha = 0.05f),
                            ),
                        center = center + Offset(-bodyRadius * 0.25f, -bodyRadius * 0.3f),
                        radius = bodyRadius,
                    ),
                radius = bodyRadius,
            )

            // The specular turns on its own axis, lit from the material's light
            // angle so the orb agrees with every other glass surface on screen.
            rotate(degrees = spin + glass.lightAngle) {
                drawCircle(
                    brush =
                        Brush.sweepGradient(
                            colors =
                                listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.5f * glass.lightIntensity),
                                    Color.Transparent,
                                    Color.Transparent,
                                ),
                            center = center,
                        ),
                    radius = bodyRadius,
                    style = Stroke(width = bodyRadius * 0.14f),
                )
            }

            if (level.floatValue > 0.01f) drawAmplitudeRings(bodyRadius, level.floatValue, accent)
        }
    }
}

// ─── Internal ───────────────────────────────────────────────

private fun DrawScope.drawAmplitudeRings(
    bodyRadius: Float,
    level: Float,
    accent: Color,
) {
    // Two rings rather than one: a single expanding ring reads as a ripple
    // effect, two reads as sound leaving a source.
    repeat(2) { ring ->
        val spread = 1f + (ring + 1) * 0.22f * level
        drawCircle(
            color = accent.copy(alpha = (0.35f - ring * 0.15f) * level),
            radius = bodyRadius * spread,
            style = Stroke(width = bodyRadius * 0.03f),
        )
    }
}

/**
 * Smooths raw input amplitude with a fast attack and slow release, updating a
 * float state once per frame so readers stay in the draw phase.
 */
@Composable
private fun rememberSmoothedAmplitude(
    amplitude: () -> Float,
    active: Boolean,
): androidx.compose.runtime.MutableFloatState {
    val current by rememberUpdatedState(amplitude)
    val smoothed = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(active) {
        if (!active) {
            // Release to zero rather than snapping, so ending a turn decays.
            while (smoothed.floatValue > 0.001f) {
                withFrameNanos { }
                smoothed.floatValue *= 0.85f
            }
            smoothed.floatValue = 0f
            return@LaunchedEffect
        }
        while (true) {
            withFrameNanos { }
            val target = current().coerceIn(0f, 1f)
            val value = smoothed.floatValue
            val rate = if (target > value) 0.45f else 0.08f
            smoothed.floatValue = value + (target - value) * rate
        }
    }

    return smoothed
}

private fun VoiceOrbState.breathTempo(): Float =
    when (this) {
        VoiceOrbState.Idle -> 2.4f
        VoiceOrbState.Listening -> 1.6f
        VoiceOrbState.Speaking -> 1.0f
        VoiceOrbState.Thinking -> 1.2f
    }

private fun VoiceOrbState.spinDuration(): Int =
    when (this) {
        VoiceOrbState.Idle -> 12000
        VoiceOrbState.Listening -> 9000
        VoiceOrbState.Speaking -> 6000
        VoiceOrbState.Thinking -> 3000
    }
