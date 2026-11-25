package com.keiser.studio.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.keiser.studio.model.BikeProtocol
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "keiser_rider_profiles")

class RiderProfileRepository(private val context: Context) {

    private val bikeProtocolKey = stringPreferencesKey("selected_bike_protocol")
    private val defaultFtpKey = intPreferencesKey("default_studio_ftp")

    fun getBikeProtocol(): Flow<BikeProtocol> {
        return context.dataStore.data.map { preferences ->
            val id = preferences[bikeProtocolKey] ?: BikeProtocol.KEISER.id
            BikeProtocol.fromId(id)
        }
    }

    suspend fun saveBikeProtocol(protocol: BikeProtocol) {
        context.dataStore.edit { preferences ->
            preferences[bikeProtocolKey] = protocol.id
        }
    }

    fun getDefaultFtp(): Flow<Int> {
        return context.dataStore.data.map { preferences ->
            (preferences[defaultFtpKey] ?: DEFAULT_FTP_WATTS).coerceIn(MIN_FTP_WATTS, MAX_FTP_WATTS)
        }
    }

    suspend fun saveDefaultFtp(ftp: Int) {
        val clamped = ftp.coerceIn(MIN_FTP_WATTS, MAX_FTP_WATTS)
        context.dataStore.edit { preferences ->
            preferences[defaultFtpKey] = clamped
        }
    }

    suspend fun hasCustomFtp(bikeId: Int): Boolean {
        val ftpKey = intPreferencesKey("rider_ftp_$bikeId")
        val prefs = context.dataStore.data.first()
        return prefs[ftpKey] != null
    }

    fun getRiderProfile(bikeId: Int): Flow<Pair<String, Int>> {
        val nameKey = stringPreferencesKey("rider_name_$bikeId")
        val ftpKey = intPreferencesKey("rider_ftp_$bikeId")

        return context.dataStore.data.map { preferences ->
            val defaultFtp = (preferences[defaultFtpKey] ?: DEFAULT_FTP_WATTS)
                .coerceIn(MIN_FTP_WATTS, MAX_FTP_WATTS)
            val name = preferences[nameKey] ?: "Bike #$bikeId"
            val ftp = (preferences[ftpKey] ?: defaultFtp).coerceIn(MIN_FTP_WATTS, MAX_FTP_WATTS)
            Pair(name, ftp)
        }
    }

    suspend fun saveRiderProfile(bikeId: Int, name: String, ftp: Int) {
        val nameKey = stringPreferencesKey("rider_name_$bikeId")
        val ftpKey = intPreferencesKey("rider_ftp_$bikeId")
        val clampedFtp = ftp.coerceIn(MIN_FTP_WATTS, MAX_FTP_WATTS)

        context.dataStore.edit { preferences ->
            preferences[nameKey] = name.trim().ifEmpty { "Bike #$bikeId" }
            preferences[ftpKey] = clampedFtp
        }
    }

    companion object {
        const val DEFAULT_FTP_WATTS = 200
        const val MIN_FTP_WATTS = 50
        const val MAX_FTP_WATTS = 600
    }
}
