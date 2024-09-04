package bpm.client.runtime

import imgui.ImGui
import imgui.ImGuiIO
import imgui.flag.ImGuiConfigFlags
import imgui.gl3.ImGuiImplGl3
import imgui.glfw.ImGuiImplGlfw
import net.minecraft.client.Minecraft
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import bpm.client.font.FontType
import bpm.client.font.Fonts
import bpm.client.runtime.windows.CanvasWindow
import bpm.common.logging.KotlinLogging
import bpm.common.network.Listener
import bpm.common.network.NetUtils
import bpm.common.network.Network.new
import bpm.common.packets.Packet
import bpm.common.packets.internal.ConnectResponsePacket
import bpm.common.packets.internal.DisconnectPacket
import bpm.common.utils.FontAwesome
import bpm.common.workspace.Workspace
import bpm.common.workspace.graph.Node
import bpm.common.workspace.packets.*
import bpm.mc.visual.NodeEditorGui
import com.mojang.blaze3d.systems.RenderSystem
import org.lwjgl.glfw.GLFW
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The `Runtime` interface provides methods for checking the status of keyboard keys and mouse buttons.
 *
 * This should be the only class needing to be implemented for a new platform.
 */
@OnlyIn(Dist.CLIENT)
object ClientRuntime : Listener {

    /**
     * The client uid that the runtime should use to connect to the server
     */
    private val clientUid: UUID by lazy { Minecraft.getInstance().player?.uuid ?: error("Player UUID not available") }
    private val cachedWorkspaces: ConcurrentHashMap<UUID, Workspace> = ConcurrentHashMap()
    private var running = false
    //Caches the workspace into memory
    internal var workspace: Workspace? = null
        set(value) {
            field = value
            if (value != null) {
                cachedWorkspaces[value.uid] = value
            }
        }

    /**This stores the glfw backend implementation for imgui**/
    private lateinit var imGuiGlfw: ImGuiImplGlfw
    private lateinit var imGuiGl3: ImGuiImplGl3
    private var queuedWorkspaceLibrary: WorkspaceLibrary? = null
    private lateinit var io: ImGuiIO
    private val started = AtomicBoolean(false)
    private var canvasWindow: CanvasWindow? = null
    private val logger = KotlinLogging.logger { }

    operator fun get(workspaceUid: UUID): Workspace? {

        val cahced = cachedWorkspaces[workspaceUid]
        if (cahced != null) {
            return cahced
        }
        //We request the workspace from the server
        client.send(new<WorkspaceSelected> {
            this.workspaceUid = workspaceUid
        })

        return null
    }

    /**
     * Represents the currently selected node in the application.
     *
     * The `selectedNode` variable is used to store a reference to the node that is currently selected by the user.
     * This variable can hold a reference to a `Node` object, or be `null` if no node is selected.
     *
     * It is important to note that this variable is nullable, meaning it can hold a null value. This is useful to
     * indicate the absence of a selected node in cases where no node has been explicitly selected.
     *
     * @property selectedNode The currently selected `Node` object, or `null` if no node is selected.
     */
    var selectedNode: Node? = null
        set(value) {
            field = value
            client.send(new<NodeSelected> {
                this.nodeId = value?.uid ?: NetUtils.DefaultUUID
                if (value != null) logger.info { "Selected node: ${value.uid}" }
                else logger.info { "Selected node: null" }
            })
        }

    /**
     * initialize the runtime with the given window handle
     */
    fun start(windowHandle: Long): ClientRuntime {
        if (started.getAndSet(true)) {
            logger.warn { "Runtime has already been started" }
            return this
        }
        ImGui.createContext();
        configureRenderspace()
        // Initialize fonts
        initializeFonts()
        imGuiGlfw = ImGuiImplGlfw()
        imGuiGl3 = ImGuiImplGl3()
        imGuiGlfw.init(windowHandle, true) // Set to true for callbacks
        imGuiGl3.init("#version 150") // Use version 150 for better compatibility

        Platform.install(windowHandle)
        if (!registerFonts()) {
            logger.error { "Failed to register fonts, exiting..." }
            stop()
            return this
        }

        client.start()
        running = true
        logger.info { "Runtime has started" }
        return this
    }

    private fun registerFonts(attempts: Int = 0): Boolean {
        try {
            Fonts.registerFonts()
            return true
        } catch (e: Exception) {
            if (attempts < 3) {
                logger.error { "Failed to register fonts, retrying..." }
                registerFonts(attempts + 1)
            } else {
                logger.error { "Failed to register fonts after 3 attempts" }
            }
        }
        return false
    }

    /**
     * Stop the runtime, we should clean up any resources here and unhook any installed glfw callbacks
     */
    fun stop() {
        client.stop()
        running = false
        logger.warn { "Runtime has been stopped" }
    }

    fun newFrame() {
        imGuiGlfw.newFrame()
        ImGui.newFrame()
    }


    fun endFrame() {
        ImGui.render()
        imGuiGl3.renderDrawData(ImGui.getDrawData())

        if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
            val backupWindowPtr = GLFW.glfwGetCurrentContext()
            ImGui.updatePlatformWindows()
            ImGui.renderPlatformWindowsDefault()
            GLFW.glfwMakeContextCurrent(backupWindowPtr)
        }
    }
    /**
     * Should be called once per frame to process events from the main thread
     */
    fun process() {
        if (!running) return

        if (canvasWindow != null) {
            canvasWindow?.render()
        }

    }


    fun connect() = client.connect()

    fun disconnect() = client.disconnect()


    /**
     * Handles the event when a connection is established.
     *
     * @param uuid The unique identifier of the connected client.
     */
    override fun onConnect(uuid: UUID) {
        client.send(new<WorkspaceLibraryRequest> {
            logger.info { "Requesting workspace library" }
        })
    }

    /**
     * Sets the workspace for the application.
     *
     * @param workspace The workspace to set.
     */
    fun setWorkspace(workspace: UUID) {
        logger.debug { "Opening workspace: $workspace" }
        client.send(new<WorkspaceSelected> {
            this.workspaceUid = workspace
        })
    }

    fun closeCanvas() {
        logger.debug { "Resetting workspace" }
        canvasWindow?.close()
    }

    /**
     * Called when a packet is received from the worker thread
     *
     * @param packet the packet that was received
     */
    override fun onPacket(packet: Packet, from: UUID): Unit = when (packet) {

        is ConnectResponsePacket -> {
            if (!packet.valid) {
                logger.error { "Connection was not valid" }
                client.terminate()
            }
            synchronized(client.listeners) {
                client.listeners.forEach {
                    it.onConnect(NetUtils.DefaultUUID)
                }
            }
        }

        is DisconnectPacket -> {
            //TODO: update local client representation of the users in the workspace
        }

        is WorkspaceLoad -> {
            this.workspace = packet.workspace
            packet.workspace?.let {
                // Open the node editor gui, has to be done on the main thread
                RenderSystem.recordRenderCall { NodeEditorGui.open(it.uid) }
            }

            if (canvasWindow == null) {
                canvasWindow = CanvasWindow(this)
            }
            canvasWindow?.close()
            canvasWindow?.open()
            logger.debug { "Received workspace load response: $packet" }
        }

        is WorkspaceSettingsLoad -> {
            workspace!!.settings = packet.workspaceSettings
            logger.debug { "Received workspace settings: ${packet.workspaceSettings}" }
        }

        is NotifyMessage -> {
            logger.info { "Received message: ${packet.message}" }
        }

        is WorkspaceCreateResponsePacket -> {
            if (packet.success) {
                logger.info { "Workspace created successfully: ${packet.workspaceUid}" }
                // Refresh the workspace list
                client.send(new<WorkspaceLibraryRequest> {})
            } else {
                logger.warn { "Failed to create workspace" }
            }
        }

        else -> Unit
    }

    /**
     * Sends a request to create a new workspace.
     *
     * @param name The name of the new workspace.
     * @param description The description of the new workspace.
     */
    fun createWorkspace(name: String, description: String) {
        client.send(new<WorkspaceCreateRequestPacket> {
            this.name = name
            this.description = description
        })
    }


    /**
     * Sets up the ImGui configuration
     */
    private fun configureRenderspace() {
        io = ImGui.getIO()
        io.iniFilename = null
        io.addConfigFlags(ImGuiConfigFlags.NavEnableKeyboard)
        io.addConfigFlags(ImGuiConfigFlags.DockingEnable)
        io.configViewportsNoTaskBarIcon = true
        io.configViewportsNoAutoMerge = true

    }

    private fun initializeFonts() {
        try {
            Fonts.register("Inter", 8..50)
            Fonts.register(
                "Fa",
                24..84,
                listOf(FontType.Regular),
                Pair(FontAwesome.IconMin, FontAwesome.IconMax),
                merge = true
            )
            Fonts.registerFonts()
        } catch (e: Exception) {
            logger.error(e) { "Failed to initialize fonts" }
            throw RuntimeException("Font initialization failed", e)
        }
    }


    fun compile(workspace: Workspace) {
        client.send(new<WorkspaceCompileRequest> {
            this.workspaceId = workspace.uid
        })
    }

    fun reloadNodeLibrary() {
        client.send(new<NodeLibraryReloadRequest> {})
    }

    fun openCanvas() {
        canvasWindow?.open()
    }


    /**
     * Enumeration representing different types of mouse buttons.
     *
     * @property value The integer value associated with the mouse button.
     */
    enum class MouseButton(val value: Int) {

        LEFT(0), RIGHT(1), MIDDLE(2), UNKNOWN(-1);

        @Suppress("EnumValuesSoftDeprecate")
        companion object {

            private val entries = MouseButton.values().associateBy { it.value }
            /**
             * Retrieves the Mouse associated with the given value.
             *
             * @param value The integer value for which to retrieve the Mouse.
             * @return The Mouse associated with the given value, or UNKNOWN if not found.
             */
            fun from(value: Int): MouseButton = this.entries.getOrDefault(value, UNKNOWN)
        }
    }

    /**
     * Represents a key.
     *
     * @param value the value of the key code, we copy glfw's key codes as they are the most common
     */
    enum class Key(val value: Int) {

        SPACE(32), APOSTROPHE(39), COMMA(44), MINUS(45), PERIOD(46), SLASH(47), ZERO(48), ONE(49), TWO(50), THREE(
            51
        ),
        FOUR(52), FIVE(53), SIX(54), SEVEN(55), EIGHT(56), NINE(57), SEMICOLON(59), EQUAL(61), A(65), B(66), C(67), D(68), E(
            69
        ),
        F(70), G(71), H(72), I(73), J(74), K(75), L(76), M(77), N(78), O(79), P(80), Q(81), R(82), S(83), T(84), U(85), V(
            86
        ),
        LEFT_SHIFT(340), RIGHT_SHIFT(344), LEFT_CONTROL(341), RIGHT_CONTROL(345), LEFT_ALT(342), RIGHT_ALT(346), LEFT_SUPER(
            343
        ),
        W(87), X(88), Y(89), Z(90), LEFT_BRACKET(91), BACKSLASH(92), RIGHT_BRACKET(93), GRAVE_ACCENT(96), WORLD_1(161), WORLD_2(
            162
        ),
        ESCAPE(256), ENTER(257), TAB(258), BACKSPACE(259), INSERT(260), DELETE(261), RIGHT(262), LEFT(263), DOWN(264), UP(
            265
        ),
        PAGE_UP(266), PAGE_DOWN(267), HOME(268), END(269), CAPS_LOCK(280), SCROLL_LOCK(281), NUM_LOCK(282), PRINT_SCREEN(
            283
        ),
        PAUSE(284), F1(290), F2(291), F3(292), F4(293), F5(294), F6(295), F7(296), F8(297), F9(298), F10(299), F11(300), F12(
            301
        ),
        F13(302), F14(303), F15(304), F16(305), F17(306), F18(307), F19(308), F20(309);

        @Suppress("EnumValuesSoftDeprecate")
        companion object {

            private val entries = values().associateBy { it.value }
            /**
             * Retrieves the Key associated with the given value.
             *
             * @param value The integer value for which to retrieve the Key.
             * @return The Key associated with the given value, or UNKNOWN if not found.
             */
            fun from(value: Int): Key = this.entries.getOrDefault(value, SPACE)
        }
    }

}