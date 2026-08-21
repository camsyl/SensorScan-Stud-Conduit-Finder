package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.StudFinderScreen
import com.example.ui.StudFinderViewModel
import com.example.ui.theme.SensorScanTheme

class MainActivity : ComponentActivity() {

    private val viewModel: StudFinderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SensorScanTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                StudFinderScreen(
                    state = uiState,
                    onTareZero = viewModel::tareZero,
                    onSetSensitivity = viewModel::setSensitivity,
                    onSetScanMode = viewModel::setScanMode,
                    onSetAudioMode = viewModel::setAudioMode,
                    onSetHapticsEnabled = viewModel::setHapticsEnabled,
                    onResetPeak = viewModel::resetPeak,
                    onShowEducationalSheet = viewModel::showEducationalSheet,
                    onShowProDialog = viewModel::setProDialogVisible,
                    onSelectProTier = viewModel::setSelectedTier,
                    onUnlockPro = viewModel::unlockPro,
                    onRestorePurchases = viewModel::restorePurchases,
                    onDismissMessageBanner = viewModel::dismissMessageBanner,
                    onDismissInterstitialAd = viewModel::dismissInterstitialAd,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()
    }
}

