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
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "keiser_rider_profiles")

class RiderProfileRepository(private val context: Context) {

    private val bikeProtocolKey = stringPreferencesKey("selected_bike_protocol")

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

    fun getRiderProfile(bikeId: Int): Flow<Pair<String, Int>> {
        val nameKey = stringPreferencesKey("rider_name_$bikeId")
        val ftpKey = intPreferencesKey("rider_ftp_$bikeId")

        return context.dataStore.data.map { preferences ->
            val name = preferences[nameKey] ?: "Rider #$bikeId"
            val ftp = preferences[ftpKey] ?: 200
            Pair(name, ftp)
        }
    }

    suspend fun saveRiderProfile(bikeId: Int, name: String, ftp: Int) {
        val nameKey = stringPreferencesKey("rider_name_$bikeId")
        val ftpKey = intPreferencesKey("rider_ftp_$bikeId")

        context.dataStore.edit { preferences ->
            preferences[nameKey] = name
            preferences[ftpKey] = ftp
        }
    }
}
