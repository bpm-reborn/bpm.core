package bpm.client.runtime.windows

import imgui.ImColor
import imgui.ImDrawList
import imgui.ImGui
import imgui.ImVec2
import imgui.flag.*
import imgui.internal.ImRect
import imgui.type.ImString
import bpm.common.logging.KotlinLogging
import bpm.client.font.Fonts
import bpm.client.runtime.ClientRuntime
import bpm.client.utils.*
import bpm.common.network.Client
import bpm.common.utils.FontAwesome
import bpm.common.property.*
import bpm.common.workspace.packets.*
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import org.joml.Vector4i
import kotlin.math.max
import kotlin.math.min

class VariablesMenu(private val canvasContext: CanvasContext) {

    private val workspace get() = ClientRuntime.workspace ?: error("Workspace not available")
    // Logger
    private val logger = KotlinLogging.logger {}

    // UI constants
    private val boxWidth = 350f
    private val boxHeight = 300f
    private var boxPosition = Vector2f(20f, 20f)

    // Fonts
    private val headerFont get() = Fonts.getFamily("Inter")["ExtraBold"][18]
    private val bodyFont get() = Fonts.getFamily("Inter")["Regular"][16]
    private val iconFont get() = Fonts.getFamily("Fa")["Regular"][18]

    // Colors
    private val backgroundColor = ImColor.rgba(30, 30, 30, 240)
    private val headerColor = ImColor.rgba(40, 40, 40, 255)
    private val textColor = ImColor.rgba(220, 220, 220, 255)
    private val accentColor = ImColor.rgba(100, 65, 165, 255)

    // Variable creation
    private var isAddingVariable = false
    private var newVariableName = ImString(256)
    private var selectedVariableType = 0
    private val variableTypes = arrayOf("String", "Int", "Float", "Bool", "Vec2f", "Vec3f", "Vec4f", "Color")

    // Dragging and interaction
    private var draggedVariable: Pair<String, Property<*>>? = null
    private var hoveredVariable: String? = null
    private var popupPosition: Vector2f? = null
    private var isPopupInteractionStarted = false


    // Variable expansion
    private var expandedVariable: String? = null
    private val animationProgress = mutableMapOf<String, Float>()
    private var isClosing = false
    private var closingProgress = 1f

    // Scaling
    private val itemHeight = 35f
    private val maxHeight = ImGui.getIO().displaySize.y * 0.8f // 80% of screen height
    private var currentScrollY = 0f

    // Action menus
    private val variableActions = mutableMapOf<String, ActionWheelMenu>()
    private val actionMenusToRender = mutableListOf<Pair<String, ActionWheelMenu>>()

    fun render(drawList: ImDrawList) {
        ImGui.pushID("VariablesMenu")
        updateAnimations()

        val variables = workspace.graph.variables
        val contentHeight = variables.size * itemHeight
        val boxHeight = kotlin.math.min(contentHeight, maxHeight)

        // Apply closing animation
        val animatedHeight = boxHeight * closingProgress

        drawBox(drawList, animatedHeight)
        drawHeader(drawList)

        // Begin a child window for scrolling
        ImGui.beginChild("VariablesScroll", boxWidth - 10f, animatedHeight - 35f, false)


        ImGui.endChild()

        handleVariableDragging(drawList)

        if (isAddingVariable) {
            //Set popup position to be center of the screen
//            popupPosition = Vector2f(
//                (ImGui.getIO().displaySize.x - 200f) / 2,
//                (ImGui.getIO().displaySize.y - 120f) / 2
//            )
            renderAddVariablePopup(drawList)
        } else {


            drawVariableList(drawList, animatedHeight)
            // Render action menus after everything else
            renderActionMenus(ImGui.getWindowDrawList())
        }

        ImGui.popID()
        handleNodeCreation(drawList)
    }

    private fun drawBox(drawList: ImDrawList, height: Float) {
        drawList.addRectFilled(
            boxPosition.x, boxPosition.y,
            boxPosition.x + boxWidth, boxPosition.y + height + 35f,
            backgroundColor
        )
        drawList.addRect(
            boxPosition.x, boxPosition.y,
            boxPosition.x + boxWidth, boxPosition.y + height + 35f,
            ImColor.rgba(60, 60, 60, 255)
        )
    }

    private fun drawHeader(drawList: ImDrawList) {
        drawList.addRectFilled(
            boxPosition.x, boxPosition.y,
            boxPosition.x + boxWidth, boxPosition.y + 30f,
            headerColor
        )

        headerFont.use {
            drawList.addText(
                headerFont,
                16f,
                boxPosition.x + 10f,
                boxPosition.y + 7f,
                textColor,
                "Variables"
            )
        }

        val addButtonSize = 20f
        val addButtonPos = Vector2f(boxPosition.x + boxWidth - addButtonSize - 5f, boxPosition.y + 5f)
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
            if (!isAddingVariable) {
                isAddingVariable = true
                isPopupInteractionStarted = true
                popupPosition = Vector2f(
                    boxPosition.x + (boxWidth - 200f) / 2,
                    boxPosition.y + 50f
                )
            }
        }
    }

    private fun drawVariableList(drawList: ImDrawList, maxHeight: Float) {
        var yOffset = 35f
        workspace.graph.variables.forEach { (name, variable) ->
            val progress = animationProgress[name] ?: 0f
            drawVariableItem(drawList, name, variable, yOffset, progress)
            yOffset += 35f + 80f * progress
        }
    }


    private fun drawVariableItem(
        drawList: ImDrawList,
        name: String,
        variable: Property<*>,
        yOffset: Float,
        progress: Float
    ) {
        val boxWidth = min(500f, this.boxWidth)
        val nodeColor = getColorForType(variable)
        val (icon, value) = getIconAndValueForVariable(variable)

        // Draw the background
        drawList.addRectFilled(
            boxPosition.x + 5f, boxPosition.y + yOffset,
            boxPosition.x + boxWidth - 5f, boxPosition.y + yOffset + 30f,
            nodeColor
        )

        //draw a border around the variable
//        val iconSize = iconFont.sizeOf(icon)
        val padding = 2f

        //Rounded multi color border for value
        val valueTextSize = bodyFont.sizeOf(value)
        val headerTextSize = headerFont.sizeOf(name)
        val typeText = variable::class.simpleName!!
        val typeTextSize = bodyFont.sizeOf(typeText)
        val margin: Float = 40f
        val startX = boxPosition.x + boxWidth - valueTextSize.x - margin
        val typeRect = ImRect(
            boxPosition.x + headerTextSize.x + 45f,
            boxPosition.y + yOffset + 25f - typeTextSize.y - 5f,
            boxPosition.x + headerTextSize.x + 40f + typeTextSize.x + 15f,
            boxPosition.y + yOffset + 25f
        )

        val valueRect = ImRect(
            startX,
            boxPosition.y + yOffset + 25f - typeTextSize.y - 5f,
            startX + valueTextSize.x + 10f,
            boxPosition.y + yOffset + 25f
        )
        val iconRect = ImRect(
            boxPosition.x,
            boxPosition.y + yOffset - padding,
            boxPosition.x + 20f + padding,
            boxPosition.y + yOffset + 30f + padding
        )
        drawList.addRectFilledMultiColor(
            iconRect.min.x - 2f, iconRect.min.y - 2f,
            iconRect.max.x + 2f, iconRect.max.y + 2f,
            ImColor.rgba(33, 33, 33, 255).toLong(),
            ImColor.rgba(22, 22, 22, 255).toLong(),
            ImColor.rgba(33, 33, 33, 255).toLong(),
            ImColor.rgba(22, 22, 22, 255).toLong()
        )

        // Draw the icon
        iconFont.use {
            drawList.addText(
                iconFont,
                16f,
                boxPosition.x + 5f,
                boxPosition.y + yOffset + 7f,
                textColor,
                icon
            )
        }

        //Creates a little bit offset and larger text for a shadow
        drawList.addText(
            headerFont,
            20f,
            boxPosition.x + 80f + 2f,
            boxPosition.y + yOffset + padding * 2 + 2f,
            ImColor.rgba(33, 33, 33, 255),
            name
        )

        // Draw the variable name and value
        drawList.addText(
            headerFont,
            20f,
            boxPosition.x + 80f,
            boxPosition.y + yOffset + padding * 2,
            textColor,
            name
        )


        drawList.addRectFilled(
            valueRect.min.x - 2f, valueRect.min.y - 2f,
            valueRect.max.x + 2f, valueRect.max.y + 2f,
            ImColor.rgba(33, 33, 33, 255),
            5f
        )

        //Draw the value text
        bodyFont.use {
            drawList.addText(
                bodyFont,
                16f,
                startX + 5f,
                boxPosition.y + yOffset + 25f - valueTextSize.y - 2f,
                textColor,
                value
            )
        }

        //Draw the type, right aligned
        //draw smaller ligher gray rounded inner border rectangle
        drawList.addRectFilled(
            boxPosition.x + 26, typeRect.min.y - 2f,
            boxPosition.x + 26 + typeTextSize.x + 10f, typeRect.max.y + 2f,
            ImColor.rgba(54, 58, 66, 233)
        )

        drawList.addRectFilledMultiColor(
            boxPosition.x + 26, typeRect.min.y - 2f,
            boxPosition.x + 26 + typeTextSize.x + 10f, typeRect.max.y + 2f,
            ImColor.rgba(33, 33, 33, 255).toLong(),
            ImColor.rgba(22, 22, 22, 40).toLong(),
            ImColor.rgba(33, 33, 33, 255).toLong(),
            ImColor.rgba(22, 22, 22, 120).toLong(),
        )



        drawList.addText(
            bodyFont,
            16f,
            boxPosition.x + 30f,
            boxPosition.y + yOffset + 25f - typeTextSize.y - 2f,
            textColor,
            typeText
        )


        // Always create or update the action menu, regardless of variable type
        val actionMenu = variableActions.getOrPut(name) {
            createActionWheel(name, variable)
        }
        actionMenu.actions[1].label = if (expandedVariable == name) "Collapse" else "Expand"
        actionMenu.reposition(boxPosition.x + boxWidth - 15f, boxPosition.y + yOffset + 15f)

        // Always add the action menu to the render list
        actionMenusToRender.add(name to actionMenu)

        // Handle expanded state if necessary
        if (progress > 0f) {
            val expandedHeight = 80f * progress
            ImGui.getWindowDrawList().addRectFilled(
                boxPosition.x + 5f, boxPosition.y + yOffset + 30f,
                boxPosition.x + boxWidth - 5f, boxPosition.y + yOffset + 30f + expandedHeight,
                ImColor.rgba(50, 50, 50, 255)
            )

            // Render the property input, ensuring it doesn't interfere with the action menu
            val updated = PropertyInput.render(
                ImGui.getForegroundDrawList(),
                name,
                variable,
                boxPosition.x + 10f,
                boxPosition.y + yOffset + 35f,
                boxWidth - 40f  // Reduced width to avoid overlapping with action menu
            )

            if (updated) {
                Client {
                    it.send(VariableUpdateRequest(name, Property.Object().apply {
                        this["type"] = Property.String(variable::class.simpleName!!)
                        this["value"] = variable
                    }))
                }
            }
        }
    }

    private var currentActionMenu: ActionWheelMenu? = null

    private fun renderActionMenus(drawList: ImDrawList) {
        // Render all main action menus
        actionMenusToRender.forEach { (name, menu) ->
            if (menu.render(drawList)) {
                when (menu.selectedActionIndex) {
                    0 -> { // "Make Node" action
                        val variable = workspace.graph.variables[name]
                        if (variable != null) {
                            openNodeTypeSelectionMenu(name, variable)
                        }
                    }

                    1 -> toggleVariableExpansion(name) // "Edit variable" action
                    2 -> workspace.removeVariable(name) // "Delete Variable" action
                }
            }
        }

        // Render sub-menu if it exists
        currentActionMenu?.let { subMenu ->
            if (subMenu.render(drawList)) {
                handleSubMenuAction(subMenu.selectedActionIndex)
                currentActionMenu = null
            }
        }

        actionMenusToRender.clear()
    }

    private fun handleSubMenuAction(actionIndex: Int) {
        currentActionMenu = null  // Close the sub-menu after any action
    }

    private fun openNodeTypeSelectionMenu(name: String, variable: Property<*>) {
        val mainMenuRadius = 50f
        val subMenuRadius = 50f

        val mainMenuCenterX = boxPosition.x + boxWidth - 15f
        val variableIndex = actionMenusToRender.indexOfFirst { it.first == name }
        val mainMenuCenterY = boxPosition.y + (variableIndex + 1) * itemHeight + 15f

        currentActionMenu = ActionWheelMenu(
            centerX = mainMenuCenterX,
            centerY = mainMenuCenterY,
            baseRadius = subMenuRadius,
            baseFontSize = 20f,
            actions = listOf(
                WheelAction("Get", FontAwesome.ArrowRight) {
                    startNodeCreation(name, "Variables/Get Variable", Vector2f(mainMenuCenterX, mainMenuCenterY))
                },
                WheelAction("Cancel", FontAwesome.Xmark) {
                    // Do nothing, just close the sub-menu
                },
                WheelAction("Set", FontAwesome.ArrowLeft) {
                    startNodeCreation(name, "Variables/Set Variable", Vector2f(mainMenuCenterX, mainMenuCenterY))
                },

                ),
            isTopLevel = false
        )
    }


    private fun createActionWheel(name: String, variable: Property<*>): ActionWheelMenu =
        ActionWheelMenu(
            centerX = 0f,
            centerY = 0f,
            baseRadius = 50f,
            baseFontSize = 20f,
            actions = listOf(
                WheelAction("Getter/Setter", FontAwesome.Cube) {
                    logger.info { "Opening node type selection menu for $name" }
                    openNodeTypeSelectionMenu(name, variable)
                },
                WheelAction("Expand", FontAwesome.ChevronDown) {
                    toggleVariableExpansion(name)
                },
                WheelAction("Delete", FontAwesome.Trash) {
//                    workspace.removeVariable(name)
                    Client { it.send(VariableDeleteRequest(name)) }
                }
            ),
            isTopLevel = true,
            icon = FontAwesome.Gear
        )


    private fun handleVariableDragging(drawList: ImDrawList) {
        val draggedVar = draggedVariable
        if (draggedVar != null && ImGui.isMouseDragging(0)) {
            val mousePos = ImGui.getMousePos()
            bodyFont.use {
                drawList.addText(
                    bodyFont,
                    14f,
                    mousePos.x + 15f,
                    mousePos.y + 15f,
                    textColor,
                    draggedVar.first
                )
            }
        } else if (draggedVar != null && ImGui.isMouseReleased(0)) {
            val mousePos = ImGui.getMousePos()
            val worldPos = canvasContext.convertToWorldCoordinates(Vector2f(mousePos.x, mousePos.y))
//            canvasContext.createNode(worldPos, nodeType)
//            client.send(VariableNodeCreateRequest(NodeType.GetVariable, worldPos, draggedVar.first))
            canvasContext.createVariableNode(NodeType.GetVariable, worldPos, draggedVar.first)
            draggedVariable = null
        }
    }

    private fun renderAddVariablePopup(drawList: ImDrawList) {
        val popupPos = popupPosition ?: return
        val popupWidth = 200f
        val popupHeight = 140f
        val padding = 10f

        drawList.addRectFilled(
            popupPos.x, popupPos.y,
            popupPos.x + popupWidth, popupPos.y + popupHeight,
            ImColor.rgba(44, 44, 44, 240),
            10f
        )

        bodyFont.use {
            //Creates a shadow effect for the new variable text
            drawList.addText(
                headerFont,
                20f,
                popupPos.x + padding + 3f,
                popupPos.y + padding + 3f,
                ImColor.rgba(0, 0, 0, 255),
                "New Variable"
            )

            drawList.addText(
                headerFont,
                20f,
                popupPos.x + padding,
                popupPos.y + padding,
                textColor,
                "New Variable"
            )

            //Seperator
            drawList.addLine(
                popupPos.x + padding, popupPos.y + 32f,
                popupPos.x + popupWidth - padding, popupPos.y + 32f,
                textColor,
                1f
            )

            val inputBgColor = ImColor.rgba(22, 22, 22, 255)
            val inputRect = ImRect(
                popupPos.x + padding, popupPos.y + 40f,
                popupPos.x + popupWidth - padding, popupPos.y + 70f
            )
            drawList.addRectFilled(
                inputRect.min.x, inputRect.min.y,
                inputRect.max.x, inputRect.max.y,
                inputBgColor,
                3f
            )

            if (ImGui.isMouseClicked(0) && inputRect.contains(ImGui.getMousePos())) {
                ImGui.setKeyboardFocusHere()
            }

            ImGui.setCursorPos(inputRect.min.x + 5, inputRect.min.y + 3)
            ImGui.pushItemWidth(inputRect.max.x - inputRect.min.x - 10)
            ImGui.pushStyleColor(ImGuiCol.FrameBg, 0)
            ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 0f, 5f)
            ImGui.inputText("##variableName", newVariableName)
            ImGui.popStyleColor()
            ImGui.popStyleVar()

            if (newVariableName.get().isBlank()) {
                drawList.addText(
                    bodyFont,
                    14f,
                    inputRect.min.x + 5,
                    inputRect.min.y + 8,
                    ImColor.rgba(150, 150, 150, 180),
                    "Name"
                )
            }

            drawList.addRectFilled(
                popupPos.x + padding, popupPos.y + 80f,
                popupPos.x + popupWidth - padding, popupPos.y + 110f,
                inputBgColor,
                3f
            )
            drawList.addText(
                bodyFont,
                14f,
                popupPos.x + padding + 5f,
                popupPos.y + 88f,
                textColor,
                variableTypes[selectedVariableType]
            )

            val addButtonWidth = 60f
            val addButtonColor = ImColor.rgba(100, 65, 165, 255)
            val addButtonRect = ImRect(
                popupPos.x + popupWidth - addButtonWidth - padding, popupPos.y + popupHeight - 40f,
                popupPos.x + popupWidth - padding, popupPos.y + popupHeight - 10f
            )
            drawList.addRectFilled(
                addButtonRect.min.x, addButtonRect.min.y,
                addButtonRect.max.x, addButtonRect.max.y,
                addButtonColor,
                3f
            )
            val addText = "Add"
            val addTextSize = ImGui.calcTextSize(addText)
            drawList.addText(
                bodyFont,
                14f,
                addButtonRect.min.x + (addButtonRect.getWidth() - addTextSize.x) / 2,
                addButtonRect.min.y + (addButtonRect.getHeight() - addTextSize.y) / 2,
                textColor,
                addText
            )

            if (ImGui.isMouseClicked(0) && addButtonRect.contains(ImGui.getMousePos())) {
                if (newVariableName.isNotEmpty()) {
                    addVariable(newVariableName.get(), variableTypes[selectedVariableType])
                    closePopup()
                }
            }
        }

        handleAddVariablePopupInteraction(popupPos, popupWidth, popupHeight)
    }

    private fun handleAddVariablePopupInteraction(popupPos: Vector2f, popupWidth: Float, popupHeight: Float) {
        if (isPopupInteractionStarted) {
            if (!ImGui.isMouseDown(0)) {
                isPopupInteractionStarted = false
            }
            return
        }

        val padding = 10f

        if (isMouseOver(
                popupPos.x + padding,
                popupPos.y + 80f,
                popupWidth - padding * 2,
                30f
            ) && ImGui.isMouseClicked(0)
        ) {
            selectedVariableType = (selectedVariableType + 1) % variableTypes.size
        }

        if ((ImGui.isMouseClicked(0) && !isMouseOver(popupPos.x, popupPos.y, popupWidth, popupHeight))
            || ImGui.isKeyPressed(ImGui.getKeyIndex(ImGuiKey.Escape))
        ) {
            closePopup()
        }
    }

    private fun addVariable(name: String, type: String) {
        val property = when (type) {
            "String" -> Property.String("")
            "Int" -> Property.Int(0)
            "Float" -> Property.Float(0f)
            "Bool" -> Property.Boolean(false)
            "Vec2f" -> Property.Vec2f(Vector2f(0f, 0f))
            "Vec3f" -> Property.Vec3f(Vector3f(0f, 0f, 0f))
            "Vec4f" -> Property.Vec4f(Vector4f(0f, 0f, 0f, 0f))
            "Color" -> Property.Vec4i(Vector4i(255, 255, 255, 255))
            else -> return
        }
        Client {
            it.send(VariableCreateRequest(name,
                Property.Object().apply {
                    this["type"] = Property.String(type)
                    this["value"] = property
                }
            ))
        }
    }

    private fun getColorForType(variable: Property<*>): Int {
        return when (variable) {
            is Property.String -> ImColor.rgba(230, 126, 34, 200)
            is Property.Int, is Property.Float -> ImColor.rgba(46, 204, 113, 200)
            is Property.Boolean -> ImColor.rgba(241, 196, 15, 200)
            is Property.Vec2f, is Property.Vec3f, is Property.Vec4f -> ImColor.rgba(52, 152, 219, 200)
            else -> ImColor.rgba(155, 89, 182, 200)
        }
    }

    private fun getIconAndValueForVariable(variable: Property<*>): Pair<String, String> {
        return when (variable) {
            is Property.String -> Pair(FontAwesome.Font, "\"${variable.get().take(10)}\"")
            is Property.Int -> Pair(FontAwesome.Hashtag, variable.get().toString())
            is Property.Float -> Pair(FontAwesome.Percent, String.format("%.2f", variable.get()))
            is Property.Boolean -> Pair(FontAwesome.ToggleOn, if (variable.get()) "true" else "false")
            is Property.Vec2f -> Pair(FontAwesome.VectorSquare, "(${variable.get().x}, ${variable.get().y})")
            is Property.Vec3f -> Pair(
                FontAwesome.Cube,
                "(${variable.get().x}, ${variable.get().y}, ${variable.get().z})"
            )

            is Property.Vec4f -> Pair(
                FontAwesome.Cubes,
                "(${variable.get().x}, ${variable.get().y}, ${variable.get().z}, ${variable.get().w})"
            )

            is Property.Vec4i -> Pair(
                FontAwesome.Paintbrush,
                "RGB(${variable.get().x}, ${variable.get().y}, ${variable.get().z})"
            )

            else -> Pair(FontAwesome.Question, "Unknown")
        }
    }

    private data class DraggedNodeInfo(
        val name: String,
        val nodeType: String,
        var position: Vector2f,
        var isPlaced: Boolean = false
    )

    private var draggedNodeInfo: DraggedNodeInfo? = null
    private fun startNodeCreation(name: String, nodeType: String, position: Vector2f) {
        draggedNodeInfo = DraggedNodeInfo(name, nodeType, position)
        ImGui.setMouseCursor(ImGuiMouseCursor.ResizeAll)
    }

    private fun handleNodeCreation(drawList: ImDrawList) {
        draggedNodeInfo?.let { info ->
            val mousePos = ImGui.getMousePos()
            info.position = Vector2f(mousePos.x, mousePos.y)

            // Draw temporary node representation
            drawTemporaryNode(drawList, info)

            if (ImGui.isMouseClicked(0)) {
                // Create the actual node on the first click
                if (info.nodeType == "Variables/Get Variable") {
                    canvasContext.createVariableNode(NodeType.GetVariable, info.position, info.name)
                } else {
                    canvasContext.createVariableNode(NodeType.SetVariable, info.position, info.name)

                }
//                canvasContext.createNode(info.position, info.nodeType)
                draggedNodeInfo = null
                ImGui.setMouseCursor(ImGuiMouseCursor.Arrow)
            }

            // Allow canceling the node creation with right-click
            if (ImGui.isMouseClicked(1)) {
                draggedNodeInfo = null
                ImGui.setMouseCursor(ImGuiMouseCursor.Arrow)
            }
        }
    }


    private fun drawTemporaryNode(drawList: ImDrawList, info: DraggedNodeInfo) {
        val nodeColor = when (info.nodeType) {
            "Variables/Get Variable" -> ImColor.rgba(46, 204, 113, 200)
            "Variables/Set Variable" -> ImColor.rgba(231, 76, 60, 200)
            else -> ImColor.rgba(149, 165, 166, 200)
        }

        val nodeSize = Vector2f(120f, 60f)
        val nodePos = info.position

        // Draw node background
        drawList.addRectFilled(
            nodePos.x, nodePos.y,
            nodePos.x + nodeSize.x, nodePos.y + nodeSize.y,
            nodeColor, 5f
        )

        // Draw node outline
        drawList.addRect(
            nodePos.x, nodePos.y,
            nodePos.x + nodeSize.x, nodePos.y + nodeSize.y,
            ImColor.rgba(255, 255, 255, 100), 5f, 0, 1f
        )

        // Draw node title
        val title = if (info.nodeType == "Variables/Get Variable") "Get" else "Set"
        val titlePos = Vector2f(nodePos.x + 10, nodePos.y + 10)
        drawList.addText(titlePos.x, titlePos.y, ImColor.rgba(255, 255, 255, 255), "$title: ${info.name}")

        // Draw instruction text
        val instructionText = "Click to place"
        val instructionPos = Vector2f(nodePos.x + 10, nodePos.y + nodeSize.y - 20)
        drawList.addText(instructionPos.x, instructionPos.y, ImColor.rgba(200, 200, 200, 200), instructionText)
    }


    private fun createGetVariableNode(name: String, variable: Property<*>, position: Vector2f) {

    }

    private fun isMouseOver(x: Float, y: Float, width: Float, height: Float): Boolean {
        val mousePos = ImGui.getMousePos()
        return mousePos.x >= x && mousePos.x <= x + width && mousePos.y >= y && mousePos.y <= y + height
    }

    private fun toggleVariableExpansion(name: String) {
        if (expandedVariable == name) {
            // If the clicked variable is already expanded, close it
            expandedVariable = null
        } else {
            // If a different variable is clicked, expand it and start closing the previously expanded one
            expandedVariable = name
        }
    }


    private fun updateAnimations() {
        val animationSpeed = 5f
        val deltaTime = ImGui.getIO().deltaTime

        workspace.graph.variables.keys.forEach { name ->
            val targetProgress = if (name == expandedVariable) 1f else 0f
            val currentProgress = animationProgress.getOrPut(name) { 0f }

            if (currentProgress < targetProgress) {
                animationProgress[name] = min(1f, currentProgress + deltaTime * animationSpeed)
            } else if (currentProgress > targetProgress) {
                animationProgress[name] = max(0f, currentProgress - deltaTime * animationSpeed)
            }

            // Remove completed closing animations
            if (animationProgress[name] == 0f && name != expandedVariable) {
                animationProgress.remove(name)
            }
        }

        // Update closing animation for the entire menu
        if (isClosing) {
            closingProgress = max(0f, closingProgress - deltaTime * animationSpeed)
            if (closingProgress == 0f) {
                isClosing = false
            }
        } else {
            closingProgress = min(1f, closingProgress + deltaTime * animationSpeed)
        }
    }

    fun closePopup() {
        isAddingVariable = false
        popupPosition = null
        newVariableName.set("")
        isPopupInteractionStarted = false
    }

    fun update() {
        if (ImGui.isKeyPressed(ImGui.getKeyIndex(ImGuiKey.Delete)) && hoveredVariable != null) {
            // Implement variable deletion logic here
            hoveredVariable = null
        }
    }

    fun close() {
        isClosing = true
    }

    fun open() {
        isClosing = false
    }

    fun isVisible(): Boolean = closingProgress > 0f

    companion object {

        private fun ImRect.getWidth(): Float = this.max.x - this.min.x
        private fun ImRect.getHeight(): Float = this.max.y - this.min.y

        private fun ImRect.contains(pos: ImVec2): Boolean {
            return pos.x >= this.min.x && pos.x <= this.max.x && pos.y >= this.min.y && pos.y <= this.max.y
        }
    }
}