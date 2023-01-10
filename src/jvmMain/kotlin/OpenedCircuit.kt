import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.io.File

@Serializable(with = CircuitFileContentsSerializer::class)
data class CircuitFileContents(val graph: Graph<CircuitPair>, val displayables: List<Displayable>, val canvas: Map<Displayable, Pair<Float, Float>>) {
    constructor(): this(Graph(), listOf(), mapOf())

    fun editGraph(graph: Graph<CircuitPair>) = copy(graph = graph)
    fun editGraph(editor: (Graph<CircuitPair>) -> Graph<CircuitPair>) = editGraph(editor(graph))

    fun editDisplayables(displayables: List<Displayable>) = copy(displayables = displayables)
    fun editDisplayables(editor: (List<Displayable>) -> List<Displayable>) = editDisplayables(editor(displayables))

    fun editCanvas(canvas: Map<Displayable, Pair<Float, Float>>) = copy(canvas = canvas)
    fun editCanvas(editor: (Map<Displayable, Pair<Float, Float>>) -> Map<Displayable, Pair<Float, Float>>) = editCanvas(editor(canvas))
}

@Serializable
@SerialName("CircuitFileContents")
internal class CircuitFileContentsSurrogate(val graph: GraphSurrogate<CircuitPair>, val displayables: List<Displayable>, val canvas: Map<Int, Pair<Float, Float>>)

class CircuitFileContentsSerializer: KSerializer<CircuitFileContents> {
    override val descriptor: SerialDescriptor = CircuitFileContentsSurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: CircuitFileContents) {
        val graphSurrogate = GraphSurrogate.fromGraph(value.graph)
        val surrogate = CircuitFileContentsSurrogate(graphSurrogate, value.displayables, value.canvas.mapKeys { (displayable, _) ->
            val graphIndex = value.graph.vertices.indexOfFirst {
                it.data.component == displayable
            }
            if (graphIndex != -1) {
                graphIndex * 2
            }
            else {
                value.displayables.indexOf(displayable) * 2 + 1
            }
        })
        encoder.encodeSerializableValue(CircuitFileContentsSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): CircuitFileContents {
        val surrogate = decoder.decodeSerializableValue(CircuitFileContentsSurrogate.serializer())
        val graph = surrogate.graph.toGraph()
        return CircuitFileContents(graph, surrogate.displayables, surrogate.canvas.mapKeys { (idx, _) ->
            if (idx % 2 == 0) {
                graph.vertices[idx / 2].data.component
            }
            else {
                surrogate.displayables[(idx - 1) / 2]
            }
        })
    }
}

data class OpenedCircuit(val filename: String?, val circuit: CircuitFileContents, val edited: Boolean) {
    constructor() : this(null, CircuitFileContents(), true)

    fun editCircuit(newCircuit: CircuitFileContents): OpenedCircuit = copy(circuit = newCircuit, edited = true)
    fun editCircuit(editor: (CircuitFileContents) -> CircuitFileContents) = editCircuit(editor(circuit))

    fun save(): OpenedCircuit {
        val json = Json.encodeToString(circuit)
        File(filename).writeText(json)
        return copy(edited = false)
    }

    fun saveAs(filename: String) = saveAs(File(filename))
    fun saveAs(file: File) : OpenedCircuit {
        val json = Json.encodeToString(circuit)
        file.writeText(json)
        return copy(edited = false, filename = file.path)
    }

    companion object {
        fun open(filename: String): OpenedCircuit = open(File(filename))

        fun open(file: File): OpenedCircuit {
            val json = file.readText()
            val circuit = Json.decodeFromString<CircuitFileContents>(json)
            return OpenedCircuit(file.path, circuit, false)
        }
    }
}