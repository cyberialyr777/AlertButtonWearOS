package com.example.alertbuttonapp.presentation

import android.content.Context
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*

class MqttService(context: Context) {

    private val client = MqttAndroidClient(context, MqttConfig.BROKER_URL, MqttConfig.getClientId())

    fun connect(onSuccess: () -> Unit, onFailure: (Throwable?) -> Unit) {
        try {
            if (!client.isConnected) {
                val options = MqttConnectOptions()
                options.isCleanSession = true // Iniciar sesión limpia
                client.connect(options, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        onSuccess()
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        onFailure(exception)
                    }
                })
            } else {
                onSuccess()
            }
        } catch (e: MqttException) {
            onFailure(e)
        }
    }

    fun publish(topic: String, message: String) {
        if (client.isConnected) {
            try {
                val mqttMessage = MqttMessage()
                mqttMessage.payload = message.toByteArray()
                client.publish(topic, mqttMessage)
            } catch (e: MqttException) {
                e.printStackTrace()
            }
        }
    }

    fun disconnect() {
        if (client.isConnected) {
            try {
                client.disconnect()
            } catch (e: MqttException) {
                e.printStackTrace()
            }
        }
    }
}