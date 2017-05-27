package me.patamon.lz

import jdk.nashorn.api.scripting.ScriptObjectMirror
import me.patamon.lz.LZString.decompress
import me.patamon.lz.LZString.decompressFromBase64
import me.patamon.lz.LZString.decompressFromEncodedURIComponent
import me.patamon.lz.LZString.decompressFromUTF16
import me.patamon.lz.LZString.decompressFromUint8Array
import me.patamon.lz.array.UInt8Array
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import javax.script.ScriptEngineManager

/**
 * test in script engine
 */
class _02_ScriptEngineTest {

    // load lz js content
    private val lzStringJs: String = this.javaClass.classLoader
            .getResourceAsStream("lz-string-1.4.4.js").bufferedReader().readText()

    // Nashorn engine (or old Rhino engine)
    private val nashorn = ScriptEngineManager().getEngineByName("nashorn")
    init {
        nashorn.eval(lzStringJs)
    }

    @Test
    fun test() {
        go("Hello World !!!")   // hello world
        go(null)    // null
        go("")      // empty string
        go("aaaaabaaaaacaaaaadaaaaaeaaaaa") // string that repeats
        // go(_01_SimpleTest.generateUTF16Chars())    // all printable UTF-16 characters
        // error above, maybe need to escape
        go(_01_SimpleTest.generateLongString())    // a long string
    }

    @Test
    fun test_undefined() {
        assertThat(null, equalTo(decompress(eval("LZString.compress(undefined)"))))
        assertThat(null, equalTo(decompress(eval("LZString.compressToBase64(undefined)"))))
        assertThat(null, equalTo(decompress(eval("LZString.compressToUTF16(undefined)"))))
        assertThat(null, equalTo(decompress(eval("LZString.compressToEncodedURIComponent(undefined)"))))
        assertThat(null, equalTo(decompress(eval("LZString.compressToUint8Array(undefined)"))))
    }

    private fun go(string: String?) {
        val script = if (string == null) string else "'$string'"
        // raw
        assertThat(string, equalTo(decompress(eval("LZString.compress($script)"))))
        // base 64
        assertThat(string, equalTo(decompressFromBase64(eval("LZString.compressToBase64($script)"))))
        // UTF-16
        assertThat(string, equalTo(decompressFromUTF16(eval("LZString.compressToUTF16($script)"))))
        // URI Encoded
        assertThat(string, equalTo(decompressFromEncodedURIComponent(eval("LZString.compressToEncodedURIComponent($script)"))))
        // uint8array
        assertThat(string, equalTo(decompressFromUint8Array(convertUInt8Array(nashorn.eval("LZString.compressToUint8Array($script)")))))
    }

    // execute script
    private fun eval(script: String): String? {
        val obj = nashorn.eval(script) ?: return null
        return obj.toString()
    }

    private fun convertUInt8Array(obj: Any): UInt8Array {
        obj as ScriptObjectMirror

        // convert to UInt8Array
        val arr = arrayListOf<Short>()
        obj.values.forEach {
            arr.add(it.toString().toShort())
        }
        return UInt8Array(*arr.toShortArray())
    }
}
