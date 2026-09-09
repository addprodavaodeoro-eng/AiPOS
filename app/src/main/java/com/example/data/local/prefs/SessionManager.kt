package com.example.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aipos_session")

class SessionManager(private val context: Context) {

    companion object {
        val KEY_USER_ID = stringPreferencesKey("logged_in_user_id")
        val KEY_STORE_ID = stringPreferencesKey("logged_in_store_id")
    }

    val userIdFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_ID]
    }
    
    val storeIdFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_STORE_ID]
    }

    suspend fun createSession(userId: String, storeId: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USER_ID] = userId
            prefs[KEY_STORE_ID] = storeId
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_USER_ID)
            prefs.remove(KEY_STORE_ID)
        }
    }
}
