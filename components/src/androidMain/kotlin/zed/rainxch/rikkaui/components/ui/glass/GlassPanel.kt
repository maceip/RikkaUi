package zed.rainxch.rikkaui.components.ui.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import zed.rainxch.rikkaui.foundation.LocalContentColor
import zed.rainxch.rikkaui.foundation.RikkaTheme

/**
 * A detached slab of glass for chrome: bottom bars, sheets, and dialogs.
 *
 * Defaults to [GlassLevel.Prominent] because chrome has to stay legible over
 * whatever scrolls beneath it, and the strongest blur is what buys that
 * legibility without going opaque.
 *
 * Panels carry no default padding — a navigation bar wants its items flush to
 * the edges, and a sheet supplies its own insets. Pass [contentPadding] when the
 * content is plain.
 *
 * Unlike the other glass components, a panel assumes it *hosts* glass: bars and
 * sheets are exactly where buttons and chips end up. It therefore records what
 * it drew by default, so a [GlassButton] inside it refracts the panel rather
 * than the scene the panel already blurred. Turn [hostsGlass] off to save the
 * extra layer when the panel holds only plain content.
 *
 * ```
 * GlassPanel(
 *     modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
 *     shape = RoundedCornerShape(28.dp),
 * ) {
 *     Row { /* tabs */ }
 * }
 * ```
 *
 * @param modifier [Modifier] applied to the root Box.
 * @param level [GlassLevel] controlling blur, refraction, tint, and shadow depth.
 * @param shape Surface outline; must be corner-based.
 * @param backdrop What shows through; taken from [LocalGlassBackdrop] by default.
 * @param tint Colour washed over the refracted backdrop.
 * @param contentColor Colour provided to children through [LocalContentColor].
 * @param contentPadding Padding between the panel edge and its content.
 * @param hostsGlass Whether the panel records what it drew for nested glass to
 *   refract; see [GlassSurface].
 * @param content [BoxScope] content lambda.
 */
@Composable
public fun GlassPanel(
    modifier: Modifier = Modifier,
    level: GlassLevel = GlassLevel.Prominent,
    shape: CornerBasedShape = RoundedCornerShape(28.dp),
    backdrop: Backdrop = LocalGlassBackdrop.current,
    tint: Color = RikkaTheme.glass.tint,
    contentColor: Color = RikkaTheme.colors.onSurface,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    hostsGlass: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val style = rememberGlassStyle(level = level, tint = tint)
    val exported = if (hostsGlass) rememberGlassBackdrop() else null

    GlassContentScope(contentColor = contentColor, nestedBackdrop = exported ?: backdrop) {
        Box(
            modifier =
                modifier
                    .glassSurface(
                        backdrop = backdrop,
                        style = style,
                        shape = shape,
                        exportedBackdrop = exported,
                    ).clip(shape)
                    .padding(contentPadding),
            content = content,
        )
    }
}
