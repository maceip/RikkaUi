package zed.rainxch.rikkaui.components.ui.call

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kyant.backdrop.Backdrop
import zed.rainxch.rikkaui.components.ui.avatar.Avatar
import zed.rainxch.rikkaui.components.ui.avatar.AvatarAnimation
import zed.rainxch.rikkaui.components.ui.avatar.AvatarSize
import zed.rainxch.rikkaui.components.ui.badge.Badge
import zed.rainxch.rikkaui.components.ui.badge.BadgeSize
import zed.rainxch.rikkaui.components.ui.badge.BadgeVariant
import zed.rainxch.rikkaui.components.ui.glass.GlassCard
import zed.rainxch.rikkaui.components.ui.glass.GlassDefaults
import zed.rainxch.rikkaui.components.ui.glass.GlassLevel
import zed.rainxch.rikkaui.components.ui.glass.LocalGlassBackdrop
import zed.rainxch.rikkaui.components.ui.icon.Icon
import zed.rainxch.rikkaui.components.ui.icon.IconSize
import zed.rainxch.rikkaui.components.ui.icon.RikkaIcons
import zed.rainxch.rikkaui.components.ui.swipeable.SwipeableRow
import zed.rainxch.rikkaui.components.ui.swipeable.rememberSwipeableRowAction
import zed.rainxch.rikkaui.components.ui.text.Text
import zed.rainxch.rikkaui.components.ui.text.TextVariant
import zed.rainxch.rikkaui.foundation.RikkaTheme

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
 * @param badge Optional status badge text (e.g. agent outcome, "NEW"). Empty hides it.
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
    badge: String = "",
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
            rememberSwipeableRowAction(
                icon = RikkaIcons.Trash,
                background = colors.destructive,
                contentColor = colors.onDestructive,
                label = deleteLabel,
                onSwipe = it,
            )
        }
    val callBackAction =
        onCallBack?.let {
            rememberSwipeableRowAction(
                icon = RikkaIcons.Phone,
                background = colors.success,
                contentColor = colors.onSuccess,
                label = callBackLabel,
                onSwipe = it,
            )
        }

    SwipeableRow(
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
                // `fallback` renders verbatim, so it must be initials, not the
                // name. No entrance animation either: a row in a list should not
                // fade its avatar in on its own — the list's enter animation owns that.
                Avatar(
                    fallback = initialsOf(name),
                    size = AvatarSize.Default,
                    animation = AvatarAnimation.None,
                    label = name,
                )

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
                        if (badge.isNotEmpty()) {
                            Badge(text = badge, variant = BadgeVariant.Secondary, size = BadgeSize.Sm)
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
        size = IconSize.Xs,
    )
}

/**
 * First letters of the first and last word, capitalised — "Ada Lovelace" to "AL".
 *
 * [Avatar] renders its `fallback` verbatim, so a full name must be reduced
 * before it gets there or it wraps and clips inside the circle.
 */
public fun initialsOf(name: String): String {
    val words = name.trim().split(' ', '\t').filter { it.isNotEmpty() }
    return when (words.size) {
        0 -> ""
        1 -> words[0].take(1).uppercase()
        else -> (words.first().take(1) + words.last().take(1)).uppercase()
    }
}
