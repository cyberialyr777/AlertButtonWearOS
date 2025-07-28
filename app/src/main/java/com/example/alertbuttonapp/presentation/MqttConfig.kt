package com.example.alertbuttonapp.presentation

object MqttConfig {
    // IMPORTANTE: Cambia esto por la URL de tu broker MQTT
    // Ejemplo: "tcp://broker.hivemq.com:1883"
    const val BROKER_URL = "tcp://TU_BROKER_URL:1883"

    // El topic donde se publicará la ubicación
    const val TOPIC = "alert/location"

    // Un ID de cliente único. Se puede generar dinámicamente.
    fun getClientId(): String {
        return "wearos-alert-button-" + System.currentTimeMillis()
    }
}