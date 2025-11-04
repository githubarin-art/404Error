package com.runanywhere.startup_hackathon20

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.runanywhere.startup_hackathon20.ui.screens.EmergencyScreen
import com.runanywhere.startup_hackathon20.ui.screens.OnboardingScreen
import com.runanywhere.startup_hackathon20.ui.theme.Startup_hackathon20Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Startup_hackathon20Theme {
                GuardianApp()
            }
        }
    }
}

@Composable
fun GuardianApp() {
    val context = LocalContext.current
    val viewModel: SafetyViewModel = viewModel(
        factory = SafetyViewModelFactory(context)
    )

    // Check if onboarding is complete
    val sharedPrefs = context.getSharedPreferences("guardian_prefs", Context.MODE_PRIVATE)

    // IMPORTANT: For testing, set this to false to see onboarding
    // For production, remove this line
    sharedPrefs.edit().putBoolean("onboarding_complete", false).apply()

    var isOnboardingComplete by remember {
        mutableStateOf(sharedPrefs.getBoolean("onboarding_complete", false))
    }

    if (!isOnboardingComplete) {
        // Show onboarding
        OnboardingScreen(
            viewModel = viewModel,
            onComplete = {
                // Mark onboarding as complete
                sharedPrefs.edit().putBoolean("onboarding_complete", true).apply()
                isOnboardingComplete = true
            }
        )
    } else {
        // Show main app
        SafetyApp(viewModel)
    }
}

@Composable
fun SafetyApp(viewModel: SafetyViewModel) {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Contacts") },
                    label = { Text("Contacts") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> EmergencyScreen(viewModel, Modifier.padding(padding))
            1 -> ContactsPlaceholder(Modifier.padding(padding))
            2 -> SettingsPlaceholder(Modifier.padding(padding))
        }
    }
}

@Composable
fun ContactsPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Contacts Management - Coming Soon", style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
fun SettingsPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Settings - Coming Soon", style = MaterialTheme.typography.headlineSmall)
    }
}

// ViewModel Factory to pass Context
class SafetyViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SafetyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SafetyViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}