package dev.rikkaui.sample

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test

/**
 * Screenshot verification for the glass call components.
 *
 * ### What this can and cannot prove
 * Paparazzi renders through layoutlib on the JVM, which has no `RenderEffect`
 * and no `RuntimeShader`. The backdrop blur and the AGSL refraction shader
 * therefore do not run here, and these snapshots show the
 * `GlassCapability.None` fallback — the opaque themed surface. That is a real
 * shipping code path, and these tests are the only automated coverage it has.
 *
 * What is genuinely verified: layout and composition of all five scenes, the
 * radial-gradient dialpad keys, every Canvas layer of the orb except the glass
 * body, the swipe row at rest, and the ExtendedSpans decorations — those are
 * ordinary Skia path and rect drawing, so they render exactly as they will on a
 * device.
 *
 * What is not: blur, refraction, dispersion, inner shadow, and the specular rim
 * — every part of the material that needs a shader. Those need a device.
 */
class GlassSnapshotTest {
    @get:Rule
    val paparazzi: Paparazzi =
        Paparazzi(
            deviceConfig = DeviceConfig.PIXEL_5,
            theme = "android:Theme.Material.Light.NoActionBar",
        )

    @Test
    fun dialpad() {
        paparazzi.snapshot { DialpadScene() }
    }

    @Test
    fun voiceOrb() {
        paparazzi.snapshot { VoiceOrbScene() }
    }

    @Test
    fun callHistory() {
        paparazzi.snapshot { CallHistoryScene() }
    }

    @Test
    fun incomingCallSheet() {
        paparazzi.snapshot { IncomingCallScene() }
    }

    @Test
    fun transcriptSpans() {
        paparazzi.snapshot { TranscriptScene() }
    }
}
