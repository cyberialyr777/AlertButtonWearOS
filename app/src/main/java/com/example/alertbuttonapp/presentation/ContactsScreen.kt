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
fun ContactsScreen(
    contacts: List<EmergencyContact>,
    onBackToMain: () -> Unit
) {
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
                            text = "Emergency Contacts",
                            style = MaterialTheme.typography.title3,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        Text(
                            text = "${contacts.size} contacts synced",
                            fontSize = 7.sp,
                            textAlign = TextAlign.Center,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }

                // Mostrar contactos
                contacts.forEach { contact ->
                    item {
                        ContactItem(contact = contact)
                    }
                }

                item {
                    Button(
                        onClick = onBackToMain,
                        modifier = Modifier.size(70.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFFB75656)
                        )
                    ) {
                        Text(
                            text = "BACK",
                            style = MaterialTheme.typography.title2,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContactItem(contact: EmergencyContact) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Text(
            text = contact.name,
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            color = if (contact.isActive) Color.White else Color.Gray
        )
        Text(
            text = contact.phoneNumber,
            fontSize = 7.sp,
            textAlign = TextAlign.Center,
            color = Color.Gray
        )
    }
} 