package com.sanathanatattva.partner.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sanathanatattva.partner.data.api.User
import com.sanathanatattva.partner.data.api.apiJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Two files on purpose: settings may be backed up, the session never is (res/xml/*backup*).
private val Context.settingsData: DataStore<Preferences> by preferencesDataStore("settings")
private val Context.sessionData: DataStore<Preferences> by preferencesDataStore("session")

data class Settings(
    val languageChosen: Boolean = false,
    val introSeen: Boolean = false,
    val voiceOn: Boolean = true,
)

class SettingsStore(private val context: Context) {
    private object Keys {
        val languageChosen = booleanPreferencesKey("language_chosen")
        val introSeen = booleanPreferencesKey("intro_seen")
        val voiceOn = booleanPreferencesKey("voice_on")
    }

    val settings: Flow<Settings> = context.settingsData.data.map {
        Settings(
            languageChosen = it[Keys.languageChosen] ?: false,
            introSeen = it[Keys.introSeen] ?: false,
            voiceOn = it[Keys.voiceOn] ?: true,
        )
    }

    suspend fun current(): Settings = settings.first()

    suspend fun setLanguageChosen() = context.settingsData.edit { it[Keys.languageChosen] = true }
    suspend fun setIntroSeen() = context.settingsData.edit { it[Keys.introSeen] = true }
    suspend fun setVoiceOn(on: Boolean) = context.settingsData.edit { it[Keys.voiceOn] = on }
}

data class Session(val token: String, val user: User)

class SessionStore(private val context: Context) {
    private object Keys {
        val token = stringPreferencesKey("token")
        val user = stringPreferencesKey("user")
    }

    /** Read by the HTTP interceptor, which can't suspend. Kept in step with the file. */
    @Volatile
    var token: String? = null
        private set

    /** The last session read, so screens can draw the right layout on their first frame. */
    @Volatile
    var latest: Session? = null
        private set

    val session: Flow<Session?> = context.sessionData.data.map { prefs ->
        val token = prefs[Keys.token]
        val user = prefs[Keys.user]?.let { runCatching { apiJson.decodeFromString(User.serializer(), it) }.getOrNull() }
        this.token = token
        (if (token != null && user != null) Session(token, user) else null).also { latest = it }
    }

    suspend fun current(): Session? = session.first()

    suspend fun save(token: String, user: User) {
        this.token = token
        context.sessionData.edit {
            it[Keys.token] = token
            it[Keys.user] = apiJson.encodeToString(User.serializer(), user)
        }
    }

    suspend fun updateUser(user: User) {
        context.sessionData.edit { it[Keys.user] = apiJson.encodeToString(User.serializer(), user) }
    }

    suspend fun clear() {
        token = null
        latest = null
        context.sessionData.edit { it.clear() }
    }
}
