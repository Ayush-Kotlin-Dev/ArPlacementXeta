package com.xeta.arplacement.ui.screens

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.MotionEvent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.ar.core.*
import com.xeta.arplacement.data.Drill
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.ar.rememberARCameraStream
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.node.CylinderNode
import io.github.sceneview.node.SphereNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberNodes
import dev.romainguy.kotlin.math.Float3
import kotlinx.coroutines.delay
import kotlin.math.sqrt

data class PlacedDrill(
    val id: String,
    val anchorNode: AnchorNode,
    val position: Float3,
    val drill: Drill
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ARScreen(
    drill: Drill,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current

    var isPlaneDetected by remember { mutableStateOf(false) }
    var placedDrills by remember { mutableStateOf<List<PlacedDrill>>(emptyList()) }
    var currentFrame by remember { mutableStateOf<Frame?>(null) }
    var trackingState by remember { mutableStateOf<TrackingState?>(null) }
    var detectedPlanesCount by remember { mutableStateOf(0) }
    var sessionInitialized by remember { mutableStateOf(false) }
    var lightEstimateState by remember { mutableStateOf<LightEstimate?>(null) }
    var trackingFailureReason by remember { mutableStateOf<TrackingFailureReason?>(null) }
    var lastPlacementTime by remember { mutableStateOf(0L) }
    var placementError by remember { mutableStateOf<String?>(null) }
    var isCleaningUp by remember { mutableStateOf(false) }

    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    val cameraStream = rememberARCameraStream(materialLoader)
    val childNodes = rememberNodes()

    // Animation for pulse effect
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Vibration helper
    fun triggerHapticFeedback(type: HapticFeedbackType = HapticFeedbackType.LongPress) {
        hapticFeedback.performHapticFeedback(type)
    }

    // Validate drill placement distance
    fun validatePlacement(newPosition: Float3): String? {
        // For single drill mode, we'll clear existing drill automatically
        return null
    }

    // Clear all placements
    fun clearAllPlacements() {
        if (!isCleaningUp) {
            placedDrills.forEach { placedDrill ->
                try {
                    // Only remove from childNodes, let SceneView handle anchor cleanup
                    childNodes -= placedDrill.anchorNode
                } catch (e: Exception) {
                    // Ignore cleanup errors during clearing
                }
            }
            placedDrills = emptyList()
            triggerHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    LaunchedEffect(placementError) {
        if (placementError != null) {
            delay(3000)
            placementError = null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        ARScene(
            modifier = Modifier.fillMaxSize(),
            childNodes = childNodes,
            engine = engine,
            sessionConfiguration = { session, config ->
                config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                config.depthMode = when (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                    true -> Config.DepthMode.AUTOMATIC
                    else -> Config.DepthMode.DISABLED
                }
                config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                config.focusMode = Config.FocusMode.AUTO
                config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            },
            cameraStream = cameraStream,
            planeRenderer = true,
            onSessionCreated = { session ->
                sessionInitialized = true
                triggerHapticFeedback(HapticFeedbackType.TextHandleMove)
            },
            onSessionResumed = {
                sessionInitialized = true
            },
            onSessionPaused = {
                // Clean up resources when session is paused
                currentFrame = null
                trackingState = null
            },
            onSessionUpdated = { session, frame ->
                currentFrame = frame
                trackingState = frame.camera.trackingState
                lightEstimateState = frame.lightEstimate

                if (frame.camera.trackingState == TrackingState.TRACKING) {
                    val allPlanes = session.getAllTrackables(Plane::class.java)
                    val validPlanes = allPlanes.filter { plane ->
                        plane.trackingState == TrackingState.TRACKING &&
                                plane.subsumedBy == null &&
                                (plane.type == Plane.Type.HORIZONTAL_UPWARD_FACING ||
                                        plane.type == Plane.Type.HORIZONTAL_DOWNWARD_FACING) &&
                                plane.polygon.remaining() / 2 >= 3 // Ensure sufficient plane size
                    }

                    detectedPlanesCount = validPlanes.size

                    if (validPlanes.isNotEmpty() && !isPlaneDetected) {
                        isPlaneDetected = true
                        triggerHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
            },
            onTouchEvent = { motionEvent: MotionEvent, _ ->
                if (motionEvent.action == MotionEvent.ACTION_UP &&
                    isPlaneDetected &&
                    currentFrame != null &&
                    sessionInitialized &&
                    !isCleaningUp &&
                    System.currentTimeMillis() - lastPlacementTime > 500 // Prevent rapid tapping
                ) {
                    val frame = currentFrame!!
                    val hits = frame.hitTest(motionEvent.x, motionEvent.y)

                    val planeHit = hits.firstOrNull { hitResult ->
                        val trackable = hitResult.trackable
                        trackable is Plane &&
                                (trackable.type == Plane.Type.HORIZONTAL_UPWARD_FACING ||
                                        trackable.type == Plane.Type.HORIZONTAL_DOWNWARD_FACING) &&
                                trackable.trackingState == TrackingState.TRACKING &&
                                trackable.isPoseInPolygon(hitResult.hitPose)
                    }

                    if (planeHit != null) {
                        val hitPosition = Float3(
                            planeHit.hitPose.translation[0],
                            planeHit.hitPose.translation[1],
                            planeHit.hitPose.translation[2]
                        )

                        // Validate placement
                        val validationError = validatePlacement(hitPosition)
                        val placementSucceeded =
                            if (validationError != null) {
                                placementError = validationError
                                triggerHapticFeedback(HapticFeedbackType.LongPress)
                                false
                            } else {
                                if (placedDrills.isNotEmpty()) {
                                    clearAllPlacements()
                                }

                                val anchor = planeHit.createAnchor()

                                // Enhanced drill colors and materials
                                val drillColor = when (drill.id) {
                                    1 -> Color(0xFF1976D2) // Blue drill
                                    2 -> Color(0xFFD32F2F) // Red drill  
                                    3 -> Color(0xFF388E3C) // Green drill
                                    4 -> Color(0xFFFF6F00) // Orange drill
                                    5 -> Color(0xFF7B1FA2) // Purple drill
                                    else -> Color(0xFF424242) // Default gray
                                }

                                // Create realistic drill appearance
                                val drillMaterial = materialLoader.createColorInstance(
                                    color = drillColor,
                                    metallic = 0.8f,
                                    roughness = 0.3f,
                                    reflectance = 0.9f
                                )

                                val baseMaterial = materialLoader.createColorInstance(
                                    color = Color(0xFF37474F),
                                    metallic = 0.5f,
                                    roughness = 0.7f,
                                    reflectance = 0.4f
                                )

                                // Create drill assembly
                                val drillBit = CylinderNode(
                                    engine = engine,
                                    radius = 0.008f, // 8mm drill bit
                                    height = 0.15f,   // 15cm length
                                    materialInstance = drillMaterial
                                ).apply {
                                    position = Float3(0.0f, 0.075f, 0.0f)
                                }

                                val drillBase = CylinderNode(
                                    engine = engine,
                                    radius = 0.02f,  // 2cm base
                                    height = 0.04f,  // 4cm height
                                    materialInstance = baseMaterial
                                ).apply {
                                    position = Float3(0.0f, 0.02f, 0.0f)
                                }

                                // Add size indicator
                                val sizeIndicator = SphereNode(
                                    engine = engine,
                                    radius = 0.003f,
                                    materialInstance = materialLoader.createColorInstance(
                                        color = Color.White,
                                        metallic = 0.0f,
                                        roughness = 0.1f,
                                        reflectance = 1.0f
                                    )
                                ).apply {
                                    position = Float3(0.025f, 0.15f, 0.0f)
                                }

                                val anchorNode = AnchorNode(engine = engine, anchor = anchor)
                                anchorNode.addChildNode(drillBit)
                                anchorNode.addChildNode(drillBase)
                                anchorNode.addChildNode(sizeIndicator)

                                childNodes += anchorNode

                                val placedDrill = PlacedDrill(
                                    id = "${drill.id}_${System.currentTimeMillis()}",
                                    anchorNode = anchorNode,
                                    position = hitPosition,
                                    drill = drill
                                )

                                placedDrills = listOf(placedDrill)
                                lastPlacementTime = System.currentTimeMillis()
                                triggerHapticFeedback(HapticFeedbackType.LongPress)
                                true
                            }
                        placementSucceeded
                    } else {
                        false
                    }
                } else {
                    false
                }
            },
            onTrackingFailureChanged = { reason ->
                trackingFailureReason = reason
                if (reason != null) {
                    triggerHapticFeedback(HapticFeedbackType.LongPress)
                }
            }
        )

        // Enhanced Top Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Navigate back"
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = drill.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Single Drill Mode",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (placedDrills.isNotEmpty()) {
                    IconButton(
                        onClick = { clearAllPlacements() },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear all",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Error Message
        placementError?.let { error ->
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 100.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Enhanced Status Card
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    trackingFailureReason != null -> MaterialTheme.colorScheme.errorContainer.copy(
                        alpha = 0.95f
                    )

                    isPlaneDetected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when {
                    trackingFailureReason != null -> {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = when (trackingFailureReason) {
                                TrackingFailureReason.INSUFFICIENT_LIGHT -> "Need more light"
                                TrackingFailureReason.EXCESSIVE_MOTION -> "Move device slower"
                                TrackingFailureReason.INSUFFICIENT_FEATURES -> "Point at textured surfaces"
                                else -> "Tracking issue"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = when (trackingFailureReason) {
                                TrackingFailureReason.INSUFFICIENT_LIGHT -> "Move to better lighting or turn on more lights"
                                TrackingFailureReason.EXCESSIVE_MOTION -> "Hold device steadier and move more slowly"
                                TrackingFailureReason.INSUFFICIENT_FEATURES -> "Point camera at surfaces with patterns or textures"
                                else -> "Follow the guidance to resume tracking"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }

                    !sessionInitialized -> {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            text = "Initializing AR...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    trackingState != TrackingState.TRACKING -> {
                        Text(
                            text = "Getting ready...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Move device slowly and point at textured surfaces",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    !isPlaneDetected -> {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            text = "Scanning surfaces...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Point camera at flat surfaces like floors or tables",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    else -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Tap white grid to place ${drill.name} (replacing any existing drill)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Single placement mode",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                            if (detectedPlanesCount > 0) {
                                Text(
                                    text = "$detectedPlanesCount surface${if (detectedPlanesCount > 1) "s" else ""} detected",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }

                            // Light quality indicator
                            lightEstimateState?.let { lightEstimate ->
                                val lightQuality = when {
                                    lightEstimate.pixelIntensity < 0.3f -> "Low light"
                                    lightEstimate.pixelIntensity > 0.8f -> "Bright light"
                                    else -> "Good lighting"
                                }
                                Text(
                                    text = "Lighting: $lightQuality",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}