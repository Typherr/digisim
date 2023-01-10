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
}

class Label : Displayable {
    override val name = "Label"
    override val isClickable: Boolean = false
    override val properties: MutableMap<String, String> = mutableMapOf(
        "text" to "Label"
    )
    override val exposedProperties: MutableMap<String, String> = mutableMapOf(
        "text" to "Text"
    )
}