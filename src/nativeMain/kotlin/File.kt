import kotlinx.cinterop.*
import platform.posix.*

class File(private val path: String) {

    fun writer(): FileWriter = FileWriter(path)

    fun readBytes(): ByteArray = memScoped {
        val fp = fopen(path, "r") ?: error("cannot open file.")
        fseek(fp, 0, SEEK_END)
        val size = ftell(fp)
        fseek(fp, 0, SEEK_SET)
        val buffer = allocArray<ByteVar>(size)
        fread(buffer, 1, size.toULong(), fp)
        fclose(fp)
        buffer.readBytes(size)
    }
}

interface Closeable {
    fun close()
}

fun <T : Closeable, R> T.use(block: (T) -> R): R {
    return try {
        block(this)
    } finally {
        close()
    }
}

class FileWriter(path: String) : Closeable {
    private val fp: CPointer<FILE>

    init {
        fp = fopen(path, "w") ?: error("cannot open file.")
    }

    fun write(bytes: ByteArray) {
        val arr = bytes.toCValues()
        fwrite(arr, 1UL, arr.size.toULong(), fp)
    }

    fun writeLine(line: String) {
        val str = line.cstr
        fwrite(str, 1UL, str.size.toULong(), fp)
        fwrite("\n".cstr, 1UL, 1UL, fp)
    }

    override fun close() {
        fclose(fp)
    }
}
