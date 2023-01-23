import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.absoluteValue

@Composable
fun Wire(from: Offset, to: Offset, color: Color = Color.Black, modifier: Modifier = Modifier) = Wire(
    from.x to from.y,
    to.x to to.y,
    color,
    modifier
)
@Composable
fun Wire(from: Pair<Float, Float>, to: Pair<Float, Float>, color: Color = Color.Black, modifier: Modifier = Modifier) {
    val width = (from.first - to.first).absoluteValue
    val height = (from.second - to.second).absoluteValue
    val backslash = (from.first > to.first) == (from.second > to.second)
    val startOffset = if (backslash) Offset.Zero else Offset(0f, height)
    val endOffset = if (backslash) Offset(width, height) else Offset(width, 0f)

    Canvas(
        modifier
            .width(with(LocalDensity.current) { width.toDp() })
            .height(with(LocalDensity.current) { height.toDp() })

    ) {
        drawLine(color, startOffset, endOffset, strokeWidth = 2.0f)
    }
}
