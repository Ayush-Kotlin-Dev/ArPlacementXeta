package com.xeta.arplacement

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.xeta.arplacement.data.Drill
import com.xeta.arplacement.data.DrillRepository
import com.xeta.arplacement.ui.screens.ARScreen
import com.xeta.arplacement.ui.screens.DrillDetailScreen
import com.xeta.arplacement.ui.screens.DrillSelectionScreen
import com.xeta.arplacement.ui.theme.ArPlacementXetaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ArPlacementXetaTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ArPlacementApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun ArPlacementApp(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = "drill_selection",
        modifier = modifier
    ) {
        composable("drill_selection") {
            DrillSelectionScreen(
                onNavigateToDetail = { drill ->
                    navController.navigate("drill_detail/${drill.id}")
                }
            )
        }

        composable("drill_detail/{drillId}") { backStackEntry ->
            val drillId = backStackEntry.arguments?.getString("drillId")?.toIntOrNull()
            val drill = DrillRepository.drills.find { it.id == drillId }

            drill?.let {
                DrillDetailScreen(
                    drill = it,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onStartAR = { selectedDrill ->
                        navController.navigate("ar_screen/${selectedDrill.id}")
                    }
                )
            }
        }

        composable("ar_screen/{drillId}") { backStackEntry ->
            val drillId = backStackEntry.arguments?.getString("drillId")?.toIntOrNull()
            val drill = DrillRepository.drills.find { it.id == drillId }

            drill?.let {
                ARScreen(
                    drill = it,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}