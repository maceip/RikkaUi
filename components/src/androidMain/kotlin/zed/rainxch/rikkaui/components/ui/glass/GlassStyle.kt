package zed.rainxch.rikkaui.components.ui.glass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import zed.rainxch.rikkaui.foundation.RikkaGlassLevel
import zed.rainxch.rikkaui.foundation.RikkaTheme

// ─── Level ──────────────────────────────────────────────────

/**
 * How far a glass surface reads as sitting above the content behind it.
 *
 * - [Subtle] — chips, dial keys, inline controls that sit *on* a surface.
 * - [Regular] — cards and action tiles that float just above it.
 * - [Prominent] — sheets, navigation bars, and dialogs that detach from it.
 */
public enum class GlassLevel {
    Subtle,
    Regular,
    Prominent,
    ;

    /** Clamps this level down [steps] rungs, bottoming out at [Subtle]. */
    internal fun stepDown(steps: Int): GlassLevel = entries[(ordinal - steps).coerceAtLeast(0)]

    /** Promotes this level [steps] rungs, topping out at [Prominent]. */
    internal fun stepUp(steps: Int = 1): GlassLevel = entries[(ordinal + steps).coerceAtMost(entries.lastIndex)]
}

// ─── Nesting ────────────────────────────────────────────────

/**
 * How many glass surfaces this content is already inside.
 *
 * Glass on glass is the material's one hard failure mode. Two slabs sampling the
 * same backdrop each blur and tint it independently, so the inner one neither
 * refracts the outer one nor sits on it — it just doubles the wash and the whole
 * stack goes milky.
 *
 * Every glass component publishes `depth + 1` to its children, and
 * [rememberGlassStyle] reads it back: a nested surface steps down a [GlassLevel]
 * per rung, halves its blur, drops its dispersion, and stops casting a drop
 * shadow onto the slab it is embedded in. Nesting therefore degrades into a
 * legible hierarchy instead of a smear, without anyone having to notice.
 *
 * [GlassContainer] resets this to zero — inside a container, content is over the
 * backdrop again rather than over glass.
 *
 * For genuinely layered compositions, prefer fixing the sampling rather than
 * relying on the clamp: pass `hostsGlass = true` to the outer surface so it
 * exports what it drew, and the nested surface refracts the parent's glass
 * instead of the raw scene behind it.
 */
public val LocalGlassDepth: ProvidableCompositionLocal<Int> = staticCompositionLocalOf { 0 }

// ─── Style ──────────────────────────────────────────────────

/**
 * A fully resolved glass material, ready to draw.
 *
 * This is the single value every glass component and [Modifier.glassSurface]
 * consumes. Everything that used to be decided inside each component — which
 * token bundle a [GlassLevel] maps to, what the nesting depth does to it, which
 * effects the device can run, what to paint when it cannot run any — is decided
 * once, here, by [rememberGlassStyle].
 *
 * @property tokens The depth tokens, already stepped down for nesting and
 *   adjusted for [capability].
 * @property tint Colour washed over the refracted backdrop; alpha comes from
 *   [tokens]. [Color.Unspecified] leaves the backdrop untinted.
 * @property shadowColor Drop shadow colour, alpha included.
 * @property innerShadowColor Inner shadow colour; alpha comes from [tokens].
 * @property lightAngle Direction the scene is lit from, in degrees.
 * @property lightIntensity Strength of the directional specular term.
 * @property lightFalloff How tightly the specular hot spot is focused.
 * @property pressRefraction Extra refraction while held, as a fraction of the
 *   level's own refraction amount.
 * @property pressHighlight Specular alpha added while held.
 * @property pressLightShift Degrees the specular sweeps while held.
 * @property capability What this device can actually draw.
 * @property fallbackColor Opaque surface painted when [capability] is
 *   [GlassCapability.None].
 * @property fallbackBorderColor Border painted alongside [fallbackColor].
 * @property fallbackElevation Shadow elevation used by the opaque fallback.
 */
@Immutable
public data class GlassStyle(
    val tokens: RikkaGlassLevel,
    val tint: Color,
    val shadowColor: Color,
    val innerShadowColor: Color,
    val lightAngle: Float,
    val lightIntensity: Float,
    val lightFalloff: Float,
    val pressRefraction: Float,
    val pressHighlight: Float,
    val pressLightShift: Float,
    val capability: GlassCapability,
    val fallbackColor: Color,
    val fallbackBorderColor: Color,
    val fallbackElevation: Dp,
)

/**
 * Resolves a [GlassLevel] into a drawable [GlassStyle] for the current theme,
 * nesting depth, and device capability.
 *
 * Call this when you are giving your own layout a glass background through
 * [Modifier.glassSurface]; the ready-made components call it for you.
 *
 * ```
 * val style = rememberGlassStyle(GlassLevel.Prominent)
 *
 * Row(Modifier.glassSurface(LocalGlassBackdrop.current, style, GlassDefaults.shape())) { ... }
 * ```
 *
 * @param level Requested depth, before nesting is taken into account.
 * @param tint Colour washed over the backdrop; the theme's glass tint by default.
 * @param capability Device tier; taken from [LocalGlassCapability] by default.
 * @param depth Nesting depth; taken from [LocalGlassDepth] by default.
 */
@Composable
public fun rememberGlassStyle(
    level: GlassLevel = GlassLevel.Regular,
    tint: Color = RikkaTheme.glass.tint,
    capability: GlassCapability = LocalGlassCapability.current,
    depth: Int = LocalGlassDepth.current,
): GlassStyle {
    val glass = RikkaTheme.glass
    val colors = RikkaTheme.colors
    val elevation = RikkaTheme.elevation

    return remember(glass, colors, elevation, level, tint, capability, depth) {
        val base =
            when (level.stepDown(depth)) {
                GlassLevel.Subtle -> glass.subtle
                GlassLevel.Regular -> glass.regular
                GlassLevel.Prominent -> glass.prominent
            }

        // Nested: subordinate to the slab it sits on. No drop shadow — a shadow
        // onto the very surface you are cut into reads as a seam. Dispersion
        // off, because fringing a fringe is mush.
        val nested =
            if (depth > 0) {
                base.copy(
                    blurRadius = base.blurRadius * 0.5f,
                    dispersion = false,
                    shadowRadius = 0.dp,
                )
            } else {
                base
            }

        // No refracting edge to define the boundary, so lean harder on the tint
        // and the rim to keep the surface readable as a shape.
        val tokens =
            if (capability == GlassCapability.Blur) {
                nested.copy(
                    tintAlpha = (nested.tintAlpha * 1.3f).coerceAtMost(0.6f),
                    highlightAlpha = (nested.highlightAlpha * 1.15f).coerceAtMost(1f),
                )
            } else {
                nested
            }

        GlassStyle(
            tokens = tokens,
            tint = tint,
            shadowColor = glass.shadowColor,
            innerShadowColor = glass.innerShadowColor,
            lightAngle = glass.lightAngle,
            lightIntensity = glass.lightIntensity,
            lightFalloff = glass.lightFalloff,
            pressRefraction = glass.pressRefraction,
            pressHighlight = glass.pressHighlight,
            pressLightShift = glass.pressLightShift,
            capability = capability,
            fallbackColor = colors.surface,
            fallbackBorderColor = colors.border,
            fallbackElevation = if (tokens.shadowRadius > 0.dp) elevation.medium else elevation.none,
        )
    }
}

// ─── Press ──────────────────────────────────────────────────

/**
 * The press response of a glass control, as two lambdas the renderer can read
 * from the draw and layer phases without recomposing.
 *
 * @property pressFraction `0f` at rest, `1f` fully held. Read from draw-phase
 *   lambdas to drive refraction and specular.
 * @property layerBlock Scale transform for `layerBlock` on [Modifier.glassSurface].
 */
@Stable
public class GlassPressState internal constructor(
    private val fraction: State<Float>,
    private val pressScale: Float,
) {
    public val pressFraction: () -> Float = { fraction.value }

    public val layerBlock: GraphicsLayerScope.() -> Unit = {
        val scale = lerp(1f, pressScale, fraction.value)
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Tracks presses on [interactionSource] and animates them into a [GlassPressState].
 *
 * Held glass does three things at once: it sinks, it refracts harder as the
 * "slab" compresses, and its specular sweeps as the light angle changes relative
 * to the deformed surface. All three run off this one fraction, so they stay in
 * step, and all three are read from the draw and layer phases — pressing a glass
 * button recomposes nothing.
 *
 * @param interactionSource Source to observe; the same one passed to `clickable`.
 * @param enabled Whether presses should register at all.
 */
@Composable
public fun rememberGlassPressState(
    interactionSource: InteractionSource,
    enabled: Boolean = true,
): GlassPressState {
    val motion = RikkaTheme.motion
    val isPressed by interactionSource.collectIsPressedAsState()

    val fraction =
        animateFloatAsState(
            targetValue = if (enabled && isPressed) 1f else 0f,
            animationSpec = motion.springSnap,
            label = "glassPress",
        )

    return remember(fraction, motion.pressScaleSubtle) { GlassPressState(fraction, motion.pressScaleSubtle) }
}

// ─── Defaults ───────────────────────────────────────────────

public object GlassDefaults {
    /**
     * The default glass shape.
     *
     * Glass shapes must be [CornerBasedShape] — the refraction shader derives
     * its edge from corner radii and has no way to trace an arbitrary outline.
     * Falls back to a plain rounded rectangle if the theme's `lg` shape is not
     * corner-based.
     */
    @Composable
    @ReadOnlyComposable
    public fun shape(): CornerBasedShape = RikkaTheme.shapes.lg as? CornerBasedShape ?: RoundedCornerShape(16.dp)

    /** Padding inside a [GlassSurface]; matches [zed.rainxch.rikkaui.components.ui.card.Card]. */
    @Composable
    @ReadOnlyComposable
    public fun contentPadding(): PaddingValues = PaddingValues(RikkaTheme.spacing.lg)

    /** Border width used by the opaque fallback when glass cannot be drawn. */
    public val FallbackBorderWidth: Dp = 1.dp
}
