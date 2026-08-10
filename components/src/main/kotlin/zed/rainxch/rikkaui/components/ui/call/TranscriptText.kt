package zed.rainxch.rikkaui.components.ui.call

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import me.saket.extendedspans.ExtendedSpans
import me.saket.extendedspans.RoundedCornerSpanPainter
import me.saket.extendedspans.SquigglyUnderlineSpanPainter
import me.saket.extendedspans.rememberSquigglyUnderlineAnimator
import zed.rainxch.rikkaui.components.ui.text.Text
import zed.rainxch.rikkaui.components.ui.text.TextVariant
import zed.rainxch.rikkaui.foundation.RikkaTheme
import me.saket.extendedspans.drawBehind as drawSpansBehind

// ─── Span styles ────────────────────────────────────────────

/**
 * Marks a range of a transcript as settled and worth noticing — a name, a time,
 * an extracted entity.
 *
 * Compose paints a `background` span as a hard rectangle that clips at line
 * breaks, which looks like a bug on wrapped text. [TranscriptText] repaints
 * these ranges as continuous rounded pills instead.
 *
 * ```
 * buildAnnotatedString {
 *     append("Booked for ")
 *     withStyle(transcriptHighlight()) { append("Tuesday at 3") }
 * }
 * ```
 *
 * @param color Pill colour behind the range. Translucent by default: a solid
 *   tint either vanishes on a light surface or swallows the text on a dark one.
 */
@Composable
public fun transcriptHighlight(color: Color = RikkaTheme.colors.primary.copy(alpha = 0.25f)): SpanStyle = SpanStyle(background = color)

/**
 * Marks a range of a transcript as *not yet settled* — words the recogniser is
 * still revising.
 *
 * [TranscriptText] draws these with an animated squiggle rather than a flat
 * underline. The motion is the point: it says "still arriving" in a way a static
 * decoration cannot, and it stops the moment the text is finalised and the span
 * is dropped.
 */
public fun transcriptTentative(): SpanStyle = SpanStyle(textDecoration = TextDecoration.Underline)

// ─── Component ──────────────────────────────────────────────

/**
 * Renders live transcript text with span decorations Compose cannot draw itself.
 *
 * Two painters are wired in: rounded pills for [transcriptHighlight] ranges, and
 * an animated squiggle for [transcriptTentative] ranges. Both need the laid-out
 * glyph positions, which is why this needs [Text]'s `onTextLayout` — decorations
 * are drawn behind the text from the measured line boxes, not guessed from the
 * string.
 *
 * ```
 * TranscriptText(
 *     buildAnnotatedString {
 *         append("I can call them back ")
 *         withStyle(transcriptTentative()) { append("this afternoon") }
 *     },
 * )
 * ```
 *
 * @param text Annotated transcript; decorate ranges with [transcriptHighlight]
 *   and [transcriptTentative].
 * @param modifier [Modifier] applied to the text.
 * @param variant [TextVariant] for the transcript body.
 * @param color Explicit text colour; defers to `LocalContentColor` when unspecified.
 *   The squiggle inherits it, so tentative text and its decoration stay in step.
 */
@Composable
public fun TranscriptText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    variant: TextVariant = TextVariant.P,
    color: Color = Color.Unspecified,
) {
    val animator = rememberSquigglyUnderlineAnimator()

    val spans =
        remember(animator) {
            ExtendedSpans(
                RoundedCornerSpanPainter(
                    cornerRadius = 8.sp,
                    padding = RoundedCornerSpanPainter.TextPaddingValues(horizontal = 4.sp),
                    topMargin = 2.sp,
                    bottomMargin = 2.sp,
                ),
                SquigglyUnderlineSpanPainter(
                    width = 2.sp,
                    wavelength = 12.sp,
                    amplitude = 1.sp,
                    bottomOffset = 2.sp,
                    animator = animator,
                ),
            )
        }

    Text(
        text = remember(text, spans) { spans.extend(text).withoutNativeUnderlines() },
        modifier = modifier.drawSpansBehind(spans),
        variant = variant,
        color = color,
        onTextLayout = { spans.onTextLayout(it) },
    )
}

/**
 * Strips `TextDecoration.Underline` while keeping everything else intact.
 *
 * `ExtendedSpans.extend` tags underlined ranges for the squiggle painter but
 * leaves the decoration on the span, so Compose draws its own straight underline
 * *as well* and tentative text ends up double-decorated. The tag lives in a
 * string annotation rather than the span style, so clearing the decoration
 * removes the duplicate without costing the squiggle.
 */
private fun AnnotatedString.withoutNativeUnderlines(): AnnotatedString {
    if (spanStyles.none { it.item.textDecoration == TextDecoration.Underline }) return this

    return buildAnnotatedString {
        append(this@withoutNativeUnderlines.text)
        this@withoutNativeUnderlines.spanStyles.forEach { range ->
            val style =
                if (range.item.textDecoration == TextDecoration.Underline) {
                    range.item.copy(textDecoration = TextDecoration.None)
                } else {
                    range.item
                }
            addStyle(style, range.start, range.end)
        }
        this@withoutNativeUnderlines.paragraphStyles.forEach { addStyle(it.item, it.start, it.end) }
        this@withoutNativeUnderlines
            .getStringAnnotations(0, this@withoutNativeUnderlines.length)
            .forEach { addStringAnnotation(it.tag, it.item, it.start, it.end) }
    }
}
