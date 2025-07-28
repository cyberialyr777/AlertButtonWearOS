package com.example.alertbuttonapp.presentation

import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.util.Log
import com.google.android.gms.location.*
import com.google.gson.Gson
import kotlinx.coroutines.*

class LocationAlertService : Service() {

    private lateinit var locationService: LocationService
    private lateinit var mqttService: MqttService
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var isSendingAlerts = false

    override fun onCreate() {
        super.onCreate()
        locationService = LocationService(this)
        mqttService = MqttService(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START" -> startAlerts()
            "STOP" -> stopAlerts()
        }
        return START_STICKY
    }

    private fun startAlerts() {
        if (isSendingAlerts) return
        isSendingAlerts = true
        Log.d("LocationAlertService", "Iniciando envío de alertas.")

        // Conectar al broker MQTT
        mqttService.connect(
            onSuccess = {
                Log.d("LocationAlertService", "Conectado al broker MQTT.")
                // Una vez conectado, empezar a obtener la ubicación
                startLocationUpdates()
            },
            onFailure = {
                Log.e("LocationAlertService", "Error al conectar con MQTT", it)
                stopSelf() // Detener el servicio si no se puede conectar
            }
        )
    }

    private fun startLocationUpdates() {
        serviceScope.launch {
            while(isSendingAlerts) {
                val location = locationService.getCurrentLocation()
                if (location != null) {
                    val locationJson = Gson().toJson(
                        mapOf(
                            "latitude" to location.latitude,
                            "longitude" to location.longitude,
                            "timestamp" to System.currentTimeMillis()
                        )
                    )
                    Log.d("LocationAlertService", "Publicando ubicación: $locationJson")
                    mqttService.publish(MqttConfig.TOPIC, locationJson)
                } else {
                    Log.w("LocationAlertService", "No se pudo obtener la ubicación.")
                }
                // Esperar 10 segundos antes de la siguiente publicación
                delay(10000)
            }
        }
    }

    private fun stopAlerts() {
        isSendingAlerts = false
        serviceScope.cancel()
        mqttService.disconnect()
        Log.d("LocationAlertService", "Servicio de alertas detenido.")
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlerts()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // No necesitamos vincular el servicio
    }
}