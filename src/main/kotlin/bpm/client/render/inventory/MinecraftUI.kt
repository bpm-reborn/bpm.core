package bpm.client.render.inventory

import bpm.client.font.Fonts
import bpm.client.runtime.ClientRuntime
import bpm.client.utils.use
import imgui.ImColor
import imgui.ImDrawList
import imgui.ImGui
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiInputTextFlags
import imgui.flag.ImGuiStyleVar
import imgui.type.ImString
import net.minecraft.client.Minecraft

object MinecraftUI {

    // Minecraft button colors
    private val buttonNormalTop = ImColor.rgba(198, 198, 198, 255)    // Light gray
    private val buttonNormalBottom = ImColor.rgba(158, 158, 158, 255) // Darker gray
    private val buttonHoveredTop = ImColor.rgba(218, 218, 218, 255)   // Lighter gray when hovered
    private val buttonHoveredBottom = ImColor.rgba(178, 178, 178, 255)
    private val buttonActiveTop = ImColor.rgba(138, 138, 138, 255)    // Darkest when pressed
    private val buttonActiveBottom = ImColor.rgba(158, 158, 158, 255)
    private val buttonBorder = ImColor.rgba(80, 80, 80, 255)         // Dark border
    private val buttonShadow = ImColor.rgba(35, 35, 35, 180)         // Button shadow
    private val textColor = ImColor.rgba(255, 255, 255, 255)            // Default text color
    private val textDisabledColor = ImColor.rgba(128, 128, 128, 255) // Disabled text color
    private val guiScale get() = Minecraft.getInstance().options.guiScale().get().toFloat()
    private val guiFontSize get() = (Minecraft.getInstance().options.guiScale().get().toFloat() * 10f).toInt()
    private val bodyFont get() = Fonts.getFamily("Minecraft")["Regular"][guiFontSize]
    private val iconFont get() = Fonts.getFamily("Fa")["Regular"][guiFontSize]

    fun iconButton(icon: String, width: Float, height: Float, enabled: Boolean = true): Boolean {
        pushButtonStyles()
        val drawList = ImGui.getWindowDrawList()
        val pos = ImGui.getCursorScreenPos()

        // Invisible button for interaction (must come before drawing to set up hover state)
        ImGui.invisibleButton(icon, width, height)

        val clicked = renderButtonBackground(drawList, pos.x, pos.y, width, height, enabled)

        val textX = ((pos.x + (width) / 2)) - 1f * guiScale
        val textY = (pos.y) - 2f * guiScale

        // Draw text with shadow
        if (enabled) {
            drawList.addText(
                iconFont,
                guiFontSize.toFloat(),
                textX + 1f * (guiScale - 2f),
                textY + 1f * (guiScale - 2f),
                ImColor.rgba(0, 0, 0, 100),
                icon
            )
            drawList.addText(iconFont, guiFontSize.toFloat(), textX, textY, textColor, icon)
        } else {
            drawList.addText(
                iconFont,
                guiFontSize.toFloat(),
                textX+ 1f * (guiScale - 2f),
                textY+ 1f * (guiScale - 2f),
                ImColor.rgba(0, 0, 0, 40),
                icon
            )
            drawList.addText(iconFont, guiFontSize.toFloat(), textX, textY, textDisabledColor, icon)
        }
        popButtonStyles()
        return clicked && enabled
    }

    /**
     * Renders a Minecraft-style button and handles its interaction
     *
     * @param label The text to display on the button
     * @param width The button width (scaled by GUI scale)
     * @param height The button height (scaled by GUI scale)
     * @param enabled Whether the button is interactive
     * @return true if the button was clicked, false otherwise
     */
    fun button(label: String, width: Float, height: Float, enabled: Boolean = true): Boolean {
        pushButtonStyles()
        val drawList = ImGui.getWindowDrawList()
        val pos = ImGui.getCursorScreenPos()

        // Invisible button for interaction (must come before drawing to set up hover state)
        ImGui.invisibleButton(label, width, height)

        val clicked = renderButtonBackground(drawList, pos.x, pos.y, width, height, enabled)

        // Center text
        val textSize = bodyFont.use {
            ImGui.calcTextSize(label)
        }

        val textX = pos.x + (width - textSize.x) / 2
        val textY = pos.y + (height - textSize.y) / 2

        // Draw text with shadow
        if (enabled) {
            drawList.addText(
                bodyFont,
                guiFontSize.toFloat(),
                textX + 1f,
                textY + 1f,
                ImColor.rgba(0, 0, 0, 100),
                label
            )
            drawList.addText(bodyFont, guiFontSize.toFloat(), textX, textY, textColor, label)
        } else {
            drawList.addText(
                bodyFont,
                guiFontSize.toFloat(),
                textX + 1f,
                textY + 1f,
                ImColor.rgba(0, 0, 0, 40),
                label
            )
            drawList.addText(bodyFont, guiFontSize.toFloat(), textX, textY, textDisabledColor, label)
        }
        popButtonStyles()
        return clicked && enabled
    }

    fun sectionTitle(text: String) {
        val drawList = ImGui.getWindowDrawList()
        val pos = ImGui.getCursorScreenPos()

        bodyFont.use {
            //Draw shadowed by offsetting the text by 1 in both x and y
            drawList.addText(
                bodyFont,
                guiFontSize.toFloat(),
                pos.x + 2f * (guiScale / 2),
                pos.y + 2f * (guiScale /2),
                ImColor.rgba(0, 0, 0, 120),
                text
            )

            drawList.addText(
                bodyFont,
                guiFontSize.toFloat(),
                pos.x,
                pos.y,
                textColor,
                text
            )

        }

        ImGui.dummy(0f, guiFontSize.toFloat() + 4f * (guiScale * 2f))
    }

    /**
     * Renders just the button background without text or interaction
     */
    private fun renderButtonBackground(
        drawList: ImDrawList,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        enabled: Boolean
    ): Boolean {
        val hovered = enabled && ImGui.isItemHovered()
        val active = enabled && ImGui.isItemActive()

        // Select colors based on state
        val (topColor, bottomColor) = when {
            !enabled -> buttonActiveTop to buttonActiveBottom
            active -> buttonActiveTop to buttonActiveBottom
            hovered -> buttonHoveredTop to buttonHoveredBottom
            else -> buttonNormalTop to buttonNormalBottom
        }

        // Button shadow (only when not pressed)
        if (!active && enabled) {
            drawList.addRectFilled(
                x, y + height,
                x + width, y + height + 2f,
                buttonShadow
            )
        }

        // Main button gradient
        drawList.addRectFilledMultiColor(
            x, y,
            x + width, y + height,
            topColor.toLong(),
            topColor.toLong(),
            bottomColor.toLong(),
            bottomColor.toLong()
        )

        // Button border
        drawList.addRect(
            x, y,
            x + width, y + height,
            buttonBorder,
            0f,
            0,
            1f
        )

        // Highlight when hovered (top and left edges)
        if (hovered && !active) {
            drawList.addLine(
                x + 1f, y + 1f,
                x + width - 1f, y + 1f,
                ImColor.rgba(255, 255, 255, 100),
                1f
            )
            drawList.addLine(
                x + 1f, y + 1f,
                x + 1f, y + height - 1f,
                ImColor.rgba(255, 255, 255, 100),
                1f
            )
        }

        return ImGui.isItemClicked()
    }

    /**
     * Renders a Minecraft-style text input field
     *
     * @param label The unique identifier for the input
     * @param value The ImString buffer containing the input value
     * @param width The input width (scaled by GUI scale)
     * @param height The input height (scaled by GUI scale)
     * @param placeholder Optional placeholder text shown when input is empty
     * @param enabled Whether the input is interactive
     * @return true if the input value was modified, false otherwise
     */
    fun input(
        label: String,
        value: ImString,
        width: Float,
        height: Float,
        placeholder: String = "",
        enabled: Boolean = true
    ): Boolean {
        val drawList = ImGui.getWindowDrawList()
        val pos = ImGui.getCursorScreenPos()

        // Draw input background
        drawList.addRectFilled(
            pos.x,
            pos.y,
            pos.x + width,
            pos.y + height,
            buttonActiveTop,  // Using pressed button color for input background
            0f
        )

        // Add border
        drawList.addRect(
            pos.x,
            pos.y,
            pos.x + width,
            pos.y + height,
            buttonBorder,
            0f,
            0,
            1f
        )

        val padding = (guiScale * 2f)
        var modified = false

        bodyFont.use {
            // Setup input styling
            ImGui.pushStyleColor(ImGuiCol.Text, if (enabled) textColor else textDisabledColor)
            ImGui.pushStyleColor(ImGuiCol.FrameBg, 0) // Transparent background
            ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, padding, 0f)
            ImGui.getIO().configInputTextCursorBlink = false
            // Hide the default ImGui cursor
            ImGui.pushStyleColor(ImGuiCol.Text, 0)

            // Input field
            ImGui.pushItemWidth(width - padding * 2)
            if (enabled) {
                modified = ImGui.inputText(
                    "##$label",
                    value,
                    ImGuiInputTextFlags.Password or          // Hides default cursor
                            ImGuiInputTextFlags.NoHorizontalScroll   // Prevents scrolling
                )
            } else {
                ImGui.inputText("##$label", value)
            }
            ImGui.popItemWidth()

            // Render placeholder if needed - only when not focused and empty
            if (value.get().isEmpty() && placeholder.isNotEmpty() && !ImGui.isItemActive()) {
                val textSize = ImGui.calcTextSize(placeholder)
                drawList.addText(
                    bodyFont,
                    guiFontSize.toFloat(),
                    pos.x + padding,
                    pos.y + (height - textSize.y) / 2,
                    ImColor.rgba(90, 90, 90, 255),
                    placeholder
                )
            }
            //Render the actual text, since we're hiding it so the cursor doesn't render
            val textSize = ImGui.calcTextSize(value.get())
            drawList.addText(
                bodyFont,
                guiFontSize.toFloat(),
                pos.x + padding,
                pos.y + (height - textSize.y) / 2,
                textColor,
                value.get()
            )

            // Custom Minecraft-style cursor rendering
            if (ImGui.isItemActive() && enabled) {
                val text = value.get()
                val textSize = ImGui.calcTextSize(text)

                // Calculate cursor position based on text width
                val cursorX = pos.x + padding + textSize.x
                val cursorY = pos.y + (height - guiFontSize) / 2

                // Blink the cursor every 0.5 seconds (using ImGui.getTime())
                if ((ImGui.getTime() * 2).toInt() % 2 == 0) {
                    // Draw the underscore cursor
                    drawList.addText(
                        bodyFont,
                        guiFontSize.toFloat(),
                        cursorX,
                        cursorY,
                        textColor,
                        "_"
                    )
                }
            }

            ImGui.popStyleColor(3) // Pop Text, FrameBg, and TextSelectedBg
            ImGui.popStyleVar()
        }

        // Maintain expected cursor position
        ImGui.dummy(width, height)

        ImGui.getIO().configInputTextCursorBlink = true
        return modified
    }

    /**
     * Pushes ImGui styles for a group of Minecraft-style buttons
     */
    private fun pushButtonStyles() {
        ImGui.pushStyleColor(ImGuiCol.Button, buttonNormalTop)
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, buttonHoveredTop)
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, buttonActiveTop)
    }

    /**
     * Pops ImGui styles for Minecraft-style buttons
     */
    private fun popButtonStyles() {
        ImGui.popStyleColor(3)
    }
}