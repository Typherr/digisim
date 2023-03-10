import kotlinx.serialization.Serializable

@Serializable(with = DisplayableSerializer::class)
sealed interface Component : Displayable {
    // lists of names for inputs/outputs
    val inputNames : List<String>
    val outputNames : List<String>

    val inputPositions: List<Pair<Double, Double>>
    val outputPositions: List<Pair<Double, Double>>

    fun compute(inputs: List<Int?>) : List<Int?> {
        // Return all inputs (adding 0 or removing inputs at the end if size not matching) by default
        return if (inputs.size < outputNames.size) {
            (inputs + List(outputNames.size - inputs.size) { 0 }).toList()
        } else {
            inputs.take(outputNames.size)
        }
    }
}