package zed.rainxch.rikkaui.components.ui.call

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.launch
import zed.rainxch.rikkaui.components.ui.glass.GlassLevel
import zed.rainxch.rikkaui.components.ui.glass.GlassSurface
import zed.rainxch.rikkaui.components.ui.glass.GlassTreatment
import zed.rainxch.rikkaui.components.ui.glass.LocalGlassBackdrop
import zed.rainxch.rikkaui.components.ui.text.Text
import zed.rainxch.rikkaui.foundation.RikkaTheme
import kotlin.math.abs
import kotlin.math.roundToInt

// ─── Defaults ───────────────────────────────────────────────

public object CallDecisionSliderDefaults {
    /** Total height reserved: the horizontal track plus the downward one below it. */
    public val Height: Dp = 210.dp

    /** Diameter of the draggable thumb; also its touch target. */
    public val ThumbSize: Dp = 60.dp

    /** How far the thumb travels left or right before it stops. */
    public val HorizontalLimit: Dp = 118.dp

    /** How far the thumb travels down before it stops. */
    public val DownwardLimit: Dp = 92.dp

    /**
     * Travel needed to commit a decision.
     *
     * Deliberately shorter than the limits: a gesture that has to be driven all
     * the way to the stop reads as unresponsive, and a call is decided under
     * time pressure.
     */
    public val CommitDistance: Dp = 76.dp

    public val TrackHeight: Dp = 68.dp

    public val TransferTrackHeight: Dp = 122.dp

    /** Width of the horizontal track, as a fraction of the control. */
    public val TrackWidthFraction: Float = 0.78f

    public val DeclineLabel: String = "Decline"

    public val AnswerLabel: String = "Answer"

    public val TransferLabel: String = "Agent"

    public val ThumbLabel: String = "Slide"
}

// ─── Component ──────────────────────────────────────────────

/**
 * The three-way slide control for a call that is still being decided.
 *
 * Slide the thumb left to decline, right to answer, or down to hand the call to
 * an agent. This is the lock-screen presentation of an incoming call: a drag is
 * deliberate in a way a tap is not, which is what keeps a pocket from answering.
 *
 * Because a drag is also unusable one-handed — and unusable at all for anyone
 * who cannot make it — the same three decisions are published as custom
 * accessibility actions. They are the control, not a courtesy; localise their
 * labels along with the visible ones.
 *
 * The tracks and the thumb are glass rather than fills: they sit over whatever
 * the call arrived on top of. The thumb takes [GlassTreatment.Smoked] because it
 * is the one piece that must stay a solid object under the finger, dense enough
 * to read as the primary action over arbitrary scenery.
 *
 * ```
 * CallDecisionSlider(
 *     onDecline = { call.decline() },
 *     onAnswer = { call.answer() },
 *     onTransfer = { call.sendToAgent() },
 *     declineLabel = strings.decline,
 *     answerLabel = strings.answer,
 *     transferLabel = strings.agent,
 * )
 * ```
 *
 * @param onDecline Invoked when the thumb is committed to the start edge.
 * @param onAnswer Invoked when the thumb is committed to the end edge.
 * @param onTransfer Invoked when the thumb is committed downward.
 * @param modifier [Modifier] applied to the root Box.
 * @param enabled Whether the control accepts the gesture and its actions.
 * @param declineLabel Visible label at the start edge.
 * @param answerLabel Visible label at the end edge.
 * @param transferLabel Visible label below the downward track.
 * @param thumbLabel Short verb drawn on the thumb itself.
 * @param contentDescription What the whole control announces. The default names
 *   the three directions using the visible labels.
 * @param declineActionLabel Name of the custom accessibility action that declines.
 * @param answerActionLabel Name of the custom accessibility action that answers.
 * @param transferActionLabel Name of the custom accessibility action that transfers.
 * @param trackTint Colour washed over the horizontal decline/answer track.
 * @param transferTrackTint Colour washed over the downward transfer track.
 * @param thumbTint Colour the thumb's smoked glass is dyed with.
 * @param thumbContentColor Colour of [thumbLabel] on the thumb.
 * @param backdrop What shows through; taken from [LocalGlassBackdrop] by default.
 */
@Composable
public fun CallDecisionSlider(
    onDecline: () -> Unit,
    onAnswer: () -> Unit,
    onTransfer: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    declineLabel: String = CallDecisionSliderDefaults.DeclineLabel,
    answerLabel: String = CallDecisionSliderDefaults.AnswerLabel,
    transferLabel: String = CallDecisionSliderDefaults.TransferLabel,
    thumbLabel: String = CallDecisionSliderDefaults.ThumbLabel,
    contentDescription: String =
        "Incoming call control. Slide left to $declineLabel, right to $answerLabel, or down for $transferLabel.",
    declineActionLabel: String = declineLabel,
    answerActionLabel: String = answerLabel,
    transferActionLabel: String = transferLabel,
    trackTint: Color = RikkaTheme.colors.surface,
    transferTrackTint: Color = RikkaTheme.colors.primaryTinted,
    thumbTint: Color = RikkaTheme.colors.primary,
    thumbContentColor: Color = RikkaTheme.colors.onPrimary,
    backdrop: Backdrop = LocalGlassBackdrop.current,
) {
    val colors = RikkaTheme.colors
    val motion = RikkaTheme.motion
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val horizontalLimit = with(density) { CallDecisionSliderDefaults.HorizontalLimit.toPx() }
    val downwardLimit = with(density) { CallDecisionSliderDefaults.DownwardLimit.toPx() }
    val commitDistance = with(density) { CallDecisionSliderDefaults.CommitDistance.toPx() }

    // An Animatable rather than plain state: a gesture released short of the
    // commit distance springs home on the theme's own spatial curve instead of
    // teleporting, and the value is read in the placement phase so dragging
    // recomposes nothing.
    val drag = remember { Animatable(Offset.Zero, Offset.VectorConverter) }

    fun release() {
        scope.launch { drag.animateTo(Offset.Zero, motion.spatialDefault()) }
    }

    fun commit(action: () -> Unit) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        action()
        release()
    }

    val emphasis = remember { TextStyle(fontWeight = FontWeight.SemiBold) }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(CallDecisionSliderDefaults.Height)
                .semantics(mergeDescendants = true) {
                    this.contentDescription = contentDescription
                    if (!enabled) disabled()
                    customActions =
                        listOf(
                            CustomAccessibilityAction(declineActionLabel) {
                                onDecline()
                                true
                            },
                            CustomAccessibilityAction(answerActionLabel) {
                                onAnswer()
                                true
                            },
                            CustomAccessibilityAction(transferActionLabel) {
                                onTransfer()
                                true
                            },
                        )
                }.pointerInput(enabled, onDecline, onAnswer, onTransfer) {
                    if (!enabled) return@pointerInput
                    detectDragGestures(
                        onDragCancel = { release() },
                        onDragEnd = {
                            val offset = drag.value
                            when {
                                offset.x <= -commitDistance -> commit(onDecline)
                                offset.x >= commitDistance -> commit(onAnswer)
                                offset.y >= commitDistance -> commit(onTransfer)
                                else -> release()
                            }
                        },
                    ) { change, amount ->
                        change.consume()
                        // One axis at a time: a diagonal drag that leaked into
                        // both would leave the thumb between two decisions.
                        val candidate = drag.value + amount
                        val next =
                            if (abs(candidate.x) > abs(candidate.y)) {
                                Offset(candidate.x.coerceIn(-horizontalLimit, horizontalLimit), 0f)
                            } else {
                                Offset(0f, candidate.y.coerceIn(0f, downwardLimit))
                            }
                        scope.launch { drag.snapTo(next) }
                    }
                },
    ) {
        GlassSurface(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 38.dp)
                    .fillMaxWidth(CallDecisionSliderDefaults.TrackWidthFraction)
                    .height(CallDecisionSliderDefaults.TrackHeight),
            level = GlassLevel.Subtle,
            shape = CircleShape,
            backdrop = backdrop,
            tint = trackTint,
            contentPadding = PaddingValues(0.dp),
        ) {}
        GlassSurface(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 70.dp)
                    .width(CallDecisionSliderDefaults.TrackHeight)
                    .height(CallDecisionSliderDefaults.TransferTrackHeight),
            level = GlassLevel.Subtle,
            shape = CircleShape,
            backdrop = backdrop,
            tint = transferTrackTint,
            contentPadding = PaddingValues(0.dp),
        ) {}

        Text(
            text = declineLabel,
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = RikkaTheme.spacing.lg, top = 62.dp),
            color = colors.destructive,
            style = emphasis,
        )
        Text(
            text = answerLabel,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = RikkaTheme.spacing.lg, top = 62.dp),
            color = colors.success,
            style = emphasis,
        )
        Text(
            text = transferLabel,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp),
            color = colors.onPrimaryTinted,
            style = emphasis,
        )

        // The thumb's drop shadow comes from the prominent glass level rather
        // than a hand-set elevation, so it seats itself the way every other
        // floating surface in the theme does.
        GlassSurface(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 42.dp)
                    .offset { IntOffset(drag.value.x.roundToInt(), drag.value.y.roundToInt()) }
                    .size(CallDecisionSliderDefaults.ThumbSize),
            level = GlassLevel.Prominent,
            shape = CircleShape,
            backdrop = backdrop,
            tint = thumbTint,
            treatment = GlassTreatment.Smoked,
            contentColor = thumbContentColor,
            contentPadding = PaddingValues(0.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = thumbLabel, style = emphasis)
        }
    }
}
