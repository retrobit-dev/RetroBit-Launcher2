package com.retrobit.launcher.ui.components

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.retrobit.launcher.ui.theme.LocalRetroBitColors

@Composable
fun VoiceCommandButton(
    onRecognized: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalRetroBitColors.current
    val prefs = remember(context) {
        context.getSharedPreferences("retrobit_launcher_prefs", android.content.Context.MODE_PRIVATE)
    }
    val storedLanguage = prefs.getString("voice_language", "id-ID") ?: "id-ID"
    var languageTag by remember { mutableStateOf(storedLanguage) }
    LaunchedEffect(storedLanguage) { languageTag = storedLanguage }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!spoken.isNullOrBlank()) onRecognized(spoken)
    }

    val launchSpeech: () -> Unit = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PROMPT, if (languageTag.startsWith("id")) "Ucapkan perintah RetroBit" else "Speak a RetroBit command")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        try {
            speechLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "Speech recognition is not available on this device.", Toast.LENGTH_SHORT).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchSpeech()
        else Toast.makeText(context, "Microphone permission is needed for voice commands.", Toast.LENGTH_SHORT).show()
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "MIC",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = colors.inkPrimary,
            modifier = Modifier
                .background(colors.surface)
                .border(1.dp, colors.border)
                .clickable {
                    val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                    if (permission == PackageManager.PERMISSION_GRANTED) launchSpeech()
                    else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
                .padding(horizontal = 7.dp, vertical = 6.dp)
        )
        Text(
            text = if (languageTag.startsWith("id")) "ID" else "EN",
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = colors.inkSecondary,
            modifier = Modifier
                .clickable {
                    languageTag = if (languageTag.startsWith("id")) "en-US" else "id-ID"
                    prefs.edit().putString("voice_language", languageTag).apply()
                }
                .padding(start = 4.dp, end = 2.dp)
        )
    }
}
