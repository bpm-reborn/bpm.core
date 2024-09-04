package bpm.common.utils

import org.joml.*
import kotlin.math.*

/**
 * Converts a Vector4f object to ImColor representation.
 *
 * @return The ImColor value as an Int.
 */

/**
 * Converts a Vector4f to a Vector4i representing color values.
 *
 * @return The Vector4i with color values obtained by multiplying each component of the Vector4f with 255 and converting it to an integer.
 */
fun Vector4f.toVecColor(): Vector4i =
    Vector4i((this.x * 255).toInt(), (this.y * 255).toInt(), (this.z * 255).toInt(), (this.w * 255).toInt())

/**
 * Returns the color vector as an integer vector representation.
 *
 * @return The color vector as an integer vector.
 */
val Vector4f.vecColor: Vector4i
    get() = this.toVecColor()



/**
 * Converts a Vector3f to a Vector3i representing the color values.
 *
 * @return The converted Vector3i.
 */
fun Vector3f.toVecColor(): Vector3i =
    Vector3i((this.x * 255).toInt(), (this.y * 255).toInt(), (this.z * 255).toInt())

/**
 * Retrieves the `vecColor` property of a Vector3f object.
 *
 * @return The vecColor property as a Vector3i object.
 * @see Vector3f
 * @see Vector3i
 */
val Vector3f.vecColor: Vector3i
    get() = this.toVecColor()



/**
 * Converts a [Vector3i] representing RGB color values to a [Vector3f] representing color values in the range of [0, 1].
 *
 * @return The converted RGB color values as a [Vector3f].
 */
fun Vector3i.toVecColor(): Vector3f =
    Vector3f((this.x / 255f), (this.y / 255f), (this.z / 255f))

/**
 * Converts a Vector3i to a Vector3f representing color values.
 *
 * @return The Vector3f with color values obtained by dividing each component of the Vector3i by 255.
 */
val Vector3i.vecColor: Vector3f
    get() = this.toVecColor()




/**
 * Converts a Vector4i to a Vector4f representing color values.
 *
 * @return A Vector4f object with color values scaled between 0 and 1.
 */
fun Vector4i.toVecColor(): Vector4f =
    Vector4f((this.x / 255f), (this.y / 255f), (this.z / 255f), (this.w / 255f))

/**
 * Converts a Vector4i to a Vector4f representing color values.
 *
 * @return The Vector4f with color values obtained by dividing each component of the Vector4i by 255.
 */
val Vector4i.vecColor: Vector4f
    get() = this.toVecColor()



/**
 * Performs the bitwise OR operation between the current integer and one or more other integers.
 *
 * @param ints One or more integers to perform the OR operation with the current integer.
 * @return The result of the OR operation between the current integer and the other integers.
 */
fun Int.orEquals(vararg ints: Int): Int {
    var out = this
    for (element in ints)
        out = out or element
    return out
}
/**
 * Computes the floating-point remainder of `a` divided by `b`.
 *
 * @param a The dividend.
 * @param b The divisor.
 * @return The computed floating-point remainder of `a` divided by `b`.
 */
fun fmodf(a: Float, b: Float): Float {
    val result = floor((a / b).toDouble()).toInt()
    return a - result * b
}



/**
 * Expands the vector by adding or subtracting the specified amount to its components.
 *
 * @param amount the amount by which to expand the vector. A positive value expands the vector,
 *               while a negative value contracts the vector.
 * @return the expanded vector.
 */
fun Vector4f.expand(amount: Float): Vector4f {
    this.x -= amount // minX
    this.y -= amount // minY
    this.z += amount // maxX
    this.w += amount // maxY
    return this
}

/**
 * Returns whether the Vector2i is a zero vector.
 *
 * @return True if x and y components of the Vector2i are both zero, false otherwise.
 */
val Vector2i.isZero: Boolean
    get() = this.x == 0 && this.y == 0
/**
 * Returns true if all components of this Vector3i are zero.
 *
 * @return true if all components are zero, false otherwise.
 */
val Vector3i.isZero: Boolean
    get() = this.x == 0 && this.y == 0 && this.z == 0
/**
 * Returns a boolean indicating whether the Vector4i instance is zero.
 *
 * @return true if all components (x, y, z, and w) of the vector are zero, false otherwise.
 */
val Vector4i.isZero: Boolean
    get() = this.x == 0 && this.y == 0 && this.z == 0 && this.w == 0
/**
 * Returns whether this Vector2f is zero.
 *
 * @return true if both x and y components are equal to zero, false otherwise.
 */
val Vector2f.isZero: Boolean
    get() = this.x == 0f && this.y == 0f
/**
 * Returns whether the vector is a zero vector.
 *
 * @return `true` if the vector is a zero vector, `false` otherwise.
 */
val Vector3f.isZero: Boolean
    get() = this.x == 0f && this.y == 0f && this.z == 0f
/**
 * Returns whether the Vector4f object is zero.
 *
 * @return True if all components (x, y, z, w) of the Vector4f object are equal to 0, false otherwise.
 */
val Vector4f.isZero: Boolean
    get() = this.x == 0f && this.y == 0f && this.z == 0f && this.w == 0f

/**
 * Expands the vector by adding or subtracting the specified amount to each component.
 *
 * @param amount The amount by which to expand the vector. Positive values will increase the components, while negative values will decrease them.
 * @return A new Vector4f instance that is expanded by the specified amount.
 */
fun Vector4f.expanded(amount: Float): Vector4f {
    return Vector4f(this.x - amount, this.y - amount, this.z + amount, this.w + amount)
}

/**
 * Checks if this Vector4f intersects with the given Vector4f.
 *
 * @param other the Vector4f to check intersection with
 * @return true if the rectangles intersect, false otherwise
 */
fun Vector4f.intersects(other: Vector4f): Boolean {
    // Check if one rectangle is on the left side of the other
    if (this.x >= other.z || other.x >= this.z) {
        return false
    }
    // Check if one rectangle is above the other
    if (this.y >= other.w || other.y >= this.w) {
        return false
    }
    // If the rectangles are neither on the left side nor above each other, they are intersecting
    return true
}


/**
 * Retrieves the XY components of a 4D vector as a 2D vector.
 *
 * @return The XY components as a [Vector2f] object.
 */
val Vector4f.xy: Vector2f
    get() = Vector2f(this.x, this.y)
/**
 * Returns a new 2-dimensional vector containing the `z` and `w` components of this 4-dimensional vector.
 *
 * @return A new 2-dimensional vector containing the `z` and `w` components of this 4-dimensional vector.
 */
val Vector4f.zw: Vector2f
    get() = Vector2f(this.z, this.w)
/**
 * Retrieves the x and z components of a 4-dimensional vector as a 2-dimensional vector.
 *
 * @return The x and z components of the 4-dimensional vector as a 2-dimensional vector.
 */
val Vector4f.xz: Vector2f
    get() = Vector2f(this.x, this.z)
/**
 * Retrieves the [Vector2f] representing the `yw` components of this [Vector4f].
 *
 * @return [Vector2f] containing the `yw` components of this [Vector4f]
 */
val Vector4f.yw: Vector2f
    get() = Vector2f(this.y, this.w)
/**
 * Returns a 2D vector with `x` and `w` components from a 4D vector.
 *
 * @receiver The 4D vector.
 * @return A new 2D vector with `x` and `w` components.
 */
val Vector4f.xw: Vector2f
    get() = Vector2f(this.x, this.w)
/**
 * Returns a new Vector2f containing the y and z components of this Vector4f.
 *
 * @return The y and z components of this Vector4f as a new Vector2f.
 */
val Vector4f.yz: Vector2f
    get() = Vector2f(this.y, this.z)
/**
 * Retrieves the `wx` component of the four-dimensional vector as a two-dimensional vector.
 *
 * @return The `wx` component as a two-dimensional vector.
 */
val Vector4f.wx: Vector2f
    get() = Vector2f(this.w, this.x)
/**
 * Returns a new Vector2f instance representing the wy components of this Vector4f.
 *
 * @return A new Vector2f instance with the wy components of this Vector4f.
 */
val Vector4f.wy: Vector2f
    get() = Vector2f(this.w, this.y)
/**
 * Retrieves the zx component of the Vector4f as a Vector2f.
 *
 * @return The zx component as a Vector2f.
 */
val Vector4f.zx: Vector2f
    get() = Vector2f(this.z, this.x)
/**
 * Returns a 2D vector composed of the z and y components of this 4D vector.
 *
 * @return A new [Vector2f] object representing the zy components of this vector.
 */
val Vector4f.zy: Vector2f
    get() = Vector2f(this.z, this.y)

/**
 * Converts a Vector4i to a Vector3f color.
 *
 * @return The Vector3f color representation of the Vector4i, where each component is normalized between 0 and 1.
 */
fun Vector4i.toColor(): Vector3f {
    return Vector3f(this.x / 255f, this.y / 255f, this.z / 255f)
}


/**
 * Determines if the specified coordinates (x, y) are contained within the limits of the vector.
 *
 * @param x The x-coordinate to check.
 * @param y The y-coordinate to check.
 * @return true if the specified coordinates are contained within the vector's limits, false otherwise.
 */
 fun Vector4f.contains(x: Float, y: Float): Boolean {
    return x in this.x..this.z && y in this.y..this.w
}