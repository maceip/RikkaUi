package zed.rainxch.rikkaui.components.ui.navigationbar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.kyant.backdrop.Backdrop
import zed.rainxch.rikkaui.components.ui.glass.GlassButtonDefaults
import zed.rainxch.rikkaui.components.ui.glass.GlassLevel
import zed.rainxch.rikkaui.components.ui.glass.GlassPanel
import zed.rainxch.rikkaui.components.ui.glass.LocalGlassBackdrop
import zed.rainxch.rikkaui.components.ui.glass.glassSurface
import zed.rainxch.rikkaui.components.ui.glass.rememberGlassStyle
import zed.rainxch.rikkaui.foundation.RikkaTheme
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

// ─── Tuning ─────────────────────────────────────────────────

/**
 * Ceiling on the indicator's travel deformation, as a fraction of its size.
 *
 * Past roughly eight percent the pill stops reading as a heavy liquid and starts
 * reading as rubber, so the stretch saturates here rather than tracking velocity
 * all the way up.
 */
private const val INDICATOR_MAX_STRETCH: Float = 0.08f

/**
 * Travel speed, in slot widths per second, at which the stretch reaches
 * [INDICATOR_MAX_STRETCH].
 *
 * Normalising by the slot width rather than by pixels keeps the deformation the
 * same on every density and at every bar width — a two-tab bar on a tablet
 * launches the pill much faster in px/s than a two-tab bar on a phone, but it is
 * the same gesture and should look the same.
 */
private const val INDICATOR_STRETCH_FULL_SPEED: Float = 10f

// ─── Component ──────────────────────────────────────────────

/**
 * A bottom bar whose selection indicator is a single pill of glass that travels
 * between tabs.
 *
 * The plain [NavigationBar] gives every item its own indicator and fades them in
 * and out, so switching tabs destroys one pill and creates another somewhere
 * else. Here there is exactly one indicator for the whole bar and selection moves
 * it, which is the only way the transition can read as continuous.
 *
 * Two things make it read as *liquid* rather than as a sliding rectangle:
 *
 * - The pill is real glass — [Modifier.glassSurface] over the bar's own exported
 *   recording, so it refracts the tabs and the panel it slides across instead of
 *   painting an opaque swatch over them.
 * - It deforms with its own velocity. The travel animation's speed is fed into
 *   `glassSurface`'s `layerBlock`, stretching the pill along X and pinching it
 *   along Y as it launches, and letting it settle square as it arrives. Because
 *   the transform goes through `layerBlock`, the backdrop sampling is inverted by
 *   the same matrix and the scenery underneath stays put while the glass alone
 *   deforms.
 *
 * Geometry assumes equal-width tabs: the indicator is `barWidth / itemCount` wide
 * and sits at `selectedIndex` slots from the start. [NavigationBarItem] already
 * takes `weight(1f)`, so that holds for the intended children; supply items that
 * do the same if you write your own.
 *
 * Pass `showIndicator = false` to the items. They paint their own static pill by
 * default, which would otherwise sit underneath this one and double up.
 *
 * ```
 * GlassContainer(background = { Image(wallpaper, null) }) {
 *     GlassNavigationBar(
 *         selectedIndex = tab,
 *         itemCount = 2,
 *         modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
 *     ) {
 *         NavigationBarItem(
 *             selected = tab == 0,
 *             onClick = { tab = 0 },
 *             icon = RikkaIcons.Phone,
 *             label = "Dialer",
 *             layout = NavigationBarItemLayout.Inline,
 *             showIndicator = false,
 *         )
 *         NavigationBarItem(
 *             selected = tab == 1,
 *             onClick = { tab = 1 },
 *             icon = RikkaIcons.Sparkles,
 *             label = "Agent",
 *             layout = NavigationBarItemLayout.Inline,
 *             showIndicator = false,
 *         )
 *     }
 * }
 * ```
 *
 * @param selectedIndex Zero-based index of the selected tab; the indicator
 *   travels here whenever it changes. Values outside the bar are clamped.
 * @param itemCount Number of tabs in [content]. This is what the indicator's
 *   width is derived from, so it must match the children you emit. Values of zero
 *   or less draw the bar with no indicator at all rather than dividing by zero.
 * @param modifier [Modifier] applied to the glass panel. The bar is a floating
 *   pill, not a dock, so size and inset it here.
 * @param backdrop What shows through the bar; taken from [LocalGlassBackdrop] by
 *   default. The indicator does *not* use this — it refracts the bar instead.
 * @param level [GlassLevel] of the bar itself. One rung below the usual chrome
 *   level, because the indicator nests inside the bar and is therefore already
 *   stepped down; leaving the bar at `Prominent` flattens the difference between
 *   the two.
 * @param indicatorLevel [GlassLevel] of the travelling pill, before nesting steps
 *   it down a rung against the bar it sits on.
 * @param shape Outline of the bar; must be corner-based. Pill by default.
 * @param indicatorShape Outline of the travelling pill; must be corner-based.
 * @param contentPadding Inset between the bar edge and the items. The indicator
 *   lives inside it, so this is also the gap it keeps from the bar's rim.
 * @param animationSpec Spec the indicator travels on. Defaults to the theme's
 *   snap spatial spring — stiff, no bounce — so the pill settles quickly on
 *   mid-range GPUs that are already sampling glass every frame of travel.
 * @param maxIndicatorWidth Ceiling on the travelling pill's width. When the
 *   equal-width slot is wider than this (unfolded phones, tablets), the pill
 *   stays this wide and is centred in its slot so it does not become a half-
 *   screen slab behind a short label. Null (default) keeps the historical
 *   fill-the-slot behaviour.
 * @param content [RowScope] content lambda, one [NavigationBarItem] per tab.
 */
@Composable
public fun GlassNavigationBar(
    selectedIndex: Int,
    itemCount: Int,
    modifier: Modifier = Modifier,
    backdrop: Backdrop = LocalGlassBackdrop.current,
    level: GlassLevel = GlassLevel.Regular,
    indicatorLevel: GlassLevel = GlassLevel.Prominent,
    shape: CornerBasedShape = GlassButtonDefaults.shape(),
    indicatorShape: CornerBasedShape = GlassButtonDefaults.shape(),
    contentPadding: PaddingValues = PaddingValues(RikkaTheme.spacing.xs),
    animationSpec: AnimationSpec<Float> = RikkaTheme.motion.spatialSnap(),
    maxIndicatorWidth: Dp? = null,
    content: @Composable RowScope.() -> Unit,
) {
    GlassPanel(
        modifier = modifier,
        level = level,
        shape = shape,
        backdrop = backdrop,
        contentPadding = contentPadding,
        hostsGlass = true,
    ) {
        // Inside the panel this is the panel's own recording, so the pill
        // refracts the bar and the tabs it slides over rather than the scene the
        // bar already blurred.
        val indicatorBackdrop = LocalGlassBackdrop.current
        val indicatorStyle = rememberGlassStyle(level = indicatorLevel)
        val density = LocalDensity.current

        // The row is what defines the usable width: it is already inset by
        // contentPadding, so no padding has to be subtracted back out here.
        var rowSize by remember { mutableStateOf(IntSize.Zero) }

        val slotWidthPx =
            if (itemCount > 0 && rowSize.width > 0) {
                rowSize.width / itemCount.toFloat()
            } else {
                0f
            }
        val maxIndicatorWidthPx =
            maxIndicatorWidth?.let { with(density) { it.toPx() } } ?: Float.POSITIVE_INFINITY
        val indicatorWidthPx = slotWidthPx.coerceAtMost(maxIndicatorWidthPx)
        // Centre a capped pill inside its equal-width slot.
        val slotInsetPx = ((slotWidthPx - indicatorWidthPx) / 2f).coerceAtLeast(0f)
        val targetOffsetPx =
            selectedIndex.coerceIn(0, (itemCount - 1).coerceAtLeast(0)) * slotWidthPx + slotInsetPx

        val indicatorOffsetPx = remember { Animatable(0f) }
        // Last width the indicator was placed against. Distinguishes "the bar
        // just got measured or resized" from "the user picked another tab":
        // only the second is travel worth animating, and animating the first
        // would fling the pill out of a bogus zero-width layout.
        var placedAgainstWidth by remember { mutableStateOf(0f) }

        LaunchedEffect(targetOffsetPx, slotWidthPx, indicatorWidthPx) {
            if (slotWidthPx <= 0f) return@LaunchedEffect
            if (placedAgainstWidth != slotWidthPx) {
                placedAgainstWidth = slotWidthPx
                indicatorOffsetPx.snapTo(targetOffsetPx)
            } else {
                indicatorOffsetPx.animateTo(targetOffsetPx, animationSpec = animationSpec)
            }
        }

        if (indicatorWidthPx > 0f) {
            Box(
                modifier =
                    Modifier
                        // Layout-phase read: travelling the pill re-places it and
                        // recomposes nothing. RTL needs no special case — this
                        // offset mirrors, and so does the row it indexes into.
                        .offset { IntOffset(x = indicatorOffsetPx.value.roundToInt(), y = 0) }
                        .size(
                            width = with(density) { indicatorWidthPx.toDp() },
                            height = with(density) { rowSize.height.toDp() },
                        ).glassSurface(
                            backdrop = indicatorBackdrop,
                            style = indicatorStyle,
                            shape = indicatorShape,
                            layerBlock = {
                                // Draw-phase read of the spring's own velocity —
                                // no differencing, no second animation to keep in
                                // step with the first. Stretch is normalised by
                                // the slot, not the (possibly capped) pill width,
                                // so travel feel stays the same at every density.
                                val slotsPerSecond =
                                    if (slotWidthPx > 0f) {
                                        indicatorOffsetPx.velocity / slotWidthPx
                                    } else {
                                        0f
                                    }
                                val stretch =
                                    (slotsPerSecond / INDICATOR_STRETCH_FULL_SPEED)
                                        .absoluteValue
                                        .coerceAtMost(INDICATOR_MAX_STRETCH)
                                scaleX = 1f + stretch
                                scaleY = 1f - stretch
                            },
                        ),
            )
        }

        Row(
            modifier =
                Modifier
                    .onSizeChanged { rowSize = it }
                    .semantics { isTraversalGroup = true },
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}
