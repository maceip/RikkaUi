package zed.rainxch.rikkaui.components.ui.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/**
 * The backdrop that glass surfaces sample from.
 *
 * Glass is not a colour — it is a lens over something else. A glass component
 * therefore needs a recording of whatever sits behind it, and that recording has
 * to be produced explicitly by marking the content that should show through.
 *
 * Defaults to an empty backdrop, which renders glass as tint and rim only. That
 * is a deliberate no-op rather than a crash: a component dropped outside a
 * [GlassContainer] still draws, it just has nothing to refract.
 */
public val LocalGlassBackdrop: ProvidableCompositionLocal<Backdrop> =
    staticCompositionLocalOf { emptyBackdrop() }

/**
 * Creates the backdrop recording that glass surfaces sample from.
 *
 * Use this together with [glassBackdropSource] when you need to choose the
 * source yourself — for example when a `LazyColumn`, not a wallpaper, is the
 * thing that should show through. For the common "background layer plus
 * floating glass" arrangement, prefer [GlassContainer].
 */
@Composable
public fun rememberGlassBackdrop(): LayerBackdrop = rememberLayerBackdrop()

/**
 * Combines several recordings into one backdrop, drawn in the order given.
 *
 * Use it when what shows through a surface is assembled from more than one
 * source — a wallpaper plus a scrolling list, say, where the list is recorded
 * separately because it also has to be scrolled and clipped on its own.
 *
 * ```
 * val wallpaper = rememberGlassBackdrop()
 * val list = rememberGlassBackdrop()
 * val combined = rememberGlassBackdrops(wallpaper, list)
 *
 * GlassPanel(backdrop = combined) { /* nav bar over both */ }
 * ```
 */
@Composable
public fun rememberGlassBackdrops(vararg backdrops: Backdrop): Backdrop = rememberCombinedBackdrop(backdrops = backdrops)

/**
 * Marks this content as the source that glass surfaces refract.
 *
 * Everything drawn by the modified node is recorded into [backdrop]; anything
 * drawn after it is not. Glass surfaces must therefore be siblings drawn *after*
 * this node, never children of it — a surface that sampled a layer it was part
 * of would feed back into itself.
 *
 * ```
 * val backdrop = rememberGlassBackdrop()
 *
 * Box(Modifier.fillMaxSize()) {
 *     Image(wallpaper, null, Modifier.matchParentSize().glassBackdropSource(backdrop))
 *     GlassCard(backdrop = backdrop) { Text("Floats above the wallpaper") }
 * }
 * ```
 */
public fun Modifier.glassBackdropSource(backdrop: LayerBackdrop): Modifier = layerBackdrop(backdrop)

/**
 * Stacks a glass-refracting foreground over a recorded [background] layer.
 *
 * The [background] fills the container and is recorded as the backdrop; the
 * [content] is drawn on top and receives that backdrop through
 * [LocalGlassBackdrop], so glass components nested anywhere inside it pick the
 * right source up without being passed it by hand.
 *
 * This is also where the glass material is negotiated with the device. The
 * container resolves [rememberGlassCapability] once and publishes it through
 * [LocalGlassCapability], so every surface inside it agrees on the tier and
 * reacts together when battery saver comes on — rather than each surface
 * probing the platform for itself.
 *
 * Note that [background] is measured with `matchParentSize`, so it never drives
 * the container's size — pass `Modifier.fillMaxSize()` (or a fixed size) if the
 * [content] does not already establish one.
 *
 * ```
 * GlassContainer(
 *     modifier = Modifier.fillMaxSize(),
 *     background = {
 *         Image(painterResource(R.drawable.wallpaper), null, contentScale = ContentScale.Crop)
 *     },
 * ) {
 *     GlassPanel(modifier = Modifier.align(Alignment.BottomCenter)) { /* nav bar */ }
 * }
 * ```
 *
 * @param modifier [Modifier] applied to the root Box.
 * @param background Content recorded as the backdrop; drawn first, sized to the
 *   container. Defaults to [GlassScenery] — a scene built to be refracted.
 *   Replacing it with a flat wash is what makes glass stop reading as glass.
 * @param backdrop The layer recording to write into; supply your own to share it across containers.
 * @param capability Glass tier for everything inside; detected from the device by default.
 * @param content Foreground content drawn over the backdrop, with [LocalGlassBackdrop] provided.
 */
@Composable
public fun GlassContainer(
    modifier: Modifier = Modifier,
    background: @Composable BoxScope.() -> Unit = { GlassScenery() },
    backdrop: LayerBackdrop = rememberGlassBackdrop(),
    capability: GlassCapability = rememberGlassCapability(),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier) {
        Box(
            modifier = Modifier.matchParentSize().glassBackdropSource(backdrop),
            content = background,
        )
        CompositionLocalProvider(
            LocalGlassBackdrop provides backdrop,
            LocalGlassCapability provides capability,
            // Content here is over the *backdrop*, not over glass, however deep
            // the container itself happens to be nested.
            LocalGlassDepth provides 0,
        ) {
            content()
        }
    }
}
