import kotlinx.cinterop.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import platform.iconv.iconv
import platform.iconv.iconv_open
import kotlin.system.exitProcess

const val CHARSET = "GBK"
val BOM_UTF8 = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: json2excel /path/to/someJsonFile")
        exitProcess(status = 1)
    }
    val rows: List<Map<String, JsonElement>> = File(args.first())
        .readBytes()
        .decodeToString(charset = CHARSET)
        .decodeJson()
    assert(rows.isNotEmpty())

    File("${args.first()}.csv")
        .writer()
        .use { writer ->
            if (Platform.osFamily == OsFamily.WINDOWS) {
                writer.write(BOM_UTF8)
            }
            val columnNames = rows.first().keys
            writer.writeLine(columnNames.joinToString(separator = ",") { it.escapeCsv() })
            rows.forEach { row ->
                columnNames
                    .map { row[it].asString() }
                    .joinToString(separator = ",") { it.escapeCsv() }
                    .let { writer.writeLine(it) }
            }
        }
}

inline fun <reified T> String.decodeJson(): T = Json.decodeFromString(this)

fun JsonElement?.asString(): String = when (this) {
    null -> "null"
    is JsonPrimitive -> content
    else -> toString()
}

fun String.escapeCsv() = "\"${replace("\"", "\"\"")}\""

fun ByteArray.decodeToString(charset: String) = memScoped {
    val conv = iconv_open(if (Platform.isLittleEndian) "UTF-16LE" else "UTF-16BE", charset)
    val pInBuf = allocPointerTo<ByteVar>().apply {
        value = toCValues().ptr
    }
    val pInBufLen = alloc<ULongVar>().apply {
        value = size.toULong()
    }
    val outBufLen = size * 3
    val outBuf = allocArray<ByteVar>(outBufLen)
    val pOutBuf = allocPointerTo<ByteVar>().apply {
        value = outBuf
    }
    val pOutBufLen = alloc<ULongVar>().apply {
        value = outBufLen.toULong()
    }
    iconv(conv, pInBuf.ptr, pInBufLen.ptr, pOutBuf.ptr, pOutBufLen.ptr)
    outBuf.reinterpret<ShortVar>().toKString()
}
