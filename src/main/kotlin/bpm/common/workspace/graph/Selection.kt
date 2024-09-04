package bpm.common.workspace.graph

import bpm.common.property.*

import org.joml.*
import java.util.*

class Selection(override val properties: PropertyMap = Property.Object()) : PropertySupplier {

    /**
     * A variable representing the state of the "minimized" flag.
     *
     * By default, the flag is set to `false`.
     *
     * @see [PropertySupplier.to]
     */
    val minimized: Boolean by properties to { false }
    /**
     * Represents an identifier.
     *
     * @property id The unique identifier value.
     */
    var id: UUID by properties to { UUID.randomUUID() }

    /**
     * Title variable represents the title of a selection.
     *
     * @property title The title of the selection.
     */
    var title: String by properties to { "Selection" }

    /**
     * Holds the description of a variable.
     *
     * This variable is delegated with a property delegate, which ensures that the description is always a non-null String. If no description is provided, an empty String is used as the default value.
     *
     * @property delegate The delegate for the variable, responsible for providing the value.
     * @return The description of the variable.
     */
    var description: String by properties to { "" }

    /**
     * Represents the starting position, using a 2D vector.
     *
     * The `start` variable is used to define the initial position of an object
     * on a 2D plane. This variable must be accessed using a delegate in order to
     * ensure proper initialization.
     *
     * @property origin The starting position as a 2D vector.
     * @see Vector2f
     */
    var origin: Vector2f by properties to { Vector2f() }

    /**
     * Represents the end position in 2D space.
     *
     * This variable is a Vector2f type and is delegated by the `properties` delegate.
     *
     * @property size The end position in 2D space.
     */
    var size: Vector2f by properties to { Vector2f() }


    /**
     * Represents a color in RGBA format.
     *
     * The color is stored as a vector of four floats representing the Red, Green, Blue and Alpha channels respectively.
     *
     * @property color The value of the color as a Vector4f.
     *
     * @constructor Creates a new Color instance with the default color value of (0.5f, 0.5f, 0.5f, 0.5f).
     * @param properties The delegate used to handle property access and mutation.
     */
    var color: Vector4i by properties to { Vector4i(50, 133, 168, 220) }


}


