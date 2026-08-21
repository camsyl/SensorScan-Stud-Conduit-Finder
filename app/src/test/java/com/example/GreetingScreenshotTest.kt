package com.example

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.FerrousDetectionState
import com.example.data.MagneticReading
import com.example.data.SweepPoint
import com.example.ui.StudFinderScreen
import com.example.ui.StudFinderUiState
import com.example.ui.theme.SensorScanTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val now = System.currentTimeMillis()
    val mockHistory = listOf(
      SweepPoint(now - 4000, 0.4f, 0.1f, 4f),
      SweepPoint(now - 3000, 1.2f, 0.2f, 12f),
      SweepPoint(now - 2000, 4.5f, 0.4f, 45f),
      SweepPoint(now - 1000, 7.8f, 0.5f, 78f),
      SweepPoint(now, 6.2f, 0.3f, 62f)
    )

    val sampleState = StudFinderUiState(
      reading = MagneticReading(
        rawX = 18.2f,
        rawY = -24.6f,
        rawZ = 38.1f,
        rawMagnitude = 48.9f,
        filteredMagnitude = 54.2f,
        ambientBaseline = 48.0f,
        deltaMagnitude = 6.2f,
        proximityPercent = 62.0f,
        detectionState = FerrousDetectionState.CENTER_TARGET,
        isAcHazardActive = false
      ),
      peakDelta = 7.8f,
      sweepHistory = mockHistory
    )

    composeTestRule.setContent {
      SensorScanTheme {
        StudFinderScreen(
          state = sampleState,
          onTareZero = {},
          onSetSensitivity = {},
          onSetScanMode = {},
          onSetAudioMode = {},
          onSetHapticsEnabled = {},
          onToggleSimulation = {},
          onResetPeak = {},
          onShowEducationalSheet = {},
          onShowProDialog = {},
          onSelectProTier = {},
          onUnlockPro = {},
          onRestorePurchases = {},
          onDismissMessageBanner = {},
          onDismissInterstitialAd = {},
          modifier = Modifier.fillMaxSize()
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}

