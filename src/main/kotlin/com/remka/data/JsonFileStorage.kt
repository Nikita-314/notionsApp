package com.remka.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

class JsonFileStorage(
    private val path: Path = Path.of(System.getProperty("remka.data.file") ?: "remka-data.json")
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun load(): RemkaSnapshot? {
        if (!path.exists()) {
            return null
        }

        val content = path.readText()
        if (content.isBlank()) {
            return null
        }

        return json.decodeFromString<RemkaSnapshot>(content)
    }

    fun save(snapshot: RemkaSnapshot) {
        val parent = path.parent
        if (parent != null) {
            Files.createDirectories(parent)
        }

        path.writeText(json.encodeToString(snapshot))
    }

    fun displayPath(): String = path.toString()
}
