package com.niezhili.focusflow.ui.task.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * DeleteConfirmDialog - 删除确认弹窗
 *
 * 确认删除任务前的提示对话框
 */
@Composable
fun DeleteConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("确定删除此任务吗？")
        },
        text = {
            Text("任务及其关联的专注计时记录将被永久删除，此操作不可撤销")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("确定删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}