package bpm.common.memory


/**
 * Represents a resizable buffer that can read and write different data types.
 * The buffer has a limit which determines its capacity, and a position which represents the current read or write position.
 * The buffer can be flipped to switch between reading and writing operations.
 * The buffer is implemented using an underlying byte array.
 *
 * @property initialCapacity The initial capacity of the buffer. Defaults to 256.
 * @constructor Creates an instance of [BufferedArray] with the specified initial capacity.
 * @constructor Creates an instance of [BufferedArray] with the specified byte array. The buffer size will be set to the size of the byte array.
 */
class BufferedArray(initialCapacity: Int = 256) : Buffer {

    constructor(bytes: ByteArray) : this(bytes.size) {
        ensureCapacity(bytes.size)
        bytes.copyInto(array, 0, 0, bytes.size)
        position = bytes.size
    }

    private var limit: Int = initialCapacity
    var array: ByteArray = ByteArray(limit)
        private set
    override var position: Int = 0
        private set
    private var flipped = false

    /**
     * Writes the specified array of bytes to the output stream.
     *
     * @param bytes the array of bytes to be written.
     */
    override fun writeBytes(bytes: ByteArray, length: Int) {
        ensureCapacity(bytes.size)
        writeInt(length)
        bytes.forEach(::writeByte)
    }

    /**
     * Reads a sequence of bytes from the input source.
     *
     * @return a byte array containing the read bytes.
     */
    override fun readBytes(): ByteArray {
        val length = readInt()
        val bytes = ByteArray(length)
        for (i in 0 until length)
            bytes[i] = readByte()
        return bytes
    }
    /**
     * Resizes the byte array to the specified capacity.
     *
     * @param newCapacity the new capacity of the byte array
     */
    private fun resize(newCapacity: Int) {
        val newByteArray = ByteArray(newCapacity)
        array.copyInto(newByteArray, 0, 0, position)
        array = newByteArray
        limit = newCapacity
    }

    /**
     * Ensures that the capacity of the data structure is sufficient to accommodate
     * the specified additional number of bytes.
     *
     * @param additionalBytes the additional number of bytes to accommodate
     */
    override fun ensureCapacity(additionalBytes: Int) {
        if (position + additionalBytes > limit) {
            resize((limit + additionalBytes) * 2)
        }
    }


    /**
     * Writes an integer value to the specified output.
     *
     * @param value The integer value to write.
     */
    override fun writeInt(value: Int) {
        ensureCapacity(4)
        array[position++] = (value shr 24).toByte()
        array[position++] = (value shr 16).toByte()
        array[position++] = (value shr 8).toByte()
        array[position++] = value.toByte()
    }
    /**
     * Reads an integer from the user.
     *
     * @return The integer entered by the user.
     */
    override fun readInt(): Int {
        if (position + 4 > limit) {
            throw IndexOutOfBoundsException("Read past buffer end")
        }
        return (array[position++].toInt() and 0xFF shl 24 or
                (array[position++].toInt() and 0xFF shl 16) or
                (array[position++].toInt() and 0xFF shl 8) or
                (array[position++].toInt() and 0xFF))
    }

    /**
     * Writes a floating point value to the output stream.
     *
     * @param value the float value to write
     */
    override fun writeFloat(value: Float) {
        ensureCapacity(4)
        val bits = java.lang.Float.floatToIntBits(value)
        array[position++] = (bits shr 24).toByte()
        array[position++] = (bits shr 16).toByte()
        array[position++] = (bits shr 8).toByte()
        array[position++] = bits.toByte()
    }
    /**
     * Reads a float value from the input source.
     *
     * @return The float value read from the input source.
     */
    override fun readFloat(): Float {
        if (position + 4 > limit) {
            throw IndexOutOfBoundsException("Read past buffer end")
        }
        return java.lang.Float.intBitsToFloat(
            (array[position++].toInt() and 0xFF shl 24 or
                    (array[position++].toInt() and 0xFF shl 16) or
                    (array[position++].toInt() and 0xFF shl 8) or
                    (array[position++].toInt() and 0xFF))
        )
    }

    /**
     * Writes the specified double value to a destination.
     *
     * @param value the double value to write
     */
    override fun writeDouble(value: Double) {
        ensureCapacity(8)
        val bits = java.lang.Double.doubleToLongBits(value)
        array[position++] = (bits shr 56).toByte()
        array[position++] = (bits shr 48).toByte()
        array[position++] = (bits shr 40).toByte()
        array[position++] = (bits shr 32).toByte()
        array[position++] = (bits shr 24).toByte()
        array[position++] = (bits shr 16).toByte()
        array[position++] = (bits shr 8).toByte()
        array[position++] = bits.toByte()
    }

    /**
     * Read a double value from the input source.
     *
     * @return the double value read from the input source.
     */
    override fun readDouble(): Double {
        if (position + 8 > limit) {
            throw IndexOutOfBoundsException("Read past buffer end")
        }
        return java.lang.Double.longBitsToDouble(
            (array[position++].toLong() and 0xFF shl 56 or
                    (array[position++].toLong() and 0xFF shl 48) or
                    (array[position++].toLong() and 0xFF shl 40) or
                    (array[position++].toLong() and 0xFF shl 32) or
                    (array[position++].toLong() and 0xFF shl 24) or
                    (array[position++].toLong() and 0xFF shl 16) or
                    (array[position++].toLong() and 0xFF shl 8) or
                    (array[position++].toLong() and 0xFF))
        )
    }
    /**
     * Writes a byte value to the specified destination.
     *
     * @param value the byte value to be written
     */
    override fun writeByte(value: Byte) {
        ensureCapacity(1)
        array[position++] = value
    }
    /**
     * Reads a single byte of data from the input source.
     *
     * @return the byte read from the input source
     */
    override fun readByte(): Byte {
        if (position < limit) {
            return array[position++]
        } else {
            throw IndexOutOfBoundsException("Read past buffer end")
        }
    }
    /**
     * Writes a character value to the output.
     *
     * @param value the character value to be written
     */
    override fun writeChar(value: Char) {
        writeByte(value.code.toByte())
    }

    /**
     * Retrieves and returns a single character from an input source.
     *
     * @return The character read from the input source.
     */
    override fun readChar(): Char {
        return readByte().toInt().toChar()
    }

    /**
     * Writes a 16-bit short value to a destination.
     *
     * @param value The short value to be written.
     */
    override fun writeShort(value: Short) {
        ensureCapacity(2)
        array[position++] = (value.toInt() shr 8).toByte()
        array[position++] = value.toByte()
    }
    /**
     * Reads a short value from an input source.
     *
     * @return The short value read from the input source.
     */
    override fun readShort(): Short {
        if (position + 2 > limit) {
            throw IndexOutOfBoundsException("Read past buffer end")
        }
        return ((array[position++].toInt() and 0xFF shl 8) or
                (array[position++].toInt() and 0xFF)).toShort()
    }
    /**
     * Writes a long value to the specified output.
     *
     * @param value the long value to be written.
     */
    override fun writeLong(value: Long) {
        ensureCapacity(8)
        array[position++] = (value shr 56).toByte()
        array[position++] = (value  shr 48).toByte()
        array[position++] = (value shr 40).toByte()
        array[position++] = (value  shr 32).toByte()
        array[position++] = (value  shr 24).toByte()
        array[position++] = (value  shr 16).toByte()
        array[position++] = (value  shr 8).toByte()
        array[position++] = value.toByte()
    }
    /**
     * Reads a long value from the user input.
     *
     * @return The long value entered by the user.
     */
    override fun readLong(): Long {
        if (position + 8 > limit) {
            throw IndexOutOfBoundsException("Read past buffer end")
        }
        return ((array[position++].toLong() and 0xFF shl 56) or
                (array[position++].toLong() and 0xFF shl 48) or
                (array[position++].toLong() and 0xFF shl 40) or
                (array[position++].toLong() and 0xFF shl 32) or
                (array[position++].toLong() and 0xFF shl 24) or
                (array[position++].toLong() and 0xFF shl 16) or
                (array[position++].toLong() and 0xFF shl 8) or
                (array[position++].toLong() and 0xFF))
    }
    /**
     * Writes a string value to the specified destination.
     *
     * @param value the string value to be written
     */
    override fun writeString(value: String) {
        ensureCapacity(value.length + 4)
        writeInt(value.length)
        value.encodeToByteArray().forEach(::writeByte)
    }
    /**
     * Reads a string input from the user.
     *
     * @return the string input provided by the user.
     */
    override fun readString(): String {
        val length = readInt()
        val bytes = ByteArray(length)
        for (i in 0 until length)
            bytes[i] = readByte()
        return bytes.decodeToString()
    }

    /**
     * Writes a boolean value to the output.
     *
     * @param value the boolean value to be written
     */
    override fun writeBoolean(value: Boolean) {
        writeByte(if (value) 1 else 0)
    }

    /**
     * Reads a boolean value from the input source.
     *
     * @return the boolean value read from the input source.
     */
    override fun readBoolean(): Boolean {
        return readByte() == 1.toByte()
    }

    /**
     * True when this buffer has been flipped for reading.
     */
    override fun isReadable(): Boolean = flipped

    /**
     * Flips the buffer for reading.
     */
    override fun flip() {
        if (!flipped) {
            flipped = true
            position = 0
        } else {
            throw IllegalStateException("Buffer already flipped")
        }
    }

    /**
     * Checks equality of this object with the specified object. Checks against the value of the buffer array
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BufferedArray) return false

        if (limit != other.limit) return false
        if (!array.contentEquals(other.array)) return false
        if (position != other.position) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = limit
        result = 31 * result + array.contentHashCode()
        result = 31 * result + position
        return result
    }
    /**
     * Intended to be called after you are done writing to the buffer.
     * Should return the byte array with the correct size.
     */
    override fun finish(): ByteArray {
        if (position == limit || position == 0) return array
        if (flipped) throw IllegalStateException("Buffer already flipped")
        flipped = true
        return array.copyOfRange(0, position)
    }
    /**
     * Advances position by the specified number of bytes.
     *
     * @param bytes The number of bytes to advance.
     * @return The advanced integer value.
     */
    override fun advance(bytes: Int) {
        position += bytes
    }
    /**
     * Moves to the specified position.
     *
     * @param position The position to move to.
     */
    override fun moved(position: Int) {
        this.position = position
    }
}