package com.keiser.studio

import android.Manifest
import android.content.pm.PackageManager
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
            Toast.makeText(this, "Bluetooth permissions granted", Toast.LENGTH_SHORT).show()
            viewModel.toggleBleScan()
        } else {
            Toast.makeText(
                this,
                "Bluetooth permission required to scan fitness fleet",
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

    private fun checkPermissionsAndToggleBle() {
        if (viewModel.uiState.value.isScanning) {
            viewModel.toggleBleScan()
            return
        }

        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
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
            viewModel.toggleBleScan()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }
}
