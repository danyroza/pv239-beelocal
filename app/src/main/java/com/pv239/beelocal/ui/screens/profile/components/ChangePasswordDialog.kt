package com.pv239.beelocal.ui.screens.profile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.pv239.beelocal.R
import com.pv239.beelocal.ui.screens.profile.PasswordDialogState

@Composable
fun ChangePasswordDialog(
    state: PasswordDialogState,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!state.isLoading) onDismiss() },
        title = {
            Text(
                text = stringResource(R.string.password_dialog_change_password),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PasswordField(
                    value = state.currentPassword,
                    onValueChange = onCurrentPasswordChange,
                    label = stringResource(R.string.password_dialog_current_password),
                    enabled = !state.isLoading
                )
                PasswordField(
                    value = state.newPassword,
                    onValueChange = onNewPasswordChange,
                    label = stringResource(R.string.password_dialog_new_password_min_8_chars),
                    enabled = !state.isLoading
                )
                PasswordField(
                    value = state.confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    label = stringResource(R.string.password_dialog_confirm_new_password),
                    enabled = !state.isLoading,
                    isError = state.confirmPassword.isNotBlank() && state.newPassword != state.confirmPassword,
                    supportingText = if (state.confirmPassword.isNotBlank() && state.newPassword != state.confirmPassword)
                        stringResource(R.string.password_dialog_passwords_dont_match) else null
                )
                if (state.error != null) {
                    Text(
                        text = state.error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = state.isValid && !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.password_dialog_update))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isLoading) {
                Text(stringResource(R.string.password_dialog_cancel))
            }
        }
    )
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    isError: Boolean = false,
    supportingText: String? = null,
) {
    var visible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) stringResource(R.string.password_dialog_hide_password) else stringResource(R.string.password_dialog_show_password)
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}