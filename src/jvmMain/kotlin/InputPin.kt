import kotlinx.serialization.Serializable

@Serializable(with = DisplayableSerializer::class)
class InputPin : Component {
    override val inputNames: List<String> = listOf()
    override val outputNames: List<String> = listOf("")
    override val name: String = "Input"

    override val inputPositions: List<Pair<Double, Double>> = listOf()
    override val outputPositions: List<Pair<Double, Double>> = listOf(144.0 / 150.0 to 24.0 / 50.0)

    override val properties: MutableMap<String, String> = mutableMapOf("value" to "0")
    override val exposedProperties: MutableMap<String, String> = mutableMapOf("value" to "Value")
    override val writableProperties: MutableList<String> = mutableListOf("value")

    override fun compute(inputs: List<Int?>): List<Int?> {
        return listOf(properties["value"]?.toIntOrNull())
    }

    override val isClickable: Boolean = true
    override fun onClick() {
        properties["value"] = if (properties["value"] == "0") "1" else "0"
    }
}