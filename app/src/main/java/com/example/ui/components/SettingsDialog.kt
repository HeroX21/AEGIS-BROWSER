package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.blocking.FilterUpdateStatus
import com.example.data.BrowserPreferences
import com.example.data.SettingsRepository
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.CriticalRed
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.launch

@Composable
fun SettingsDialog(
    preferences: BrowserPreferences,
    settingsRepository: SettingsRepository,
    filterStatus: FilterUpdateStatus,
    onTriggerFilterUpdate: () -> Unit,
    onClearAllData: () -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var showDataClearConfirmation by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .testTag("settings_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(scrollState)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Browser Settings",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section: Privacy & Protection
                Text(
                    text = "PRIVACY & SECURITY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricCyan
                )

                Spacer(modifier = Modifier.height(10.dp))

                SettingSwitchRow(
                    title = "Built-in Ad Blocking",
                    subtitle = "Filter advertising networks and cosmetic elements",
                    checked = preferences.adBlockingEnabled,
                    onCheckedChange = { scope.launch { settingsRepository.setAdBlockingEnabled(it) } }
                )

                SettingSwitchRow(
                    title = "Tracking Protection",
                    subtitle = "Prevent cross-site trackers and telemetry beacons",
                    checked = preferences.trackingProtectionEnabled,
                    onCheckedChange = { scope.launch { settingsRepository.setTrackingProtectionEnabled(it) } }
                )

                SettingSwitchRow(
                    title = "Popup & Malicious Redirect Guard",
                    subtitle = "Suppress unwanted popups and rapid redirect loops",
                    checked = preferences.popupProtectionEnabled,
                    onCheckedChange = { scope.launch { settingsRepository.setPopupProtectionEnabled(it) } }
                )

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = DarkSurfaceBorder)
                Spacer(modifier = Modifier.height(16.dp))

                // Section: Search Provider
                Text(
                    text = "DEFAULT SEARCH ENGINE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricCyan
                )

                Spacer(modifier = Modifier.height(10.dp))

                listOf("Google", "DuckDuckGo", "Brave", "Startpage").forEach { engine ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = preferences.searchEngineName == engine,
                            onClick = { scope.launch { settingsRepository.setSearchEngine(engine) } },
                            colors = RadioButtonDefaults.colors(selectedColor = ElectricCyan)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = engine,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = DarkSurfaceBorder)
                Spacer(modifier = Modifier.height(16.dp))

                // Section: Filter Management
                Text(
                    text = "FILTER LISTS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricCyan
                )

                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "AdGuard Base & Tracking Protection",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${filterStatus.ruleCount} active rules • Ver: ${filterStatus.version}",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        if (filterStatus.lastError != null) {
                            Text(
                                text = filterStatus.lastError,
                                fontSize = 11.sp,
                                color = WarningAmber,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = onTriggerFilterUpdate,
                            enabled = !filterStatus.isUpdating,
                            colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (filterStatus.isUpdating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Check for Filter Updates", fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = DarkSurfaceBorder)
                Spacer(modifier = Modifier.height(16.dp))

                // Section: Clear Browsing Data
                Text(
                    text = "DATA MANAGEMENT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricCyan
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        onClearAllData()
                        showDataClearConfirmation = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CriticalRed.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = CriticalRed, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear All Browsing Data & Tabs", color = CriticalRed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                if (showDataClearConfirmation) {
                    Text(
                        text = "History, temporary data, and private tabs cleared.",
                        color = ElectricCyan,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // About footer
                Text(
                    text = "Aegis Browser v1.0 • Powered by Mozilla GeckoView",
                    fontSize = 11.sp,
                    color = TextMuted
                )
                Text(
                    text = "Zero cloud telemetry. Browsing data remains strictly on your device.",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Text(text = subtitle, fontSize = 11.sp, color = TextMuted)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ElectricCyan,
                checkedTrackColor = CobaltBlue.copy(alpha = 0.5f),
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = DarkSurfaceBorder
            )
        )
    }
}
