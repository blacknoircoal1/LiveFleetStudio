package com.livefleet.studio.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.livefleet.studio.bluetooth.AntPlusUsbReceiver
import com.livefleet.studio.bluetooth.LiveFleetBleScanner
import com.livefleet.studio.bluetooth.UniversalGattManager
import com.livefleet.studio.data.RiderProfileRepository
import com.livefleet.studio.model.BikeData
import com.livefleet.studio.model.BikeProtocol
import com.livefleet.studio.model.BrandType
import com.livefleet.studio.model.Concept2ErgFilter
import com.livefleet.studio.model.Concept2MetricMode
import com.livefleet.studio.model.CompletedInterval
import com.livefleet.studio.model.FtpResultItem
import com.livefleet.studio.model.FtpTestState
import com.livefleet.studio.model.FtpZone
import com.livefleet.studio.model.IntervalAccumulator
import com.livefleet.studio.model.IntervalState
import com.livefleet.studio.model.LeaderboardViewMode
import com.livefleet.studio.model.SortOrder
import com.livefleet.studio.model.TileHeroMetric
import com.livefleet.studio.model.computeBikeIntervalStats
import com.livefleet.studio.util.AppDiagnosticLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

data class LiveFleetUiState(
    val bikes: List<BikeData> = emptyList(),
    val activeBikeCount: Int = 0,
    val packetsPerSecond: Int = 0,
    val averagePower: Int = 0,
    val averageCadence: Float = 0f,
    val isScanning: Boolean = false,
    val isDemoMode: Boolean = false,
    val demoBikeCount: Int = 12,
    val sortOrder: SortOrder = SortOrder.BIKE_NUMBER,
    val ftpTest: FtpTestState = FtpTestState(),
    val intervalState: IntervalState = IntervalState(),
    val sessionElapsedSeconds: Long = 0L,
    val isSessionTimerRunning: Boolean = false,
    val viewMode: LeaderboardViewMode = LeaderboardViewMode.GAUGES,
    val zoneCount: Int = 7, // 5 or 7 zones (Keiser defaults to 7)
    val defaultFtp: Int = RiderProfileRepository.DEFAULT_FTP_WATTS,
    val selectedProtocol: BikeProtocol = BikeProtocol.KEISER,
    val studioLockProximityEnabled: Boolean = false,
    val whitelistedMachines: Map<String, Int> = emptyMap(),
    val instructorBikeKey: String = "",
    val concept2MetricMode: Concept2MetricMode = Concept2MetricMode.METRES,
    val concept2ErgFilter: Concept2ErgFilter = Concept2ErgFilter.ALL,
    val tileHeroMetric: TileHeroMetric = TileHeroMetric.WATTS,
    val antPlusStatus: String = "No USB ANT+ Dongle",
    val isSettingsOpen: Boolean = false
)

class LiveFleetViewModel(application: Application) : AndroidViewModel(application) {

    private val bleScanner = LiveFleetBleScanner(application)
    private val gattManager = UniversalGattManager(application)
    private val antPlusReceiver = AntPlusUsbReceiver(application)
    private val repository = RiderProfileRepository(application)

    private val _uiState = MutableStateFlow(LiveFleetUiState())
    val uiState: StateFlow<LiveFleetUiState> = _uiState.asStateFlow()

    private val bikeMap = mutableMapOf<Int, BikeData>()
    private val sessionAccumulators = mutableMapOf<Int, IntervalAccumulator>()
    private val intervalAccumulators = mutableMapOf<Int, IntervalAccumulator>()
    private var demoJob: Job? = null
    private var ftpTimerJob: Job? = null
    private var staleCheckJob: Job? = null
    private var sessionTimerJob: Job? = null
    private var scanWatchdogJob: Job? = null
    private var packetRateJob: Job? = null
    private var lastScanRefreshMs: Long = 0L

    private data class DemoRiderConfig(
        val id: Int,
        val name: String,
        val ftp: Int,
        val baseWatts: Int,
        val baseRpm: Float,
        val gear: Int
    )

    private val baseDemoConfigs = listOf(
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

    private fun generateDemoConfigs(count: Int): List<DemoRiderConfig> {
        val clamped = count.coerceIn(1, 30)
        return (1..clamped).map { idx ->
            if (idx <= baseDemoConfigs.size) {
                baseDemoConfigs[idx - 1]
            } else {
                val ftp = 170 + ((idx * 17) % 115)
                val baseWatts = (ftp * (82 + (idx * 7) % 35)) / 100
                val baseRpm = 76f + ((idx * 3) % 18)
                val gear = 11 + (idx % 7)
                DemoRiderConfig(idx, "Bike #$idx", ftp, baseWatts, baseRpm, gear)
            }
        }
    }

    init {
        AppDiagnosticLogger.init(application)

        // Requirement 7: Zero cached machines on cold app start
        clearLiveSession()

        // Load persisted bike protocol
        viewModelScope.launch {
            repository.getBikeProtocol().collect { protocol ->
                _uiState.update { it.copy(selectedProtocol = protocol) }
                bleScanner.setProtocol(protocol)
                antPlusReceiver.setProtocol(protocol)
            }
        }

        // Load persisted zone count (defaults to 7 when Keiser is active, 5 otherwise)
        viewModelScope.launch {
            repository.getZoneCount().collect { count ->
                _uiState.update { it.copy(zoneCount = count) }
                bikeMap.replaceAll { _, bike ->
                    bike.copy(ftpZone = FtpZone.fromFtpPercent(bike.ftpPercent, count))
                }
                refreshUiState()
            }
        }

        // Load persisted default studio FTP
        viewModelScope.launch {
            repository.getDefaultFtp().collect { defaultFtp ->
                _uiState.update { it.copy(defaultFtp = defaultFtp) }
            }
        }

        // Load persisted Studio Lock & Proximity RSSI Filter
        viewModelScope.launch {
            repository.getStudioLockProximityEnabled().collect { enabled ->
                bleScanner.studioLockProximityEnabled = enabled
                _uiState.update { it.copy(studioLockProximityEnabled = enabled) }
            }
        }

        // Load persisted Studio Bike # whitelist (MAC -> Studio Bike #)
        viewModelScope.launch {
            repository.getWhitelistedMachines().collect { whitelist ->
                bleScanner.whitelistedHardwareKeys = whitelist.keys
                _uiState.update { it.copy(whitelistedMachines = whitelist) }
            }
        }

        // Load persisted Instructor Bike key
        viewModelScope.launch {
            repository.getInstructorBikeKey().collect { key ->
                _uiState.update { it.copy(instructorBikeKey = key) }
                var changed = false
                bikeMap.entries.toList().forEach { (id, b) ->
                    val shouldBeInstructor = key.isNotEmpty() &&
                        RiderProfileRepository.instructorKeyFor(b.macAddress, id) == key
                    if (b.isInstructorBike != shouldBeInstructor) {
                        bikeMap[id] = b.copy(isInstructorBike = shouldBeInstructor)
                        changed = true
                    }
                }
                if (changed) refreshUiState()
            }
        }

        // Load persisted Concept2 secondary metric preference (defaults to Metres)
        viewModelScope.launch {
            repository.getConcept2MetricMode().collect { mode ->
                _uiState.update { it.copy(concept2MetricMode = mode) }
            }
        }

        // Load persisted Concept2 Erg subtype filter
        viewModelScope.launch {
            repository.getConcept2ErgFilter().collect { filter ->
                bleScanner.concept2ErgFilter = filter
                gattManager.setConcept2ErgFilter(filter)
                _uiState.update { it.copy(concept2ErgFilter = filter) }
                refreshUiState()
            }
        }

        // Load persisted Tile Hero Metric preference
        viewModelScope.launch {
            repository.getTileHeroMetric().collect { metric ->
                _uiState.update { it.copy(tileHeroMetric = metric) }
            }
        }

        // Collect BLE broadcasts
        viewModelScope.launch {
            bleScanner.bikeDataFlow.collect { rawBike ->
                processIncomingBike(rawBike)
            }
        }

        // Layer 3 — Enqueue BLE GATT connections into serialized GATT queue (max 6)
        viewModelScope.launch {
            bleScanner.gattDeviceFlow.collect { discovered ->
                if (_uiState.value.isScanning && !antPlusReceiver.isActiveFlow.value) {
                    gattManager.enqueueConnectionIfNeeded(discovered)
                }
            }
        }

        // Collect live unified BLE GATT telemetry
        viewModelScope.launch {
            gattManager.bikeDataFlow.collect { rawBike ->
                processIncomingBike(rawBike)
            }
        }

        // Collect live USB ANT+ telemetry
        viewModelScope.launch {
            antPlusReceiver.bikeDataFlow.collect { rawBike ->
                if (_uiState.value.selectedProtocol != BikeProtocol.KEISER) {
                    processIncomingBike(rawBike)
                }
            }
        }

        // Auto-priority switch: when USB ANT+ activates, drop BLE GATT sessions to avoid duplicates
        viewModelScope.launch {
            antPlusReceiver.isActiveFlow.collect { isAntActive ->
                if (isAntActive) {
                    gattManager.disconnectAll()
                }
            }
        }

        // Track USB ANT+ dongle status
        viewModelScope.launch {
            antPlusReceiver.statusFlow.collect { status ->
                if (_uiState.value.antPlusStatus != status) {
                    AppDiagnosticLogger.log("ANT+", "Status changed: $status")
                }
                _uiState.update { it.copy(antPlusStatus = status) }
            }
        }

        // Sync BLE scanner failure events with UI state and attempt gentle recovery
        viewModelScope.launch {
            bleScanner.scanFailedFlow.collect {
                if (_uiState.value.isScanning) {
                    delay(1500)
                    val restarted = bleScanner.startScan()
                    if (!restarted) {
                        _uiState.update { it.copy(isScanning = false) }
                    }
                }
            }
        }

        // Requirement 1: 1 Hz Packet-Rate Sampler (AtomicInteger drained once per second into StateFlow)
        packetRateJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val rate = if (_uiState.value.isScanning) {
                    bleScanner.drainPacketCount() +
                        gattManager.drainPacketCount() +
                        antPlusReceiver.drainPacketCount()
                } else {
                    bleScanner.resetPacketCount()
                    gattManager.resetPacketCount()
                    antPlusReceiver.resetPacketCount()
                    0
                }
                if (_uiState.value.packetsPerSecond != rate) {
                    _uiState.update { it.copy(packetsPerSecond = rate) }
                }
            }
        }

        // Requirement 7: 10-Second Stale-Machine Watchdog across ALL brands
        // Runs every 1 second; closes GATT handle (if open) and removes any silent machine after 10s
        // (skipped only during Demo Mode or active FTP Test).
        staleCheckJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val now = System.currentTimeMillis()
                val allowAutoPurge = !_uiState.value.isDemoMode && !_uiState.value.ftpTest.isActive
                var updated = false

                bikeMap.entries.toList().forEach { (id, b) ->
                    val elapsed = now - b.lastSeenTimestamp
                    if (allowAutoPurge && elapsed >= STALE_PURGE_TIMEOUT_MS) {
                        AppDiagnosticLogger.log("FLEET", "10s stale purge: brand=${b.brand} id=${b.displayId.ifEmpty { "#$id" }} mac=${b.macAddress}")
                        if (b.macAddress.isNotBlank()) {
                            gattManager.disconnectDevice(b.macAddress)
                        }
                        bikeMap.remove(id)
                        updated = true
                    } else {
                        val shouldBeIdle = elapsed >= IDLE_TIMEOUT_MS
                        if (b.isStale != shouldBeIdle) {
                            bikeMap[id] = b.copy(isStale = shouldBeIdle)
                            updated = true
                        }
                    }
                }
                if (updated) {
                    refreshUiState()
                }
            }
        }

        // BLE Scan Watchdog: prevents silent Android/Samsung BLE scan suspension after long idle periods.
        scanWatchdogJob = viewModelScope.launch {
            while (true) {
                delay(15_000)
                if (!_uiState.value.isScanning) continue
                val now = System.currentTimeMillis()
                if (now - lastScanRefreshMs < SCAN_REFRESH_COOLDOWN_MS) continue

                val scanAge = now - bleScanner.scanStartedTimestampMs
                val silenceDuration = now - bleScanner.lastPacketTimestampMs
                val needsRefresh = !bleScanner.isScanning ||
                    scanAge >= SCAN_MAX_CONTINUOUS_MS ||
                    silenceDuration >= SCAN_SILENCE_REFRESH_MS

                if (needsRefresh) {
                    refreshBleScanSafely()
                }
            }
        }

        // Master 1 Hz clock is started on demand via restartMasterClockLoop() so session & interval stay in lockstep
    }

    private fun restartMasterClockLoop() {
        sessionTimerJob?.cancel()
        sessionTimerJob = viewModelScope.launch {
            while (_uiState.value.isSessionTimerRunning) {
                delay(1000)
                if (!_uiState.value.isSessionTimerRunning) break

                val zoneCount = _uiState.value.zoneCount
                val isIntervalActive = _uiState.value.intervalState.isActive
                val liveAvgW: Int
                val liveAvgCad: Float
                if (isIntervalActive) {
                    val liveStats = intervalAccumulators.values.map { computeBikeIntervalStats(it, zoneCount) }
                    liveAvgW = if (liveStats.isNotEmpty()) {
                        liveStats.map { it.avgWatts }.average().toInt()
                    } else {
                        _uiState.value.averagePower
                    }
                    liveAvgCad = if (liveStats.isNotEmpty()) {
                        Math.round(liveStats.map { it.avgCadence }.average().toFloat() * 10f) / 10f
                    } else {
                        _uiState.value.averageCadence
                    }
                } else {
                    liveAvgW = 0
                    liveAvgCad = 0f
                }

                _uiState.update { state ->
                    val nextInterval = if (state.intervalState.isActive) {
                        state.intervalState.copy(
                            elapsedSeconds = state.intervalState.elapsedSeconds + 1,
                            liveClassAvgWatts = liveAvgW,
                            liveClassAvgCadence = liveAvgCad
                        )
                    } else {
                        state.intervalState
                    }
                    state.copy(
                        sessionElapsedSeconds = state.sessionElapsedSeconds + 1,
                        intervalState = nextInterval
                    )
                }
            }
        }
    }

    private fun processIncomingBike(bike: BikeData) {
        viewModelScope.launch {
            val normalizedMac = RiderProfileRepository.normalizeHardwareKey(bike.macAddress)
            val lockedBikeNum = if (normalizedMac.isNotEmpty()) {
                _uiState.value.whitelistedMachines[normalizedMac]
            } else {
                null
            }

            val resolvedBikeId = lockedBikeNum ?: bike.bikeId
            val resolvedDisplayId = if (lockedBikeNum != null) {
                "#$lockedBikeNum"
            } else {
                bike.displayId
            }
            val resolvedWhitelisted = lockedBikeNum != null || bike.isWhitelisted

            // If this MAC was previously mapped under an unlocked ID, remove the duplicate entry
            if (lockedBikeNum != null && bike.bikeId != lockedBikeNum) {
                bikeMap.remove(bike.bikeId)
            }

            // Ignore brand-new idle machines (0 W & 0 RPM) until a rider actually pedals or rows
            val isAlreadyVisible = bikeMap.containsKey(resolvedBikeId)
            if (!isAlreadyVisible && bike.powerWatts == 0 && bike.cadenceRpm == 0f) {
                return@launch
            }

            // Preserve any already-confirmed subtype (e.g., ROWERG / SKIERG) across packets
            val existingEq = bikeMap[resolvedBikeId]?.equipmentType ?: ""
            val resolvedEqType = when {
                bike.equipmentType.isNotEmpty() -> bike.equipmentType
                existingEq.isNotEmpty() -> existingEq
                else -> ""
            }

            val (name, ftp) = repository.getRiderProfile(resolvedBikeId).first()
            val ftpPct = if (ftp > 0) (bike.powerWatts * 100) / ftp else 0
            val zone = FtpZone.fromFtpPercent(ftpPct, _uiState.value.zoneCount)
            val instKey = _uiState.value.instructorBikeKey
            val isInstructor = instKey.isNotEmpty() &&
                RiderProfileRepository.instructorKeyFor(bike.macAddress, resolvedBikeId) == instKey

            val sessionAcc = sessionAccumulators[resolvedBikeId]
            val computedAvgWatts = when {
                bike.avgWatts > 0 -> bike.avgWatts
                sessionAcc != null && sessionAcc.sampleCount > 0 ->
                    (sessionAcc.wattSum.toDouble() / sessionAcc.sampleCount).toInt()
                else -> bike.powerWatts
            }
            val computedCalPerHr = when {
                bike.caloriesPerHour > 0 -> bike.caloriesPerHour
                bike.powerWatts > 0 && bike.brandType == BrandType.CONCEPT2 ->
                    (bike.powerWatts * 3.4416 + 300.0).toInt()
                bike.powerWatts > 0 -> (bike.powerWatts * 3.6 + 300.0).toInt()
                else -> 0
            }

            val fullBike = bike.copy(
                bikeId = resolvedBikeId,
                displayId = resolvedDisplayId,
                equipmentType = resolvedEqType,
                isWhitelisted = resolvedWhitelisted,
                isInstructorBike = isInstructor,
                riderName = name.ifEmpty { "Bike #$resolvedBikeId" },
                avgWatts = computedAvgWatts,
                caloriesPerHour = computedCalPerHr,
                ftp = ftp,
                ftpPercent = ftpPct,
                ftpZone = zone,
                isStale = false,
                lastSeenTimestamp = System.currentTimeMillis()
            )
            if (!isAlreadyVisible) {
                AppDiagnosticLogger.log(
                    "FLEET",
                    "Tile appeared: brand=${fullBike.brand} id=${fullBike.displayId.ifEmpty { "#${fullBike.bikeId}" }} " +
                        "eq=${fullBike.equipmentType.ifEmpty { "-" }} mac=${fullBike.macAddress} ${fullBike.powerWatts}W ${fullBike.cadenceRpm.toInt()}RPM ftp=$ftp"
                )
            }
            bikeMap[resolvedBikeId] = fullBike

            // Sample power if FTP test is active and past the 5-second countdown
            if (_uiState.value.ftpTest.isActive && !_uiState.value.ftpTest.isPreparing) {
                val currentSamples = _uiState.value.ftpTest.samples.toMutableMap()
                val list = currentSamples.getOrDefault(resolvedBikeId, emptyList()).toMutableList()
                list.add(bike.powerWatts)
                currentSamples[resolvedBikeId] = list
                _uiState.update { it.copy(ftpTest = it.ftpTest.copy(samples = currentSamples)) }
            }

            // Sample power/cadence for whole-session summary while workout timer is running
            if (_uiState.value.isSessionTimerRunning) {
                recordSessionSample(fullBike)
            }

            // Sample power/cadence if freestyle interval tracking is active
            if (_uiState.value.intervalState.isActive) {
                recordIntervalSample(fullBike)
            }

            refreshUiState()
        }
    }

    /**
     * Requirement 7: Global Session Wipe.
     * Cancels pending GATT queues, disconnects & closes all open GATT handles,
     * clears the live machine map, and resets telemetry counters to 0.
     */
    private fun clearLiveSession() {
        gattManager.disconnectAll()
        bleScanner.resetPacketCount()
        gattManager.resetPacketCount()
        antPlusReceiver.resetPacketCount()
        bikeMap.clear()
        _uiState.update {
            it.copy(
                bikes = emptyList(),
                activeBikeCount = 0,
                packetsPerSecond = 0,
                averagePower = 0,
                averageCadence = 0f
            )
        }
    }

    /**
     * Clean lifecycle teardown for MainActivity.onDestroy() / ViewModel.onCleared().
     */
    fun teardownBluetooth() {
        bleScanner.stopScan()
        gattManager.disconnectAll()
        antPlusReceiver.stop()
        _uiState.update { it.copy(isScanning = false, packetsPerSecond = 0) }
    }

    // Session Timer Controls
    fun toggleSessionTimer() {
        val current = _uiState.value
        val nextRunning = !current.isSessionTimerRunning
        if (nextRunning && current.sessionElapsedSeconds == 0L && current.intervalState.hasCompletedSummary) {
            sessionAccumulators.clear()
            intervalAccumulators.clear()
            _uiState.update {
                it.copy(
                    isSessionTimerRunning = true,
                    intervalState = IntervalState(),
                    ftpTest = if (it.ftpTest.isActive) it.ftpTest else FtpTestState()
                )
            }
        } else {
            _uiState.update { it.copy(isSessionTimerRunning = nextRunning) }
        }
        if (nextRunning) {
            restartMasterClockLoop()
        } else {
            sessionTimerJob?.cancel()
            sessionTimerJob = null
        }
    }

    fun resetSessionTimer() {
        sessionTimerJob?.cancel()
        sessionTimerJob = null
        clearIntervals()
        _uiState.update { it.copy(sessionElapsedSeconds = 0L, isSessionTimerRunning = false) }
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
            gattManager.disconnectAll()
            antPlusReceiver.stop()
            _uiState.update { it.copy(isScanning = false, packetsPerSecond = 0) }
        } else {
            // Requirement 7: Wipe live map, GATT handles, and counters on every Start Scan tap
            if (_uiState.value.isDemoMode) {
                demoJob?.cancel()
                demoJob = null
                _uiState.update { it.copy(isDemoMode = false) }
            }
            clearLiveSession()
            val started = bleScanner.startScan()
            if (started) {
                lastScanRefreshMs = System.currentTimeMillis()
                if (_uiState.value.selectedProtocol != BikeProtocol.KEISER) {
                    antPlusReceiver.start()
                }
            }
            _uiState.update { it.copy(isScanning = started) }
        }
    }

    fun onAppResumed() {
        if (!_uiState.value.isScanning) return
        val now = System.currentTimeMillis()
        if (now - lastScanRefreshMs < 10_000L && bleScanner.isScanning) return
        viewModelScope.launch {
            refreshBleScanSafely()
        }
    }

    private suspend fun refreshBleScanSafely() {
        if (!_uiState.value.isScanning) return
        lastScanRefreshMs = System.currentTimeMillis()
        bleScanner.stopScan()
        delay(300)
        if (_uiState.value.isScanning) {
            val restarted = bleScanner.startScan()
            if (!restarted) {
                _uiState.update { it.copy(isScanning = false, packetsPerSecond = 0) }
            }
        }
    }

    fun toggleDemoMode(enable: Boolean) {
        AppDiagnosticLogger.log("CONFIG", "Demo Mode -> $enable (bikes=${_uiState.value.demoBikeCount}, protocol=${_uiState.value.selectedProtocol.id})")
        _uiState.update { it.copy(isDemoMode = enable) }
        if (enable) {
            startDemoSimulation()
        } else {
            stopDemoSimulation()
        }
    }

    fun setDemoBikeCount(count: Int) {
        val clamped = count.coerceIn(1, 30)
        _uiState.update { it.copy(demoBikeCount = clamped) }
        if (_uiState.value.isDemoMode) {
            (1..30).forEach { bikeMap.remove(it) }
            startDemoSimulation()
        }
    }

    private fun startDemoSimulation() {
        demoJob?.cancel()
        val protocol = _uiState.value.selectedProtocol
        val activeDemoConfigs = generateDemoConfigs(_uiState.value.demoBikeCount)
        activeDemoConfigs.forEach { cfg ->
            val ftpPct = (cfg.baseWatts * 100) / cfg.ftp
            val demoBrand = demoBrandFor(protocol, cfg.id)
            val eqType = demoEquipmentTypeFor(protocol, cfg.id)
            val initialCadence = demoBaseCadenceFor(eqType, cfg.baseRpm)
            bikeMap[cfg.id] = BikeData(
                bikeId = cfg.id,
                displayId = demoDisplayIdFor(protocol, cfg.id),
                cadenceRpm = initialCadence,
                heartRateBpm = 0f,
                powerWatts = cfg.baseWatts,
                calories = 80 + cfg.id * 9,
                distance = 1.25f + cfg.id * 0.25f,
                distanceMeters = 1250 + cfg.id * 140,
                distanceUnit = if (demoBrand == "Concept2") "M" else "MI",
                gear = cfg.gear,
                lastSeenTimestamp = System.currentTimeMillis(),
                isStale = false,
                isInstructorBike = _uiState.value.instructorBikeKey.isNotEmpty() &&
                    RiderProfileRepository.instructorKeyFor("", cfg.id) == _uiState.value.instructorBikeKey,
                riderName = cfg.name,
                brand = demoBrand,
                brandType = BrandType.fromBrandString(demoBrand),
                equipmentType = eqType,
                ftp = cfg.ftp,
                ftpPercent = ftpPct,
                ftpZone = FtpZone.fromFtpPercent(ftpPct, _uiState.value.zoneCount)
            )
        }
        refreshUiState()

        demoJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                activeDemoConfigs.forEach { cfg ->
                    val existing = bikeMap[cfg.id] ?: return@forEach
                    val isRowOrSki = existing.equipmentType == "ROWERG" || existing.equipmentType == "SKIERG"
                    val rpmDelta = if (isRowOrSki) (Random.nextFloat() * 2f - 1f) else (Random.nextFloat() * 4f - 2f)
                    val wattsDelta = Random.nextInt(-8, 9)

                    val newRpm = when (existing.equipmentType) {
                        "ROWERG" -> (existing.cadenceRpm + rpmDelta).coerceIn(22f, 34f)
                        "SKIERG" -> (existing.cadenceRpm + rpmDelta).coerceIn(30f, 44f)
                        else -> (existing.cadenceRpm + rpmDelta).coerceIn(65f, 115f)
                    }
                    val newWatts = (existing.powerWatts + wattsDelta).coerceIn(80, 450)
                    val metersGain = (newWatts / 45).coerceAtLeast(2)
                    val newMeters = existing.distanceMeters + metersGain
                    val newDist = existing.distance + (metersGain / 1000f)
                    val newCal = existing.calories + if (newWatts > 150) 1 else 0

                    val ftpPct = if (existing.ftp > 0) (newWatts * 100) / existing.ftp else 0

                    val updated = existing.copy(
                        cadenceRpm = Math.round(newRpm * 10f) / 10f,
                        powerWatts = newWatts,
                        avgWatts = ((existing.avgWatts.takeIf { it > 0 } ?: newWatts) * 4 + newWatts) / 5,
                        caloriesPerHour = if (newWatts > 0) (newWatts * 3.4416 + 300.0).toInt() else 0,
                        speedKmh = Math.round((16f + (newWatts / 15f)) * 10f) / 10f,
                        liveSplitSeconds = if (existing.brandType == BrandType.WATTBIKE && newWatts > 0) {
                            3600f / (16f + (newWatts / 15f))
                        } else {
                            existing.liveSplitSeconds
                        },
                        distance = Math.round(newDist * 100f) / 100f,
                        distanceMeters = newMeters,
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

                    if (_uiState.value.isSessionTimerRunning) {
                        recordSessionSample(updated)
                    }

                    if (_uiState.value.intervalState.isActive) {
                        recordIntervalSample(updated)
                    }
                }
                refreshUiState()
            }
        }
    }

    private fun stopDemoSimulation() {
        demoJob?.cancel()
        demoJob = null
        (1..30).forEach { bikeMap.remove(it) }
        refreshUiState()
    }

    // Freestyle Lap / Interval & Session Summary Tracking
    private fun recordSessionSample(bike: BikeData) {
        val acc = sessionAccumulators.getOrPut(bike.bikeId) {
            IntervalAccumulator(
                bikeId = bike.bikeId,
                displayId = bike.displayId,
                riderName = bike.riderName,
                ftp = bike.ftp,
                equipmentType = bike.equipmentType
            )
        }
        sessionAccumulators[bike.bikeId] = acc.copy(
            displayId = bike.displayId.ifEmpty { acc.displayId },
            riderName = bike.riderName.ifEmpty { acc.riderName },
            ftp = bike.ftp,
            equipmentType = bike.equipmentType.ifEmpty { acc.equipmentType },
            wattSum = acc.wattSum + bike.powerWatts,
            cadenceSum = acc.cadenceSum + bike.cadenceRpm,
            peakWatts = maxOf(acc.peakWatts, bike.powerWatts),
            sampleCount = acc.sampleCount + 1
        )
    }

    private fun recordIntervalSample(bike: BikeData) {
        val acc = intervalAccumulators.getOrPut(bike.bikeId) {
            IntervalAccumulator(
                bikeId = bike.bikeId,
                displayId = bike.displayId,
                riderName = bike.riderName,
                ftp = bike.ftp,
                equipmentType = bike.equipmentType
            )
        }
        val updatedAcc = acc.copy(
            displayId = bike.displayId.ifEmpty { acc.displayId },
            riderName = bike.riderName.ifEmpty { acc.riderName },
            ftp = bike.ftp,
            equipmentType = bike.equipmentType.ifEmpty { acc.equipmentType },
            wattSum = acc.wattSum + bike.powerWatts,
            cadenceSum = acc.cadenceSum + bike.cadenceRpm,
            peakWatts = maxOf(acc.peakWatts, bike.powerWatts),
            sampleCount = acc.sampleCount + 1
        )
        intervalAccumulators[bike.bikeId] = updatedAcc
    }

    private fun buildCompletedInterval(): CompletedInterval {
        val state = _uiState.value.intervalState
        val zoneCount = _uiState.value.zoneCount
        val stats = if (intervalAccumulators.isNotEmpty()) {
            intervalAccumulators.values
                .map { computeBikeIntervalStats(it, zoneCount) }
                .sortedBy { it.bikeId }
        } else {
            bikeMap.values
                .filter { !it.isStale }
                .map { b ->
                    computeBikeIntervalStats(
                        IntervalAccumulator(
                            bikeId = b.bikeId,
                            displayId = b.displayId,
                            riderName = b.riderName,
                            ftp = b.ftp,
                            wattSum = b.powerWatts.toLong(),
                            cadenceSum = b.cadenceRpm.toDouble(),
                            peakWatts = b.powerWatts,
                            sampleCount = 1,
                            equipmentType = b.equipmentType
                        ),
                        zoneCount
                    )
                }
                .sortedBy { it.bikeId }
        }

        val classAvgW = if (stats.isNotEmpty()) stats.map { it.avgWatts }.average().toInt() else 0
        val classAvgCad = if (stats.isNotEmpty()) {
            Math.round(stats.map { it.avgCadence }.average().toFloat() * 10f) / 10f
        } else 0f

        return CompletedInterval(
            intervalNumber = state.currentIntervalNumber.coerceAtLeast(1),
            durationSeconds = state.elapsedSeconds,
            startSessionSeconds = state.startSessionSeconds,
            classAvgWatts = classAvgW,
            classAvgCadence = classAvgCad,
            bikeStats = stats
        )
    }

    fun onStopwatchButtonClick() {
        val current = _uiState.value.intervalState
        if (!current.isActive && (current.completedIntervals.isEmpty() || current.hasCompletedSummary)) {
            if (current.hasCompletedSummary) {
                sessionAccumulators.clear()
                intervalAccumulators.clear()
                _uiState.update {
                    it.copy(
                        intervalState = IntervalState(),
                        ftpTest = if (it.ftpTest.isActive) it.ftpTest else FtpTestState()
                    )
                }
            }
            startOrNextInterval()
        } else {
            _uiState.update { state ->
                state.copy(
                    intervalState = state.intervalState.copy(
                        isStripExpanded = !state.intervalState.isStripExpanded
                    )
                )
            }
        }
    }

    fun startOrNextInterval() {
        val current = _uiState.value.intervalState
        val baseHistory = if (current.hasCompletedSummary) emptyList() else current.completedIntervals
        val updatedHistory = if (current.isActive) {
            baseHistory + buildCompletedInterval()
        } else {
            baseHistory
        }

        intervalAccumulators.clear()
        val nextNum = updatedHistory.size + 1
        _uiState.update {
            it.copy(
                isSessionTimerRunning = true,
                intervalState = IntervalState(
                    isActive = true,
                    isStripExpanded = true,
                    currentIntervalNumber = nextNum,
                    elapsedSeconds = 0L,
                    startSessionSeconds = it.sessionElapsedSeconds,
                    liveClassAvgWatts = it.averagePower,
                    liveClassAvgCadence = it.averageCadence,
                    completedIntervals = updatedHistory,
                    isViewingSummary = false
                )
            )
        }
        restartMasterClockLoop()
    }

    fun stopInterval() {
        val current = _uiState.value.intervalState
        if (!current.isActive) return

        val finished = buildCompletedInterval()
        intervalAccumulators.clear()
        val updatedHistory = current.completedIntervals + finished

        _uiState.update {
            it.copy(
                intervalState = it.intervalState.copy(
                    isActive = false,
                    isStripExpanded = true,
                    elapsedSeconds = 0L,
                    completedIntervals = updatedHistory,
                    isViewingSummary = false
                )
            )
        }
    }

    fun endWorkout() {
        AppDiagnosticLogger.log("WORKOUT", "End Workout at session=${_uiState.value.sessionElapsedSeconds}s, intervals=${_uiState.value.intervalState.completedIntervals.size}")
        sessionTimerJob?.cancel()
        sessionTimerJob = null

        val currentIntervalState = _uiState.value.intervalState
        val finalIntervals = if (currentIntervalState.isActive) {
            currentIntervalState.completedIntervals + buildCompletedInterval()
        } else {
            currentIntervalState.completedIntervals
        }
        intervalAccumulators.clear()

        val zoneCount = _uiState.value.zoneCount
        val sessionStats = if (sessionAccumulators.isNotEmpty()) {
            sessionAccumulators.values
                .map { computeBikeIntervalStats(it, zoneCount) }
                .sortedBy { it.bikeId }
        } else {
            bikeMap.values
                .filter { !it.isStale }
                .map { b ->
                    computeBikeIntervalStats(
                        IntervalAccumulator(
                            bikeId = b.bikeId,
                            displayId = b.displayId,
                            riderName = b.riderName,
                            ftp = b.ftp,
                            wattSum = b.powerWatts.toLong(),
                            cadenceSum = b.cadenceRpm.toDouble(),
                            peakWatts = b.powerWatts,
                            sampleCount = 1,
                            equipmentType = b.equipmentType
                        ),
                        zoneCount
                    )
                }
                .sortedBy { it.bikeId }
        }

        val sessionAvgW = if (sessionStats.isNotEmpty()) sessionStats.map { it.avgWatts }.average().toInt() else 0
        val sessionAvgCad = if (sessionStats.isNotEmpty()) {
            Math.round(sessionStats.map { it.avgCadence }.average().toFloat() * 10f) / 10f
        } else 0f

        _uiState.update {
            it.copy(
                isSessionTimerRunning = false,
                intervalState = IntervalState(
                    isActive = false,
                    isStripExpanded = false,
                    elapsedSeconds = 0L,
                    completedIntervals = finalIntervals,
                    sessionDurationSeconds = it.sessionElapsedSeconds,
                    sessionClassAvgWatts = sessionAvgW,
                    sessionClassAvgCadence = sessionAvgCad,
                    sessionBikeStats = sessionStats,
                    isViewingSummary = true,
                    hasCompletedSummary = true
                )
            )
        }
    }

    fun finishWorkoutSummary() {
        sessionTimerJob?.cancel()
        sessionTimerJob = null
        sessionAccumulators.clear()
        intervalAccumulators.clear()
        _uiState.update {
            it.copy(
                sessionElapsedSeconds = 0L,
                isSessionTimerRunning = false,
                intervalState = it.intervalState.copy(
                    isActive = false,
                    isStripExpanded = false,
                    elapsedSeconds = 0L,
                    isViewingSummary = false,
                    hasCompletedSummary = true
                )
            )
        }
    }

    fun reopenWorkoutSummary() {
        if (_uiState.value.intervalState.hasCompletedSummary) {
            _uiState.update {
                it.copy(
                    intervalState = it.intervalState.copy(
                        isViewingSummary = true
                    )
                )
            }
        }
    }

    fun clearIntervals() {
        sessionAccumulators.clear()
        intervalAccumulators.clear()
        _uiState.update {
            it.copy(intervalState = IntervalState())
        }
    }

    // Group FTP Test Mode
    fun startFtpTest(minutes: Int, factor: Float) {
        AppDiagnosticLogger.log("WORKOUT", "Started $minutes-min FTP Test (factor=$factor)")
        val totalSecs = minutes * 60
        val current = _uiState.value
        val shouldClearOldSummary = current.sessionElapsedSeconds == 0L && current.intervalState.hasCompletedSummary
        if (shouldClearOldSummary) {
            sessionAccumulators.clear()
            intervalAccumulators.clear()
        }
        _uiState.update {
            it.copy(
                isSessionTimerRunning = true,
                intervalState = if (shouldClearOldSummary) IntervalState() else it.intervalState,
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
        restartMasterClockLoop()

        ftpTimerJob?.cancel()
        ftpTimerJob = viewModelScope.launch {
            while (_uiState.value.ftpTest.prepCountdownSeconds > 0) {
                delay(1000)
                val currentPrep = _uiState.value.ftpTest.prepCountdownSeconds
                val nextPrep = currentPrep - 1
                if (nextPrep > 0) {
                    _uiState.update { it.copy(ftpTest = it.ftpTest.copy(prepCountdownSeconds = nextPrep)) }
                } else {
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

        val results = bikeMap.values
            .filter { bike ->
                val eq = bike.equipmentType.uppercase()
                eq != "ROWERG" && eq != "SKIERG"
            }
            .map { bike ->
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
                    pendingResults = null,
                    lastCompletedResults = results,
                    isViewingResults = false
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
            AppDiagnosticLogger.log("CONFIG", "Zone count -> $count")
            _uiState.update { it.copy(zoneCount = count) }
            bikeMap.replaceAll { _, bike ->
                bike.copy(ftpZone = FtpZone.fromFtpPercent(bike.ftpPercent, count))
            }
            refreshUiState()
            viewModelScope.launch {
                repository.saveZoneCount(count)
            }
        }
    }

    fun updateRiderProfile(bikeId: Int, riderName: String, ftp: Int) {
        val clampedFtp = ftp.coerceIn(
            RiderProfileRepository.MIN_FTP_WATTS,
            RiderProfileRepository.MAX_FTP_WATTS
        )
        val cleanName = riderName.trim().ifEmpty { "Bike #$bikeId" }
        AppDiagnosticLogger.log("CONFIG", "Rider profile edited: bikeId=$bikeId name='$cleanName' ftp=${clampedFtp}W")

        bikeMap[bikeId]?.let { existing ->
            val newPct = (existing.powerWatts * 100) / clampedFtp
            bikeMap[bikeId] = existing.copy(
                riderName = cleanName,
                ftp = clampedFtp,
                ftpPercent = newPct,
                ftpZone = FtpZone.fromFtpPercent(newPct, _uiState.value.zoneCount)
            )
            refreshUiState()
        }

        viewModelScope.launch {
            repository.saveRiderProfile(bikeId, cleanName, clampedFtp)
        }
    }

    fun setInstructorBike(bike: BikeData, isInstructor: Boolean) {
        val targetKey = if (isInstructor) {
            RiderProfileRepository.instructorKeyFor(bike.macAddress, bike.bikeId)
        } else {
            ""
        }

        _uiState.update { it.copy(instructorBikeKey = targetKey) }
        bikeMap.entries.toList().forEach { (id, b) ->
            val match = targetKey.isNotEmpty() &&
                RiderProfileRepository.instructorKeyFor(b.macAddress, id) == targetKey
            if (b.isInstructorBike != match) {
                bikeMap[id] = b.copy(isInstructorBike = match)
            }
        }
        refreshUiState()

        viewModelScope.launch {
            if (targetKey.isNotEmpty()) {
                repository.saveInstructorBikeKey(targetKey)
            } else {
                repository.clearInstructorBikeKey()
            }
        }
    }

    fun lockMachineToBikeNumber(macAddress: String, currentBikeId: Int, studioBikeNumber: Int) {
        val normalized = RiderProfileRepository.normalizeHardwareKey(macAddress)
        if (normalized.isEmpty()) return
        val targetBikeNum = studioBikeNumber.coerceIn(1, 250)

        viewModelScope.launch {
            repository.lockMachineToBikeNumber(normalized, targetBikeNum)
        }

        bikeMap.remove(currentBikeId)?.let { existing ->
            val remapped = existing.copy(
                bikeId = targetBikeNum,
                displayId = "#$targetBikeNum",
                isWhitelisted = true
            )
            bikeMap[targetBikeNum] = remapped
            refreshUiState()
        }
    }

    fun unlockMachine(macAddress: String, currentBikeId: Int) {
        val normalized = RiderProfileRepository.normalizeHardwareKey(macAddress)
        if (normalized.isEmpty()) return

        viewModelScope.launch {
            repository.unlockMachine(normalized)
        }

        bikeMap[currentBikeId]?.let { existing ->
            bikeMap[currentBikeId] = existing.copy(isWhitelisted = false)
            refreshUiState()
        }
    }

    fun setStudioLockProximityEnabled(enabled: Boolean) {
        bleScanner.studioLockProximityEnabled = enabled
        _uiState.update { it.copy(studioLockProximityEnabled = enabled) }
        viewModelScope.launch {
            repository.saveStudioLockProximityEnabled(enabled)
        }
    }

    fun setConcept2MetricMode(mode: Concept2MetricMode) {
        AppDiagnosticLogger.log("CONFIG", "Concept2/Wattbike screen mode -> ${mode.label}")
        _uiState.update { it.copy(concept2MetricMode = mode) }
        viewModelScope.launch {
            repository.saveConcept2MetricMode(mode)
        }
    }

    fun setConcept2ErgFilter(filter: Concept2ErgFilter) {
        AppDiagnosticLogger.log("CONFIG", "Concept2 erg filter -> ${filter.label}")
        bleScanner.concept2ErgFilter = filter
        gattManager.setConcept2ErgFilter(filter)
        _uiState.update { it.copy(concept2ErgFilter = filter) }
        if (_uiState.value.isDemoMode && _uiState.value.selectedProtocol == BikeProtocol.CONCEPT2) {
            (1..30).forEach { bikeMap.remove(it) }
            startDemoSimulation()
        } else {
            refreshUiState()
        }
        viewModelScope.launch {
            repository.saveConcept2ErgFilter(filter)
        }
    }

    fun setTileHeroMetric(metric: TileHeroMetric) {
        AppDiagnosticLogger.log("CONFIG", "Tile hero metric -> ${metric.displayName}")
        _uiState.update { it.copy(tileHeroMetric = metric) }
        viewModelScope.launch {
            repository.saveTileHeroMetric(metric)
        }
    }

    fun setDefaultFtp(ftp: Int) {
        val clampedFtp = ftp.coerceIn(
            RiderProfileRepository.MIN_FTP_WATTS,
            RiderProfileRepository.MAX_FTP_WATTS
        )
        _uiState.update { it.copy(defaultFtp = clampedFtp) }

        viewModelScope.launch {
            repository.saveDefaultFtp(clampedFtp)
            var updated = false
            bikeMap.keys.toList().forEach { bikeId ->
                if (!repository.hasCustomFtp(bikeId)) {
                    bikeMap[bikeId]?.let { existing ->
                        val newPct = (existing.powerWatts * 100) / clampedFtp
                        bikeMap[bikeId] = existing.copy(
                            ftp = clampedFtp,
                            ftpPercent = newPct,
                            ftpZone = FtpZone.fromFtpPercent(newPct, _uiState.value.zoneCount)
                        )
                        updated = true
                    }
                }
            }
            if (updated) {
                refreshUiState()
            }
        }
    }

    private fun refreshUiState() {
        val rawBikes = bikeMap.values.toList()
        val ergFilter = _uiState.value.concept2ErgFilter
        val isConcept2Mode = _uiState.value.selectedProtocol == BikeProtocol.CONCEPT2
        val visibleBikes = if (isConcept2Mode && ergFilter != Concept2ErgFilter.ALL) {
            rawBikes.filter { bike ->
                if (bike.brandType != BrandType.CONCEPT2) {
                    true
                } else {
                    val eq = bike.equipmentType.uppercase()
                    when (ergFilter) {
                        Concept2ErgFilter.ALL -> true
                        Concept2ErgFilter.BIKEERG_ONLY -> eq == "BIKEERG"
                        Concept2ErgFilter.ROWERG_ONLY -> eq == "ROWERG"
                        Concept2ErgFilter.SKIERG_ONLY -> eq == "SKIERG"
                    }
                }
            }
        } else {
            rawBikes
        }

        val activeBikes = visibleBikes.filter { !it.isStale }

        val sorted = when (_uiState.value.sortOrder) {
            SortOrder.WATTS -> visibleBikes.sortedByDescending { it.powerWatts }
            SortOrder.FTP_PERCENT -> visibleBikes.sortedByDescending { it.ftpPercent }
            SortOrder.CADENCE -> visibleBikes.sortedByDescending { it.cadenceRpm }
            SortOrder.DISTANCE -> visibleBikes.sortedByDescending { it.distance }
            SortOrder.BIKE_NUMBER -> visibleBikes.sortedBy { it.bikeId }
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

    /**
     * Layer 3 — Instant Auto-Purge on Brand Switch:
     * Immediately cancel pending GATT queues, close all open BluetoothGatt handles,
     * clear the live machine map, reset counters, and restart scanning cleanly if active.
     */
    fun setBikeProtocol(protocol: BikeProtocol) {
        viewModelScope.launch {
            val wasScanning = _uiState.value.isScanning
            if (wasScanning) {
                bleScanner.stopScan()
                antPlusReceiver.stop()
            }

            clearLiveSession()
            val defaultZones = if (protocol == BikeProtocol.KEISER) 7 else 5
            AppDiagnosticLogger.log("CONFIG", "Protocol switched -> ${protocol.id} (zoneCount=$defaultZones)")
            repository.saveBikeProtocol(protocol)
            repository.saveZoneCount(defaultZones)
            _uiState.update {
                it.copy(
                    selectedProtocol = protocol,
                    zoneCount = defaultZones,
                    packetsPerSecond = 0
                )
            }
            bleScanner.setProtocol(protocol)
            antPlusReceiver.setProtocol(protocol)

            if (_uiState.value.isDemoMode) {
                startDemoSimulation()
            }

            if (wasScanning) {
                delay(250)
                val restarted = bleScanner.startScan()
                if (restarted) {
                    lastScanRefreshMs = System.currentTimeMillis()
                    if (protocol != BikeProtocol.KEISER) {
                        antPlusReceiver.start()
                    }
                }
                _uiState.update { it.copy(isScanning = restarted) }
            }
        }
    }

    private fun demoDisplayIdFor(protocol: BikeProtocol, bikeId: Int): String = when (protocol) {
        BikeProtocol.CONCEPT2 -> String.format("396%02d", bikeId)
        BikeProtocol.FTMS_UNIVERSAL -> String.format("023%02d", bikeId)
        BikeProtocol.AUTO_DETECT -> when (bikeId) {
            7, 8 -> String.format("396%02d", bikeId)
            9, 10 -> String.format("023%02d", bikeId)
            else -> ""
        }
        else -> ""
    }

    private fun demoEquipmentTypeFor(protocol: BikeProtocol, bikeId: Int): String = when (protocol) {
        BikeProtocol.CONCEPT2 -> when (_uiState.value.concept2ErgFilter) {
            Concept2ErgFilter.BIKEERG_ONLY -> "BIKEERG"
            Concept2ErgFilter.ROWERG_ONLY -> "ROWERG"
            Concept2ErgFilter.SKIERG_ONLY -> "SKIERG"
            Concept2ErgFilter.ALL -> when {
                bikeId <= 4 -> "BIKEERG"
                bikeId <= 8 -> "ROWERG"
                else -> "SKIERG"
            }
        }
        BikeProtocol.AUTO_DETECT -> when (bikeId) {
            7 -> "BIKEERG"
            8 -> "ROWERG"
            else -> ""
        }
        else -> ""
    }

    private fun demoBaseCadenceFor(equipmentType: String, cyclingBaseRpm: Float): Float = when (equipmentType) {
        "ROWERG" -> 26f + (cyclingBaseRpm % 6f)
        "SKIERG" -> 34f + (cyclingBaseRpm % 8f)
        else -> cyclingBaseRpm
    }

    private fun demoBrandFor(protocol: BikeProtocol, bikeId: Int): String = when (protocol) {
        BikeProtocol.AUTO_DETECT -> when (bikeId) {
            1, 2 -> "Keiser"
            3, 4 -> "Stages"
            5, 6 -> "Schwinn"
            7, 8 -> "Concept2"
            9, 10 -> "Wattbike"
            else -> "PowerMeter"
        }
        BikeProtocol.KEISER -> "Keiser"
        BikeProtocol.STAGES -> "Stages"
        BikeProtocol.SCHWINN_4IIII -> "Schwinn"
        BikeProtocol.CONCEPT2 -> "Concept2"
        BikeProtocol.FTMS_UNIVERSAL -> "Wattbike"
        BikeProtocol.POWER_METERS -> "PowerMeter"
    }

    override fun onCleared() {
        super.onCleared()
        teardownBluetooth()
        antPlusReceiver.release()
        demoJob?.cancel()
        ftpTimerJob?.cancel()
        staleCheckJob?.cancel()
        sessionTimerJob?.cancel()
        scanWatchdogJob?.cancel()
        packetRateJob?.cancel()
    }

    companion object {
        private const val IDLE_TIMEOUT_MS = 7_000L
        private const val STALE_PURGE_TIMEOUT_MS = 10_000L
        private const val SCAN_SILENCE_REFRESH_MS = 60_000L
        private const val SCAN_MAX_CONTINUOUS_MS = 12 * 60 * 1000L // 12 min (beats 30-min OS scan timeout)
        private const val SCAN_REFRESH_COOLDOWN_MS = 60_000L
    }
}
