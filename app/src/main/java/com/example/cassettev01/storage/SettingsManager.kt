package com.example.cassettev01.storage

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// One shared DataStore for the app settings.
val Context.dataStore by preferencesDataStore(name = "cassette_settings")

object SettingsManager {
    private val FOLDER_URI = stringPreferencesKey("folder_uri")
    private val LOOP_MODE = intPreferencesKey("loop_mode")
    private val THEME_MODE = stringPreferencesKey("theme_mode")
    private val WAVEFORM_STYLE = stringPreferencesKey("waveform_style")
    private val SHUFFLE_MODE = booleanPreferencesKey("shuffle_mode")
    private val APPEARANCE_MODE = stringPreferencesKey("appearance_mode")
    private val TUTORIAL_COMPLETED = booleanPreferencesKey("tutorial_completed")

    fun getFolderUri(context: Context): Flow<String?> = context.dataStore.data.map { it[FOLDER_URI] }

    suspend fun saveFolderUri(context: Context, uri: String) {
        context.dataStore.edit { it[FOLDER_URI] = uri }
    }

    fun getLoopMode(context: Context): Flow<Int> = context.dataStore.data.map { it[LOOP_MODE] ?: 0 }

    suspend fun saveLoopMode(context: Context, mode: Int) {
        context.dataStore.edit { it[LOOP_MODE] = mode }
    }

    fun getTheme(context: Context): Flow<String> = context.dataStore.data.map { it[THEME_MODE] ?: "CARBON" }

    suspend fun saveTheme(context: Context, themeName: String) {
        context.dataStore.edit { it[THEME_MODE] = themeName }
    }

    fun getWaveformStyle(context: Context): Flow<String> =
        context.dataStore.data.map { it[WAVEFORM_STYLE] ?: "THICK_BARS" }

    suspend fun saveWaveformStyle(context: Context, styleName: String) {
        context.dataStore.edit { it[WAVEFORM_STYLE] = styleName }
    }

    fun getAppearance(context: Context): Flow<String> = context.dataStore.data.map { it[APPEARANCE_MODE] ?: "DARK" }

    suspend fun saveAppearance(context: Context, appearance: String) {
        context.dataStore.edit { it[APPEARANCE_MODE] = appearance }
    }

    fun getTutorialCompleted(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[TUTORIAL_COMPLETED] ?: false }

    suspend fun saveTutorialCompleted(context: Context, completed: Boolean) {
        context.dataStore.edit { it[TUTORIAL_COMPLETED] = completed }
    }

    fun getShuffle(context: Context): Flow<Boolean> = context.dataStore.data.map { it[SHUFFLE_MODE] ?: false }

    suspend fun saveShuffle(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[SHUFFLE_MODE] = enabled }
    }
}