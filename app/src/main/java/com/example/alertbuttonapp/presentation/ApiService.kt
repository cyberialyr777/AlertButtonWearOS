package com.example.alertbuttonapp.presentation

import android.location.Location
import retrofit2.Response
import retrofit2.http.*
import retrofit2.http.Body
import retrofit2.http.POST

// Interfaz para las peticiones HTTP
interface ApiService {
    // Alertas de emergencia
    @POST("alerts/emergency")
    suspend fun sendEmergencyAlert(@Body alert: EmergencyAlert): Response<AlertResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): retrofit2.Response<AuthResponse>
}


// Cliente Retrofit
object RetrofitClient {
    
    private val retrofit = retrofit2.Retrofit.Builder()
        .baseUrl(BackendConfig.BASE_URL)
        .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
        .build()
    
    val apiService: ApiService = retrofit.create(ApiService::class.java)
}

// Clase para manejar el envío de alertas
class AlertService {
    
    // Enviar alerta de emergencia con contactos
    suspend fun sendEmergencyAlert(location: Location?, contacts: List<EmergencyContact>): Result<AlertResponse> {
        return try {
            if (location == null) {
                return Result.failure(Exception("Location not available"))
            }
            
            val alert = EmergencyAlert(
                latitude = location.latitude,
                longitude = location.longitude,
                contacts = contacts
            )
            
            val response = RetrofitClient.apiService.sendEmergencyAlert(alert)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Empty response"))
            } else {
                Result.failure(Exception("Error sending alert"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

 