package me.patamon.lz.array

import java.nio.ByteBuffer


/**
 * abstract typed array
 *
 * refer to https://github.com/eclipsesource/J2V8/blob/master/src/main/java/com/eclipsesource/v8/utils/typedarrays/TypedArray.java
 */
abstract class TypedArray {

    protected val buffer: ByteBuffer

    protected constructor(buffer: ByteBuffer) {
        if (!buffer.isDirect) {
            throw IllegalArgumentException("ByteBuffer must be a allocated as a direct ByteBuffer")
        }
        if ((buffer.limit() % getStructureSize(this.getType())) != 0) {
            throw IllegalArgumentException("ByteBuffer must be a allocated as a direct ByteBuffer")
        }
        this.buffer = buffer
    }

    /**
     * Return the size of this view. The size of the view is determined by the size
     * of the buffer, and the size of the data projected onto it. For example, for a
     * buffer size of 8, and a view representing 16bit integers, the size would be 4.
     *
     * @return The size of this view
     */
    abstract fun length(): Int

    /**
     * Returns the 'Type' of this TypedArray using one of the constants defined in V8Value.
     *
     * @return The 'Type' of this typed array.
     */
    abstract fun getType(): Int


    /**
     * Computes the size of the structures required for each TypedArray variation.
     *
     * @param type The type of the TypeArray
     * @return The size of the structures required
     */
    private fun getStructureSize(type: Int): Int = when (type) {
        TypedArrayValue.FLOAT_64_ARRAY -> 8
        TypedArrayValue.INT_32_ARRAY,
        TypedArrayValue.UNSIGNED_INT_32_ARRAY,
        TypedArrayValue.FLOAT_32_ARRAY -> 4
        TypedArrayValue.UNSIGNED_INT_16_ARRAY,
        TypedArrayValue.INT_16_ARRAY -> 2
        TypedArrayValue.INT_8_ARRAY,
        TypedArrayValue.UNSIGNED_INT_8_ARRAY,
        TypedArrayValue.UNSIGNED_INT_8_CLAMPED_ARRAY -> 1
        else -> throw IllegalArgumentException("Cannot create a typed array of the type")
    }

    override fun toString(): String {
        return buffer.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false
        other as TypedArray
        if (buffer != other.buffer) return false
        return true
    }

    override fun hashCode(): Int {
        return this.buffer.hashCode()
    }
}