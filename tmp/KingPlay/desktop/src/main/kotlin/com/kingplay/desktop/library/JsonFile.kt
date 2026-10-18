package com.kingplay.desktop.library

import com.kingplay.core.model.backup.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Small JSON files that are written safely: a crash mid-write can never destroy the previous copy. */
object JsonFile {
    /** The parsed top-level object, or null if the file is missing, damaged or not an object. */
    fun read(file: File): Map<*, *>? {
        if (!file.isFile) return null
        return runCatching { Json.parse(file.readText(Charsets.UTF_8)) as? Map<*, *> }.getOrNull()
    }

    @Synchronized
    fun write(file: File, value: Map<String, Any?>) {
        file.parentFile?.mkdirs()
        val temp = File(file.path + ".tmp")
        temp.writeText(Json.write(value), Charsets.UTF_8)
        Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
}
