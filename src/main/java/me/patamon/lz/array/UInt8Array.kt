package me.patamon.lz.array

import java.io.Serializable
import java.nio.ByteBuffer

/**
 * The Uint8Array typed array represents an array of 8-bit unsigned integers
 */
open class UInt8Array(buffer: ByteBuffer) : TypedArray(buffer), Serializable {

    constructor(length: Int): this(ByteBuffer.allocateDirect(length))

    constructor(vararg elements: Short): this(ByteBuffer.allocateDirect(elements.size)) {
       elements.forEachIndexed { index, element ->
           this[index] = element
       }
    }

    /**
     * Returns the 8-bit unsigned integer at the given index.
     *
     * @param index The index at which to return the value.
     * *
     * @return The 8-bit unsigned integer at the index.
     */
    operator fun get(index: Int): Short {
        return (0xFF and buffer.get(index).toInt()).toShort()
    }

    /**
     * Puts a 8-bit unsigned integer at a particular index.
     *
     * @param index The index at which to place the value.
     * *
     * @param value The 8-bit unsigned integer to put into buffer.
     */
    fun put(index: Int, value: Short) {
        buffer.put(index, (0x00FF and value.toInt()).toByte())
    }
    operator fun set(index: Int, value: Short) {
        this.put(index, value)
    }

    override fun length(): Int {
        return buffer.limit()
    }

    override fun getType(): Int {
        return TypedArrayValue.UNSIGNED_INT_8_ARRAY
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("[")
        for (i in 0 until this.length()) {
            sb.append(this[i])
            if (i != this.length() - 1) {
                sb.append(",")
            }
        }
        sb.append("]")
        return sb.toString()
    }
}