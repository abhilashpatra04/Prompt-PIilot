package com.example.promptpilot

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import com.example.promptpilot.screens.ModernScaffold
import com.example.promptpilot.ui.theme.MainTheme
import dagger.hilt.android.AndroidEntryPoint

// This annotation enables dependency injection with Hilt for this Activity.
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // ViewModel for managing UI-related data in a lifecycle-conscious way.
//    private val mainViewModel: MainViewModel by viewModels()

    // onCreate is called when the activity is starting.
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This tells Android not to fit system windows (status bar, etc.) automatically.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Set the content view to a ComposeView, which hosts our Compose UI.
        setContentView(
            ComposeView(this).apply {
                consumeWindowInsets = false // Don't consume window insets automatically.
                setContent {
                    // --- Modern UI: Use ModernScaffold for the new look ---
                    ModernScaffold()
                }
            }
        )
    }
}

// Preview function for Compose UI preview in Android Studio
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MainTheme {
        // You can add composables here to preview them
    }
}