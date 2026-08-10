package zed.rainxch.rikkaui.components.ui.call

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import zed.rainxch.rikkaui.components.ui.glass.GlassCapability
import zed.rainxch.rikkaui.components.ui.glass.GlassLevel
import zed.rainxch.rikkaui.components.ui.glass.GlassStyle
import zed.rainxch.rikkaui.components.ui.glass.LocalGlassBackdrop
import zed.rainxch.rikkaui.components.ui.glass.glassSurface
import zed.rainxch.rikkaui.components.ui.glass.rememberGlassPressState
import zed.rainxch.rikkaui.components.ui.glass.rememberGlassStyle
import zed.rainxch.rikkaui.components.ui.text.Text
import zed.rainxch.rikkaui.components.ui.text.TextVariant
import zed.rainxch.rikkaui.foundation.RikkaTheme

// ─── Model ──────────────────────────────────────────────────

/**
 * One key on a [GlassDialpad].
 *
 * @property digit The character this key enters.
 * @property letters The letters printed under the digit, as on a phone keypad.
 *   Empty for keys that carry none.
 * @property longPressDigit Character entered on a long press instead of [digit],
 *   which is how `0` becomes `+`. Null disables long press for this key.
 */
@Immutable
public data class DialpadKey(
    val digit: Char,
    val letters: String = "",
    val longPressDigit: Char? = null,
)

public object GlassDialpadDefaults {
    /** The standard 12-key telephone layout, letters included. */
    public val Keys: List<DialpadKey> =
        listOf(
            DialpadKey('1'),
            DialpadKey('2', "ABC"),
            DialpadKey('3', "DEF"),
            DialpadKey('4', "GHI"),
            DialpadKey('5', "JKL"),
            DialpadKey('6', "MNO"),
            DialpadKey('7', "PQRS"),
            DialpadKey('8', "TUV"),
            DialpadKey('9', "WXYZ"),
            DialpadKey('*'),
            DialpadKey('0', "+", longPressDigit = '+'),
            DialpadKey('#'),
        )

    public val KeySize: Dp = 72.dp
}

// ─── Component ──────────────────────────────────────────────

/**
 * A dial pad of circular glass keys.
 *
 * Each key is a lens rather than a button: it refracts whatever the pad is laid
 * over, lit by a radial gradient offset toward the theme's light source so the
 * grid reads as a tray of glass beads lit from one direction rather than twelve
 * independently glowing discs.
 *
 * Pressing a key sinks it, brightens it, and refracts harder — all in the draw
 * and layer phases, so a fast dialer does not recompose per keystroke. Keys with
 * a [DialpadKey.longPressDigit] fire a distinct haptic on the long press, which
 * is the only feedback that tells you `0` turned into `+` without looking.
 *
 * ```
 * var number by remember { mutableStateOf("") }
 *
 * GlassDialpad(onKeyPress = { number += it })
 * ```
 *
 * @param onKeyPress Invoked with the entered character — [DialpadKey.digit], or
 *   [DialpadKey.longPressDigit] when the key was held.
 * @param modifier [Modifier] applied to the root Column.
 * @param keys Key layout, chunked into rows of three. Defaults to the standard pad.
 * @param keySize Diameter of each key; also its touch target.
 * @param enabled Whether keys respond to input.
 * @param level [GlassLevel] for the keys. Subtle by default — keys sit *on* the
 *   dialer surface rather than floating above it.
 * @param backdrop What shows through; taken from [LocalGlassBackdrop] by default.
 * @param spacing Gap between keys, horizontally and vertically.
 */
@Composable
public fun GlassDialpad(
    onKeyPress: (Char) -> Unit,
    modifier: Modifier = Modifier,
    keys: List<DialpadKey> = GlassDialpadDefaults.Keys,
    keySize: Dp = GlassDialpadDefaults.KeySize,
    enabled: Boolean = true,
    level: GlassLevel = GlassLevel.Subtle,
    backdrop: Backdrop = LocalGlassBackdrop.current,
    spacing: Dp = RikkaTheme.spacing.lg,
) {
    val style = rememberGlassStyle(level = level)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        keys.chunked(3).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                row.forEach { key ->
                    DialpadKeyButton(
                        key = key,
                        size = keySize,
                        style = style,
                        backdrop = backdrop,
                        enabled = enabled,
                        onKeyPress = onKeyPress,
                    )
                }
            }
        }
    }
}

// ─── Key ────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DialpadKeyButton(
    key: DialpadKey,
    size: Dp,
    style: GlassStyle,
    backdrop: Backdrop,
    enabled: Boolean,
    onKeyPress: (Char) -> Unit,
) {
    val haptics = LocalHapticFeedback.current

    val interactionSource = remember { MutableInteractionSource() }
    val press = rememberGlassPressState(interactionSource, enabled = enabled)

    // Glass keys are laid over arbitrary scenery, so the glyph is white with a
    // shadow rather than a theme colour — the shadow is what keeps it readable
    // when the backdrop behind a key happens to be pale.
    val glyphStyle =
        TextStyle(
            fontWeight = FontWeight.Light,
            shadow =
                Shadow(
                    color = Color.Black.copy(alpha = 0.35f),
                    offset = Offset(0f, 1f),
                    blurRadius = 6f,
                ),
        )

    val description = if (key.letters.isEmpty()) key.digit.toString() else "${key.digit} ${key.letters}"

    Box(
        modifier =
            Modifier
                .size(size)
                .semantics {
                    contentDescription = description
                    role = Role.Button
                }.glassSurface(
                    backdrop = backdrop,
                    style = style,
                    shape = CircleShape,
                    pressFraction = press.pressFraction,
                    layerBlock = press.layerBlock,
                ).drawWithCache {
                    // Built once per size change, then only re-run on draw — the
                    // press brightness reads its fraction inside the draw block.
                    val lit =
                        Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.28f), Color.Transparent),
                            center = Offset(this.size.width * 0.32f, this.size.height * 0.26f),
                            radius = this.size.maxDimension * 0.75f,
                        )
                    onDrawBehind {
                        drawCircle(lit)
                        val brightness = press.pressFraction() * 0.22f
                        if (brightness > 0f) drawCircle(Color.White.copy(alpha = brightness))
                    }
                }.border(
                    width = 1.dp,
                    brush =
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = if (style.capability == GlassCapability.None) 0f else 0.45f),
                                Color.White.copy(alpha = 0f),
                            ),
                        ),
                    shape = CircleShape,
                ).clip(CircleShape)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onLongClick =
                        key.longPressDigit?.let { long ->
                            {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onKeyPress(long)
                            }
                        },
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onKeyPress(key.digit)
                    },
                ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = key.digit.toString(),
                variant = TextVariant.H3,
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                style = glyphStyle,
            )
            if (key.letters.isNotEmpty()) {
                Text(
                    text = key.letters,
                    color = Color.White.copy(alpha = if (enabled) 0.7f else 0.3f),
                    textAlign = TextAlign.Center,
                    style = glyphStyle.copy(fontSize = 10.sp, letterSpacing = 1.sp),
                )
            }
        }
    }
}
