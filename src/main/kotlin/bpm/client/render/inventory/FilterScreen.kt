package bpm.client.render.inventory

import bpm.client.font.Fonts
import bpm.client.runtime.windows.CanvasGraphics
import bpm.client.utils.toVec2f
import bpm.client.utils.use
import bpm.common.utils.FontAwesome
import imgui.ImColor
import imgui.ImDrawList
import imgui.ImGui
import imgui.type.ImBoolean
import imgui.type.ImString
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import org.joml.Vector2f


import bpm.client.utils.use
import imgui.flag.*
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import java.util.Vector


object FilterScreen {

    // Fonts
    private val guiScale get() = Minecraft.getInstance().options.guiScale().get().toFloat()
    private val guiFontSize get() = (Minecraft.getInstance().options.guiScale().get().toFloat() * 10f).toInt()
    private val headerFont = Fonts.getFamily("Inter")["ExtraBold"][guiFontSize]
    private val bodyFont = Fonts.getFamily("Inter")["Regular"][guiFontSize]
    private val iconFont = Fonts.getFamily("Fa")["Regular"][guiFontSize]

    // Colors - matching VariablesMenu
    private val backgroundColor = ImColor.rgba(30, 30, 30, 240)
    private val headerColor = ImColor.rgba(40, 40, 40, 255)
    private val textColor = ImColor.rgba(220, 220, 220, 255)
    private val accentColor = ImColor.rgba(100, 65, 165, 255)
    private val gradientTopColor = ImColor.rgba(60, 60, 65, 255)
    private val gradientBottomColor = ImColor.rgba(45, 45, 50, 255)
    private val inputBgColor = ImColor.rgba(22, 22, 22, 255)

    // UI State
    private val isOpen = ImBoolean(false)
    private val searchQuery = ImString(256)
    private var showBlockSelection = false
    private var onFilterCreated: ((Filter) -> Unit)? = null
    private val filters = mutableListOf<Filter>()
    private var filterToRemove: Int? = null
    private var boxPosition = Vector2f(20f, 20f)
    private val boxWidth = 350f
    private val boxHeight = 400f  // Fixed height
    private var scrollY = 0f
    private var closingProgress = 1f
    private var isClosing = false

    val position: Vector2f
        get() = Vector2f(boxPosition.x, boxPosition.y)
    val size = Vector2f(boxWidth, boxHeight)

    // Temporary state for new filter creation
    private val tempMatchNbt = ImBoolean(false)
    private val tempIsBlacklist = ImBoolean(false)

    // Dragging state
    private var isDragging = false
    private var dragOffset = Vector2f()
    private val recordedDrawCalls = mutableListOf<(gfx: CanvasGraphics) -> Unit>()

    // Animation
    private var currentScale = 1f
    private val animationSpeed = 5f

    fun show(callback: (Filter) -> Unit) {
        isOpen.set(true)
        onFilterCreated = callback
        searchQuery.set("")
        filters.clear()
        isClosing = false
        tempMatchNbt.set(false)
        tempIsBlacklist.set(false)
    }

    fun hide() {
        isOpen.set(false)
        onFilterCreated = null
        isClosing = true
    }

    fun render(gfx: CanvasGraphics) {
        if (!isOpen.get()) return

        recordedDrawCalls.clear()
        updateAnimation()

        ImGui.pushID("FilterScreen")

        // Begin main window
        ImGui.setNextWindowPos(boxPosition.x, boxPosition.y)
        ImGui.setNextWindowSize(boxWidth, boxHeight * closingProgress)

        val windowFlags = ImGuiWindowFlags.NoTitleBar or
                ImGuiWindowFlags.NoResize or
                ImGuiWindowFlags.NoMove or
                ImGuiWindowFlags.NoScrollbar or
                ImGuiWindowFlags.NoCollapse

        ImGui.begin("FilterScreen", isOpen, windowFlags)

        val drawList = ImGui.getWindowDrawList()

        // Draw main background
        val windowPos = ImGui.getWindowPos()
        drawList.addRectFilled(
            windowPos.x, windowPos.y,
            windowPos.x + boxWidth, windowPos.y + boxHeight * closingProgress,
            backgroundColor
        )

        // Draw header
        drawHeader(drawList, windowPos.toVec2f)

        // Draw search bar
//        drawSearchBar(drawList, Vector2f(windowPos.x + 10f, windowPos.y + 40f))

        // Begin scrollable content area
        ImGui.setNextWindowContentSize(boxWidth - 20f, calculateContentHeight())
        ImGui.setNextWindowPos(windowPos.x + 10f, windowPos.y + 45f)
        ImGui.pushClipRect(
            windowPos.x + 10f,
            windowPos.y + 50f,
            windowPos.x + boxWidth - 10f,
            windowPos.y + boxHeight - 10f,
            true
        )

        ImGui.beginChild(
            "ScrollingRegion",
            boxWidth - 20f,
            boxHeight - 50f,
            false
        )


        val contentStartY = ImGui.getCursorPosY()
        filters.forEachIndexed { index, filter ->
            drawFilterItem(
                drawList,
                filter,
                index,
                Vector2f(boxPosition.x + 10f, contentStartY + (index * 65f))
            )
            ImGui.dummy(0f, 65f)
        }

        ImGui.endChild()
        ImGui.popClipRect()

        // Handle pending filter removal
        filterToRemove?.let {
            filters.removeAt(it)
            filterToRemove = null
        }

        ImGui.end()
        ImGui.popID()

        if (showBlockSelection) {
            BlockSelectionScreen.render(gfx)
        }
    }


    private fun handleScrollInput() {
        if (isMouseOver(boxPosition.x, boxPosition.y, boxWidth, boxHeight)) {
            val wheel = ImGui.getIO().mouseWheel
            if (wheel != 0f) {
                // Adjust scroll speed as needed
                scrollY += wheel * 30f

                // Calculate max scroll
                val contentHeight = calculateContentHeight()
                val maxScroll = (contentHeight - boxHeight + 100f).coerceAtLeast(0f)

                // Clamp scroll value
                scrollY = scrollY.coerceIn(-maxScroll, 0f)
            }
        }
    }

    private fun drawBox(drawList: ImDrawList, height: Float) {
        // Main background
        drawList.addRectFilled(
            boxPosition.x, boxPosition.y,
            boxPosition.x + boxWidth, boxPosition.y + height,
            backgroundColor
        )

        // Border
        drawList.addRect(
            boxPosition.x, boxPosition.y,
            boxPosition.x + boxWidth, boxPosition.y + height,
            ImColor.rgba(60, 60, 60, 255)
        )

        // Search bar at fixed position
        drawSearchBar(drawList, Vector2f(boxPosition.x + 10f, boxPosition.y + 40f))
    }


    private fun drawHeader(drawList: ImDrawList, windowPos: Vector2f) {
        // Header background
        drawList.addRectFilled(
            windowPos.x, windowPos.y,
            windowPos.x + boxWidth, windowPos.y + 30f,
            headerColor
        )

        // Handle dragging
        if (ImGui.isMouseClicked(0) && isMouseOver(windowPos.x, windowPos.y, boxWidth, 30f)) {
            isDragging = true
            val mousePos = ImGui.getMousePos()
            dragOffset.x = mousePos.x - windowPos.x
            dragOffset.y = mousePos.y - windowPos.y
        }

        if (isDragging) {
            if (ImGui.isMouseDown(0)) {
                val mousePos = ImGui.getMousePos()
                boxPosition.x = mousePos.x - dragOffset.x
                boxPosition.y = mousePos.y - dragOffset.y
            } else {
                isDragging = false
            }
        }

        // Title
        headerFont.use {
            drawList.addText(
                headerFont,
                16f,
                windowPos.x + 10f,
                windowPos.y + 7f,
                textColor,
                "Filters"
            )
        }

        // Add button
        drawAddButton(drawList, windowPos)
    }

    private fun drawAddButton(drawList: ImDrawList, windowPos: Vector2f) {
        val addButtonSize = 20f
        val addButtonPos = Vector2f(windowPos.x + boxWidth - addButtonSize - 5f, windowPos.y + 5f)

        drawList.addRectFilled(
            addButtonPos.x, addButtonPos.y,
            addButtonPos.x + addButtonSize, addButtonPos.y + addButtonSize,
            accentColor
        )

        iconFont.use {
            drawList.addText(
                iconFont,
                16f,
                addButtonPos.x + 3f,
                addButtonPos.y + 2f,
                textColor,
                FontAwesome.Plus
            )
        }

        if (isMouseOver(addButtonPos.x, addButtonPos.y, addButtonSize, addButtonSize) && ImGui.isMouseClicked(0)) {
            showBlockSelection = true
            BlockSelectionScreen.show { block ->
                val filter = Filter.ItemFilter(
                    target = block,
                    matchNbt = false,
                    isBlacklist = false
                )
                filters.add(filter)
                showBlockSelection = false
            }
        }
    }

    private fun drawContent(drawList: ImDrawList) {
        // Set up content area dimensions
        val contentStartY = boxPosition.y + 70f  // After header and search
        val contentHeight = boxHeight - 70f

        // Set up clip rect for scrollable area
        ImGui.beginChild(
            "ScrollArea",
            boxWidth - 20f,  // Leave space for scrollbar
            contentHeight,
            false,  // No border
            ImGuiWindowFlags.NoScrollbar or ImGuiWindowFlags.NoScrollWithMouse
        )

        var currentY = scrollY

        // Filter list with spacing adjustment for the increased item height
        filters.forEachIndexed { index, filter ->
            drawFilterItem(drawList, filter, index, Vector2f(boxPosition.x + 10f, contentStartY + currentY))
            currentY += 65f
        }

        ImGui.endChild()

        // Draw scrollbar if needed
        val totalContentHeight = calculateContentHeight()
        if (totalContentHeight > contentHeight) {
            val scrollbarWidth = 8f
            val scrollbarHeight = (contentHeight / totalContentHeight) * contentHeight
            val scrollRatio = -scrollY / (totalContentHeight - contentHeight)
            val scrollbarY = contentStartY + (contentHeight - scrollbarHeight) * scrollRatio

            drawList.addRectFilled(
                boxPosition.x + boxWidth - scrollbarWidth - 5f,
                scrollbarY,
                boxPosition.x + boxWidth - 5f,
                scrollbarY + scrollbarHeight,
                ImColor.rgba(80, 80, 80, 180)
            )
        }
    }

    private fun calculateContentHeight(): Float {
        val filterItemHeight = 65f
        val padding = 20f
        return (filters.size * filterItemHeight) + padding
    }

    private fun drawSearchBar(drawList: ImDrawList, position: Vector2f) {
        drawList.addRectFilled(
            position.x, position.y,
            position.x + boxWidth - 20f, position.y + 30f,
            inputBgColor,
            5f
        )

        iconFont.use {
            drawList.addText(
                iconFont,
                14f,
                position.x + 8f,
                position.y + 8f,
                textColor,
                FontAwesome.MagnifyingGlass
            )
        }

        ImGui.setCursorPos(position.x + 30f, position.y + 5f)
        ImGui.pushItemWidth(boxWidth - 60f)
        if (ImGui.inputText("##search", searchQuery)) {
            // Handle search
        }
        ImGui.popItemWidth()
    }

    private fun drawSwitch(
        drawList: ImDrawList,
        position: Vector2f,
        label: String,
        state: ImBoolean,
        width: Float,
        height: Float,
        labelWidth: Float
    ) {
        // Label
        bodyFont.use {
            drawList.addText(
                bodyFont,
                14f,
                position.x,
                position.y + 4f,
                textColor,
                label
            )
        }

        // Switch background
        val switchX = position.x + labelWidth
        val switchBgColor = if (state.get()) accentColor else inputBgColor
        drawList.addRectFilled(
            switchX, position.y,
            switchX + width, position.y + height,
            switchBgColor,
            height / 2
        )

        // Switch knob
        val knobSize = height - 4f
        val knobX = if (state.get()) switchX + width - knobSize - 2f else switchX + 2f
        drawList.addCircleFilled(
            knobX + knobSize / 2, position.y + height / 2,
            knobSize / 2,
            ImColor.rgba(255, 255, 255, 255)
        )

        if (isMouseOver(switchX, position.y, width, height) && ImGui.isMouseClicked(0)) {
            state.set(!state.get())
        }
    }

    private fun drawFilterItem(drawList: ImDrawList, filter: Filter, index: Int, position: Vector2f) {
//        ImGui.setCursorScreenPos(boxPosition.x, position.y)
        val pos = ImGui.getCursorScreenPos()
        val itemHeight = 65f
        val switchWidth = 50f
        val switchHeight = 24f
        val switchSpacing = 10f
        val labelWidth = 70f

        // Background gradient
        drawList.addRectFilledMultiColor(
            pos.x,
            pos.y,
            pos.x + boxWidth - 35f,
            pos.y + itemHeight - 4f,
            gradientTopColor.toLong(),
            gradientTopColor.toLong(),
            gradientBottomColor.toLong(),
            gradientBottomColor.toLong()
        )

        when (filter) {
            is Filter.ItemFilter -> {
                // Record the block rendering for post-render
                recordedDrawCalls.add { gfx ->
                    gfx.renderBlockItem(
                        filter.target,
                        pos.x + 5f,
                        pos.y + 4f,
                        32f
                    )
                }

                bodyFont.use {
                    drawList.addText(
                        bodyFont,
                        14f,
                        pos.x + 45f,
                        pos.y + 12f,
                        textColor,
                        filter.name
                    )
                }
            }

            is Filter.TagFilter -> {
                iconFont.use {
                    drawList.addText(
                        iconFont,
                        14f,
                        pos.x + 5f,
                        pos.y + 4f,
                        textColor,
                        FontAwesome.Tag
                    )
                }

                bodyFont.use {
                    drawList.addText(
                        bodyFont,
                        14f,
                        pos.x + 45f,
                        pos.y + 12f,
                        textColor,
                        filter.name
                    )
                }
            }
        }

        // Filter settings
        val settingsY = pos.y + 35f
        val matchNbtState = ImBoolean(filter.matchNbt)
        val blacklistState = ImBoolean(filter.isBlacklist)

        // Draw NBT Switch
        drawSwitch(
            drawList,
            Vector2f(pos.x + 10f, settingsY),
            "Match NBT",
            matchNbtState,
            switchWidth,
            switchHeight,
            labelWidth
        )

        // Draw Blacklist Switch
        drawSwitch(
            drawList,
            Vector2f(pos.x + labelWidth + switchWidth + switchSpacing, settingsY),
            "Blacklist",
            blacklistState,
            switchWidth,
            switchHeight,
            labelWidth
        )

        // Update filter if settings change
        if (matchNbtState.get() != filter.matchNbt || blacklistState.get() != filter.isBlacklist) {
            val updatedFilter = when (filter) {
                is Filter.ItemFilter -> filter.copy(
                    matchNbt = matchNbtState.get(),
                    isBlacklist = blacklistState.get()
                )

                is Filter.TagFilter -> filter.copy(
                    matchNbt = matchNbtState.get(),
                    isBlacklist = blacklistState.get()
                )
            }
            filters[index] = updatedFilter
            onFilterCreated?.invoke(updatedFilter)
        }

        // Delete button
        val deleteButtonSize = 24f
        val deleteButtonX = pos.x + boxWidth - deleteButtonSize - 30f
        if (isMouseOver(deleteButtonX, pos.y + 8f, deleteButtonSize, deleteButtonSize)) {
            if (ImGui.isMouseClicked(0)) {
                filterToRemove = index
            }
            drawList.addText(
                iconFont,
                14f,
                deleteButtonX + 5f,
                pos.y + 12f,
                ImColor.rgba(255, 100, 100, 255),
                FontAwesome.Trash
            )
        } else {
            drawList.addText(
                iconFont,
                14f,
                deleteButtonX + 5f,
                pos.y + 12f,
                textColor,
                FontAwesome.Trash
            )
        }
    }

    fun renderPost(gfx: GuiGraphics, graphics: CanvasGraphics) {
        if (!isOpen.get()) return

        val transformedPos = graphics.toScreenSpaceVector(boxPosition.x, boxPosition.y + 50f)
        val transformedSize = graphics.toScreenSpaceVector(boxWidth, boxHeight - 60f)

        gfx.enableScissor(
            transformedPos.x.toInt(),
            transformedPos.y.toInt(),
            (transformedPos.x + transformedSize.x).toInt(),
            (transformedPos.y + transformedSize.y).toInt()
        )

        recordedDrawCalls.forEach { it(graphics) }

        gfx.disableScissor()

        if (showBlockSelection) {
            BlockSelectionScreen.renderPost(gfx, graphics)
        }
    }

    private fun updateAnimation() {
        val deltaTime = ImGui.getIO().deltaTime
        if (isClosing) {
            closingProgress = (closingProgress - deltaTime * 5f).coerceAtLeast(0f)
            if (closingProgress == 0f) {
                isOpen.set(false)
            }
        } else {
            closingProgress = (closingProgress + deltaTime * 5f).coerceAtMost(1f)
        }
    }

    private fun isMouseOver(x: Float, y: Float, width: Float, height: Float): Boolean {
        val mousePos = ImGui.getMousePos()
        return mousePos.x >= x && mousePos.x <= x + width &&
                mousePos.y >= y && mousePos.y <= y + height
    }

}