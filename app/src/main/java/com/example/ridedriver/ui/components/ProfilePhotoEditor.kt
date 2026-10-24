package com.example.ridedriver.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ridedriver.data.local.SessionManager
import com.example.ridedriver.data.repository.ProfilePhotoRepository
import kotlinx.coroutines.launch

@Composable
fun ProfilePhotoEditor() {
    val context = LocalContext.current
    val session by SessionManager.observe(context).collectAsState(initial = SessionManager.Session())
    val scope = rememberCoroutineScope()
    var uploading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var lastPhoto by remember { mutableStateOf<Uri?>(null) }
    fun upload(uri: Uri) {
        if (uploading) return
        lastPhoto = uri
        error = null
        uploading = true
        scope.launch {
            try {
                ProfilePhotoRepository.upload(context, uri)
                    .onSuccess { Toast.makeText(context, "Profile photo updated", Toast.LENGTH_SHORT).show() }
                    .onFailure { error = it.message }
            } finally { uploading = false }
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) upload(uri)
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        UserAvatar(session.name, session.profilePictureUrl, Modifier.size(88.dp), "Profile photo", session.profilePictureVersion)
        TextButton(enabled = !uploading, onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
            Text(if (uploading) "Uploading photo…" else if (session.profilePictureUrl.isNullOrBlank()) "Upload profile photo" else "Change profile photo")
        }
        if (uploading) LinearProgressIndicator(Modifier.fillMaxWidth())
        Text("JPG, PNG or WebP, up to 5 MB", style = MaterialTheme.typography.bodySmall)
        error?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
            TextButton(enabled = !uploading, onClick = { lastPhoto?.let(::upload) }) { Text("Retry upload") }
        }
        Spacer(Modifier.height(16.dp))
    }
}
