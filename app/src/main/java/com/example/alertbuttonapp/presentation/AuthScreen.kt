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
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn

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

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        item { Text("Iniciar Sesión", style = MaterialTheme.typography.title3) }

        item {
            // Este es un TextField muy básico. En una app real, podrías usar un
            // InputChip para abrir un teclado en pantalla completa.
            BasicTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.padding(8.dp)
            )
        }
        item {
            BasicTextField(
                value = password,
                onValueChange = { password = it },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.padding(8.dp)
            )
        }

        item {
            Button(
                onClick = {
                    isLoading = true
                    errorMessage = null
                    // En AuthScreen.kt, dentro del onClick del Button

                    scope.launch {
                        try {
                            val response = apiService.login(LoginRequest(email, password))
                            if (response.isSuccessful && response.body() != null) {
                                val authResponse = response.body()!!

                                // Guardar el token y los datos del usuario
                                SessionManager.saveAuthToken(context, authResponse.accessToken)
                                SessionManager.saveUser(context, authResponse.user)

                                // Navegar a la siguiente pantalla
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
                enabled = !isLoading
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