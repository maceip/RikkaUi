package dev.rikkaui.sample

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * Compose previews for the glass call components.
 *
 * These are the same scenes the Paparazzi tests snapshot, so what renders in the
 * IDE preview pane and what lands in `src/test/snapshots` cannot drift apart.
 *
 * Note that glass will *not* refract in the preview pane: layoutlib has no
 * `RenderEffect`/`RuntimeShader`, so these render the
 * [zed.rainxch.rikkaui.components.ui.glass.GlassCapability.None] fallback —
 * the opaque themed surface. That is a real shipping code path and worth
 * seeing, but it is not the glass material.
 */
@Preview(name = "Dialpad", widthDp = 360, heightDp = 520)
@Composable
private fun DialpadPreview() {
    DialpadScene()
}

@Preview(name = "Voice orb", widthDp = 360, heightDp = 360)
@Composable
private fun VoiceOrbPreview() {
    VoiceOrbScene()
}

@Preview(name = "Call history", widthDp = 360, heightDp = 320)
@Composable
private fun CallHistoryPreview() {
    CallHistoryScene()
}

@Preview(name = "Incoming call", widthDp = 360, heightDp = 560)
@Composable
private fun IncomingCallPreview() {
    IncomingCallScene()
}

@Preview(name = "Transcript spans", widthDp = 360, heightDp = 200)
@Composable
private fun TranscriptPreview() {
    TranscriptScene()
}
