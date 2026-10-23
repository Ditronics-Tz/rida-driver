package com.example.ridedriver.data.repository

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URI

/** Multipart and response rules shared by photo upload and avatar rendering. */
internal object ProfilePhotoContract {
    const val MAX_BYTES = 5 * 1024 * 1024
    val extensions = mapOf("image/jpeg" to "jpg", "image/png" to "png", "image/webp" to "webp")

    fun readBytes(input: InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count == -1) break
            require(output.size() + count <= MAX_BYTES) { "Choose a photo smaller than 5 MB." }
            output.write(buffer, 0, count)
        }
        require(output.size() > 0) { "This photo is empty. Choose another image." }
        return output.toByteArray()
    }

    fun photoUrl(payload: Map<*, *>, baseUrl: String): String? {
        val keys = listOf("profile_picture_url", "profile_picture", "profile_image_url", "avatar_url", "photo_url", "profilePictureUrl")
        for (key in keys) {
            val value = payload[key] as? String ?: continue
            resolveUrl(value, baseUrl)?.let { return it }
        }
        for (key in listOf("data", "profile", "user")) {
            val nested = payload[key] as? Map<*, *> ?: continue
            photoUrl(nested, baseUrl)?.let { return it }
        }
        return null
    }

    fun resolveUrl(value: String, baseUrl: String): String? = runCatching {
        if (value.isBlank()) return null
        val url = URI(baseUrl).resolve(value.trim())
        url.takeIf { it.scheme.equals("https", ignoreCase = true) && !it.host.isNullOrBlank() && it.userInfo == null }
            ?.toString()
    }.getOrNull()
}
