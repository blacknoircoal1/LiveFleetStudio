package com.keiser.studio.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.keiser.studio.bluetooth.KeiserBleScanner
import com.keiser.studio.data.RiderProfileRepository
import com.keiser.studio.model.BikeData
import com.keiser.studio.model.BikeProtocol
import com.keiser.studio.model.FtpResultItem
import com.keiser.studio.model.FtpTestState
import com.keiser.studio.model.FtpZone
import com.keiser.studio.model.LeaderboardViewMode
import com.keiser.studio.model.SortOrder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

data class KeiserUiState(
    val bikes: List<BikeData> = emptyList(),
    val activeBikeCount: Int = 0,
    val averagePower: Int = 0,
    val averageCadence: Float = 0f,
    val isScanning: Boolean = false,
    val isDemoMode: Boolean = false,
    val sortOrder: SortOrder = SortOrder.WATTS,
    val ftpTest: FtpTestState = FtpTestState(),
    val sessionElapsedSeconds: Long = 0L,
    val isSessionTimerRunning: Boolean = false,
    val viewMode: LeaderboardViewMode = LeaderboardViewMode.GAUGES,
    val zoneCount: Int = 5, // 5 or 7 zones
    val selectedProtocol: BikeProtocol = BikeProtocol.KEISER,
    val isSettingsOpen: Boolean = false
)

class KeiserViewModel(application: Application) : AndroidViewModel(application) {

    private val bleScanner = KeiserBleScanner(application)
    private val repository = RiderProfileRepository(application)

    private val _uiState = MutableStateFlow(KeiserUiState())
    val uiState: StateFlow<KeiserUiState> = _uiState.asStateFlow()

    private val bikeMap = mutableMapOf<Int, BikeData>()
    private var demoJob: Job? = null
    private var ftpTimerJob: Job? = null
    private var staleCheckJob: Job? = null
    private var sessionTimerJob: Job? = null

    private data class DemoRiderConfig(
        val id: Int,
        val name: String,
        val ftp: Int,
        val baseWatts: Int,
        val baseRpm: Float,
        val gear: Int
    )

    private val demoConfigs = listOf(
        DemoRiderConfig(1, "Bike #1", 185, 175, 84f, 12),
        DemoRiderConfig(2, "Bike #2", 240, 250, 88f, 15),
        DemoRiderConfig(3, "Bike #3", 280, 295, 92f, 17),
        DemoRiderConfig(4, "Bike #4", 195, 190, 82f, 13),
        DemoRiderConfig(5, "Bike #5", 215, 205, 76f, 14),
        DemoRiderConfig(6, "Bike #6", 170, 165, 86f, 11),
        DemoRiderConfig(7, "Bike #7", 260, 270, 85f, 16),
        DemoRiderConfig(8, "Bike #8", 200, 195, 87f, 13),
        DemoRiderConfig(9, "Bike #9", 230, 235, 89f, 14),
        DemoRiderConfig(10, "Bike #10", 210, 220, 81f, 13),
        DemoRiderConfig(11, "Bike #11", 250, 240, 78f, 15),
        DemoRiderConfig(12, "Bike #12", 180, 170, 85f, 12)
    )

    init {
        // Load persisted bike protocol
        viewModelScope.launch {
            repository.getBikeProtocol().collect { protocol ->
                _uiState.update { it.copy(selectedProtocol = protocol) }
                bleScanner.setProtocol(protocol)
            }
        }

        // Collect BLE broadcasts
        viewModelScope.launch {
            bleScanner.bikeDataFlow.collect { rawBike ->
                processIncomingBike(rawBike)
            }
        }

        // Stale bike checker (10 seconds timeout)
        staleCheckJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val now = System.currentTimeMillis()
                var updated = false
                bikeMap.values.forEach { b ->
                    val shouldBeStale = (now - b.lastSeenTimestamp) > 10_000
                    if (b.isStale != shouldBeStale) {
                        bikeMap[b.bikeId] = b.copy(isStale = shouldBeStale)
                        updated = true
                    }
                }
                if (updated) {
                    refreshUiState()
                }
            }
        }

        // Continuous Main Session Elapsed Timer (Warmup -> Intervals/FTP -> Cooldown)
        sessionTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (_uiState.value.isSessionTimerRunning) {
                    _uiState.update { it.copy(sessionElapsedSeconds = it.sessionElapsedSeconds + 1) }
                }
            }
        }
    }

    private fun processIncomingBike(bike: BikeData) {
        viewModelScope.launch {
            val (name, ftp) = repository.getRiderProfile(bike.bikeId).first()
            val ftpPct = if (ftp > 0) (bike.powerWatts * 100) / ftp else 0
            val zone = FtpZone.fromFtpPercent(ftpPct, _uiState.value.zoneCount)

            val fullBike = bike.copy(
                riderName = name.ifEmpty { "Bike #${bike.bikeId}" },
                ftp = ftp,
                ftpPercent = ftpPct,
                ftpZone = zone,
                isStale = false,
                lastSeenTimestamp = System.currentTimeMillis()
            )
            bikeMap[bike.bikeId] = fullBike

            // Sample power if FTP test is active and past the 5-second countdown
            if (_uiState.value.ftpTest.isActive && !_uiState.value.ftpTest.isPreparing) {
                val currentSamples = _uiState.value.ftpTest.samples.toMutableMap()
                val list = currentSamples.getOrDefault(bike.bikeId, emptyList()).toMutableList()
                list.add(bike.powerWatts)
                currentSamples[bike.bikeId] = list
                _uiState.update { it.copy(ftpTest = it.ftpTest.copy(samples = currentSamples)) }
            }

            refreshUiState()
        }
    }

    // Session Timer Controls
    fun toggleSessionTimer() {
        _uiState.update { it.copy(isSessionTimerRunning = !it.isSessionTimerRunning) }
    }

    fun resetSessionTimer() {
        _uiState.update { it.copy(sessionElapsedSeconds = 0L) }
    }

    // View Mode Switcher (Gauges, Table)
    fun setViewMode(mode: LeaderboardViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun setSortOrder(order: SortOrder) {
        _uiState.update { it.copy(sortOrder = order) }
        refreshUiState()
    }

    fun toggleBleScan() {
        val currentlyScanning = _uiState.value.isScanning
        if (currentlyScanning) {
            bleScanner.stopScan()
            _uiState.update { it.copy(isScanning = false) }
        } else {
            bleScanner.startScan()
            _uiState.update { it.copy(isScanning = true) }
        }
    }

    fun toggleDemoMode(enable: Boolean) {
        _uiState.update { it.copy(isDemoMode = enable) }
        if (enable) {
            startDemoSimulation()
        } else {
            stopDemoSimulation()
        }
    }

    private fun startDemoSimulation() {
        demoJob?.cancel()
        val demoBrand = when (_uiState.value.selectedProtocol) {
            BikeProtocol.AUTO_DETECT -> "Fleet"
            BikeProtocol.KEISER -> "Keiser"
            BikeProtocol.STAGES -> "Stages"
            BikeProtocol.SCHWINN_4IIII -> "Schwinn"
            BikeProtocol.CONCEPT2 -> "Concept2"
            BikeProtocol.FTMS_UNIVERSAL -> "FTMS"
            BikeProtocol.POWER_METERS -> "Power"
        }
        demoConfigs.forEach { cfg ->
            val ftpPct = (cfg.baseWatts * 100) / cfg.ftp
            bikeMap[cfg.id] = BikeData(
                bikeId = cfg.id,
                cadenceRpm = cfg.baseRpm,
                heartRateBpm = 0f,
                powerWatts = cfg.baseWatts,
                calories = 100 + cfg.id * 10,
                distance = 3.5f + cfg.id * 0.3f,
                distanceUnit = "MI",
                gear = cfg.gear,
                lastSeenTimestamp = System.currentTimeMillis(),
                isStale = false,
                riderName = cfg.name,
                brand = demoBrand,
                ftp = cfg.ftp,
                ftpPercent = ftpPct,
                ftpZone = FtpZone.fromFtpPercent(ftpPct, _uiState.value.zoneCount)
            )
        }
        refreshUiState()

        demoJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                demoConfigs.forEach { cfg ->
                    val existing = bikeMap[cfg.id] ?: return@forEach
                    val rpmDelta = (Random.nextFloat() * 4f - 2f)
                    val wattsDelta = Random.nextInt(-8, 9)

                    val newRpm = (existing.cadenceRpm + rpmDelta).coerceIn(65f, 115f)
                    val newWatts = (existing.powerWatts + wattsDelta).coerceIn(80, 450)
                    val newDist = existing.distance + (newRpm / 60f) * 0.005f
                    val newCal = existing.calories + if (newWatts > 150) 1 else 0

                    val ftpPct = if (existing.ftp > 0) (newWatts * 100) / existing.ftp else 0

                    val updated = existing.copy(
                        cadenceRpm = Math.round(newRpm * 10f) / 10f,
                        powerWatts = newWatts,
                        distance = Math.round(newDist * 100f) / 100f,
                        calories = newCal,
                        ftpPercent = ftpPct,
                        ftpZone = FtpZone.fromFtpPercent(ftpPct, _uiState.value.zoneCount),
                        lastSeenTimestamp = System.currentTimeMillis()
                    )
                    bikeMap[cfg.id] = updated

                    if (_uiState.value.ftpTest.isActive && !_uiState.value.ftpTest.isPreparing) {
                        val currentSamples = _uiState.value.ftpTest.samples.toMutableMap()
                        val list = currentSamples.getOrDefault(cfg.id, emptyList()).toMutableList()
                        list.add(newWatts)
                        currentSamples[cfg.id] = list
                        _uiState.update { it.copy(ftpTest = it.ftpTest.copy(samples = currentSamples)) }
                    }
                }
                refreshUiState()
            }
        }
    }

    private fun stopDemoSimulation() {
        demoJob?.cancel()
        demoJob = null
        demoConfigs.forEach { bikeMap.remove(it.id) }
        refreshUiState()
    }

    // Group FTP Test Mode (5s preparation countdown, then test runs while session timer ticks!)
    fun startFtpTest(minutes: Int, factor: Float) {
        val totalSecs = minutes * 60
        _uiState.update {
            it.copy(
                ftpTest = FtpTestState(
                    isActive = true,
                    isPreparing = true,
                    prepCountdownSeconds = 5,
                    durationSeconds = totalSecs,
                    remainingSeconds = totalSecs,
                    startSessionSeconds = it.sessionElapsedSeconds,
                    scalingFactor = factor,
                    testType = "$minutes-Min",
                    samples = emptyMap(),
                    pendingResults = null,
                    isViewingResults = false
                )
            )
        }

        ftpTimerJob?.cancel()
        ftpTimerJob = viewModelScope.launch {
            // 1. Initial 5-second countdown ("Get Ready!")
            while (_uiState.value.ftpTest.prepCountdownSeconds > 0) {
                delay(1000)
                val currentPrep = _uiState.value.ftpTest.prepCountdownSeconds
                val nextPrep = currentPrep - 1
                if (nextPrep > 0) {
                    _uiState.update { it.copy(ftpTest = it.ftpTest.copy(prepCountdownSeconds = nextPrep)) }
                } else {
                    // Prep countdown finished! Begin the actual FTP test!
                    _uiState.update {
                        it.copy(
                            ftpTest = it.ftpTest.copy(
                                isPreparing = false,
                                prepCountdownSeconds = 0,
                                startSessionSeconds = it.sessionElapsedSeconds
                            )
                        )
                    }
                    break
                }
            }

            // 2. Main FTP test countdown
            while (_uiState.value.ftpTest.remainingSeconds > 0) {
                delay(1000)
                val newRem = _uiState.value.ftpTest.remainingSeconds - 1
                _uiState.update { it.copy(ftpTest = it.ftpTest.copy(remainingSeconds = newRem)) }
            }
            finishFtpTest()
        }
    }

    fun finishFtpTestEarly() {
        finishFtpTest()
    }

    fun cancelFtpTest() {
        ftpTimerJob?.cancel()
        ftpTimerJob = null
        val previousResults = _uiState.value.ftpTest.lastCompletedResults
        val prevTestType = _uiState.value.ftpTest.testType
        val prevFactor = _uiState.value.ftpTest.scalingFactor
        _uiState.update {
            it.copy(
                ftpTest = FtpTestState(
                    lastCompletedResults = previousResults,
                    testType = prevTestType,
                    scalingFactor = prevFactor,
                    isViewingResults = false
                )
            )
        }
    }

    private fun finishFtpTest() {
        ftpTimerJob?.cancel()
        ftpTimerJob = null

        val testState = _uiState.value.ftpTest
        val factor = testState.scalingFactor

        val results = bikeMap.values.map { bike ->
            val samples = testState.samples[bike.bikeId] ?: listOf(bike.powerWatts)
            val avgWatts = if (samples.isNotEmpty()) samples.average().toInt() else bike.powerWatts
            val newFtp = (avgWatts * factor).toInt()
            val oldFtp = bike.ftp
            val delta = newFtp - oldFtp

            FtpResultItem(
                bikeId = bike.bikeId,
                riderName = bike.riderName,
                avgWatts = avgWatts,
                factor = factor,
                newFtp = newFtp,
                oldFtp = oldFtp,
                delta = delta
            )
        }.sortedBy { it.bikeId }

        _uiState.update {
            it.copy(
                ftpTest = it.ftpTest.copy(
                    isActive = false,
                    isPreparing = false,
                    pendingResults = results,
                    lastCompletedResults = results,
                    isViewingResults = true
                )
            )
        }
    }

    fun dismissFtpResults() {
        val results = _uiState.value.ftpTest.pendingResults ?: _uiState.value.ftpTest.lastCompletedResults
        results?.forEach { res ->
            val existing = bikeMap[res.bikeId]
            if (existing != null && res.newFtp > 0) {
                val newPct = (existing.powerWatts * 100) / res.newFtp
                bikeMap[res.bikeId] = existing.copy(
                    ftp = res.newFtp,
                    ftpPercent = newPct,
                    ftpZone = FtpZone.fromFtpPercent(newPct, _uiState.value.zoneCount)
                )
            }
        }
        val lastResults = _uiState.value.ftpTest.lastCompletedResults ?: results
        _uiState.update {
            it.copy(
                ftpTest = it.ftpTest.copy(
                    pendingResults = null,
                    isViewingResults = false,
                    lastCompletedResults = lastResults
                )
            )
        }
        refreshUiState()
    }

    fun showLastFtpResults() {
        if (_uiState.value.ftpTest.lastCompletedResults != null) {
            _uiState.update {
                it.copy(
                    ftpTest = it.ftpTest.copy(
                        isViewingResults = true
                    )
                )
            }
        }
    }

    fun setZoneCount(count: Int) {
        if (count == 5 || count == 7) {
            _uiState.update { it.copy(zoneCount = count) }
            bikeMap.replaceAll { _, bike ->
                bike.copy(ftpZone = FtpZone.fromFtpPercent(bike.ftpPercent, count))
            }
            refreshUiState()
        }
    }

    private fun refreshUiState() {
        val allBikes = bikeMap.values.toList()
        val activeBikes = allBikes.filter { !it.isStale }

        val sorted = when (_uiState.value.sortOrder) {
            SortOrder.WATTS -> allBikes.sortedByDescending { it.powerWatts }
            SortOrder.FTP_PERCENT -> allBikes.sortedByDescending { it.ftpPercent }
            SortOrder.CADENCE -> allBikes.sortedByDescending { it.cadenceRpm }
            SortOrder.DISTANCE -> allBikes.sortedByDescending { it.distance }
            SortOrder.BIKE_NUMBER -> allBikes.sortedBy { it.bikeId }
        }

        val avgPower = if (activeBikes.isNotEmpty()) {
            activeBikes.map { it.powerWatts }.average().toInt()
        } else 0

        val avgCadence = if (activeBikes.isNotEmpty()) {
            val avg = activeBikes.map { it.cadenceRpm }.average().toFloat()
            Math.round(avg * 10f) / 10f
        } else 0f

        _uiState.update {
            it.copy(
                bikes = sorted,
                activeBikeCount = activeBikes.size,
                averagePower = avgPower,
                averageCadence = avgCadence
            )
        }
    }

    fun openSettings() {
        _uiState.update { it.copy(isSettingsOpen = true) }
    }

    fun closeSettings() {
        _uiState.update { it.copy(isSettingsOpen = false) }
    }

    fun setBikeProtocol(protocol: BikeProtocol) {
        viewModelScope.launch {
            repository.saveBikeProtocol(protocol)
            _uiState.update { it.copy(selectedProtocol = protocol) }
            bleScanner.setProtocol(protocol)
        }
    }

    override fun onCleared() {
        super.onCleared()
        bleScanner.stopScan()
        demoJob?.cancel()
        ftpTimerJob?.cancel()
        staleCheckJob?.cancel()
        sessionTimerJob?.cancel()
    }
}
