package bpm.client.runtime

import org.joml.Vector2f
import org.lwjgl.glfw.*
import org.lwjgl.glfw.GLFW.glfwGetFramebufferSize
import org.lwjgl.glfw.GLFW.glfwGetKey

object Platform {

    /**
     * Represents the handle of a window.
     *
     * This variable stores the handle of a window as a `Long` value. The window handle is used
     * to uniquely identify a window in a graphical user interface (GUI).
     *
     * Window handles are typically obtained from the operating system or GUI framework when a
     * window is created. They are used for various purposes, such as moving, resizing, or closing
     * the window programmatically.
     *
     * The `windowHandle` variable has a private setter, meaning that it can only be modified
     * by the enclosing class. This ensures that the handle is set correctly and prevents external
     * code from erroneously modifying it.
     *
     * @see Window
     * @see GUIFramework
     */
    var windowHandle: Long = 0
        private set

    fun install(window: Long) {
        windowHandle = window
    }


    /**
     * Represents the width of a frame.
     *
     * This variable holds the width of a frame. It is used to determine the size of a graphical frame in a user interface.
     * The value of this variable can be set privately.
     *
     * @property frameWidth The width of the frame.
     */
    var frameWidth = 0
        private set
    /**
     * Represents the height of a frame.
     *
     * The frame height is used to determine the vertical size of a frame in a graphical user interface.
     */
    var frameHeight = 0
        private set

    /**
     * Represents the size of a frame.
     *
     * @property frameWidth The width of the frame.
     * @property frameHeight The height of the frame.
     */
    val frameSize: Vector2f
        get() {
            val buffer = IntArray(2)
            glfwGetFramebufferSize(windowHandle, buffer, buffer)
            frameWidth = buffer[0]
            frameHeight = buffer[1]
            return Vector2f(frameWidth.toFloat(), frameHeight.toFloat())
        }

    /**
     * Checks if a specific key is currently being pressed.
     *
     * @param key the key code to check
     * @return true if the key is being pressed, false otherwise
     */
    fun isKeyDown(key: ClientRuntime.Key): Boolean = /*keyStateMap[key] ?: false*/
        glfwGetKey(windowHandle, key.value) == GLFW.GLFW_PRESS


    /**
     * Checks whether a given key is released.
     *
     * @param key the integer value representing the key to check
     * @return true if the key is released, false otherwise
     */
    fun isKeyUp(key: ClientRuntime.Key): Boolean = /*!isKeyDown(key)*/
        glfwGetKey(windowHandle, key.value) == GLFW.GLFW_RELEASE

    /**
     * Checks if the specified mouse button is currently pressed.
     *
     * @param button the button code of the mouse button to be checked.
     * @return true if the specified mouse button is currently pressed, false otherwise.
     */
    fun isMouseDown(button: ClientRuntime.MouseButton): Boolean = /*mouseButtonStateMap[button] ?: false*/
        GLFW.glfwGetMouseButton(windowHandle, button.value) == GLFW.GLFW_PRESS

    /**
     * Checks if the mouse button is currently released.
     *
     * @param button The mouse button to check for release.
     * @return `true` if the mouse button is released, `false` otherwise.
     */
    fun isMouseUp(button: ClientRuntime.MouseButton): Boolean =
        GLFW.glfwGetMouseButton(windowHandle, button.value) == GLFW.GLFW_RELEASE


    fun setMouseIcon(icon: MouseIcon) {
//        GLFW.glfwSetCursor(windowHandle, GLFW.GLFW_HAND_CURSOR)
    }

    fun setMousePosition(x: Double, y: Double) {
        GLFW.glfwSetCursorPos(windowHandle, x, y)
    }

    fun getMousePosition(): Vector2f {
        val x = DoubleArray(1)
        val y = DoubleArray(1)
        GLFW.glfwGetCursorPos(windowHandle, x, y)
        return Vector2f(x[0].toFloat(), y[0].toFloat())
    }


    enum class MouseIcon(val cursor: Long) {
        ARROW(GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR)),
        CROSSHAIR(GLFW.glfwCreateStandardCursor(GLFW.GLFW_CROSSHAIR_CURSOR)),
        HAND(GLFW.glfwCreateStandardCursor(GLFW.GLFW_POINTING_HAND_CURSOR)),
        IBEAM(GLFW.glfwCreateStandardCursor(GLFW.GLFW_IBEAM_CURSOR)),
        HRESIZE(GLFW.glfwCreateStandardCursor(GLFW.GLFW_HRESIZE_CURSOR)),
        VRESIZE(GLFW.glfwCreateStandardCursor(GLFW.GLFW_VRESIZE_CURSOR)),
    }
}