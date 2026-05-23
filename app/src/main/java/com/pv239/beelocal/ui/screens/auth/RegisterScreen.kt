package com.pv239.beelocal.ui.screens.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pv239.beelocal.ui.theme.BeelocalTheme

/**
 * Registration screen — visual mockup only.
 *
 * Captures username, email, and password. `onRegisterSuccess` is fired on the
 * primary CTA so the navigation flow can advance; real Firebase auth wiring
 * will replace the stub later.
 */
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    AuthScreenScaffold {
        AuthCard(
            title = "Create your account",
            subtitle = "Start exploring your local community."
        ) {
            AuthTextField(
                value = username,
                onValueChange = { username = it },
                label = "Username",
                placeholder = "honey_bee_23",
                leadingIcon = Icons.Outlined.Person
            )
            Spacer(Modifier.height(16.dp))
            AuthTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email address",
                placeholder = "you@hive.com",
                leadingIcon = Icons.Outlined.Email,
                keyboardType = KeyboardType.Email
            )
            Spacer(Modifier.height(16.dp))
            AuthTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                placeholder = "••••••••",
                leadingIcon = Icons.Outlined.Lock,
                isPassword = true,
                keyboardType = KeyboardType.Password
            )
            Spacer(Modifier.height(24.dp))

            // --- Primary CTA ---
            Button(
                onClick = onRegisterSuccess,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(percent = 50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Text(
                    text = "Get Started",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(16.dp))

            AuthDivider()
            Spacer(Modifier.height(16.dp))

            // --- Secondary CTA: switch to Log In ---
            OutlinedButton(
                onClick = onNavigateToLogin,
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
                    text = "I already have an account",
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
