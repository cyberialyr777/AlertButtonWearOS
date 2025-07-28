package com.example.alertbuttonapp.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.wear.compose.material.ScalingLazyColumn

@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val apiService = RetrofitClient.apiService

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // En AuthScreen.kt, reemplaza tu ScalingLazyColumn

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
    ) {
        item { Text("Iniciar Sesión", style = MaterialTheme.typography.title3) }

        item {
            // TextField para el Email (mejorado)
            BasicTextField(
                value = email,
                onValueChange = { email = it },
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)) {
                        if (email.isEmpty()) Text("Email", color = Color.Gray)
                        innerTextField()
                    }
                },
                textStyle = TextStyle(color = Color.White)
            )
        }
        item {
            // TextField para la Contraseña (mejorado)
            BasicTextField(
                value = password,
                onValueChange = { password = it },
                visualTransformation = PasswordVisualTransformation(),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)) {
                        if (password.isEmpty()) Text("Contraseña", color = Color.Gray)
                        innerTextField()
                    }
                },
                textStyle = TextStyle(color = Color.White)
            )
        }

        item {
            Button(
                onClick = {
                    isLoading = true
                    errorMessage = null
                    scope.launch {
                        try {
                            val response = apiService.login(LoginRequest(email, password))
                            if (response.isSuccessful && response.body() != null) {
                                val authResponse = response.body()!!
                                SessionManager.saveAuthToken(context, authResponse.accessToken)
                                SessionManager.saveUser(context, authResponse.user)
                                onLoginSuccess()
                            } else {
                                errorMessage = "Credenciales inválidas"
                            }
                        } catch (e: Exception) {
                            errorMessage = "Error de red: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading && email.isNotEmpty() && password.isNotEmpty()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Entrar")
                }
            }
        }

        item {
            if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colors.error, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}