import kotlinx.serialization.Serializable

@Serializable(with = DisplayableSerializer::class)
class OutputPin : Component {
    override val inputNames: List<String> = listOf("")
    override val outputNames: List<String> = listOf()
    override val name: String = "Output"

    override val inputPositions: List<Pair<Double, Double>> = listOf(0.0 to 0.0)
    override val outputPositions: List<Pair<Double, Double>> = listOf()

    override val isClickable: Boolean = false

    override val properties: MutableMap<String, String> = mutableMapOf("value" to "undefined")
    override val exposedProperties: MutableMap<String, String> = mutableMapOf("value" to "Value")

    override fun compute(inputs: List<Int?>): List<Int?> {
        properties["value"] = inputs.firstOrNull()?.toString() ?: "undefined"
        return listOf()
    }
}