package com.windupairships.airshippiano.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.windupairships.airshippiano.midi.MidiDeviceGroup
import com.windupairships.airshippiano.notedetection.NoteDisplay

@Composable
fun DeviceRow(
    group: MidiDeviceGroup,
    showNotes: Boolean,
    onTransposeChange: (Int) -> Unit
) {
    val noteInfo = remember(group.activeNotes, group.transpose) {
        NoteDisplay.describe(group.activeNotes, group.transpose)
    }

    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Device name + endpoints
            if (group.deviceName.isNotEmpty()) {
                Text(
                    text = group.deviceName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF666666)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            for (endpoint in group.endpoints) {
                Canvas(modifier = Modifier.size(5.dp)) {
                    drawCircle(Color(0xFF4CAF50))
                }
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = endpoint.displayName,
                    fontSize = 11.sp,
                    color = Color(0xFF666666)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            // Transpose controls
            IconButton(
                onClick = { onTransposeChange(group.transpose - 1) },
                modifier = Modifier.size(28.dp)
            ) {
                Text(
                    text = "\u2212",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = when {
                    group.transpose == 0 -> "0"
                    group.transpose > 0 -> "+${group.transpose}"
                    else -> "${group.transpose}"
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                color = if (group.transpose == 0)
                    MaterialTheme.colorScheme.outline
                else
                    MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(26.dp),
                textAlign = TextAlign.Center
            )

            IconButton(
                onClick = { onTransposeChange(group.transpose + 1) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Transpose up",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        // Per-device note display when multiple devices
        if (showNotes && group.activeNotes.isNotEmpty()) {
            Text(
                text = noteInfo.label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}
