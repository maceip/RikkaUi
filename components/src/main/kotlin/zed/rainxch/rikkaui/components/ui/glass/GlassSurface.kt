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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpOffset
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val AtRest: () -> Float = { 0f }

private val DEGREES_TO_RADIANS: Float = (PI / 180.0).toFloat()

/** How far the dome's poles sit from the centre, as a fraction of the surface. */
private const val DOME_POLE_OFFSET: Float = 0.45f

/** Dome gradient reach, as a fraction of the surface's longest dimension. */
private const val DOME_RADIUS_SCALE: Float = 0.52f

/** Bevel lip reach from the lit/dark edge toward centre. */
private const val BEVEL_EDGE_OFFSET: Float = 0.55f

/** Bevel gradient radius as a fraction of the shorter surface side. */
private const val BEVEL_RADIUS_SCALE: Float = 0.55f

// ─── Modifier ───────────────────────────────────────────────

/**
 * Draws the liquid glass material behind this node.
 *
 * The contributions stack in the order light would meet them: the backdrop is
 * graded, blurred, and refracted; a tint washes over the result; a dome gradient
 * curves the face and a frosted band closes off the bottom edge; then an inner
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
            if (tokens.shadowRadius <= 0.dp) {
                null
            } else {
                val press = pressFraction()
                val radians = style.lightAngle * DEGREES_TO_RADIANS
                // Shadow falls away from the light — opposite the lit pole.
                val dist = tokens.shadowRadius * (0.22f * (1f + press * 0.3f))
                Shadow(
                    radius = tokens.shadowRadius,
                    offset = DpOffset(dist * cos(radians), dist * sin(radians)),
                    color = style.shadowColor,
                )
            }
        },
        innerShadow = {
            if (tokens.innerShadowRadius <= 0.dp || tokens.innerShadowAlpha <= 0f) {
                null
            } else {
                val press = pressFraction()
                val thicken = 1f + press * style.pressInnerShadow
                val radians = style.lightAngle * DEGREES_TO_RADIANS
                // Inset toward the light so the shaded lip reads as thickness.
                val bevel =
                    maxOf(tokens.bevelWidth, tokens.innerShadowRadius * 0.35f) * 0.5f
                InnerShadow(
                    radius = tokens.innerShadowRadius * thicken,
                    offset = DpOffset(-bevel * cos(radians), -bevel * sin(radians)),
                    color = style.innerShadowColor,
                    alpha = (tokens.innerShadowAlpha * thicken).coerceAtMost(0.55f),
                )
            }
        },
        layerBlock = layerBlock,
        exportedBackdrop = exportedBackdrop,
        onDrawSurface = {
            if (style.tint.isSpecified && tokens.tintAlpha > 0f) {
                drawRect(style.tint.copy(alpha = tokens.tintAlpha))
            }

            val radians = style.lightAngle * DEGREES_TO_RADIANS
            // +x is right and +y is DOWN in DrawScope, while lightAngle is
            // measured counter-clockwise from +x in scene space; negating
            // both puts the lit pole up-left at the default 45f.
            val dx = -cos(radians)
            val dy = -sin(radians)

            // Edge hills. Survives at Blur when the AGSL lens cannot run: a lit
            // lip and a shaded lip aimed by lightAngle, so trays and keys still
            // read as molasses globes rather than frosted acrylic cards.
            if (tokens.bevelWidth > 0.dp &&
                (tokens.bevelLightAlpha > 0f || tokens.bevelShadowAlpha > 0f)
            ) {
                val litEdge =
                    Offset(
                        size.width * (0.5f + dx * BEVEL_EDGE_OFFSET),
                        size.height * (0.5f + dy * BEVEL_EDGE_OFFSET),
                    )
                val darkEdge =
                    Offset(
                        size.width * (0.5f - dx * BEVEL_EDGE_OFFSET),
                        size.height * (0.5f - dy * BEVEL_EDGE_OFFSET),
                    )
                val bevelRadius = size.minDimension * BEVEL_RADIUS_SCALE
                if (tokens.bevelLightAlpha > 0f) {
                    drawRect(
                        Brush.radialGradient(
                            0f to Color.White.copy(alpha = tokens.bevelLightAlpha),
                            0.35f to Color.White.copy(alpha = tokens.bevelLightAlpha * 0.28f),
                            0.6f to Color.Transparent,
                            center = litEdge,
                            radius = bevelRadius,
                        ),
                    )
                }
                if (tokens.bevelShadowAlpha > 0f) {
                    drawRect(
                        Brush.radialGradient(
                            0f to Color.Black.copy(alpha = tokens.bevelShadowAlpha),
                            0.45f to Color.Transparent,
                            center = darkEdge,
                            radius = bevelRadius,
                        ),
                    )
                }
            }

            // Dome. Poles pushed out and the radius tightened so the ramp
            // accelerates toward the silhouette — a cap, not a diagonal sheen.
            if (tokens.domeStrength > 0f) {
                val litCenter =
                    Offset(
                        size.width * (0.5f + dx * DOME_POLE_OFFSET),
                        size.height * (0.5f + dy * DOME_POLE_OFFSET),
                    )
                val darkCenter =
                    Offset(
                        size.width * (0.5f - dx * DOME_POLE_OFFSET),
                        size.height * (0.5f - dy * DOME_POLE_OFFSET),
                    )
                val domeRadius = size.maxDimension * DOME_RADIUS_SCALE
                drawRect(
                    Brush.radialGradient(
                        0f to Color.White.copy(alpha = tokens.domeStrength),
                        0.45f to Color.White.copy(alpha = tokens.domeStrength * 0.22f),
                        0.85f to Color.Transparent,
                        1f to Color.Transparent,
                        center = litCenter,
                        radius = domeRadius,
                    ),
                )
                drawRect(
                    Brush.radialGradient(
                        0f to Color.Black.copy(alpha = tokens.domeStrength * 0.55f),
                        0.7f to Color.Transparent,
                        1f to Color.Transparent,
                        center = darkCenter,
                        radius = domeRadius,
                    ),
                )
            }

            // Frost. Drawn in the material's own tint, not in white: plain glass
            // frosts white because the default tint is white, and a green-tinted
            // surface frosts green without anyone passing a second colour.
            if (tokens.frostAlpha > 0f && tokens.frostFraction > 0f && style.tint.isSpecified) {
                val bandHeight = size.height * tokens.frostFraction
                val bandTop = size.height - bandHeight
                drawRect(
                    brush =
                        Brush.verticalGradient(
                            0f to style.tint.copy(alpha = 0f),
                            1f to style.tint.copy(alpha = tokens.frostAlpha),
                            startY = bandTop,
                            endY = size.height,
                        ),
                    topLeft = Offset(0f, bandTop),
                    size = Size(size.width, bandHeight),
                )
            }
        },
    )
}

/**
 * Memoised [glassSurface] for composable call sites.
 *
 * [Modifier.glassSurface] builds fresh effect lambdas on every call, and the
 * underlying `drawBackdrop` element compares those by identity — so any
 * recomposition that re-evaluates the modifier chain rebuilds native
 * `RenderEffect`s. Remembering the element keeps the glass graph alive across
 * parent recompositions when [backdrop], [style], [shape], and the press
 * lambdas are stable (as they are when produced by [rememberGlassStyle] /
 * [rememberGlassPressState]).
 */
@Composable
public fun Modifier.rememberGlassSurface(
    backdrop: Backdrop,
    style: GlassStyle,
    shape: CornerBasedShape,
    pressFraction: () -> Float = AtRest,
    exportedBackdrop: LayerBackdrop? = null,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
): Modifier =
    this.then(
        remember(backdrop, style, shape, pressFraction, exportedBackdrop, layerBlock) {
            Modifier.glassSurface(
                backdrop = backdrop,
                style = style,
                shape = shape,
                pressFraction = pressFraction,
                exportedBackdrop = exportedBackdrop,
                layerBlock = layerBlock,
            )
        },
    )

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
 * @param treatment Use [GlassTreatment.Smoked] for a colour-dense surface that
 *   still samples and refracts its backdrop.
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
    treatment: GlassTreatment = GlassTreatment.Clear,
    contentColor: Color = RikkaTheme.colors.onSurface,
    contentPadding: PaddingValues = GlassDefaults.contentPadding(),
    contentAlignment: Alignment = Alignment.TopStart,
    hostsGlass: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val style = rememberGlassStyle(level = level, tint = tint, treatment = treatment)
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
