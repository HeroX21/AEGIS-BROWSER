package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.PrivatePurple
import com.example.ui.theme.ShieldGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

/**
 * Top bar component for Aegis Browser featuring a unified address & search field,
 * privacy shield status, reload/stop controls, and navigation overflow actions.
 */
@Composable
fun BrowserTopBar(
    currentUrl: String,
    isLoading: Boolean,
    progress: Int,
    isSecure: Boolean,
    isPrivate: Boolean,
    isSiteWhitelisted: Boolean,
    blockedCount: Int,
    isBookmarked: Boolean,
    onNavigate: (String) -> Unit,
    onReload: () -> Unit,
    onStop: () -> Unit,
    onShieldClick: () -> Unit,
    onNewTab: (Boolean) -> Unit,
    onToggleBookmark: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var addressInput by remember { mutableStateOf(currentUrl) }
    var isEditing by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(currentUrl) {
        if (!isEditing) {
            addressInput = currentUrl
        }
    }

    Surface(
        color = DarkSurface,
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Privacy Shield Trigger with Badge
                IconButton(
                    onClick = onShieldClick,
                    modifier = Modifier.testTag("browser_top_bar_shield")
                ) {
                    BadgedBox(
                        badge = {
                            if (blockedCount > 0) {
                                Badge(
                                    containerColor = ElectricCyan,
                                    contentColor = Color(0xFF041824)
                                ) {
                                    Text(
                                        text = if (blockedCount > 99) "99+" else blockedCount.toString(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isSiteWhitelisted) Icons.Default.Shield else Icons.Default.Security,
                            contentDescription = "Privacy Shield",
                            tint = if (isSiteWhitelisted) WarningAmber else ElectricCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Address & Search Input Field
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(DarkSurfaceElevated)
                        .border(
                            width = 1.dp,
                            color = if (isPrivate) PrivatePurple else DarkSurfaceBorder,
                            shape = RoundedCornerShape(22.dp)
                        )
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSecure) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Secure Connection",
                                tint = ShieldGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        TextField(
                            value = addressInput,
                            onValueChange = {
                                addressInput = it
                                isEditing = true
                            },
                            placeholder = {
                                Text(
                                    text = if (isPrivate) "Search or enter private URL" else "Search or type URL",
                                    fontSize = 13.sp,
                                    color = TextMuted
                                )
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = ElectricCyan
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Go
                            ),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    onNavigate(addressInput)
                                    isEditing = false
                                    focusManager.clearFocus()
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                                .testTag("top_bar_url_input")
                        )

                        if (isEditing && addressInput.isNotEmpty()) {
                            IconButton(
                                onClick = { addressInput = "" },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear input",
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Reload / Stop Action Button
                if (isLoading) {
                    IconButton(onClick = onStop) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = WarningAmber,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                } else {
                    IconButton(onClick = onReload) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reload",
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Overflow Menu
                Box {
                    IconButton(
                        onClick = { showOverflowMenu = true },
                        modifier = Modifier.testTag("top_bar_overflow_menu")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = TextSecondary
                        )
                    }

                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false },
                        modifier = Modifier
                            .background(DarkSurfaceElevated)
                            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(8.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("New Tab", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = ElectricCyan) },
                            onClick = {
                                onNewTab(false)
                                showOverflowMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("New Private Tab", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PrivatePurple) },
                            onClick = {
                                onNewTab(true)
                                showOverflowMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (isBookmarked) "Remove Bookmark" else "Bookmark Page",
                                    color = TextPrimary
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = null,
                                    tint = ElectricCyan
                                )
                            },
                            onClick = {
                                onToggleBookmark()
                                showOverflowMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Bookmarks & History", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.History, contentDescription = null, tint = CobaltBlue) },
                            onClick = {
                                onOpenLibrary()
                                showOverflowMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Settings", color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Security, contentDescription = null, tint = TextSecondary) },
                            onClick = {
                                onOpenSettings()
                                showOverflowMenu = false
                            }
                        )
                    }
                }
            }

            // Animated Loading Progress Indicator
            if (isLoading) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp),
                    color = if (isPrivate) PrivatePurple else ElectricCyan,
                    trackColor = Color.Transparent
                )
            }
        }
    }
}
