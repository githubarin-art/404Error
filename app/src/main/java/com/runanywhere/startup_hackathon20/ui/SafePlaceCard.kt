package com.runanywhere.startup_hackathon20.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.runanywhere.startup_hackathon20.data.SafePlace
import androidx.compose.foundation.layout.FlowRow

@Composable
fun SafePlaceCard(
    place: SafePlace,
    onNavigate: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = place.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${place.distance.toInt()}m away",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row {
                Icon(
                    imageVector = if (place.isOpen24h) Icons.Default.AccessTime else Icons.Default.Close,
                    contentDescription = if (place.isOpen24h) "24h Open" else "Closed",
                    tint = if (place.isOpen24h) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Text(
                    text = if (place.isOpen24h) "24h Open" else "Closed",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            // Types as chips
            FlowRow(modifier = Modifier.padding(top = 8.dp)) {
                place.types.take(2).forEach { type ->
                    AssistChip(
                        onClick = { /* No action */ },
                        label = { Text(text = type) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onNavigate,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Navigate")
            }
        }
    }
}