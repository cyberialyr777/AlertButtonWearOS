package com.example.alertbuttonapp.presentation

import com.google.gson.annotations.SerializedName

// Modelo de contacto de emergencia
data class EmergencyContact(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("phone")
    val phoneNumber: String,
    @SerializedName("email")
    val email: String? = null,
    @SerializedName("is_active")
    val isActive: Boolean = true
)

// Modelo de alerta de emergencia
data class EmergencyAlert(
    @SerializedName("latitude")
    val latitude: Double,
    @SerializedName("longitude")
    val longitude: Double,
    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    @SerializedName("contacts")
    val contacts: List<EmergencyContact>? = null
)

// Modelo de respuesta de alerta
data class AlertResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String,
    @SerializedName("alert_id")
    val alertId: String? = null,
    @SerializedName("contacts_notified")
    val contactsNotified: Int = 0
)

// Modelo de Usuario (según tu README_BACKEND.md)
data class User(
    @SerializedName("id")
    val id: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("phone")
    val phone: String? = null
)

// Modelo para la petición de Login
data class LoginRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String
)

// Modelo para la respuesta de autenticación (según tu README_BACKEND.md)
data class AuthResponse(
    @SerializedName("user")
    val user: User,
    @SerializedName("accessToken")
    val accessToken: String,
    @SerializedName("refreshToken")
    val refreshToken: String
)