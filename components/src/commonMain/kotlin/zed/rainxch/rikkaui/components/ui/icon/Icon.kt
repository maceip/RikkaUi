package zed.rainxch.rikkaui.components.ui.icon

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import zed.rainxch.rikkaicons.core.AppIcon
import zed.rainxch.rikkaicons.core.DecorativeAppIcon
import zed.rainxch.rikkaicons.core.IconToken
import zed.rainxch.rikkaui.foundation.LocalContentColor
import zed.rainxch.rikkaui.foundation.RikkaTheme

// ─── Size ───────────────────────────────────────────────────

/** Icon size variants. */
public enum class IconSize(
    public val dp: Dp,
) {
    Xs(12.dp),
    Sm(16.dp),
    Default(20.dp),
    Lg(24.dp),
    Xl(32.dp),
}

// ─── Component ──────────────────────────────────────────────

/**
 * Foundation-only icon composable that resolves a semantic [IconToken].
 *
 * Tint resolution order: explicit [tint] > [LocalContentColor] > theme onBackground.
 * Pass `null` for [contentDescription] to mark the icon as decorative (clears semantics).
 *
 * ```
 * Icon(RikkaIcons.Heart, contentDescription = "Favorite")
 *
 * Icon(
 *     imageVector = RikkaIcons.Settings,
 *     contentDescription = null,
 *     size = IconSize.Lg,
 *     spin = true,
 * )
 * ```
 *
 * @param imageVector The semantic [IconToken] to render (e.g. from [RikkaIcons]).
 * @param contentDescription Accessibility label; null marks the icon as purely decorative.
 * @param modifier [Modifier] applied to the icon container.
 * @param tint Color applied as a tint filter; [Color.Unspecified] defers to [LocalContentColor].
 * @param size Optional [IconSize] override; null uses the vector's default dimensions.
 * @param spin When true, continuously rotates the icon using theme motion duration.
 */
@Composable
public fun Icon(
    imageVector: IconToken,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    size: IconSize? = null,
    spin: Boolean = false,
) {
    val resolvedTint =
        when {
            tint != Color.Unspecified -> tint
            LocalContentColor.current != Color.Unspecified -> LocalContentColor.current
            else -> RikkaTheme.colors.onBackground
        }
    val semanticsModifier =
        if (contentDescription == null) {
            modifier.clearAndSetSemantics {}
        } else {
            modifier
        }

    val sizeModifier =
        if (size != null) {
            Modifier.size(size.dp)
        } else {
            Modifier.size(IconSize.Default.dp)
        }

    val spinModifier =
        if (spin) {
            val motion = RikkaTheme.motion
            val infiniteTransition = rememberInfiniteTransition()
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec =
                    infiniteRepeatable(
                        animation =
                            tween(
                                durationMillis = motion.durationSpin,
                                easing = LinearEasing,
                            ),
                    ),
            )
            Modifier.graphicsLayer { rotationZ = rotation }
        } else {
            Modifier
        }

    Box(
        modifier =
            semanticsModifier
                .then(sizeModifier)
                .then(spinModifier),
    ) {
        if (contentDescription == null) {
            DecorativeAppIcon(
                token = imageVector,
                tint = resolvedTint,
                size = size?.dp ?: IconSize.Default.dp,
            )
        } else {
            AppIcon(
                token = imageVector,
                contentDescription = contentDescription,
                tint = resolvedTint,
                size = size?.dp ?: IconSize.Default.dp,
            )
        }
    }
}
