package com.pv239.beelocal.ui.screens.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.pv239.beelocal.R
import com.pv239.beelocal.ui.theme.BeelocalTheme

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val state = viewModel.uiState
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                RegisterEvent.Success -> onRegisterSuccess()
            }
        }
    }

    AuthScreenScaffold {
        AuthCard(
            title = stringResource(R.string.register_card_title),
            subtitle = stringResource(R.string.register_card_subtitle)
        ) {
            AuthTextField(
                value = username,
                onValueChange = { username = it },
                label = stringResource(R.string.register_username_label),
                placeholder = stringResource(R.string.register_username_placeholder),
                leadingIcon = Icons.Outlined.Person
            )
            Spacer(Modifier.height(16.dp))
            AuthTextField(
                value = email,
                onValueChange = { email = it },
                label = stringResource(R.string.register_email_label),
                placeholder = stringResource(R.string.register_email_placeholder),
                leadingIcon = Icons.Outlined.Email,
                keyboardType = KeyboardType.Email
            )
            Spacer(Modifier.height(16.dp))
            AuthTextField(
                value = password,
                onValueChange = { password = it },
                label = stringResource(R.string.register_password_label),
                placeholder = stringResource(R.string.register_password_placeholder),
                leadingIcon = Icons.Outlined.Lock,
                isPassword = true,
                keyboardType = KeyboardType.Password
            )
            Spacer(Modifier.height(16.dp))
            AuthTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = stringResource(R.string.register_confirm_password_label),
                placeholder = stringResource(R.string.register_confirm_password_placeholder),
                leadingIcon = Icons.Outlined.Lock,
                isPassword = true,
                keyboardType = KeyboardType.Password
            )
            Spacer(Modifier.height(16.dp))
            state.errorMessage?.let { msg ->
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
            }

            // --- Primary CTA ---
            Button(
                onClick = { viewModel.register(username, email, password, confirmPassword) },
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(percent = 50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    Text(
                        text = stringResource(R.string.register_cta_primary),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            AuthDivider()
            Spacer(Modifier.height(16.dp))

            // --- Secondary CTA: switch to Log In ---
            OutlinedButton(
                onClick = onNavigateToLogin,
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(percent = 50),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = null
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Login,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = stringResource(R.string.register_cta_login),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun RegisterScreenPreview() {
    BeelocalTheme {
        RegisterScreen(onRegisterSuccess = {}, onNavigateToLogin = {})
    }
}