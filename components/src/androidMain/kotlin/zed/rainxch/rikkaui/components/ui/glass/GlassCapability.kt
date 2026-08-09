package zed.rainxch.rikkaui.components.ui.glass

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

/**
 * How much of the glass material this device can actually draw.
 *
 * Glass is the one part of the design system whose *appearance* is negotiable at
 * runtime, so the negotiation is modelled explicitly rather than left to a
 * scatter of `SDK_INT` checks inside the renderer.
 *
 * - [Full] — colour grade, blur, and refraction. Android 13 (API 33) and up.
 * - [Blur] — colour grade and blur, no refracting edge. Android 12 (API 31–32).
 * - [None] — no backdrop sampling at all. The surface falls back to an opaque
 *   themed surface with a border, exactly like
 *   [zed.rainxch.rikkaui.components.ui.card.Card].
 *
 * ### Why [None] is opaque
 * The obvious fallback — keep the tint, drop the effects — is a trap. A dark
 * glass tint is around 14% white; over a photograph that is not a surface, it is
 * a smear, and any text on it is unreadable. Below API 31 there is no blur to
 * separate content from backdrop, so the only honest fallback is to stop
 * pretending and paint a real surface.
 */
public enum class GlassCapability {
    Full,
    Blur,
    None,
}

/**
 * The glass capability in force for this subtree.
 *
 * Defaults to what the OS version alone allows. [GlassContainer] narrows it
 * further using device and power state — see [rememberGlassCapability] — so
 * glass placed inside a container reacts to battery saver, while glass used
 * standalone still degrades correctly by API level.
 *
 * Provide it yourself to force a tier, which is the supported way to offer a
 * "reduce transparency" setting:
 *
 * ```
 * CompositionLocalProvider(LocalGlassCapability provides GlassCapability.None) {
 *     Settings()
 * }
 * ```
 */
public val LocalGlassCapability: ProvidableCompositionLocal<GlassCapability> =
    staticCompositionLocalOf { platformGlassCapability() }

/**
 * The best glass tier this OS version supports, ignoring device and power state.
 *
 * Cheap enough to call from a draw path — it reads nothing but `SDK_INT`.
 */
public fun platformGlassCapability(): GlassCapability =
    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> GlassCapability.Full
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> GlassCapability.Blur
        else -> GlassCapability.None
    }

/**
 * Resolves the glass tier for the current device, including state that can
 * change while the app runs.
 *
 * On top of [platformGlassCapability] this drops to [GlassCapability.None] when:
 *
 * - the device is low-RAM (`ActivityManager.isLowRamDevice`), where a
 *   full-screen blur is a frame-budget problem rather than a nicety;
 * - battery saver is on, which is tracked live — turning saver on visibly
 *   collapses glass to flat surfaces and turning it off restores them;
 * - the composable is being inspected in a preview, where `RenderEffect` does
 *   not run and glass would otherwise preview as an empty tint.
 *
 * [GlassContainer] calls this and publishes the result through
 * [LocalGlassCapability], so most code never calls it directly.
 */
@Composable
public fun rememberGlassCapability(): GlassCapability {
    if (LocalInspectionMode.current) return GlassCapability.None

    val context = LocalContext.current
    val platform = remember { platformGlassCapability() }

    val lowRam =
        remember(context) {
            context.getSystemService(ActivityManager::class.java)?.isLowRamDevice ?: false
        }
    if (platform == GlassCapability.None || lowRam) return GlassCapability.None

    val powerManager = remember(context) { context.getSystemService(PowerManager::class.java) }
    var powerSaving by remember(powerManager) { mutableStateOf(powerManager?.isPowerSaveMode == true) }

    DisposableEffect(context, powerManager) {
        if (powerManager == null) return@DisposableEffect onDispose {}

        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context?,
                    intent: Intent?,
                ) {
                    powerSaving = powerManager.isPowerSaveMode
                }
            }
        val filter = IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        // Protected system broadcast, so it must be registered as unexported on
        // API 34+ where context-registered receivers require an explicit flag.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        powerSaving = powerManager.isPowerSaveMode

        onDispose { context.unregisterReceiver(receiver) }
    }

    return if (powerSaving) GlassCapability.None else platform
}
