package com.example.smartwaste.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartwaste.auth.FirebaseAuthService
import com.example.smartwaste.ui.theme.GreenDark
import com.example.smartwaste.ui.theme.GreenPrimary
import kotlinx.coroutines.launch

@Composable
fun MainLoginScreen(onLoginSuccess: (String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val authService = remember { FirebaseAuthService() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "SmartWaste Login",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = GreenDark
        )
        Spacer(Modifier.height(48.dp))
        
        LoginTextField(
            value = email,
            onValueChange = { email = it; errorMessage = "" },
            label = "Email",
            icon = Icons.Default.Email
        )
        Spacer(Modifier.height(16.dp))
        
        LoginTextField(
            value = password,
            onValueChange = { password = it; errorMessage = "" },
            label = "Password",
            icon = Icons.Default.Lock,
            isPassword = true
        )
        
        if (errorMessage.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(errorMessage, color = Color.Red, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
        }
        
        Spacer(Modifier.height(32.dp))
        
        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    errorMessage = ""
                    val result = authService.login(email, password)
                    result.onSuccess { user ->
                        isLoading = false
                        Log.d("MainLoginScreen", "Login successful for: ${user.email}")
                        onLoginSuccess(user.email ?: "")
                    }
                    result.onFailure { exception ->
                        isLoading = false
                        errorMessage = exception.message ?: "Login failed"
                        Log.e("MainLoginScreen", "Login error: ${exception.message}")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading && email.isNotEmpty() && password.isNotEmpty()
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Login", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LoginTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    isPassword: Boolean = false
) {
    var passwordVisible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = GreenPrimary) },
        trailingIcon = {
            if (isPassword) {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = GreenPrimary
                    )
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GreenPrimary,
            focusedLabelColor = GreenPrimary
        )
    )
}
