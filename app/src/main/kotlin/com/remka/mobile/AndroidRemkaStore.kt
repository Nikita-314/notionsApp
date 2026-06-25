package com.remka.mobile

import com.remka.data.RemkaSnapshot
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class AndroidRemkaStore(
    private val file: File
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun load(): RemkaSnapshot? {
        if (!file.exists()) {
            return null
        }

        val content = file.readText()
        if (content.isBlank()) {
            return null
        }

        return json.decodeFromString<RemkaSnapshot>(content)
    }

    fun save(snapshot: RemkaSnapshot) {
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(snapshot))
    }
}
