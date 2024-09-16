package bpm.client.render.panel

import bpm.client.utils.toVec2f
import bpm.client.utils.use
import bpm.common.utils.FontAwesome
import imgui.ImColor
import imgui.ImDrawList
import imgui.ImGui
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiMouseButton
import imgui.flag.ImGuiMouseCursor
import imgui.type.ImString
import org.joml.Vector2f
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

object ConsolePanel : Panel("Console", FontAwesome.Terminal) {


    private val logQueue = ConcurrentLinkedQueue<LogEntry>()
    private val dateFormat = SimpleDateFormat("HH:mm:ss")

    fun log(message: String, level: LogLevel = LogLevel.INFO, calledFrom: String = "") {
        val timestamp = dateFormat.format(Date())
        logQueue.offer(LogEntry(timestamp, level, message, calledFrom))
    }

    private val searchBuffer = ImString(256)

    enum class LogLevel(val color: Int) {
        DEBUG(ImColor.rgba(150, 150, 150, 255)),
        INFO(ImColor.rgba(255, 255, 255, 255)),
        WARNING(ImColor.rgba(255, 255, 0, 255)),
        ERROR(ImColor.rgba(255, 0, 0, 255))
    }

    data class LogEntry(val timestamp: String, val level: LogLevel, val message: String, val calledFrom: String)

    override fun renderBody(drawList: ImDrawList, position: Vector2f, size: Vector2f) {
//        renderSearchBar(drawList, position, size)
        renderLogs(drawList, position, size)
    }

    override fun renderFooterContent(drawList: ImDrawList, position: Vector2f, size: Vector2f) {
        // Clear button


        // Log level buttons
//        LogLevel.entries.forEachIndexed { index, logLevel ->
//            val buttonWidth = 80f
//            val buttonHeight = 20f
//            val buttonX = size.x - 100f + (index * (buttonWidth + 5f))
//            val buttonY = 5f
//            if (ImGui.button(logLevel.name, buttonWidth, buttonHeight)) {
//                log("Test ${logLevel.name}", logLevel)
//            }
//        }
        renderSearchBar(drawList, position, size)
        // Add variable button
        renderClearLogsButton(
            drawList,
            Vector2f(position.x + size.x - 35f, position.y + 10f),
            Vector2f(30f, 30f)
        )
    }

    private fun renderSearchBar(drawList: ImDrawList, position: Vector2f, size: Vector2f) {
        val searchBarHeight = 30f
        val searchBarY = position.y + 10f

        // Search bar background
        drawList.addRectFilled(
            position.x + 5f,
            searchBarY,
            position.x + size.x - 5f,
            searchBarY + searchBarHeight,
            ImColor.rgba(60, 60, 60, 255),
            5f
        )

        // Search icon
        iconFam[20].use {
            drawList.addText(
                it,
                20f,
                position.x + 15f,
                searchBarY + 5f,
                ImColor.rgba(150, 150, 150, 255),
                FontAwesome.AlignCenter
            )
        }

        // Search input
        bodyFam[18].use {
            ImGui.setCursorScreenPos(position.x + 25f, position.y + 13f)
            ImGui.pushItemWidth(size.x - 30f)
            ImGui.pushStyleColor(ImGuiCol.FrameBg, ImColor.rgba(60, 60, 60, 0).toInt())
            ImGui.pushStyleColor(ImGuiCol.Text, ImColor.rgba(200, 200, 200, 255).toInt())
            ImGui.pushStyleColor(ImGuiCol.Border, ImColor.rgba(60, 60, 60, 255).toInt())
            ImGui.inputText("##search", searchBuffer)
            ImGui.popStyleColor(3)
            ImGui.popItemWidth()
        }
    }

    private fun renderClearLogsButton(drawList: ImDrawList, position: Vector2f, size: Vector2f) {
        val isHovered = isMouseOver(position, size.x, size.y)
        val buttonColor = if (isHovered) buttonHoverColor else buttonColor

        drawList.addRectFilled(
            position.x,
            position.y,
            position.x + size.x,
            position.y + size.y,
            buttonColor,
            5f
        )

        iconFam.header.use {
            drawList.addText(
                iconFam.header,
                30f,
                position.x + size.x / 2 - 7f,
                position.y + size.y / 2 - 17f,
                ImColor.rgba(255, 255, 255, 255),
                FontAwesome.TrashCan
            )
        }

        if (isHovered) {
            ImGui.setMouseCursor(ImGuiMouseCursor.Hand)
            graphics.renderTooltip("Clear Logs")
        }

        if (ImGui.isMouseClicked(ImGuiMouseButton.Left) && isHovered) {
            logQueue.clear()
        }

    }


    private fun renderLogs(drawList: ImDrawList, position: Vector2f, size: Vector2f) {
        val startCursorPos = ImGui.getCursorScreenPos().toVec2f
        val logAreaY = startCursorPos.y + 5f
        val logAreaHeight = size.y - 10f
//
//        ImGui.pushStyleColor(ImGuiCol.ChildBg, ImColor.rgba(30, 30, 30, 255).toInt())
//        ImGui.setNextWindowPos(startCursorPos.x + 5f, logAreaY)
//        ImGui.setNextWindowSize(size.x - 10f, logAreaHeight)
//        if (ImGui.beginChild("ConsoleLogArea", size.x - 10f, logAreaHeight, true)) {
        val filteredLogs = logQueue.filter { it.message.contains(searchBuffer.get(), ignoreCase = true) }
        filteredLogs.forEach { logEntry ->
            renderLogEntry(drawList, logEntry)
        }

        if (ImGui.getScrollY() >= ImGui.getScrollMaxY()) {
            ImGui.setScrollHereY(1.0f)
        }
//        }
//        ImGui.endChild()
//        ImGui.popStyleColor()
    }

    private fun renderLogEntry(drawList: ImDrawList, logEntry: LogEntry) {
        val startPos = ImGui.getCursorScreenPos()
        val fontSize = 24
        var x = startPos.x + 10f
        // Timestamp
        boldFam[fontSize].use {
            val textLen = ImGui.calcTextSize(logEntry.timestamp).x
            drawList.addRectFilled(
                x - 5f,
                startPos.y + 5f,
                x + textLen + 5f,
                startPos.y + 5f + fontSize,
                ImColor.rgba(50, 50, 50, 255),
            )
            drawList.addText(
                it,
                fontSize.toFloat(),
                x,
                startPos.y,
                ImColor.rgba(100, 100, 100, 255),
                logEntry.timestamp
            )

            x += 100f
        }

        // Log level
        boldFam[fontSize].use {
            val textLen = ImGui.calcTextSize("[${logEntry.level}]").x
            drawList.addRectFilled(
                x - 5f,
                startPos.y + 5f,
                x + textLen + 5f,
                startPos.y + 5f + fontSize,
                logEntry.level.color,
            )
            drawList.addText(
                it,
                fontSize.toFloat(),
                x,
                startPos.y,
                ImColor.rgba(100, 100, 100, 255),
                "[${logEntry.level}]"
            )
            x += textLen + 10f
        }

        // Called from
        if (logEntry.calledFrom.isNotEmpty()) {
            bodyFam[fontSize].use {
                drawList.addText(
                    it,
                    fontSize.toFloat(),
                    x,
                    startPos.y,
                    ImColor.rgba(100, 100, 255, 255),
                    logEntry.calledFrom
                )
            }
            val textLen = ImGui.calcTextSize(logEntry.calledFrom).x
            x += textLen + 10f
        }

        // Message
        bodyFam[fontSize].use {
            drawList.addText(
                it,
                fontSize.toFloat(),
                x,
                startPos.y,
                ImColor.rgba(200, 200, 200, 255),
                logEntry.message
            )
        }

        ImGui.dummy(0f, 20f)
    }

    override fun onResize() {
        // Handle any resize logic if needed
    }

    init {
        // Initialize any necessary components
    }
}