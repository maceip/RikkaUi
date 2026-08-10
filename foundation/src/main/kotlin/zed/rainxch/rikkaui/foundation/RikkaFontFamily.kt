package zed.rainxch.rikkaui.foundation

import androidx.annotation.FontRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/**
 * Creates a [FontFamily] from font resources with all required weights.
 *
 * This is the primary way to set up fonts. Provide your font files for each
 * weight and the design system handles the rest.
 *
 * Usage:
 * ```
 * val fontFamily = rememberRikkaFontFamily(
 *     light = R.font.inter_light,
 *     regular = R.font.inter_regular,
 *     medium = R.font.inter_medium,
 *     semiBold = R.font.inter_semibold,
 *     bold = R.font.inter_bold,
 *     extraBold = R.font.inter_extrabold,
 * )
 *
 * RikkaTheme(
 *     typography = rikkaTypography(fontFamily),
 * ) {
 *     // All Text and Button components now use Inter
 * }
 * ```
 *
 * @param light Font resource id for weight 300 (used in subtle/decorative text)
 * @param regular Font resource id for weight 400 (body text, paragraphs)
 * @param medium Font resource id for weight 500 (small text, labels)
 * @param semiBold Font resource id for weight 600 (headings h2-h4, large text)
 * @param bold Font resource id for weight 700 (emphasis, strong text)
 * @param extraBold Font resource id for weight 800 (h1 headings)
 */
@Composable
public fun rememberRikkaFontFamily(
    @FontRes light: Int,
    @FontRes regular: Int,
    @FontRes medium: Int,
    @FontRes semiBold: Int,
    @FontRes bold: Int,
    @FontRes extraBold: Int,
): FontFamily {
    val family =
        FontFamily(
            Font(light, FontWeight.Light),
            Font(regular, FontWeight.Normal),
            Font(medium, FontWeight.Medium),
            Font(semiBold, FontWeight.SemiBold),
            Font(bold, FontWeight.Bold),
            Font(extraBold, FontWeight.ExtraBold),
        )
    return remember(family) { family }
}
