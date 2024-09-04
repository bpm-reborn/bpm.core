package bpm.client.runtime.windows

import imgui.ImColor
import imgui.ImGui
import imgui.ImVec2
import imgui.flag.ImDrawFlags
import imgui.flag.ImGuiMouseCursor
import imgui.type.ImString
import bpm.client.font.FontType
import bpm.client.font.Fonts
import bpm.client.render.IRender
import bpm.client.runtime.ClientRuntime
import bpm.common.utils.FontAwesome
import bpm.client.utils.sizeOf
import bpm.common.packets.internal.Time
import bpm.common.workspace.packets.WorkspaceLibrary
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

class ProjectListWindow(private val runtime: ClientRuntime, var library: WorkspaceLibrary) : IRender {

    private val baseWidth = 1280f // Base width for scaling
    private val baseHeight = 920f // Base height for scaling
    private var scale: Float = 1f // Default scale factor
    private val now = Time.now // Placeholder for current time
    private val headerFont = Fonts.getFamily("Inter")[FontType.ExtraBold]
    private val bodyFont = Fonts.getFamily("Inter")[FontType.Regular]
    private val bodyBoldFont = Fonts.getFamily("Inter")[FontType.Bold]
    private val iconFont = Fonts.getFamily("Fa")[FontType.Regular]
    private var start: ImVec2 = ImVec2(0f, 0f)
    override fun render() {
        scale = (ImGui.getWindowWidth() / baseWidth).coerceAtMost(1f)
        val windowWidth = ImGui.getWindowWidth()
        val windowPos = ImGui.getWindowPos()
        val containerWidth = windowWidth - 40 * scale
        val containerX = (windowWidth - containerWidth) / 2f
        val containerY = 10f
        start = ImVec2(windowPos.x + containerX, windowPos.y + containerY + ImGui.getWindowContentRegionMinY())

        renderCreateWorkspaceButton(containerWidth)

        for ((uuid, values) in library.workspaces) {
            val x = start.x
            val y = start.y
            renderWorkspaceSelection(uuid, values.first, values.second, "doozyz", containerWidth)
            //Move imGui cursor to the next row
            start = ImVec2(start.x, start.y)
            ImGui.dummy(containerWidth, start.y - y)
        }
    }

    private fun renderWorkspaceSelection(
        uuid: UUID,
        name: String,
        description: String,
        createdBy: String,
        containerWidth: Float = 1200f
    ) {

        val titleBoxHeight = 40 * scale

//        renderBackground(start, containerWidth, backgroundHeight)

        val title = computeTitle(name, containerWidth)
        val end = renderTitle(start, title)
        val size = renderTabs(end, containerWidth, uuid)
        //render description
        var nextY = start.y + titleBoxHeight - 5 * scale
        renderDescription(start, description, nextY, containerWidth, size)


    }

    private fun computeTitle(name: String, containerWidth: Float): String {
        val maxTitleWidth = containerWidth - 120f * scale
        var currentWidth = 0f
        var maxTitleLength = 0
        val headerFontSize = 22f * scale
        val headerFont = this.headerFont[headerFontSize.roundToInt()]
        for (char in name) {
            val charWidth = headerFont.sizeOf(char.toString()).x
            if (currentWidth + charWidth > maxTitleWidth) break
            currentWidth += charWidth
            maxTitleLength++
        }
        return if (name.length > maxTitleLength) name.substring(0, maxTitleLength) + "..." else name
    }

    private fun renderTitle(start: ImVec2, title: String): ImVec2 {
        val headerFontSize = 22f * scale
        val headerFont = this.headerFont[headerFontSize.roundToInt()]
        val drawList = ImGui.getWindowDrawList()
        val textX = start.x + 20 * scale
        // draws a rounded box surrounding the title
        val end = ImVec2(
            textX + headerFont.sizeOf(title, headerFontSize, maxWidth = 1000f).x + 5 * scale,
            start.y + headerFontSize + 15 * scale
        )
        drawList.addRectFilled(
            start.x + 14 * scale,
            start.y + 8 * scale,
            end.x,
            end.y,
            ImColor.rgba(13, 11, 11, 255),
            10 * scale,
            ImDrawFlags.RoundCornersTop
        )
        drawList.addRect(
            start.x + 15 * scale,
            start.y + 8 * scale,
            end.x - 1 * scale,
            end.y - 3 * scale,
            ImColor.rgba(225, 225, 225, 255),
            10 * scale,
            ImDrawFlags.RoundCornersTop,
            1 * scale
        )
        drawList.addText(headerFont, headerFontSize, textX, start.y + 10 * scale, 0xFFFFFFFF.toInt(), title)
        return end
    }

    private fun renderDescription(
        start: ImVec2,
        description: String,
        nextY: Float,
        containerWidth: Float,
        size: ImVec2
    ) {
        val bodyFontSize = (14f * scale).roundToInt()
        val bodyFont = this.bodyFont[bodyFontSize]
        val maxWrap = start.x + containerWidth - 10 * scale
        val drawList = ImGui.getWindowDrawList()
        // Draw the box surrounding the description
        //the wrapped width
        val wrapped = bodyFont.sizeOf(
            description,
            bodyFontSize.toFloat(),
            maxWidth = containerWidth,
            wrapWidth = containerWidth - 30 * scale
        )

        val wrappedWidth = max(size.x - start.x, wrapped.x + 30 * scale)

        drawList.addRectFilled(
            start.x + 14 * scale,
            nextY,
            start.x + wrappedWidth,
            nextY + wrapped.y + 10 * scale,
            ImColor.rgba(13, 11, 11, 255),
            10 * scale,
            ImDrawFlags.RoundCornersBottom
        )
        ImGui.pushTextWrapPos(maxWrap)
        drawList.addText(
            bodyFont,
            bodyFontSize.toFloat(),
            start.x + 20 * scale,
            nextY + 5 * scale,
            0xFFFFFFFF.toInt(),
            description,
            wrapped.x
        )
        //render line seperator
        drawList.addLine(
            start.x + 15 * scale,
            nextY - 1 * scale,

            start.x + wrappedWidth - 1 * scale,
            nextY - 1 * scale,
            ImColor.rgba(225, 219, 223, 255),
            1 * scale
        )
        ImGui.setCursorScreenPos(start.x + 20 * scale, nextY - 25 * scale)
        start.set(start.x, nextY + wrapped.y + 10 * scale)
        ImGui.popTextWrapPos()

    }

    private fun renderTabs(
        titleEnd: ImVec2,
        containerWidth: Float,
        workspace: UUID
    ): ImVec2 {

        var end = drawTab(titleEnd, containerWidth, 30 * scale, "settings", FontAwesome.Gear)
        end = drawTab(end, containerWidth, 30 * scale, "delete", FontAwesome.TrashCan)
        end = drawTab(end, containerWidth, 30 * scale, "open", FontAwesome.DoorOpen) {
            runtime.setWorkspace(workspace)
        }
        return ImVec2(end.x, end.y)
    }

    private fun drawTab(
        start: ImVec2,
        containerWidth: Float,
        backgroundHeight: Float,
        text: String,
        icon: String,
        onClick: () -> Unit = {}
    ): ImVec2 {
        val bodyFontSize = (14f * scale).roundToInt()
        val bodyFont = this.bodyFont[bodyFontSize]
        val drawList = ImGui.getWindowDrawList()
        val padding = 5 * scale
        val backgroundWidth = 100 * scale
        val startX = start.x + 20 * scale
        val textY = start.y - 25 * scale
        val textWidth = bodyFont.sizeOf(text, bodyFontSize.toFloat()).x
        val mousePos = ImGui.getMousePos()
        if (mousePos.x > startX && mousePos.x < startX + backgroundWidth && mousePos.y > textY - padding && mousePos.y < textY + backgroundHeight) {
            drawList.addRectFilled(
                startX - padding - 2.5f * scale,
                textY - padding - 2.5f * scale,
                startX + backgroundWidth + 2.5f * scale,
                textY + backgroundHeight,
                ImColor.rgba(200, 205, 198, 255),
                10f * scale,
                ImDrawFlags.RoundCornersTop
            )
            ImGui.setMouseCursor(ImGuiMouseCursor.Hand)
            if (ImGui.isMouseClicked(0)) {
                onClick()
            }
        }
        drawList.addRectFilled(
            startX - padding,
            textY - padding,
            startX + backgroundWidth,
            textY + backgroundHeight,
            ImColor.rgba(13, 11, 11, 255),
            10f * scale,
            ImDrawFlags.RoundCornersTop
        )
        val iconSize = (24 * scale).roundToInt()
        val icons = iconFont[iconSize]
        drawList.addText(
            icons,
            iconSize.toFloat(),
            startX + 5 * scale,
            textY - 8 * scale,
            ImColor.rgba(255, 255, 255, 255),
            icon
        )
        drawList.addText(
            bodyFont,
            bodyFontSize.toFloat(),
            startX + backgroundWidth / 2 - textWidth / 2,
            textY,
            ImColor.rgba(255, 255, 255, 255),
            text
        )


        //return the end of the tab
        return ImVec2(startX + backgroundWidth, start.y)
    }

    private fun renderCreateWorkspaceButton(containerWidth: Float) {
        val buttonWidth = 200f * scale
        val buttonHeight = 40f * scale
        val buttonX = start.x + (containerWidth - buttonWidth) / 2
        val buttonY = start.y

        ImGui.setCursorScreenPos(buttonX, buttonY)
        if (ImGui.button("Create New Workspace", buttonWidth, buttonHeight)) {
            ImGui.openPopup("Create Workspace")
        }

        if (ImGui.beginPopup("Create Workspace")) {
            val name = ImString(100)
            val description = ImString(255)

            ImGui.inputText("Name", name)
            ImGui.inputTextMultiline("Description", description, 200f, 100f)

            if (ImGui.button("Create", 120f, 0f)) {
                runtime.createWorkspace(name.get(), description.get())
                ImGui.closeCurrentPopup()
            }
            ImGui.sameLine()
            if (ImGui.button("Cancel", 120f, 0f)) {
                ImGui.closeCurrentPopup()
            }

            ImGui.endPopup()
        }

        start.y += buttonHeight + 20f * scale
    }


}
