package com.livefleet.studio.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.livefleet.studio.model.BikeProtocol
import com.livefleet.studio.model.Concept2ErgFilter
import com.livefleet.studio.model.Concept2MetricMode
import com.livefleet.studio.model.TileHeroMetric
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "livefleet_rider_profiles")

class RiderProfileRepository(private val context: Context) {

    private val bikeProtocolKey = stringPreferencesKey("selected_bike_protocol")
    private val zoneCountKey = intPreferencesKey("power_zone_count")
    private val defaultFtpKey = intPreferencesKey("default_studio_ftp")
    private val studioLockProximityKey = booleanPreferencesKey("studio_lock_proximity_enabled")
    private val concept2MetricModeKey = stringPreferencesKey("concept2_metric_mode")
    private val concept2ErgFilterKey = stringPreferencesKey("concept2_erg_filter")
    private val whitelistedMachinesKey = stringPreferencesKey("whitelisted_machines_map")
    private val instructorBikeKey = stringPreferencesKey("instructor_bike_key")
    private val tileHeroMetricKey = stringPreferencesKey("tile_hero_metric")

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

    fun getZoneCount(): Flow<Int> {
        return context.dataStore.data.map { preferences ->
            val protocol = BikeProtocol.fromId(preferences[bikeProtocolKey] ?: BikeProtocol.KEISER.id)
            val fallback = if (protocol == BikeProtocol.KEISER) 7 else 5
            val stored = preferences[zoneCountKey]
            if (stored == 5 || stored == 7) stored else fallback
        }
    }

    suspend fun saveZoneCount(count: Int) {
        if (count != 5 && count != 7) return
        context.dataStore.edit { preferences ->
            preferences[zoneCountKey] = count
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

    fun getStudioLockProximityEnabled(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[studioLockProximityKey] ?: false
        }
    }

    suspend fun saveStudioLockProximityEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[studioLockProximityKey] = enabled
        }
    }

    fun getConcept2MetricMode(): Flow<Concept2MetricMode> {
        return context.dataStore.data.map { preferences ->
            Concept2MetricMode.fromId(preferences[concept2MetricModeKey])
        }
    }

    suspend fun saveConcept2MetricMode(mode: Concept2MetricMode) {
        context.dataStore.edit { preferences ->
            preferences[concept2MetricModeKey] = mode.id
        }
    }

    fun getConcept2ErgFilter(): Flow<Concept2ErgFilter> {
        return context.dataStore.data.map { preferences ->
            Concept2ErgFilter.fromId(preferences[concept2ErgFilterKey])
        }
    }

    suspend fun saveConcept2ErgFilter(filter: Concept2ErgFilter) {
        context.dataStore.edit { preferences ->
            preferences[concept2ErgFilterKey] = filter.id
        }
    }

    fun getTileHeroMetric(): Flow<TileHeroMetric> {
        return context.dataStore.data.map { preferences ->
            TileHeroMetric.fromId(preferences[tileHeroMetricKey])
        }
    }

    suspend fun saveTileHeroMetric(metric: TileHeroMetric) {
        context.dataStore.edit { preferences ->
            preferences[tileHeroMetricKey] = metric.id
        }
    }

    /**
     * Persists only static Studio MAC/serial -> Studio Bike # (1..250) whitelist mappings.
     * Never persists live/connected session tiles.
     */
    fun getWhitelistedMachines(): Flow<Map<String, Int>> {
        return context.dataStore.data.map { preferences ->
            decodeWhitelist(preferences[whitelistedMachinesKey].orEmpty())
        }
    }

    suspend fun lockMachineToBikeNumber(hardwareKey: String, bikeNumber: Int) {
        val normalized = normalizeHardwareKey(hardwareKey)
        if (normalized.isEmpty()) return
        val clampedBike = bikeNumber.coerceIn(1, 250)
        context.dataStore.edit { preferences ->
            val map = decodeWhitelist(preferences[whitelistedMachinesKey].orEmpty()).toMutableMap()
            map[normalized] = clampedBike
            preferences[whitelistedMachinesKey] = encodeWhitelist(map)
        }
    }

    suspend fun unlockMachine(hardwareKey: String) {
        val normalized = normalizeHardwareKey(hardwareKey)
        if (normalized.isEmpty()) return
        context.dataStore.edit { preferences ->
            val map = decodeWhitelist(preferences[whitelistedMachinesKey].orEmpty()).toMutableMap()
            map.remove(normalized)
            preferences[whitelistedMachinesKey] = encodeWhitelist(map)
        }
    }

    fun getInstructorBikeKey(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[instructorBikeKey] ?: ""
        }
    }

    suspend fun saveInstructorBikeKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[instructorBikeKey] = key
        }
    }

    suspend fun clearInstructorBikeKey() {
        context.dataStore.edit { preferences ->
            preferences.remove(instructorBikeKey)
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

        fun normalizeHardwareKey(key: String): String =
            key.trim().uppercase()

        fun instructorKeyFor(macAddress: String, bikeId: Int): String {
            val normalizedMac = normalizeHardwareKey(macAddress)
            return if (normalizedMac.isNotEmpty()) normalizedMac else "ID:$bikeId"
        }

        private fun encodeWhitelist(map: Map<String, Int>): String {
            return map.entries
                .filter { it.key.isNotBlank() && it.value in 1..250 }
                .joinToString(";") { "${it.key}=${it.value}" }
        }

        private fun decodeWhitelist(raw: String): Map<String, Int> {
            if (raw.isBlank()) return emptyMap()
            val result = mutableMapOf<String, Int>()
            raw.split(";").forEach { entry ->
                val idx = entry.indexOf('=')
                if (idx > 0 && idx < entry.length - 1) {
                    val key = normalizeHardwareKey(entry.substring(0, idx))
                    val num = entry.substring(idx + 1).toIntOrNull()
                    if (key.isNotEmpty() && num != null && num in 1..250) {
                        result[key] = num
                    }
                }
            }
            return result
        }
    }
}
