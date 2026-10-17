package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.compatibility.DiagnosticReport
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun DiagnosticBanner(
    report: DiagnosticReport,
    onRetry: () -> Unit,
    onAllowSite: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
            .testTag("diagnostic_banner")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with Category Badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (report.category.isWarning) Icons.Default.Warning else Icons.Default.Info,
                    contentDescription = null,
                    tint = if (report.category.isWarning) WarningAmber else ElectricCyan,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = report.category.displayName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (report.category.isWarning) WarningAmber else ElectricCyan
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = report.domain,
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Summary description
            Text(
                text = report.summary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Suggestion
            Text(
                text = report.suggestion,
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Retry", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Retry", fontSize = 12.sp)
                }

                if (report.canAllowSite) {
                    OutlinedButton(
                        onClick = onAllowSite,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = "Allow Site", modifier = Modifier.size(14.dp), tint = WarningAmber)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Allow Site", fontSize = 12.sp, color = WarningAmber)
                    }
                }

                TextButton(onClick = onDismiss) {
                    Text("Dismiss", fontSize = 12.sp, color = TextMuted)
                }
            }
        }
    }
}
