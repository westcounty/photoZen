package com.example.photozen.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.photozen.domain.model.FilterPreset

/**
 * 保存预设对话框
 * 
 * @param onConfirm 确认保存回调，参数为预设名称
 * @param onDismiss 取消回调
 */
@Composable
fun SavePresetDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var presetName by remember { mutableStateOf("") }
    val isValid = presetName.trim().isNotEmpty()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Save,
                contentDescription = null
            )
        },
        title = {
            Text("保存为预设")
        },
        text = {
            Column {
                Text(
                    text = "为当前筛选条件取一个名称，方便下次快速使用。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { 
                        if (it.length <= FilterPreset.MAX_NAME_LENGTH) {
                            presetName = it
                        }
                    },
                    label = { Text("预设名称") },
                    placeholder = { Text("例如：上月相机照片") },
                    singleLine = true,
                    supportingText = {
                        Text("${presetName.length}/${FilterPreset.MAX_NAME_LENGTH}")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(presetName.trim()) },
                enabled = isValid
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
