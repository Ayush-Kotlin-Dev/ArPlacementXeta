package com.xeta.arplacement.ui.screens

import android.view.MotionEvent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberNodes
import dev.romainguy.kotlin.math.Float3
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ARScreen(
    drill: Drill,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlaneDetected by remember { mutableStateOf(false) }
    var isObjectPlaced by remember { mutableStateOf(false) }
    var placedAnchorNode by remember { mutableStateOf<AnchorNode?>(null) }
    var currentFrame by remember { mutableStateOf<Frame?>(null) }
    var trackingState by remember { mutableStateOf<TrackingState?>(null) }
    var detectedPlanesCount by remember { mutableStateOf(0) }
    var sessionInitialized by remember { mutableStateOf(false) }

    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    val cameraStream = rememberARCameraStream(materialLoader)
    val childNodes = rememberNodes()

    LaunchedEffect(isObjectPlaced) {
        if (isObjectPlaced) {
            delay(3000)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        ARScene(
            modifier = Modifier.fillMaxSize(),
            childNodes = childNodes,
            engine = engine,
            sessionConfiguration = { session, config ->
                config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                config.depthMode = when (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                    true -> Config.DepthMode.AUTOMATIC
                    else -> Config.DepthMode.DISABLED
                }
                config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                config.focusMode = Config.FocusMode.AUTO
            },
            cameraStream = cameraStream,
            planeRenderer = true,
            onSessionCreated = { _ ->
                sessionInitialized = true
            },
            onSessionUpdated = { session, frame ->
                currentFrame = frame
                trackingState = frame.camera.trackingState

                if (frame.camera.trackingState == TrackingState.TRACKING) {
                    val allPlanes = session.getAllTrackables(Plane::class.java)
                    val validPlanes = allPlanes.filter { plane ->
                        plane.trackingState == TrackingState.TRACKING &&
                                plane.subsumedBy == null &&
                                plane.type == Plane.Type.HORIZONTAL_UPWARD_FACING
                    }

                    detectedPlanesCount = validPlanes.size

                    if (validPlanes.isNotEmpty() && !isPlaneDetected) {
                        isPlaneDetected = true
                    }
                }
            },
            onTouchEvent = { motionEvent: MotionEvent, _ ->
                if (motionEvent.action == MotionEvent.ACTION_UP &&
                    isPlaneDetected &&
                    currentFrame != null &&
                    sessionInitialized
                ) {

                    val frame = currentFrame!!

                    val hits = frame.hitTest(motionEvent.x, motionEvent.y)

                    val planeHit = hits.firstOrNull { hitResult ->
                        val trackable = hitResult.trackable
                        trackable is Plane &&
                                trackable.type == Plane.Type.HORIZONTAL_UPWARD_FACING &&
                                trackable.trackingState == TrackingState.TRACKING &&
                                trackable.isPoseInPolygon(hitResult.hitPose)
                    }

                    planeHit?.let { hitResult ->
                        placedAnchorNode?.let { oldNode ->
                            childNodes -= oldNode
                            oldNode.anchor?.detach()
                            oldNode.destroy()
                        }

                        try {
                            val anchor = hitResult.createAnchor()

                            val drillColor = when (drill.id) {
                                1 -> Color(0xFF2196F3)
                                2 -> Color(0xFFF44336)
                                3 -> Color(0xFF4CAF50)
                                else -> Color(0xFF9E9E9E)
                            }

                            val material = materialLoader.createColorInstance(
                                color = drillColor,
                                metallic = 0.0f,
                                roughness = 0.6f,
                                reflectance = 0.2f
                            )

                            val drillNode = CylinderNode(
                                engine = engine,
                                radius = 0.03f,
                                height = 0.12f,
                                materialInstance = material
                            ).apply {
                                position = Float3(0.0f, 0.06f, 0.0f)
                            }

                            val anchorNode = AnchorNode(engine = engine, anchor = anchor)
                            anchorNode.addChildNode(drillNode)

                            childNodes += anchorNode
                            placedAnchorNode = anchorNode
                            isObjectPlaced = true

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    true
                } else {
                    false
                }
            },
            onTrackingFailureChanged = { trackingFailureReason ->
                when (trackingFailureReason) {
                    TrackingFailureReason.INSUFFICIENT_LIGHT -> {
                    }

                    TrackingFailureReason.EXCESSIVE_MOTION -> {
                    }

                    TrackingFailureReason.INSUFFICIENT_FEATURES -> {
                    }

                    else -> {
                    }
                }
            }
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
                        text = "AR Placement Mode",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isObjectPlaced) {
                    IconButton(
                        onClick = {
                            placedAnchorNode?.let { node ->
                                childNodes -= node
                                node.anchor?.detach()
                                node.destroy()
                            }
                            placedAnchorNode = null
                            isObjectPlaced = false
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Clear placement",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
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
                    !sessionInitialized -> {
                        Text(
                            text = "Initializing AR session...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    }

                    trackingState != TrackingState.TRACKING -> {
                        Text(
                            text = "Move your device slowly",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Point camera at well-lit textured surfaces to track environment",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    !isPlaneDetected -> {
                        Text(
                            text = "Scanning for surfaces...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Point your camera at a flat horizontal surface like the floor or table",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    }

                    isPlaneDetected && !isObjectPlaced -> {
                        Text(
                            text = "Tap to place ${drill.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Tap on detected surface (white grid) to place your drill marker",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (detectedPlanesCount > 0) {
                            Text(
                                text = "$detectedPlanesCount surface${if (detectedPlanesCount > 1) "s" else ""} detected",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }

                    isObjectPlaced -> {
                        Text(
                            text = "${drill.name} placed successfully!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Tap elsewhere to move • Use refresh button to clear",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}