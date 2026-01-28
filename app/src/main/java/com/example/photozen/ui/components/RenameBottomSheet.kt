package com.example.photozen.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.example.photozen.data.local.entity.PhotoEntity
import com.example.photozen.util.GeoLocationResolver
import com.example.photozen.util.LocationDetails

/**
 * 重命名构造器 BottomSheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenameBottomSheet(
    photos: List<PhotoEntity>,
    onDismiss: () -> Unit,
    onConfirm: (RenameTemplate) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Template state
    var elements by remember { mutableStateOf<List<RenameElement>>(emptyList()) }
    var separator by remember { mutableStateOf(Separator.UNDERSCORE) }

    // Element config dialogs
    var showCustomTextInput by remember { mutableStateOf(false) }
    var showGeoConfig by remember { mutableStateOf(false) }
    var showIncrementConfig by remember { mutableStateOf(false) }
    var showDateConfig by remember { mutableStateOf(false) }
    var showSeparatorPicker by remember { mutableStateOf(false) }

    // Async geo resolve for preview
    val context = LocalContext.current
    val geoResolver = remember { GeoLocationResolver.from(context) }
    var previewLocation by remember { mutableStateOf<LocationDetails?>(null) }
    val hasGeoElement = elements.any { it is RenameElement.GeoLocation }
    val firstPhoto = photos.firstOrNull()

    LaunchedEffect(hasGeoElement, firstPhoto?.id) {
        previewLocation = if (hasGeoElement && firstPhoto != null) {
            geoResolver.resolveDetails(firstPhoto)
        } else null
    }

    // Preview
    val template = RenameTemplate(elements, separator)
    val previewName = if (photos.isNotEmpty() && elements.isNotEmpty()) {
        val name = generateNewName(photos.first(), template, 0, photos.size, previewLocation)
        val ext = getPhotoExtension(photos.first().displayName)
        sanitizeFileName(name) + ext
    } else {
        photos.firstOrNull()?.displayName ?: "photo.jpg"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Title
            Text(
                text = "重命名 (${photos.size}张照片)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Preview card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "文件名预览",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = previewName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Added elements
            if (elements.isNotEmpty()) {
                Text(
                    text = "已添加元素",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Element chips with drag handle and delete
                Column(
                    modifier = Modifier.animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    elements.forEachIndexed { index, element ->
                        ElementChip(
                            element = element,
                            canMoveUp = index > 0,
                            canMoveDown = index < elements.size - 1,
                            onMoveUp = {
                                val mutable = elements.toMutableList()
                                val item = mutable.removeAt(index)
                                mutable.add(index - 1, item)
                                elements = mutable
                            },
                            onMoveDown = {
                                val mutable = elements.toMutableList()
                                val item = mutable.removeAt(index)
                                mutable.add(index + 1, item)
                                elements = mutable
                            },
                            onRemove = {
                                elements = elements.toMutableList().apply { removeAt(index) }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Separator selector
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "连接符:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box {
                        FilledTonalButton(onClick = { showSeparatorPicker = true }) {
                            Text(separator.label)
                        }
                        DropdownMenu(
                            expanded = showSeparatorPicker,
                            onDismissRequest = { showSeparatorPicker = false }
                        ) {
                            Separator.entries.forEach { sep ->
                                DropdownMenuItem(
                                    text = { Text(sep.label) },
                                    onClick = {
                                        separator = sep
                                        showSeparatorPicker = false
                                    },
                                    leadingIcon = if (sep == separator) {
                                        {
                                            Icon(
                                                Icons.Default.DragHandle,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Add element section
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "添加元素",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Add element buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AddElementButton(
                    label = "自定义文本",
                    onClick = { showCustomTextInput = true },
                    modifier = Modifier.weight(1f)
                )
                AddElementButton(
                    label = "地理位置",
                    onClick = { showGeoConfig = true },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AddElementButton(
                    label = "自增序号",
                    onClick = { showIncrementConfig = true },
                    modifier = Modifier.weight(1f)
                )
                AddElementButton(
                    label = "照片日期",
                    onClick = { showDateConfig = true },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("取消")
                }
                Button(
                    onClick = { onConfirm(template) },
                    modifier = Modifier.weight(1f),
                    enabled = elements.isNotEmpty()
                ) {
                    Text("开始重命名")
                }
            }
        }
    }

    // ---- Element config dialogs ----

    if (showCustomTextInput) {
        CustomTextDialog(
            onDismiss = { showCustomTextInput = false },
            onConfirm = { text ->
                elements = elements + RenameElement.CustomText(text)
                showCustomTextInput = false
            }
        )
    }

    if (showGeoConfig) {
        GeoConfigDialog(
            onDismiss = { showGeoConfig = false },
            onConfirm = { mode, separator ->
                elements = elements + RenameElement.GeoLocation(mode, separator)
                showGeoConfig = false
            }
        )
    }

    if (showIncrementConfig) {
        IncrementConfigDialog(
            onDismiss = { showIncrementConfig = false },
            onConfirm = { startValue, stepValue ->
                elements = elements + RenameElement.AutoIncrement(startValue, stepValue)
                showIncrementConfig = false
            }
        )
    }

    if (showDateConfig) {
        DateConfigDialog(
            onDismiss = { showDateConfig = false },
            onConfirm = { fields, format ->
                elements = elements + RenameElement.PhotoDate(fields, format)
                showDateConfig = false
            }
        )
    }
}

// ============== Sub-components ==============

@Composable
private fun ElementChip(
    element: RenameElement,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    val label = when (element) {
        is RenameElement.CustomText -> "文本: ${element.text}"
        is RenameElement.GeoLocation -> {
            val sep = if (element.mode == GeoMode.PROVINCE_CITY) " [${element.separator.label}]" else ""
            "地理位置: ${element.mode.label}$sep"
        }
        is RenameElement.AutoIncrement -> "序号：从${element.startValue}开始"
        is RenameElement.PhotoDate -> {
            val fieldLabels = element.fields.sortedBy { it.ordinal }.joinToString("") { it.label }
            "日期: $fieldLabels"
        }
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Move buttons
            Column {
                IconButton(
                    onClick = onMoveUp,
                    enabled = canMoveUp,
                    modifier = Modifier.size(24.dp)
                ) {
                    Text("▲", style = MaterialTheme.typography.labelSmall)
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = canMoveDown,
                    modifier = Modifier.size(24.dp)
                ) {
                    Text("▼", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "移除",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun AddElementButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

// ============== Config Dialogs ==============

@Composable
private fun CustomTextDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义文本") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("输入文本") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank()
            ) { Text("添加") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun GeoConfigDialog(
    onDismiss: () -> Unit,
    onConfirm: (GeoMode, GeoSeparator) -> Unit
) {
    var selectedMode by remember { mutableStateOf(GeoMode.PROVINCE_CITY) }
    var selectedSeparator by remember { mutableStateOf(GeoSeparator.UNDERSCORE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("地理位置") },
        text = {
            Column {
                Text(
                    "根据照片 GPS 信息匹配省市（无 GPS 数据则跳过）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text("位置粒度", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                GeoMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedMode = mode }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedMode == mode,
                            onClick = { selectedMode = mode }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(mode.label)
                    }
                }

                // 省市连接符（仅省份+城市模式显示）
                if (selectedMode == GeoMode.PROVINCE_CITY) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("省市连接符", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    GeoSeparator.entries.forEach { sep ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedSeparator = sep }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedSeparator == sep,
                                onClick = { selectedSeparator = sep }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(sep.label)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedMode, selectedSeparator) }) { Text("添加") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun IncrementConfigDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var startValue by remember { mutableStateOf("1") }
    var stepValue by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自增序号") },
        text = {
            Column {
                OutlinedTextField(
                    value = startValue,
                    onValueChange = { startValue = it.filter { c -> c.isDigit() } },
                    label = { Text("起始值") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = stepValue,
                    onValueChange = { stepValue = it.filter { c -> c.isDigit() } },
                    label = { Text("步进值") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(startValue.toIntOrNull() ?: 1, stepValue.toIntOrNull() ?: 1) }
            ) { Text("添加") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DateConfigDialog(
    onDismiss: () -> Unit,
    onConfirm: (Set<DateField>, DateFormat) -> Unit
) {
    var selectedFields by remember {
        mutableStateOf(setOf(DateField.YEAR, DateField.MONTH, DateField.DAY))
    }
    var selectedFormat by remember { mutableStateOf(DateFormat.CHINESE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("照片日期") },
        text = {
            Column {
                Text(
                    "取拍摄时间，无拍摄时间则取创建时间",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text("选择字段", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DateField.entries.forEach { field ->
                        FilterChip(
                            selected = field in selectedFields,
                            onClick = {
                                selectedFields = if (field in selectedFields)
                                    selectedFields - field
                                else
                                    selectedFields + field
                            },
                            label = { Text(field.label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("日期格式", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                DateFormat.entries.forEach { format ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedFormat = format }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedFormat == format,
                            onClick = { selectedFormat = format }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(format.label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedFields, selectedFormat) },
                enabled = selectedFields.isNotEmpty()
            ) { Text("添加") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

/**
 * 重命名进度对话框
 */
@Composable
fun RenameProgressDialog(
    current: Int,
    total: Int,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* 不允许点击外部关闭 */ },
        title = { Text("正在重命名...") },
        text = {
            Column {
                LinearProgressIndicator(
                    progress = { if (total > 0) current.toFloat() / total else 0f },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "$current / $total",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onCancel) { Text("取消") }
        }
    )
}

/**
 * 重命名结果对话框
 */
@Composable
fun RenameResultDialog(
    successCount: Int,
    failCount: Int,
    failReasons: List<String> = emptyList(),
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (failCount == 0) "重命名完成" else "重命名结果")
        },
        text = {
            Column {
                if (failCount == 0) {
                    Text("已成功重命名 $successCount 张照片")
                } else {
                    Text("成功 $successCount 张，失败 $failCount 张")
                    if (failReasons.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "失败原因：",
                            style = MaterialTheme.typography.labelMedium
                        )
                        failReasons.take(5).forEach { reason ->
                            Text(
                                "• $reason",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        if (failReasons.size > 5) {
                            Text(
                                "... 还有 ${failReasons.size - 5} 个错误",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("确定") }
        }
    )
}
