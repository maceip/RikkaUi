package zed.rainxch.rikkaui.components.ui.call

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import zed.rainxch.rikkaicons.core.IconToken
import zed.rainxch.rikkaui.components.ui.glass.GlassContentScope
import zed.rainxch.rikkaui.components.ui.glass.GlassDefaults
import zed.rainxch.rikkaui.components.ui.glass.GlassLevel
import zed.rainxch.rikkaui.components.ui.glass.LocalGlassBackdrop
import zed.rainxch.rikkaui.components.ui.glass.rememberGlassPressState
import zed.rainxch.rikkaui.components.ui.glass.rememberGlassStyle
import zed.rainxch.rikkaui.components.ui.glass.rememberGlassSurface
import zed.rainxch.rikkaui.components.ui.icon.Icon
import zed.rainxch.rikkaui.components.ui.icon.IconSize
import zed.rainxch.rikkaui.components.ui.text.Text
import zed.rainxch.rikkaui.components.ui.text.TextVariant
import zed.rainxch.rikkaui.foundation.RikkaTheme

// ─── Defaults ───────────────────────────────────────────────

public object CallActionTileDefaults {
    /** Fixed rather than content-driven, so a grid of tiles lines up in columns. */
    public val Width: Dp = 96.dp

    /** Floor only — a wrapped two-line label grows the tile instead of clipping. */
    public val MinHeight: Dp = 68.dp

    public val ActiveStateDescription: String = "On"

    public val InactiveStateDescription: String = "Off"
}

// ─── Component ──────────────────────────────────────────────

/**
 * One latching control on a live call's action grid.
 *
 * Mute, speaker, and hold carry their on/off condition in [active] rather than
 * in the label, so a screen reader announces the state instead of the user
 * having to infer it from a word change.
 *
 * Engaging a control promotes it one [GlassLevel] and crossfades [activeTint]
 * over it, so "on" reads as the tile rising out of the surface rather than as a
 * colour swap a glance can miss. The press response is the material's own — the
 * tile sinks, refracts harder, and sweeps its specular, all in the draw and
 * layer phases, so a grid of eight tiles recomposes nothing on touch.
 *
 * ```
 * CallActionTile(
 *     icon = RikkaIcons.MicOff,
 *     label = "Mute",
 *     active = call.muted,
 *     onClick = { call.toggleMute() },
 * )
 * ```
 *
 * @param icon The control's glyph, drawn above [label].
 * @param label The control's name, e.g. "Mute".
 * @param active Whether the control is currently engaged.
 * @param onClick Invoked when the tile is tapped.
 * @param modifier [Modifier] applied to the root Column.
 * @param enabled Whether the tile responds to input; disabled tiles dim their content.
 * @param level [GlassLevel] used while inactive; [active] promotes it one rung.
 * @param shape Surface outline; must be corner-based.
 * @param backdrop What shows through; taken from [LocalGlassBackdrop] by default.
 * @param tint Colour washed over the refracted backdrop while inactive.
 * @param activeTint Colour washed over the refracted backdrop while [active].
 * @param contentColor Colour provided to children while inactive.
 * @param activeContentColor Colour provided to children while [active].
 * @param contentPadding Padding between the surface edge and its content.
 * @param activeStateDescription Announced state while [active]. Localise it.
 * @param inactiveStateDescription Announced state while inactive. Localise it.
 */
@Composable
public fun CallActionTile(
    icon: IconToken,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    level: GlassLevel = GlassLevel.Subtle,
    shape: CornerBasedShape = GlassDefaults.shape(),
    backdrop: Backdrop = LocalGlassBackdrop.current,
    tint: Color = RikkaTheme.glass.tint,
    activeTint: Color = RikkaTheme.colors.primary,
    contentColor: Color = RikkaTheme.colors.onSurface,
    activeContentColor: Color = RikkaTheme.colors.onPrimary,
    contentPadding: PaddingValues = PaddingValues(RikkaTheme.spacing.sm),
    activeStateDescription: String = CallActionTileDefaults.ActiveStateDescription,
    inactiveStateDescription: String = CallActionTileDefaults.InactiveStateDescription,
) {
    val motion = RikkaTheme.motion

    // The level swap is a discrete token change, so the tint carries the
    // transition — without it, latching pops.
    val animatedTint by animateColorAsState(
        targetValue = if (active) activeTint else tint,
        animationSpec = motion.effectsDefault(),
        label = "callActionTileTint",
    )
    val animatedContentColor by animateColorAsState(
        targetValue = if (active) activeContentColor else contentColor,
        animationSpec = motion.effectsDefault(),
        label = "callActionTileContent",
    )

    val style = rememberGlassStyle(level = if (active) level.stepUp() else level, tint = animatedTint)

    val interactionSource = remember { MutableInteractionSource() }
    val press = rememberGlassPressState(interactionSource, enabled = enabled)

    GlassContentScope(
        contentColor = if (enabled) animatedContentColor else animatedContentColor.copy(alpha = 0.5f),
        nestedBackdrop = backdrop,
    ) {
        Column(
            modifier =
                modifier
                    .width(CallActionTileDefaults.Width)
                    .defaultMinSize(minHeight = CallActionTileDefaults.MinHeight)
                    .semantics {
                        this.role = Role.Button
                        stateDescription = if (active) activeStateDescription else inactiveStateDescription
                        if (!enabled) disabled()
                    }.rememberGlassSurface(
                        backdrop = backdrop,
                        style = style,
                        shape = shape,
                        pressFraction = press.pressFraction,
                        // Scale through the backdrop layer so the refracted
                        // scenery stays anchored while the tile presses in.
                        layerBlock = press.layerBlock,
                    ).clip(shape)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = enabled,
                        onClick = onClick,
                    ).padding(contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RikkaTheme.spacing.xs, Alignment.CenterVertically),
        ) {
            // No explicit tint: the surface publishes the content colour the
            // label already reads, so the glyph follows the same accent swap
            // when the control latches on.
            Icon(imageVector = icon, contentDescription = null, size = IconSize.Lg)
            Text(text = label, variant = TextVariant.Small, textAlign = TextAlign.Center)
        }
    }
}
