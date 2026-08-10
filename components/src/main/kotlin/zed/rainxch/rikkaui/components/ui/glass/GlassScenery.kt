package zed.rainxch.rikkaui.components.ui.glass

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import zed.rainxch.rikkaui.foundation.RikkaScenery
import zed.rainxch.rikkaui.foundation.RikkaTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private const val DRIFT_PERIOD_MILLIS: Int = 42_000

/** Grain tile edge in cells. Large enough that the repeat is not perceptible. */
private const val GRAIN_TILE_CELLS: Int = 64

/**
 * Paints a backdrop worth refracting.
 *
 * This is the other half of the glass material. [Modifier.glassSurface] renders
 * a lens; this renders something for the lens to act on. Used together they
 * produce liquid glass, and used apart they produce a translucent panel over a
 * wash — which is the usual reason an implementation "doesn't look like the
 * Apple thing".
 *
 * It is the default background of [GlassContainer], so the shortest correct way
 * to get glass is to not pass a background at all.
 *
 * ```
 * GlassContainer(modifier = Modifier.fillMaxSize()) {
 *     GlassCard { Text("Refracts the scene behind it") }
 * }
 * ```
 *
 * Everything is drawn in the draw phase from a single `drawBehind`, and the
 * drift phase is read there too, so an animated scene costs no recomposition.
 *
 * @param modifier [Modifier] applied to the scene; normally `fillMaxSize`.
 * @param scenery The scene to paint; the theme's by default.
 * @param animated Whether the lobes drift. Off renders the scene at rest.
 */
@Composable
public fun GlassScenery(
    modifier: Modifier = Modifier,
    scenery: RikkaScenery = RikkaTheme.scenery,
    animated: Boolean = true,
) {
    val capability = LocalGlassCapability.current
    // A perpetually animating full-screen background is the wrong thing to keep
    // running on a device that has asked for power saving.
    val drifting = animated && capability != GlassCapability.None && scenery.driftFraction > 0f

    val transition = rememberInfiniteTransition(label = "scenery")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (drifting) 1f else 0f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = DRIFT_PERIOD_MILLIS, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "sceneryPhase",
    )

    val density = LocalDensity.current
    val grain =
        remember(scenery.grainAlpha, scenery.grainCell, density.density) {
            if (scenery.grainAlpha <= 0f) {
                null
            } else {
                val cellPx = max(1, (scenery.grainCell * density.density).toInt())
                grainBrush(cellPx)
            }
        }

    Box(
        modifier
            .fillMaxSize()
            .drawBehind { drawScenery(scenery, grain, phase) },
    )
}

/**
 * Draws [scenery] behind this node without needing a dedicated layout.
 *
 * Reach for it when the scene has to be part of an existing background — a list
 * that scrolls under glass chrome, say — rather than a sibling of the content.
 */
public fun Modifier.glassScenery(
    scenery: RikkaScenery,
    grain: ShaderBrush? = null,
    phase: () -> Float = { 0f },
): Modifier = drawBehind { drawScenery(scenery, grain, phase()) }

// ─── Drawing ────────────────────────────────────────────────

private fun DrawScope.drawScenery(
    scenery: RikkaScenery,
    grain: ShaderBrush?,
    phase: Float,
) {
    drawRect(Brush.verticalGradient(listOf(scenery.top, scenery.bottom)))

    val reach = size.maxDimension
    val drift = scenery.driftFraction * size.minDimension

    scenery.lobes.forEachIndexed { index, lobe ->
        // Each lobe travels its own small circle at its own phase offset. The
        // paths are incommensurate, so the scene never visibly repeats even
        // though every lobe is on the same clock.
        val angle = (phase * 2f * PI.toFloat()) + index * 1.31f
        val wobble = 1f + 0.35f * index
        val center =
            Offset(
                x = lobe.centerX * size.width + cos(angle) * drift * wobble,
                y = lobe.centerY * size.height + sin(angle * 0.73f) * drift * wobble,
            )
        val radius = lobe.radius * reach
        drawCircle(
            brush =
                Brush.radialGradient(
                    colors = listOf(lobe.color.copy(alpha = lobe.alpha), Color.Transparent),
                    center = center,
                    radius = radius,
                ),
            radius = radius,
            center = center,
        )
    }

    if (grain != null && scenery.grainAlpha > 0f) {
        drawRect(brush = grain, alpha = scenery.grainAlpha)
    }

    if (scenery.vignetteAlpha > 0f) {
        drawRect(
            brush =
                Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = scenery.vignetteAlpha)),
                    center = center,
                    radius = reach * 0.75f,
                ),
        )
    }
}

/**
 * Builds a tiled luminance grain.
 *
 * Generated once into a bitmap and repeated rather than drawn per pixel: this is
 * a full-screen effect that must not cost anything per frame. The values are
 * mid-grey around 50%, so the grain modulates whatever is under it instead of
 * lightening or darkening the scene on average.
 */
private fun grainBrush(cellPx: Int): ShaderBrush {
    val edge = GRAIN_TILE_CELLS * cellPx
    val random = java.util.Random(0x5EED)
    val pixels = IntArray(edge * edge)

    for (cellY in 0 until GRAIN_TILE_CELLS) {
        for (cellX in 0 until GRAIN_TILE_CELLS) {
            val value = 96 + random.nextInt(64)
            val argb = (0xFF shl 24) or (value shl 16) or (value shl 8) or value
            for (y in 0 until cellPx) {
                val row = (cellY * cellPx + y) * edge + cellX * cellPx
                for (x in 0 until cellPx) pixels[row + x] = argb
            }
        }
    }

    val bitmap: ImageBitmap =
        Bitmap.createBitmap(pixels, edge, edge, Bitmap.Config.ARGB_8888).asImageBitmap()
    return ShaderBrush(ImageShader(bitmap, TileMode.Repeated, TileMode.Repeated))
}
