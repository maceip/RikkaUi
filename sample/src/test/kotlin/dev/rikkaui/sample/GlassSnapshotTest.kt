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
 * and no `RuntimeShader`. Backdrop sampling and translucent tint still render,
 * so these are not `GlassCapability.None` fallback snapshots. Blur, AGSL lens
 * refraction, dispersion, inner shadow, and the specular rim do not render.
 *
 * What is genuinely verified: layout and composition of all five scenes,
 * translucent backdrop sampling and tint, radial-gradient dialpad keys, Canvas
 * layers of the orb, the swipe row at rest, and ExtendedSpans decorations.
 *
 * What is not: blur, refraction, dispersion, inner shadow, and the specular rim.
 * Those remain physical-device acceptance criteria.
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
