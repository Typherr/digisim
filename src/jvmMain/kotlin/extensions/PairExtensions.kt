package extensions

import androidx.compose.ui.geometry.Offset

fun Pair<Float, Float>.toOffset() = Offset(first, second)
fun Pair<Float, Float>.add(other: Offset) = toOffset() + other
