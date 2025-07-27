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