package com.example.alertbuttonapp.presentation

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.alertbuttonapp.presentation.theme.AlertButtonAppTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Permissions granted - update UI
                updateUI()
            }
            else -> {
                // Permissions denied - show message
                Toast.makeText(
                    this,
                    "Location permissions are required to send alerts",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        updateUI()
    }
    
    private fun updateUI() {
        setContent {
            AppNavigation(
                onRequestLocationPermission = { requestLocationPermission() }
            )
        }
    }
    
    private fun requestLocationPermission() {
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
}

@Composable
fun EmergencyScreen(
    onRequestLocationPermission: () -> Unit = {},
    contacts: List<EmergencyContact> = emptyList(),
    onShowContacts: () -> Unit = {},
    onManageContacts: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationService = remember { LocationService(context) }
    val alertService = remember { AlertService() }
    
    var showConfirmation by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var isSendingAlert by remember { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(locationService.hasLocationPermission()) }
    var currentLocation by remember { mutableStateOf<String?>(null) }
    var permissionRequested by remember { mutableStateOf(false) }
    var alertResponse by remember { mutableStateOf<AlertResponse?>(null) }
    
    // Check permissions on startup
    LaunchedEffect(Unit) {
        if (!hasLocationPermission && !permissionRequested) {
            permissionRequested = true
            onRequestLocationPermission()
        }
    }
    
    // Get location when permissions are granted
    LaunchedEffect(Unit) {
        if (hasLocationPermission) {
            try {
                val location = locationService.getCurrentLocation()
                currentLocation = if (location != null) {
                    "${location.latitude}, ${location.longitude}"
                } else {
                    "Not available"
                }
            } catch (_: Exception) {
                currentLocation = "Error getting location"
            }
        }
    }
    
    if (showSuccess) {
        SuccessScreen(
            onBackToMain = {
                showSuccess = false
                showConfirmation = false
            },
            alertResponse = alertResponse
        )
    } else if (showConfirmation) {
        ConfirmationScreen(
            onConfirm = {
                showConfirmation = false
                scope.launch {
                    isSendingAlert = true
                    try {
                        // Use current location if we have it, or get a new one
                        val location = if (currentLocation != null && currentLocation != "Not available" && currentLocation != "Error getting location") {
                            // Parse current location
                            try {
                                val coords = currentLocation!!.split(", ")
                                if (coords.size == 2) {
                                    val lat = coords[0].toDouble()
                                    val lng = coords[1].toDouble()
                                    android.location.Location("").apply {
                                        latitude = lat
                                        longitude = lng
                                    }
                                } else null
                            } catch (_: Exception) {
                                null
                            }
                        } else {
                            locationService.getCurrentLocation()
                        }
                        
                        // Update displayed location
                        currentLocation = if (location != null) {
                            "${location.latitude}, ${location.longitude}"
                        } else {
                            "Not available"
                        }

                        val result = alertService.sendEmergencyAlert(location, contacts, context)
                        result.onSuccess { response ->
                            alertResponse = response
                            showSuccess = true
                        }.onFailure { error ->
                            Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        isSendingAlert = false
                    }
                }
            },
            onCancel = {
                showConfirmation = false
            }
        )
    } else {
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
                            text = "Emergency Button",
                            style = MaterialTheme.typography.title3,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        Text(
                            text = if (hasLocationPermission) {
                                "Press the button to send an alert with your location."
                            } else {
                                "Location permissions are required."
                            },
                            fontSize = 7.sp,
                            textAlign = TextAlign.Center,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }

                    item {
                                            Button(
                        onClick = {
                            if (hasLocationPermission && !isSendingAlert) {
                                showConfirmation = true
                            } else if (!hasLocationPermission) {
                                onRequestLocationPermission()
                            }
                        },
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (isSendingAlert) Color.Gray else Color(0xFFB75656)
                        ),
                        enabled = !isSendingAlert
                    ) {
                        Text(
                            text = if (isSendingAlert) "..." else if (!hasLocationPermission) "PERMISOS" else "SOS",
                            style = MaterialTheme.typography.title2,
                            color = Color.White
                        )
                    }
                    }

                                    item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Location: ${currentLocation ?: "Getting..."}",
                            fontSize = 8.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        
                        // Show contacts information
                        if (contacts.isNotEmpty()) {
                            Text(
                                text = "Contacts: ${contacts.size}",
                                fontSize = 7.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Botón para ver contactos
                if (contacts.isNotEmpty()) {
                    item {
                        Button(
                            onClick = onShowContacts,
                            modifier = Modifier.size(60.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color.Gray
                            )
                        ) {
                            Text(
                                text = "VIEW",
                                style = MaterialTheme.typography.body2,
                                color = Color.White,
                                fontSize = 6.sp
                            )
                        }
                    }
                }

                // Botón para gestionar contactos
                item {
                    Button(
                        onClick = onManageContacts,
                        modifier = Modifier.size(60.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.Blue
                        )
                    ) {
                        Text(
                            text = "MANAGE",
                            style = MaterialTheme.typography.body2,
                            color = Color.White,
                            fontSize = 6.sp
                        )
                    }
                }
                }
            }
        }
    }
}

@Composable
fun ConfirmationScreen(
    onConfirm: () -> Unit,
    onCancel: () -> Unit
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
                            text = "Confirm Alert",
                            style = MaterialTheme.typography.title3,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        Text(
                            text = "Are you sure you want to send an emergency alert?",
                            fontSize = 7.sp,
                            textAlign = TextAlign.Center,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }

                item {
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.size(70.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFFB75656)
                        )
                    ) {
                        Text(
                            text = "YES",
                            style = MaterialTheme.typography.title2,
                            color = Color.White
                        )
                    }
                }

                item {
                    Button(
                        onClick = onCancel,
                        modifier = Modifier.size(70.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.Gray
                        )
                    ) {
                        Text(
                            text = "NO",
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
fun SuccessScreen(
    onBackToMain: () -> Unit,
    alertResponse: AlertResponse? = null
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
                            text = "✅ Alert Sent",
                            style = MaterialTheme.typography.title3,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            color = Color.Green,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        Text(
                            text = "Your emergency alert has been sent successfully with your location.",
                            fontSize = 7.sp,
                            textAlign = TextAlign.Center,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        
                        // Mostrar información del backend si está disponible
                        alertResponse?.let { response ->
                            Text(
                                text = "ID: ${response.alertId ?: "N/A"}",
                                fontSize = 6.sp,
                                textAlign = TextAlign.Center,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                            Text(
                                text = "Contacts notified: ${response.contactsNotified}",
                                fontSize = 6.sp,
                                textAlign = TextAlign.Center,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                item {
                    Button(
                        onClick = onBackToMain,
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFFB75656)
                        )
                    ) {
                        Text(
                            text = "OK",
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
fun AppNavigation(
    onRequestLocationPermission: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope() // CoroutineScope para lanzar las llamadas a la API
    val apiService = RetrofitClient.getApiService(context)

    val initialScreen = if (SessionManager.getAuthToken(context) != null) Screen.Auth else Screen.Auth
    var currentScreen by remember { mutableStateOf(initialScreen) }

    var contacts by remember { mutableStateOf<List<EmergencyContact>>(emptyList()) }
    var editingContact by remember { mutableStateOf<EmergencyContact?>(null) }

    // Carga los contactos del backend cuando estás en una pantalla que los necesita
    LaunchedEffect(currentScreen) {
        if (currentScreen == Screen.Emergency || currentScreen == Screen.Contacts || currentScreen == Screen.ContactManager) {
            val userId = SessionManager.getUserId(context)
            if (userId != null) {
                try {
                    val response = apiService.getContacts(userId)
                    if (response.isSuccessful) {
                        contacts = response.body() ?: emptyList()
                    } else {
                        val errorCode = response.code()
                        val errorMessage = response.errorBody()?.string() ?: "Unknown error"
                        Toast.makeText(context, "Error $errorCode: $errorMessage", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Connection error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Navegación y lógica principal de la aplicación
    when (currentScreen) {
        Screen.Auth -> {
            AuthScreen(onLoginSuccess = { currentScreen = Screen.Emergency })
        }

        Screen.Emergency -> {
            EmergencyScreen(
                onRequestLocationPermission = onRequestLocationPermission,
                contacts = contacts,
                onShowContacts = { currentScreen = Screen.Contacts },
                onManageContacts = { currentScreen = Screen.ContactManager }
            )
        }

        Screen.Contacts -> {
            ContactsScreen(
                contacts = contacts,
                onBackToMain = { currentScreen = Screen.Emergency }
            )
        }

        Screen.ContactManager -> {
            ContactManagerScreen(
                contacts = contacts,
                onAddContact = {
                    editingContact = null
                    currentScreen = Screen.ContactEdit
                },
                onEditContact = { contact ->
                    editingContact = contact
                    currentScreen = Screen.ContactEdit
                },
                onDeleteContact = { contactToDelete ->
                    scope.launch {
                        try {
                            val response = apiService.deleteContact(contactToDelete.id)
                            if (response.isSuccessful) {
                                // Si se borra en el backend, actualiza la lista local
                                contacts = contacts.filter { it.id != contactToDelete.id }
                                Toast.makeText(context, "Contact deleted", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Error deleting: ${response.code()}", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onBackToMain = { currentScreen = Screen.Emergency }
            )
        }

        Screen.ContactEdit -> {
            ContactEditScreen(
                contact = editingContact,
                onSave = { contactToSave ->
                    scope.launch {
                        try {
                            // Si 'editingContact' es null, es un contacto nuevo
                            if (editingContact == null) {
                                val response = apiService.createContact(contactToSave)
                                if (response.isSuccessful && response.body() != null) {
                                    val newContact = response.body()!!
                                    contacts = contacts + newContact // Añade el nuevo contacto a la lista
                                    Toast.makeText(context, "Contact added", Toast.LENGTH_SHORT).show()
                                    currentScreen = Screen.ContactManager // Vuelve a la lista
                                } else {
                                    Toast.makeText(context, "Error adding: ${response.code()}", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                // Si no, es una edición de un contacto existente
                                val response = apiService.updateContact(contactToSave.id, contactToSave)
                                if (response.isSuccessful && response.body() != null) {
                                    val updatedContact = response.body()!!
                                    // Reemplaza el contacto viejo con el actualizado
                                    contacts = contacts.map { if (it.id == updatedContact.id) updatedContact else it }
                                    Toast.makeText(context, "Contact updated", Toast.LENGTH_SHORT).show()
                                    currentScreen = Screen.ContactManager // Vuelve a la lista
                                } else {
                                    Toast.makeText(context, "Error updating: ${response.code()}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onCancel = {
                    currentScreen = Screen.ContactManager
                }
            )
        }
    }
}

// Enum for screens
enum class Screen {
    Auth,
    Emergency,
    Contacts,
    ContactManager,
    ContactEdit
}
@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    EmergencyScreen(
        onRequestLocationPermission = {},
        contacts = emptyList(),
        onShowContacts = {},
        onManageContacts = {}
    )
}