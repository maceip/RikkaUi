package zed.rainxch.rikkaui.foundation

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A single depth level of the liquid glass material.
 *
 * A glass surface is built from six stacked contributions, all of which scale
 * together as the surface moves "closer" to the viewer:
 *
 * 1. **Colour grade** — vibrancy, saturation, and brightness applied to the
 *    backdrop *before* anything else. This is what buys legibility: darkening a
 *    bright backdrop under dark glass is cheaper and cleaner than piling on tint.
 * 2. **Blur** — how much the graded backdrop is softened.
 * 3. **Refraction** — the lens distortion applied at the surface edge, which is
 *    what makes glass read as a physical slab rather than a translucent panel.
 * 4. **Tint** — the flat wash of colour laid over the refracted backdrop.
 * 5. **Modelling** — the dome gradient that curves the otherwise flat face, and
 *    the frosted band along the bottom edge that closes the surface off. Both
 *    are painted in the material's own tint and lit from its own angle, so they
 *    read as shape rather than as decoration.
 * 6. **Highlight / shadow** — the specular rim, the inner shadow that gives the
 *    slab thickness, and the drop shadow that seats it in space.
 *
 * @property blurRadius Backdrop blur radius. Requires Android 12 (API 31); on
 *   older versions the blur is skipped and only the tint remains.
 * @property refractionHeight Depth of the refracting edge band. Requires Android
 *   13 (API 33) for the AGSL shader; skipped below that.
 * @property refractionAmount Strength of the edge lens distortion.
 * @property depthEffect Whether the refraction gradient is biased radially. On
 *   it reads as a domed lens, off as a flat pane. Cheap — it is one extra term
 *   in the same shader, not a second pass.
 * @property dispersion Whether the refracted edge splits into RGB fringes, the
 *   way a real prism does. This is the signature liquid-glass cue, and the most
 *   expensive one: it runs a wider shader. Reserve it for large surfaces, where
 *   the fringe has room to read as colour rather than as noise.
 * @property tintAlpha Opacity of [RikkaGlass.tint] over the refracted backdrop.
 * @property saturation Saturation multiplier applied to the backdrop. `1f` is
 *   neutral; above it, colours behind the glass bloom.
 * @property brightness Brightness offset applied to the backdrop, in `-1f..1f`.
 *   Negative darkens — use it under light content, positive under dark content.
 * @property vibrancy Whether the tuned vibrancy grade is applied on top of
 *   [saturation] and [brightness].
 * @property highlightAlpha Opacity of the specular rim highlight.
 * @property highlightWidth Thickness of the specular rim.
 * @property highlightBlurRadius Softness of the specular rim. Sharp rims read as
 *   thin and hard, soft rims as thick and molten.
 * @property innerShadowRadius Radius of the shadow cast *inside* the surface.
 *   This is what gives the slab thickness — without it glass reads as a decal.
 * @property innerShadowAlpha Opacity of the inner shadow.
 * @property shadowRadius Drop shadow radius cast by the glass slab.
 * @property domeStrength Strength of the convex-lens shading painted over the
 *   refracted backdrop: a specular bloom on the lit side and a matching falloff
 *   opposite it. Refraction alone only bends the *edge*, so the middle of a
 *   surface stays optically flat and the slab reads as a cut-out pane. This
 *   gradient pair is what curves the face, turning the slab into a domed cap
 *   that sits up off the content behind it. It is aimed by
 *   [RikkaGlass.lightAngle], not by its own direction, so a dome, a rim, and a
 *   drop shadow all agree about where the light is. `0f` disables it.
 * @property frostFraction Share of the surface height, measured up from the
 *   bottom edge, that carries the frosted wash. The bottom of a card is where
 *   content thins out and the backdrop shows through hardest; frosting that band
 *   gives text a floor to sit on and closes the surface off instead of letting
 *   it fade into the scenery.
 * @property frostAlpha Peak opacity of the frosted band at the very bottom edge,
 *   fading to nothing at the top of the band. The wash is drawn in
 *   [RikkaGlass.tint], so it frosts white on plain glass and green on green
 *   glass — the frost belongs to the material, not to a fixed colour. `0f`
 *   disables it.
 */
@Immutable
public data class RikkaGlassLevel(
    val blurRadius: Dp,
    val refractionHeight: Dp,
    val refractionAmount: Dp,
    val depthEffect: Boolean = false,
    val dispersion: Boolean = false,
    val tintAlpha: Float,
    val saturation: Float = 1f,
    val brightness: Float = 0f,
    val vibrancy: Boolean = true,
    val highlightAlpha: Float,
    val highlightWidth: Dp = 0.5.dp,
    val highlightBlurRadius: Dp = 0.25.dp,
    val innerShadowRadius: Dp = 0.dp,
    val innerShadowAlpha: Float = 0f,
    val shadowRadius: Dp,
    val domeStrength: Float = 0f,
    val frostFraction: Float = 0.1f,
    val frostAlpha: Float = 0f,
)

/**
 * RikkaGlass defines the liquid glass material scale for the design system.
 *
 * Three levels from barely-there to fully detached, mirroring the way
 * [RikkaElevation] scales shadows:
 *
 * - [subtle] — chips, dial keys, inline controls that sit *on* a surface.
 * - [regular] — cards and action tiles that float just above it.
 * - [prominent] — sheets, navigation bars, and dialogs that detach from it.
 *
 * ### One light source
 * [lightAngle] and [lightFalloff] are properties of the *material*, not of a
 * level, because a scene lit from two directions stops reading as a scene. Every
 * glass surface in the app takes its specular from the same angle, so a stack of
 * them reads as one lit object rather than a pile of stickers.
 *
 * ### Platform support
 * The glass components that consume these tokens are Android-only, because the
 * underlying refraction shaders are. The tokens themselves are plain data and
 * live in common code so a multiplatform implementation can adopt them later.
 *
 * ### Customization
 * ```
 * RikkaTheme(
 *     glass = rikkaGlass(isDark = true).copy(
 *         tint = Color(0xFF0B1020),
 *         lightAngle = 315f,
 *     ),
 * ) { ... }
 * ```
 *
 * @property tint The colour washed over the refracted backdrop. Alpha comes from
 *   the level's `tintAlpha`, so this should be supplied fully opaque.
 * @property shadowColor Colour of the drop shadow cast by a glass surface,
 *   including its alpha — glass shadows are far softer than opaque ones.
 * @property innerShadowColor Colour of the shadow cast inside the surface. Alpha
 *   comes from the level's `innerShadowAlpha`.
 * @property lightAngle Direction the scene is lit from, in degrees clockwise
 *   from the +x axis. The default `45f` puts the light above and to the left,
 *   which is where the drop shadow's downward offset already implies it is.
 * @property lightIntensity Strength of the directional term in the specular rim.
 * @property lightFalloff How quickly the specular decays away from [lightAngle].
 *   Higher values give a tighter, glossier hot spot.
 * @property pressRefraction Extra refraction, as a fraction of the level's own
 *   `refractionAmount`, applied while a glass control is held. This is the
 *   "liquid" in liquid glass: the slab visibly squeezes under the finger.
 * @property pressHighlight Specular alpha added while a glass control is held.
 * @property pressLightShift Degrees the specular sweeps while a control is held.
 */
@Immutable
public data class RikkaGlass(
    val tint: Color,
    val shadowColor: Color,
    val innerShadowColor: Color = Color.Black,
    val lightAngle: Float = 45f,
    val lightIntensity: Float = 0.5f,
    val lightFalloff: Float = 1f,
    val pressRefraction: Float = 0.6f,
    val pressHighlight: Float = 0.2f,
    val pressLightShift: Float = 30f,
    val subtle: RikkaGlassLevel,
    val regular: RikkaGlassLevel,
    val prominent: RikkaGlassLevel,
)

/**
 * Builds the default glass material for a light or dark palette.
 *
 * Dark glass leans on a stronger rim highlight and frost, because a dark
 * blurred backdrop gives the edge far less contrast to work with. Legibility
 * comes from tint and frost — not from darkening the backdrop. A negative
 * brightness grade flattens exactly the colour variance a 12–44dp lens needs
 * to read as refraction when the scene is [RikkaScenery].
 *
 * Light glass keeps a small positive brightness so content stays lifted over
 * pale washes without crushing chroma.
 *
 * @param isDark Whether the surrounding palette is dark.
 */
public fun rikkaGlass(isDark: Boolean): RikkaGlass =
    RikkaGlass(
        // White in both modes: the wash lifts a dark backdrop and frosts a light
        // one. What changes between modes is the alpha, which lives on the level.
        tint = Color.White,
        shadowColor = Color.Black.copy(alpha = if (isDark) 0.24f else 0.12f),
        innerShadowColor = if (isDark) Color.Black else Color(0xFF334155),
        subtle =
            RikkaGlassLevel(
                blurRadius = 4.dp,
                refractionHeight = 8.dp,
                refractionAmount = 12.dp,
                // A chip is too small for a fringe to read as anything but noise.
                depthEffect = false,
                dispersion = false,
                tintAlpha = if (isDark) 0.10f else 0.22f,
                saturation = 1.2f,
                // Dark: neutral so scenery chroma survives the grade. Light: a
                // slight lift over pale washes.
                brightness = if (isDark) 0f else 0.02f,
                highlightAlpha = if (isDark) 0.30f else 0.45f,
                highlightWidth = 0.5.dp,
                highlightBlurRadius = 0.25.dp,
                innerShadowRadius = 3.dp,
                innerShadowAlpha = if (isDark) 0.22f else 0.14f,
                // Small, but not absent: a dial key with no drop shadow at all
                // is painted onto the surface rather than resting on it.
                shadowRadius = 6.dp,
                domeStrength = if (isDark) 0.10f else 0.09f,
                frostFraction = 0.1f,
                frostAlpha = if (isDark) 0.14f else 0.17f,
            ),
        regular =
            RikkaGlassLevel(
                blurRadius = 12.dp,
                refractionHeight = 14.dp,
                refractionAmount = 26.dp,
                depthEffect = true,
                dispersion = true,
                tintAlpha = if (isDark) 0.14f else 0.30f,
                saturation = 1.5f,
                brightness = if (isDark) 0f else 0.04f,
                highlightAlpha = if (isDark) 0.45f else 0.60f,
                highlightWidth = 0.75.dp,
                highlightBlurRadius = 0.5.dp,
                innerShadowRadius = 9.dp,
                innerShadowAlpha = if (isDark) 0.28f else 0.18f,
                shadowRadius = 24.dp,
                domeStrength = if (isDark) 0.13f else 0.11f,
                frostFraction = 0.1f,
                frostAlpha = if (isDark) 0.20f else 0.24f,
            ),
        prominent =
            RikkaGlassLevel(
                blurRadius = 24.dp,
                refractionHeight = 22.dp,
                refractionAmount = 44.dp,
                depthEffect = true,
                dispersion = true,
                tintAlpha = if (isDark) 0.18f else 0.38f,
                saturation = 1.8f,
                brightness = if (isDark) 0f else 0.06f,
                highlightAlpha = if (isDark) 0.55f else 0.72f,
                highlightWidth = 1.dp,
                highlightBlurRadius = 0.75.dp,
                innerShadowRadius = 14.dp,
                innerShadowAlpha = if (isDark) 0.32f else 0.20f,
                shadowRadius = 48.dp,
                domeStrength = if (isDark) 0.16f else 0.14f,
                frostFraction = 0.1f,
                frostAlpha = if (isDark) 0.26f else 0.30f,
            ),
    )

/**
 * Derives the glass material from a palette by inspecting its background
 * luminance, so [RikkaTheme] picks the right variant without being told twice
 * whether the app is in dark mode.
 */
public fun rikkaGlassFor(colors: RikkaColors): RikkaGlass = rikkaGlass(isDark = colors.background.luminance() < 0.5f)

/**
 * Pre-built glass materials for common looks.
 *
 * ```
 * RikkaTheme(glass = RikkaGlassPresets.crystal(isDark = true)) { ... }
 * ```
 */
public object RikkaGlassPresets {
    /**
     * Heavier blur, more tint, no dispersion. The safe, legible default for
     * content-dense chrome sitting over busy or unpredictable backdrops.
     */
    public fun frosted(isDark: Boolean): RikkaGlass {
        val base = rikkaGlass(isDark)
        return base.copy(
            subtle = base.subtle.frost(isDark),
            regular = base.regular.frost(isDark),
            prominent = base.prominent.frost(isDark),
        )
    }

    /**
     * Minimal blur, maximum refraction and dispersion. The showpiece: reads as a
     * thick, optically active slab. Wants a rich backdrop and room to breathe.
     */
    public fun crystal(isDark: Boolean): RikkaGlass {
        val base = rikkaGlass(isDark)
        return base.copy(
            lightFalloff = 2f,
            pressRefraction = 0.9f,
            subtle = base.subtle.crystallize(),
            regular = base.regular.crystallize(),
            prominent = base.prominent.crystallize(),
        )
    }

    /**
     * Tint and rim only — no refraction, no dispersion, no inner shadow. Costs
     * roughly what a translucent panel costs, and behaves identically on every
     * API level. Good for low-end device tiers and for UIs where glass is an
     * accent rather than the point.
     */
    public fun flat(isDark: Boolean): RikkaGlass {
        val base = rikkaGlass(isDark)
        return base.copy(
            subtle = base.subtle.flatten(),
            regular = base.regular.flatten(),
            prominent = base.prominent.flatten(),
        )
    }

    private fun RikkaGlassLevel.frost(isDark: Boolean): RikkaGlassLevel =
        copy(
            blurRadius = blurRadius * 1.75f,
            dispersion = false,
            tintAlpha = (tintAlpha * 1.35f).coerceAtMost(if (isDark) 0.4f else 0.6f),
            saturation = 1f + (saturation - 1f) * 0.5f,
        )

    private fun RikkaGlassLevel.crystallize(): RikkaGlassLevel =
        copy(
            blurRadius = blurRadius * 0.4f,
            refractionHeight = refractionHeight * 1.4f,
            refractionAmount = refractionAmount * 1.5f,
            depthEffect = true,
            dispersion = true,
            tintAlpha = tintAlpha * 0.6f,
            highlightAlpha = (highlightAlpha * 1.2f).coerceAtMost(1f),
        )

    private fun RikkaGlassLevel.flatten(): RikkaGlassLevel =
        copy(
            refractionAmount = 0.dp,
            depthEffect = false,
            dispersion = false,
            innerShadowRadius = 0.dp,
            innerShadowAlpha = 0f,
        )
}

public val LocalRikkaGlass: ProvidableCompositionLocal<RikkaGlass> =
    staticCompositionLocalOf { rikkaGlass(isDark = false) }
