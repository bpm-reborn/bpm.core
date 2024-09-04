package bpm.common.workspace

import bpm.common.logging.KotlinLogging
import bpm.common.memory.Buffer
import bpm.common.property.Property
import bpm.common.property.PropertyList
import bpm.common.serial.Serial
import bpm.common.serial.Serialize
import bpm.common.type.NodeLibrary
import bpm.common.workspace.graph.*
import net.neoforged.fml.loading.FMLPaths
import org.joml.Vector2f
import java.nio.file.Path
import java.util.UUID


/**
 * A data class representing a workspace.
 *
 * @property graph The graph associated with the workspace.
 * @property nodeLibrary The node library property of the workspace.
 * @property workspaceName The name of the workspace.
 * @property users The users registered in the workspace.
 * @property description The description of the workspace (default is "Default Workspace").
 * @property uid The unique identifier of the workspace (auto-generated if not provided).
 */
data class Workspace(
    val graph: Graph,
    val nodeLibrary: NodeLibrary,
    val workspaceName: String,
    val users: MutableMap<UUID, User>,
    val description: String = "Default Workspace",
    val uid: UUID = UUID.randomUUID(),
    var settings: WorkspaceSettings = WorkspaceSettings()
) {

    /**
     * This flag is used to determine if there was any error during the last compilation.
     * If so, we need to recompile the workspace before execution can continue.
     */
    var needsRecompile = false

    /**
     * Returns the center coordinates of the current viewport.
     *
     * @return A Vector2f representing the center coordinates of the viewport.
     */
    val viewportCenter: Vector2f
        get() {
            val centerX = settings.bounds.x + (settings.bounds.z - settings.bounds.x) / 2
            val centerY = settings.bounds.y + (settings.bounds.w - settings.bounds.y) / 2
            return Vector2f(centerX, centerY)
        }
    /**
     * Converts the absolute position to a position relative to the current settings.
     *
     * @param absPos the absolute position to convert
     * @return the converted relative position
     */
    fun convertPosition(absPos: Vector2f): Vector2f = convertPosition(absPos.x, absPos.y)

    /**
     * Converts the given absolute position to a scaled and adjusted position
     * based on the current zoom level and scrolled position.
     *
     * @param absX The absolute x-coordinate of the position.
     * @param absY The absolute y-coordinate of the position.
     * @return A Vector2f object representing the converted position.
     */
    fun convertPosition(absX: Float, absY: Float): Vector2f {
        val zoom = settings.zoom

        // Apply zoom scaling and adjust for scrolled position
        val x = (absX * zoom) + settings.scrolled.x
        val y = (absY * zoom) + settings.scrolled.y

        return Vector2f(x, y)
    }

    /**
     * Converts the absolute size represented by a Vector2f into another Vector2f.
     *
     * @param absSize The absolute size represented by a Vector2f.
     * @return The converted size represented by another Vector2f.
     */
    fun convertSize(absSize: Vector2f): Vector2f = convertSize(absSize.x, absSize.y)

    /**
     * Converts the absolute width and height to scaled width and height based on the current zoom level.
     *
     * @param absWidth The absolute width of the object.
     * @param absHeight The absolute height of the object.
     * @return A Vector2f object representing the scaled width and height.
     */
    fun convertSize(absWidth: Float, absHeight: Float): Vector2f {
        val zoom = settings.zoom
        val width = absWidth * zoom
        val height = absHeight * zoom
        return Vector2f(width, height)
    }


    /**
     * Registers a user in the system.
     *
     * @param user The user object to be registered.
     */
    fun registerUser(user: User) {
        users[user.uid] = user
    }

    fun getEdge(uid: UUID): Edge? {
        return graph.getEdge(uid)
    }

    /**
     * Retrieves the user with the specified UID.
     *
     * @param uid the unique identifier of the user
     * @return the user with the specified UID, or null if no such user exists
     */
    operator fun get(uid: UUID): User? {
        return users[uid]
    }

    /**
     * Adds a node to the graph.
     *
     * @param node the node to be added
     * @return true if the node was successfully added, false if the node with the same UID already exists
     */
    fun addNode(node: Node): Boolean {
        if (graph.getNode(node.uid) != null) {
            logger.warn { "Attempted to add an already added node with uid ${node.uid}" }
            return false
        }
        graph.addNode(node)
        return true
    }

    fun addEdge(owner: Node, edge: Edge) {
        graph.addEdge(owner, edge)
    }

    fun addLink(from: UUID, to: UUID) {
        graph.addLink(from, to)
    }

    fun addLink(connection: Link) {
        graph.addLink(connection)
    }

    fun addVariable(name: String, property: Property<*>) {
        graph.addVariable(name, property)
    }

    fun getVariable(name: String): Property<*> {
        return graph.getVariable(name)
    }

    /**
     * Retrieves a Node based on the given UID.
     *
     * @param uid The unique identifier of the Node to retrieve.
     * @return The Node with the specified UID, or null if no Node with the UID is found.
     */
    fun getNode(uid: UUID): Node? = graph.getNode(uid)

    /**
     * Removes a node from the graph based on the given UID.
     *
     * @param uid The unique identifier of the node to be removed.
     */
    fun removeNode(uid: UUID) = graph.removeNode(uid)

    /**
     * Removes a link from the graph based on the given UID.
     *
     * @param uid The unique identifier of the link to be removed.
     */
    fun removeLink(uid: UUID) = graph.removeLink(uid)
    /**
     * Removes a variable from the graph based on the given name.
     *
     * @param name The name of the variable to be removed.
     */
    fun removeVariable(name: String) = graph.removeVariable(name)

    fun updateVariable(name: String, property: Property<*>) {
        graph.updateVariable(name, property)
    }

    /**
     * Saves the data.
     *
     * This method is responsible for saving the data. It should be called to persist
     * any changes made to the data.
     *
     * @throws Exception if an error occurs while saving the data.
     */
    fun save() {
        val path = childPath("workspaces", "$uid.ws", createDirs = false)
        Serial.write(path, this)
        logger.info { "Wrote workspace to file $path" }
    }

    fun variableIteratior(iterable: (String, Property<*>) -> Unit) {
        graph.variables
    }

    /**
     * Serializer is a class that provides methods for serializing and deserializing instances
     * of the Workspace class.
     */
    object Serializer : Serialize<Workspace>(Workspace::class) {

        /**
         * Deserializes the contents of the Buffer and returns an instance of type T.
         *
         * @return The deserialized object of type T.
         */
        override fun deserialize(buffer: Buffer): Workspace {
            val uid = buffer.readUUID()
            val workspaceName = buffer.readString()
            val description = buffer.readString()
            val graph = Serial.read<Graph>(buffer) ?: error("Failed to read graph from buffer!")
            val library = Serial.read<PropertyList>(buffer) ?: error("Failed to read library from buffer!")
            val nodeLibrary = NodeLibrary().loadFrom(library)
            val userCount = buffer.readInt()
            val users = mutableMapOf<UUID, User>()
            repeat(userCount) {
                val user = Serial.read<User>(buffer) ?: error("Failed to read user from buffer!")
                users[user.uid] = user
            }
            val settings = Serial.read<WorkspaceSettings>(buffer) ?: error("Failed to read settings from buffer!")
            return Workspace(graph, nodeLibrary, workspaceName, users, description, uid, settings)
        }
        /**
         * Serializes the provided value into the buffer.
         *
         * @param value The value to be serialized.
         */
        override fun serialize(buffer: Buffer, value: Workspace) {
            buffer.writeUUID(value.uid)
            buffer.writeString(value.workspaceName)
            buffer.writeString(value.description)
            Serial.write(buffer, value.graph)
            Serial.write(buffer, value.nodeLibrary.collectToPropertyList())
            buffer.writeInt(value.users.size)
            value.users.forEach { (_, user) ->
                Serial.write(buffer, user)
            }
            Serial.write(buffer, value.settings)
        }

    }

    companion object {

        private val logger = KotlinLogging.logger { }

        init {
            val rootPath = rootPath()
            if (!rootPath.toFile().exists()) {
                rootPath.toFile().mkdirs()
            }

        }

        /**
         * Loads a workspace with the given name from the file system.
         *
         * @param workspaceName the name of the workspace to load
         * @return the loaded workspace or null if the workspace doesn't exist or is not a file
         */
        fun load(workspaceName: String): Workspace? {
            val workspaceFile = childPath("workspaces", "$workspaceName.ws", createDirs = false).toFile()
            if (!workspaceFile.exists() || !workspaceFile.isFile) {
                logger.warn { "Attempted to load null workspace with name '$workspaceFile'" }
                return null
            }
            return Serial.read<Workspace>(workspaceFile.toPath())
        }

        /**
         * Retrieves a list of all available workspaces.
         *
         * @return A list of workspace names.
         */
        fun list(): List<String> {
            val folder = childPath("workspaces", createDirs = false).toFile()
            if (!folder.exists()) {
                logger.warn { "No workspace folder found!" }
                return emptyList()
            }
            val folders = folder.listFiles()?.map { it.nameWithoutExtension }?.toList()
            if (folders == null) {
                logger.warn { "Failed to locate any workspaces in workspace folder!" }
                return emptyList()
            }
            return folders
        }


        fun childPath(vararg children: String, createDirs: Boolean = true): Path {
            val path = Path.of(rootPath().toString(), *children)
            if (!path.toFile().exists() && createDirs) {
                path.toFile().mkdirs()
            }
            return path
        }

        fun rootPath(): Path {
            val workspaces = FMLPaths.GAMEDIR.get().resolve("workspaces")
            if (!workspaces.toFile().exists()) {
                workspaces.toFile().mkdirs()
            }

            return workspaces
        }

        /**
         * Creates a new workspace with the given name and description.
         *
         * @param name The name of the new workspace.
         * @param description The description of the new workspace.
         * @return The newly created Workspace object.
         */
        fun create(name: String, description: String, uuid: UUID = UUID.randomUUID()): Workspace {
            val graph = Graph()
            val nodeLibrary = NodeLibrary()
            val users = mutableMapOf<UUID, User>()
            val workspace = Workspace(graph, nodeLibrary, name, users, description, uuid)
            workspace.save()
            logger.info { "Created new workspace: $name" }
            return workspace
        }
    }
}