package com.xeta.arplacement.ui.screens

import android.view.MotionEvent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
import io.github.sceneview.material.setBaseColorFactor
import io.github.sceneview.node.CylinderNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberNodes
import dev.romainguy.kotlin.math.Float3

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

    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    val cameraStream = rememberARCameraStream(materialLoader)
    val childNodes = rememberNodes()

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
            },
            cameraStream = cameraStream,
            planeRenderer = true,
            onSessionUpdated = { session, frame ->
                currentFrame = frame
                if (frame.camera.trackingState == TrackingState.TRACKING) {
                    val updatedPlanes = frame.getUpdatedTrackables(Plane::class.java)
                    if (updatedPlanes.isNotEmpty() && !isPlaneDetected) {
                        isPlaneDetected = true
                    }
                }
            },
            onTouchEvent = { motionEvent: MotionEvent, _ ->
                if (motionEvent.action == MotionEvent.ACTION_UP && isPlaneDetected && currentFrame != null) {
                    // Perform hit test against the frame
                    val hits = currentFrame!!.hitTest(motionEvent.x, motionEvent.y)
                    val planeHit = hits.firstOrNull { hitResult ->
                        val trackable = hitResult.trackable
                        trackable is Plane && trackable.isPoseInPolygon(hitResult.hitPose)
                    }

                    planeHit?.let { hitResult ->
                        // Remove previous anchor if exists
                        placedAnchorNode?.let { oldNode ->
                            childNodes -= oldNode
                            oldNode.destroy()
                        }

                        // Create anchor from hit result
                        val anchor = hitResult.createAnchor()

                        // Create drill color based on drill type
                        val drillColor = when (drill.id) {
                            1 -> Color.Blue
                            2 -> Color.Red
                            3 -> Color.Green
                            else -> Color.Gray
                        }

                        // Create material with color
                        val material = materialLoader.createColorInstance(
                            color = drillColor,
                            metallic = 0.0f,
                            roughness = 0.8f,
                            reflectance = 0.1f
                        )

                        // Create cube geometry node to represent the drill
                        val cubeNode = CylinderNode(
                            engine = engine,
                            radius = 0.05f,
                            height = 0.1f,
                            materialInstance = material
                        ).apply {
                            position = Float3(0.0f, 0.05f, 0.0f)
                        }

                        // Create anchor node and add cube
                        val anchorNode = AnchorNode(engine = engine, anchor = anchor)
                        anchorNode.addChildNode(cubeNode)

                        // Add to scene
                        childNodes += anchorNode
                        placedAnchorNode = anchorNode
                        isObjectPlaced = true
                    }

                    true // Consume the touch event
                } else {
                    false // Don't consume the touch event
                }
            }
        )

        // Top UI overlay
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
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
                        contentDescription = "Back"
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
            }
        }

        // Bottom instruction overlay
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
                    !isPlaneDetected -> {
                        Text(
                            text = "🔍 Scanning for surfaces...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Point your camera at a flat surface like the floor or table",
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
                            text = "✋ Tap to place ${drill.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Tap on the detected surface (white dots) to place your drill marker",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    isObjectPlaced -> {
                        Text(
                            text = "✅ Drill placed successfully!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Tap elsewhere to move the drill marker",
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