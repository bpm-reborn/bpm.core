package bpm.client.runtime.renders

import imgui.ImGui
import imgui.ImGuiWindowClass
import imgui.flag.ImGuiWindowFlags
import imgui.internal.flag.ImGuiDockNodeFlags
import imgui.type.ImBoolean
import bpm.client.render.IRender
import org.joml.Vector2f
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
     * Represents a boolean value indicating whether a variable is meant to hold floating point numbers.
     *
     * @property floating A boolean value indicating whether the variable is for floating point numbers.
     */
    var floating = false

    /**
     * Represents the size of a graphical object in a 2D space.
     *
     * @property width The width component of the size.
     * @property height The height component of the size.
     *
     * @constructor Creates a new instance of the [Vector2f] class with the specified width and height.
     *
     * @param width The width component of the size.
     * @param height The height component of the size.
     */
    var size: (() -> Pair<Float, Float>)? = null

    /**
     * A boolean variable indicating whether the content is centered or not.
     */
    var centered: Boolean = false

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
    public override fun render() {

        val open = if (!floating) {
            val winClass = ImGuiWindowClass()
            winClass.dockNodeFlagsOverrideSet = ImGuiDockNodeFlags.NoWindowMenuButton or ImGuiDockNodeFlags.NoCloseButton
            ImGui.setNextWindowClass(winClass)
            var flags = ImGuiWindowFlags.NoNavFocus or ImGuiWindowFlags.NoBringToFrontOnFocus or ImGuiWindowFlags.NoCollapse
            if (noTitleBar) flags = flags or ImGuiWindowFlags.NoTitleBar
            ImGui.begin(
                name,
                flags
            )
        } else {
            val noDocking = ImGuiWindowFlags.NoDocking or ImGuiWindowFlags.NoNavFocus or ImGuiWindowFlags.NoCollapse or ImGuiWindowFlags.NoDecoration
            if (size != null) {
                val size = size!!()
                ImGui.setNextWindowSize(size.first, size.second)
            }

            //center in window
            if (centered && size != null) {
                val size = size!!()
                val displaySize = ImGui.getIO().displaySize
                ImGui.setNextWindowPos(
                    (displaySize.x - size.first) / 2,
                    (displaySize.y - size.second) / 2,
                )
            }
            ImGui.begin(
                name,
                open,
                noDocking,
            )
        }
        if (open) {
            screen.render()

        }
        if (!this.open.get()) parent?.removeFloatingWindow(this)
        ImGui.end()
        super.render()
    }


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