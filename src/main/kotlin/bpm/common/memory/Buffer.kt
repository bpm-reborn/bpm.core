package bpm.common.memory


import org.joml.*
import java.util.*

/**
 * The Buffer interface represents a buffer that can be used for reading and writing various data types.
 */
interface Buffer {

    /**
     * Represents the position in a sequence or collection.
     *
     * @property position The numerical value of the position.
     */
    val position: Int
    /**
     * Ensures that the capacity of the buffer is at least the specified additional number of bytes.
     *
     * @param additionalBytes the number of additional bytes needed to be accommodated in the buffer
     */
    fun ensureCapacity(additionalBytes: Int)
    /**
     * Writes the specified array of bytes to the output stream.
     *
     * @param bytes the array of bytes to be written.
     */
    fun writeBytes(bytes: ByteArray, length: Int = bytes.size)

    /**
     * Writes the specified Enum value as a string.
     *
     * @param value The Enum value to be written.
     */
    fun writeEnum(value: Enum<*>) {
        writeString(value.name)
    }

    /**
     * Reads a string from user input and returns the corresponding enum value from the given enum class.
     *
     * @param enumClass the enum class to read the value from
     * @return the enum value
     */
    fun <T : Enum<T>> readEnum(enumClass: Class<T>): T {
        val name = readString()
        return enumClass.enumConstants.first { it.name == name }
    }

    /**
     * Represents a variable `bytes` of type `ByteArray`.
     *
     * This variable provides getters and setters to read and write bytes.
     *
     * @property bytes The byte array value.
     * @get Returns the byte array by calling the `readBytes()` function.
     * @set Sets the byte array by calling the `writeBytes()` function.
     */
    var bytes: ByteArray
        get() = readBytes()
        set(value) = writeBytes(value)

    /**
     * Reads a sequence of bytes from the input source.
     *
     * @return a byte array containing the read bytes.
     */
    fun readBytes(): ByteArray

    /**
     * Writes an integer value to the specified output.
     *
     * @param value The integer value to write.
     */
    fun writeInt(value: Int)

    /**
     * The [int] variable represents an integer value.
     *
     * It provides a custom getter and setter that interact with external input and output functions.
     * The getter reads an integer value using the `readInt` function, while the setter writes an integer value
     * using the `writeInt` function.
     *
     * @property int The integer value represented by this [int] variable.
     * @getter Returns the integer value read from input.
     * @setter Sets the given integer value to output.
     */
    var int: Int
        get() = readInt()
        set(value) = writeInt(value)

    /**
     * Reads an integer from the user.
     *
     * @return The integer entered by the user.
     */
    fun readInt(): Int

    /**
     * Writes a floating point value to the output stream.
     *
     * @param value the float value to write
     */
    fun writeFloat(value: Float)

    /**
     * Represents a variable of type float.
     *
     * This variable provides a getter and a setter that allow reading and writing
     * float values. The getter reads a float value using the `readFloat()` function,
     * while the setter writes a float value using the `writeFloat(value: Float)`
     * function.
     *
     * @property float The float value stored in this variable.
     * @getter get() Returns the stored float value by calling `readFloat()`.
     * @setter set(value) Writes the specified float value by calling `writeFloat(value)`.
     */
    var float: Float
        get() = readFloat()
        set(value) = writeFloat(value)

    /**
     * Reads a float value from the input source.
     *
     * @return The float value read from the input source.
     */
    fun readFloat(): Float

    /**
     * Writes the specified double value to a destination.
     *
     * @param value the double value to write
     */
    fun writeDouble(value: Double)
    /**
     * Represents a variable of type [Double].
     *
     * This variable provides a getter and setter for accessing and modifying the value of the variable.
     * The getter reads the value of the variable using the `readDouble` function.
     * The setter writes the value of the variable using the `writeDouble` function.
     */
    var double: Double
        get() = readDouble()
        set(value) = writeDouble(value)

    /**
     * Read a double value from the input source.
     *
     * @return the double value read from the input source.
     */
    fun readDouble(): Double

    /**
     * Writes a byte value to the specified destination.
     *
     * @param value the byte value to be written
     */
    fun writeByte(value: Byte)

    /**
     * Represents a mutable byte value.
     *
     * The [byte] property can be read from and assigned to. Reading the property will invoke
     * the [readByte] function to obtain the current value. Assigning a value to the property
     * will invoke the [writeByte] function to write the new value.
     */
    var byte: Byte
        get() = readByte()
        set(value) = writeByte(value)

    /**
     * Reads a single byte of data from the input source.
     *
     * @return the byte read from the input source
     */
    fun readByte(): Byte

    /**
     * Writes a character value to the output.
     *
     * @param value the character value to be written
     */
    fun writeChar(value: Char)

    /**
     * Represents a character value.
     *
     * This variable allows reading and writing character values.
     */
    var char: Char
        get() = readChar()
        set(value) = writeChar(value)

    /**
     * Retrieves and returns a single character from an input source.
     *
     * @return The character read from the input source.
     */
    fun readChar(): Char

    /**
     * Writes a 16-bit short value to a destination.
     *
     * @param value The short value to be written.
     */
    fun writeShort(value: Short)
    /**
     * Represents a short value.
     *
     * @property short The current short value.
     */
    var short: Short
        get() = readShort()
        set(value) = writeShort(value)

    /**
     * Reads a short value from an input source.
     *
     * @return The short value read from the input source.
     */
    fun readShort(): Short
    /**
     * Writes a long value to the specified output.
     *
     * @param value the long value to be written.
     */
    fun writeLong(value: Long)
    /**
     * Property representing a long value.
     *
     * This property provides a get and set accessor for a long value. The get accessor retrieves the value of
     * the long property by calling the `readLong()` function. The set accessor sets the value of the long
     * property by calling the `writeLong(value)` function, where `value` is the new value to be set.
     */
    var long: Long
        get() = readLong()
        set(value) = writeLong(value)

    /**
     * Reads a long value from the user input.
     *
     * @return The long value entered by the user.
     */
    fun readLong(): Long

    /**
     * Writes a string value to the specified destination.
     *
     * @param value the string value to be written
     */
    fun writeString(value: String)

    /**
     * Represents a variable `string` of type `String`.
     *
     * This variable provides a getter and setter which allows reading and writing
     * the string value through the `readString()` and `writeString()` functions
     * respectively.
     *
     * @see readString
     * @see writeString
     */
    var string: String
        get() = readString()
        set(value) = writeString(value)

    /**
     * Reads a string input from the user.
     *
     * @return the string input provided by the user.
     */
    fun readString(): String
    /**
     * Writes a boolean value to the output.
     *
     * @param value the boolean value to be written
     */
    fun writeBoolean(value: Boolean)
    /**
     * Represents a boolean variable.
     *
     * @property boolean The value of the boolean variable.
     */
    var boolean: Boolean
        get() = readBoolean()
        set(value) = writeBoolean(value)

    /**
     * Reads a boolean value from the input source.
     *
     * @return the boolean value read from the input source.
     */
    fun readBoolean(): Boolean
    /**
     * Writes the name of the specified class into the buffer.
     *
     * @param value the class object whose name is to be written
     */
    fun writeClass(value: Class<*>) {
        writeString(value.name)
    }

    /**
     * Reads a class from the buffer.
     *
     * @return the class read from the buffer.
     *
     * @throws ClassNotFoundException if the class cannot be found.
     */
    fun readClass(): Class<*> {
        return Class.forName(readString())
    }

    /**
     * Writes the Vector2f value.
     *
     * @param value The Vector2f value to be written.
     */
    fun writeVector2f(value: Vector2f) {
        writeFloat(value.x)
        writeFloat(value.y)
    }
    /**
     * Reads a Vector2f object.
     *
     * @return The Vector2f object read.
     */
    fun readVector2f(): Vector2f {
        return Vector2f(readFloat(), readFloat())
    }
    /**
     * Writes the Vector3f value to a file or stream.
     *
     * @param value The Vector3f value to be written.
     */
    fun writeVector3f(value: Vector3f) {
        writeFloat(value.x)
        writeFloat(value.y)
        writeFloat(value.z)
    }

    /**
     * Reads a Vector3f from an input source.
     *
     * @return The read Vector3f.
     */
    fun readVector3f(): Vector3f {
        return Vector3f(readFloat(), readFloat(), readFloat())
    }

    /**
     * Writes a Vector4f value to a specified location.
     *
     * @param value The Vector4f value to be written.
     */
    fun writeVector4f(value: Vector4f) {
        writeFloat(value.x)
        writeFloat(value.y)
        writeFloat(value.z)
        writeFloat(value.w)
    }

    /**
     * Reads a Vector4f object.
     *
     * @return a Vector4f object read from the data source.
     */
    fun readVector4f(): Vector4f {
        return Vector4f(readFloat(), readFloat(), readFloat(), readFloat())
    }

    /**
     * Writes the given Vector2i value to a specific output.
     *
     * @param value The Vector2i value to be written.
     */
    fun writeVector2i(value: Vector2i) {
        writeInt(value.x)
        writeInt(value.y)
    }

    /**
     * Reads a Vector2i object from an input source.
     *
     * @return The Vector2i object read from the input source.
     */
    fun readVector2i(): Vector2i {
        return Vector2i(readInt(), readInt())
    }

    /**
     * Writes the given Vector3i value.
     *
     * @param value the Vector3i object to be written
     */
    fun writeVector3i(value: Vector3i) {
        writeInt(value.x)
        writeInt(value.y)
        writeInt(value.z)
    }

    /**
     * Reads a Vector3i object from input source.
     *
     * @return A Vector3i object read from the input source.
     */
    fun readVector3i(): Vector3i {
        return Vector3i(readInt(), readInt(), readInt())
    }

    /**
     * Writes the given 4D integer vector to a specific output.
     *
     * @param value The 4D integer vector to be written.
     */
    fun writeVector4i(value: Vector4i) {
        writeInt(value.x)
        writeInt(value.y)
        writeInt(value.z)
        writeInt(value.w)
    }

    /**
     * Reads a Vector4i from an input source.
     *
     * @return A Vector4i object representing the read values.
     */
    fun readVector4i(): Vector4i {
        return Vector4i(readInt(), readInt(), readInt(), readInt())
    }

    /**
     * Writes the given Vector2d object.
     *
     * @param value the Vector2d object to be written
     */
    fun writeVector2d(value: Vector2d) {
        writeDouble(value.x)
        writeDouble(value.y)
    }

    /**
     * Reads a Vector2d from an input source.
     *
     * @return A Vector2d object representing a 2D vector.
     */
    fun readVector2d(): Vector2d {
        return Vector2d(readDouble(), readDouble())
    }

    /**
     * A method to write a Vector3d object.
     *
     * @param value The Vector3d object to be written.
     */
    fun writeVector3d(value: Vector3d) {
        writeDouble(value.x)
        writeDouble(value.y)
        writeDouble(value.z)
    }
    /**
     * Reads and returns a Vector3d object.
     *
     * @return a Vector3d object representing a three-dimensional vector.
     */
    fun readVector3d(): Vector3d {
        return Vector3d(readDouble(), readDouble(), readDouble())
    }
    /**
     * Writes the given vector value to a destination.
     *
     * @param value The vector to be written.
     */
    fun writeVector4d(value: Vector4d) {
        writeDouble(value.x)
        writeDouble(value.y)
        writeDouble(value.z)
        writeDouble(value.w)
    }
    /**
     * Reads a 4-dimensional vector from a source.
     *
     * @return The read 4-dimensional vector as a Vector4d object.
     */
    fun readVector4d(): Vector4d {
        return Vector4d(readDouble(), readDouble(), readDouble(), readDouble())
    }

    //TODO: we should use the most significant bits first, then the least significant bits instead of the string vlaue
    fun writeUUID(value: UUID) {
        writeLong(value.mostSignificantBits)
        writeLong(value.leastSignificantBits)
    }
    //TODO: we should use the most significant bits first, then the least significant bits instead of the string vlaue
    fun readUUID(): UUID {
        return UUID(readLong(), readLong())
    }

    /**
     * True when this buffer has been flipped for reading.
     */
    fun isReadable(): Boolean

    /**
     * Flips the buffer for reading.
     */
    fun flip()

    /**
     * Finishes the task and returns the result as a byte array.
     *
     * @return The result of the task as a byte array.
     */
    fun finish(): ByteArray
    /**
     * Advances position by the specified number of bytes.
     *
     * @param bytes The number of bytes to advance.
     * @return The advanced integer value.
     */
    fun advance(bytes: Int)

    /**
     * Moves to the specified position.
     *
     * @param position The position to move to.
     */
    fun moved(position: Int)


    companion object {

        /**
         * For now, we only support our byte array buffer implementation.
         *
         * We may add support for direct buffers in the future.
         */
        fun allocate(initialLimit: Int = 512): Buffer = BufferedArray(initialLimit)

        /**
         * Creates a Buffer object from the given byte array. Flips the buffer to make it readable.
         *
         * @param bytes The byte array used to create the Buffer.
         * @return A Buffer object created from the given byte array.
         */
        fun wrap(bytes: ByteArray): Buffer {
            val buffer = BufferedArray(bytes)
            buffer.flip()
            return buffer
        }
    }
}