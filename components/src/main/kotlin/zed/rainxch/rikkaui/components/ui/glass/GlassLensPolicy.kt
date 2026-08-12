package zed.rainxch.rikkaui.components.ui.glass

import android.os.SystemClock
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.delay

/**
 * How long a promoted lens stays [GlassCapability.Full] after the last request.
 *
 * Long enough for the finger to leave and the eye to register the edge bend;
 * short enough that an idle keypad does not keep RenderThread hot.
 */
public const val GlassLensHoldMs: Long = 700L

/**
 * Smart Full-tier lens budget: idle on [GlassCapability.Blur], promote to Full
 * on interaction, then demote after [holdMs].
 *
 * Live refraction is the signature look — and the thermal cost. This policy
 * keeps lenses for the moments that earn them (press, drag, a brief afterglow)
 * without running AGSL samplers while the dialer sits untouched.
 *
 * When the platform ceiling is already below Full (API 31–32, battery saver,
 * low-RAM), [promote] is a no-op and [effective] stays at the ceiling.
 */
@Stable
public class GlassLensPolicy internal constructor(
    private val ceiling: GlassCapability,
    private val holdMs: Long,
) {
    /** Elapsed-realtime deadline while Full is requested; 0 when idle on Blur. */
    internal var lensUntilElapsedMs by mutableLongStateOf(0L)
        private set

    /** Capability to publish through [LocalGlassCapability] / [GlassContainer]. */
    public val effective: GlassCapability
        get() {
            if (ceiling != GlassCapability.Full) return ceiling
            return if (SystemClock.elapsedRealtime() < lensUntilElapsedMs) {
                GlassCapability.Full
            } else {
                GlassCapability.Blur
            }
        }

    /**
     * Request Full-tier lenses until [holdMs] after now.
     *
     * Safe to call from press collectors; repeated calls extend the window.
     */
    public fun promote() {
        if (ceiling != GlassCapability.Full) return
        lensUntilElapsedMs = SystemClock.elapsedRealtime() + holdMs
    }

    /** Clears the promotion window once its deadline has passed. */
    internal fun clearIfExpired() {
        if (SystemClock.elapsedRealtime() >= lensUntilElapsedMs) {
            lensUntilElapsedMs = 0L
        }
    }
}

/**
 * Remembers a [GlassLensPolicy] bound to the device ceiling from
 * [rememberGlassCapability].
 *
 * When lenses are promoted, a coroutine sleeps until the hold expires and
 * clears the deadline so [GlassLensPolicy.effective] falls back to Blur and
 * recomposes the tree once — no per-frame polling.
 */
@Composable
public fun rememberGlassLensPolicy(
    ceiling: GlassCapability = rememberGlassCapability(),
    holdMs: Long = GlassLensHoldMs,
): GlassLensPolicy {
    val policy = remember(ceiling, holdMs) { GlassLensPolicy(ceiling, holdMs) }
    val until = policy.lensUntilElapsedMs
    LaunchedEffect(until, ceiling, holdMs) {
        if (ceiling != GlassCapability.Full || until == 0L) return@LaunchedEffect
        val remaining = until - SystemClock.elapsedRealtime()
        if (remaining > 0L) delay(remaining)
        // Deadline passed: bump state so readers recompose onto Blur.
        if (policy.lensUntilElapsedMs == until) {
            policy.clearIfExpired()
        }
    }
    return policy
}

/**
 * Promotes [LocalGlassLensPolicy] for as long as [interactionSource] is pressed,
 * and again on release so the afterglow outlives the finger.
 */
@Composable
public fun GlassLensPressPromoter(
    interactionSource: InteractionSource,
    enabled: Boolean = true,
) {
    val policy = LocalGlassLensPolicy.current ?: return
    val pressed by interactionSource.collectIsPressedAsState()
    // Gate the release afterglow so first composition (pressed=false) does not
    // promote lenses the moment a glass control enters the tree.
    var sawPress by remember { mutableStateOf(false) }
    LaunchedEffect(pressed, enabled) {
        if (!enabled) {
            sawPress = false
            return@LaunchedEffect
        }
        if (pressed) {
            sawPress = true
            // Refresh while held so a long press does not demote mid-gesture.
            while (true) {
                policy.promote()
                delay(GlassLensHoldMs / 2)
            }
        } else if (sawPress) {
            policy.promote()
            sawPress = false
        }
    }
}

/**
 * The active lens policy for this subtree, if any.
 *
 * Glass press handlers call [GlassLensPolicy.promote] when present so a press
 * on any glass control lights the scene's lenses for a beat.
 */
public val LocalGlassLensPolicy: ProvidableCompositionLocal<GlassLensPolicy?> =
    staticCompositionLocalOf { null }
