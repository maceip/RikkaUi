package zed.rainxch.rikkaui.components.ui.glass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import zed.rainxch.rikkaui.foundation.LocalContentColor
import zed.rainxch.rikkaui.foundation.RikkaTheme

// ─── Defaults ───────────────────────────────────────────────

public object GlassButtonDefaults {
    /** Pill by default — glass reads as a physical lozenge, not a panel. */
    @Composable
    @ReadOnlyComposable
    public fun shape(): CornerBasedShape = RikkaTheme.shapes.full as? CornerBasedShape ?: RoundedCornerShape(50)

    @Composable
    @ReadOnlyComposable
    public fun contentPadding(): PaddingValues = PaddingValues(horizontal = RikkaTheme.spacing.lg)
}

// ─── Component ──────────────────────────────────────────────

/**
 * The glass counterpart to [zed.rainxch.rikkaui.components.ui.button.Button].
 *
 * A pressable lozenge of glass. Pressing scales it through the backdrop layer,
 * so the scenery behind stays anchored while the button sinks — and at the same
 * time the edge refracts harder and the specular sweeps, so the slab reads as
 * deforming under the finger rather than merely shrinking. All of it runs in the
 * draw and layer phases; a press recomposes nothing.
 *
 * The row height honours [zed.rainxch.rikkaui.foundation.RikkaTheme.minTouchTarget],
 * so a button stays tappable even when its content is a single small icon.
 *
 * ```
 * GlassButton(onClick = { call() }) {
 *     Icon(RikkaIcons.Phone, null)
 *     Text("Call")
 * }
 * ```
 *
 * @param onClick Invoked when the button is tapped.
 * @param modifier [Modifier] applied to the root Row.
 * @param enabled Whether the button responds to input; disabled buttons dim their content.
 * @param level [GlassLevel] controlling blur, refraction, tint, and shadow depth.
 * @param shape Surface outline; must be corner-based. Pill by default.
 * @param backdrop What shows through; taken from [LocalGlassBackdrop] by default.
 * @param tint Colour washed over the refracted backdrop. Pass an accent here for
 *   a primary action — it tints the glass without making it opaque.
 * @param contentColor Colour provided to children through [LocalContentColor].
 * @param contentPadding Padding between the surface edge and its content.
 * @param label Accessibility content description; use it when the content is icon-only.
 * @param content [RowScope] content lambda, centred and spaced by `spacing.sm`.
 */
@Composable
public fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    level: GlassLevel = GlassLevel.Regular,
    shape: CornerBasedShape = GlassButtonDefaults.shape(),
    backdrop: Backdrop = LocalGlassBackdrop.current,
    tint: Color = RikkaTheme.glass.tint,
    contentColor: Color = RikkaTheme.colors.onSurface,
    contentPadding: PaddingValues = GlassButtonDefaults.contentPadding(),
    label: String = "",
    content: @Composable RowScope.() -> Unit,
) {
    val style = rememberGlassStyle(level = level, tint = tint)

    val interactionSource = remember { MutableInteractionSource() }
    val press = rememberGlassPressState(interactionSource, enabled = enabled)

    GlassContentScope(
        contentColor = if (enabled) contentColor else contentColor.copy(alpha = 0.5f),
        nestedBackdrop = backdrop,
    ) {
        Row(
            modifier =
                modifier
                    .semantics {
                        if (label.isNotEmpty()) contentDescription = label
                        if (!enabled) disabled()
                    }.glassSurface(
                        backdrop = backdrop,
                        style = style,
                        shape = shape,
                        pressFraction = press.pressFraction,
                        layerBlock = press.layerBlock,
                    ).clip(shape)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = enabled,
                        role = Role.Button,
                        onClick = onClick,
                    ).defaultMinSize(minWidth = RikkaTheme.minTouchTarget, minHeight = RikkaTheme.minTouchTarget)
                    .padding(contentPadding),
            horizontalArrangement = Arrangement.spacedBy(RikkaTheme.spacing.sm, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/**
 * A circular glass button sized for a single icon or digit.
 *
 * This is the dial-key shape: the keypad is a grid of these over a shared
 * backdrop, which is why the size is fixed rather than content-driven.
 *
 * ```
 * GlassIconButton(onClick = { press('5') }, label = "Five", size = 72.dp) { Text("5") }
 * ```
 *
 * @param onClick Invoked when the button is tapped.
 * @param modifier [Modifier] applied to the root Box.
 * @param enabled Whether the button responds to input.
 * @param size Diameter of the button; also its touch target.
 * @param level [GlassLevel] controlling blur, refraction, tint, and shadow depth.
 * @param backdrop What shows through; taken from [LocalGlassBackdrop] by default.
 * @param tint Colour washed over the refracted backdrop.
 * @param contentColor Colour provided to children through [LocalContentColor].
 * @param label Accessibility content description. Supply it — the content is
 *   usually a bare icon or glyph with nothing readable in it.
 * @param content Centred content, typically an icon or a single glyph.
 */
@Composable
public fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 56.dp,
    level: GlassLevel = GlassLevel.Subtle,
    backdrop: Backdrop = LocalGlassBackdrop.current,
    tint: Color = RikkaTheme.glass.tint,
    contentColor: Color = RikkaTheme.colors.onSurface,
    label: String = "",
    content: @Composable RowScope.() -> Unit,
) {
    GlassButton(
        onClick = onClick,
        modifier = modifier.size(size),
        enabled = enabled,
        level = level,
        // 50% corners on a square give a circle, and stay corner-based so the
        // refraction shader still has radii to work with.
        shape = RoundedCornerShape(percent = 50),
        backdrop = backdrop,
        tint = tint,
        contentColor = contentColor,
        contentPadding = PaddingValues(0.dp),
        label = label,
        content = content,
    )
}
