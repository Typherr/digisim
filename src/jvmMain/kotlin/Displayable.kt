interface Displayable {
    fun onClick() {}

    // name of component
    val name : String

    val properties: MutableMap<String, String>

    /**
     * Maps keys in the *properties* map to display names in the UI
     */
    val exposedProperties: MutableMap<String, String>
}

object Label : Displayable {
    override val name = "Label"
    override val properties: MutableMap<String, String> = mutableMapOf(
        "text" to "Label"
    )
    override val exposedProperties: MutableMap<String, String> = mutableMapOf(
        "text" to "Text"
    )
}