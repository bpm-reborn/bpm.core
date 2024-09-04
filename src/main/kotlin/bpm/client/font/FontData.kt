package bpm.client.font

data class FontData(val bytes: ByteArray, val fontRange: ShortArray? = null, var merge: Boolean = false) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FontData

        if (!bytes.contentEquals(other.bytes)) return false
        if (fontRange != null) {
            if (other.fontRange == null) return false
            if (!fontRange.contentEquals(other.fontRange)) return false
        } else if (other.fontRange != null) return false
        return true
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + (fontRange?.hashCode() ?: 0)
        return result
    }


}