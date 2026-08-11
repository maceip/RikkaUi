package dev.rikkaui.sample

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import zed.rainxch.rikkaui.components.ui.call.CallDirection
import zed.rainxch.rikkaui.components.ui.call.CallHistoryItem
import zed.rainxch.rikkaui.components.ui.call.GlassDialpad
import zed.rainxch.rikkaui.components.ui.call.IncomingCallSheet
import zed.rainxch.rikkaui.components.ui.call.TranscriptLine
import zed.rainxch.rikkaui.components.ui.call.TranscriptSpeaker
import zed.rainxch.rikkaui.components.ui.call.TranscriptText
import zed.rainxch.rikkaui.components.ui.call.VoiceOrb
import zed.rainxch.rikkaui.components.ui.call.VoiceOrbState
import zed.rainxch.rikkaui.components.ui.call.transcriptHighlight
import zed.rainxch.rikkaui.components.ui.call.transcriptTentative
import zed.rainxch.rikkaui.components.ui.glass.GlassContainer
import zed.rainxch.rikkaui.components.ui.text.Text
import zed.rainxch.rikkaui.components.ui.text.TextVariant
import zed.rainxch.rikkaui.foundation.RikkaPalette
import zed.rainxch.rikkaui.foundation.RikkaTheme
import zed.rainxch.rikkaicons.core.ProvideIconPack
import zed.rainxch.rikkaicons.pack.phosphor.PhosphorPack

/**
 * A deterministic backdrop for glass to refract.
 *
 * Painted rather than loaded from a drawable so screenshot tests do not depend
 * on an asset, and so the same scene renders byte-identically on every run.
 */
@Composable
fun DemoBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        drawRect(
            Brush.linearGradient(
                colors = listOf(Color(0xFF1B2A5B), Color(0xFF7A2E6E), Color(0xFFD4693C)),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            ),
        )
        // Blobs give the refracting edge something with structure to bend.
        drawCircle(Color(0x66F5C15E), radius = size.minDimension * 0.28f, center = Offset(size.width * 0.18f, size.height * 0.22f))
        drawCircle(Color(0x5535D3C4), radius = size.minDimension * 0.22f, center = Offset(size.width * 0.82f, size.height * 0.42f))
        drawCircle(Color(0x44FF5D8F), radius = size.minDimension * 0.3f, center = Offset(size.width * 0.55f, size.height * 0.78f))
    }
}

/** Wraps [content] in the theme and a recorded backdrop, the way a real screen would. */
@Composable
fun GlassScene(
    isDark: Boolean = true,
    content: @Composable () -> Unit,
) {
    RikkaTheme(palette = RikkaPalette.Zinc, isDark = isDark) {
        ProvideIconPack(PhosphorPack.Regular) {
        GlassContainer(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                content()
            }
        }
        }
    }
}

// ─── Scenes ─────────────────────────────────────────────────

@Composable
fun DialpadScene() {
    GlassScene {
        GlassDialpad(onKeyPress = {})
    }
}

@Composable
fun VoiceOrbScene() {
    GlassScene {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            VoiceOrb(state = VoiceOrbState.Listening, amplitude = { 0.65f })
            Text("Listening", variant = TextVariant.Large, color = Color.White)
        }
    }
}

@Composable
fun CallHistoryScene() {
    GlassScene {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CallHistoryItem(
                name = "Ada Lovelace",
                detail = "+1 555 0134",
                timestamp = "2 min",
                direction = CallDirection.Missed,
                badge = "AGENT",
                deleteLabel = "Delete",
                callBackLabel = "Call back",
                onDelete = {},
                onCallBack = {},
            )
            CallHistoryItem(
                name = "Grace Hopper",
                detail = "Mobile",
                timestamp = "1 h",
                direction = CallDirection.Outgoing,
                deleteLabel = "Delete",
                callBackLabel = "Call back",
                onDelete = {},
                onCallBack = {},
            )
        }
    }
}

@Composable
fun IncomingCallScene() {
    GlassScene {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            IncomingCallSheet(
                callerName = "Ada Lovelace",
                callerDetail = "+1 555 0134",
                transcript = demoTranscript(),
                quickReplies = listOf("Call back", "In a meeting"),
                answerLabel = "Answer",
                declineLabel = "Decline",
                onAnswer = {},
                onDecline = {},
                onQuickReply = {},
            )
        }
    }
}

/** Isolates the span painters so the decorations can be checked on their own. */
@Composable
fun TranscriptScene() {
    RikkaTheme(palette = RikkaPalette.Zinc, isDark = false) {
        ProvideIconPack(PhosphorPack.Regular) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TranscriptText(text = settledLine())
                TranscriptText(text = tentativeLine())
            }
        }
        }
    }
}

@Composable
private fun demoTranscript(): List<TranscriptLine> =
    listOf(
        TranscriptLine(settledLine(), TranscriptSpeaker.Agent),
        TranscriptLine(AnnotatedString("Who is this?"), TranscriptSpeaker.Caller),
        TranscriptLine(tentativeLine(), TranscriptSpeaker.Agent),
    )

@Composable
private fun settledLine(): AnnotatedString =
    buildAnnotatedString {
        append("They are asking about ")
        withStyle(transcriptHighlight()) { append("Tuesday at 3") }
        append(".")
    }

private fun tentativeLine(): AnnotatedString =
    buildAnnotatedString {
        append("I think they said ")
        withStyle(transcriptTentative()) { append("reschedule to Thursday") }
    }
