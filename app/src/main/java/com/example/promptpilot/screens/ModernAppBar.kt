// ModernAppBar.kt
// A modern, beautiful top app bar using Material 3, soft colors, and rounded corners.
// This file is a new UI layer and does NOT affect the old UI. It is designed to be used in your new scaffold.
// Every important line and function is commented for beginners.

package com.example.promptpilot.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.promptpilot.ui.theme.MainTheme

// The modern app bar composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernAppBar(
    onMenuClick: () -> Unit, // Callback for when the menu (drawer) button is clicked
    isTtsEnabled: Boolean = true, // Voice response state
    onTtsToggle: () -> Unit // Callback for voice toggle
) {
    MainTheme { // Use the app's theme for consistent look
        Surface(
            shadowElevation = 6.dp, // Drop shadow for depth
            tonalElevation = 0.dp,  // No tonal elevation
            color = Color.Transparent // Soft, modern background color
        ) {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Prompt Pilot", // App name or screen title
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White // Text color for contrast
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { onMenuClick() }, // Open the drawer when menu icon is clicked
                    ) {
                        Icon(
                            Icons.Filled.Menu, // Hamburger menu icon
                            contentDescription = "Menu",
                            tint = Color.Gray, // Icon color
                        )
                    }
                },
                actions = {
                    // Voice response toggle button
                    IconButton(
                        onClick = { onTtsToggle() }
                    ) {
                        Icon(
                            imageVector = if (isTtsEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = if (isTtsEnabled) "Disable voice responses" else "Enable voice responses",
                            tint = if (isTtsEnabled) Color.Green else Color.Gray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent, // App bar background
                    titleContentColor = Color.Transparent, // Title color
                ),
            )
        }
    }
}

