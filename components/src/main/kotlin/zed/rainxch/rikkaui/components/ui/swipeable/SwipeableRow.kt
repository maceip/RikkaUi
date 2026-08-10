package zed.rainxch.rikkaui.components.ui.swipeable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox
import zed.rainxch.rikkaicons.core.IconToken
import zed.rainxch.rikkaui.components.ui.icon.Icon
import zed.rainxch.rikkaui.components.ui.icon.IconSize
import zed.rainxch.rikkaui.foundation.RikkaTheme

/** An action revealed from either edge of a [SwipeableRow]. */
@Immutable
public data class SwipeableRowAction(
    val onSwipe: () -> Unit,
    val background: Color,
    val icon: @Composable () -> Unit,
    val label: String = "",
    val weight: Double = 1.0,
    val isUndo: Boolean = false,
)

/** Defaults shared by every RikkaUI swipeable row. */
public object SwipeableRowDefaults {
    public val SwipeThreshold: Dp = 56.dp
    public val ActionPanelSize: Dp = 48.dp

    @Composable
    public fun shape(): CornerBasedShape = RikkaTheme.shapes.lg as? CornerBasedShape ?: RoundedCornerShape(12.dp)
}

/**
 * General-purpose row wrapper that reveals actions from either horizontal edge.
 *
 * The revealed layer is transparent before the commit threshold and clipped to
 * [shape]. Labelled actions are also exposed as accessibility custom actions.
 */
@Composable
public fun SwipeableRow(
    modifier: Modifier = Modifier,
    startActions: List<SwipeableRowAction> = emptyList(),
    endActions: List<SwipeableRowAction> = emptyList(),
    swipeThreshold: Dp = SwipeableRowDefaults.SwipeThreshold,
    shape: CornerBasedShape = SwipeableRowDefaults.shape(),
    content: @Composable BoxScope.() -> Unit,
) {
    val actionSemantics =
        remember(startActions, endActions) {
            (startActions + endActions)
                .filter { it.label.isNotEmpty() }
                .map { action ->
                    CustomAccessibilityAction(action.label) {
                        action.onSwipe()
                        true
                    }
                }
        }

    SwipeableActionsBox(
        modifier =
            modifier
                .clip(shape)
                .semantics {
                    if (actionSemantics.isNotEmpty()) customActions = actionSemantics
                },
        startActions = startActions.map { it.toLibraryAction() },
        endActions = endActions.map { it.toLibraryAction() },
        swipeThreshold = swipeThreshold,
        backgroundUntilSwipeThreshold = Color.Transparent,
        content = content,
    )
}

/** Creates a themed, icon-based action for [SwipeableRow]. */
@Composable
public fun rememberSwipeableRowAction(
    icon: IconToken,
    background: Color,
    contentColor: Color,
    label: String,
    onSwipe: () -> Unit,
    weight: Double = 1.0,
    isUndo: Boolean = false,
): SwipeableRowAction =
    remember(icon, background, contentColor, label, weight, isUndo, onSwipe) {
        SwipeableRowAction(
            onSwipe = onSwipe,
            background = background,
            icon = {
                Box(
                    modifier = Modifier.size(SwipeableRowDefaults.ActionPanelSize),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        size = IconSize.Lg,
                    )
                }
            },
            label = label,
            weight = weight,
            isUndo = isUndo,
        )
    }

private fun SwipeableRowAction.toLibraryAction(): SwipeAction =
    SwipeAction(
        onSwipe = onSwipe,
        icon = icon,
        background = background,
        weight = weight,
        isUndo = isUndo,
    )
