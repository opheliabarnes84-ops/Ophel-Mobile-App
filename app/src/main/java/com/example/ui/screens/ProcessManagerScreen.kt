package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProcessCategory
import com.example.data.model.ProcessInfo
import com.example.ui.theme.FrostDarkSurface
import com.example.ui.theme.FrostDarkSurfaceVariant
import com.example.ui.theme.FrostPrimary
import com.example.ui.theme.FrostSecondary
import com.example.ui.theme.FrostTertiary
import com.example.ui.theme.ThermalHeated
import com.example.ui.theme.ThermalOptimal
import com.example.ui.viewmodel.ThermalUiState

@Composable
fun ProcessManagerScreen(
    uiState: ThermalUiState,
    onToggleSelect: (String) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onKillSelected: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snapshot = uiState.snapshot
    val processes = uiState.processList
    var selectedCategory by remember { mutableStateOf<ProcessCategory?>(null) }

    val filteredProcesses = remember(processes, selectedCategory) {
        if (selectedCategory == null) processes else processes.filter { it.category == selectedCategory }
    }

    val selectedCount = processes.count { it.isSelected && !it.isProtected }
    val totalRamMb = snapshot.totalRamBytes / (1024 * 1024)
    val usedRamMb = snapshot.usedRamBytes / (1024 * 1024)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
            .testTag("process_manager_screen")
    ) {
        // RAM Memory Banner
        Card(
            colors = CardDefaults.cardColors(containerColor = FrostDarkSurfaceVariant),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .border(1.dp, Color(0x3338BDF8), RoundedCornerShape(18.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = "RAM Icon",
                            tint = FrostSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "RAM Memory Allocation",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "${snapshot.usedRamPercent}% In Use",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (snapshot.usedRamPercent > 75) ThermalHeated else FrostSecondary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { snapshot.usedRamPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (snapshot.usedRamPercent > 75) ThermalHeated else FrostSecondary,
                    trackColor = Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Used: ${usedRamMb} MB",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "Total: ${totalRamMb} MB",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        // Action Row: Select All & Kill Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Processes (${processes.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh processes",
                        tint = FrostPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = Color(0x2238BDF8),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable {
                        onSelectAll(selectedCount == 0)
                    }
                ) {
                    Text(
                        text = if (selectedCount == 0) "Select All" else "Deselect",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FrostSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Button(
                    onClick = onKillSelected,
                    enabled = selectedCount > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FrostPrimary,
                        contentColor = Color(0xFF001F29),
                        disabledContainerColor = Color(0xFF1E293B),
                        disabledContentColor = Color(0xFF64748B)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("kill_selected_processes_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AcUnit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Freeze ($selectedCount)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Category filter chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { selectedCategory = null },
                label = { Text("All", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = FrostPrimary,
                    selectedLabelColor = Color(0xFF001F29)
                )
            )
            FilterChip(
                selected = selectedCategory == ProcessCategory.MEDIA,
                onClick = { selectedCategory = ProcessCategory.MEDIA },
                label = { Text("Media", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = FrostPrimary,
                    selectedLabelColor = Color(0xFF001F29)
                )
            )
            FilterChip(
                selected = selectedCategory == ProcessCategory.SOCIAL,
                onClick = { selectedCategory = ProcessCategory.SOCIAL },
                label = { Text("Social", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = FrostPrimary,
                    selectedLabelColor = Color(0xFF001F29)
                )
            )
            FilterChip(
                selected = selectedCategory == ProcessCategory.UTILITY,
                onClick = { selectedCategory = ProcessCategory.UTILITY },
                label = { Text("Utilities", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = FrostPrimary,
                    selectedLabelColor = Color(0xFF001F29)
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Process List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredProcesses, key = { it.packageName }) { proc ->
                ProcessItemCard(
                    process = proc,
                    onToggle = { onToggleSelect(proc.packageName) }
                )
            }
            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
fun ProcessItemCard(
    process: ProcessInfo,
    onToggle: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (process.isKilled) Color(0xFF0D1724) else FrostDarkSurfaceVariant
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (process.isSelected && !process.isKilled) FrostPrimary.copy(alpha = 0.5f) else Color(0x22334155),
                RoundedCornerShape(14.dp)
            )
            .clickable(enabled = !process.isProtected && !process.isKilled) { onToggle() }
            .testTag("process_item_${process.packageName}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (process.isKilled) Color(0xFF064E3B) else Color(0xFF1E293B)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (process.isKilled) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Frozen",
                            tint = ThermalOptimal,
                            modifier = Modifier.size(20.dp)
                        )
                    } else if (process.isProtected) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Protected",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = "App Icon",
                            tint = FrostSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = process.appName,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = if (process.isKilled) Color(0xFF64748B) else Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (process.isKilled) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = ThermalOptimal.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "FROZEN",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ThermalOptimal,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${process.memoryUsageMb} MB • ${process.importance}",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            if (process.isProtected) {
                Surface(
                    color = Color(0x2264748B),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "System",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else if (!process.isKilled) {
                Checkbox(
                    checked = process.isSelected,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = FrostPrimary,
                        uncheckedColor = Color(0xFF64748B),
                        checkmarkColor = Color(0xFF001F29)
                    )
                )
            }
        }
    }
}
