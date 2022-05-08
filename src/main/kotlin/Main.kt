import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import java.io.File
import java.nio.charset.Charset
import kotlin.system.exitProcess

val charset: Charset = Charset.forName("GBK")

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: json2excel inputFilename")
        exitProcess(status = 1)
    }
    val rows: List<Map<String, JsonElement>> = File(args.first())
        .readText(charset = charset)
        .jsonParsed()
    assert(rows.isNotEmpty())

    File("${args.first()}.csv")
        .printWriter(charset = charset)
        .use { writer ->
            val columnNames = rows.first().keys.toList()
            writer.println(columnNames.joinToString(separator = ",") { "\"$it\"" })
            rows.forEach { row ->
                columnNames
                    .map { key -> row[key] }
                    .toCsvRow()
                    .let { writer.println(it) }
            }
        }
}

val gson = Gson()

inline fun <reified T> String.jsonParsed(): T = gson
    .fromJson(this, object : TypeToken<T>() {}.type)

fun List<JsonElement?>.toCsvRow() =
    this.map {
        when {
            it == null -> "null"
            it.isJsonPrimitive -> it.asString
            else -> it.toString()
        }
    }.joinToString(separator = ",") {
        "\"${it.replace("\"", "\"\"")}\""
    }
