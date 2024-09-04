package bpm.mc.visual

import bpm.Bpm
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import bpm.client.runtime.ClientRuntime
import bpm.common.network.Client
import bpm.common.workspace.packets.*
import java.util.*

class NodeEditorGui(private val workspaceUuid: UUID) : Screen(Component.literal("Node Editor")) {

    private var openTime: Long = 0

    override fun init() {
        super.init()
        openTime = System.currentTimeMillis()
        Client {
            it.send(NodeLibraryRequest())
//            it.send(WorkspaceSelected(workspaceUuid))
        }

        Overlay2D.skipped = true
    }

    override fun render(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        try {
            ClientRuntime.newFrame()
            ClientRuntime.process()
            ClientRuntime.endFrame()
        } catch (e: Exception) {
            Bpm.LOGGER.error("Error rendering NodeEditorGui", e)
        }
    }

    override fun onClose() {
        super.onClose()
        //Fix for nodes not being removed when closing the gui
        Overlay2D.skipped = false
        Client {
            it.send(WorkspaceCompileRequest(workspaceUuid))
            val settings = ClientRuntime.workspace?.settings
                ?: error("Workspace settings not found. This should not happen")
            it.send(WorkspaceSettingsStore(workspaceUuid, settings))
        }
        ClientRuntime.closeCanvas()
    }

    override fun isPauseScreen(): Boolean = false

    companion object {

        fun open(uuid: UUID) {
            Minecraft.getInstance().setScreen(NodeEditorGui(uuid))
        }
    }
}