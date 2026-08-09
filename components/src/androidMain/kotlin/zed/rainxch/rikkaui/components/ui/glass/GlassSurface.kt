package zed.rainxch.rikkaui.components.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import zed.rainxch.rikkaui.foundation.LocalContentColor
import zed.rainxch.rikkaui.foundation.LocalTextStyle
import zed.rainxch.rikkaui.foundation.RikkaTheme

private val AtRest: () -> Float = { 0f }

// ─── Modifier ───────────────────────────────────────────────

/**
 * Draws the liquid glass material behind this node.
 *
 * The contributions stack in the order light would meet them: the backdrop is
 * graded, blurred, and refracted; a tint washes over the result; then an inner
 * shadow gives the slab thickness and a specular rim plus drop shadow seat it in
 * space.
 *
 * This is the seam every glass component is built on. Reach for it directly when
 * you are giving an existing layout a glass background rather than composing one
 * of the ready-made components.
 *
 * ### Graceful degradation
 * The [style]'s [GlassCapability] decides what is drawn. At
 * [GlassCapability.Blur] the refraction is skipped and the tint and rim are
 * strengthened to compensate. At [GlassCapability.None] no backdrop is sampled
 * at all and the node paints an opaque themed surface with a border instead —
 * see [GlassCapability] for why that is the right fallback rather than a
 * tint-only ghost.
 *
 * ```
 * val style = rememberGlassStyle(GlassLevel.Regular)
 *
 * Row(
 *     Modifier
 *         .glassSurface(LocalGlassBackdrop.current, style, GlassDefaults.shape())
 *         .padding(16.dp),
 * ) { /* content */ }
 * ```
 *
 * @param backdrop What shows through the glass; see [LocalGlassBackdrop].
 * @param style Resolved material, normally from [rememberGlassStyle].
 * @param shape Surface outline. Must be corner-based — see [GlassDefaults.shape].
 * @param pressFraction Read at draw time to drive the press response: `0f` at
 *   rest, `1f` fully held. Supply [GlassPressState.pressFraction] for a control,
 *   or leave it for a static surface.
 * @param exportedBackdrop Records what this surface drew so nested glass can
 *   refract *it* rather than the scene behind it. See [LocalGlassDepth].
 * @param layerBlock Optional transform applied to the surface. Prefer this over
 *   a separate `Modifier.graphicsLayer` for press and drag animations: the
 *   backdrop sampling is inverted by the same transform, so the scenery stays
 *   put while the glass moves over it.
 */
public fun Modifier.glassSurface(
    backdrop: Backdrop,
    style: GlassStyle,
    shape: CornerBasedShape,
    pressFraction: () -> Float = AtRest,
    exportedBackdrop: LayerBackdrop? = null,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
): Modifier {
    val tokens = style.tokens

    if (style.capability == GlassCapability.None) {
        return this
            .then(if (layerBlock != null) Modifier.graphicsLayer(layerBlock) else Modifier)
            .shadow(style.fallbackElevation, shape, clip = false)
            .background(style.fallbackColor, shape)
            .border(GlassDefaults.FallbackBorderWidth, style.fallbackBorderColor, shape)
    }

    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            if (tokens.brightness != 0f || tokens.saturation != 1f) {
                colorControls(tokens.brightness, tokens.saturation)
            }
            if (tokens.vibrancy) vibrancy()
            if (tokens.blurRadius > 0.dp) blur(tokens.blurRadius.toPx())
            if (style.capability == GlassCapability.Full &&
                tokens.refractionHeight > 0.dp &&
                tokens.refractionAmount > 0.dp
            ) {
                // Held glass refracts harder — the slab reads as compressing
                // under the finger rather than merely shrinking.
                val amount = tokens.refractionAmount.toPx() * (1f + pressFraction() * style.pressRefraction)
                lens(tokens.refractionHeight.toPx(), amount, tokens.depthEffect, tokens.dispersion)
            }
        },
        highlight = {
            val press = pressFraction()
            Highlight(
                width = tokens.highlightWidth,
                blurRadius = tokens.highlightBlurRadius,
                alpha = (tokens.highlightAlpha + press * style.pressHighlight).coerceIn(0f, 1f),
                style =
                    HighlightStyle.Default(
                        intensity = style.lightIntensity,
                        // Sweeping the specular on press is what sells the
                        // deformation: a flat sheen that only dims reads as a
                        // dimmer, not as a surface bending toward the light.
                        angle = style.lightAngle + press * style.pressLightShift,
                        falloff = style.lightFalloff,
                    ),
            )
        },
        shadow = {
            if (tokens.shadowRadius > 0.dp) Shadow(radius = tokens.shadowRadius, color = style.shadowColor) else null
        },
        innerShadow = {
            if (tokens.innerShadowRadius > 0.dp && tokens.innerShadowAlpha > 0f) {
                InnerShadow(
                    radius = tokens.innerShadowRadius,
                    color = style.innerShadowColor,
                    alpha = tokens.innerShadowAlpha,
                )
            } else {
                null
            }
        },
        layerBlock = layerBlock,
        exportedBackdrop = exportedBackdrop,
        onDrawSurface = {
            if (style.tint.isSpecified && tokens.tintAlpha > 0f) {
                drawRect(style.tint.copy(alpha = tokens.tintAlpha))
            }
        },
    )
}

// ─── Component ──────────────────────────────────────────────

/**
 * A container filled with the liquid glass material.
 *
 * The glass counterpart to a plain surface: it refracts whatever
 * [LocalGlassBackdrop] holds instead of painting an opaque background. Provides
 * [LocalContentColor] and [LocalTextStyle] to its children the same way
 * [zed.rainxch.rikkaui.components.ui.card.Card] does.
 *
 * ```
 * GlassContainer(background = { Image(wallpaper, null) }) {
 *     GlassSurface(level = GlassLevel.Prominent) {
 *         Text("Reads through the wallpaper")
 *     }
 * }
 * ```
 *
 * @param modifier [Modifier] applied to the root Box.
 * @param level [GlassLevel] controlling blur, refraction, tint, and shadow depth.
 * @param shape Surface outline; must be corner-based.
 * @param backdrop What shows through; taken from [LocalGlassBackdrop] by default.
 * @param tint Colour washed over the refracted backdrop.
 * @param contentColor Colour provided to children through [LocalContentColor].
 * @param contentPadding Padding between the surface edge and its content.
 * @param contentAlignment Alignment of the content within the surface.
 * @param hostsGlass Whether this surface expects glass inside it. On, it records
 *   what it drew so nested glass refracts this surface instead of the scene
 *   behind it — correct, at the cost of one extra layer. Off, nested glass still
 *   degrades sanely through [LocalGlassDepth]; it just samples the same backdrop.
 * @param content [BoxScope] content lambda.
 */
@Composable
public fun GlassSurface(
    modifier: Modifier = Modifier,
    level: GlassLevel = GlassLevel.Regular,
    shape: CornerBasedShape = GlassDefaults.shape(),
    backdrop: Backdrop = LocalGlassBackdrop.current,
    tint: Color = RikkaTheme.glass.tint,
    contentColor: Color = RikkaTheme.colors.onSurface,
    contentPadding: PaddingValues = GlassDefaults.contentPadding(),
    contentAlignment: Alignment = Alignment.TopStart,
    hostsGlass: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val style = rememberGlassStyle(level = level, tint = tint)
    val exported = if (hostsGlass) rememberGlassBackdrop() else null

    GlassContentScope(contentColor = contentColor, nestedBackdrop = exported ?: backdrop) {
        Box(
            modifier =
                modifier
                    .glassSurface(
                        backdrop = backdrop,
                        style = style,
                        shape = shape,
                        exportedBackdrop = exported,
                    )
                    // Clip after the material so content cannot spill past the
                    // refracting edge that was just drawn for it.
                    .clip(shape)
                    .padding(contentPadding),
            contentAlignment = contentAlignment,
            content = content,
        )
    }
}

/**
 * Provides everything a glass surface owes its children: the content colour and
 * text style it paints them in, the nesting depth that keeps glass-in-glass from
 * compounding, and the backdrop nested glass should sample.
 *
 * Every glass component funnels through here, so a change to what "inside glass"
 * means lands in one place rather than five.
 */
@Composable
internal fun GlassContentScope(
    contentColor: Color,
    nestedBackdrop: Backdrop,
    textStyle: TextStyle = RikkaTheme.typography.p,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalContentColor provides contentColor,
        LocalTextStyle provides textStyle,
        LocalGlassDepth provides LocalGlassDepth.current + 1,
        LocalGlassBackdrop provides nestedBackdrop,
        content = content,
    )
}
