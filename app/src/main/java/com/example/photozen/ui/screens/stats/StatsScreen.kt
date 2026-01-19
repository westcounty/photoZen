package com.example.photozen.ui.screens.stats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.photozen.ui.components.*

/**
 * 统计页面
 * 
 * 展示用户的整理统计数据，包括：
 * - 总整理数
 * - 本周/本月整理数
 * - 分类明细
 * - 日历热力图
 * - 连续整理天数
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 显示错误信息
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }
    
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("整理统计") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "刷新"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (uiState.isLoading) {
            // 加载状态
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // 内容
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 总览卡片
                TotalStatsCard(totalSorted = uiState.summary.totalSorted)
                
                // 周/月统计
                PeriodStatsRow(
                    weekSorted = uiState.summary.weekSorted,
                    monthSorted = uiState.summary.monthSorted
                )
                
                // 本周分类明细
                WeekBreakdownCard(
                    keptCount = uiState.summary.weekKept,
                    trashedCount = uiState.summary.weekTrashed,
                    maybeCount = uiState.summary.weekMaybe
                )
                
                // 日历热力图
                CalendarHeatmapCard(
                    calendarData = uiState.calendarData,
                    selectedDate = uiState.selectedDate,
                    selectedDateCount = uiState.selectedDateCount,
                    onDayClick = viewModel::onDayClicked
                )
                
                // 连续整理天数
                StreakCard(days = uiState.summary.consecutiveDays)
                
                // 底部间距
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * 日历热力图卡片
 */
@Composable
private fun CalendarHeatmapCard(
    calendarData: Map<String, Int>,
    selectedDate: String?,
    selectedDateCount: Int,
    onDayClick: (String, Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "整理日历",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "最近 3 个月",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 热力图
            CalendarHeatmap(
                data = calendarData,
                weeks = 12,
                onDayClick = onDayClick
            )
            
            // 选中日期信息
            selectedDate?.let { date ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = formatSelectedDate(date, selectedDateCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 格式化选中日期显示
 */
private fun formatSelectedDate(date: String, count: Int): String {
    // date 格式: "2026-01-19"
    val parts = date.split("-")
    if (parts.size == 3) {
        val month = parts[1].toIntOrNull() ?: 0
        val day = parts[2].toIntOrNull() ?: 0
        return "${month}月${day}日: 整理了 $count 张照片"
    }
    return "$date: $count 张"
}
