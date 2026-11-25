package com.keiser.studio

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.keiser.studio.ui.screens.LeaderboardScreen
import com.keiser.studio.ui.theme.BgMain
import com.keiser.studio.ui.theme.KeiserStudioTheme
import com.keiser.studio.viewmodel.KeiserViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: KeiserViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Toast.makeText(this, "Bluetooth & Location permissions granted", Toast.LENGTH_SHORT).show()
            startScanIfRadiosReady()
        } else {
            Toast.makeText(
                this,
                "Bluetooth and Location permissions are required to scan fitness fleet broadcasts",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Keep the screen awake during active studio projector sessions
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            KeiserStudioTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BgMain
                ) {
                    val uiState by viewModel.uiState.collectAsState()

                    LeaderboardScreen(
                        uiState = uiState,
                        onSortChange = viewModel::setSortOrder,
                        onDemoToggle = viewModel::toggleDemoMode,
                        onBleToggle = { checkPermissionsAndToggleBle() },
                        onStartFtpTest = viewModel::startFtpTest,
                        onFinishFtpEarly = viewModel::finishFtpTestEarly,
                        onCancelFtpTest = viewModel::cancelFtpTest,
                        onDismissFtpResults = viewModel::dismissFtpResults,
                        onViewLastFtpResults = viewModel::showLastFtpResults,
                        onViewModeChange = viewModel::setViewMode,
                        onZoneCountChange = viewModel::setZoneCount,
                        onUpdateRiderProfile = viewModel::updateRiderProfile,
                        onDefaultFtpChange = viewModel::setDefaultFtp,
                        onOpenSettings = viewModel::openSettings,
                        onCloseSettings = viewModel::closeSettings,
                        onSelectProtocol = viewModel::setBikeProtocol,
                        onToggleSessionTimer = viewModel::toggleSessionTimer,
                        onResetSessionTimer = viewModel::resetSessionTimer
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAppResumed()
    }

    private fun checkPermissionsAndToggleBle() {
        if (viewModel.uiState.value.isScanning) {
            viewModel.toggleBleScan()
            return
        }

        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }

        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            startScanIfRadiosReady()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun startScanIfRadiosReady() {
        val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        if (btManager?.adapter?.isEnabled != true) {
            Toast.makeText(this, "Please turn on Bluetooth to scan for bikes", Toast.LENGTH_LONG).show()
            return
        }

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val isLocationOn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager?.isLocationEnabled == true
        } else {
            @Suppress("DEPRECATION")
            locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
        }

        if (!isLocationOn) {
            Toast.makeText(
                this,
                "Please turn on Location Services so Android allows passive BLE bike broadcasts",
                Toast.LENGTH_LONG
            ).show()
        }

        viewModel.toggleBleScan()
    }
}
