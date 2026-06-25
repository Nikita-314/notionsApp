package com.remka.mobile

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.remka.data.RemkaSnapshot
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AndroidRemkaStore(
    private val file: File
) {
    private val keyAlias = "remka_local_data_key_v1"
    private val encryptedPrefix = "REMKA_ENCRYPTED_V1"
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

        if (content.startsWith(encryptedPrefix)) {
            val encryptedBody = content.lineSequence()
                .drop(1)
                .joinToString(separator = "")
                .trim()
            if (encryptedBody.isBlank()) {
                return null
            }

            val encryptedPayload = Base64.decode(encryptedBody, Base64.NO_WRAP)
            if (encryptedPayload.size <= GCM_IV_SIZE_BYTES) {
                return null
            }
            val iv = encryptedPayload.copyOfRange(0, GCM_IV_SIZE_BYTES)
            val cipherText = encryptedPayload.copyOfRange(GCM_IV_SIZE_BYTES, encryptedPayload.size)
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(GCM_TAG_SIZE_BITS, iv))

            return json.decodeFromString<RemkaSnapshot>(
                cipher.doFinal(cipherText).decodeToString()
            )
        }

        val snapshot = json.decodeFromString<RemkaSnapshot>(content)
        save(snapshot)
        return snapshot
    }

    fun save(snapshot: RemkaSnapshot) {
        file.parentFile?.mkdirs()
        val tempFile = file.resolveSibling("${file.name}.tmp")
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val cipherText = cipher.doFinal(json.encodeToString(snapshot).encodeToByteArray())
        val encryptedPayload = cipher.iv + cipherText

        tempFile.writeText(
            encryptedPrefix + "\n" + Base64.encodeToString(encryptedPayload, Base64.NO_WRAP)
        )

        try {
            Files.move(
                tempFile.toPath(),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(
                tempFile.toPath(),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }
        val existingKey = keyStore.getKey(keyAlias, null) as? SecretKey
        if (existingKey != null) {
            return existingKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val keySpec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(keySpec)
        return keyGenerator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_SIZE_BYTES = 12
        const val GCM_TAG_SIZE_BITS = 128
    }
}
