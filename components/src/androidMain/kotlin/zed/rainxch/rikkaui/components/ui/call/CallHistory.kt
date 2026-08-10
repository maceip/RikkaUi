package zed.rainxch.rikkaui.components.ui.call

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox
import zed.rainxch.rikkaui.components.ui.avatar.Avatar
import zed.rainxch.rikkaui.components.ui.avatar.AvatarSize
import zed.rainxch.rikkaui.components.ui.badge.Badge
import zed.rainxch.rikkaui.components.ui.badge.BadgeSize
import zed.rainxch.rikkaui.components.ui.badge.BadgeVariant
import zed.rainxch.rikkaui.components.ui.glass.GlassCard
import zed.rainxch.rikkaui.components.ui.glass.GlassDefaults
import zed.rainxch.rikkaui.components.ui.glass.GlassLevel
import zed.rainxch.rikkaui.components.ui.glass.LocalGlassBackdrop
import zed.rainxch.rikkaui.components.ui.icon.Icon
import zed.rainxch.rikkaui.components.ui.icon.RikkaIcons
import zed.rainxch.rikkaui.components.ui.text.Text
import zed.rainxch.rikkaui.components.ui.text.TextVariant
import zed.rainxch.rikkaui.foundation.RikkaTheme

// ─── Swipe actions ──────────────────────────────────────────

/**
 * An action revealed by swiping a [GlassSwipeableRow].
 *
 * @property onSwipe Invoked once the row is released past the threshold.
 * @property background Colour of the panel revealed behind the row.
 * @property icon Icon drawn on the revealed panel, centred.
 * @property label Accessibility label. Swipe actions are unreachable with a
 *   screen reader, so this is what surfaces them as a custom action instead —
 *   supply it, or the action exists only for sighted, dextrous users.
 * @property weight How much of the swept width this action claims when several
 *   share an edge.
 * @property isUndo Whether triggering this action swipes the row back to rest
 *   rather than off-screen.
 */
@Immutable
public data class GlassSwipeAction(
    val onSwipe: () -> Unit,
    val background: Color,
    val icon: @Composable () -> Unit,
    val label: String = "",
    val weight: Double = 1.0,
    val isUndo: Boolean = false,
)

public object GlassSwipeDefaults {
    /** How far the row must travel before releasing commits the action. */
    public val SwipeThreshold: Dp = 56.dp

    /** Size of the icon drawn on a revealed action panel. */
    public val ActionIconSize: Dp = 24.dp
}

/**
 * Wraps [content] in a row that reveals coloured action panels when swiped.
 *
 * The revealed panel is clipped to the row's own [shape], so the colour follows
 * the card's rounded corners instead of squaring them off — without that, the
 * illusion that the card is sliding over something breaks the moment you touch it.
 *
 * Nothing is painted behind the row until the swipe passes the threshold, so a
 * half-committed gesture shows the glass and the scenery through it rather than
 * a block of colour. That is the feedback: colour arriving *is* the commit.
 *
 * Swipe actions are also published as accessibility custom actions, since a
 * gesture that only works with a fingertip is not an affordance for everyone.
 *
 * ```
 * GlassSwipeableRow(
 *     startActions = listOf(deleteAction),
 *     endActions = listOf(callBackAction),
 * ) {
 *     GlassCard { Text("Ada Lovelace") }
 * }
 * ```
 *
 * @param modifier [Modifier] applied to the root Box.
 * @param startActions Actions revealed by swiping from the start (left) edge.
 * @param endActions Actions revealed by swiping from the end (right) edge.
 * @param swipeThreshold Travel required before a release commits.
 * @param shape Outline the revealed panels are clipped to; match the content's shape.
 * @param content The row content, typically a [GlassCard].
 */
@Composable
public fun GlassSwipeableRow(
    modifier: Modifier = Modifier,
    startActions: List<GlassSwipeAction> = emptyList(),
    endActions: List<GlassSwipeAction> = emptyList(),
    swipeThreshold: Dp = GlassSwipeDefaults.SwipeThreshold,
    shape: CornerBasedShape = GlassDefaults.shape(),
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
                .semantics { if (actionSemantics.isNotEmpty()) customActions = actionSemantics },
        startActions = startActions.map { it.toSwipeAction() },
        endActions = endActions.map { it.toSwipeAction() },
        swipeThreshold = swipeThreshold,
        // Transparent until commit: the glass stays readable mid-gesture, and
        // the colour landing is what tells you the action will fire.
        backgroundUntilSwipeThreshold = Color.Transparent,
        content = content,
    )
}

private fun GlassSwipeAction.toSwipeAction(): SwipeAction =
    SwipeAction(
        onSwipe = onSwipe,
        icon = icon,
        background = background,
        weight = weight,
        isUndo = isUndo,
    )

/** Builds a [GlassSwipeAction] from a RikkaUI icon, sized and tinted for the panel. */
@Composable
public fun rememberGlassSwipeAction(
    icon: ImageVector,
    background: Color,
    contentColor: Color,
    label: String,
    onSwipe: () -> Unit,
    weight: Double = 1.0,
    isUndo: Boolean = false,
): GlassSwipeAction {
    val iconSize = GlassSwipeDefaults.ActionIconSize
    return remember(icon, background, contentColor, label, weight, isUndo, onSwipe) {
        GlassSwipeAction(
            onSwipe = onSwipe,
            background = background,
            icon = {
                Box(Modifier.size(iconSize * 2), contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(iconSize))
                }
            },
            label = label,
            weight = weight,
            isUndo = isUndo,
        )
    }
}

// ─── Call history ───────────────────────────────────────────

/** Which way a logged call went. */
public enum class CallDirection {
    Incoming,
    Outgoing,
    Missed,
}

/**
 * One row of call history: a glass card that reveals delete and call-back
 * actions when swiped.
 *
 * Missed calls colour their direction icon with `destructive`, which is the only
 * thing that distinguishes them at a glance in a long list.
 *
 * ```
 * CallHistoryItem(
 *     name = "Ada Lovelace",
 *     detail = "+1 555 0134",
 *     timestamp = "2 min ago",
 *     direction = CallDirection.Missed,
 *     deleteLabel = strings.delete,
 *     callBackLabel = strings.callBack,
 *     onDelete = { log.remove(id) },
 *     onCallBack = { dialer.call(number) },
 * )
 * ```
 *
 * @param name Caller name, or the number when there is no contact.
 * @param modifier [Modifier] applied to the swipeable row.
 * @param detail Secondary line — number, location, or call duration.
 * @param timestamp When the call happened, shown trailing.
 * @param direction Which way the call went; drives the leading icon.
 * @param agentBadge Text for a badge marking this as an agent-handled call.
 *   Empty hides the badge.
 * @param deleteLabel Accessibility label for the delete action; also its custom action name.
 * @param callBackLabel Accessibility label for the call-back action.
 * @param onClick Invoked when the card itself is tapped.
 * @param onDelete Invoked when the row is swiped from the start edge. Null hides the action.
 * @param onCallBack Invoked when the row is swiped from the end edge. Null hides the action.
 * @param backdrop What shows through the card; from [LocalGlassBackdrop] by default.
 */
@Composable
public fun CallHistoryItem(
    name: String,
    modifier: Modifier = Modifier,
    detail: String = "",
    timestamp: String = "",
    direction: CallDirection = CallDirection.Incoming,
    agentBadge: String = "",
    deleteLabel: String = "",
    callBackLabel: String = "",
    onClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onCallBack: (() -> Unit)? = null,
    backdrop: Backdrop = LocalGlassBackdrop.current,
) {
    val colors = RikkaTheme.colors
    val shape = GlassDefaults.shape()

    val deleteAction =
        onDelete?.let {
            rememberGlassSwipeAction(
                icon = RikkaIcons.Trash,
                background = colors.destructive,
                contentColor = colors.onDestructive,
                label = deleteLabel,
                onSwipe = it,
            )
        }
    val callBackAction =
        onCallBack?.let {
            rememberGlassSwipeAction(
                icon = RikkaIcons.Phone,
                background = colors.success,
                contentColor = colors.onSuccess,
                label = callBackLabel,
                onSwipe = it,
            )
        }

    GlassSwipeableRow(
        modifier = modifier,
        startActions = listOfNotNull(deleteAction),
        endActions = listOfNotNull(callBackAction),
        shape = shape,
    ) {
        GlassCard(
            level = GlassLevel.Regular,
            shape = shape,
            backdrop = backdrop,
            onClick = onClick,
            label = name,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(RikkaTheme.spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(fallback = name, size = AvatarSize.Default)

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(RikkaTheme.spacing.xs),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(RikkaTheme.spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DirectionIcon(direction)
                        Text(text = name, variant = TextVariant.Large)
                        if (agentBadge.isNotEmpty()) {
                            Badge(text = agentBadge, variant = BadgeVariant.Secondary, size = BadgeSize.Sm)
                        }
                    }
                    if (detail.isNotEmpty()) {
                        Text(text = detail, variant = TextVariant.Muted)
                    }
                }

                if (timestamp.isNotEmpty()) {
                    Text(text = timestamp, variant = TextVariant.Small, color = colors.onMuted)
                }
            }
        }
    }
}

@Composable
private fun DirectionIcon(direction: CallDirection) {
    val colors = RikkaTheme.colors
    val (icon, tint) =
        when (direction) {
            CallDirection.Incoming -> RikkaIcons.ArrowDown to colors.onMuted
            CallDirection.Outgoing -> RikkaIcons.ArrowUp to colors.onMuted
            CallDirection.Missed -> RikkaIcons.ArrowDown to colors.destructive
        }
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(14.dp),
    )
}
