import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp

@Composable
fun MeasureUnconstrainedViewSize(
    viewToMeasure: @Composable () -> Unit,
    content: @Composable (measuredWidth: Dp, measuredHeight: Dp) -> Unit,
) {
    SubcomposeLayout { constraints ->
        val measuredPlacable = subcompose("viewToMeasure", viewToMeasure)[0]
            .measure(Constraints())

        val contentPlaceable = subcompose("content") {
            content(measuredPlacable.width.toDp(), measuredPlacable.height.toDp())
        }[0].measure(constraints)
        layout(contentPlaceable.width, contentPlaceable.height) {
            contentPlaceable.place(0, 0)
        }
    }
}
