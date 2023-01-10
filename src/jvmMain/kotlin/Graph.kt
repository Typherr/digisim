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

    val vertices = mutableListOf<Vertex<T>>()
    val adjacencyMap = mutableMapOf<Vertex<T>, MutableList<Vertex<T>>>()

    operator fun get(vertex: Vertex<T>): MutableList<Vertex<T>> {
        if (!adjacencyMap.containsKey(vertex)) {
            adjacencyMap[vertex] = mutableListOf()
        }
        return adjacencyMap[vertex]!!
    }
    operator fun get(key: T): MutableList<Vertex<T>> = this[Vertex(key)]

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
        if (!vertices.contains(source)) {
            vertices.add(source)
        }
        if (!vertices.contains(destination)) {
            vertices.add(destination)
        }
        if (!adjacencyMap.containsKey(source)) {
            adjacencyMap[source] = mutableListOf()
        }

        adjacencyMap[source]!!.add(destination)
    }

    fun addEdge(source: T, destination: T) = addEdge(Vertex(source), Vertex(destination))

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
internal class GraphSurrogate<T>(val vertices: MutableList<Graph.Vertex<T>>, val adjacencyMap: MutableMap<Int, MutableList<Int>>) {
    companion object {
        fun <T> fromGraph(graph: Graph<T>) : GraphSurrogate<T> {
            val adjMapIndices = mutableMapOf<Int, MutableList<Int>>()
            for ((source, destinations) in graph.adjacencyMap.entries) {
                val sourceIdx = graph.vertices.indexOf(source)
                adjMapIndices[sourceIdx] = destinations.map { graph.vertices.indexOf(it) }.toMutableList()
            }
            return GraphSurrogate(graph.vertices, adjMapIndices)
        }
    }

    fun toGraph(): Graph<T> {
        val graph = Graph<T>()
        for (vertex in this.vertices) {
            graph.addVertex(vertex)
        }
        for ((source, destinations) in this.adjacencyMap.entries) {
            for (destination in destinations) {
                graph.addEdge(graph.vertices[source], graph.vertices[destination])
            }
        }
        return graph
    }
}

class GraphSerializer<T>(private val dataSerializer: KSerializer<T>) : KSerializer<Graph<T>> {

    override val descriptor: SerialDescriptor = GraphSurrogate.serializer(dataSerializer).descriptor

    override fun deserialize(decoder: Decoder): Graph<T> {
        @Suppress("UNCHECKED_CAST")
        val surrogate = decoder.decodeSerializableValue(GraphSurrogate.serializer(dataSerializer))
        return surrogate.toGraph()
    }

    override fun serialize(encoder: Encoder, value: Graph<T>) {
        val surrogate = GraphSurrogate.fromGraph(value)
        encoder.encodeSerializableValue(GraphSurrogate.serializer(dataSerializer), surrogate)
    }
}