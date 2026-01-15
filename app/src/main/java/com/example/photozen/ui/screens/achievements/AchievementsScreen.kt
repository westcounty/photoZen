package com.example.photozen.ui.screens.achievements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.photozen.ui.components.AchievementGrid
import com.example.photozen.ui.components.generateAchievements
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.theme.TrashRed

/**
 * Achievements Screen - displays all achievements and progress.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AchievementsViewModel = hiltViewModel()
) {
    val achievementData by viewModel.achievementData.collectAsState()
    val achievements = generateAchievements(achievementData)
    val unlockedCount = achievements.count { it.isUnlocked }
    
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "成就系统",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "已解锁 $unlockedCount / ${achievements.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Statistics summary card
            StatisticsCard(
                totalSorted = achievementData.totalSorted,
                trashCount = achievementData.trashCount,
                keepCount = achievementData.keepCount
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Encouragement message
            Text(
                text = "继续努力，解锁更多成就！",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Achievement grid
            AchievementGrid(achievements = achievements)
        }
    }
}

/**
 * Statistics card showing user's photo organizing history.
 */
@Composable
private fun StatisticsCard(
    totalSorted: Int,
    trashCount: Int,
    keepCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "📊 累计统计",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = { 
                        Icon(
                            Icons.Default.PhotoLibrary, 
                            null, 
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    value = totalSorted,
                    label = "已整理"
                )
                
                StatItem(
                    icon = {
                        Icon(
                            Icons.Default.Favorite,
                            null,
                            tint = KeepGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    value = keepCount,
                    label = "已保留"
                )
                
                StatItem(
                    icon = {
                        Icon(
                            Icons.Default.Delete,
                            null,
                            tint = TrashRed,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    value = trashCount,
                    label = "已删除"
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: @Composable () -> Unit,
    value: Int,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = formatNumber(value),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

/**
 * Format large numbers with K suffix.
 */
private fun formatNumber(num: Int): String {
    return when {
        num >= 10000 -> "${num / 1000}K+"
        num >= 1000 -> String.format("%.1fK", num / 1000f)
        else -> num.toString()
    }
}
