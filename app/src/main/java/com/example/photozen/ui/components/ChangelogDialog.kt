package com.example.photozen.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.photozen.BuildConfig
import com.example.photozen.util.ChangelogEntry
import com.example.photozen.util.ChangelogParser

/**
 * Changelog dialog showing version history.
 * Dynamically loads and parses CHANGELOG.md from assets.
 * Can be used from SettingsScreen (manual) and HomeScreen (auto-popup).
 */
@Composable
fun ChangelogDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var changelog by remember { mutableStateOf<ChangelogEntry?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Load changelog from assets - always get the latest version
    LaunchedEffect(Unit) {
        changelog = ChangelogParser.parseLatestVersion(context)
        isLoading = false
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("更新日志")
            }
        },
        text = {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else if (changelog != null) {
                ChangelogContent(changelog!!)
            } else {
                Text(
                    text = "暂无更新日志",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

/**
 * Display changelog content parsed from CHANGELOG.md.
 */
@Composable
private fun ChangelogContent(entry: ChangelogEntry) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Version header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "v${entry.version}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = entry.date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Title
        if (entry.title.isNotEmpty()) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        // Summary
        if (entry.summary.isNotEmpty()) {
            Text(
                text = entry.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Sections
        entry.sections.forEachIndexed { index, section ->
            if (index > 0) {
                HorizontalDivider()
            }
            
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            section.items.forEach { item ->
                ChangelogItem(
                    title = item.title,
                    description = item.description
                )
            }
        }
    }
}

/**
 * Single changelog item.
 */
@Composable
private fun ChangelogItem(title: String, description: String) {
    Column(modifier = Modifier.padding(start = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (description.isNotEmpty()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
