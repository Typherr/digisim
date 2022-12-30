import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer
import kotlin.reflect.typeOf


@Serializable(with = GraphSerializer::class)
class Graph<T> {
    @Serializable
    data class Vertex<T>(val data: T)

    internal val vertices = mutableListOf<Vertex<T>>()
    internal val adjacencyMap = mutableMapOf<Vertex<T>, MutableList<Vertex<T>>>()

    fun createVertex(data: T): Vertex<T> {
        val vertex = Vertex(data)
        addVertex(vertex)
        return vertex
    }

    fun removeVertex(vertex: Vertex<T>) {
        adjacencyMap.remove(vertex)
        for (v in adjacencyMap.values) {
            v.remove(vertex)
        }
        vertices.remove(vertex)
    }

    fun addEdge(source: Vertex<T>, destination: Vertex<T>) {
        if (!adjacencyMap.containsKey(source)) {
            adjacencyMap[source] = mutableListOf()
        }

        adjacencyMap[source]!!.add(destination)
    }

    fun removeEdge(source: Vertex<T>, destination: Vertex<T>) {
        // Should never be true
        if (!adjacencyMap.containsKey(source)) return

        adjacencyMap[source]!!.remove(destination)
    }

    internal fun addVertex(vertex: Vertex<T>) {
        vertices.add(vertex)
        adjacencyMap[vertex] = mutableListOf()
    }

    override fun toString(): String {
        return buildString {
            adjacencyMap.forEach { (source, destinations) ->
                val edgeString = destinations.joinToString { it.data.toString() }
                append("${source.data} -> [$edgeString]\n")
            }
        }
    }
}

@Serializable
private class GraphSurrogate<T>(val vertices: MutableList<Graph.Vertex<T>>, val adjacencyMap: MutableMap<Int, MutableList<Int>>)

class GraphSerializer<T> : KSerializer<Graph<T>> {

    override val descriptor: SerialDescriptor = serializer(typeOf<GraphSurrogate<T>>()).descriptor

    override fun deserialize(decoder: Decoder): Graph<T> {
        @Suppress("UNCHECKED_CAST")
        val surrogate = decoder.decodeSerializableValue(serializer(typeOf<GraphSurrogate<T>>()) as KSerializer<GraphSurrogate<T>>)
        val graph = Graph<T>()
        for (vertex in surrogate.vertices) {
            graph.addVertex(vertex)
        }
        for ((source, destinations) in surrogate.adjacencyMap.entries) {
            for (destination in destinations) {
                graph.addEdge(graph.vertices[source], graph.vertices[destination])
            }
        }
        return graph
    }

    override fun serialize(encoder: Encoder, value: Graph<T>) {
        val adjMapIndices = mutableMapOf<Int, MutableList<Int>>()
        for ((source, destinations) in value.adjacencyMap.entries) {
            val sourceIdx = value.vertices.indexOf(source)
            adjMapIndices[sourceIdx] = destinations.map { value.vertices.indexOf(it) }.toMutableList()
        }
        val surrogate = GraphSurrogate(value.vertices, adjMapIndices)
        encoder.encodeSerializableValue(serializer(typeOf<GraphSurrogate<T>>()), surrogate)
    }
}