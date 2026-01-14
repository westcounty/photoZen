package com.example.photozen.ui.screens.workflow

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.photozen.ui.theme.KeepGreen
import com.example.photozen.ui.theme.MaybeAmber
import com.example.photozen.ui.theme.TrashRed
import kotlinx.coroutines.delay

/**
 * Victory Screen - Celebration page shown after completing the workflow.
 * 
 * Shows:
 * - Trophy animation
 * - Session statistics (photos sorted, time, max combo)
 * - Action buttons (Return home, Share)
 */
@Composable
fun VictoryScreen(
    stats: WorkflowStats,
    onReturnHome: () -> Unit,
    onShare: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showContent by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var showButtons by remember { mutableStateOf(false) }
    
    // Staggered animation timing
    LaunchedEffect(Unit) {
        delay(200)
        showContent = true
        delay(400)
        showStats = true
        delay(300)
        showButtons = true
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Trophy Animation
            AnimatedVisibility(
                visible = showContent,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn()
            ) {
                TrophyAnimation()
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Congratulations Text
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = tween(500))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "太棒了！",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "整理任务已完成",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Statistics Cards
            AnimatedVisibility(
                visible = showStats,
                enter = fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.9f)
            ) {
                StatsGrid(stats = stats)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Action Buttons
            AnimatedVisibility(
                visible = showButtons,
                enter = fadeIn(animationSpec = tween(300))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onReturnHome,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KeepGreen
                        )
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("返回首页", fontWeight = FontWeight.Bold)
                    }
                    
                    // Share button (for future use)
                    /*
                    FilledTonalButton(
                        onClick = onShare,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("分享成果")
                    }
                    */
                }
            }
        }
    }
}

/**
 * Animated trophy icon with bouncing and glowing effect.
 */
@Composable
private fun TrophyAnimation() {
    val scale = remember { Animatable(0.5f) }
    val rotation = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        // Bounce in
        scale.animateTo(
            targetValue = 1.2f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        
        // Subtle wobble
        while (true) {
            rotation.animateTo(
                targetValue = 5f,
                animationSpec = tween(1500, easing = FastOutSlowInEasing)
            )
            rotation.animateTo(
                targetValue = -5f,
                animationSpec = tween(1500, easing = FastOutSlowInEasing)
            )
        }
    }
    
    Box(
        modifier = Modifier
            .size(120.dp)
            .scale(scale.value)
            .graphicsLayer { rotationZ = rotation.value }
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaybeAmber.copy(alpha = 0.3f),
                        MaybeAmber.copy(alpha = 0.1f),
                        Color.Transparent
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.EmojiEvents,
            contentDescription = "Trophy",
            tint = MaybeAmber,
            modifier = Modifier.size(72.dp)
        )
    }
}

/**
 * Grid of statistic cards.
 */
@Composable
private fun StatsGrid(stats: WorkflowStats) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Main stat: Total sorted
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stats.totalSorted.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "张照片已整理",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Secondary stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                value = stats.keptCount.toString(),
                label = "保留",
                icon = Icons.Default.Check,
                color = KeepGreen,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                value = stats.trashedCount.toString(),
                label = "删除",
                icon = Icons.Default.Close,
                color = TrashRed,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                value = stats.maybeCount.toString(),
                label = "待定",
                icon = Icons.Default.QuestionMark,
                color = MaybeAmber,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Time, Combo and Tagged row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                value = stats.durationFormatted,
                label = "用时",
                icon = Icons.Default.Timer,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                value = "x${stats.maxCombo}",
                label = "最高连击",
                icon = Icons.Default.LocalFireDepartment,
                color = if (stats.maxCombo >= 20) TrashRed 
                       else if (stats.maxCombo >= 10) MaybeAmber 
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Tagged stats row (only show if any photos were tagged)
        if (stats.taggedCount > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    value = stats.taggedCount.toString(),
                    label = "已分类",
                    icon = Icons.Default.Label,
                    color = KeepGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Individual stat card.
 */
@Composable
private fun StatCard(
    value: String,
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
