package com.windupairships.airshippiano.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.windupairships.airshippiano.BuildConfig
import com.windupairships.airshippiano.R
import com.windupairships.airshippiano.notedetection.NoteDisplay

@Composable
fun PianoScreen(viewModel: PianoViewModel) {
    val deviceGroups by viewModel.midiManager.deviceGroups.collectAsState()
    var volume by remember { mutableFloatStateOf(0.8f) }
    var logoAppeared by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val logoScale by animateFloatAsState(
        targetValue = if (logoAppeared) 1f else 0.9f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "logoScale"
    )

    LaunchedEffect(Unit) {
        logoAppeared = true
        viewModel.setVolume(volume)
    }

    val allNotes = remember(deviceGroups) {
        val notes = mutableSetOf<Int>()
        for (group in deviceGroups) {
            for (note in group.activeNotes) {
                val transposed = note + group.transpose
                if (transposed in 0..127) notes.add(transposed)
            }
        }
        notes.toSet()
    }

    val noteInfo = remember(allNotes) { NoteDisplay.describe(allNotes) }
    val isPlaying = allNotes.isNotEmpty()
    val multipleDevices = deviceGroups.size > 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo
        Image(
            painter = painterResource(R.drawable.app_logo),
            contentDescription = "Airship Piano",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp, vertical = 20.dp)
                .scale(logoScale)
        )

        // Note display
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isPlaying) {
                Text(
                    text = noteInfo.label,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = noteInfo.detail,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
                if (allNotes.size > 1) {
                    Text(
                        text = "${allNotes.size} keys",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    listOf(Color(0xFF1976D2), Color(0xFF7B1FA2))
                                ),
                                shape = RoundedCornerShape(50)
                            )
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            } else {
                Text(
                    text = "Play a note...",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // Bottom section
        Column(
            modifier = Modifier.padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Volume slider
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (volume > 0) Icons.AutoMirrored.Filled.VolumeMute
                        else Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = if (volume > 0) "Volume" else "Muted",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = volume,
                    onValueChange = {
                        volume = it
                        viewModel.setVolume(it)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Max volume",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Device list
            if (deviceGroups.isEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Canvas(modifier = Modifier.size(5.dp)) {
                        drawCircle(Color.Red.copy(alpha = 0.8f))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "No MIDI device",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF666666)
                    )
                }
            } else {
                for (group in deviceGroups) {
                    DeviceRow(
                        group = group,
                        showNotes = multipleDevices,
                        onTransposeChange = { offset ->
                            viewModel.midiManager.setTranspose(group.deviceName, offset)
                        }
                    )
                }
            }

            // Bluetooth MIDI button
            Button(
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                },
                modifier = Modifier.padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Bluetooth MIDI",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Version + GitHub link
            TextButton(
                onClick = { uriHandler.openUri("https://github.com/billzajac/airship-piano") },
                modifier = Modifier.padding(top = 6.dp)
            ) {
                Text(
                    text = "v${BuildConfig.VERSION_NAME}  </> Open Source",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF666666)
                )
            }
        }
    }
}
