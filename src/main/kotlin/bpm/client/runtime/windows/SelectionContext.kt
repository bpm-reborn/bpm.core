package bpm.client.runtime.windows

import imgui.*
import imgui.flag.*
import imgui.type.ImFloat
import imgui.type.ImInt
import imgui.type.ImString
import bpm.client.font.Fonts
import bpm.common.network.Client
import bpm.common.workspace.Workspace
import bpm.common.workspace.graph.Node
import bpm.common.property.*
import bpm.common.workspace.packets.NodeDeleteRequest
import org.joml.*
import java.util.*

class SelectionContextOverlay(private val workspace: Workspace) {

    private var isOpen = false
    private var animationProgress = 0f
    private val animationDuration = 0.3f
    private val panelWidth = 300f
    private val headerHeight = 50f

    private val headerFont get() = Fonts.getFamily("Inter")["Bold"][20]
    private val subHeaderFont get() = Fonts.getFamily("Inter")["SemiBold"][16]
    private val bodyFont get() = Fonts.getFamily("Inter")["Regular"][14]
    private val iconFont get() = Fonts.getFamily("Fa")["Regular"][16]
    private var isOverlayHovered = false

    private val backgroundColor = ImColor.rgba(30, 30, 30, 240)
    private val headerColor = ImColor.rgba(40, 40, 40, 255)
    private val accentColor = ImColor.rgba(100, 65, 165, 255)
    private val textColor = ImColor.rgba(220, 220, 220, 255)
    private val separatorColor = ImColor.rgba(60, 60, 60, 255)
    private val deleteButtonColor = ImColor.rgba(200, 60, 60, 255)

    private val propertyCache = mutableMapOf<UUID, Map<String, Property<*>>>()

    fun render(selectedNodes: Set<UUID>) {
        updateAnimationState(selectedNodes)
        if (animationProgress <= 0f) return

        val viewportSize = ImGui.getMainViewport().size
        val panelPos = ImVec2(-panelWidth * (1f - animationProgress), 0f)

        ImGui.setNextWindowPos(panelPos.x, panelPos.y)
        ImGui.setNextWindowSize(panelWidth, viewportSize.y)

        ImGui.begin(
            "Selection Properties",
            ImGuiWindowFlags.NoTitleBar or ImGuiWindowFlags.NoResize or ImGuiWindowFlags.NoMove
        )
        isOverlayHovered = ImGui.isWindowHovered(ImGuiHoveredFlags.RootAndChildWindows)
        renderHeader()

        if (selectedNodes.size > 1) {
            renderDeleteAllButton(selectedNodes)
        }

        selectedNodes.forEach { nodeId ->
            val node = workspace.getNode(nodeId) ?: return@forEach
            renderNodeElement(node)
            ImGui.separator()
        }

        ImGui.end()
    }

    private fun renderHeader() {
        ImGui.pushFont(headerFont)
        ImGui.text("Selection Properties")
        ImGui.popFont()

        ImGui.dummy(0f, 3f)
//        ImGui.sameLine(panelWidth - headerHeight)
//        if (ImGui.button(FontAwesome.Xmark)) {
//            isOpen = false
//        }
        ImGui.separator()

        ImGui.dummy(0f, 3f)
    }

    private fun renderDeleteAllButton(selectedNodes: Set<UUID>) {
        ImGui.pushStyleColor(ImGuiCol.Button, deleteButtonColor)
        if (ImGui.button("Delete All Selected Nodes", -1f, 0f)) {
            selectedNodes.forEach { nodeId ->
                Client { it.send(NodeDeleteRequest(nodeId)) }
            }
        }
        ImGui.popStyleColor()
    }

    private fun renderNodeElement(node: Node) {
        ImGui.pushFont(subHeaderFont)
        ImGui.text(node.name)
        ImGui.popFont()
        ImGui.dummy(0f, 5f)
        val properties = getNodeProperties(node)
        properties.forEach { (key, property) ->
            renderProperty(key, property, node)
        }
        ImGui.pushStyleColor(ImGuiCol.Button, deleteButtonColor)
        if (ImGui.button("Delete Node", -1f, 0f)) {
            Client { it.send(NodeDeleteRequest(node.uid)) }
        }
        ImGui.popStyleColor()


    }

    private fun getNodeProperties(node: Node): Map<String, Property<*>> {
        return propertyCache.getOrPut(node.uid) {
            listOf("type", "color", "x", "y", "width", "height")
                .mapNotNull { key -> node.properties[key]?.let { key to it } }
                .toMap()
        }
    }

    private fun renderProperty(key: String, property: Property<*>, node: Node) {
        ImGui.text("$key:")
        ImGui.sameLine(80f)

        val inputId = "##$key${node.uid}"
        when (property) {
            is Property.Int -> renderIntProperty(inputId, property, node, key)
            is Property.Float -> renderFloatProperty(inputId, property, node, key)
            is Property.String -> renderStringProperty(inputId, property, node, key)
            is Property.Vec4i -> renderColorProperty(inputId, property, node, key)
            else -> renderGenericProperty(inputId, property, node, key)
        }
    }

    private val intBuffer = ImInt()
    private fun renderIntProperty(inputId: String, property: Property.Int, node: Node, key: String) {
        intBuffer.set(property.get())
        if (ImGui.inputInt(inputId, intBuffer)) {
            updateNodeProperty(node, key, intBuffer.get())
        }
    }

    private val floatBuffer = ImFloat()
    private fun renderFloatProperty(inputId: String, property: Property.Float, node: Node, key: String) {
        floatBuffer.set(property.get())
        if (ImGui.inputFloat(inputId, floatBuffer)) {
            updateNodeProperty(node, key, floatBuffer.get())
        }
    }

    private fun renderStringProperty(inputId: String, property: Property.String, node: Node, key: String) {
        val value = ImString(property.get())
        if (ImGui.inputText(inputId, value)) {
            updateNodeProperty(node, key, value.get())
        }
    }

    private val colorBuffer = FloatArray(4)
    private fun renderColorProperty(inputId: String, property: Property.Vec4i, node: Node, key: String) {
        val value = property.get()
        colorBuffer[0] = value.x / 255f
        colorBuffer[1] = value.y / 255f
        colorBuffer[2] = value.z / 255f
        colorBuffer[3] = value.w / 255f

        if (ImGui.colorEdit4(inputId, colorBuffer)) {
            val newValue = Vector4i(
                (colorBuffer[0] * 255).toInt(),
                (colorBuffer[1] * 255).toInt(),
                (colorBuffer[2] * 255).toInt(),
                (colorBuffer[3] * 255).toInt()
            )
            updateNodeProperty(node, key, newValue)
        }
    }

    private fun renderGenericProperty(inputId: String, property: Property<*>, node: Node, key: String) {
        val value = ImString(property.get().toString())
        if (key == "type") {
            ImGui.textDisabled(value.get())
        } else {
            if (ImGui.inputText(inputId, value)) {
                updateNodeProperty(node, key, value.get())
            }
        }
    }

    private fun updateNodeProperty(node: Node, key: String, value: Any) {
        when (val property = node.properties[key]) {
            is Property.Int -> property.set(value as Int)
            is Property.Float -> property.set(value as Float)
            is Property.String -> property.set(value as String)
            is Property.Vec4i -> property.set(value as Vector4i)
            else -> {
                // Handle other property types or log an error
            }
        }
        // Uncomment the following line when NodePropertyUpdateRequest is implemented
        // endpoint.send(NodePropertyUpdateRequest(node.uid, key, value))
        propertyCache.remove(node.uid)  // Invalidate cache
    }

    private fun updateAnimationState(selectedNodes: Set<UUID>) {
        val targetState = selectedNodes.isNotEmpty()
        if (targetState != isOpen) {
            isOpen = targetState
        }

        val deltaTime = ImGui.getIO().deltaTime
        if (isOpen && animationProgress < 1f) {
            animationProgress = (animationProgress + deltaTime / animationDuration).coerceAtMost(1f)
        } else if (!isOpen && animationProgress > 0f) {
            animationProgress = (animationProgress - deltaTime / animationDuration).coerceAtLeast(0f)
        }
    }

    fun isVisible(): Boolean = animationProgress > 0f

    fun isHovered(): Boolean = isOverlayHovered
}