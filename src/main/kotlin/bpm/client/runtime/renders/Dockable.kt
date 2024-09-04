package bpm.client.runtime.renders

import imgui.type.ImInt
import bpm.client.render.IRender

/**
 * Represents a dockable object that can be used in a docking system.
 *
 * @property name The name of the dockable object.
 * @property parent The parent dockable object, if any.
 * @property outputIds The output IDs of the dockable object for each split direction.
 * @property dockedWindows The windows currently docked to the dockable object for each split direction.
 * @property dockspaceIds The dockspace IDs of the dockable object for each split direction.
 * @property dockspaceId The dockspace ID of the dockable object. If the object has a parent, it returns the parent's dockspace ID. Otherwise, it returns the object's name.
 * @property isRoot Indicates whether the dockable object is a root object or not.
 * @property dirty Indicates whether the dockable object has been modified and needs to be saved.
 * @property dockMainId The main dock ID of the dockable object.
 * @property dockMain The main dock of the dockable object. Throws an exception if dockMainId is null and the object is not a root object.
 * @property split The split configuration of the dockable object.
 */
abstract class Dockable(val name: String, protected val parent: Dockable? = null) {

    /**
     * Variable to store output IDs for each direction of a split.
     *
     * @property outputIds A mutable map that holds the output IDs for each direction of a split.
     */
    protected val outputIds: MutableMap<Split.Direction, Int> = mutableMapOf()

    /**
     * Represents the windows currently docked in the application.
     *
     * It is a mutable map where the keys are the directions in which
     * the windows are docked and the values are the corresponding
     * docked windows.
     *
     * @property dockedWindows The mutable map of docked windows.
     */
    protected val dockedWindows: MutableMap<Split.Direction, Window> = mutableMapOf()

    /**
     * This variable represents a mutable list of floating windows.
     * Floating windows are windows that are not attached to any parent container.
     *
     * The list provides a way to keep track of all the floating windows currently
     * present in the application.
     *
     * @property floatingWindows The list of floating windows.
     */
    protected val floatingWindows: MutableList<Window> = mutableListOf()

    /**
     * The dockspaceIds variable is a protected property representing a mutable map
     * that associates directions with integer values.
     *
     * The keys in the map represent Split.Direction objects, which specify the direction
     * of a split. Splits can be horizontal (Split.Direction.Horizontal) or vertical
     * (Split.Direction.Vertical).
     *
     * The values in the map represent integer identifiers associated with each
     * specific dockspace. The identifiers can be used to reference dockspaces
     * during runtime or for other purposes.
     *
     * This mutable map allows adding, removing, and updating entries at runtime.
     * It provides a convenient way to manage and access dockspaces using their
     * associated direction and identifier.
     *
     * @since [insert initial version]
     */
    protected val dockspaceIds: MutableMap<Split.Direction, Int> = mutableMapOf()
    /**
     * Returns the ID of the dockspace.
     *
     * @return The ID of the dockspace. If the parent dockspace is not null, its ID is returned. If the parent dockspace
     *         is null, the name of the dockspace is returned.
     */
    protected val dockspaceId: String get() = parent?.dockspaceId ?: name
    /**
     * Determines if the current object is the root object.
     *
     * @return true if the object is the root, false otherwise.
     */
    protected val isRoot: Boolean get() = parent == null
    /**
     * Represents the state of dirtiness.
     *
     * This variable indicates whether an object is dirty, meaning it has been
     * modified but not yet saved or updated. By default, an object is not dirty.
     * This variable can be accessed and manipulated only within the class or its
     * subclasses.
     *
     * @since 1.0.0
     */
    protected var dirty = false
    /**
     * The identifier for the main dock.
     *
     * This variable holds the identifier for the main dock. The identifier is an `ImInt` type,
     * which allows for an immutable and nullable integer value.
     *
     * @since 1.0.0
     */
    var dockMainId: ImInt? = null
    /**
     * Represents the main dock associated with this instance.
     *
     * The dockMain variable is a protected property of type ImInt.
     *
     * @return The main dock ID if it is set, otherwise retrieves the parent's main dock ID if the current instance is not the root.
     *
     * @throws IllegalStateException if the dockMainId is null and the current instance is the root.
     */
    protected val dockMain: ImInt
        get() = if (dockMainId == null && !isRoot) parent!!.dockMain else dockMainId
            ?: throw IllegalStateException("DockMainId is null")
    /**
     * Represents a split used for dividing a space into two sections.
     *
     * @property direction The direction in which the split will be applied.
     * @property factor The position where the split will be placed along the direction.
     */
    protected var split = Split(Split.Direction.CENTER, 0f)
    /**
     * Represents a flag indicating whether the dock has been built or not.
     */
    private var hasBuilt = false

    /**
     * A private variable representing a list of [Window] objects that are ready to be closed.
     */
    private val closeQueue = mutableListOf<Window>()

    /**
     * Adds a window to the docked windows collection and marks the
     * docking area as dirty.
     *
     * @param window The window to be added to the docked windows collection.
     * @return The added window.
     */
    fun addWindow(window: Window): Window {
        dockedWindows[window.split.direction] = window
        markDirty()
        return window
    }

    /**
     * Adds a floating window to the list of floating windows.
     *
     * @param window The floating window to add.
     * @return The added floating window.
     */
    fun addFloatingWindow(window: Window): Window {
        floatingWindows.add(window)
        markDirty()
        return window
    }

    /**
     * Adds a new window with the given name and renderer to the container.
     * The window will be docked according to the specified split direction and size.
     *
     * @param name The name of the window.
     * @param renderer The renderer to be used for the window.
     * @param splitDirection The direction in which the window should be split. Default is CENTER.
     * @param splitSize The size of the split. Default is 0.2f.
     * @return The newly created Window object.
     */
    fun addWindow(
        name: String,
        renderer: IRender,
        splitDirection: Split.Direction = Split.Direction.CENTER,
        splitSize: Float = 0.2f
    ): Window {
        val dock = Window(name, renderer, this)
        dock.split = Split(splitDirection, splitSize)
        dockedWindows[splitDirection] = dock
        markDirty()
        return dock
    }

    /**
     * Adds a floating window to the application.
     *
     * @param name The name of the window.
     * @param renderer The renderer for the window.
     * @param noTitleBar Whether the window should have a title bar or not. Default value is false.
     * @return The created Window object.
     */
    fun addFloatingWindow(
        name: String,
        renderer: IRender,
        noTitleBar: Boolean = false
    ): Window {
        // Add to root recursively
        val dock = Window(name, renderer, this)
        dock.floating = true
        dock.noTitleBar = noTitleBar
        floatingWindows.add(dock)
        markDirty()
        return dock
    }

    /**
     * Removes a window from the docking container based on the specified direction.
     *
     * @param direction the direction in which the window is located
     */
    fun removeWindow(direction: Split.Direction) {
        dockedWindows.remove(direction)
        markDirty()
    }

    /**
     * Removes a window from the docked windows collection.
     *
     * @param window The window to be removed.
     */
    fun removeWindow(window: Window) {
        closeQueue.add(window)
    }

    fun removeFloatingWindow(window: Window) {
        closeQueue.add(window)
    }

    /**
     * Marks the object as dirty, indicating that it has been modified and needs to be saved.
     * If the object has a parent, the parent will also be marked as dirty.
     */
    protected fun markDirty() {
        dirty = true
        parent?.markDirty()
    }

    /**
     * Processes the data and renders the output.
     * If the data is dirty, the dock is rebuilt before rendering.
     */
    fun process() {
        render()
        //Iterate over the close queue and remove the windows
        closeQueue.forEach {
            if (it.parent == null || it.parent == this) {
                floatingWindows.remove(it)
            } else {
                it.parent.removeFloatingWindow(it)
            }
        }
    }

    /**
     * Renders the docked windows.
     *
     * This method iterates over the docked windows and calls the `render` method
     * of each window in order to render them.
     *
     * @since 1.0.0
     */
    protected open fun render() {
        dockedWindows.forEach { (_, window) ->
            window.render()
        }
        floatingWindows.forEach { window ->
            window.render()
        }
    }

    /**
     * Rebuilds the dock by setting `dirty` to false to avoid double rebuilds.
     */
    protected open fun rebuildDock() {
        hasBuilt = true
        val outId = ImInt()
        Split.Direction.entries.filter { it != Split.Direction.CENTER }.forEach {
            val size = dockedWindows[it]?.split?.size ?: it.defaultSize

            dockspaceIds[it] = imgui.internal.ImGui.dockBuilderSplitNode(
                this.dockMain.get(),
                it.dir,
                size,
                outId,
                this.dockMain
            )
            outputIds[it] = outId.get()
        }
        dockspaceIds[Split.Direction.CENTER] = dockMain.get()
        outputIds[Split.Direction.CENTER] = dockMain.get()

        dockedWindows.forEach { (direction, window) ->
            val outputId = outputIds[direction]!!
            window.dockMainId = ImInt(outputId)
            dockspaceIds[direction]?.let { imgui.internal.ImGui.dockBuilderDockWindow(window.name, it) }
        }
        dockedWindows.values.forEach {
            it.rebuildDock()
        }

        dirty = false
    }


}