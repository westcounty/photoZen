package com.example.photozen.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 日期范围选择器
 * 
 * 支持选择开始日期和结束日期。
 * 
 * ## 设计规范
 * - 两个日期输入框并排
 * - 点击打开日期选择器对话框
 * - 支持快捷选项（最近7天、最近30天、本月、上月）
 * 
 * @param startDate 开始日期时间戳
 * @param endDate 结束日期时间戳
 * @param onRangeChange 日期范围变更回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePicker(
    startDate: Long?,
    endDate: Long?,
    onRangeChange: (Long?, Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }
    
    Column(modifier = modifier) {
        // 快捷选项
        QuickDateOptions(
            onSelectRange = onRangeChange
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 日期输入区域
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 开始日期
            DateInputField(
                label = "开始日期",
                value = startDate?.let { dateFormat.format(it) } ?: "不限",
                onClick = { showStartPicker = true },
                onClear = { onRangeChange(null, endDate) },
                hasValue = startDate != null,
                modifier = Modifier.weight(1f)
            )
            
            // 结束日期
            DateInputField(
                label = "结束日期",
                value = endDate?.let { dateFormat.format(it) } ?: "不限",
                onClick = { showEndPicker = true },
                onClear = { onRangeChange(startDate, null) },
                hasValue = endDate != null,
                modifier = Modifier.weight(1f)
            )
        }
    }
    
    // 开始日期选择器
    if (showStartPicker) {
        DatePickerDialogWrapper(
            initialDate = startDate,
            maxDate = endDate,
            onDateSelected = { date ->
                onRangeChange(date, endDate)
                showStartPicker = false
            },
            onDismiss = { showStartPicker = false }
        )
    }
    
    // 结束日期选择器
    if (showEndPicker) {
        DatePickerDialogWrapper(
            initialDate = endDate,
            minDate = startDate,
            onDateSelected = { date ->
                onRangeChange(startDate, date)
                showEndPicker = false
            },
            onDismiss = { showEndPicker = false }
        )
    }
}

/**
 * 快捷日期选项
 */
@Composable
private fun QuickDateOptions(
    onSelectRange: (Long?, Long?) -> Unit
) {
    val calendar = remember { Calendar.getInstance() }
    
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 最近 7 天
        item {
            SuggestionChip(
                onClick = {
                    val end = System.currentTimeMillis()
                    calendar.timeInMillis = end
                    calendar.add(Calendar.DAY_OF_MONTH, -7)
                    onSelectRange(calendar.timeInMillis, end)
                },
                label = { Text("最近7天") }
            )
        }
        
        // 最近 30 天
        item {
            SuggestionChip(
                onClick = {
                    val end = System.currentTimeMillis()
                    calendar.timeInMillis = end
                    calendar.add(Calendar.DAY_OF_MONTH, -30)
                    onSelectRange(calendar.timeInMillis, end)
                },
                label = { Text("最近30天") }
            )
        }
        
        // 本月
        item {
            SuggestionChip(
                onClick = {
                    calendar.timeInMillis = System.currentTimeMillis()
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val start = calendar.timeInMillis
                    
                    calendar.add(Calendar.MONTH, 1)
                    calendar.add(Calendar.MILLISECOND, -1)
                    val end = calendar.timeInMillis
                    
                    onSelectRange(start, end)
                },
                label = { Text("本月") }
            )
        }
        
        // 上月
        item {
            SuggestionChip(
                onClick = {
                    calendar.timeInMillis = System.currentTimeMillis()
                    calendar.add(Calendar.MONTH, -1)
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val start = calendar.timeInMillis
                    
                    calendar.add(Calendar.MONTH, 1)
                    calendar.add(Calendar.MILLISECOND, -1)
                    val end = calendar.timeInMillis
                    
                    onSelectRange(start, end)
                },
                label = { Text("上月") }
            )
        }
        
        // 今年
        item {
            SuggestionChip(
                onClick = {
                    calendar.timeInMillis = System.currentTimeMillis()
                    calendar.set(Calendar.MONTH, Calendar.JANUARY)
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val start = calendar.timeInMillis
                    
                    onSelectRange(start, System.currentTimeMillis())
                },
                label = { Text("今年") }
            )
        }
        
        // 清除
        item {
            SuggestionChip(
                onClick = { onSelectRange(null, null) },
                label = { Text("不限") }
            )
        }
    }
}

/**
 * 日期输入框
 */
@Composable
private fun DateInputField(
    label: String,
    value: String,
    onClick: () -> Unit,
    onClear: () -> Unit,
    hasValue: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { },
        label = { Text(label) },
        readOnly = true,
        singleLine = true,
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = {
            if (hasValue) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "清除",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        modifier = modifier.clickable { onClick() }
    )
}

/**
 * 日期选择器对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialogWrapper(
    initialDate: Long?,
    minDate: Long? = null,
    maxDate: Long? = null,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate
    )
    
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDate ->
                        // 验证日期范围
                        val afterMin = minDate?.let { selectedDate >= it } ?: true
                        val beforeMax = maxDate?.let { selectedDate <= it } ?: true
                        if (afterMin && beforeMax) {
                            onDateSelected(selectedDate)
                        }
                    }
                },
                enabled = datePickerState.selectedDateMillis != null
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
