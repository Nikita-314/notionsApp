plugins {
    kotlin("jvm") version "2.3.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
    kotlin("plugin.serialization") version "2.3.21" apply false
    id("com.android.application") version "9.0.0" apply false
}

allprojects {
    group = "com.nikita.notionsapp"
    version = "1.0-SNAPSHOT"
}
