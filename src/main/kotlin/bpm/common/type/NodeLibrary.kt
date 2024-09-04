package bpm.common.type

import com.charleskorn.kaml.*
import bpm.common.property.*

import bpm.common.logging.KotlinLogging
import bpm.common.workspace.Workspace
import org.joml.*
import java.nio.file.*


/**
 * Represents a workspace containing a set of node types, stored within a specified directory.
 * The class provides functionality to parse and manage node types, creating, manipulating and
 * storing them in specialized collections.
 *
 * The node types are parsed from the specified directory upon creation of the `NodeWorkspace`.
 * Additionally, the class exposes a `parse` function to manually trigger re-parsing of the node types.
 *
 * This class is extensible and can be used as a base class for more specialized node workspaces.
 * Error handling and logging capabilities are built into the class, providing easy tracking of operations.
 *
 * @param directory the directory path where the node types are stored.
 *
 * @property directory the directory path where the node types are stored.
 * @property parsedNodes a map, keyed by `NodeTypeMeta`, of the parsed `NodeType` instances
 * @property registerNodes a map, keyed by node type names, of `NodeTypeMeta` instances
 * @property log the logger instance used for logging operations and exceptions
 */
class NodeLibrary(private val directory: Path? = null) : Iterable<NodeTypeMeta> {

    /** The key is the name of the node type, uses the [NodeTypeMeta] property **/
    private val parsedNodes = mutableMapOf<NodeTypeMeta, NodeType>()

    /** The key is the name of the node type, uses the [NodeTypeMeta.nodeTypeName] property **/
    private val registerNodes: MutableMap<String, NodeTypeMeta> = mutableMapOf()
    private val log = KotlinLogging.logger {}

    init {
        if (directory != null) readFrom(directory)
    }

    /**
     * Reads node types from the specified directory path.
     *
     * @param path The directory path to read node types from.
     */
    fun readFrom(path: Path) {
        //Copy from root schemas
        val rootSchema = Workspace.childPath("schemas")
        if (!rootSchema.toFile().exists()) {
            log.info { "Copying schemas from root schemas directory." }
            rootSchema.toFile().copyRecursively(path.toFile(), false)
        }
        registerNodes.clear()
        log.info { "Reading node types from directory: $path" }
        parseNodeTypes(performPartialParse(readMetasFrom(path)))
        log.info { "Completed reading node types." }
    }

    /**
     * Loads node types from the given property list.
     *
     * @param properties the property list containing node type information
     */
    fun loadFrom(properties: PropertyList): NodeLibrary {
        registerNodes.clear()
        log.info { "Reading node types from property list." }
        for (property in properties) {
            if (property !is PropertyMap) continue
            if (!property.contains("name") || !property.contains("group")) continue
            val name = property["name"].cast<Property.String>().get()
            val group = property["group"].cast<Property.String>().get()
            val meta = NodeTypeMeta(name, group)
            val nodeType = NodeType(meta, property)
            parsedNodes[meta] = nodeType
            registerNodes[meta.nodeTypeName] = meta
            mergeProperties(meta)
        }
        log.info { "Completed reading node types. Loaded ${parsedNodes.size} node types." }
        return this
    }

    //TODO: use last modified time to check if the file has changed
    fun reload() {
        if (directory != null) {
            log.warn { "Reloading NodeNamespace" }
            parsedNodes.clear()
            registerNodes.clear()
            readFrom(directory)
        }
    }

    fun clear() {
        parsedNodes.clear()
        registerNodes.clear()
    }


    /**
     * Retrieves and returns a copy of all the values of the parsed nodes as a list.
     *
     * @return the list containing all the values of the parsed nodes.
     */
    fun collect(): List<NodeType> = parsedNodes.values.toList()

    /**
     * Collects the elements of the current list and returns a new [PropertyList]
     * containing the properties of those elements.
     *
     * @return The [PropertyList] containing the properties of the elements in the list.
     */
    fun collectToPropertyList(): PropertyList = Property.List(collect().map { it.properties }.toMutableList())


    /**
     * Retrieves the node of the specified name from the parsed nodes.
     *
     * @param name The name of the node to retrieve.
     * @return The node with the specified name if found, otherwise null.
     */
    operator fun get(name: String): NodeType? = parsedNodes[registerNodes[name]]

    /**
     * Adds a NodeType to the parsedNodes and registerNodes maps.
     *
     * @param nodeType The NodeType to be added.
     */
    fun add(nodeType: NodeType) {
        parsedNodes[nodeType.meta] = nodeType
        registerNodes[nodeType.meta.nodeTypeName] = nodeType.meta
    }

    /**
     * Adds the specified [NodeType] to this collection.
     *
     * @param nodeType The [NodeType] to be added to this collection.
     */
    operator fun plusAssign(nodeType: NodeType) = add(nodeType)

    /**
     * Reads node type metadata from the specified directory.
     *
     * @return A list of NodeTypeMeta objects representing the node type metadata found in the directory.
     */
    private fun readMetasFrom(directory: Path): List<NodeTypeMeta> {
        log.debug { "Reading node type metadata from directory." }
        val nodeTypes = mutableListOf<NodeTypeMeta>()
        Files.walk(directory).filter { it.toString().endsWith(".node") }.forEach {
            val name = it.fileName.toString().removeSuffix(".node")
            val group = it.parent.fileName.toString()
            nodeTypes.add(NodeTypeMeta(it, name, group))
        }
        log.info { "Found ${nodeTypes.size} node type metadata." }
        return nodeTypes
    }


    /**
     * Parses the given list of partial node type metadata and returns a list of fully transformed metadata.
     *
     * @param partials The list of partial node type metadata to parse.
     * @return The list of fully transformed node type metadata.
     */
    private fun performPartialParse(partials: List<NodeTypeMeta>): List<NodeTypeMeta> {
        log.info { "Performing partial parsing for node types." }
        val transformedMetas = arrayListOf<NodeTypeMeta>()
        for (meta in partials) {
            //get the name of the node type
            val name: String = meta.yaml.getScalar("name")?.content ?: meta.name

            //get the group of the node type
            val group: String = meta.yaml.getScalar("group")?.content ?: meta.group

            //The extension type of the node type
            val extends: String = meta.yaml.getScalar("extends")?.content ?: meta.extends

            transformedMetas.add(NodeTypeMeta(meta.path, name, group, extends))
        }
        log.info { "Partial parsing completed. Transformed ${transformedMetas.size} node types." }
        return transformedMetas
    }

    /**
     * Parses a list of node types and stores them in the appropriate data structures.
     *
     * @param nodeTypes The list of node types to parse.
     */
    private fun parseNodeTypes(nodeTypes: List<NodeTypeMeta>) {
        val types = sortAndValidateNodeExtendOrder(nodeTypes)
        for (meta in types) {
            log.debug { "Parsing node type: ${meta.nodeTypeName}" }
            val nodeType = NodeType(meta, parsePropertyBagForNode(meta.yaml))
            parsedNodes[meta] = nodeType
            registerNodes[meta.nodeTypeName] = meta
            mergeProperties(meta)
            log.debug { "Parsed node type: ${meta.nodeTypeName}" }
        }
    }

    /**
     * Sorts and validates the node types in extended order.
     *
     * @param nodeTypes The list of node types to sort and validate.
     * @return The sorted and validated list of node types in extended order.
     */
    private fun sortAndValidateNodeExtendOrder(nodeTypes: List<NodeTypeMeta>): List<NodeTypeMeta> {
        log.info { "Sorting and validating node extend order." }
        val graph = buildDependencyGraph(nodeTypes)
        val sortedList = topologicalSort(graph).reversed()
        log.info { "Sorting and validation completed. Sorted list: $sortedList" }
        return sortedList
    }

    /**
     * Builds a dependency graph based on the given list of node types.
     * The graph is represented as a map where each key is a node type and the corresponding value is a list of its dependent node types.
     *
     * @param nodeTypes The list of node types to build the dependency graph from.
     * @return The dependency graph as a map of node types and their dependent node types.
     */
    private fun buildDependencyGraph(nodeTypes: List<NodeTypeMeta>): Map<NodeTypeMeta, List<NodeTypeMeta>> {
        val graph = mutableMapOf<NodeTypeMeta, MutableList<NodeTypeMeta>>()
        val graphMapper = mutableMapOf<String, NodeTypeMeta>()

        for (node in nodeTypes) {
            val extends = node.extends
            if (extends.isBlank()) {
                continue
            }
            val parent = graphMapper.getOrPut(extends) {
                nodeTypes.firstOrNull { it.nodeTypeName == extends }
                    ?: throw Exception("Node type $extends does not exist.")
            }
            graph.getOrPut(parent) { mutableListOf() }.add(node)
        }
        return graph
    }

    /**
     * Performs a topological sort on the given graph and returns the sorted list of nodes.
     *
     * @param graph the directed acyclic graph represented as a map of nodes and their adjacent nodes
     * @return the sorted list of nodes
     */
    private fun topologicalSort(graph: Map<NodeTypeMeta, List<NodeTypeMeta>>): List<NodeTypeMeta> {
        val visited = mutableSetOf<NodeTypeMeta>()
        val sortedList = mutableListOf<NodeTypeMeta>()

        for (node in graph.keys) {
            topologicalSortVisit(node, graph, visited, sortedList)
        }

        return sortedList
    }

    /**
     * Performs a topological sort visit on a given node in a graph.
     *
     * @param node The node to perform the topological sort visit on.
     * @param graph The graph represented as a map of nodes to their adjacent neighbors.
     * @param visited A mutable set to keep track of visited nodes.
     * @param sortedList A mutable list to store the sorted nodes in topological order.
     */
    private fun topologicalSortVisit(
        node: NodeTypeMeta,
        graph: Map<NodeTypeMeta, List<NodeTypeMeta>>,
        visited: MutableSet<NodeTypeMeta>,
        sortedList: MutableList<NodeTypeMeta>
    ) {
        if (!visited.contains(node)) {
            visited.add(node)
            graph[node]?.forEach { neighbor ->
                topologicalSortVisit(neighbor, graph, visited, sortedList)
            }
            sortedList.add(node)
        }
    }

    /**
     * Parses the given YamlMap and converts it into a PropertyBag.
     *
     * @param meta The YamlMap to be parsed.
     * @return The parsed PropertyBag.
     */
    private fun parsePropertyBagForNode(yaml: YamlMap): PropertyMap {
        val properties = Property.Object()
        for ((key, value) in yaml.entries) {
            when (key.content) {
                "edges" -> properties[key.content] = parseEdges(value as YamlMap)
                else -> properties[key.content] = parseProperty(value)
            }
        }
        return properties
    }

    private fun parseEdges(yaml: YamlMap): PropertyMap {
        val edges = Property.Object()
        for ((key, value) in yaml.entries) {
            val edgeProperties = Property.Object()
            val edgeYaml = value as YamlMap
            edgeProperties["name"] = Property.String(key.content)
            edgeProperties["description"] = Property.String(edgeYaml.getScalar("description")?.content ?: "")
            edgeProperties["direction"] = Property.String(edgeYaml.getScalar("direction")?.content ?: "input")
            edgeProperties["type"] = Property.String(edgeYaml.getScalar("type")?.content ?: "exec")
            if (edgeYaml.get<YamlMap>("value") != null) {
                edgeProperties["value"] = parsePropertyBagForNode(edgeYaml.get("value")!!)
            }
            edges[key.content] = edgeProperties
        }
        return edges
    }

    /**
     * Merges the properties of a given node type with the properties of its parent node type, if it extends another node type.
     *
     * @param meta The metadata of the node type to merge properties for.
     * @throws Exception if the node type does not exist or if the parent node type cannot be found (likely due to a circular dependency).
     */
    private fun mergeProperties(meta: NodeTypeMeta) {
        val ours = parsedNodes[meta] ?: throw Exception("Node type ${meta.nodeTypeName} does not exist.")
        val properties = ours.properties
        // If the node type extends another node type, we need to merge the properties of the parent node type with the properties of the current node type.
        if (meta.extends.isNotBlank()) {
            val parent = parsedNodes[registerNodes[meta.extends]]
                ?: throw Exception("Failed to find parent type ${meta.extends} while parsing ${meta.nodeTypeName}. This is likely due to a circular dependency.")
            mergeParentProperties(parent.properties, properties)
        }
    }
    /**
     * Merges properties from a parent node type into the current node type.
     * Properties in the current node type take precedence over the parent node type.
     *
     * @param parentProperties The parent node type's properties.
     * @param childProperties The current node type's properties.
     */
    private fun mergeParentProperties(parentProperties: PropertyMap, childProperties: PropertyMap) {
        parentProperties.forEach { (key, parentProperty) ->
            val childProperty = childProperties[key]
            when {
                // If the child does not have this property, inherit from the parent
                childProperty == Property.Null -> childProperties[key] = parentProperty
                // If both parent and child have an ObjectProperty, merge them recursively
                parentProperty is Property.Object && childProperty is Property.Object -> mergeParentProperties(
                    parentProperty, childProperty
                )
            }
        }
    }

    /**
     * Parses the given YamlNode and converts it into a Property.
     * @param yaml The YamlNode to be parsed.
     * @return The parsed Property.
     */
    private fun parseProperty(yaml: YamlNode): Property<*> {
        return when (yaml) {
            is YamlScalar -> parseScalar(yaml) ?: Property.String(yaml.content)
            is YamlMap -> parsePropertyBagForNode(yaml)
            is YamlList -> parseList(yaml)
            else -> Property.Null
        }
    }

    /**
     * Parses a YamlList into a Property.
     *
     * @param yaml The YamlList to parse.
     * @return The parsed Property.
     */
    private fun parseList(yaml: YamlList): Property<*> {
        val list = Property.List()
        for (value in yaml.items) {
            list.add(parseProperty(value))
        }
        return list
    }

    /**
     * Tries to parse the given YAML scalar into an instance of the specified property type.
     *
     * @param yaml The YAML scalar to parse.
     * @param parser The function that converts the YAML scalar to the desired type.
     * @return An instance of the specified property type if the parsing succeeds, null otherwise.
     */
    private inline fun <reified P : Property<*>> tryParse(
        yaml: YamlScalar, crossinline parser: YamlScalar.() -> Any
    ): P? = try {
        val typeClass = P::class
        val value = yaml.parser()
        typeClass.constructors.first().call(value)
    } catch (e: Exception) {
        null
    }

    /**
     * Parses a scalar value from a YAML node and returns a corresponding property object.
     * This is an expensive operation, so it should be used sparingly.
     *
     * @param yaml The YAML scalar node to parse.
     * @return The parsed property object, or null if the scalar value could not be parsed.
     */
    private fun parseScalar(yaml: YamlScalar): Property<*>? {
        return tryParse<Property.Boolean>(yaml) { toBoolean() } ?: tryParse<Property.Int>(yaml) { toInt() }
        ?: tryParse<Property.Long>(yaml) { toLong() } ?: tryParse<Property.Float>(yaml) { toFloat() }
        ?: tryParse<Property.Double>(yaml) { toDouble() } ?: tryParse<Property.Short>(yaml) { toShort() }
        ?: tryParse<Property.Byte>(yaml) { toByte() } ?: tryParse<Property.Float>(yaml) { toFloat() }
        ?: tryParse<Property.Double>(yaml) { toDouble() }
        ?: tryParse<Property.Class>(yaml) { Class.forName(this.content) } ?: tryParse<Property.Vec4i>(yaml) {
            toColor(
                content
            )
        } ?: tryParse<Property.Vec2i>(yaml) { content.toVec2i() }
        ?: tryParse<Property.Vec3i>(yaml) { content.toVec3i() } ?: tryParse<Property.Vec4i>(yaml) { content.toVec4i() }
        ?: tryParse<Property.Vec2f>(yaml) { content.toVec2f() } ?: tryParse<Property.Vec3f>(yaml) { content.toVec3f() }
        ?: tryParse<Property.Vec4f>(yaml) { content.toVec4f() } ?: tryParse<Property.Char>(yaml) { content }
    }

    /**
     * Converts a string to a Vector2i object.
     *
     * @param content The string to be converted.
     * @return The Vector2i object.
     * @throws IllegalArgumentException if the string does not represent a valid Vector2i object.
     */
    private fun String.toVec2i(): Vector2i {
        val values = this.split(",").map { it.trim().toInt() }
        if (values.size != 2) throw IllegalArgumentException("Invalid Vec2i format: $this")
        return Vector2i(values[0], values[1])
    }

    /**
     * Converts a string to a Vector3i object.
     *
     * @param content The string to be converted.
     * @return The Vector3i object.
     * @throws IllegalArgumentException if the string does not represent a valid Vector3i object.
     */
    private fun String.toVec3i(): Vector3i {
        val values = this.split(",").map { it.trim().toInt() }
        if (values.size != 3) throw IllegalArgumentException("Invalid Vec3i format: $this")
        return Vector3i(values[0], values[1], values[2])

    }

    /**
     * Converts a string to a Vector4i object.
     *
     * @param content The string to be converted.
     * @return The Vector4i object.
     * @throws IllegalArgumentException if the string does not represent a valid Vector4i object.
     */

    private fun String.toVec4i(): Vector4i {
        val values = this.split(",").map { it.trim().toInt() }
        if (values.size != 4) throw IllegalArgumentException("Invalid Vec4i format: $this")
        return Vector4i(values[0], values[1], values[2], values[3])

    }

    /**
     * Converts a string to a Vector2f object.
     *
     * @param content The string to be converted.
     * @return The Vector2f object.
     * @throws IllegalArgumentException if the string does not represent a valid Vector2f object.
     */
    private fun String.toVec2f(): Vector2f {
        val values = this.split(",").map { it.trim().toFloat() }
        if (values.size != 2) throw IllegalArgumentException("Invalid Vec2f format: $this")
        return Vector2f(values[0], values[1])
    }

    /**
     * Converts a string to a Vector3f object.
     *
     * @param content The string to be converted.
     * @return The Vector3f object.
     * @throws IllegalArgumentException if the string does not represent a valid Vector3f object.
     */
    private fun String.toVec3f(): Vector3f {
        val values = this.split(",").map { it.trim().toFloat() }
        if (values.size != 3) throw IllegalArgumentException("Invalid Vec3f format: $this")
        return Vector3f(values[0], values[1], values[2])
    }

    /**
     * Converts a string to a Vector4f object.
     *
     * @param content The string to be converted.
     * @return The Vector4f object.
     * @throws IllegalArgumentException if the string does not represent a valid Vector4f object.
     */
    private fun String.toVec4f(): Vector4f {
        val values = this.split(",").map { it.trim().toFloat() }
        if (values.size != 4) throw IllegalArgumentException("Invalid Vec4f format: $this")
        return Vector4f(values[0], values[1], values[2], values[3])
    }

    /**
     * Converts a color represented as a string to a Vector4i color object.
     *
     * @param content The color string to be converted.
     *                The supported formats are:
     *                - Hexadecimal color format: starts with '#', followed by six hexadecimal digits (e.g., "#FF0000" for red)
     *                - RGB color format: starts with 'rgb(', followed by three comma-separated integer values in the range [0, 255] (e.g., "rgb(255, 0, 0)" for red)
     *                - RGBA color format: starts with 'rgba(', followed by four comma-separated integer values in the range [0, 255] (e.g., "rgba(255, 0, 0, 0.5)" for red with 50% transparency)
     *
     * @return The color represented as a Vector4i object, where the x, y, z, and w components represent the red, green, blue, and alpha channels respectively.
     *
     * @throws IllegalArgumentException if the provided color format is invalid.
     */
    private fun toColor(content: String): Vector4i {
        return when {
            content.startsWith("#") -> parseHexColor(content)
            content.startsWith("rgb(") -> parseRgbColor(content)
            content.startsWith("rgba(") -> parseRgbaColor(content)
            else -> throw IllegalArgumentException("Invalid color format: $content")
        }
    }

    /**
     * Parses a hexadecimal color string and returns a Vector4i representing the color.
     *
     * @param hex The hexadecimal color string in the format "#RRGGBB" or "#AARRGGBB" where
     *            RR, GG, BB, and AA are two-digit hexadecimal values representing the red, green,
     *            blue, and alpha components of the color respectively.
     *
     * @return A Vector4i representing the color with components R, G, B, and A.
     *
     * @throws IllegalArgumentException If the hex color format is invalid.
     */
    private fun parseHexColor(hex: String): Vector4i {
        val color = hex.removePrefix("#")
        return when (color.length) {
            6 -> Vector4i(
                color.substring(0, 2).toInt(16), color.substring(2, 4).toInt(16), color.substring(4, 6).toInt(16), 255
            )

            8 -> Vector4i(
                color.substring(2, 4).toInt(16),
                color.substring(4, 6).toInt(16),
                color.substring(6, 8).toInt(16),
                color.substring(0, 2).toInt(16)
            )

            else -> throw IllegalArgumentException("Invalid hex color format: $hex")
        }
    }

    /**
     * Parses the RGB color string and returns a Vector4i representation of the color.
     *
     * @param rgb The RGB color string in the format "rgb(r, g, b)" where r, g, and b are the red, green, and blue color values respectively.
     * @throws IllegalArgumentException If the RGB color string is invalid.
     * @return The Vector4i representation of the RGB color, where the first three components are the red, green, and blue values respectively, and the last component is the alpha value set to 255.
     */
    private fun parseRgbColor(rgb: String): Vector4i {
        val values = rgb.removePrefix("rgb(").removeSuffix(")").split(",").map { it.trim().toFloat() }
        if (values.size != 3) throw IllegalArgumentException("Invalid RGB color format: $rgb")

        val (r, g, b) = values.map { (it * if (it <= 1) 255 else 1).toInt() }
        return Vector4i(r, g, b, 255)
    }

    /**
     * Parses a string representation of an RGBA color and returns a Vector4i object representing the color values.
     *
     * @param rgba The string representation of the RGBA color in the format "rgba(r, g, b, a)", where r, g, b, and a are the red, green, blue, and alpha values respectively.
     * Note: The color values can be specified as floating point values between 0 and 1, or as integer values between 0 and 255.
     *
     * @return A Vector4i object representing the color values of the specified RGBA color.
     *
     * @throws IllegalArgumentException if the input string does not conform to the expected format or if the number of color values is not equal to 4.
     */
    private fun parseRgbaColor(rgba: String): Vector4i {
        val values = rgba.removePrefix("rgba(").removeSuffix(")").split(",").map { it.trim().toFloat() }
        if (values.size != 4) throw IllegalArgumentException("Invalid RGBA color format: $rgba")

        val (r, g, b, a) = values.map { (it * if (it <= 1) 255 else 1).toInt() }
        return Vector4i(r, g, b, a)
    }

    override fun iterator(): Iterator<NodeTypeMeta> {
        return parsedNodes.keys.iterator()
    }


    /**
     * Returns a string representation of the object.
     *
     * The returned string includes information about the NodeTypeNamespace object, including its directory,
     * parsed nodes, and registered nodes.
     *
     * The string format is as follows:
     *
     * NodeTypeNamespace(directory={directory})
     * Parsed Nodes:
     *   - {nodeTypeName}:
     *     - Path: {path}
     *     - Properties: {formattedPropertyBag}
     * Registered Nodes:
     *   - {name}: {nodeTypeName}
     *
     * @return a string representation of the object.
     */
    override fun toString(): String {
        return buildString {
            append("NodeNamespace(directory=$directory)\n")
            append("Parsed Nodes:\n")
            parsedNodes.forEach { (meta, nodeType) ->
                append("  - ${meta.nodeTypeName}:\n")
                append("    - Path: ${meta.path}\n")
                append("    - Properties: ${formatPropertyBag(nodeType.properties, "     ")}\n")
            }
            append("Registered Nodes:\n")
            registerNodes.forEach { (name, meta) ->
                append("  - $name: ${meta.nodeTypeName}\n")
            }
        }
    }

    companion object {

        /**
         * Formats a property bag to a string representation.
         *
         * @param propertyBag The property bag to be formatted.
         * @param indent The indentation string.
         * @return The formatted property bag as a string.
         */
        fun formatPropertyBag(propertyBag: PropertyMap, indent: String): String {
            return buildString {
                append("{\n")
                propertyBag.forEach { (key, value) ->
                    append("$indent  $key: ${formatProperty(value, "$indent  ")}\n")
                }
                append("$indent}")
            }
        }

        /**
         * Formats the given property based on its type.
         *
         * @param property The property to be formatted.
         * @param nextIndent The indentation level for the next line.
         * @return The formatted property as a string.
         */
        fun formatProperty(property: Property<*>, nextIndent: String): String {
            return when (property) {
                is Property.Object -> formatPropertyBag(property, nextIndent)
                is Property.List -> formatArrayProperty(property, nextIndent)
                else -> property.toString()
            }
        }

        /**
         * Formats an array property by adding indentation and line breaks.
         *
         * @param arrayProperty The array property to format.
         * @param nextIndent The indentation to use for the next level of properties.
         * @return The formatted array property as a string.
         */
        fun formatArrayProperty(arrayProperty: PropertyList, nextIndent: String): String {
            return buildString {
                append("[\n")
                arrayProperty.get().forEach { value ->
                    append("$nextIndent  ${formatProperty(value, "$nextIndent  ")}\n")
                }
                append("$nextIndent]")
            }
        }
    }

}