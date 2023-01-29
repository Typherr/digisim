import kotlinx.serialization.Serializable
import java.text.DateFormat
import java.util.*

@Serializable(with = DisplayableSerializer::class)
open class Clock : Component {
    override val inputNames: List<String> = listOf()
    override val outputNames: List<String> = listOf("")
    override val name: String = "Clock"

    override val inputPositions: List<Pair<Double, Double>> = listOf()
    override val outputPositions: List<Pair<Double, Double>> = listOf(144.0 / 150.0 to 24.0 / 50.0)

    override val properties: MutableMap<String, String> =
        mutableMapOf("value" to "0", "lastUpdate" to "0", "delay" to "1000")
    override val exposedProperties: MutableMap<String, String> =
        mutableMapOf("value" to "Value", "delay" to "Delay (ms)")
    override val writableProperties: MutableList<String> = mutableListOf("value", "delay")

    var value
        get() = properties["value"]?.toIntOrNull()
        set(value) { properties["value"] = value.toString() }
    private var lastUpdate
        get() = properties["lastUpdate"]?.toLongOrNull() ?: 0
        set(value) { properties["lastUpdate"] = value.toString() }
    private var delay
        get() = properties["delay"]?.toLongOrNull() ?: 0
        set(value) { properties["delay"] = value.toString() }

    override fun compute(inputs: List<Int?>): List<Int?> {
        if(delay > 0) {
            val currentTime = System.currentTimeMillis()
            if (currentTime > lastUpdate + delay) {
                value = if (value == 1) 0 else 1
                lastUpdate = currentTime
            }
        }

        return listOf(value)
    }

    override val isClickable: Boolean = true
    override fun onClick() {
        value = if (value == 0) 1 else 0
    }
}