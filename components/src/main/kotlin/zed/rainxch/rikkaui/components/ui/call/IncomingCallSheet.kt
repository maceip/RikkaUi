package zed.rainxch.rikkaui.components.ui.call

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import zed.rainxch.rikkaui.components.ui.avatar.Avatar
import zed.rainxch.rikkaui.components.ui.avatar.AvatarAnimation
import zed.rainxch.rikkaui.components.ui.avatar.AvatarSize
import zed.rainxch.rikkaui.components.ui.glass.GlassButton
import zed.rainxch.rikkaui.components.ui.glass.GlassChip
import zed.rainxch.rikkaui.components.ui.glass.GlassLevel
import zed.rainxch.rikkaui.components.ui.glass.GlassPanel
import zed.rainxch.rikkaui.components.ui.glass.GlassSurface
import zed.rainxch.rikkaui.components.ui.glass.GlassTreatment
import zed.rainxch.rikkaui.components.ui.glass.LocalGlassBackdrop
import zed.rainxch.rikkaui.components.ui.icon.Icon
import zed.rainxch.rikkaui.components.ui.icon.RikkaIcons
import zed.rainxch.rikkaui.components.ui.text.Text
import zed.rainxch.rikkaui.components.ui.text.TextVariant
import zed.rainxch.rikkaui.foundation.RikkaTheme

// ─── Model ──────────────────────────────────────────────────

/** Who said a line of an [IncomingCallSheet] transcript. */
public enum class TranscriptSpeaker {
    /** The agent handling the call. Bubbles lean start-aligned and tinted. */
    Agent,

    /** The person on the other end. Bubbles lean end-aligned and neutral. */
    Caller,
}

/**
 * One line of a live call transcript.
 *
 * @property text The spoken line. Decorate ranges with [transcriptHighlight] for
 *   settled entities and [transcriptTentative] for words still being revised.
 * @property speaker Who said it.
 */
@Immutable
public data class TranscriptLine(
    val text: AnnotatedString,
    val speaker: TranscriptSpeaker,
)

public object IncomingCallSheetDefaults {
    /** Sheets round only their top corners — the bottom runs off-screen. */
    public val Shape: RoundedCornerShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    public val MaxBubbleWidth: Dp = 280.dp
}

// ─── Component ──────────────────────────────────────────────

/**
 * A frosted glass sheet for an incoming call, with the agent's live transcript
 * above the answer controls.
 *
 * The sheet is a [GlassPanel] at [GlassLevel.Prominent], which is what keeps a
 * scrolling transcript legible over whatever the call arrived on top of. Because
 * the panel hosts glass, the buttons and chips inside it refract *the sheet*
 * rather than the screen behind it — so the controls read as cut from the same
 * slab instead of floating in front of it.
 *
 * Answer and decline use colour-dense smoked glass in `success` and
 * `destructive`: the backdrop still samples through them, but cannot overwhelm
 * their meaning. Each also takes an explicit label, because
 * "the green one" is not an instruction a screen reader can follow.
 *
 * ```
 * IncomingCallSheet(
 *     callerName = "Ada Lovelace",
 *     callerDetail = "+1 555 0134",
 *     transcript = transcript,
 *     quickReplies = listOf("Call you back", "In a meeting"),
 *     answerLabel = strings.answer,
 *     declineLabel = strings.decline,
 *     onAnswer = { call.answer() },
 *     onDecline = { call.decline() },
 *     onQuickReply = { call.sendReply(it) },
 * )
 * ```
 *
 * @param callerName Who is calling.
 * @param onAnswer Invoked when the call is answered.
 * @param onDecline Invoked when the call is declined.
 * @param answerLabel Accessibility label for the answer button.
 * @param declineLabel Accessibility label for the decline button.
 * @param modifier [Modifier] applied to the sheet panel.
 * @param callerDetail Secondary line — number, or why the agent picked up.
 * @param transcript Live transcript lines, oldest first.
 * @param quickReplies Short canned replies offered as chips. Empty hides the row.
 * @param onQuickReply Invoked with the chosen reply. Required for the chips to be interactive.
 * @param visible Whether the sheet is shown; drives the slide-and-fade transition.
 * @param backdrop What shows through the sheet; from [LocalGlassBackdrop] by default.
 * @param header Optional slot replacing the default avatar-and-name header.
 * @param content Optional slot for call state the sheet cannot know about —
 *   screening status, a carrier-capability notice, an expandable automation
 *   panel. Sits between the transcript and the actions, in the sheet's own
 *   column spacing.
 * @param actions Optional slot replacing the default decline-then-answer row.
 *   A call that has to be decided from the lock screen, or one whose actions are
 *   gated with a reason, needs different controls in that position; everything
 *   above it stays the same. When supplied, [onAnswer], [onDecline] and their
 *   labels are the slot's to use or ignore.
 */
@Composable
public fun IncomingCallSheet(
    callerName: String,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    answerLabel: String,
    declineLabel: String,
    modifier: Modifier = Modifier,
    callerDetail: String = "",
    transcript: List<TranscriptLine> = emptyList(),
    quickReplies: List<String> = emptyList(),
    onQuickReply: ((String) -> Unit)? = null,
    visible: Boolean = true,
    backdrop: Backdrop = LocalGlassBackdrop.current,
    header: (@Composable ColumnScope.() -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
    actions: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val colors = RikkaTheme.colors
    val spacing = RikkaTheme.spacing
    val motion = RikkaTheme.motion

    AnimatedVisibility(
        visible = visible,
        enter =
            slideInVertically(animationSpec = tween(motion.durationEnter), initialOffsetY = { it }) + fadeIn(tween(motion.durationEnter)),
        exit = fadeOut(tween(motion.durationDefault)),
    ) {
        GlassPanel(
            modifier = modifier.fillMaxWidth(),
            shape = IncomingCallSheetDefaults.Shape,
            backdrop = backdrop,
            contentPadding = PaddingValues(spacing.xl),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
                if (header != null) {
                    header()
                } else {
                    CallerHeader(callerName = callerName, callerDetail = callerDetail)
                }

                if (transcript.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        transcript.forEach { line -> TranscriptBubble(line) }
                    }
                }

                if (quickReplies.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        quickReplies.forEach { reply ->
                            GlassChip(
                                onClick = onQuickReply?.let { send -> { send(reply) } },
                                label = reply,
                            ) {
                                Text(text = reply, variant = TextVariant.Small)
                            }
                        }
                    }
                }

                content?.invoke(this)

                if (actions != null) {
                    actions()
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.md),
                    ) {
                        GlassButton(
                            onClick = onDecline,
                            modifier = Modifier.weight(1f),
                            level = GlassLevel.Regular,
                            tint = colors.destructive,
                            treatment = GlassTreatment.Smoked,
                            contentColor = colors.onDestructive,
                            label = declineLabel,
                        ) {
                            Icon(imageVector = RikkaIcons.X, contentDescription = null)
                            Text(text = declineLabel)
                        }
                        GlassButton(
                            onClick = onAnswer,
                            modifier = Modifier.weight(1f),
                            level = GlassLevel.Regular,
                            tint = colors.success,
                            treatment = GlassTreatment.Smoked,
                            contentColor = colors.onSuccess,
                            label = answerLabel,
                        ) {
                            Icon(imageVector = RikkaIcons.Phone, contentDescription = null)
                            Text(text = answerLabel)
                        }
                    }
                }
            }
        }
    }
}

// ─── Internal ───────────────────────────────────────────────

@Composable
private fun CallerHeader(
    callerName: String,
    callerDetail: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(RikkaTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(
            fallback = initialsOf(callerName),
            size = AvatarSize.Lg,
            animation = AvatarAnimation.None,
            label = callerName,
        )
        Column(verticalArrangement = Arrangement.spacedBy(RikkaTheme.spacing.xs)) {
            Text(text = callerName, variant = TextVariant.H3)
            if (callerDetail.isNotEmpty()) {
                Text(text = callerDetail, variant = TextVariant.Muted)
            }
        }
    }
}

/**
 * One transcript line as a glass bubble.
 *
 * Agent lines sit at [GlassLevel.Subtle] and tint toward the accent; caller
 * lines stay neutral. Both are glass rather than opaque, so a fast-arriving
 * transcript reads as layers accumulating on the sheet instead of a chat log
 * pasted over it.
 */
@Composable
private fun TranscriptBubble(line: TranscriptLine) {
    val colors = RikkaTheme.colors
    val isAgent = line.speaker == TranscriptSpeaker.Agent

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isAgent) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        GlassSurface(
            modifier = Modifier.widthIn(max = IncomingCallSheetDefaults.MaxBubbleWidth),
            level = GlassLevel.Subtle,
            shape = RoundedCornerShape(18.dp),
            tint = if (isAgent) colors.primary else Color.White,
            contentColor = if (isAgent) colors.onPrimary else colors.onSurface,
            contentPadding =
                PaddingValues(
                    horizontal = RikkaTheme.spacing.md,
                    vertical = RikkaTheme.spacing.sm,
                ),
        ) {
            TranscriptText(text = line.text, variant = TextVariant.Small)
        }
    }
}
