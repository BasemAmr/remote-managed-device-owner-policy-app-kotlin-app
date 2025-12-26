package com.selfcontrol.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight

/**
 * Reusable confirmation dialog component
 * 
 * @param title Dialog title
 * @param message Dialog message/description
 * @param confirmText Text for confirm button (default: "Confirm")
 * @param dismissText Text for dismiss button (default: "Cancel")
 * @param icon Optional icon to display
 * @param isDestructive If true, uses error colors for confirm button
 * @param onConfirm Callback when user confirms
 * @param onDismiss Callback when user dismisses
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    icon: ImageVector? = Icons.Default.Warning,
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = if (isDestructive) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.primary
                )
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = if (isDestructive) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}

/**
 * Simplified confirmation dialog for destructive actions (like delete, block)
 */
@Composable
fun DestructiveConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmDialog(
        title = title,
        message = message,
        confirmText = "Confirm",
        dismissText = "Cancel",
        isDestructive = true,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

/**
 * Simplified confirmation dialog for non-destructive actions
 */
@Composable
fun SimpleConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmDialog(
        title = title,
        message = message,
        confirmText = "OK",
        dismissText = "Cancel",
        isDestructive = false,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}
