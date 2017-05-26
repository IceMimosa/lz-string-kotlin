package me.patamon.lz

import me.patamon.lz.array.UInt8Array

/**
 * LZ-based compression algorithm
 */
object LZString {
    private val keyStrBase64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/="
    private val keyStrUriSafe = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+-$"
    private val baseReverseDic = hashMapOf<String, HashMap<Char, Int>>()

    private fun getBaseValue(alphabet: String, character: Char): Char {
        val map = baseReverseDic[alphabet] ?: {
            HashMap<Char, Int>().apply {
                baseReverseDic.put(alphabet, this)
                alphabet.forEachIndexed { index, char ->
                    this.put(char, index)
                }
            }
        }()
        return map[character]!!.toInt().toChar()
    }

    fun compressToBase64(input: String?): String {
        input ?: return ""
        val res = LZString._compress(input, 6, { a ->
            keyStrBase64[a]
        })
        when (res.length % 4) {
        // To produce valid Base64
            0 -> return res
            1 -> return res + "==="
            2 -> return res + "=="
            3 -> return res + "="
            else -> return res// When could this happen ?
        }
    }

    fun decompressFromBase64(input: String?): String? {
        if (input == null) return ""
        if (input == "") return null
        return LZString._decompress(input.length, 32, { index ->
            getBaseValue(keyStrBase64, input[index])
        })
    }

    fun compressToUTF16(input: String?): String {
        input ?: return ""
        return LZString._compress(input, 15, { a ->
            (a + 32).toChar()
        }) + " "
    }

    fun decompressFromUTF16(compressed: String?): String? {
        if (compressed == null) return ""
        if (compressed == "") return null
        return LZString._decompress(compressed.length, 16384, { index ->
            (compressed[index].toInt() - 32).toChar()
        })
    }

    fun compressToUint8Array(input: String?): UInt8Array {
        val compressed = LZString.compress(input)
        val buf = UInt8Array(compressed.length * 2)

        compressed.forEachIndexed { index, current_value ->
            buf[index * 2] = (current_value.toInt() ushr 8).toShort()
            buf[index * 2 + 1] = (current_value.toInt() % 256).toShort()
        }
        return buf
    }

    fun decompressFromUint8Array(buffer: UInt8Array?): String? {
        buffer ?: return ""
        val totalLen = buffer.length() / 2
        val result = CharArray(totalLen)
        var index = 0
        while (index < totalLen) {
            result[index] = (buffer[index * 2] * 256 + buffer[index * 2 + 1]).toChar()
            index++
        }
        return LZString.decompress(result.joinToString(""))
    }

    fun compressToEncodedURIComponent(input: String?): String {
        input ?: return ""
        return LZString._compress(input, 6, { a ->
            keyStrUriSafe[a]
        }) + " "
    }

    fun decompressFromEncodedURIComponent(input: String?): String? {
        if (input == null) return ""
        if (input == "") return null
        val reInput = input.replace(' ', '+')
        return LZString._decompress(reInput.length, 32, { index ->
            getBaseValue(keyStrUriSafe, reInput[index])
        })
    }

    fun compress(uncompressed: String?): String {
        return LZString._compress(uncompressed, 16, { a ->
            a.toChar()
        })
    }

    private fun _compress(uncompressed: String?, bitsPerChar: Int, getCharFromInt: (int: Int) -> Char): String {
        uncompressed ?: return ""

        var i: Int
        var value: Int
        val context_dictionary = hashMapOf<String, Int>()
        val context_dictionaryToCreate = HashSet<String>()
        var context_c: String
        var context_wc: String
        var context_w = ""
        var context_enlargeIn = 2.0 // Compensate for the first entry which should not count
        var context_dictSize = 3
        var context_numBits = 2
        val context_data = ArrayList<Char>(uncompressed.length / 3)
        var context_data_val = 0
        var context_data_position = 0
        var ii: Int = 0

        while (ii < uncompressed.length) {
            context_c = uncompressed[ii].toString()
            if (!context_dictionary.containsKey(context_c)) {
                context_dictionary.put(context_c, context_dictSize++)
                context_dictionaryToCreate.add(context_c)
            }

            context_wc = context_w + context_c
            if (context_dictionary.containsKey(context_wc)) {
                context_w = context_wc
            } else {
                if (context_dictionaryToCreate.contains(context_w)) {
                    if (context_w[0].toInt() < 256) {
                        i = 0
                        while (i < context_numBits) {
                            context_data_val = context_data_val shl 1
                            if (context_data_position == bitsPerChar - 1) {
                                context_data_position = 0
                                context_data.add(getCharFromInt(context_data_val))
                                context_data_val = 0
                            } else {
                                context_data_position++
                            }
                            i++
                        }
                        value = context_w[0].toInt()
                        i = 0
                        while (i < 8) {
                            context_data_val = context_data_val shl 1 or (value and 1)
                            if (context_data_position == bitsPerChar - 1) {
                                context_data_position = 0
                                context_data.add(getCharFromInt(context_data_val))
                                context_data_val = 0
                            } else {
                                context_data_position++
                            }
                            value = value shr 1
                            i++
                        }
                    } else {
                        value = 1
                        i = 0
                        while (i < context_numBits) {
                            context_data_val = context_data_val shl 1 or value
                            if (context_data_position == bitsPerChar - 1) {
                                context_data_position = 0
                                context_data.add(getCharFromInt(context_data_val))
                                context_data_val = 0
                            } else {
                                context_data_position++
                            }
                            value = 0
                            i++
                        }
                        value = context_w[0].toInt()
                        i = 0
                        while (i < 16) {
                            context_data_val = context_data_val shl 1 or (value and 1)
                            if (context_data_position == bitsPerChar - 1) {
                                context_data_position = 0
                                context_data.add(getCharFromInt(context_data_val))
                                context_data_val = 0
                            } else {
                                context_data_position++
                            }
                            value = value shr 1
                            i++
                        }
                    }
                    context_enlargeIn--
                    if (context_enlargeIn == 0.0) {
                        context_enlargeIn = Math.pow(2.0, context_numBits.toDouble())
                        context_numBits++
                    }
                    context_dictionaryToCreate.remove(context_w)
                } else {
                    value = context_dictionary[context_w]!!
                    i = 0
                    while (i < context_numBits) {
                        context_data_val = (context_data_val shl 1) or (value and 1)
                        if (context_data_position == bitsPerChar - 1) {
                            context_data_position = 0
                            context_data.add(getCharFromInt(context_data_val))
                            context_data_val = 0
                        } else {
                            context_data_position++
                        }
                        value = value shr 1
                        i++
                    }

                }
                context_enlargeIn--
                if (context_enlargeIn == 0.0) {
                    context_enlargeIn = Math.pow(2.0, context_numBits.toDouble())
                    context_numBits++
                }
                // Add wc to the dictionary.
                context_dictionary.put(context_wc, context_dictSize++)
                context_w = context_c
            }
            ii += 1
        }

        // Output the code for w.
        if (!context_w.isEmpty()) {
            if (context_dictionaryToCreate.contains(context_w)) {
                if (context_w[0].toInt() < 256) {
                    i = 0
                    while (i < context_numBits) {
                        context_data_val = context_data_val shl 1
                        if (context_data_position == bitsPerChar - 1) {
                            context_data_position = 0
                            context_data.add(getCharFromInt(context_data_val))
                            context_data_val = 0
                        } else {
                            context_data_position++
                        }
                        i++
                    }
                    value = context_w[0].toInt()
                    i = 0
                    while (i < 8) {
                        context_data_val = context_data_val shl 1 or (value and 1)
                        if (context_data_position == bitsPerChar - 1) {
                            context_data_position = 0
                            context_data.add(getCharFromInt(context_data_val))
                            context_data_val = 0
                        } else {
                            context_data_position++
                        }
                        value = value shr 1
                        i++
                    }
                } else {
                    value = 1
                    i = 0
                    while (i < context_numBits) {
                        context_data_val = context_data_val shl 1 or value
                        if (context_data_position == bitsPerChar - 1) {
                            context_data_position = 0
                            context_data.add(getCharFromInt(context_data_val))
                            context_data_val = 0
                        } else {
                            context_data_position++
                        }
                        value = 0
                        i++
                    }
                    value = context_w[0].toInt()
                    i = 0
                    while (i < 16) {
                        context_data_val = context_data_val shl 1 or (value and 1)
                        if (context_data_position == bitsPerChar - 1) {
                            context_data_position = 0
                            context_data.add(getCharFromInt(context_data_val))
                            context_data_val = 0
                        } else {
                            context_data_position++
                        }
                        value = value shr 1
                        i++
                    }
                }
                context_enlargeIn--
                if (context_enlargeIn == 0.0) {
                    context_enlargeIn = Math.pow(2.0, context_numBits.toDouble())
                    context_numBits++
                }
                context_dictionaryToCreate.remove(context_w)
            } else {
                value = context_dictionary[context_w]!!
                i = 0
                while (i < context_numBits) {
                    context_data_val = context_data_val shl 1 or (value and 1)
                    if (context_data_position == bitsPerChar - 1) {
                        context_data_position = 0
                        context_data.add(getCharFromInt(context_data_val))
                        context_data_val = 0
                    } else {
                        context_data_position++
                    }
                    value = value shr 1
                    i++
                }

            }
            context_enlargeIn--
            if (context_enlargeIn == 0.0) {
                context_enlargeIn = Math.pow(2.0, context_numBits.toDouble())
                context_numBits++
            }
        }

        // Mark the end of the stream
        value = 2
        i = 0
        while (i < context_numBits) {
            context_data_val = context_data_val shl 1 or (value and 1)
            if (context_data_position == bitsPerChar - 1) {
                context_data_position = 0
                context_data.add(getCharFromInt(context_data_val))
                context_data_val = 0
            } else {
                context_data_position++
            }
            value = value shr 1
            i++
        }

        // Flush the last char
        while (true) {
            context_data_val = context_data_val shl 1
            if (context_data_position == bitsPerChar - 1) {
                context_data.add(getCharFromInt(context_data_val))
                break
            } else
                context_data_position++
        }
        return context_data.joinToString(separator = "")
    }

    private data class DecData(
            var value: Char = ' ',
            var position: Int = 0,
            var index: Int = 0
    )

    fun decompress(compressed: String?): String? {
        if (compressed == null) return ""
        if (compressed === "") return null
        return LZString._decompress(compressed.length, 32768, { index ->
            compressed[index]
        })
    }

    private fun _decompress(length: Int, resetValue: Int, getNextValue: (int: Int) -> Char): String? {
        val dictionary = ArrayList<String>()
        val next: Int
        var enlargeIn = 4.0
        var dictSize = 4
        var numBits = 3
        var entry: String
        val result = arrayListOf<String>()
        var w: String
        var bits: Int
        var resb: Int
        var maxpower: Int
        var power: Int
        val c: String
        val data = DecData(getNextValue(0), resetValue, 1)

        var i = 0
        while (i < 3) {
            dictionary.add(i, i.toChar().toString())
            i += 1
        }

        bits = 0
        maxpower = Math.pow(2.0, 2.0).toInt()
        power = 1
        while (power != maxpower) {
            resb = data.value.toInt() and data.position
            data.position = data.position shr 1
            if (data.position == 0) {
                data.position = resetValue
                data.value = getNextValue(data.index++)
            }
            bits = bits or (if (resb > 0) 1 else 0) * power
            power = power shl 1
        }

        // next is unused ?
        next = bits
        when (next) {
            0 -> {
                bits = 0
                maxpower = Math.pow(2.0, 8.0).toInt()
                power = 1
                while (power != maxpower) {
                    resb = data.value.toInt() and data.position
                    data.position = data.position shr 1
                    if (data.position == 0) {
                        data.position = resetValue
                        data.value = getNextValue(data.index++)
                    }
                    bits = bits or (if (resb > 0) 1 else 0) * power
                    power = power shl 1
                }
                c = bits.toChar().toString()
            }
            1 -> {
                bits = 0
                maxpower = Math.pow(2.0, 16.0).toInt()
                power = 1
                while (power != maxpower) {
                    resb = data.value.toInt() and data.position
                    data.position = data.position shr 1
                    if (data.position == 0) {
                        data.position = resetValue
                        data.value = getNextValue(data.index++)
                    }
                    bits = bits or (if (resb > 0) 1 else 0) * power
                    power = power shl 1
                }
                c = bits.toChar().toString()
            }
            2 -> return ""
            else -> return ""
        }
        dictionary.add(3, c)
        w = c
        result.add(w)
        while (true) {
            if (data.index > length) {
                return ""
            }

            bits = 0
            maxpower = Math.pow(2.0, numBits.toDouble()).toInt()
            power = 1
            while (power != maxpower) {
                resb = data.value.toInt() and data.position
                data.position = data.position shr 1
                if (data.position == 0) {
                    data.position = resetValue
                    data.value = getNextValue(data.index++)
                }
                bits = bits or (if (resb > 0) 1 else 0) * power
                power = power shl 1
            }
            // very strange here, c above is as char/string, here further is a int, rename "c" in the switch as "cc"
            var cc = bits
            when (cc) {
                0 -> {
                    bits = 0
                    maxpower = Math.pow(2.0, 8.0).toInt()
                    power = 1
                    while (power != maxpower) {
                        resb = data.value.toInt() and data.position
                        data.position = data.position shr 1
                        if (data.position == 0) {
                            data.position = resetValue
                            data.value = getNextValue(data.index++)
                        }
                        bits = bits or (if (resb > 0) 1 else 0) * power
                        power = power shl 1
                    }

                    dictionary.add(dictSize++, bits.toChar().toString())
                    cc = dictSize - 1
                    enlargeIn--
                }
                1 -> {
                    bits = 0
                    maxpower = Math.pow(2.0, 16.0).toInt()
                    power = 1
                    while (power != maxpower) {
                        resb = data.value.toInt() and data.position
                        data.position = data.position shr 1
                        if (data.position == 0) {
                            data.position = resetValue
                            data.value = getNextValue(data.index++)
                        }
                        bits = bits or (if (resb > 0) 1 else 0) * power
                        power = power shl 1
                    }
                    dictionary.add(dictSize++, bits.toChar().toString())
                    cc = dictSize - 1
                    enlargeIn--
                }
                2 -> {
                    val sb = StringBuffer(result.size)
                    for (s in result)
                        sb.append(s)
                    return sb.toString()
                }
            }

            if (enlargeIn == 0.0) {
                enlargeIn = Math.pow(2.0, numBits.toDouble())
                numBits++
            }

            if (cc < dictionary.size && dictionary[cc].isNotEmpty()) {
                entry = dictionary[cc]
            } else {
                if (cc == dictSize) {
                    entry = w + w[0]
                } else {
                    return null
                }
            }
            result.add(entry)

            // Add w+entry[0] to the dictionary.
            dictionary.add(dictSize++, w + entry[0])
            enlargeIn--

            w = entry

            if (enlargeIn == 0.0) {
                enlargeIn = Math.pow(2.0, numBits.toDouble())
                numBits++
            }

        }
    }

    @JvmStatic fun main(args: Array<String>) {
        var input: String
        input = "hello world !!!"
        input = "hello1hello2hello3hello4hello5hello6hello7hello8hello9helloAhelloBhelloChelloDhelloEhelloF"

        println(decompress(compress(input)))
        println(decompressFromBase64(compressToBase64(input)))
        println(decompressFromUTF16(compressToUTF16(input)))
        println(decompressFromEncodedURIComponent(compressToEncodedURIComponent(input)))
        println(decompressFromUint8Array(compressToUint8Array(input)))
    }
}