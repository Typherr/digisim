package extensions

fun <T> List<T>.update(index: Int, newValue: T): List<T> = toMutableList().apply { this[index] = newValue }
