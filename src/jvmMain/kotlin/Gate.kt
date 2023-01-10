import kotlinx.serialization.Serializable

abstract class Gate(override val name: String, private val truthTable : List<Int>) : Component {
    override val inputNames: List<String>
        get() = if (truthTable.size == 2) {
            listOf("IN")
        } else {
            ('A'..'Z').take(truthTable.size.countTrailingZeroBits()).map { it.toString() }
        }
    override val outputNames: List<String>
        get() = listOf("OUT")

    override fun compute(inputs: List<Int?>): List<Int?> {
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

    override val properties: MutableMap<String, String> = mutableMapOf()
    override val exposedProperties: MutableMap<String, String> = mutableMapOf()

    override val isClickable: Boolean = false
}

@Serializable(with = DisplayableSerializer::class)
class Buffer: Gate("Buffer", listOf(0, 1)) {
    override val inputNames: List<String>
        get() = listOf("IN")
    override val outputNames: List<String>
        get() = listOf("OUT")
}

@Serializable(with = DisplayableSerializer::class)
class NotGate: Gate("NOT Gate", listOf(1, 0)) {
    override val inputNames: List<String>
        get() = listOf("IN")
    override val outputNames: List<String>
        get() = listOf("OUT")
}

@Serializable(with = DisplayableSerializer::class)
class AndGate: Gate("AND Gate", listOf(0, 0, 0, 1)) {
    override val inputNames: List<String>
        get() = listOf("A", "B")
    override val outputNames: List<String>
        get() = listOf("OUT")
}

@Serializable(with = DisplayableSerializer::class)
class NandGate: Gate("NAND Gate", listOf(1, 1, 1, 0)) {
    override val inputNames: List<String>
        get() = listOf("A", "B")
    override val outputNames: List<String>
        get() = listOf("OUT")
}

@Serializable(with = DisplayableSerializer::class)
class OrGate: Gate("OR Gate", listOf(0, 1, 1, 1)) {
    override val inputNames: List<String>
        get() = listOf("A", "B")
    override val outputNames: List<String>
        get() = listOf("OUT")
}

@Serializable(with = DisplayableSerializer::class)
class NorGate: Gate("NOR Gate", listOf(1, 0, 0, 0)) {
    override val inputNames: List<String>
        get() = listOf("A", "B")
    override val outputNames: List<String>
        get() = listOf("OUT")
}

@Serializable(with = DisplayableSerializer::class)
class XorGate: Gate("XOR Gate", listOf(0, 1, 1, 0)) {
    override val inputNames: List<String>
        get() = listOf("A", "B")
    override val outputNames: List<String>
        get() = listOf("OUT")
}

@Serializable(with = DisplayableSerializer::class)
class XnorGate: Gate("XNOR Gate", listOf(1, 0, 0, 1)) {
    override val inputNames: List<String>
        get() = listOf("A", "B")
    override val outputNames: List<String>
        get() = listOf("OUT")
}

fun main() {
    for( i in (0 until 4))
    print(XorGate().compute(i.toString(2).map { it - '0' }))
}