package zed.rainxch.rikkaui.components.ui.navigationbar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import zed.rainxch.rikkaicons.core.IconToken
import zed.rainxch.rikkaui.components.ui.icon.Icon
import zed.rainxch.rikkaui.components.ui.icon.IconSize
import zed.rainxch.rikkaui.components.ui.text.Text
import zed.rainxch.rikkaui.components.ui.text.TextVariant
import zed.rainxch.rikkaui.foundation.RikkaTheme

// ─── Animation ────────────────────────────────────────────────

public enum class NavigationBarAnimation {
    /** Spring-based transitions (default). */
    Spring,

    /** Smooth eased tween transitions. */
    Tween,

    /** Instant, no animation. */
    None,
}

// ─── Layout ───────────────────────────────────────────────────

/** How a [NavigationBarItem] arranges its icon against its label. */
public enum class NavigationBarItemLayout {
    /**
     * Icon above the label, indicator pill behind the icon alone. The taller of
     * the two, and the default.
     */
    Stacked,

    /**
     * Icon and label on one row, indicator pill wrapping both. Use this where
     * the bar is a short floating pill rather than a full-width dock: a stacked
     * item needs roughly 56dp of height before the label clears the icon, which
     * a 64dp pill cannot give it without the text crowding the edge.
     */
    Inline,
}

// ─── NavigationBar ────────────────────────────────────────────

/**
 * A bottom navigation bar with a top border divider.
 *
 * Renders a fixed-height (80dp) row with evenly spaced navigation items. Place
 * [NavigationBarItem] composables inside the [content] lambda.
 *
 * @param modifier [Modifier] applied to the navigation bar container.
 * @param containerColor Background color of the navigation bar. Defaults to [RikkaTheme.colors.background].
 * @param content Row content lambda for [NavigationBarItem] composables.
 */
@Composable
public fun NavigationBar(
    modifier: Modifier = Modifier,
    containerColor: Color = RikkaTheme.colors.background,
    content: @Composable RowScope.() -> Unit,
) {
    val borderColor = RikkaTheme.colors.border
    val dividerHeight = 1.dp

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .drawBehind {
                    drawLine(
                        color = borderColor,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = dividerHeight.toPx(),
                    )
                }.background(containerColor),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(
                        horizontal = RikkaTheme.spacing.sm,
                    ).semantics { isTraversalGroup = true },
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content()
        }
    }
}

// ─── NavigationBarItem (content lambda) ───────────────────────

/**
 * A navigation bar item with icon, optional label, and animated indicator pill.
 *
 * This is the content-lambda overload that accepts composable lambdas for icon, label,
 * and selected icon. Features animated indicator pill, press scale, hover highlight,
 * and label fade transitions.
 *
 * @param selected Whether this item is currently selected.
 * @param onClick Callback invoked when the item is clicked.
 * @param icon Composable icon displayed in the default (unselected) state.
 * @param modifier [Modifier] applied to the item container.
 * @param label Optional composable label displayed below the icon. Pass null to hide.
 * @param selectedIcon Optional composable icon displayed when selected. Falls back to [icon] if null.
 * @param enabled Whether the item is interactive. Defaults to true.
 * @param alwaysShowLabel Whether to always show the label or only when selected. Defaults to true.
 * @param animation [NavigationBarAnimation] style for state transitions. Defaults to [NavigationBarAnimation.Spring].
 * @param indicatorColor Color of the selection indicator pill. Defaults to [RikkaTheme.colors.secondary] when [Color.Unspecified].
 * @param layout Whether the icon sits above the label or beside it. See [NavigationBarItemLayout].
 * @param iconSlotSize Side of the square the icon is laid out in. Raise it with
 *   the icon itself; an icon larger than its slot is constrained down to fit.
 * @param showIndicator Whether the item draws its own selection pill. Turn it off
 *   when the bar owns the indicator instead of the item: `GlassNavigationBar`
 *   (Android only) travels a single pill between tabs, and a per-item pill
 *   underneath it would double up.
 */
@Composable
public fun RowScope.NavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    selectedIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    alwaysShowLabel: Boolean = true,
    animation: NavigationBarAnimation = NavigationBarAnimation.Spring,
    indicatorColor: Color = Color.Unspecified,
    layout: NavigationBarItemLayout = NavigationBarItemLayout.Stacked,
    iconSlotSize: Dp = 24.dp,
    showIndicator: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val colors = RikkaTheme.colors
    val motion = RikkaTheme.motion

    // ─── Resolve animation specs ──────────────────────────
    val floatAnimSpec: AnimationSpec<Float> = resolveAnimSpec(animation, motion)
    val dpAnimSpec: AnimationSpec<Dp> = resolveAnimSpec(animation, motion)
    val fastFloatSpec: AnimationSpec<Float> =
        if (animation == NavigationBarAnimation.Tween) {
            motion.effectsFast()
        } else {
            floatAnimSpec
        }

    val resolvedIndicator =
        if (indicatorColor != Color.Unspecified) {
            indicatorColor
        } else {
            colors.secondary
        }

    // ─── Indicator animation ─────────────────────────────
    // Both branches drive the pill off these two values, so suppressing it for a
    // bar that owns its own indicator is a matter of never raising them.
    val indicatorVisible = selected && showIndicator
    val indicatorWidth by animateDpAsState(
        targetValue = if (indicatorVisible) 48.dp else 0.dp,
        animationSpec = dpAnimSpec,
    )
    val indicatorAlpha by animateFloatAsState(
        targetValue = if (indicatorVisible) 1f else 0f,
        animationSpec = floatAnimSpec,
    )

    // ─── Label fade animation ────────────────────────────
    val showLabel = alwaysShowLabel || selected
    val labelAlpha by animateFloatAsState(
        targetValue = if (showLabel && label != null) 1f else 0f,
        animationSpec = floatAnimSpec,
    )
    val labelOffset by animateDpAsState(
        targetValue = if (showLabel && label != null) 0.dp else 4.dp,
        animationSpec = dpAnimSpec,
    )

    // ─── Press scale ─────────────────────────────────────
    val pressScale by animateFloatAsState(
        targetValue =
            if (isPressed && enabled) {
                motion.pressScaleSubtle
            } else {
                1f
            },
        animationSpec = floatAnimSpec,
    )

    // ─── Hover background ────────────────────────────────
    val hoverAlpha by animateFloatAsState(
        targetValue =
            if (isHovered && enabled && !selected) {
                0.5f
            } else {
                0f
            },
        animationSpec = fastFloatSpec,
    )

    Box(
        modifier =
            modifier
                .weight(1f)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Tab,
                    enabled = enabled,
                    onClick = onClick,
                ).semantics(mergeDescendants = true) {
                    this.selected = selected
                    if (!enabled) disabled()
                }.graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                },
        contentAlignment = Alignment.Center,
    ) {
        when (layout) {
            NavigationBarItemLayout.Stacked ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    // ─── Icon with indicator ─────────────────
                    Box(
                        contentAlignment = Alignment.Center,
                    ) {
                        // Indicator pill behind icon
                        Box(
                            modifier =
                                Modifier
                                    .width(indicatorWidth)
                                    .height(32.dp)
                                    .graphicsLayer { alpha = indicatorAlpha }
                                    .background(
                                        color = resolvedIndicator,
                                        shape = RikkaTheme.shapes.full,
                                    ),
                        )

                        // Hover highlight
                        if (hoverAlpha > 0f) {
                            Box(
                                modifier =
                                    Modifier
                                        .width(48.dp)
                                        .height(32.dp)
                                        .graphicsLayer { alpha = hoverAlpha }
                                        .background(
                                            color = colors.muted,
                                            shape = RikkaTheme.shapes.full,
                                        ),
                            )
                        }

                        // Icon
                        Box(
                            modifier = Modifier.size(iconSlotSize),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected && selectedIcon != null) {
                                selectedIcon()
                            } else {
                                icon()
                            }
                        }
                    }

                    // ─── Label ───────────────────────────────
                    if (label != null) {
                        Box(
                            modifier =
                                Modifier
                                    .padding(top = RikkaTheme.spacing.xs)
                                    .offset { IntOffset(x = 0, y = labelOffset.roundToPx()) }
                                    .graphicsLayer { alpha = labelAlpha },
                        ) {
                            label()
                        }
                    }
                }

            // Inline wraps icon *and* label, so the pill is a background on the
            // row rather than a sized box behind the icon: its width comes from
            // the content and needs no animation of its own.
            NavigationBarItemLayout.Inline ->
                Row(
                    modifier =
                        Modifier
                            .background(
                                color = resolvedIndicator.copy(alpha = indicatorAlpha),
                                shape = RikkaTheme.shapes.full,
                            ).background(
                                color = colors.muted.copy(alpha = hoverAlpha),
                                shape = RikkaTheme.shapes.full,
                            ).padding(
                                horizontal = RikkaTheme.spacing.md,
                                vertical = RikkaTheme.spacing.sm,
                            ),
                    horizontalArrangement = Arrangement.spacedBy(RikkaTheme.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(iconSlotSize),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected && selectedIcon != null) {
                            selectedIcon()
                        } else {
                            icon()
                        }
                    }

                    if (label != null && showLabel) {
                        Box(modifier = Modifier.graphicsLayer { alpha = labelAlpha }) {
                            label()
                        }
                    }
                }
        }
    }
}

// ─── NavigationBarItem (convenience overload) ─────────────────

/**
 * Convenience navigation bar item that accepts [IconToken] and [String] directly.
 *
 * Automatically animates icon and label colors between active/inactive states.
 * Delegates to the content-lambda overload of [NavigationBarItem].
 *
 * @param selected Whether this item is currently selected.
 * @param onClick Callback invoked when the item is clicked.
 * @param icon [IconToken] displayed in the default (unselected) state.
 * @param label String label displayed below the icon.
 * @param modifier [Modifier] applied to the item container.
 * @param selectedIcon Optional [IconToken] displayed when selected. Falls back to [icon] if null.
 * @param enabled Whether the item is interactive. Defaults to true.
 * @param alwaysShowLabel Whether to always show the label or only when selected. Defaults to true.
 * @param animation [NavigationBarAnimation] style for state transitions. Defaults to [NavigationBarAnimation.Spring].
 * @param indicatorColor Color of the selection indicator pill. Defaults to [RikkaTheme.colors.secondary] when [Color.Unspecified].
 * @param activeColor Icon and label color when selected. Defaults to [RikkaTheme.colors.primary] when [Color.Unspecified].
 * @param inactiveColor Icon and label color when not selected. Defaults to [RikkaTheme.colors.onMuted] when [Color.Unspecified].
 * @param layout Whether the icon sits above the label or beside it. See [NavigationBarItemLayout].
 * @param iconSize Size of the icon glyph and of the slot it is laid out in. Defaults to [IconSize.Default].
 * @param showIndicator Whether the item draws its own selection pill. Turn it off
 *   when the bar owns the indicator instead of the item: `GlassNavigationBar`
 *   (Android only) travels a single pill between tabs, and a per-item pill
 *   underneath it would double up.
 */
@Composable
public fun RowScope.NavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: IconToken,
    label: String,
    modifier: Modifier = Modifier,
    selectedIcon: IconToken? = null,
    enabled: Boolean = true,
    alwaysShowLabel: Boolean = true,
    animation: NavigationBarAnimation = NavigationBarAnimation.Spring,
    indicatorColor: Color = Color.Unspecified,
    activeColor: Color = Color.Unspecified,
    inactiveColor: Color = Color.Unspecified,
    layout: NavigationBarItemLayout = NavigationBarItemLayout.Stacked,
    iconSize: IconSize = IconSize.Default,
    showIndicator: Boolean = true,
) {
    val colors = RikkaTheme.colors
    val motion = RikkaTheme.motion

    // ─── Resolve color targets ────────────────────────────
    val resolvedActive =
        if (activeColor != Color.Unspecified) {
            activeColor
        } else {
            colors.primary
        }
    val resolvedInactive =
        if (inactiveColor != Color.Unspecified) {
            inactiveColor
        } else {
            colors.onMuted
        }

    // ─── Resolve animation spec for colors ────────────────
    val colorAnimSpec: AnimationSpec<Color> = resolveAnimSpec(animation, motion)

    // ─── Animated icon + label colors ────────────────────
    val iconColor by animateColorAsState(
        targetValue = if (selected) resolvedActive else resolvedInactive,
        animationSpec = colorAnimSpec,
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) resolvedActive else resolvedInactive,
        animationSpec = colorAnimSpec,
    )

    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                size = iconSize,
            )
        },
        modifier =
            modifier.semantics {
                contentDescription = label
            },
        label = {
            Text(
                text = label,
                variant = TextVariant.Small,
                color = labelColor,
            )
        },
        selectedIcon =
            if (selectedIcon != null) {
                {
                    Icon(
                        imageVector = selectedIcon,
                        contentDescription = null,
                        tint = iconColor,
                        size = iconSize,
                    )
                }
            } else {
                {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        size = iconSize,
                    )
                }
            },
        enabled = enabled,
        alwaysShowLabel = alwaysShowLabel,
        animation = animation,
        indicatorColor = indicatorColor,
        layout = layout,
        iconSlotSize = iconSize.dp,
        showIndicator = showIndicator,
    )
}

// ─── Internal: Animation Spec Resolution ──────────────────────

@Composable
private fun <T> resolveAnimSpec(
    animation: NavigationBarAnimation,
    motion: zed.rainxch.rikkaui.foundation.RikkaMotion,
): AnimationSpec<T> =
    when (animation) {
        NavigationBarAnimation.Spring -> motion.spatialDefault()
        NavigationBarAnimation.Tween -> motion.effectsDefault()
        NavigationBarAnimation.None -> snap()
    }
