package bpm.common.workspace.graph

import bpm.common.logging.KotlinLogging
import bpm.common.memory.Buffer
import bpm.common.property.*
import bpm.common.serial.Serial
import bpm.common.serial.Serialize
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class Graph(
    /**
     * Represents the nodes that are contained within the graph.
     */
    nodes: PropertyList = Property.List(),
    /**
     * Represents the edges that are contained within the graph.
     */
    edges: PropertyList = Property.List(),
    /**
     * Represents the connections between nodes.
     */
    links: PropertyList = Property.List(),
    /**
     * The variables that are contained within the graph.
     */
    variables: PropertyList = Property.List()
) {

    /**
     * Keeps track of nodes using a concurrent map.
     *
     * Each node is identified by a unique UUID, and can be accessed through the map using the UUID as the key.
     *
     * The `nodes` map is thread-safe, allowing multiple threads to access and modify the map concurrently without
     * explicit synchronization.
     *
     * @property nodeMap The map that holds the nodes, where the UUID is the key and the corresponding Node object is the value.
     */
    private val nodeMap: ConcurrentMap<UUID, Node> = ConcurrentHashMap()

    /**
     * Collection of edges represented as a private concurrent map.
     * The edges are stored with unique UUID keys and corresponding Edge values.
     * This map is thread-safe, allowing concurrent access and modifications.
     *
     * @property edgeMap The concurrent map that stores the edges.
     *
     * @see UUID
     * @see Edge
     * @see ConcurrentHashMap
     */
    private val edgeMap: ConcurrentMap<UUID, Edge> = ConcurrentHashMap()

    /**
     * Represents a concurrent map that maps a UUID to a list of UUIDs, representing the edges of a node.
     */
    private val nodeEdgeMap: ConcurrentMap<UUID, MutableList<UUID>> = ConcurrentHashMap()
    /**
     * Represents a collection of variables stored with their corresponding names.
     * The variables are stored in a concurrent map implementation to support concurrent access and modification.
     *
     * @property variableMap A ConcurrentHashMap that stores variable names as keys and Property objects as values.
     */
    private val variableMap: ConcurrentMap<String, Property<*>> = ConcurrentHashMap()

    /**
     * Represents a collection of links stored with their corresponding UUIDs.
     * The links are stored in a concurrent map implementation to support concurrent access and modification.
     *
     * @property linkMap A ConcurrentHashMap that stores UUIDs as keys and Link objects as values.
     */
    private val linkMap: ConcurrentMap<UUID, Link> = ConcurrentHashMap()

    init {
        Serializer.initGraph(nodes, edges, links, this, variables)
    }


    /**
     * Returns a collection of nodes contained within the graph.
     */
    val nodes: Collection<Node> get() = nodeMap.values
    /**
     * Returns a collection of edges contained within the graph.
     */
    val edges: Collection<Edge> get() = edgeMap.values
    /**
     * Immutable ref to the variables map.
     */
    val variables: Map<String, Property<*>> get() = variableMap

    /**
     * Adds a node to the system.
     *
     * @param node The node to be added.
     */
    fun addNode(node: Node) {
        nodeMap[node.uid] = node
    }

    /**
     * Returns the Node object associated with the provided UUID.
     *
     * @param uid The UUID of the Node to retrieve.
     * @return The Node object corresponding to the provided UUID, or null if no such Node exists.
     */
    fun getNode(uid: UUID): Node? {
        return nodeMap[uid]
    }

    /**
     * Adds an edge to the graph.
     *
     * @param edge the edge to be added
     */
    fun addEdge(owner: Node, edge: Edge) {
        edge.owner = owner.uid
        owner.edgeIds.add(Property.UUID(edge.uid))
        edgeMap[edge.uid] = edge
        val ownerEdges = nodeEdgeMap.getOrPut(owner.uid) { mutableListOf() }
        ownerEdges.add(edge.uid)
    }

    fun addVariable(name: String, value: Property<*>) {
        variableMap[name] = value
    }

    fun getVariable(name: String): Property<*> {
        return variableMap[name] ?: Property.Null
    }

    /**
     * Retrieves an edge identified by the given UUID.
     *
     * @param uid The unique identifier of the edge.
     * @return The edge identified by the given UUID, or null if the edge does not exist.
     */
    fun getEdge(uid: UUID): Edge? {
        return edgeMap[uid]
    }


    /**
     * Retrieves the edges associated with the given owner node.
     *
     * @param owner The owner node to retrieve edges for.
     * @return A list of edges associated with the owner node. If no edges are found, an empty list is returned.
     */
    fun getEdges(owner: UUID): List<Edge> {
        val ownerEdges = nodeEdgeMap[owner] ?: return emptyList()
        return ownerEdges.mapNotNull { edgeMap[it] }
    }
    /**
     * Retrieves a list of edges associated with the specified owner node.
     *
     * @param owner The owner node to retrieve the edges for.
     * @return A list of edges associated with the owner node.
     */
    fun getEdges(owner: Node): List<Edge> = getEdges(owner.uid)


    /**
     * Retrieves a list of links associated with the given owner node.
     *
     * @param owner The owner node to retrieve links for.
     * @return A list of links associated with the owner node. If no links are found, an empty list is returned.
     */
    fun getLinks(owner: Node): List<Link> {
        val edges = getEdges(owner)
        return getLinks().filter { link ->
            link.to in edges.map { it.uid } || link.from in edges.map { it.uid }
        }
    }

    fun getLinks(owner: UUID): List<Link> {
        val edges = getEdges(owner)
        return getLinks().filter { link ->
            link.to in edges.map { it.uid } || link.from in edges.map { it.uid }
        }
    }

    /**
     * Establishes a connection and returns a unique identifier for the connection.
     *
     * @return The unique identifier for the connection as a UUID.
     */
    fun addLink(connection: Link): UUID {
        linkMap[connection.uid] = connection
        return connection.uid
    }

    fun addLink(from: UUID, to: UUID): UUID {
        val connectionId = UUID.randomUUID()
        val connection = configured<Link> {
            "uuid" to connectionId
            "from" to from
            "to" to to
        }
        return addLink(connection)
    }


    /**
     * Retrieves the link associated with the given unique identifier.
     *
     * @param uid The unique identifier of the link.
     * @return The link associated with the given unique identifier, or null if no link is found.
     */
    fun getLink(uid: UUID): Link? {
        return linkMap[uid]
    }

    fun getLinks(): Collection<Link> {
        return linkMap.values
    }

    override fun toString(): String {
        return "Graph(nodes=$nodeMap, edges=$edgeMap, links=$linkMap, variables=$variableMap)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Graph) return false

        if (nodeMap != other.nodeMap) return false
        if (edgeMap != other.edgeMap) return false
        if (nodeEdgeMap != other.nodeEdgeMap) return false
        if (linkMap != other.linkMap) return false
        if (variableMap != other.variableMap) return false
        return true
    }

    override fun hashCode(): Int {
        var result = nodeMap.hashCode()
        result = 31 * result + edgeMap.hashCode()
        result = 31 * result + nodeEdgeMap.hashCode()
        result = 31 * result + linkMap.hashCode()
        result = 31 * result + variableMap.hashCode()
        return result
    }

    fun removeNode(uid: UUID): Node? = nodeMap.remove(uid)

    fun removeLink(uid: UUID): Link? = linkMap.remove(uid)

    fun removeVariable(name: String): Property<*> {
        return variableMap.remove(name) ?: Property.Null
    }

    fun updateVariable(name: String, property: Property<*>) {
        variableMap[name] = property
    }

    fun clear() {
        nodeMap.clear()
        edgeMap.clear()
        nodeEdgeMap.clear()
        linkMap.clear()
        variableMap.clear()
    }


    object Serializer : Serialize<Graph>(Graph::class) {

        private val queuedEdges: MutableMap<UUID, MutableList<Edge>> = mutableMapOf()
        /**
         * Initializes the graph from the given property maps.
         *
         * @param nodes The property map containing the nodes.
         * @param connections The property map containing the connections.
         */
        internal fun initGraph(
            nodes: PropertyList,
            edges: PropertyList,
            connections: PropertyList,
            graph: Graph,
            variables: PropertyList,
        ) {
            for (value in nodes) {
                logger.debug { "Adding node ${value.castOr<PropertyMap> { error("Failed to get node") }["uid"].get()} to graph, with value of $value" }
                val node = Node(value as PropertyMap)
                graph.addNode(node)

                // Add any queued edges
                val queued = queuedEdges[node.uid]
                if (queued != null) {
                    for (edge in queued) {
                        graph.addEdge(node, edge)
                    }
                }

            }

            for (value in edges) {
                logger.debug { "Adding edge ${value.cast<PropertyMap>()["uid"].get()} to graph, with value of $value" }
                val ownerid = value.cast<PropertyMap>()["owner"].cast<Property.UUID>()
                val owner = graph.getNode(ownerid.get())
                if (owner == null) {
                    //Not an error, it just hasn't been processed yet so lets queue it up
                    queuedEdges.getOrPut(ownerid.get()) { mutableListOf() }.add(Edge(value as PropertyMap))
                    continue
                }
                graph.addEdge(owner, Edge(value as PropertyMap))
            }

            for (value in connections) {
                logger.info { "Adding connection ${value.cast<PropertyMap>()["uid"].get()} to graph, with value of $value" }
                graph.addLink(Link(value as PropertyMap))
            }

            for (value in variables) {
                logger.info { "Adding variable ${value.cast<PropertyMap>()["name"].get()} to graph, with value of $value" }
                val map = value.cast<PropertyMap>()
                val name = map["name"].cast<Property.String>().get()
                val prop = map["value"]
                graph.addVariable(name, prop)
            }
        }
        /**
         * Deserializes the contents of the Buffer and returns an instance of type T.
         *
         * @return The deserialized object of type T.
         */
        override fun deserialize(buffer: Buffer): Graph {
            val nodes = Serial.read<PropertyList>(buffer)
            val edges = Serial.read<PropertyList>(buffer)
            val connections = Serial.read<PropertyList>(buffer)
            val variables = Serial.read<PropertyList>(buffer)
            if (nodes == null) {
                logger.warn { "Failed to read nodes in to graph from buffer!" }
                throw Exception("Failed to read nodes in to graph from buffer!")
            }
            if (edges == null) {
                logger.warn { "Failed to read edges in to graph from buffer!" }
                throw Exception("Failed to read edges in to graph from buffer!")
            }
            if (connections == null) {
                logger.warn { "Failed to read connections in to graph from buffer!" }
                throw Exception("Failed to read connections in to graph from buffer!")
            }
            if (variables == null) {
                logger.warn { "Failed to read variables in to graph from buffer!" }
                throw Exception("Failed to read variables in to graph from buffer!")
            }
            return Graph(nodes, edges, connections, variables)
        }
        /**
         * Serializes the provided value into the buffer.
         *
         * @param value The value to be serialized.
         */
        override fun serialize(buffer: Buffer, value: Graph) {
            val nodeList = Property.List()
            value.nodeMap.forEach {
                nodeList.add(it.value.properties)
            }
            Serial.write(buffer, nodeList)
            val edgeList = Property.List()
            value.edgeMap.forEach {
                edgeList.add(it.value.properties)
            }
            Serial.write(buffer, edgeList)
            val connectionList = Property.List()
            value.linkMap.forEach {
                connectionList.add(it.value.properties)
            }
            Serial.write(buffer, connectionList)
            val variableList = Property.List()
            value.variableMap.forEach {
                val obj = Property.Object()
                obj["name"] = Property.String(it.key)
                obj["value"] = it.value
                variableList.add(obj)
            }
            Serial.write(buffer, variableList)
        }
    }


    companion object {

        private val logger = KotlinLogging.logger { }
    }


}