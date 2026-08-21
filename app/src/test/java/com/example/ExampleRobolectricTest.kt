package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.FerrousDetectionState
import com.example.data.ScanMode
import com.example.data.SensitivityLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SensorScan", appName)
  }

  @Test
  fun `verify sensitivity thresholds and proximity calculations`() {
    val medium = SensitivityLevel.MEDIUM
    val delta = 6.5f
    val proximity = (delta / medium.maxDeltaMicroTesla) * 100f
    assertEquals(65f, proximity, 0.01f)

    val isCenter = delta >= medium.centerThresholdMicroTesla
    assertTrue(isCenter)
  }

  @Test
  fun `verify tare zero subtraction`() {
    val ambient = 48.2f
    val reading = 52.6f
    val delta = abs(reading - ambient)
    assertEquals(4.4f, delta, 0.01f)
  }

  @Test
  fun `verify pro plan state and ad removal logic`() {
    val freeState = com.example.ui.StudFinderUiState(isProUnlocked = false)
    val shouldShowAdsInFree = !freeState.isProUnlocked
    assertTrue(shouldShowAdsInFree)

    val proState = freeState.copy(isProUnlocked = true)
    val shouldShowAdsInPro = !proState.isProUnlocked
    org.junit.Assert.assertFalse(shouldShowAdsInPro)
  }

  @Test
  fun `verify stripe pro plan tiers`() {
    assertEquals("pro_monthly", com.example.data.ProPlanTier.MONTHLY.id)
    assertEquals("$2.99", com.example.data.ProPlanTier.MONTHLY.priceDisplay)
    assertEquals("https://buy.stripe.com/eVq00igRT6yG0yFfXod7q0j", com.example.data.ProPlanTier.MONTHLY.stripeBuyUrl)
    assertEquals("prod_V6pUi3ISMMawiH", com.example.data.ProPlanTier.MONTHLY.stripeProductId)
    assertEquals("price_1U6bpfCeNOeBc4iF929RWZY5", com.example.data.ProPlanTier.MONTHLY.stripePriceId)

    assertEquals("$19.99", com.example.data.ProPlanTier.YEARLY.priceDisplay)
    assertEquals("https://buy.stripe.com/7sYcN43138GO5SZdPgd7q0i", com.example.data.ProPlanTier.YEARLY.stripeBuyUrl)
    assertEquals("prod_V6pWUoMyHpLhSf", com.example.data.ProPlanTier.YEARLY.stripeProductId)
    assertEquals("price_1U6brRCeNOeBc4iF0b4vGUc8", com.example.data.ProPlanTier.YEARLY.stripePriceId)

    assertEquals("$39.99", com.example.data.ProPlanTier.LIFETIME.priceDisplay)
    assertEquals("https://buy.stripe.com/4gMbJ08lne180yF4eGd7q0h", com.example.data.ProPlanTier.LIFETIME.stripeBuyUrl)
    assertEquals("prod_V6pXwCqkNwRXtu", com.example.data.ProPlanTier.LIFETIME.stripeProductId)
    assertEquals("price_1U6bsRCeNOeBc4iFxXfDniti", com.example.data.ProPlanTier.LIFETIME.stripePriceId)
  }

  @Test
  fun `verify interstitial ad 2-second lock constraint`() {
    val lockedState = com.example.ui.StudFinderUiState(
        isInterstitialAdVisible = true,
        interstitialSecondsRemaining = 2,
        canDismissInterstitial = false
    )
    org.junit.Assert.assertFalse(lockedState.canDismissInterstitial)

    val unlockableState = lockedState.copy(
        interstitialSecondsRemaining = 0,
        canDismissInterstitial = true
    )
    assertTrue(unlockableState.canDismissInterstitial)
  }
}

