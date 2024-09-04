package bpm.server

import bpm.common.logging.KotlinLogging
import bpm.common.network.Endpoint
import bpm.common.network.Listener
import bpm.common.network.Network.new
import bpm.common.network.Server
import bpm.common.network.listener
import bpm.common.packets.Packet
import bpm.common.packets.internal.ConnectRequest
import bpm.common.packets.internal.DisconnectPacket
import bpm.common.property.Property
import bpm.common.property.PropertyMap
import bpm.common.property.configured
import bpm.common.schemas.Schemas
import bpm.common.type.NodeType
import bpm.common.vm.EvalContext
import bpm.common.workspace.packets.WorkspaceCreateRequestPacket
import bpm.common.workspace.packets.WorkspaceCreateResponsePacket
import bpm.common.workspace.Workspace
import bpm.common.workspace.WorkspaceSettings
import bpm.common.workspace.graph.Edge
import bpm.common.workspace.graph.Node
import bpm.common.workspace.graph.User
import bpm.common.workspace.packets.*
import bpm.server.lua.Notify
import org.joml.Vector2f
import org.joml.Vector4i
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object ServerRuntime : Listener {


    private val logger = KotlinLogging.logger { }

    /**
     * The `workspaces` variable is a private mutable map that stores instances of the `Workspace` class indexed by their `UUID`.
     *
     * @see Workspace
     *
     * @property workspaces A mutable map where `UUID` keys are associated with `Workspace` values.
     */
    internal val workspaces = ConcurrentHashMap<UUID, Workspace>()

    /**
     * Represents the collection of currently opened workspaces.
     *
     * This variable is a mutable map that stores workspace instances associated with unique
     * UUID identifiers. It allows adding, removing, and accessing the workspaces.
     *
     * @property openedWorkspaces The mutable map that contains the opened workspaces. The UUID
     * keys are used to uniquely identify each workspace, while the corresponding values represent
     * the workspace instances.
     */
    private val openedWorkspaces = ConcurrentHashMap<UUID, UUID>()

    /**
     * Represents a collection of users.
     *
     * @property users A mutable map of user IDs to User instances.
     */
    private val users = mutableMapOf<UUID, User>()

    /**
     * Keeps an in memory cache of workspace settings mapped to each player
     */
    private val workspaceSettings = ConcurrentHashMap<UUID, MutableMap<UUID, WorkspaceSettings>>()


    /**
     * Clears the existing workspaces list and refreshes it by loading all available workspaces.
     */
    override fun onInstall() {
        val workspaceNames = Workspace.list()
        workspaceNames.forEach { uuid ->
            val workspace = Workspace.load(uuid)
            if (workspace == null) {
                logger.warn { "Failed to load workspace: $uuid" }
                return@forEach
            }
            workspaces[workspace.uid] = workspace
            logger.debug { "Loaded workspace: $uuid" }
        }
    }


    private fun sendError(workspaceUid: UUID, msg: EvalContext.Result) {
        sendToUsersInWorkspace(workspaceUid, new<NotifyMessage> {
            icon = 0xf071
            message = msg.message
            header = msg::class.simpleName ?: "Error"
            color = "#f54242"
            lifetime = 2.5f
            type = NotifyMessage.NotificationType.ERROR
        })
    }
    /**
     * Returns true if the function was executed successfully, false otherwise.
     */
    fun execute(workspace: Workspace, functionName: String, vararg args: Any?) {
        if (workspace.needsRecompile) {
            logger.warn { "Workspace needs recompilation" }
            return
        }
        val result = EvalContext.callFunction(workspace, functionName, *args)
        if (result.isRealFailure) {
            sendError(workspace.uid, result)
            workspace.needsRecompile = true
            return
        }
        workspace.needsRecompile = false
    }

    /**
     * Called when a packet is received.
     *
     * @param packet the packet that was received
     */
    override fun onPacket(packet: Packet, from: UUID) = when (packet) {
        // When the user asks for a list of workspaces, send them the list of workspaces.
        is WorkspaceLibraryRequest -> sendWorkspaces(from)
        is WorkspaceSettingsStore -> {
            val settings = workspaceSettings.getOrPut(from) { mutableMapOf() }
            settings[packet.workspaceUid] = packet.workspaceSettings
        }

        is WorkspaceSettingsRead -> {
            val settings = workspaceSettings[from]?.get(packet.workspaceUid)
            if (settings != null) {
                server.send(new<WorkspaceSettingsLoad> {
                    this.workspaceSettings = settings
                }, from)
            }
            Unit
        }

//        is WorkspaceSelected -> openWorkspace(packet.workspaceUid, from)
        is NodeMoved -> broadCastNodeMove(from, packet)
        is WorkspaceCreateRequestPacket -> createWorkspace(packet.name, packet.description, from)
        is NodeCreateRequest -> createNode(
            users[from]?.workspaceUid ?: error("User not in workspace"),
            packet.nodeType,
            packet.position,
        )

        is NodeDeleteRequest -> {
            val workspace = workspaces[users[from]?.workspaceUid ?: error("User not in workspace")]
            workspace?.removeNode(packet.uuid)

            sendToUsersInWorkspace(users[from]?.workspaceUid ?: error("User not in workspace"), new<NodeDeleted> {
                this.uuid = packet.uuid
            })
        }

        is LinkCreateRequest -> {
            val workspace = workspaces[users[from]?.workspaceUid ?: error("User not in workspace")]
            workspace?.addLink(packet.link)
            logger.debug { "Link created: ${packet.link}" }

            sendToUsersInWorkspace(users[from]?.workspaceUid ?: error("User not in workspace"), new<LinkCreated> {
                this.link = packet.link
            })
        }

        is LinkDeleteRequest -> {
            val workspace = workspaces[users[from]?.workspaceUid ?: error("User not in workspace")]
            workspace?.removeLink(packet.uuid)
            sendToUsersInWorkspace(users[from]?.workspaceUid ?: error("User not in workspace"), new<LinkDeleted> {
                this.uuid = packet.uuid
            })
        }

        is WorkspaceCompileRequest -> {
            val workspace = workspaces[packet.workspaceId] ?: error("Workspace not found")
            compileWorkspace(workspace)
        }

        is EdgePropertyUpdate -> {
            val workspace = workspaces[users[from]?.workspaceUid ?: error("User not in workspace")]
                ?: error("Workspace not found")
            updateEdgeProperty(workspace, packet.edgeUid, packet.property, from)
        }

        is VariableCreateRequest -> {
            val workspace = workspaces[users[from]?.workspaceUid ?: error("User not in workspace")]
                ?: error("Workspace not found")
            createVariable(workspace, packet.name, packet.property, from)
        }

        is VariableDeleteRequest -> {
            val workspace = workspaces[users[from]?.workspaceUid ?: error("User not in workspace")]
                ?: error("Workspace not found")
            workspace.removeVariable(packet.name)
            sendToUsersInWorkspace(workspace.uid, new<VariableDeleted> {
                this.name = packet.name
            })
        }

        is VariableUpdateRequest -> {
            val workspace = workspaces[users[from]?.workspaceUid ?: error("User not in workspace")]
                ?: error("Workspace not found")
            workspace.updateVariable(packet.variableName, packet.property["value"])
            sendToUsersInWorkspace(workspace.uid, new<VariableUpdated> {
                this.variableName = packet.variableName
                this.property = packet.property
            })
        }

        is VariableNodeCreateRequest -> {
            val workspace = workspaces[users[from]?.workspaceUid ?: error("User not in workspace")]
                ?: error("Workspace not found")
            val type = if (packet.type == bpm.common.workspace.packets.NodeType.GetVariable) "Variables/Get Variable" else "Variables/Set Variable"
            val library = listener<Schemas>(Endpoint.Side.SERVER).library
            val nodeType = library[type] ?: error("Node type not found")
            val edges = nodeType.properties["edges"] as? Property.Object ?: error("Edges not found")
            val input = edges["name"] as? Property.Object ?: error("Input not found")
            val value = input["value"] as? Property.Object ?: error("Value not found")
            val default = value["default"] as? Property.String ?: error("Default not found")


            //Sets the default to the variable name
            default.set(packet.variableName)

            val node = createFromType(
                workspace, nodeType, packet.position
            )

            val variableName = packet.variableName
            val variable = workspace.getVariable(variableName)
            if (variable is Property.Null) run {
                logger.warn { "Failed to create node: $type. Variable not found." }
                return
            } else {
                node.properties["value"] = variable
            }

            //send the node
            sendToUsersInWorkspace(workspace.uid, new<NodeCreated> {
                this.node = node
            })

        }

        is DisconnectPacket -> {
//            server.disconnected(Endpoint.Connection(from, Endpoint.Side.SERVER))
        }

        is ConnectRequest -> {
//            server.connected(Endpoint.Connection(from, Endpoint.Side.SERVER))
        }


        else -> Unit
    }

    private fun createVariable(workspace: Workspace, name: String, property: PropertyMap, from: UUID) {
        workspace.addVariable(name, property["value"] ?: Property.Null)
        sendToUsersInWorkspace(workspace.uid, new<VariableCreated> {
            this.name = name
            this.property = property
        })
    }

    private fun updateEdgeProperty(workspace: Workspace, edgeUid: UUID, property: Property<*>, sender: UUID) {
        val edge = workspace.getEdge(edgeUid) ?: return
        edge.properties["value"] = property
        server.sendToAll(new<EdgePropertyUpdate> {
            this.edgeUid = edgeUid
            this.property = edge.properties["value"] as PropertyMap
        }, sender)
    }


    private fun compileWorkspace(workspace: Workspace) {
        workspace.save()
        workspace.needsRecompile = false
        val evalResult = EvalContext.eval(workspace)
        if (evalResult.isRealFailure) {
            sendError(workspace.uid, evalResult)
            workspace.needsRecompile = true
            logger.error(evalResult.message)
        } else {
            execute(workspace, "Run")
        }
    }

    private fun createNode(workspaceId: UUID, nodeType: String, position: Vector2f) {
        val workspace = workspaces[workspaceId] ?: run {
            logger.warn { "Failed to create node: $workspaceId" }
            return
        }

        val schema = listener<Schemas>(Endpoint.Side.SERVER).library[nodeType] ?: run {
            logger.warn { "Failed to create node: $nodeType. Unknown type." }
            return
        }
        val node = createFromType(workspace, schema, position)

        sendToUsersInWorkspace(workspaceId, new<NodeCreated> {
            this.node = node
        })
    }

    private fun sendToUsersInWorkspace(workspaceId: UUID, packet: Packet) {
        val workspace = workspaces[workspaceId]
        if (workspace == null) {
            logger.warn("Failed to find workspace")
        }

        val usersInWorkspace = openedWorkspaces.filter { (_, value) ->
            value == workspaceId
        }.keys.toTypedArray()

        for (user in usersInWorkspace) {
            server.send(packet, user)
        }
//
//        val usersNotInWorkspace = users.filter { (_, user) ->
//            user.workspaceUid != workspaceId
//        }.map { (_, user) ->
//            user
//        }.toMutableList().map { it.uid }.toTypedArray()

//        val users =


//            server.sendToAll(packet, *usersNotInWorkspace)
    }

    private fun intToUnicodeEscaped(codePoint: Int): String {
        return "\\u" + codePoint.toString(16).padStart(4, '0')
    }

    private fun createFromType(workspace: Workspace, nodeType: NodeType, position: Vector2f): Node {
        val name = nodeType["name"] as? Property.String ?: Property.String(nodeType.meta.name)
        val theme = nodeType["theme"] as? Property.Object ?: Property.Object()
        val color = theme["color"] as? Property.Vec4i ?: Property.Vec4i(Vector4i(255, 255, 255, 255))
        val edges = nodeType["edges"] as? Property.Object ?: Property.Object()
        val width = theme["width"] as? Property.Float ?: theme["width"] as? Property.Int ?: Property.Float(100f)
        val height = theme["height"] as? Property.Float ?: theme["height"] as? Property.Int ?: Property.Float(50f)
        val iconInt = theme["icon"] as? Property.Int ?: Property.Int(0)
        val newNode = configured<Node> {
            "name" to name
            "type" to nodeType.meta.group
            "color" to color
            "x" to position.x as Float
            "y" to position.y as Float
            "uid" to UUID.randomUUID()
            "width" to width
            "height" to height
            "edges" to Property.Object()
            "icon" to iconInt
        }

        // Process edges
        for ((edgeName, edgeProperty) in edges) {
            if (edgeProperty !is Property.Object) continue

            val edgeDirection = edgeProperty["direction"] as? Property.String ?: Property.String("input")
            val edgeType = edgeProperty["type"] as? Property.String ?: Property.String("exec")
            val edgeDescription = edgeProperty["description"] as? Property.String ?: Property.String("")
            val value = edgeProperty["value"] as? Property.Object ?: Property.Object()
            val icon = edgeProperty["icon"] as? Property.Int ?: Property.Int(0)
            val edge = configured<Edge> {
                "name" to Property.String(edgeName)
                "direction" to edgeDirection
                "type" to edgeType
                "description" to edgeDescription
                "uid" to Property.UUID(UUID.randomUUID())
                "value" to value
                "icon" to icon
            }

            (newNode["edges"] as Property.Object)[edgeName] = edge.properties
            workspace.addEdge(newNode, edge)
        }
        workspace.addNode(newNode)
        return newNode
    }


    /**
     * Creates a new workspace and sends the result back to the client.
     *
     * @param name The name of the new workspace.
     * @param description The description of the new workspace.
     * @param sendTo The UUID of the client to send the result to.
     */
    private fun createWorkspace(name: String, description: String, sendTo: UUID) {
        val workspace = Workspace.create(name, description)
        val response = new<WorkspaceCreateResponsePacket> {
            this.success = true
            this.workspaceUid = workspace.uid
        }
        //inject the schema into the workspace
        server.send(response, sendTo)
        logger.info { "Created new workspace for client $sendTo: ${workspace.workspaceName}" }
    }

    operator fun get(uuid: UUID): Workspace? = workspaces[uuid]


    /**
     * Opens a workspace for a given user.
     *
     * @param workspaceId The ID of the workspace to open.
     * @param sendTo The UUID of the user to open the workspace for.
     */
    fun openWorkspace(workspaceId: UUID, sendTo: UUID) {
        var workspace = workspaces[workspaceId]
        if (workspace == null) {
            workspace = Workspace.create("Untitled", "An empty workspace", workspaceId)
            workspaces[workspace.uid] = workspace
        }

        openedWorkspaces[sendTo] = workspaceId
        val user = User(sendTo, null, workspaceId)
        logger.debug { "Client '$sendTo' Opened workspace: $workspaceId, $user" }
        server.send(new<WorkspaceLoad> {
            this.workspace = workspace
        }, sendTo)
        notifyUsersOfWorkspace(sendTo)
        users[sendTo] = user
        //Send the settings too
        val settings = workspaceSettings.getOrPut(sendTo) { mutableMapOf() }
        val workspaceSettings = settings[workspaceId]
        if (workspaceSettings != null) {
            server.send(new<WorkspaceSettingsLoad> {
                this.workspaceSettings = workspaceSettings
            }, sendTo)
        }

    }

    private fun notifyUsersOfWorkspace(sendTo: UUID) {
        val workspaceId = openedWorkspaces[sendTo] ?: return
        val users = users.filter { (_, user) ->
            user.workspaceUid == workspaceId && user.uid != sendTo
        }.map { (_, user) ->
            user
        }.toMutableList()
        if (users.isNotEmpty()) server.send(new<UserConnectedToWorkspace> {
            this.users.addAll(users)
        }, sendTo)


        // now we notify the other users that a new user has joined
        val user = ServerRuntime.users[sendTo] ?: return
        server.sendToAll(new<UserConnectedToWorkspace> {
            this.users.add(user)
        }, sendTo)

    }

    /**
     * Sends workspaces to the specified recipient.
     *
     * @param sendTo The recipient's UUID to send the workspaces to.
     */
    private fun sendWorkspaces(sendTo: UUID) {
//        refreshWorkspaces()
        val valuedWorkspaces = workspaces.mapValues { (_, workspace) ->
            Pair(workspace.workspaceName, workspace.description)
        }
        val workspaces = new<WorkspaceLibrary> {
            this.workspaces.putAll(valuedWorkspaces)
        }
        server.send(workspaces, sendTo)
        logger.debug { "Sent workspaces to: $sendTo, ${workspaces.workspaces}" }
    }

    /**
     * Broadcasts a node move packet to all connected clients except the specified sender.
     *
     * @param sender the UUID of the client to exclude from receiving the packet
     * @param nodeMovePacket the NodeMoved packet to broadcast
     */
    private fun broadCastNodeMove(sender: UUID, nodeMovePacket: NodeMoved) {

        //we get the workspace id by looking up the workspace id of the client that sent the packet.
        val senderWorkspaceId = openedWorkspaces[sender] ?: return
        val workspace = workspaces[senderWorkspaceId] ?: return

        //Only finds clients that have the same workspace opened.
        val clients = openedWorkspaces.filter { (_, workspaceId) ->
            workspaceId != senderWorkspaceId
        }.keys.toMutableList()

        //update the node position in the workspace
        workspace.getNode(nodeMovePacket.uid)?.let { node ->
            node.x = nodeMovePacket.x
            node.y = nodeMovePacket.y
        }

        //add the sender to the list of clients to exclude
        clients.add(sender)
        //send the packet to all clients that have the same workspace opened, except the sender.
        server.sendToAll(nodeMovePacket, *clients.toTypedArray())
    }
    //Removes any function callbacks that were registered for the workspace
    fun closeWorkspace(workspaceUid: UUID) {
        val workspace = workspaces[workspaceUid] ?: return
        workspace.save()
//        workspaces.remove(workspaceUid)
//        openedWorkspaces.filter { (_, uid) -> uid == workspaceUid }.keys.forEach { openedWorkspaces.remove(it) }
//        users.filter { (_, user) -> user.workspaceUid == workspaceUid }.keys.forEach { users.remove(it) }

        workspace.needsRecompile = true
    }

    fun recompileWorkspace(workspaceUi: UUID) {
        val workspace = workspaces[workspaceUi] ?: return
        if (workspace.needsRecompile) {
            compileWorkspace(workspace)
        } else {
            logger.warn { "Workspace does not need recompilation" }
        }
    }


}