package bpm.mc.visual

import bpm.client.render.panel.ConsolePanel
import bpm.client.render.panel.PanelManager
import bpm.client.runtime.ClientRuntime
import imgui.ImGui
import imgui.flag.ImGuiWindowFlags
import bpm.client.runtime.windows.NotificationManager
import bpm.common.network.Client
import bpm.common.network.Listener
import bpm.common.packets.Packet
import bpm.common.workspace.packets.NodeLibraryRequest
import bpm.common.workspace.packets.NotifyMessage
import bpm.common.workspace.packets.WorkspaceCompileRequest
import bpm.common.workspace.packets.WorkspaceSettingsStore
import bpm.pipe.PipeNetwork
import bpm.pipe.proxy.PacketProxiedStatesRequest
import java.util.*

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics

object ClientGui : Listener {

    private val notificationManager: NotificationManager = NotificationManager()
    private var workspaceUuid: UUID? = null
    private var workspaceOpen = false

    fun renderPre(graphics: GuiGraphics) {
        if (workspaceOpen) return
//        ClientRuntime.newFrame()
//        processOverlay()
//        ClientRuntime.endFrame()
    }

    fun renderPost(graphics: GuiGraphics) {
        if (!workspaceOpen) return
        ClientRuntime.processPost(graphics)
    }


    fun open(uuid: UUID) {
        workspaceOpen = true
        Client.send(NodeLibraryRequest())
        //Send a request for all proxies
        PipeNetwork.ProxyManagerClient.requestProxies(uuid)
        val nodeEditorGui = NodeEditorGui()
        //Set this here simply to capture input, we don't do any rendering here
        Minecraft.getInstance().setScreen(nodeEditorGui)
        workspaceUuid = uuid
    }

    fun close() {
        workspaceOpen = false
        Client {
            it.send(WorkspaceCompileRequest(workspaceUuid!!))
            val settings = ClientRuntime.workspace?.settings
                ?: error("Workspace settings not found. This should not happen")
            it.send(WorkspaceSettingsStore(workspaceUuid!!, settings))
        }
        ClientRuntime.closeCanvas()
    }


    private fun processOverlay() {
        val mainViewport = ImGui.getMainViewport()
        ImGui.setNextWindowPos(mainViewport.posX, mainViewport.posY)
        ImGui.setNextWindowSize(mainViewport.sizeX, mainViewport.sizeY)
        ImGui.begin(
            "ClientGuiOverlay",
            ImGuiWindowFlags.NoTitleBar or ImGuiWindowFlags.NoResize or ImGuiWindowFlags.NoMove or ImGuiWindowFlags.NoScrollbar or ImGuiWindowFlags.NoScrollWithMouse or ImGuiWindowFlags.NoBringToFrontOnFocus or ImGuiWindowFlags.NoNavFocus
                    or ImGuiWindowFlags.NoBackground or ImGuiWindowFlags.NoDocking or ImGuiWindowFlags.NoSavedSettings or ImGuiWindowFlags.NoNav
        )
        val drawList = ImGui.getWindowDrawList()
        val displaySize = ImGui.getIO().displaySize
        notificationManager.renderNotifications(drawList, displaySize)
        ImGui.text("Hello, world!")
        ImGui.end()
    }


    override fun onPacket(packet: Packet, from: UUID) {
        if (packet is NotifyMessage) {
            notificationManager.addNotification(packet)
            ConsolePanel.log(packet.message, ConsolePanel.LogLevel.ERROR)
        }

    }

}