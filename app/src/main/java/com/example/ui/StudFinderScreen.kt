package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SensorsOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AudioMode
import com.example.data.FerrousDetectionState
import com.example.data.ProPlanTier
import com.example.data.ScanMode
import com.example.data.SensitivityLevel
import com.example.data.SweepPoint
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.AmberSecondary
import com.example.ui.theme.CarbonBackground
import com.example.ui.theme.CarbonBorder
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CarbonSurfaceHighlight
import com.example.ui.theme.CarbonSurfaceVariant
import com.example.ui.theme.CyanElectric
import com.example.ui.theme.CyanElectricGlow
import com.example.ui.theme.HazardRed
import com.example.ui.theme.TargetGreen
import com.example.ui.theme.TextDisabled
import com.example.ui.theme.TextHighEmphasis
import com.example.ui.theme.TextMediumEmphasis
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudFinderScreen(
    state: StudFinderUiState,
    onTareZero: () -> Unit,
    onSetSensitivity: (SensitivityLevel) -> Unit,
    onSetScanMode: (ScanMode) -> Unit,
    onSetAudioMode: (AudioMode) -> Unit,
    onSetHapticsEnabled: (Boolean) -> Unit,
    onToggleSimulation: (Boolean) -> Unit,
    onResetPeak: () -> Unit,
    onShowEducationalSheet: (Boolean) -> Unit,
    onShowProDialog: (Boolean) -> Unit,
    onSelectProTier: (ProPlanTier) -> Unit,
    onUnlockPro: () -> Unit,
    onRestorePurchases: () -> Unit,
    onDismissMessageBanner: () -> Unit,
    onDismissInterstitialAd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("stud_finder_scaffold"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "SensorScan",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (state.isProUnlocked) TargetGreen.copy(alpha = 0.2f)
                                        else AmberPrimary.copy(alpha = 0.2f)
                                    )
                                    .border(
                                        1.dp,
                                        if (state.isProUnlocked) TargetGreen.copy(alpha = 0.6f)
                                        else AmberPrimary.copy(alpha = 0.5f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (state.isProUnlocked) "PRO ACTIVE" else "FREE TIER",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.isProUnlocked) TargetGreen else AmberPrimary
                                )
                            }
                        }
                        Text(
                            text = "STUD & CONDUIT DETECTOR",
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.2.sp,
                            color = TextMediumEmphasis
                        )
                    }
                },
                actions = {
                    // Pro plan action button if free tier
                    if (!state.isProUnlocked) {
                        IconButton(
                            onClick = { onShowProDialog(true) },
                            modifier = Modifier.testTag("pro_upgrade_topbar_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = "Upgrade to Pro Plan (Remove Ads)",
                                tint = AmberPrimary
                            )
                        }
                    }

                    // Demo / Simulation mode toggle
                    IconButton(
                        onClick = { onToggleSimulation(!state.isSimulatedMode) },
                        modifier = Modifier.testTag("simulation_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (state.isSimulatedMode) Icons.Default.SensorsOff else Icons.Default.Sensors,
                            contentDescription = if (state.isSimulatedMode) "Hardware Simulation Active" else "Physical Sensor Active",
                            tint = if (state.isSimulatedMode) AmberSecondary else CyanElectric
                        )
                    }

                    // Reset Peak
                    IconButton(
                        onClick = onResetPeak,
                        modifier = Modifier.testTag("reset_peak_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Peak Delta",
                            tint = TextMediumEmphasis
                        )
                    }

                    // Educational Guide
                    IconButton(
                        onClick = { onShowEducationalSheet(true) },
                        modifier = Modifier.testTag("educational_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Drywall Physics Guide",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .widthIn(max = 640.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Notification Message Banner (e.g. Pro Activated)
            AnimatedVisibility(
                visible = state.messageBanner != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                if (state.messageBanner != null) {
                    MessageBannerCard(
                        message = state.messageBanner,
                        onDismiss = onDismissMessageBanner
                    )
                }
            }

            // Top Ad Banner for Free Users
            if (!state.isProUnlocked) {
                AdBannerCard(
                    onUpgradeClicked = { onShowProDialog(true) },
                    testTag = "top_ad_banner"
                )
            }

            // Hardware missing notice or simulation badge
            if (!state.isHardwareAvailable) {
                HardwareWarningBanner(isSimulated = state.isSimulatedMode)
            }

            // Live AC Hazard Warning Card
            AnimatedVisibility(
                visible = state.reading.isAcHazardActive,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                AcHazardWarningCard(
                    variance = state.reading.acVariance,
                    peakToPeak = state.reading.acPeakToPeak
                )
            }

            // Mode Selector Segmented Chips
            ScanModeSelector(
                currentMode = state.scanMode,
                onModeSelected = onSetScanMode
            )

            // Primary Target Proximity Visualizer Gauge
            FerrousTargetVisualizer(
                proximityPercent = state.reading.proximityPercent,
                detectionState = state.reading.detectionState,
                deltaMagnitude = state.reading.deltaMagnitude,
                peakDelta = state.peakDelta,
                ambientBaseline = state.reading.ambientBaseline,
                rawMagnitude = state.reading.rawMagnitude,
                isCalibrating = state.isCalibrating
            )

            // Real-Time Sweep Visualizer (Scrolling Oscilloscope Canvas)
            SweepOscilloscopeCard(
                history = state.sweepHistory,
                currentDelta = state.reading.deltaMagnitude,
                currentVariance = state.reading.acVariance,
                thresholdAnomaly = state.sensitivity.anomalyThresholdMicroTesla,
                thresholdCenter = state.sensitivity.centerThresholdMicroTesla,
                maxScale = state.sensitivity.maxDeltaMicroTesla
            )

            // Prominent "TARE / CALIBRATE ZERO" Button
            TareCalibrationButton(
                isCalibrating = state.isCalibrating,
                baseline = state.reading.ambientBaseline,
                onTare = onTareZero
            )

            // Sensitivity Presets (Coarse, Medium, Fine)
            SensitivitySelectorCard(
                currentLevel = state.sensitivity,
                onLevelSelected = onSetSensitivity
            )

            // Audio & Haptic Control Deck
            FeedbackControlsCard(
                audioMode = state.audioMode,
                isHapticsEnabled = state.isHapticsEnabled,
                onAudioModeChanged = onSetAudioMode,
                onHapticsChanged = onSetHapticsEnabled
            )

            // Precision Sensor Telemetry Details
            SensorTelemetryCard(
                rawX = state.reading.rawX,
                rawY = state.reading.rawY,
                rawZ = state.reading.rawZ,
                rawMag = state.reading.rawMagnitude,
                filteredMag = state.reading.filteredMagnitude,
                acVariance = state.reading.acVariance,
                acPeakToPeak = state.reading.acPeakToPeak
            )

            // Bottom Ad Banner for Free Users or Pro Plan Showcase
            if (!state.isProUnlocked) {
                AdBannerCard(
                    onUpgradeClicked = { onShowProDialog(true) },
                    testTag = "bottom_ad_banner"
                )

                ProUpgradePitchCard(
                    onUpgradeClicked = { onShowProDialog(true) }
                )
            } else {
                ProStatusActiveCard()
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Fullscreen Interstitial Ad Overlay (appears periodically on free tier; locked for 2s before tap to close)
    if (state.isInterstitialAdVisible && !state.isProUnlocked) {
        FullscreenInterstitialAdDialog(
            secondsRemaining = state.interstitialSecondsRemaining,
            canDismiss = state.canDismissInterstitial,
            onDismiss = onDismissInterstitialAd,
            onUpgradeClicked = {
                onDismissInterstitialAd()
                onShowProDialog(true)
            }
        )
    }

    // Pro Plan Upgrade Dialog with Stripe Tier Selection
    if (state.isProDialogVisible) {
        ProUpgradeDialog(
            isProUnlocked = state.isProUnlocked,
            selectedTier = state.selectedTier,
            monthlyBuyUrl = state.monthlyBuyUrl,
            yearlyBuyUrl = state.yearlyBuyUrl,
            lifetimeBuyUrl = state.lifetimeBuyUrl,
            onSelectTier = onSelectProTier,
            onDismiss = { onShowProDialog(false) },
            onUpgrade = onUnlockPro,
            onRestore = onRestorePurchases
        )
    }

    // Educational Guide Modal Bottom Sheet
    if (state.isEducationalSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { onShowEducationalSheet(false) },
            sheetState = sheetState,
            containerColor = CarbonSurface,
            contentColor = TextHighEmphasis
        ) {
            EducationalGuideSheet(
                onDismiss = { onShowEducationalSheet(false) }
            )
        }
    }
}

/**
 * Ferrous Target Proximity Visualizer with animated crosshairs,
 * detection status banner, and digital readouts.
 */
@Composable
fun FerrousTargetVisualizer(
    proximityPercent: Float,
    detectionState: FerrousDetectionState,
    deltaMagnitude: Float,
    peakDelta: Float,
    ambientBaseline: Float,
    rawMagnitude: Float,
    isCalibrating: Boolean
) {
    val animatedProximity by animateFloatAsState(
        targetValue = proximityPercent,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "proximity_anim"
    )

    val targetColor by animateColorAsState(
        targetValue = when (detectionState) {
            FerrousDetectionState.CENTER_TARGET -> TargetGreen
            FerrousDetectionState.ANOMALY -> AmberPrimary
            FerrousDetectionState.CLEAR -> TextDisabled
        },
        animationSpec = tween(durationMillis = 150),
        label = "target_color"
    )

    // Pulse animation when direct center is reached
    val infiniteTransition = rememberInfiniteTransition(label = "center_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ferrous_target_card"),
        colors = CardDefaults.cardColors(containerColor = CarbonSurface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.5.dp,
            if (detectionState == FerrousDetectionState.CENTER_TARGET) TargetGreen
            else if (detectionState == FerrousDetectionState.ANOMALY) AmberPrimary.copy(alpha = 0.6f)
            else CarbonBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status Header Banner
            DetectionStatusBanner(detectionState = detectionState, isCalibrating = isCalibrating)

            Spacer(modifier = Modifier.height(16.dp))

            // Radial / Radar Target Proximity Gauge
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 14.dp.toPx()
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = (size.minDimension - strokeWidth) / 2f

                    // Background arc (240 degrees)
                    drawArc(
                        color = Color(0xFF1E2638),
                        startAngle = 150f,
                        sweepAngle = 240f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Active Proximity Sweep Arc
                    val activeSweepAngle = (animatedProximity / 100f) * 240f
                    if (activeSweepAngle > 0f) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                0.0f to AmberSecondary,
                                0.7f to AmberPrimary,
                                1.0f to if (animatedProximity > 80f) TargetGreen else AmberPrimary
                            ),
                            startAngle = 150f,
                            sweepAngle = activeSweepAngle,
                            useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    // Radar grid ticks
                    for (i in 0..10) {
                        val tickAngle = Math.toRadians((150.0 + i * (240.0 / 10.0)))
                        val innerR = radius - strokeWidth * 0.75f
                        val outerR = radius + strokeWidth * 0.75f

                        val startP = Offset(
                            center.x + (innerR * kotlin.math.cos(tickAngle)).toFloat(),
                            center.y + (innerR * kotlin.math.sin(tickAngle)).toFloat()
                        )
                        val endP = Offset(
                            center.x + (outerR * kotlin.math.cos(tickAngle)).toFloat(),
                            center.y + (outerR * kotlin.math.sin(tickAngle)).toFloat()
                        )

                        drawLine(
                            color = if (i * 10 <= animatedProximity) AmberPrimary.copy(alpha = 0.8f) else Color(0x33FFFFFF),
                            start = startP,
                            end = endP,
                            strokeWidth = if (i % 5 == 0) 2.5.dp.toPx() else 1.2.dp.toPx()
                        )
                    }

                    // Framing stud crosshairs
                    val crosshairColor = targetColor.copy(alpha = 0.5f)
                    drawLine(
                        color = crosshairColor,
                        start = Offset(center.x, center.y - radius * 0.55f),
                        end = Offset(center.x, center.y + radius * 0.55f),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    drawLine(
                        color = crosshairColor,
                        start = Offset(center.x - radius * 0.55f, center.y),
                        end = Offset(center.x + radius * 0.55f, center.y),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }

                // Center readout
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (detectionState == FerrousDetectionState.CENTER_TARGET) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Target Center Locked",
                            tint = TargetGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Text(
                        text = String.format(Locale.US, "%.0f%%", animatedProximity),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = if (detectionState == FerrousDetectionState.CENTER_TARGET) TargetGreen else TextHighEmphasis
                    )

                    Text(
                        text = "PROXIMITY",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMediumEmphasis,
                        letterSpacing = 1.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Fast digital readout row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CarbonSurfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricReadoutItem(
                    label = "DELTA ΔB",
                    value = String.format(Locale.US, "%+.1f µT", deltaMagnitude),
                    highlightColor = if (deltaMagnitude > 2f) AmberPrimary else TextHighEmphasis
                )

                Box(
                    modifier = Modifier
                        .height(28.dp)
                        .width(1.dp)
                        .background(CarbonBorder)
                )

                MetricReadoutItem(
                    label = "PEAK ΔB",
                    value = String.format(Locale.US, "%.1f µT", peakDelta),
                    highlightColor = AmberSecondary
                )

                Box(
                    modifier = Modifier
                        .height(28.dp)
                        .width(1.dp)
                        .background(CarbonBorder)
                )

                MetricReadoutItem(
                    label = "TARE BASE",
                    value = String.format(Locale.US, "%.1f µT", ambientBaseline),
                    highlightColor = CyanElectric
                )
            }
        }
    }
}

@Composable
fun DetectionStatusBanner(
    detectionState: FerrousDetectionState,
    isCalibrating: Boolean
) {
    val (bgColor, textColor, labelText) = when {
        isCalibrating -> Triple(CyanElectric.copy(alpha = 0.2f), CyanElectric, "CALIBRATING BASELINE...")
        detectionState == FerrousDetectionState.CENTER_TARGET -> Triple(TargetGreen.copy(alpha = 0.2f), TargetGreen, "DIRECT CENTER: DRYWALL FASTENER")
        detectionState == FerrousDetectionState.ANOMALY -> Triple(AmberPrimary.copy(alpha = 0.2f), AmberPrimary, "FERROUS ANOMALY DETECTED")
        else -> Triple(CarbonSurfaceVariant, TextMediumEmphasis, "SCANNING: CLEAR / AMBIENT")
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, textColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = labelText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 2D Real-Time Scrolling Oscilloscope Sweep Chart Canvas.
 * Graphs the last 5 seconds of magnetic delta and AC variance spikes.
 */
@Composable
fun SweepOscilloscopeCard(
    history: List<SweepPoint>,
    currentDelta: Float,
    currentVariance: Float,
    thresholdAnomaly: Float,
    thresholdCenter: Float,
    maxScale: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sweep_oscilloscope_card"),
        colors = CardDefaults.cardColors(containerColor = CarbonSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CarbonBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = AmberPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "REAL-TIME SWEEP OSCILLOSCOPE",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextHighEmphasis
                    )
                }

                Text(
                    text = "5.0s Window",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMediumEmphasis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Canvas Sweep Display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CarbonBackground)
                    .border(1.dp, CarbonBorder, RoundedCornerShape(8.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val scaleY = height / (maxScale * 1.15f).coerceAtLeast(5.0f)

                    // Draw oscilloscope background grid
                    val gridColumns = 5
                    val gridRows = 4
                    for (c in 0..gridColumns) {
                        val gx = c * (width / gridColumns)
                        drawLine(
                            color = Color(0x18FFFFFF),
                            start = Offset(gx, 0f),
                            end = Offset(gx, height),
                            strokeWidth = 1f
                        )
                    }
                    for (r in 0..gridRows) {
                        val gy = r * (height / gridRows)
                        drawLine(
                            color = Color(0x18FFFFFF),
                            start = Offset(0f, gy),
                            end = Offset(width, gy),
                            strokeWidth = 1f
                        )
                    }

                    // Anomaly & Center Threshold guide lines
                    val yAnomaly = (height - (thresholdAnomaly * scaleY)).coerceIn(0f, height)
                    drawLine(
                        color = AmberPrimary.copy(alpha = 0.35f),
                        start = Offset(0f, yAnomaly),
                        end = Offset(width, yAnomaly),
                        strokeWidth = 1.5f
                    )

                    val yCenter = (height - (thresholdCenter * scaleY)).coerceIn(0f, height)
                    drawLine(
                        color = TargetGreen.copy(alpha = 0.4f),
                        start = Offset(0f, yCenter),
                        end = Offset(width, yCenter),
                        strokeWidth = 1.5f
                    )

                    // Plot Ferrous ΔB Sweep Path
                    if (history.size >= 2) {
                        val now = System.currentTimeMillis()
                        val windowMs = 5000f

                        val linePath = Path()
                        val fillPath = Path()

                        var isFirst = true

                        for (i in history.indices) {
                            val pt = history[i]
                            val ageMs = (now - pt.timestampMillis).toFloat().coerceAtLeast(0f)
                            val x = (width - (ageMs / windowMs) * width).coerceIn(0f, width)
                            val y = (height - (pt.deltaB * scaleY)).coerceIn(0f, height)

                            if (isFirst) {
                                linePath.moveTo(x, y)
                                fillPath.moveTo(x, height)
                                fillPath.lineTo(x, y)
                                isFirst = false
                            } else {
                                linePath.lineTo(x, y)
                                fillPath.lineTo(x, y)
                            }
                        }

                        // Close fill path
                        fillPath.lineTo(width, height)
                        fillPath.close()

                        // Gradient fill under curve
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(AmberPrimary.copy(alpha = 0.35f), Color.Transparent),
                                startY = 0f,
                                endY = height
                            )
                        )

                        // Main curve line
                        drawPath(
                            path = linePath,
                            color = AmberPrimary,
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Current sweeping scanhead line at the right edge
                    drawLine(
                        color = CyanElectric,
                        start = Offset(width - 2f, 0f),
                        end = Offset(width - 2f, height),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                // Legend overlay
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LegendTag(color = AmberPrimary, label = "ΔB (Ferrous)")
                    LegendTag(color = TargetGreen, label = "Stud Center")
                }
            }
        }
    }
}

@Composable
fun LegendTag(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(CarbonSurface.copy(alpha = 0.8f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = TextHighEmphasis
        )
    }
}

/**
 * Large Prominent TARE / ZERO Calibration Button.
 */
@Composable
fun TareCalibrationButton(
    isCalibrating: Boolean,
    baseline: Float,
    onTare: () -> Unit
) {
    Button(
        onClick = onTare,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .shadow(if (isCalibrating) 12.dp else 4.dp, RoundedCornerShape(14.dp), ambientColor = AmberPrimary)
            .testTag("tare_zero_button"),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isCalibrating) AmberSecondary else AmberPrimary,
            contentColor = Color(0xFF261900)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Tare Zero",
                tint = Color(0xFF261900),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = if (isCalibrating) "CALIBRATING BASELINE..." else "TARE / ZERO CALIBRATE",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "Press against clear drywall before sweep (${String.format(Locale.US, "%.1f", baseline)} µT)",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = Color(0xFF4A3200)
                )
            }
        }
    }
}

/**
 * Live AC Electromagnetic Conduit / Wire Hazard Warning Card.
 */
@Composable
fun AcHazardWarningCard(
    variance: Float,
    peakToPeak: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ac_alert_pulse")
    val alertAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alert_alpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ac_hazard_warning_card"),
        colors = CardDefaults.cardColors(containerColor = HazardRed.copy(alpha = 0.18f)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(2.dp, HazardRed.copy(alpha = alertAlpha))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(HazardRed),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ElectricBolt,
                    contentDescription = "Live AC Hazard",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "CAUTION: LIVE AC CONDUIT / WIRE",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFF8A80)
                )
                Text(
                    text = "50/60 Hz electromagnetic oscillation detected! Do not drill here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextHighEmphasis
                )
                Text(
                    text = String.format(Locale.US, "AC Variance: %.2f | AC P-to-P: %.2f µT", variance, peakToPeak),
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanElectric
                )
            }
        }
    }
}

/**
 * Scan Mode Selector (Dual Mode, Ferrous Only, AC EMF Only).
 */
@Composable
fun ScanModeSelector(
    currentMode: ScanMode,
    onModeSelected: (ScanMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CarbonSurface)
            .border(1.dp, CarbonBorder, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ScanMode.values().forEach { mode ->
            val isSelected = currentMode == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) AmberPrimary else Color.Transparent)
                    .clickable { onModeSelected(mode) }
                    .padding(vertical = 8.dp)
                    .testTag("mode_${mode.name.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mode.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color(0xFF261900) else TextMediumEmphasis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Sensitivity Selector Card with Fine, Medium, Coarse options.
 */
@Composable
fun SensitivitySelectorCard(
    currentLevel: SensitivityLevel,
    onLevelSelected: (SensitivityLevel) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sensitivity_selector_card"),
        colors = CardDefaults.cardColors(containerColor = CarbonSurface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, CarbonBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(
                text = "SENSOR SENSITIVITY RANGE",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = TextHighEmphasis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SensitivityLevel.values().forEach { level ->
                    val isSelected = currentLevel == level
                    FilterChip(
                        selected = isSelected,
                        onClick = { onLevelSelected(level) },
                        label = {
                            Text(
                                text = level.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sensitivity_${level.name.lowercase()}"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberPrimary,
                            selectedLabelColor = Color(0xFF261900),
                            containerColor = CarbonSurfaceVariant,
                            labelColor = TextMediumEmphasis
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${currentLevel.label} (Full Scale: ±${currentLevel.maxDeltaMicroTesla} µT)",
                style = MaterialTheme.typography.bodySmall,
                color = TextMediumEmphasis
            )
        }
    }
}

/**
 * Audio and Haptic Feedback Deck.
 */
@Composable
fun FeedbackControlsCard(
    audioMode: AudioMode,
    isHapticsEnabled: Boolean,
    onAudioModeChanged: (AudioMode) -> Unit,
    onHapticsChanged: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("feedback_controls_card"),
        colors = CardDefaults.cardColors(containerColor = CarbonSurface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, CarbonBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Audio Mode Selector
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AUDIO TONE",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMediumEmphasis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AudioMode.values().forEach { mode ->
                        val isSelected = audioMode == mode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) CyanElectric else CarbonSurfaceVariant)
                                .clickable { onAudioModeChanged(mode) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("audio_${mode.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF00363D) else TextHighEmphasis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Haptic Switch Toggle
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "HAPTICS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMediumEmphasis
                )
                Spacer(modifier = Modifier.height(4.dp))
                IconButton(
                    onClick = { onHapticsChanged(!isHapticsEnabled) },
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isHapticsEnabled) TargetGreen.copy(alpha = 0.2f) else CarbonSurfaceVariant)
                        .testTag("haptics_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Vibration,
                        contentDescription = "Toggle Tactile Feedback",
                        tint = if (isHapticsEnabled) TargetGreen else TextDisabled
                    )
                }
            }
        }
    }
}

/**
 * Raw 3-Axis Magnetometer Telemetry Details.
 */
@Composable
fun SensorTelemetryCard(
    rawX: Float,
    rawY: Float,
    rawZ: Float,
    rawMag: Float,
    filteredMag: Float,
    acVariance: Float,
    acPeakToPeak: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sensor_telemetry_card"),
        colors = CardDefaults.cardColors(containerColor = CarbonSurfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = "3-AXIS MAGNETOMETER TELEMETRY (RAW)",
                style = MaterialTheme.typography.labelSmall,
                color = TextMediumEmphasis,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = String.format(Locale.US, "X: %+.1f µT", rawX),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextHighEmphasis
                )
                Text(
                    text = String.format(Locale.US, "Y: %+.1f µT", rawY),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextHighEmphasis
                )
                Text(
                    text = String.format(Locale.US, "Z: %+.1f µT", rawZ),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextHighEmphasis
                )
                Text(
                    text = String.format(Locale.US, "|B|: %.1f µT", rawMag),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = AmberPrimary
                )
            }
        }
    }
}

@Composable
fun MetricReadoutItem(
    label: String,
    value: String,
    highlightColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = TextMediumEmphasis,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = highlightColor
        )
    }
}

@Composable
fun HardwareWarningBanner(isSimulated: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hardware_warning_banner"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSimulated) AmberPrimary.copy(alpha = 0.15f) else HazardRed.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (isSimulated) AmberPrimary else HazardRed)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSimulated) Icons.Default.Sensors else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isSimulated) AmberPrimary else HazardRed,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isSimulated)
                    "SIMULATION DEMO ACTIVE: Synthesizing real-time drywall studs and 60Hz AC conduit sweeps."
                else
                    "No hardware magnetometer detected on this device. Switch to Simulation mode to test.",
                style = MaterialTheme.typography.bodySmall,
                color = TextHighEmphasis
            )
        }
    }
}

/**
 * Educational Guide Sheet explaining the physics of drywall fastener detection.
 */
@Composable
fun EducationalGuideSheet(
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("educational_guide_sheet")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "How Magnetic Stud Finding Works",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AmberPrimary
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Close Guide",
                    tint = TextMediumEmphasis
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Why Phone Sensors Detect Screws, Not Wood:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextHighEmphasis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Smartphone magnetometers measure local magnetic flux density (in micro-Teslas, µT). Wood has no significant magnetic permeability. Instead, this tool detects the ferrous steel drywall screws and nails that attach gypsum drywall sheets to wooden framing studs every 12 to 16 inches vertically.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMediumEmphasis
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Professional Scanning Technique:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextHighEmphasis
        )
        Spacer(modifier = Modifier.height(8.dp))

        GuideStepItem(
            step = "1",
            title = "Tare / Zero on Clear Wall",
            description = "Hold your phone flat against a portion of the wall known to be free of metal, then tap the TARE button to calibrate the ambient Earth magnetic baseline."
        )

        GuideStepItem(
            step = "2",
            title = "Sweep Horizontally",
            description = "Glide the phone slowly and flat across the wall surface in a horizontal line. Watch the Delta ΔB meter and pitch tone rise when passing over a drywall fastener."
        )

        GuideStepItem(
            step = "3",
            title = "Verify Vertical Alignment",
            description = "Once a screw is located, scan vertically up and down ~12\" to 16\" to find adjacent screws in the same stud line, confirming the exact stud center."
        )

        GuideStepItem(
            step = "4",
            title = "AC Conduit & Wiring Safety",
            description = "Live alternating current (50/60 Hz) produces high-frequency magnetic variance. If the AC Hazard alert triggers, avoid drilling into that area to prevent electrical damage."
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary, contentColor = Color(0xFF261900))
        ) {
            Text("GOT IT, START SCANNING", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun GuideStepItem(
    step: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(AmberPrimary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF261900)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = TextHighEmphasis
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextMediumEmphasis
            )
        }
    }
}

/**
 * Message / Toast Banner Card
 */
@Composable
fun MessageBannerCard(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("message_banner_card"),
        colors = CardDefaults.cardColors(containerColor = TargetGreen.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, TargetGreen.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = TargetGreen,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TargetGreen,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = TargetGreen.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Simulated In-App Monetization Ad Banner
 */
@Composable
fun AdBannerCard(
    onUpgradeClicked: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = CarbonSurfaceVariant),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CarbonBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "AD" sponsor badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFE28743).copy(alpha = 0.2f))
                    .border(1.dp, Color(0xFFE28743), RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "AD",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE28743)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DeWalt & Milwaukee Tool Deals",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextHighEmphasis
                )
                Text(
                    text = "Save up to 40% on digital levels & drills",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMediumEmphasis,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedButton(
                onClick = onUpgradeClicked,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, AmberPrimary.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberPrimary)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = AmberPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Hide Ads",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Card inviting free users to unlock the Pro Plan
 */
@Composable
fun ProUpgradePitchCard(
    onUpgradeClicked: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pro_upgrade_pitch_card"),
        colors = CardDefaults.cardColors(containerColor = CarbonSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, AmberPrimary.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = AmberPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Upgrade to Pro Plan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AmberPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Remove all advertisements, enable uninterrupted real-time DSP sweep scanning, and support continuous tool calibration updates.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMediumEmphasis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onUpgradeClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("upgrade_pro_pitch_button"),
                colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary, contentColor = Color(0xFF261900)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "GO PRO - $2.99 ONE-TIME",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

/**
 * Card indicating that the Pro Plan is currently active (ad-free)
 */
@Composable
fun ProStatusActiveCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pro_status_active_card"),
        colors = CardDefaults.cardColors(containerColor = CarbonSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, TargetGreen.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(TargetGreen.copy(alpha = 0.15f))
                    .border(1.dp, TargetGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = TargetGreen,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "PRO License Active",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TargetGreen
                )
                Text(
                    text = "100% Ad-free experience • Pro DSP scan engine enabled",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMediumEmphasis
                )
            }
        }
    }
}

/**
 * Pro Plan Checkout / Upgrade Dialog with Stripe Tier Selection (Monthly $2.99, Yearly $19.99, Lifetime $39.99)
 */
@Composable
fun ProUpgradeDialog(
    isProUnlocked: Boolean,
    selectedTier: ProPlanTier,
    monthlyBuyUrl: String,
    yearlyBuyUrl: String,
    lifetimeBuyUrl: String,
    onSelectTier: (ProPlanTier) -> Unit,
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit,
    onRestore: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = AmberPrimary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isProUnlocked) "PRO Active" else "SensorScan PRO",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextHighEmphasis
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isProUnlocked)
                        "You currently have the Pro Plan activated. All banner ads, interstitial full-screen ads, and sponsor cards are permanently removed."
                    else
                        "Select a plan to remove all banner and full-screen ads with professional precision DSP detection:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMediumEmphasis
                )

                if (!isProUnlocked) {
                    // Subscription Tier Selector Chips / Cards
                    ProPlanTier.entries.forEach { tier ->
                        val isSelected = selectedTier == tier
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectTier(tier) }
                                .testTag("pro_tier_${tier.id}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) AmberPrimary.copy(alpha = 0.15f) else CarbonSurfaceVariant
                            ),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                if (isSelected) 2.dp else 1.dp,
                                if (isSelected) AmberPrimary else CarbonBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) AmberPrimary else Color.Transparent)
                                        .border(
                                            2.dp,
                                            if (isSelected) AmberPrimary else TextMediumEmphasis,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF261900),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = tier.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextHighEmphasis
                                        )
                                        if (tier.isPopular) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(AmberPrimary.copy(alpha = 0.25f))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = "BEST VALUE",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp,
                                                    color = AmberPrimary
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${tier.priceDisplay} ${tier.billingPeriod}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMediumEmphasis
                                    )
                                }

                                Text(
                                    text = tier.priceDisplay,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = if (isSelected) AmberPrimary else TextHighEmphasis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    ProFeatureBullet(title = "Zero Advertisements", description = "No banner ads or 2-second interstitial ads.")
                    ProFeatureBullet(title = "Stripe Secured Checkout", description = "Instant activation and cross-device restoration.")
                    ProFeatureBullet(title = "Unrestricted Fast DSP", description = "Continuous sensor sampling for stud and conduit scans.")
                }
            }
        },
        confirmButton = {
            if (!isProUnlocked) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            val activeBuyUrl = when (selectedTier) {
                                ProPlanTier.MONTHLY -> monthlyBuyUrl
                                ProPlanTier.YEARLY -> yearlyBuyUrl
                                ProPlanTier.LIFETIME -> lifetimeBuyUrl
                            }
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(activeBuyUrl))
                                context.startActivity(intent)
                            } catch (_: Throwable) { }
                            onUpgrade()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberPrimary,
                            contentColor = Color(0xFF261900)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("confirm_pro_purchase_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CONTINUE WITH STRIPE (${selectedTier.priceDisplay})",
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            } else {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = TargetGreen, contentColor = Color(0xFF00220F))
                ) {
                    Text(text = "DONE", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isProUnlocked) {
                    TextButton(
                        onClick = onRestore,
                        modifier = Modifier.testTag("restore_purchases_button")
                    ) {
                        Text(text = "Restore Purchases", color = TextMediumEmphasis, fontSize = 12.sp)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(text = "Close", color = TextMediumEmphasis)
                }
            }
        },
        containerColor = CarbonSurface,
        iconContentColor = AmberPrimary,
        titleContentColor = TextHighEmphasis,
        textContentColor = TextMediumEmphasis
    )
}

/**
 * Fullscreen Interstitial Ad Dialog with 2-second lock before dismiss is enabled.
 */
@Composable
fun FullscreenInterstitialAdDialog(
    secondsRemaining: Int,
    canDismiss: Boolean,
    onDismiss: () -> Unit,
    onUpgradeClicked: () -> Unit
) {
    Dialog(
        onDismissRequest = {
            if (canDismiss) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = canDismiss,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("fullscreen_interstitial_ad_dialog"),
            color = CarbonBackground
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Sponsor Tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFE28743).copy(alpha = 0.2f))
                            .border(1.dp, Color(0xFFE28743), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "SPONSORED PROMOTION",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE28743)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Ad Visual Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 420.dp),
                        colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, CarbonBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(AmberPrimary.copy(alpha = 0.15f))
                                    .border(2.dp, AmberPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ElectricBolt,
                                    contentDescription = null,
                                    tint = AmberPrimary,
                                    modifier = Modifier.size(44.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Pro-Grade Laser Levels & Stud Sensors",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextHighEmphasis,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Get extreme accuracy and 360° self-leveling green beams for framing, plumbing, and electrical installations.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMediumEmphasis,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = onUpgradeClicked,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AmberPrimary,
                                    contentColor = Color(0xFF261900)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "REMOVE ALL ADS WITH PRO",
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Dismiss affordance with 2-second countdown timer
                    if (!canDismiss) {
                        Text(
                            text = "Ad will close in $secondsRemaining seconds...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMediumEmphasis
                        )
                    } else {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CarbonSurfaceVariant,
                                contentColor = TextHighEmphasis
                            ),
                            border = BorderStroke(1.dp, CarbonBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 420.dp)
                                .testTag("dismiss_interstitial_button")
                        ) {
                            Text(
                                text = "TAP TO CLOSE AD",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProFeatureBullet(
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = TargetGreen,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = TextHighEmphasis
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextMediumEmphasis
            )
        }
    }
}


