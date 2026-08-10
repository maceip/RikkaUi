package dev.rikkaui.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Host for the glass component gallery.
 *
 * On a device this renders the real material — `RenderEffect` and the AGSL
 * refraction shaders only exist at runtime, so this is the only surface where
 * the glass can actually be seen. Previews and Paparazzi both fall back.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GalleryApp() }
    }
}

@Composable
private fun GalleryApp() {
    val scroll = rememberScrollState()
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scroll),
    ) {
        DialpadScene()
        VoiceOrbScene()
        CallHistoryScene()
        IncomingCallScene()
        TranscriptScene()
    }
}
