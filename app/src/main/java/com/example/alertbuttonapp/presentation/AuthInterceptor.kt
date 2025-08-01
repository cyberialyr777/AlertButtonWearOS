package com.example.alertbuttonapp.presentation

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response

// Este interceptor añade la cabecera de autorización a cada solicitud.
class AuthInterceptor(private val context: Context) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        // Obtenemos el token que guardamos durante el login.
        val token = SessionManager.getAuthToken(context)

        val requestBuilder = chain.request().newBuilder()

        // Si el token existe, lo añadimos a la cabecera 'Authorization'.
        // El formato "Bearer <token>" es un estándar muy común.
        token?.let {
            requestBuilder.addHeader("Authorization", "Bearer $it")
        }

        // La solicitud continúa su camino, pero ahora con el token incluido.
        return chain.proceed(requestBuilder.build())
    }
}