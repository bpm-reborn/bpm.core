package bpm.client.render.renderspace

import bpm.client.render.IRender
import bpm.client.runtime.windows.CanvasGraphics
import imgui.ImGui
import imgui.ImGuiWindowClass
import imgui.flag.ImGuiTabItemFlags
import imgui.flag.ImGuiWindowFlags
import imgui.internal.flag.ImGuiDockNodeFlags
import imgui.type.ImBoolean
import net.minecraft.client.gui.GuiGraphics
import java.util.UUID

class Window(name: String, private val screen: IRender, window: Dockable) : Dockable(name, window) {

    /**
     * Represents a unique identifier for a screen.
     *
     * @property screenId A randomly generated UUID that uniquely identifies the screen.
     */
    val screenId: UUID = UUID.randomUUID()

    /**
     * Determines whether the title bar should be displayed.
     *
     * @param noTitleBar A boolean value indicating whether the title bar should be displayed.
     * @return Nothing.
     */
    var noTitleBar = false

    /**
     * Determines whether the window should be resizable.
     *
     * @param noResize A boolean value indicating whether the window should be resizable.
     * @return Nothing.
     */
    var noResize = false

    /**
     * Represents a boolean value indicating whether a variable is meant to hold floating point numbers.
     *
     * @property floating A boolean value indicating whether the variable is for floating point numbers.
     */
    var floating = false
    /**
     * Checks if the object is currently open.
     *
     * @return `true` if the object is open, `false` otherwise.
     */
    val isOpen: Boolean
        get() = open.get()

    /**
     * Represents the state of openness.
     *
     * @property open The state of openness.
     */
    private val open = ImBoolean(true)


    /**
     * Renders the window and its contents.
     * The window can have various flags that modify its behavior, such as no navigation focus,
     * no bring to front on focus, no collapse, no title bar, and no docking.
     *
     * @see [ImGuiWindowFlags]
     * @see [ImGuiWindowClass]
     */
    public override fun render(gfx: GuiGraphics, graphics: CanvasGraphics) {


        val open = if (!floating) {
            val winClass = ImGuiWindowClass()

            var flags = ImGuiWindowFlags.NoNavFocus or ImGuiWindowFlags.NoBringToFrontOnFocus or ImGuiWindowFlags.NoCollapse or ImGuiWindowFlags.NoScrollbar or ImGuiWindowFlags.NoScrollWithMouse

            if (noTitleBar) {
                // Window flags to prevent movement and resizing
                flags = flags or ImGuiWindowFlags.NoTitleBar or
                        ImGuiWindowFlags.NoMove or
                        ImGuiWindowFlags.NoResize

                // Dock node flags to prevent resizing from dock edges and remove window controls
                winClass.dockNodeFlagsOverrideSet = ImGuiDockNodeFlags.NoWindowMenuButton or
                        ImGuiDockNodeFlags.NoCloseButton or
                        ImGuiDockNodeFlags.NoTabBar or
                        ImGuiDockNodeFlags.NoDocking or
                        ImGuiDockNodeFlags.NoDockingSplitMe

                ImGui.setNextWindowSizeConstraints(
                    ImGui.getWindowWidth(), ImGui.getWindowHeight(),  // min width/height
                    ImGui.getWindowWidth(), ImGui.getWindowHeight()   // max width/height
                )
            } else {
                winClass.dockNodeFlagsOverrideSet = ImGuiDockNodeFlags.NoWindowMenuButton or
                        ImGuiDockNodeFlags.NoCloseButton
            }

            if (noResize) {
                winClass.dockNodeFlagsOverrideSet = ImGuiDockNodeFlags.NoWindowMenuButton or
                        ImGuiDockNodeFlags.NoCloseButton or
                        ImGuiDockNodeFlags.NoTabBar or
                        ImGuiDockNodeFlags.NoDocking or
                        ImGuiDockNodeFlags.NoResize or
                        ImGuiDockNodeFlags.NoResizeY
                ImGuiDockNodeFlags.NoDockingSplitMe

            }

            ImGui.setNextWindowClass(winClass)
            ImGui.begin(
                name,
                flags
            )
        } else {
            ImGui.begin(
                name,
                open,
            )
        }
        if (open) {
            screen.render(graphics, gfx)
            if (width == 0 || height == 0) {
                width = imgui.ImGui.getWindowSize().x.toInt()
                height = imgui.ImGui.getWindowSize().y.toInt()
            }
        }
        if (!this.open.get()) parent?.removeFloatingWindow(this)
        ImGui.end()
        super.render(gfx, graphics)
    }


//    public override fun render() {
//        val winClass = ImGuiWindowClass()
//        winClass.dockNodeFlagsOverrideSet = ImGuiDockNodeFlags.NoWindowMenuButton or ImGuiDockNodeFlags.NoCloseButton
//        ImGui.setNextWindowClass(winClass)
//        var flags = ImGuiWindowFlags.NoNavFocus or ImGuiWindowFlags.NoBringToFrontOnFocus or ImGuiWindowFlags.NoCollapse
//        if (noTitleBar) flags = flags or ImGuiWindowFlags.NoTitleBar
//        if (floating) flags = flags or ImGuiWindowFlags.NoDocking
//
//        if (ImGui.begin(name, open, flags)){
//            screen.render()
//            super.render()
//        }
//        ImGui.end()
//        if(!isOpen && floating) parent?.removeFloatingWindow(this)
//    }


    /**
     * Checks if the current Window object is equal to the provided object.
     *
     * @param other The object to compare against.
     * @return Returns true if the current Window object is equal to the provided object, false otherwise.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Window

        if (screen != other.screen) return false
        if (screenId != other.screenId) return false

        return true
    }

    /**
     * Computes the hash code value for this object.
     *
     * @return The hash code value for this object.
     */
    override fun hashCode(): Int {
        var result = screen.hashCode()
        result = 31 * result + screenId.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     *
     * @return A string representation of the screenId.
     */
    override fun toString(): String {
        return screenId.toString()
    }

}