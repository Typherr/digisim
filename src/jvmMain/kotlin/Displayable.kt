import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

@Serializable(with = DisplayableSerializer::class)
sealed interface Displayable {
    fun onClick() {}
    val isClickable: Boolean

    // name of component
    val name: String

    val properties: MutableMap<String, String>

    /**
     * Maps keys in the *properties* map to display names in the UI
     */
    val exposedProperties: MutableMap<String, String>

    val writableProperties: MutableList<String>
}

class Label : Displayable {
    override val name = "Label"
    override val isClickable: Boolean = false
    override val properties: MutableMap<String, String> = mutableMapOf(
        "text" to "Label",
        "color" to "black",
    )
    override val exposedProperties: MutableMap<String, String> = mutableMapOf(
        "text" to "Text",
        "color" to "Color",
    )
    override val writableProperties: MutableList<String> = mutableListOf(
        "text",
        "color",
    )

    val color: Color
        get() = getColor(properties["color"] ?: "")

    companion object {
        val colors = mapOf(
            "black" to Color.Black,
            "blue" to Color.Blue,
            "cyan" to Color.Cyan,
            "darkgray" to Color.DarkGray,
            "gray" to Color.Gray,
            "green" to Color.Green,
            "lightgray" to Color.LightGray,
            "magenta" to Color.Magenta,
            "red" to Color.Red,
            "white" to Color.White,
            "yellow" to Color.Yellow,
        )

        fun getColor(color: String): Color {
            val color = color.lowercase()
            if (colors.containsKey(color)) {
                return colors[color]!!
            }
            if (color.startsWith("#")) {
                try {
                    val segments = color.drop(1).chunked(2).map { it.toInt(16) }
                    return Color(
                        segments.getOrElse(0) { 0x00 },
                        segments.getOrElse(1) { 0x00 },
                        segments.getOrElse(2) { 0x00 },
                        segments.getOrElse(3) { 0xFF })
                } catch (_: NumberFormatException) {
                    // Ignore hex if invalid
                }
            }
            return Color.Black
        }
    }
}