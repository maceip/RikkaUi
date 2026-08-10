package zed.rainxch.rikkaui.foundation

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * One coloured lobe in a [RikkaScenery].
 *
 * Positions and radii are fractions of the scene, not absolute sizes, so a
 * scene composes identically on a phone and a tablet.
 *
 * @property color Lobe colour at its centre, fading to transparent at [radius].
 * @property centerX Horizontal centre, `0f` left to `1f` right. Values outside
 *   that range are legal and useful — a lobe anchored off-screen contributes
 *   only its steep flank, which is exactly what refraction feeds on.
 * @property centerY Vertical centre, `0f` top to `1f` bottom.
 * @property radius Reach, as a fraction of the scene's larger dimension.
 * @property alpha Peak opacity at the centre.
 */
@Immutable
public data class RikkaSceneryLobe(
    val color: Color,
    val centerX: Float,
    val centerY: Float,
    val radius: Float,
    val alpha: Float,
)

/**
 * A backdrop built to be looked *through*.
 *
 * ### Why this is a design system concern
 * Glass is a lens, and a lens over a flat field is invisible. Every cue the
 * material spends effort on needs structure behind it to act on:
 *
 * - **Blur** of a smooth gradient returns the same smooth gradient.
 * - **Refraction** displaces the sample point by `refractionAmount` — 12dp at
 *   [RikkaGlass.subtle], 44dp at [RikkaGlass.prominent]. If the backdrop does
 *   not change colour appreciably across that distance, the displaced sample is
 *   the colour that was already there and nothing reads.
 * - **Dispersion** splits an edge into RGB fringes. With no saturated edge there
 *   is nothing to split.
 *
 * A dark near-uniform wash — the obvious "tasteful" app background — starves all
 * three, and the result is a translucent panel with a rim rather than glass.
 * Shipping the material without shipping a scene worth refracting is what makes
 * liquid glass look fake.
 *
 * ### How the parts earn their place
 * [lobes] carry the load. They are deliberately saturated and deliberately
 * mid-sized: a lobe whose radius is a fraction of the screen produces a colour
 * gradient steep enough that a 26dp displacement lands somewhere visibly
 * different. Two or three enormous washes will not do it.
 *
 * [grainAlpha] adds high-frequency texture. Most of it is destroyed by any real
 * blur, which is the point — it survives only under [RikkaGlass.subtle], where
 * the blur is 4dp, and gives small controls like dial keys something to bend
 * that larger surfaces do not get.
 *
 * @property top Base wash colour at the top of the scene.
 * @property bottom Base wash colour at the bottom.
 * @property lobes Coloured lobes painted over the base, in order.
 * @property grainAlpha Opacity of the tiled luminance grain. `0f` disables it.
 * @property grainCell Size of one grain cell in density-independent pixels.
 *   Below about 2dp the grain is invisible on a high-density screen; above about
 *   6dp it reads as blotching rather than texture.
 * @property vignetteAlpha Opacity of the corner darkening that keeps the eye on
 *   the content. `0f` disables it.
 * @property driftFraction How far the lobes wander, as a fraction of the scene.
 *   Motion is what separates a scene from a wallpaper: a slowly breathing
 *   backdrop makes every glass surface over it shimmer without anything on
 *   screen appearing to move. `0f` disables it.
 */
@Immutable
public data class RikkaScenery(
    val top: Color,
    val bottom: Color,
    val lobes: List<RikkaSceneryLobe>,
    val grainAlpha: Float = 0.05f,
    val grainCell: Float = 3f,
    val vignetteAlpha: Float = 0.28f,
    val driftFraction: Float = 0.015f,
)

/**
 * The default scene for a light or dark palette.
 *
 * Tuned so that the steepest colour gradients sit where content usually does —
 * the middle band — rather than only at the corners, because that is where the
 * glass will be.
 */
public fun rikkaScenery(isDark: Boolean): RikkaScenery =
    if (isDark) {
        RikkaScenery(
            top = Color(0xFF141A2E),
            bottom = Color(0xFF07080F),
            lobes =
                listOf(
                    // Two large washes set the mood.
                    RikkaSceneryLobe(Color(0xFF6D5BFF), centerX = 0.88f, centerY = 0.04f, radius = 0.70f, alpha = 0.62f),
                    RikkaSceneryLobe(Color(0xFF00B3C8), centerX = 0.06f, centerY = 0.40f, radius = 0.62f, alpha = 0.46f),
                    // Four mid-sized lobes do the optical work: each one is small
                    // enough that a 26dp displacement crosses a real colour change.
                    RikkaSceneryLobe(Color(0xFFFF4E87), centerX = 0.30f, centerY = 0.80f, radius = 0.34f, alpha = 0.50f),
                    RikkaSceneryLobe(Color(0xFFFFA23A), centerX = 0.78f, centerY = 0.66f, radius = 0.28f, alpha = 0.40f),
                    RikkaSceneryLobe(Color(0xFF37E2A0), centerX = 0.62f, centerY = 0.30f, radius = 0.22f, alpha = 0.34f),
                    RikkaSceneryLobe(Color(0xFF8A5BFF), centerX = 0.14f, centerY = 0.62f, radius = 0.20f, alpha = 0.38f),
                ),
            grainAlpha = 0.055f,
            vignetteAlpha = 0.32f,
        )
    } else {
        RikkaScenery(
            top = Color(0xFFF2F5FF),
            bottom = Color(0xFFE4E8F5),
            lobes =
                listOf(
                    RikkaSceneryLobe(Color(0xFF7C6BFF), centerX = 0.88f, centerY = 0.04f, radius = 0.70f, alpha = 0.40f),
                    RikkaSceneryLobe(Color(0xFF00C2D8), centerX = 0.06f, centerY = 0.40f, radius = 0.62f, alpha = 0.32f),
                    RikkaSceneryLobe(Color(0xFFFF6E9C), centerX = 0.30f, centerY = 0.80f, radius = 0.34f, alpha = 0.34f),
                    RikkaSceneryLobe(Color(0xFFFFB65C), centerX = 0.78f, centerY = 0.66f, radius = 0.28f, alpha = 0.30f),
                    RikkaSceneryLobe(Color(0xFF3FD9A6), centerX = 0.62f, centerY = 0.30f, radius = 0.22f, alpha = 0.26f),
                    RikkaSceneryLobe(Color(0xFF9B7CFF), centerX = 0.14f, centerY = 0.62f, radius = 0.20f, alpha = 0.28f),
                ),
            grainAlpha = 0.04f,
            vignetteAlpha = 0.16f,
        )
    }

/** Derives the scene from a palette's background luminance, like [rikkaGlassFor]. */
public fun rikkaSceneryFor(colors: RikkaColors): RikkaScenery = rikkaScenery(isDark = colors.background.luminance() < 0.5f)

/**
 * Pre-built scenes.
 *
 * ```
 * RikkaTheme(scenery = RikkaSceneryPresets.ember(isDark = true)) { ... }
 * ```
 */
public object RikkaSceneryPresets {
    /** Warm oranges and reds. Reads as evening. */
    public fun ember(isDark: Boolean): RikkaScenery {
        val base = rikkaScenery(isDark)
        return base.copy(
            top = if (isDark) Color(0xFF2A1520) else Color(0xFFFFF1E8),
            bottom = if (isDark) Color(0xFF0D0709) else Color(0xFFF7E3D6),
            lobes =
                listOf(
                    base.lobes[0].copy(color = Color(0xFFFF6B3D)),
                    base.lobes[1].copy(color = Color(0xFFC2185B)),
                    base.lobes[2].copy(color = Color(0xFFFFB300)),
                    base.lobes[3].copy(color = Color(0xFFFF3D6E)),
                    base.lobes[4].copy(color = Color(0xFFFF8A3D)),
                    base.lobes[5].copy(color = Color(0xFFB23C6E)),
                ),
        )
    }

    /** Cool blues and greens. Reads as depth. */
    public fun deep(isDark: Boolean): RikkaScenery {
        val base = rikkaScenery(isDark)
        return base.copy(
            top = if (isDark) Color(0xFF0B1A2E) else Color(0xFFEAF3FF),
            bottom = if (isDark) Color(0xFF04070E) else Color(0xFFDCE9F7),
            lobes =
                listOf(
                    base.lobes[0].copy(color = Color(0xFF1E6BFF)),
                    base.lobes[1].copy(color = Color(0xFF00C2A8)),
                    base.lobes[2].copy(color = Color(0xFF0A9BE0)),
                    base.lobes[3].copy(color = Color(0xFF35D6C0)),
                    base.lobes[4].copy(color = Color(0xFF4B8CFF)),
                    base.lobes[5].copy(color = Color(0xFF0E7FA8)),
                ),
        )
    }

    /**
     * The base wash only, with no lobes and no grain.
     *
     * Here so that "I want a plain background" is a deliberate choice with a
     * name, rather than something an app arrives at by hand-rolling a gradient
     * and then wondering why its glass looks flat.
     */
    public fun flat(isDark: Boolean): RikkaScenery =
        rikkaScenery(isDark).copy(
            lobes = emptyList(),
            grainAlpha = 0f,
            driftFraction = 0f,
        )
}

public val LocalRikkaScenery: ProvidableCompositionLocal<RikkaScenery> =
    staticCompositionLocalOf { rikkaScenery(isDark = false) }
