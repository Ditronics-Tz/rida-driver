package com.example.ridedriver.data.repository

import java.io.ByteArrayInputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfilePhotoContractTest {
    private val base = "https://rida.example/api/v1/"

    @Test
    fun acceptsCanonicalUploadResponse() {
        assertEquals("https://cdn.example/avatar.jpg?v=2", ProfilePhotoContract.photoUrl(
            mapOf("profile_picture_url" to "https://cdn.example/avatar.jpg?v=2"), base
        ))
    }

    @Test
    fun restoresPhotoFromNestedProfileResponse() {
        assertEquals("https://rida.example/uploads/avatar.png", ProfilePhotoContract.photoUrl(
            mapOf("data" to mapOf("profile" to mapOf("profile_picture" to "/uploads/avatar.png"))), base
        ))
    }

    @Test
    fun ignoresMissingAndBlankPhotos() {
        assertNull(ProfilePhotoContract.photoUrl(emptyMap<String, Any>(), base))
        assertNull(ProfilePhotoContract.photoUrl(mapOf("profile_picture_url" to "  "), base))
    }

    @Test
    fun rejectsLocalFilesCleartextAndCredentialUrls() {
        for (url in listOf("file:///etc/photo", "content://photos/1", "http://cdn.example/photo.jpg", "https://user:secret@cdn.example/photo.jpg")) {
            assertNull(ProfilePhotoContract.resolveUrl(url, base))
        }
    }

    @Test
    fun preservesImageBytes() {
        val bytes = byteArrayOf(0, 1, 2, 3)
        assertArrayEquals(bytes, ProfilePhotoContract.readBytes(ByteArrayInputStream(bytes)))
    }

    @Test
    fun permitsPhotoAtSizeLimit() {
        assertEquals(ProfilePhotoContract.MAX_BYTES, ProfilePhotoContract.readBytes(
            ByteArrayInputStream(ByteArray(ProfilePhotoContract.MAX_BYTES))
        ).size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOversizedPhoto() {
        ProfilePhotoContract.readBytes(ByteArrayInputStream(ByteArray(ProfilePhotoContract.MAX_BYTES + 1)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsEmptyPhoto() {
        ProfilePhotoContract.readBytes(ByteArrayInputStream(byteArrayOf()))
    }
}
