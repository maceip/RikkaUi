package dev.rikkaui.sample

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test

/** Android-only component captures consumed by the generated Pages gallery. */
class GalleryPhoneSnapshotTest {
    @get:Rule
    val paparazzi: Paparazzi =
        Paparazzi(
            deviceConfig = DeviceConfig.PIXEL_9_PRO,
            theme = "android:Theme.Material.Light.NoActionBar",
        )

    @Test
    fun swipeableRow() {
        paparazzi.snapshot { CallHistoryScene() }
    }

    @Test
    fun glass() {
        paparazzi.snapshot { IncomingCallScene() }
    }

    @Test
    fun call() {
        paparazzi.snapshot { DialpadScene() }
    }
}

/** Unfolded inner-display captures using layoutlib's newest Pixel Fold profile. */
class GalleryFoldSnapshotTest {
    @get:Rule
    val paparazzi: Paparazzi =
        Paparazzi(
            deviceConfig = DeviceConfig.PIXEL_9_PRO_FOLD,
            theme = "android:Theme.Material.Light.NoActionBar",
        )

    @Test
    fun swipeableRow() {
        paparazzi.snapshot { CallHistoryScene() }
    }

    @Test
    fun glass() {
        paparazzi.snapshot { IncomingCallScene() }
    }

    @Test
    fun call() {
        paparazzi.snapshot { DialpadScene() }
    }
}
