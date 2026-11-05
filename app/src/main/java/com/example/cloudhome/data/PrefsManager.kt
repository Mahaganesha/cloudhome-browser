package com.example.cloudhome.data

import android.content.Context

/**
 * Simple persistence for the server config so the user does not have to
 * retype it on every launch.
 *
 * NOTE: this stores the password in plain SharedPreferences. For a real app,
 * switch to EncryptedSharedPreferences (androidx.security:security-crypto) or
 * store credentials in the Android Keystore.
 */
class PrefsManager(context: Context) {

    private val prefs = context.getSharedPreferences("cloudhome_prefs", Context.MODE_PRIVATE)

    fun load(): ServerConfig? {
        val host = prefs.getString("host", null) ?: return null
        val user = prefs.getString("user", null) ?: return null
        val pass = prefs.getString("pass", null) ?: return null
        val share = prefs.getString("share", null) ?: return null
        return ServerConfig(host, user, pass, share)
    }

    fun save(c: ServerConfig) {
        prefs.edit()
            .putString("host", c.host)
            .putString("user", c.username)
            .putString("pass", c.password)
            .putString("share", c.share)
            .apply()
    }
}
