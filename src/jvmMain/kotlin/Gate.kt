abstract class Gate(override val name: String, private val truthTable : List<Int>) : Component {
    override fun compute(inputs: List<Int>): List<Int> {
        var bitNumber = 0
        for (input in inputs.reversed()) {
            bitNumber = bitNumber shl 1
            if (input != 0) {
                bitNumber = bitNumber or 1
            }
        }

        var output = truthTable[bitNumber]

        val result = mutableListOf<Int>()

        while (output != 0) {
            result.add(0, output and 1)
            output = output shr 1
        }

        while (result.size != outputNames.size) {
            result.add(0, 0)
        }

        return result
    }
}

object Buffer: Gate("Buffer", listOf(0, 1)) {
    override val inputNames: List<String>
        get() = listOf("IN")
    override val outputNames: List<String>
        get() = listOf("OUT")
}

object NotGate: Gate("And", listOf(1, 0)) {
    override val inputNames: List<String>
        get() = listOf("IN")
    override val outputNames: List<String>
        get() = listOf("OUT")
}

object AndGate: Gate("And", listOf(0, 0, 0, 1)) {
    override val inputNames: List<String>
        get() = listOf("A", "B")
    override val outputNames: List<String>
        get() = listOf("OUT")
}

object NandGate: Gate("And", listOf(1, 1, 1, 0)) {
    override val inputNames: List<String>
        get() = listOf("A", "B")
    override val outputNames: List<String>
        get() = listOf("OUT")
}

object OrGate: Gate("And", listOf(0, 1, 1, 1)) {
    override val inputNames: List<String>
        get() = listOf("A", "B")
    override val outputNames: List<String>
        get() = listOf("OUT")
}

object NorGate: Gate("And", listOf(1, 0, 0, 0)) {
    override val inputNames: List<String>
        get() = listOf("A", "B")
    override val outputNames: List<String>
        get() = listOf("OUT")
}

object XorGate: Gate("And", listOf(0, 1, 1, 0)) {
    override val inputNames: List<String>
        get() = listOf("A", "B")
    override val outputNames: List<String>
        get() = listOf("OUT")
}

object XnorGate: Gate("And", listOf(1, 0, 0, 1)) {
    override val inputNames: List<String>
        get() = listOf("A", "B")
    override val outputNames: List<String>
        get() = listOf("OUT")
}

fun main() {
    for( i in (0 until 4))
    print(XorGate.compute(i.toString(2).map { it - '0' }))
}