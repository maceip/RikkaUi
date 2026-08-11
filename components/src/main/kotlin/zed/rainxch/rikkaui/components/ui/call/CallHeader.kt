package zed.rainxch.rikkaui.components.ui.call

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import zed.rainxch.rikkaui.components.ui.text.Text
import zed.rainxch.rikkaui.components.ui.text.TextVariant
import zed.rainxch.rikkaui.foundation.RikkaTheme

/**
 * Who a live call is with, and how long it has been running.
 *
 * The elapsed clock is passed in already formatted: the caller owns the tick, so
 * nothing here has to recompose on a timer it does not control.
 *
 * ```
 * CallHeader(
 *     callerName = "Ada Lovelace",
 *     callerNumber = "+1 555 0134",
 *     statusLine = "On the Triplex line",
 *     elapsed = "02:14",
 * )
 * ```
 *
 * @param callerName Who is on the call, or the number when there is no contact.
 * @param modifier [Modifier] applied to the root Column.
 * @param callerNumber The number, shown under [callerName]. Blank, or equal to
 *   [callerName], hides the line rather than repeating it.
 * @param statusLine Short stack or screening status, shown above the name in
 *   caps. Blank hides it.
 * @param elapsed The call clock, or null when there is no connect time to count
 *   from — a screened leg has none, and a fabricated one would be a lie.
 */
@Composable
public fun CallHeader(
    callerName: String,
    modifier: Modifier = Modifier,
    callerNumber: String = "",
    statusLine: String = "",
    elapsed: String? = null,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RikkaTheme.spacing.xs),
    ) {
        if (statusLine.isNotBlank()) {
            Text(text = statusLine.uppercase(), variant = TextVariant.Small)
        }
        Text(text = callerName, variant = TextVariant.H3, textAlign = TextAlign.Center)
        if (callerNumber.isNotBlank() && callerNumber != callerName) {
            Text(text = callerNumber, variant = TextVariant.Muted)
        }
        elapsed?.let { Text(text = it, variant = TextVariant.Large) }
    }
}
