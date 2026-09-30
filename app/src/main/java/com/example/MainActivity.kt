package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.CoolingAnimationDialog
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ProcessManagerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.FrostDarkBackground
import com.example.ui.theme.FrostDarkSurface
import com.example.ui.theme.FrostGuardTheme
import com.example.ui.theme.FrostPrimary
import com.example.ui.theme.FrostSecondary
import com.example.ui.theme.ThermalCritical
import com.example.ui.theme.ThermalHeated
import com.example.ui.theme.ThermalOptimal
import com.example.ui.theme.ThermalWarm
import com.example.ui.viewmodel.ThermalViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val openCoolingDirectly = intent?.getBooleanExtra("OPEN_COOLING_ACTION", false) ?: false

        setContent {
            FrostGuardTheme {
                val viewModel: ThermalViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                // Request POST_NOTIFICATIONS permission gracefully on Android 13+
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { /* Result handled */ }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

                    if (openCoolingDirectly) {
                        viewModel.startCoolingSequence(deepChill = true)
                    }
                }

                // Handle BackHandler for sub-tabs to navigate back to Dashboard
                if (uiState.selectedTab != 0) {
                    BackHandler {
                        viewModel.setSelectedTab(0)
                    }
                }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(FrostDarkBackground),
                    containerColor = FrostDarkBackground,
                    topBar = {
                        FrostGuardTopBar(
                            temperatureC = uiState.snapshot.temperatureC,
                            useFahrenheit = uiState.settings.useFahrenheit
                        )
                    },
                    bottomBar = {
                        FrostGuardBottomNav(
                            selectedTab = uiState.selectedTab,
                            onTabSelected = { viewModel.setSelectedTab(it) }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = uiState.selectedTab,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "tab_transition"
                        ) { tab ->
                            when (tab) {
                                0 -> DashboardScreen(
                                    uiState = uiState,
                                    onCoolDownClick = { viewModel.startCoolingSequence(deepChill = true) },
                                    onToggleStressSimulation = { enable ->
                                        viewModel.toggleStressSimulation(enable)
                                    },
                                    onNavigateToProcesses = { viewModel.setSelectedTab(1) }
                                )
                                1 -> ProcessManagerScreen(
                                    uiState = uiState,
                                    onToggleSelect = { pkg -> viewModel.toggleProcessSelection(pkg) },
                                    onSelectAll = { select -> viewModel.selectAllProcesses(select) },
                                    onKillSelected = { viewModel.killSelectedProcesses() },
                                    onRefresh = { viewModel.refreshProcesses() }
                                )
                                2 -> AnalyticsScreen(
                                    uiState = uiState,
                                    onClearHistory = { viewModel.clearHistory() }
                                )
                                3 -> SettingsScreen(
                                    settings = uiState.settings,
                                    onUpdateSettings = { newSettings -> viewModel.updateSettings(newSettings) }
                                )
                            }
                        }

                        // Cooling Animation and Result Dialog
                        if (uiState.coolingModalVisible) {
                            CoolingAnimationDialog(
                                isInProgress = uiState.isCoolingInProgress,
                                progress = uiState.coolingProgress,
                                stepText = uiState.coolingStepText,
                                result = uiState.coolingResult,
                                useFahrenheit = uiState.settings.useFahrenheit,
                                onDismiss = { viewModel.dismissCoolingModal() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FrostGuardTopBar(
    temperatureC: Float,
    useFahrenheit: Boolean
) {
    val displayTemp = if (useFahrenheit) (temperatureC * 9f / 5f) + 32f else temperatureC
    val unit = if (useFahrenheit) "°F" else "°C"

    val statusColor = when {
        temperatureC >= 43.0f -> ThermalCritical
        temperatureC >= 40.0f -> ThermalHeated
        temperatureC >= 37.0f -> ThermalWarm
        else -> ThermalOptimal
    }

    Surface(
        color = FrostDarkSurface,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(listOf(FrostPrimary, FrostSecondary))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AcUnit,
                        contentDescription = "App Logo",
                        tint = Color(0xFF001F29),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "FrostGuard",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Thermal Shield & Process Cooler",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // Real-time temperature badge
            Surface(
                color = statusColor.copy(alpha = 0.18f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = String.format("%.1f%s", displayTemp, unit),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = statusColor
                    )
                }
            }
        }
    }
}

@Composable
fun FrostGuardBottomNav(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = FrostDarkSurface,
        modifier = Modifier.testTag("frost_guard_bottom_nav")
    ) {
        val navItems = listOf(
            Triple(0, "Cooling", Pair(Icons.Default.AcUnit, Icons.Outlined.AcUnit)),
            Triple(1, "Tasks", Pair(Icons.Default.Memory, Icons.Outlined.Memory)),
            Triple(2, "Analytics", Pair(Icons.Default.ShowChart, Icons.Outlined.ShowChart)),
            Triple(3, "Shield", Pair(Icons.Default.Security, Icons.Outlined.Security))
        )

        navItems.forEach { (index, title, icons) ->
            val isSelected = selectedTab == index
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(index) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) icons.first else icons.second,
                        contentDescription = title,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF001F29),
                    selectedTextColor = FrostPrimary,
                    indicatorColor = FrostPrimary,
                    unselectedIconColor = Color(0xFF94A3B8),
                    unselectedTextColor = Color(0xFF94A3B8)
                ),
                modifier = Modifier.testTag("nav_tab_$index")
            )
        }
    }
}
