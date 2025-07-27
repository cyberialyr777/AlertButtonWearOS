package com.example.alertbuttonapp.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import androidx.wear.compose.material.TimeText
import com.example.alertbuttonapp.presentation.theme.AlertButtonAppTheme

@Composable
fun ContactEditScreen(
    contact: EmergencyContact? = null,
    onSave: (EmergencyContact) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(contact?.name ?: "") }
    var phone by remember { mutableStateOf(contact?.phoneNumber ?: "") }
    var email by remember { mutableStateOf(contact?.email ?: "") }
    
    AlertButtonAppTheme {
        val listState = rememberScalingLazyListState()

        Scaffold(
            timeText = { TimeText(modifier = Modifier.padding(top = 4.dp)) },
            positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
        ) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                autoCentering = AutoCenteringParams(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp),
            ) {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (contact == null) "Add Contact" else "Edit Contact",
                            style = MaterialTheme.typography.title3,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Campo de nombre
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Name:",
                            fontSize = 8.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Text(
                            text = name.ifEmpty { "Enter name" },
                            fontSize = 9.sp,
                            color = if (name.isNotEmpty()) Color.White else Color.Gray,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                // Campo de teléfono
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Phone:",
                            fontSize = 8.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Text(
                            text = phone.ifEmpty { "Enter phone" },
                            fontSize = 9.sp,
                            color = if (phone.isNotEmpty()) Color.White else Color.Gray,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                // Campo de email (opcional)
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Email (optional):",
                            fontSize = 8.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Text(
                            text = email.ifEmpty { "Enter email" },
                            fontSize = 9.sp,
                            color = if (email.isNotEmpty()) Color.White else Color.Gray,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                // Botón guardar
                item {
                    Button(
                        onClick = {
                            if (name.isNotEmpty() && phone.isNotEmpty()) {
                                val newContact = EmergencyContact(
                                    id = contact?.id ?: java.util.UUID.randomUUID().toString(),
                                    name = name,
                                    phoneNumber = phone,
                                    email = email.ifEmpty { null },
                                    isActive = true
                                )
                                onSave(newContact)
                            }
                        },
                        modifier = Modifier.size(70.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (name.isNotEmpty() && phone.isNotEmpty()) Color.Green else Color.Gray
                        ),
                        enabled = name.isNotEmpty() && phone.isNotEmpty()
                    ) {
                        Text(
                            text = "SAVE",
                            style = MaterialTheme.typography.title2,
                            color = Color.White,
                            fontSize = 8.sp
                        )
                    }
                }

                // Botón cancelar
                item {
                    Button(
                        onClick = onCancel,
                        modifier = Modifier.size(70.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFFB75656)
                        )
                    ) {
                        Text(
                            text = "CANCEL",
                            style = MaterialTheme.typography.title2,
                            color = Color.White,
                            fontSize = 8.sp
                        )
                    }
                }
            }
        }
    }
} 