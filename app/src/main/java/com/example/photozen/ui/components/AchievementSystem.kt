package com.example.photozen.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.photozen.data.repository.AchievementData

/**
 * Achievement category enumeration.
 */
enum class AchievementCategory(val displayName: String, val icon: ImageVector) {
    SORTING("整理大师", Icons.Default.PhotoLibrary),
    COMBO("连击高手", Icons.Default.Bolt),
    TAGGING("标签达人", Icons.Default.Label),
    COLLECTOR("收藏专家", Icons.Default.Favorite),
    CLEANER("清理专家", Icons.Default.Delete),
    CREATIVE("创意大师", Icons.Default.ContentCopy),
    EXPORTER("导出达人", Icons.Default.Save),
    EXPLORER("探索者", Icons.Default.Visibility),
    DEDICATED("坚持不懈", Icons.Default.Today),
    MASTER("大师之路", Icons.Default.WorkspacePremium)
}

/**
 * Achievement data class representing a single achievement.
 */
data class Achievement(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val category: AchievementCategory,
    val targetValue: Int,
    val currentValue: Int,
    val color: Color,
    val rarity: AchievementRarity = AchievementRarity.COMMON
) {
    val isUnlocked: Boolean get() = currentValue >= targetValue
    val progress: Float get() = (currentValue.toFloat() / targetValue).coerceIn(0f, 1f)
}

/**
 * Achievement rarity levels.
 */
enum class AchievementRarity(val displayName: String, val color: Color) {
    COMMON("普通", Color(0xFF9E9E9E)),
    UNCOMMON("稀有", Color(0xFF4CAF50)),
    RARE("史诗", Color(0xFF2196F3)),
    EPIC("传说", Color(0xFF9C27B0)),
    LEGENDARY("神话", Color(0xFFFF9800))
}

/**
 * Generate all achievements based on current achievement data.
 */
fun generateAchievements(data: AchievementData): List<Achievement> {
    return listOf(
        // ==================== 整理大师 (Sorting Master) ====================
        Achievement(
            id = "sort_10", name = "初出茅庐", description = "累计整理 10 张照片",
            icon = Icons.Default.PhotoLibrary, category = AchievementCategory.SORTING,
            targetValue = 10, currentValue = data.totalSorted,
            color = Color(0xFF4CAF50), rarity = AchievementRarity.COMMON
        ),
        Achievement(
            id = "sort_50", name = "小有成就", description = "累计整理 50 张照片",
            icon = Icons.Default.PhotoLibrary, category = AchievementCategory.SORTING,
            targetValue = 50, currentValue = data.totalSorted,
            color = Color(0xFF4CAF50), rarity = AchievementRarity.COMMON
        ),
        Achievement(
            id = "sort_100", name = "整理能手", description = "累计整理 100 张照片",
            icon = Icons.Default.Star, category = AchievementCategory.SORTING,
            targetValue = 100, currentValue = data.totalSorted,
            color = Color(0xFF4CAF50), rarity = AchievementRarity.UNCOMMON
        ),
        Achievement(
            id = "sort_500", name = "整理专家", description = "累计整理 500 张照片",
            icon = Icons.Default.Grade, category = AchievementCategory.SORTING,
            targetValue = 500, currentValue = data.totalSorted,
            color = Color(0xFF2196F3), rarity = AchievementRarity.RARE
        ),
        Achievement(
            id = "sort_1000", name = "千图斩", description = "累计整理 1,000 张照片",
            icon = Icons.Default.MilitaryTech, category = AchievementCategory.SORTING,
            targetValue = 1000, currentValue = data.totalSorted,
            color = Color(0xFF9C27B0), rarity = AchievementRarity.EPIC
        ),
        Achievement(
            id = "sort_5000", name = "整理大师", description = "累计整理 5,000 张照片",
            icon = Icons.Default.EmojiEvents, category = AchievementCategory.SORTING,
            targetValue = 5000, currentValue = data.totalSorted,
            color = Color(0xFFFF9800), rarity = AchievementRarity.LEGENDARY
        ),
        Achievement(
            id = "sort_10000", name = "万图传说", description = "累计整理 10,000 张照片",
            icon = Icons.Default.WorkspacePremium, category = AchievementCategory.SORTING,
            targetValue = 10000, currentValue = data.totalSorted,
            color = Color(0xFFFFD700), rarity = AchievementRarity.LEGENDARY
        ),
        
        // ==================== 连击高手 (Combo Master) ====================
        Achievement(
            id = "combo_5", name = "初次连击", description = "达成 5 连击",
            icon = Icons.Default.Bolt, category = AchievementCategory.COMBO,
            targetValue = 5, currentValue = data.maxCombo,
            color = Color(0xFFFF5722), rarity = AchievementRarity.COMMON
        ),
        Achievement(
            id = "combo_10", name = "连击新星", description = "达成 10 连击",
            icon = Icons.Default.FlashOn, category = AchievementCategory.COMBO,
            targetValue = 10, currentValue = data.maxCombo,
            color = Color(0xFFFF5722), rarity = AchievementRarity.UNCOMMON
        ),
        Achievement(
            id = "combo_20", name = "连击达人", description = "达成 20 连击",
            icon = Icons.Default.Speed, category = AchievementCategory.COMBO,
            targetValue = 20, currentValue = data.maxCombo,
            color = Color(0xFFFF5722), rarity = AchievementRarity.RARE
        ),
        Achievement(
            id = "combo_50", name = "连击之王", description = "达成 50 连击",
            icon = Icons.Default.LocalFireDepartment, category = AchievementCategory.COMBO,
            targetValue = 50, currentValue = data.maxCombo,
            color = Color(0xFFFF5722), rarity = AchievementRarity.EPIC
        ),
        Achievement(
            id = "combo_100", name = "不可阻挡", description = "达成 100 连击",
            icon = Icons.Default.Rocket, category = AchievementCategory.COMBO,
            targetValue = 100, currentValue = data.maxCombo,
            color = Color(0xFFFFD700), rarity = AchievementRarity.LEGENDARY
        ),
        
        // ==================== 标签达人 (Tagging Expert) ====================
        Achievement(
            id = "tag_10", name = "标签入门", description = "为 10 张照片添加标签",
            icon = Icons.Default.Label, category = AchievementCategory.TAGGING,
            targetValue = 10, currentValue = data.totalTagged,
            color = Color(0xFFA78BFA), rarity = AchievementRarity.COMMON
        ),
        Achievement(
            id = "tag_50", name = "标签爱好者", description = "为 50 张照片添加标签",
            icon = Icons.Default.Label, category = AchievementCategory.TAGGING,
            targetValue = 50, currentValue = data.totalTagged,
            color = Color(0xFFA78BFA), rarity = AchievementRarity.UNCOMMON
        ),
        Achievement(
            id = "tag_100", name = "分类专家", description = "为 100 张照片添加标签",
            icon = Icons.Default.Label, category = AchievementCategory.TAGGING,
            targetValue = 100, currentValue = data.totalTagged,
            color = Color(0xFFA78BFA), rarity = AchievementRarity.RARE
        ),
        Achievement(
            id = "tag_500", name = "标签大师", description = "为 500 张照片添加标签",
            icon = Icons.Default.AutoAwesome, category = AchievementCategory.TAGGING,
            targetValue = 500, currentValue = data.totalTagged,
            color = Color(0xFFA78BFA), rarity = AchievementRarity.EPIC
        ),
        Achievement(
            id = "tags_created_5", name = "标签创造者", description = "创建 5 个标签",
            icon = Icons.Default.Label, category = AchievementCategory.TAGGING,
            targetValue = 5, currentValue = data.tagsCreated,
            color = Color(0xFFA78BFA), rarity = AchievementRarity.COMMON
        ),
        Achievement(
            id = "tags_created_20", name = "标签架构师", description = "创建 20 个标签",
            icon = Icons.Default.FilterVintage, category = AchievementCategory.TAGGING,
            targetValue = 20, currentValue = data.tagsCreated,
            color = Color(0xFFA78BFA), rarity = AchievementRarity.RARE
        ),
        
        // ==================== 收藏专家 (Collector) ====================
        Achievement(
            id = "keep_10", name = "初次收藏", description = "保留 10 张照片",
            icon = Icons.Default.Favorite, category = AchievementCategory.COLLECTOR,
            targetValue = 10, currentValue = data.keepCount,
            color = Color(0xFFE91E63), rarity = AchievementRarity.COMMON
        ),
        Achievement(
            id = "keep_50", name = "收藏爱好者", description = "保留 50 张照片",
            icon = Icons.Default.Favorite, category = AchievementCategory.COLLECTOR,
            targetValue = 50, currentValue = data.keepCount,
            color = Color(0xFFE91E63), rarity = AchievementRarity.UNCOMMON
        ),
        Achievement(
            id = "keep_200", name = "精品收藏家", description = "保留 200 张照片",
            icon = Icons.Default.Favorite, category = AchievementCategory.COLLECTOR,
            targetValue = 200, currentValue = data.keepCount,
            color = Color(0xFFE91E63), rarity = AchievementRarity.RARE
        ),
        Achievement(
            id = "keep_500", name = "收藏大师", description = "保留 500 张照片",
            icon = Icons.Default.Verified, category = AchievementCategory.COLLECTOR,
            targetValue = 500, currentValue = data.keepCount,
            color = Color(0xFFE91E63), rarity = AchievementRarity.EPIC
        ),
        
        // ==================== 清理专家 (Cleaner) ====================
        Achievement(
            id = "trash_50", name = "断舍离", description = "删除 50 张照片",
            icon = Icons.Default.Delete, category = AchievementCategory.CLEANER,
            targetValue = 50, currentValue = data.trashCount,
            color = Color(0xFFF44336), rarity = AchievementRarity.COMMON
        ),
        Achievement(
            id = "trash_200", name = "清理达人", description = "删除 200 张照片",
            icon = Icons.Default.Delete, category = AchievementCategory.CLEANER,
            targetValue = 200, currentValue = data.trashCount,
            color = Color(0xFFF44336), rarity = AchievementRarity.UNCOMMON
        ),
        Achievement(
            id = "trash_500", name = "存储守护者", description = "删除 500 张照片",
            icon = Icons.Default.Delete, category = AchievementCategory.CLEANER,
            targetValue = 500, currentValue = data.trashCount,
            color = Color(0xFFF44336), rarity = AchievementRarity.RARE
        ),
        Achievement(
            id = "empty_trash_3", name = "清洁工", description = "清空回收站 3 次",
            icon = Icons.Default.Delete, category = AchievementCategory.CLEANER,
            targetValue = 3, currentValue = data.trashEmptied,
            color = Color(0xFFF44336), rarity = AchievementRarity.COMMON
        ),
        Achievement(
            id = "empty_trash_10", name = "清理大师", description = "清空回收站 10 次",
            icon = Icons.Default.Delete, category = AchievementCategory.CLEANER,
            targetValue = 10, currentValue = data.trashEmptied,
            color = Color(0xFFF44336), rarity = AchievementRarity.RARE
        ),
        
        // ==================== 创意大师 (Creative Master) ====================
        Achievement(
            id = "virtual_copy_1", name = "创意初体验", description = "创建 1 个虚拟副本",
            icon = Icons.Default.ContentCopy, category = AchievementCategory.CREATIVE,
            targetValue = 1, currentValue = data.virtualCopiesCreated,
            color = Color(0xFF00BCD4), rarity = AchievementRarity.COMMON
        ),
        Achievement(
            id = "virtual_copy_10", name = "副本达人", description = "创建 10 个虚拟副本",
            icon = Icons.Default.ContentCopy, category = AchievementCategory.CREATIVE,
            targetValue = 10, currentValue = data.virtualCopiesCreated,
            color = Color(0xFF00BCD4), rarity = AchievementRarity.UNCOMMON
        ),
        Achievement(
            id = "virtual_copy_50", name = "创意大师", description = "创建 50 个虚拟副本",
            icon = Icons.Default.ContentCopy, category = AchievementCategory.CREATIVE,
            targetValue = 50, currentValue = data.virtualCopiesCreated,
            color = Color(0xFF00BCD4), rarity = AchievementRarity.EPIC
        ),
        
        // ==================== 导出达人 (Export Master) ====================
        Achievement(
            id = "export_1", name = "首次导出", description = "导出 1 张照片",
            icon = Icons.Default.Save, category = AchievementCategory.EXPORTER,
            targetValue = 1, currentValue = data.photosExported,
            color = Color(0xFF8BC34A), rarity = AchievementRarity.COMMON
        ),
        Achievement(
            id = "export_10", name = "导出能手", description = "导出 10 张照片",
            icon = Icons.Default.Save, category = AchievementCategory.EXPORTER,
            targetValue = 10, currentValue = data.photosExported,
            color = Color(0xFF8BC34A), rarity = AchievementRarity.UNCOMMON
        ),
        Achievement(
            id = "export_50", name = "导出专家", description = "导出 50 张照片",
            icon = Icons.Default.Save, category = AchievementCategory.EXPORTER,
            targetValue = 50, currentValue = data.photosExported,
            color = Color(0xFF8BC34A), rarity = AchievementRarity.RARE
        ),
        
        // ==================== 探索者 (Explorer) ====================
        Achievement(
            id = "compare_5", name = "比较新手", description = "完成 5 次照片对比",
            icon = Icons.Default.Visibility, category = AchievementCategory.EXPLORER,
            targetValue = 5, currentValue = data.comparisonSessions,
            color = Color(0xFF3F51B5), rarity = AchievementRarity.COMMON
        ),
        Achievement(
            id = "compare_20", name = "火眼金睛", description = "完成 20 次照片对比",
            icon = Icons.Default.Visibility, category = AchievementCategory.EXPLORER,
            targetValue = 20, currentValue = data.comparisonSessions,
            color = Color(0xFF3F51B5), rarity = AchievementRarity.UNCOMMON
        ),
        Achievement(
            id = "compare_50", name = "鉴赏专家", description = "完成 50 次照片对比",
            icon = Icons.Default.Visibility, category = AchievementCategory.EXPLORER,
            targetValue = 50, currentValue = data.comparisonSessions,
            color = Color(0xFF3F51B5), rarity = AchievementRarity.RARE
        ),
        Achievement(
            id = "flow_5", name = "Flow 入门", description = "完成 5 次 Flow 工作流",
            icon = Icons.Default.Loop, category = AchievementCategory.EXPLORER,
            targetValue = 5, currentValue = data.flowSessionsCompleted,
            color = Color(0xFF3F51B5), rarity = AchievementRarity.COMMON
        ),
        Achievement(
            id = "flow_20", name = "Flow 达人", description = "完成 20 次 Flow 工作流",
            icon = Icons.Default.Loop, category = AchievementCategory.EXPLORER,
            targetValue = 20, currentValue = data.flowSessionsCompleted,
            color = Color(0xFF3F51B5), rarity = AchievementRarity.RARE
        ),
        
        // ==================== 坚持不懈 (Dedicated) ====================
        Achievement(
            id = "streak_3", name = "坚持三天", description = "连续 3 天使用 PicZen",
            icon = Icons.Default.Today, category = AchievementCategory.DEDICATED,
            targetValue = 3, currentValue = data.consecutiveDays,
            color = Color(0xFFFF9800), rarity = AchievementRarity.COMMON
        ),
        Achievement(
            id = "streak_7", name = "周末战士", description = "连续 7 天使用 PicZen",
            icon = Icons.Default.Today, category = AchievementCategory.DEDICATED,
            targetValue = 7, currentValue = data.consecutiveDays,
            color = Color(0xFFFF9800), rarity = AchievementRarity.UNCOMMON
        ),
        Achievement(
            id = "streak_14", name = "半月坚持", description = "连续 14 天使用 PicZen",
            icon = Icons.Default.Today, category = AchievementCategory.DEDICATED,
            targetValue = 14, currentValue = data.consecutiveDays,
            color = Color(0xFFFF9800), rarity = AchievementRarity.RARE
        ),
        Achievement(
            id = "streak_30", name = "整月达人", description = "连续 30 天使用 PicZen",
            icon = Icons.Default.Today, category = AchievementCategory.DEDICATED,
            targetValue = 30, currentValue = data.consecutiveDays,
            color = Color(0xFFFF9800), rarity = AchievementRarity.EPIC
        ),
        Achievement(
            id = "perfect_1", name = "完美一天", description = "单日整理超过 100 张",
            icon = Icons.Default.Star, category = AchievementCategory.DEDICATED,
            targetValue = 1, currentValue = data.perfectDays,
            color = Color(0xFFFF9800), rarity = AchievementRarity.UNCOMMON
        ),
        Achievement(
            id = "perfect_7", name = "效率达人", description = "7 天单日整理超过 100 张",
            icon = Icons.Default.Star, category = AchievementCategory.DEDICATED,
            targetValue = 7, currentValue = data.perfectDays,
            color = Color(0xFFFF9800), rarity = AchievementRarity.EPIC
        )
    )
}

/**
 * Achievement summary card showing progress overview.
 */
@Composable
fun AchievementSummaryCard(
    achievements: List<Achievement>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unlockedCount = achievements.count { it.isUnlocked }
    val totalCount = achievements.size
    val progress = unlockedCount.toFloat() / totalCount
    
    // Find next achievement to unlock
    val nextAchievement = achievements
        .filter { !it.isUnlocked }
        .minByOrNull { it.targetValue - it.currentValue }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFFD700),
                                        Color(0xFFFFA500)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "成就系统",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$unlockedCount / $totalCount 已解锁",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFFFFD700),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            // Next achievement hint
            nextAchievement?.let { achievement ->
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(achievement.color.copy(alpha = 0.1f))
                        .padding(8.dp)
                ) {
                    Icon(
                        achievement.icon,
                        contentDescription = null,
                        tint = achievement.color,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "下一个：${achievement.name}",
                            style = MaterialTheme.typography.labelMedium,
                            color = achievement.color,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${achievement.currentValue}/${achievement.targetValue}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${(achievement.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = achievement.color,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Recently unlocked badges preview
            val recentUnlocked = achievements.filter { it.isUnlocked }.takeLast(5)
            if (recentUnlocked.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(recentUnlocked) { achievement ->
                        AchievementBadgeSmall(achievement = achievement)
                    }
                }
            }
        }
    }
}

/**
 * Small achievement badge for preview.
 */
@Composable
private fun AchievementBadgeSmall(
    achievement: Achievement,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(achievement.color.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            achievement.icon,
            contentDescription = achievement.name,
            tint = achievement.color,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Full achievement badge with details.
 */
@Composable
fun AchievementBadge(
    achievement: Achievement,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (achievement.isUnlocked) 1f else 0.9f,
        animationSpec = spring(),
        label = "scale"
    )
    
    Card(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (achievement.isUnlocked) 1f else 0.5f
            },
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.isUnlocked) {
                achievement.color.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .width(100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (achievement.isUnlocked) {
                            Brush.linearGradient(
                                colors = listOf(
                                    achievement.color,
                                    achievement.color.copy(alpha = 0.7f)
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.Gray,
                                    Color.Gray.copy(alpha = 0.7f)
                                )
                            )
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    achievement.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                
                // Unlocked checkmark
                if (achievement.isUnlocked) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = achievement.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = achievement.rarity.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = achievement.rarity.color,
                fontWeight = FontWeight.Medium
            )
            
            if (!achievement.isUnlocked) {
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { achievement.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = achievement.color,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    text = "${achievement.currentValue}/${achievement.targetValue}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Achievement list grouped by category.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AchievementGrid(
    achievements: List<Achievement>,
    modifier: Modifier = Modifier
) {
    val groupedAchievements = achievements.groupBy { it.category }
    
    Column(modifier = modifier) {
        groupedAchievements.forEach { (category, categoryAchievements) ->
            val unlockedCount = categoryAchievements.count { it.isUnlocked }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    category.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "$unlockedCount/${categoryAchievements.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categoryAchievements.forEach { achievement ->
                    AchievementBadge(achievement = achievement)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
