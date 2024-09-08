package bpm.mc.visual

import bpm.Bpm
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import bpm.client.runtime.ClientRuntime
import bpm.common.network.Client
import bpm.common.workspace.packets.*
import imgui.ImColor
import imgui.ImGui
import imgui.ImVec2
import imgui.ImVec4
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiStyleVar
import imgui.flag.ImGuiWindowFlags
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.util.*

object NodeEditorGui : Screen(Component.literal("Node Editor")) {
    private var workspaceUuid: UUID? = null
    private var openTime: Long = 0
    private val inventoryRenderer = MinecraftInventoryRenderer()

    private val testItemStacks = listOf(
        ItemStack.EMPTY,
        ItemStack(Items.DIAMOND, 3),
        ItemStack(Items.GOLDEN_APPLE),
        ItemStack(Items.IRON_SWORD),
        ItemStack(Items.BOW),
        ItemStack(Items.ARROW, 64),
        ItemStack(Items.DIAMOND_PICKAXE),
        ItemStack(Items.DIAMOND_AXE),
        ItemStack(Items.DIAMOND_SHOVEL),
        ItemStack(Items.DIAMOND_HOE),
        ItemStack(Items.DIAMOND_HELMET),
    )

    fun open(uuid: UUID) {
        workspaceUuid = uuid
        Minecraft.getInstance().setScreen(this)
    }

    override fun init() {
        super.init()
        openTime = System.currentTimeMillis()
        Client {
            it.send(NodeLibraryRequest())
        }
        Overlay2D.skipped = true
    }

    override fun render(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        try {
            ClientRuntime.newFrame()
            ClientRuntime.process()
            ClientRuntime.endFrame()
//
//            ClientRuntime.newFrame()
//            inventoryRenderer.renderInventory(testItemStacks, pGuiGraphics)
//            ClientRuntime.endFrame()
        } catch (e: Exception) {
            Bpm.LOGGER.error("Error rendering NodeEditorGui", e)
        }
    }

    override fun onClose() {
        super.onClose()
        Overlay2D.skipped = false
        Client {
            it.send(WorkspaceCompileRequest(workspaceUuid!!))
            val settings = ClientRuntime.workspace?.settings
                ?: error("Workspace settings not found. This should not happen")
            it.send(WorkspaceSettingsStore(workspaceUuid!!, settings))
        }
        ClientRuntime.closeCanvas()
        workspaceUuid = null
    }

    override fun isPauseScreen(): Boolean = false
}
