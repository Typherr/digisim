import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

private fun <E> MutableSet<E>.removeFirst(): E {
    val result = this.first()
    this.remove(result)
    return result
}

class Simulator(private val circuit: Graph<CircuitPair>) {
    private val inputComponents: List<Component>
        get() = circuit.vertices
            .filter { it.data.component.inputNames.isEmpty() }
            .map { it.data.component }
    private val toSimulate: MutableSet<Component> = LinkedHashSet()
    private val _knownValues: MutableMap<Component, List<Int?>> = mutableMapOf()

    val knownValues: Map<Component, List<Int?>>
        get() = _knownValues

    fun simulateComponent(component: Component) {
        toSimulate.add(component)
    }

    /**
     * Complete a simulation step
     * @param restart Whether to restart the simulation if it is complete
     * @throws RuntimeException Thrown if [restart] is false and the simulation would restart
     */
    fun simulationStep(restart: Boolean = true) {
        if (toSimulate.isEmpty()) {
            if (!restart) {
                throw RuntimeException("Simulation would restart")
            }
            for (component in inputComponents) {
                val result = component.compute(listOf())
                _knownValues[component] = result
                for (resultIndex in result.indices) {
                    val destinations = circuit[component index resultIndex]
                    toSimulate.addAll(destinations.map { it.data.component })
                }
            }
        }
        else {
            // Get component from toSimulate
            val simulatedComponent = toSimulate.removeFirst()

            // Find the inputs it needs and get its input values from knownValues
            val inputs = MutableList<Int?>(simulatedComponent.inputNames.size) { null }
            for((fromPair, toPairs) in circuit.adjacencyMap) {
                val (fromComp, fromIdx) = fromPair.data
                if (!_knownValues.containsKey(fromComp)) {
                    continue
                }
                for ((toComp, toIdx) in toPairs.map { it.data }) {
                    if (toComp == simulatedComponent) {
                        // fromComp[fromIdx] -> toComp[toIdx] wire found
                        inputs[toIdx] = _knownValues[fromComp]!![fromIdx]
                    }
                }
                if (inputs.none { it == null }) {
                    break
                }
            }

            // Simulate the component
            val result = simulatedComponent.compute(inputs)

            // Add the result to known values
            _knownValues[simulatedComponent] = result

            // Notify all components that are using outputs of this component that they need to simulate
            for (resultIndex in result.indices) {
                val destinations = circuit[simulatedComponent index resultIndex]
                toSimulate.addAll(destinations.map { it.data.component }.distinct())
            }
        }
    }
}

fun main() {
    val circuit = Graph<CircuitPair>()
//    // A - AND1[0]
//    // B - AND1[1]  AND1[0] - OR1[0]
//    // C -------------------- OR1[1]  OR1[0] - OUT
//    val inputA = InputPin()
//    val inputB = InputPin()
//    val inputC = InputPin()
//
//    val outputOut = OutputPin()
//
//    val and1 = AndGate()
//
//    val or1 = OrGate()
//
//    val components = listOf(inputA, inputB, inputC, outputOut, and1, or1)
//
//    circuit.addEdge(inputA to 0, and1 to 0)
//    circuit.addEdge(inputB to 0, and1 to 1)
//    circuit.addEdge(inputC to 0, or1 to 1)
//    circuit.addEdge(and1 to 0, or1 to 0)
//    circuit.addEdge(or1 to 0, outputOut to 0)

    // R ------- NOR1[0]
    // NOR2[0] - NOR1[1]  NOR1[0] - Q
    //
    // NOR1[0] - NOR2[0]  NOR2[0] - NOTQ
    // s ------- NOR2[1]

    val inputR = InputPin()
    val inputS = InputPin()

    val nor1 = NorGate()
    val nor2 = NorGate()

    val outputQ = OutputPin()
    val outputNotQ = OutputPin()

    val components = listOf(inputR, inputS, nor1, nor2, outputQ, outputNotQ)

    circuit.addEdge(inputR index 0, nor1 index 0)
    circuit.addEdge(inputS index 0, nor2 index 1)
    circuit.addEdge(nor2 index 0, nor1 index 1)
    circuit.addEdge(nor1 index 0, nor2 index 0)
    circuit.addEdge(nor1 index 0, outputQ index 0)
    circuit.addEdge(nor2 index 0, outputNotQ index 0)

//    val json = Json.encodeToString(circuit)
//
//    File("${System.getProperty("user.home")}/Desktop/circuit.json").writeText(json)
//
//    val circuit2 = Json.decodeFromString<Graph<CircuitPair>>(json)

    val sim = Simulator(circuit)

    while (true) {
        for (component in components) {
            println("${component.name}@${component.hashCode()}")
            if (component.outputNames.isNotEmpty()) {
                println("Outputs: ")
                for ((idx, output) in component.outputNames.withIndex()) {
                    val value = sim.knownValues[component]?.getOrNull(idx)?.toString() ?: "undefined"
                    println("  $output: $value")
                }
            }
            if (component.properties.isNotEmpty()) {
                println("Properties: ")
                for ((key, value) in component.properties) {
                    val displayName = component.exposedProperties[key] ?: key
                    println("  $displayName: $value")
                }
            }
            println()
        }

        println()

        println("1. Next step")
        println("2. Complete simulation (Might result in infinite loop)")
        println("3. Click component")
        println("0. Exit")

        print("> ")
        var input = readlnOrNull()?.toIntOrNull()
        if (input == 0) {
            break
        }
        else if (input == 1) {
            // Next step
            sim.simulationStep()
        }
        else if (input == 2) {
            try {
                while (true) {
                    sim.simulationStep(false)
                }
            } catch (_: RuntimeException) {
                // Stop the simulation
            }
        }
        else if (input == 3) {
            // Click component
            val clickable = components.filter { it.isClickable }
            while (true) {
                for ((i, comp) in clickable.withIndex()) {
                    println("${i + 1}. ${comp.name}@${comp.hashCode()}")
                }
                println("0. Exit")

                print("> ")
                input = readlnOrNull()?.toIntOrNull()
                if (input == 0) {
                    break
                } else if (input == null) {
                    println("Unknown input")
                } else if (input > clickable.size) {
                    println("Invalid index")
                } else {
                    clickable[input - 1].onClick()
                    sim.simulateComponent(clickable[input - 1])
                }
            }
        }
        else {
            println("Unknown input")
        }
    }
}
