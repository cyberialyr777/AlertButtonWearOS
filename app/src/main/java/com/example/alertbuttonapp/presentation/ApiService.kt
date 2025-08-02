package com.example.alertbuttonapp.presentation

import android.content.Context
import retrofit2.Response
import retrofit2.http.*

// La interfaz ApiService no cambia
interface ApiService {
    @POST("alerts/emergency")
    suspend fun sendEmergencyAlert(@Body alert: EmergencyAlert): Response<AlertResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): retrofit2.Response<AuthResponse>

    @GET("emergency-contacts/user/{userId}")
    suspend fun getContacts(@Path("userId") userId: String): Response<List<EmergencyContact>>

    @POST("emergency-contacts")
    suspend fun createContact(@Body contact: EmergencyContact): Response<EmergencyContact>

    @PATCH("emergency-contacts/{contactId}")
    suspend fun updateContact(@Path("contactId") contactId: String, @Body contact: EmergencyContact): Response<EmergencyContact>

    @DELETE("emergency-contacts/{contactId}")
    suspend fun deleteContact(@Path("contactId") contactId: String): Response<Unit>
}

// La clase AlertService no cambia
class AlertService {
    suspend fun sendEmergencyAlert(location: android.location.Location?, contacts: List<EmergencyContact>, context: Context): Result<AlertResponse> {
        return try {
            if (location == null) {
                return Result.failure(Exception("Location not available"))
            }
            val alert = EmergencyAlert(
                latitude = location.latitude,
                longitude = location.longitude,
                contacts = contacts
            )
            // Ahora obtenemos la instancia de la API pasando el contexto
            val response = RetrofitClient.getApiService(context).sendEmergencyAlert(alert)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Empty response"))
            } else {
                Result.failure(Exception("Error sending alert: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Objeto RetrofitClient (versión final y correcta)
object RetrofitClient {
    private var apiService: ApiService? = null

    fun getApiService(context: Context): ApiService {
        if (apiService == null) {
            val authInterceptor = AuthInterceptor(context.applicationContext)
            val okHttpClient = okhttp3.OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .build()
            val retrofit = retrofit2.Retrofit.Builder()
                .baseUrl(BackendConfig.BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                .build()
            apiService = retrofit.create(ApiService::class.java)
        }
        return apiService!!
    }
}