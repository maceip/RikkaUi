package zed.rainxch.rikkaui.components.ui.glass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import com.kyant.backdrop.Backdrop
import zed.rainxch.rikkaui.foundation.LocalContentColor
import zed.rainxch.rikkaui.foundation.RikkaTheme

/**
 * The glass counterpart to [zed.rainxch.rikkaui.components.ui.card.Card].
 *
 * Groups related content on a refracting slab instead of an opaque surface. Use
 * the same `CardHeader` / `CardContent` / `CardFooter` sections inside it.
 *
 * Screen readers treat the card as a single navigable unit via merged
 * descendants, matching `Card`'s behaviour.
 *
 * ```
 * GlassCard(onClick = { open() }) {
 *     CardHeader { Text("Recent call", variant = TextVariant.H3) }
 *     CardContent { Text("+1 555 0134 — 2 min ago") }
 * }
 * ```
 *
 * @param modifier [Modifier] applied to the root Column.
 * @param level [GlassLevel] controlling blur, refraction, tint, and shadow depth.
 * @param shape Surface outline; must be corner-based.
 * @param backdrop What shows through; taken from [LocalGlassBackdrop] by default.
 * @param onClick Optional click handler; when non-null the card becomes interactive.
 * @param enabled Whether an interactive card responds to input.
 * @param tint Colour washed over the refracted backdrop.
 * @param contentColor Colour provided to children through [LocalContentColor].
 * @param contentPadding Padding between the surface edge and its content.
 * @param label Accessibility content description for the card.
 * @param hostsGlass Whether the card expects glass inside it; see [GlassSurface].
 * @param content [ColumnScope] content lambda for the card body.
 */
@Composable
public fun GlassCard(
    modifier: Modifier = Modifier,
    level: GlassLevel = GlassLevel.Regular,
    shape: CornerBasedShape = GlassDefaults.shape(),
    backdrop: Backdrop = LocalGlassBackdrop.current,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    tint: Color = RikkaTheme.glass.tint,
    contentColor: Color = RikkaTheme.colors.onSurface,
    contentPadding: PaddingValues = GlassDefaults.contentPadding(),
    label: String = "",
    hostsGlass: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val style = rememberGlassStyle(level = level, tint = tint)
    val exported = if (hostsGlass) rememberGlassBackdrop() else null

    val interactionSource = remember { MutableInteractionSource() }
    val press = rememberGlassPressState(interactionSource, enabled = enabled && onClick != null)

    val clickModifier =
        if (onClick != null) {
            Modifier.clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
        } else {
            Modifier
        }

    GlassContentScope(
        contentColor = if (enabled) contentColor else contentColor.copy(alpha = 0.5f),
        nestedBackdrop = exported ?: backdrop,
    ) {
        Column(
            modifier =
                modifier
                    .semantics(mergeDescendants = true) {
                        if (label.isNotEmpty()) contentDescription = label
                        if (!enabled && onClick != null) disabled()
                    }.glassSurface(
                        backdrop = backdrop,
                        style = style,
                        shape = shape,
                        pressFraction = press.pressFraction,
                        exportedBackdrop = exported,
                        // Scale through the backdrop layer so the refracted
                        // scenery stays anchored while the card presses in.
                        layerBlock = press.layerBlock,
                    ).clip(shape)
                    .then(clickModifier)
                    .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(RikkaTheme.spacing.sm),
            content = content,
        )
    }
}
