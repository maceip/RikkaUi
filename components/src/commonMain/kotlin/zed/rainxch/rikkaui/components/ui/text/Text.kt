package zed.rainxch.rikkaui.components.ui.text

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import zed.rainxch.rikkaui.foundation.LocalContentColor
import zed.rainxch.rikkaui.foundation.LocalTextStyle
import zed.rainxch.rikkaui.foundation.RikkaTheme

/** Typography variant mapping to a theme text style. */
public enum class TextVariant {
    /** Largest heading */
    H1,

    /** Second-level heading */
    H2,

    /** Third-level heading */
    H3,

    /** Fourth-level heading */
    H4,

    /** Body paragraph (default) */
    P,

    /** Lead paragraph, muted color */
    Lead,

    /** Large emphasis text */
    Large,

    /** Small text */
    Small,

    /** Muted/secondary text */
    Muted,
}

/**
 * Themed text component that resolves typography and color from [RikkaTheme] tokens.
 *
 * Color resolution order: explicit [color] > [LocalContentColor] > variant default.
 * Heading variants (H1-H4) automatically receive heading accessibility semantics.
 *
 * ```
 * Text("Hello, Rikka!", variant = TextVariant.H2)
 *
 * Text(
 *     text = "Muted caption",
 *     variant = TextVariant.Muted,
 *     selectable = true,
 * )
 * ```
 *
 * @param text The text string to display.
 * @param modifier [Modifier] applied to the underlying [BasicText].
 * @param variant [TextVariant] that maps to a theme typography style (e.g. H1, P, Muted).
 * @param color Explicit text color; [Color.Unspecified] defers to [LocalContentColor] or variant default.
 * @param textAlign Horizontal text alignment; null uses the style default.
 * @param overflow How visual overflow is handled (e.g. [TextOverflow.Ellipsis]).
 * @param maxLines Maximum number of lines before truncation.
 * @param minLines Minimum number of lines to occupy.
 * @param selectable Whether the text can be selected by the user.
 * @param style Additional [TextStyle] merged on top of the variant style.
 * @param onTextLayout Invoked when the text is laid out. Span painters that draw
 *   their own decorations need the resulting [TextLayoutResult] to know where
 *   the glyphs landed.
 */
@Composable
public fun Text(
    text: String,
    modifier: Modifier = Modifier,
    variant: TextVariant = TextVariant.P,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    selectable: Boolean = false,
    style: TextStyle = TextStyle.Default,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
) {
    SelectionScope(selectable) {
        BasicText(
            text = text,
            modifier = modifier.headingSemantics(variant),
            style = resolveTextStyle(variant, color, textAlign, style),
            onTextLayout = onTextLayout,
            overflow = overflow,
            maxLines = maxLines,
            minLines = minLines,
        )
    }
}

/**
 * Themed text component for [AnnotatedString], so a single string can carry
 * per-range styling, links, and inline content.
 *
 * Same token resolution as the [String] overload — the annotated spans layer on
 * top of the resolved variant style rather than replacing it, so a highlighted
 * range inside a `Muted` paragraph is still muted-sized and muted-coloured.
 *
 * ```
 * Text(
 *     buildAnnotatedString {
 *         append("Call from ")
 *         withStyle(SpanStyle(background = RikkaTheme.colors.primaryTinted)) {
 *             append("Ada")
 *         }
 *     },
 *     variant = TextVariant.Small,
 * )
 * ```
 *
 * @param text The annotated text to display.
 * @param modifier [Modifier] applied to the underlying [BasicText].
 * @param variant [TextVariant] that maps to a theme typography style.
 * @param color Explicit text color; [Color.Unspecified] defers to [LocalContentColor] or variant default.
 * @param textAlign Horizontal text alignment; null uses the style default.
 * @param overflow How visual overflow is handled.
 * @param maxLines Maximum number of lines before truncation.
 * @param minLines Minimum number of lines to occupy.
 * @param selectable Whether the text can be selected by the user.
 * @param style Additional [TextStyle] merged on top of the variant style.
 * @param inlineContent Composables substituted for [androidx.compose.foundation.text.InlineTextContent]
 *   placeholders in [text], keyed by the annotation tag.
 * @param onTextLayout Invoked when the text is laid out; required by span painters.
 */
@Composable
public fun Text(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    variant: TextVariant = TextVariant.P,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    selectable: Boolean = false,
    style: TextStyle = TextStyle.Default,
    inlineContent: Map<String, InlineTextContent> = emptyMap(),
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
) {
    SelectionScope(selectable) {
        BasicText(
            text = text,
            modifier = modifier.headingSemantics(variant),
            style = resolveTextStyle(variant, color, textAlign, style),
            onTextLayout = onTextLayout,
            overflow = overflow,
            maxLines = maxLines,
            minLines = minLines,
            inlineContent = inlineContent,
        )
    }
}

// ─── Internal ───────────────────────────────────────────────

/**
 * Merge order: parent default → variant override → explicit style → color → alignment.
 *
 * A parent (Card, Button) provides a default text style. The variant (H1, P, …)
 * overrides it so headings inside cards still render at heading size. The
 * explicit `style` param wins over everything.
 */
@Composable
private fun resolveTextStyle(
    variant: TextVariant,
    color: Color,
    textAlign: TextAlign?,
    style: TextStyle,
): TextStyle {
    val contentColor = LocalContentColor.current
    val resolvedColor =
        when {
            color != Color.Unspecified -> color
            contentColor != Color.Unspecified -> contentColor
            else -> variantColor(variant)
        }

    val textAlignStyle = if (textAlign != null) TextStyle(textAlign = textAlign) else TextStyle.Default

    return LocalTextStyle.current
        .merge(variantStyle(variant))
        .merge(style)
        .merge(TextStyle(color = resolvedColor))
        .merge(textAlignStyle)
}

/**
 * Heading variants get accessibility heading semantics, enabling screen reader
 * heading navigation (swipe up/down on TalkBack, rotor on VoiceOver).
 */
private fun Modifier.headingSemantics(variant: TextVariant): Modifier =
    when (variant) {
        TextVariant.H1, TextVariant.H2, TextVariant.H3, TextVariant.H4 -> semantics { heading() }
        else -> this
    }

@Composable
private fun SelectionScope(
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    if (enabled) SelectionContainer { content() } else content()
}

@Composable
private fun variantStyle(variant: TextVariant): TextStyle =
    when (variant) {
        TextVariant.H1 -> RikkaTheme.typography.h1
        TextVariant.H2 -> RikkaTheme.typography.h2
        TextVariant.H3 -> RikkaTheme.typography.h3
        TextVariant.H4 -> RikkaTheme.typography.h4
        TextVariant.P -> RikkaTheme.typography.p
        TextVariant.Lead -> RikkaTheme.typography.lead
        TextVariant.Large -> RikkaTheme.typography.large
        TextVariant.Small -> RikkaTheme.typography.small
        TextVariant.Muted -> RikkaTheme.typography.muted
    }

@Composable
private fun variantColor(variant: TextVariant): Color =
    when (variant) {
        TextVariant.Lead -> RikkaTheme.colors.onMuted
        TextVariant.Muted -> RikkaTheme.colors.onMuted
        else -> RikkaTheme.colors.onBackground
    }
