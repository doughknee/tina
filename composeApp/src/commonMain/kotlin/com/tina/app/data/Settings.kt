package com.tina.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber
import okio.Path.Companion.toPath

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class Settings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val use24h: Boolean = false,
    val defaultReminderMinutes: Int = DEFAULT_REMINDER_MINUTES,
)

fun createSettingsStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { producePath().toPath() })

private val KEY_THEME = stringPreferencesKey("themeMode")
private val KEY_DYNAMIC = booleanPreferencesKey("dynamicColor")
private val KEY_FIRST_DAY = intPreferencesKey("firstDayOfWeek")
private val KEY_24H = booleanPreferencesKey("use24h")
private val KEY_REMINDER = intPreferencesKey("defaultReminderMinutes")

class SettingsRepository(private val store: DataStore<Preferences>) {
    val settings: Flow<Settings> = store.data.map { p ->
        Settings(
            themeMode = p[KEY_THEME]?.let { value -> ThemeMode.entries.firstOrNull { it.name == value } }
                ?: ThemeMode.SYSTEM,
            dynamicColor = p[KEY_DYNAMIC] ?: true,
            firstDayOfWeek = p[KEY_FIRST_DAY]?.let { DayOfWeek(it) } ?: DayOfWeek.MONDAY,
            use24h = p[KEY_24H] ?: false,
            defaultReminderMinutes = p[KEY_REMINDER] ?: DEFAULT_REMINDER_MINUTES,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = store.edit { it[KEY_THEME] = mode.name }
    suspend fun setDynamicColor(enabled: Boolean) = store.edit { it[KEY_DYNAMIC] = enabled }
    suspend fun setFirstDayOfWeek(day: DayOfWeek) = store.edit { it[KEY_FIRST_DAY] = day.isoDayNumber }
    suspend fun setUse24h(enabled: Boolean) = store.edit { it[KEY_24H] = enabled }
    suspend fun setDefaultReminderMinutes(minutes: Int) = store.edit { it[KEY_REMINDER] = minutes }
}
