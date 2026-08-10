package zed.rainxch.rikkaui.components.ui.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import com.kyant.backdrop.Backdrop
import zed.rainxch.rikkaui.foundation.RikkaTheme

/**
 * A small glass pill for status, filters, and tags.
 *
 * Sits at [GlassLevel.Subtle] by default because a chip belongs *on* the surface
 * it labels, not floating above it. Selecting a chip promotes it to
 * [GlassLevel.Regular] and crossfades the tint to the accent, so selection reads
 * as the chip lifting rather than merely recolouring.
 *
 * ```
 * GlassChip(selected = filter == Missed, onClick = { filter = Missed }) {
 *     Text("Missed")
 * }
 * ```
 *
 * @param modifier [Modifier] applied to the root Row.
 * @param selected Whether the chip is currently selected.
 * @param onClick Optional click handler; when non-null the chip becomes interactive.
 * @param enabled Whether the chip responds to input.
 * @param level [GlassLevel] used when unselected; selection promotes it one rung.
 * @param shape Surface outline; must be corner-based. Pill by default.
 * @param backdrop What shows through; taken from [LocalGlassBackdrop] by default.
 * @param tint Colour washed over the refracted backdrop when unselected.
 * @param selectedTint Colour washed over the refracted backdrop when selected.
 * @param contentPadding Padding between the surface edge and its content.
 * @param label Accessibility content description; use it when the content is icon-only.
 * @param content [RowScope] content lambda, typically a short label.
 */
@Composable
public fun GlassChip(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    level: GlassLevel = GlassLevel.Subtle,
    shape: CornerBasedShape = GlassButtonDefaults.shape(),
    backdrop: Backdrop = LocalGlassBackdrop.current,
    tint: Color = RikkaTheme.glass.tint,
    selectedTint: Color = RikkaTheme.colors.primary,
    contentPadding: PaddingValues =
        PaddingValues(
            horizontal = RikkaTheme.spacing.md,
            vertical = RikkaTheme.spacing.xs,
        ),
    label: String = "",
    content: @Composable RowScope.() -> Unit,
) {
    val motion = RikkaTheme.motion

    // The level swap is a discrete token change, so the tint carries the
    // transition — without it, selection pops.
    val animatedTint by animateColorAsState(
        targetValue = if (selected) selectedTint else tint,
        animationSpec = motion.effectsDefault(),
        label = "glassChipTint",
    )
    val animatedContentColor by animateColorAsState(
        targetValue =
            when {
                selected -> RikkaTheme.colors.onPrimary
                else -> RikkaTheme.colors.onSurface
            },
        animationSpec = motion.effectsDefault(),
        label = "glassChipContent",
    )

    val style =
        rememberGlassStyle(
            level = if (selected) level.stepUp() else level,
            tint = animatedTint,
        )

    val interactionSource = remember { MutableInteractionSource() }
    val press = rememberGlassPressState(interactionSource, enabled = enabled && onClick != null)

    val clickModifier =
        if (onClick != null) {
            Modifier.selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Checkbox,
                onClick = onClick,
            )
        } else {
            Modifier
        }

    GlassContentScope(
        contentColor = if (enabled) animatedContentColor else animatedContentColor.copy(alpha = 0.5f),
        nestedBackdrop = backdrop,
        textStyle = RikkaTheme.typography.small,
    ) {
        Row(
            modifier =
                modifier
                    .semantics {
                        if (label.isNotEmpty()) contentDescription = label
                        if (!enabled && onClick != null) disabled()
                    }.glassSurface(
                        backdrop = backdrop,
                        style = style,
                        shape = shape,
                        pressFraction = press.pressFraction,
                        layerBlock = press.layerBlock,
                    ).clip(shape)
                    .then(clickModifier)
                    .padding(contentPadding),
            horizontalArrangement = Arrangement.spacedBy(RikkaTheme.spacing.xs, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}
