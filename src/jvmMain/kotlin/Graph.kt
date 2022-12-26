class Graph {
    data class Vertex<T>(val index: Int, val data: T)

    data class Edge<T,W>(val source: Vertex<T>, val destination: Vertex<T>, val weight: W? = null)

    class AdjacencyList<T,W> {
        private val adjacencyMap = mutableMapOf<Vertex<T>, ArrayList<Edge<T,W>>>()

        fun createVertex(data: T): Vertex<T> {
            val vertex = Vertex(adjacencyMap.count(), data)
            adjacencyMap[vertex] = arrayListOf()
            return vertex
        }

        fun addDirectedEdge(source: Vertex<T>, destination: Vertex<T>, weight: W? = null) {
            val edge = Edge(source, destination, weight)
            adjacencyMap[source]?.add(edge)
        }

        override fun toString(): String {
            return buildString {
                adjacencyMap.forEach { (vertex, edges) ->
                    val edgeString = edges.joinToString { it.destination.data.toString() }
                    append("${vertex.data} -> [$edgeString]\n")
                }
            }
        }
    }
}