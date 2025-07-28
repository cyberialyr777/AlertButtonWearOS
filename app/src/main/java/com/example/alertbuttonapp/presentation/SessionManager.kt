package com.example.alertbuttonapp.presentation

import android.content.Context
import android.content.SharedPreferences

object SessionManager {
    private const val PREF_NAME = "AlertButtonPrefs"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_NAME = "user_name"


    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // Guardar el token de acceso
    fun saveAuthToken(context: Context, token: String) {
        getSharedPreferences(context).edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }

    // Obtener el token de acceso
    fun getAuthToken(context: Context): String? {
        return getSharedPreferences(context).getString(KEY_ACCESS_TOKEN, null)
    }

    // Guardar los datos del usuario
    fun saveUser(context: Context, user: User) {
        val editor = getSharedPreferences(context).edit()
        editor.putString(KEY_USER_ID, user.id)
        editor.putString(KEY_USER_EMAIL, user.email)
        editor.putString(KEY_USER_NAME, user.name)
        editor.apply()
    }

    // Obtener el ID del usuario
    fun getUserId(context: Context): String? {
        return getSharedPreferences(context).getString(KEY_USER_ID, null)
    }

    // Limpiar toda la sesión (para cerrar sesión)
    fun clearSession(context: Context) {
        getSharedPreferences(context).edit().clear().apply()
    }
}