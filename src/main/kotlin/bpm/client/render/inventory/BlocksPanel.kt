package bpm.client.render.inventory

import bpm.client.font.Fonts
import bpm.client.runtime.ClientRuntime
import bpm.client.runtime.windows.CanvasGraphics
import bpm.client.utils.toVec2f
import bpm.client.utils.use
import imgui.ImColor
import imgui.ImDrawList
import imgui.ImGui
import imgui.flag.*
import imgui.type.ImBoolean
import imgui.type.ImString
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import org.joml.Vector2f

object BlockListModal {

    private const val COLUMNS = 9
    private const val ROWS = 5  // Fixed number of rows per page
    private const val ITEMS_PER_PAGE = COLUMNS * ROWS
    private val isOpen = ImBoolean(false)
    private val searchQuery = ImString(256)
    private var currentPage = 0
    private val bodyFont = Fonts.getFamily("Inter")["Regular"]
    private val recordedDrawCalls = mutableListOf<(gfx: GuiGraphics) -> Unit>()

    // Minecraft-accurate colors
    private val backgroundColor = ImColor.rgba(198, 198, 198, 255)
    private val slotColor = ImColor.rgba(139, 139, 139, 255)
    private val slotBorderColor = ImColor.rgba(55, 55, 55, 255)
    private val textColor = ImColor.rgba(63, 63, 63, 255)

    // Base sizes (will be scaled)
    private const val BASE_SLOT_SIZE = 18f
    private const val BASE_PADDING = 1f
    private const val BASE_INVENTORY_PADDING = 7f
    private const val BASE_SEARCH_HEIGHT = 16f
    private const val BASE_INVENTORY_HEIGHT = (3 * BASE_SLOT_SIZE + BASE_INVENTORY_PADDING + BASE_SLOT_SIZE)
    private const val BASE_WINDOW_WIDTH = (COLUMNS * (BASE_SLOT_SIZE + BASE_PADDING) + BASE_INVENTORY_PADDING * 2)
    private const val BASE_NAV_BUTTON_WIDTH = 20f
    private const val BASE_NAV_BUTTON_HEIGHT = 16f

    // Scaled dimensions
    private val guiScale get() = (ClientRuntime.workspace?.settings?.zoom ?: 1f) * 2f
    private val SLOT_SIZE get() = BASE_SLOT_SIZE * guiScale
    private val SLOT_PADDING get() = BASE_PADDING * guiScale
    private val SLOT_SPACING get() = SLOT_SIZE + SLOT_PADDING
    private val INVENTORY_PADDING get() = BASE_INVENTORY_PADDING * guiScale
    private val SEARCH_HEIGHT get() = BASE_SEARCH_HEIGHT * guiScale
    private val WINDOW_WIDTH get() = BASE_WINDOW_WIDTH * guiScale
    private val NAV_BUTTON_WIDTH get() = 10f * guiScale
    private val NAV_BUTTON_HEIGHT get() = 8f * guiScale

    // Item hover state
    private var hoveredItemStack: ItemStack? = null
    private var hoveredItemPos = Vector2f()

    fun show() {
        isOpen.set(true)
        currentPage = 0
    }

    fun render(gui: CanvasGraphics) {
        if (!isOpen.get()) return

        hoveredItemStack = null

        // Calculate dimensions
        val viewport = ImGui.getMainViewport()
        val modalWidth = WINDOW_WIDTH
        val modalHeight = SEARCH_HEIGHT + INVENTORY_PADDING * 4 +
                (ROWS * SLOT_SPACING) + BASE_INVENTORY_HEIGHT * guiScale

        // Center the modal
        ImGui.setNextWindowPos(
            viewport.centerX - modalWidth / 2,
            viewport.centerY - modalHeight / 2,
            ImGuiCond.Always
        )
        ImGui.setNextWindowSize(modalWidth, modalHeight)

        val windowFlags = ImGuiWindowFlags.NoResize or
                ImGuiWindowFlags.NoMove or
                ImGuiWindowFlags.NoCollapse or
                ImGuiWindowFlags.NoDocking or
                ImGuiWindowFlags.NoTitleBar

        // Minecraft-style window appearance
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, INVENTORY_PADDING, INVENTORY_PADDING)
        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0f)
        ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 1f)
        ImGui.pushStyleColor(ImGuiCol.WindowBg, backgroundColor)
        ImGui.pushStyleColor(ImGuiCol.Border, slotBorderColor)

        if (ImGui.begin("Block Selection", isOpen, windowFlags)) {
            val drawList = ImGui.getWindowDrawList()
            val startPos = ImGui.getCursorScreenPos()

            // Get filtered blocks and calculate total pages
            val filteredBlocks = BlockCache.search(searchQuery.get())
            val totalPages = ((filteredBlocks?.size ?: 0) + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE
            currentPage = currentPage.coerceIn(0, maxOf(0, totalPages - 1))

            // Navigation and search bar row
            renderNavigationAndSearch(drawList, startPos.toVec2f, totalPages)
            ImGui.dummy(0f, INVENTORY_PADDING)

            // Calculate grid start position
            val gridStartX = startPos.x
            val gridStartY = ImGui.getCursorScreenPos().y

            // Render item grid for current page
            val startIdx = currentPage * ITEMS_PER_PAGE
            val endIdx = minOf(startIdx + ITEMS_PER_PAGE, filteredBlocks?.size ?: 0)

            filteredBlocks?.subList(startIdx, endIdx)?.forEachIndexed { index, blockData ->
                val row = index / COLUMNS
                val col = index % COLUMNS
                val x = gridStartX + col * SLOT_SPACING
                val y = gridStartY + row * SLOT_SPACING

                renderSlot(drawList, x, y)

                recordedDrawCalls.add { gfx ->
                    gui.renderBlockItem(
                        blockData.itemStack,
                        x + SLOT_PADDING,
                        (y - SLOT_PADDING * 2),
                        SLOT_SIZE - SLOT_PADDING * 2
                    )
                }
                handleItemHover(x, y, blockData)
            }

            // Player inventory
            ImGui.setCursorPosY(ImGui.getWindowHeight() - (BASE_INVENTORY_HEIGHT * guiScale) - INVENTORY_PADDING)
            val player = Minecraft.getInstance().player
            if (player != null) {
                renderPlayerInventory(drawList, player, gui)
            }
        }
        ImGui.end()

        ImGui.popStyleVar(3)
        ImGui.popStyleColor(2)
    }

    private fun renderNavigationAndSearch(drawList: ImDrawList, startPos: Vector2f, totalPages: Int) {
        // Left navigation button
        val startY = ImGui.getCursorPosY()


        // Search bar
//        ImGui.sameLine()
        // Scale the spacing with GUI scale
//        ImGui.setCursorPosX(ImGui.getCursorPos().x - 2 * guiScale)

        // Calculate search width accounting for scaled spacing
        val searchWidth = (((WINDOW_WIDTH - NAV_BUTTON_WIDTH * 4)) - 4 * guiScale)

        if (MinecraftUI.input(
                "search",
                searchQuery,
                searchWidth,
                NAV_BUTTON_HEIGHT,
                "Search..."
            )
        ) {
            currentPage = 0  // Reset to first page when search changes
        }

        // Right navigation button
        ImGui.setCursorPosX(ImGui.getWindowWidth() - NAV_BUTTON_WIDTH * 2 - INVENTORY_PADDING - 4 * guiScale)
        ImGui.setCursorPosY(startY)
        if (MinecraftUI.button("<", NAV_BUTTON_WIDTH, NAV_BUTTON_HEIGHT, currentPage > 0) && currentPage > 0) {
            currentPage--
        }
        ImGui.setCursorPosX(ImGui.getWindowWidth() - NAV_BUTTON_WIDTH - INVENTORY_PADDING - 2 * guiScale)
        ImGui.setCursorPosY(startY)
        if (MinecraftUI.button(
                ">",
                NAV_BUTTON_WIDTH,
                NAV_BUTTON_HEIGHT,
                currentPage < totalPages - 1
            ) && currentPage < totalPages - 1
        ) {
            currentPage++
        }
    }

    private fun renderPlayerInventory(drawList: ImDrawList, player: Player, gui: CanvasGraphics) {

        val startX = ImGui.getCursorScreenPos().x
        val startY = ImGui.getCursorScreenPos().y

        ImGui.setCursorScreenPos(startX, startY - INVENTORY_PADDING - 2f * guiScale)
        MinecraftUI.sectionTitle("Inventory")


        // Main inventory (3 rows)
        for (row in 0..2) {
            for (col in 0..8) {
                val x = startX + col * SLOT_SPACING
                val y = startY + row * SLOT_SPACING
                renderSlot(drawList, x, y)

                val index = (row * 9 + col + 9)
                val stack = player.inventory.items[index]
                if (!stack.isEmpty) {
                    recordedDrawCalls.add { gfx ->
                        gui.renderBlockItem(
                            stack,
                            x + SLOT_PADDING,
                            (y - SLOT_PADDING * 2),
                            SLOT_SIZE - SLOT_PADDING * 2
                        )
                    }
                }
            }
        }

        // Hotbar
        val hotbarY = startY + 3 * SLOT_SPACING + INVENTORY_PADDING
        for (i in 0..8) {
            val x = startX + i * SLOT_SPACING
            renderSlot(drawList, x, hotbarY)

            val stack = player.inventory.items[i]
            if (!stack.isEmpty) {
                recordedDrawCalls.add { gfx ->
                    gui.renderBlockItem(
                        stack,
                        x + SLOT_PADDING,
                        hotbarY - SLOT_PADDING * 2,
                        SLOT_SIZE - SLOT_PADDING * 2
                    )
                }
            }
        }
    }

    private fun renderSlot(drawList: ImDrawList, x: Float, y: Float) {
        drawList.addRectFilled(
            x, y,
            x + SLOT_SIZE,
            y + SLOT_SIZE,
            slotColor,
            0f
        )
        drawList.addRect(
            x, y,
            x + SLOT_SIZE,
            y + SLOT_SIZE,
            slotBorderColor,
            0f,
            0,
            1f
        )
    }

    private fun handleItemHover(x: Float, y: Float, blockData: CachedBlockData) {
        if (ImGui.isMouseHoveringRect(x, y, x + SLOT_SIZE, y + SLOT_SIZE)) {
            hoveredItemStack = blockData.itemStack
            hoveredItemPos = Vector2f(x, y)

            if (ImGui.isMouseClicked(ImGuiMouseButton.Left)) {
                println("Selected block: ${blockData.blockId}")
                isOpen.set(false)
            }
        }
    }

    fun renderPost(gfx: GuiGraphics, graphics: CanvasGraphics) {
        if (!isOpen.get()) return

        val viewport = ImGui.getMainViewport()
        val transformedPos = graphics.toScreenSpaceVector(viewport.pos.x, viewport.pos.y)
        val transformedSize = graphics.toScreenSpaceVector(viewport.sizeX, viewport.sizeY)

        gfx.enableScissor(
            transformedPos.x.toInt(),
            transformedPos.y.toInt(),
            (transformedPos.x + transformedSize.x).toInt(),
            (transformedPos.y + transformedSize.y).toInt()
        )

        recordedDrawCalls.forEach { it(gfx) }
        recordedDrawCalls.clear()


        gfx.disableScissor()

        hoveredItemStack?.let { stack ->
            val pos = graphics.toScreenSpaceVector(ImGui.getMousePos().x, ImGui.getMousePos().y)
            gfx.renderTooltip(
                Minecraft.getInstance().font,
                stack,
                pos.x.toInt(),
                pos.y.toInt()
            )
        }
    }
}