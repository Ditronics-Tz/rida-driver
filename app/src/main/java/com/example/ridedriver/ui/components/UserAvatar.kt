package com.example.ridedriver.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.example.ridedriver.data.remote.RidaApiClient
import com.example.ridedriver.data.repository.ProfilePhotoContract
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

private val avatarClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .followSslRedirects(false)
    .build()

/** Displays a saved profile photo, with initials as a loading/error fallback. */
@Composable
fun UserAvatar(
    name: String,
    photoUrl: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    photoVersion: Long = 0
) {
    val preview = LocalInspectionMode.current
    val bitmap by produceState<Bitmap?>(null, photoUrl, preview, photoVersion) {
        value = null
        val url = photoUrl?.let { ProfilePhotoContract.resolveUrl(it, RidaApiClient.BASE_URL) }
        if (preview || url == null) return@produceState
        value = withContext(Dispatchers.IO) {
            try {
                avatarClient.newCall(Request.Builder().url(url).build()).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val bytes = response.body?.byteStream()?.use(ProfilePhotoContract::readBytes)
                        ?: return@withContext null
                    decodeAvatar(bytes)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
        }
    }
    val initials = name.trim().split(Regex("\\s+")).filter(String::isNotBlank).take(2)
        .joinToString("") { it.first().uppercase() }
    Box(
        modifier = modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.06f)),
        contentAlignment = Alignment.Center
    ) {
        val image = bitmap
        when {
            image != null -> Image(
                bitmap = image.asImageBitmap(), contentDescription = contentDescription,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
            )
            initials.isNotEmpty() -> Text(initials, style = MaterialTheme.typography.labelLarge, color = Color.Black)
            else -> RidaIcon(RidaIcons.Account, contentDescription, Modifier.size(24.dp), tint = Color.Black)
        }
    }
}

private fun decodeAvatar(bytes: ByteArray): Bitmap? {
    if (Build.VERSION.SDK_INT >= 28) {
        return ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(bytes))) { decoder, info, _ ->
            decoder.setTargetSampleSize((maxOf(info.size.width, info.size.height) / 384).coerceAtLeast(1))
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    }
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    var sample = 1
    while (maxOf(options.outWidth, options.outHeight) / sample > 768) sample *= 2
    options.inSampleSize = sample
    options.inJustDecodeBounds = false
    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
    val orientation = runCatching {
        ExifInterface(ByteArrayInputStream(bytes)).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    val matrix = Matrix().apply {
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(-90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
        }
    }
    return if (matrix.isIdentity) decoded else Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
}
