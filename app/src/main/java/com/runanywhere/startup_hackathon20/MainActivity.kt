package com.runanywhere.startup_hackathon20

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.runanywhere.startup_hackathon20.ui.screens.EmergencyScreen
import com.runanywhere.startup_hackathon20.ui.screens.OnboardingScreen
import com.runanywhere.startup_hackathon20.ui.screens.ContactsScreen
import com.runanywhere.startup_hackathon20.ui.screens.SettingsScreen
import com.runanywhere.startup_hackathon20.ui.screens.ThreatAnalysisScreen
import com.runanywhere.startup_hackathon20.ui.theme.Startup_hackathon20Theme
import com.runanywhere.startup_hackathon20.ui.theme.*
import android.util.Log

class MainActivity : ComponentActivity() {
    private lateinit var safetyViewModel: SafetyViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Startup_hackathon20Theme {
                GuardianApp { viewModel ->
                    // Store ViewModel reference for hardware button handling
                    safetyViewModel = viewModel
                }
            }
        }
    }
    
    /**
     * Handle hardware button presses (Volume buttons for answering safety questions)
     * Volume Up = Yes
     * Volume Down = No
     * 
     * This allows users to answer questions discreetly without looking at screen
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Only handle volume buttons when a safety question is active
        if (::safetyViewModel.isInitialized) {
            val currentQuestion = safetyViewModel.currentQuestion.value
            
            if (currentQuestion != null) {
                when (keyCode) {
                    KeyEvent.KEYCODE_VOLUME_UP -> {
                        // Volume Up = YES
                        Log.i("MainActivity", " Volume UP pressed - Answering YES to safety question")
                        safetyViewModel.answerProtocolQuestionYes()
                        return true // Consume the event
                    }
                    KeyEvent.KEYCODE_VOLUME_DOWN -> {
                        // Volume Down = NO
                        Log.i("MainActivity", " Volume DOWN pressed - Answering NO to safety question")
                        safetyViewModel.answerProtocolQuestionNo()
                        return true // Consume the event
                    }
                }
            }
        }
        
        return super.onKeyDown(keyCode, event)
    }
}

@Composable
fun GuardianApp(onViewModelCreated: (SafetyViewModel) -> Unit = {}) {
    val context = LocalContext.current
    val viewModel: SafetyViewModel = viewModel(
        factory = SafetyViewModelFactory(context)
    )
    
    // Notify MainActivity about ViewModel creation
    LaunchedEffect(viewModel) {
        onViewModelCreated(viewModel)
    }

    // Check if onboarding is complete
    val sharedPrefs = context.getSharedPreferences("guardian_prefs", Context.MODE_PRIVATE)

    var isOnboardingComplete by remember {
        mutableStateOf(sharedPrefs.getBoolean("onboarding_complete", false))
    }

    // Smooth transition between onboarding and main app
    AnimatedContent(
        targetState = isOnboardingComplete,
        transitionSpec = {
            fadeIn(
                animationSpec = tween(600, easing = EaseInOut)
            ) togetherWith fadeOut(
                animationSpec = tween(600, easing = EaseInOut)
            )
        },
        label = "app_transition"
    ) { onboardingCompleteState ->
        if (!onboardingCompleteState) {
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
}

@Composable
fun SafetyApp(viewModel: SafetyViewModel) {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        containerColor = ModernWhite,
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                NavigationBar(
                    containerColor = Color.White,
                    contentColor = ModernTextPrimary,
                    modifier = Modifier.height(72.dp)
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = {
                            Icon(
                                Icons.Default.Home,
                                contentDescription = "Home",
                                modifier = Modifier.size(26.dp)
                            )
                        },
                        label = {
                            Text(
                                "HOME",
                                fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SafetyRed,
                            selectedTextColor = SafetyRed,
                            unselectedIconColor = ModernTextTertiary,
                            unselectedTextColor = ModernTextTertiary,
                            indicatorColor = SafetyRed.copy(alpha = 0.1f)
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Contacts",
                                modifier = Modifier.size(26.dp)
                            )
                        },
                        label = {
                            Text(
                                "CONTACTS",
                                fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SoftTeal,
                            selectedTextColor = SoftTeal,
                            unselectedIconColor = ModernTextTertiary,
                            unselectedTextColor = ModernTextTertiary,
                            indicatorColor = SoftTeal.copy(alpha = 0.1f)
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Threat Analysis",
                                modifier = Modifier.size(26.dp)
                            )
                        },
                        label = {
                            Text(
                                "THREAT",
                                fontWeight = if (selectedTab == 2) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AmberYellowDark,
                            selectedTextColor = AmberYellowDark,
                            unselectedIconColor = ModernTextTertiary,
                            unselectedTextColor = ModernTextTertiary,
                            indicatorColor = AmberYellow.copy(alpha = 0.1f)
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                modifier = Modifier.size(26.dp)
                            )
                        },
                        label = {
                            Text(
                                "SETTINGS",
                                fontWeight = if (selectedTab == 3) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ModernTextSecondary,
                            selectedTextColor = ModernTextSecondary,
                            unselectedIconColor = ModernTextTertiary,
                            unselectedTextColor = ModernTextTertiary,
                            indicatorColor = ModernGrayMedium
                        )
                    )
                }
            }
        }
    ) { padding ->
        // Smooth animated transitions between screens
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                // Slide and fade animation
                (slideInHorizontally(
                    initialOffsetX = { fullWidth -> if (targetState > initialState) fullWidth else -fullWidth },
                    animationSpec = tween(400, easing = EaseInOutCubic)
                ) + fadeIn(
                    animationSpec = tween(400)
                )) togetherWith (slideOutHorizontally(
                    targetOffsetX = { fullWidth -> if (targetState > initialState) -fullWidth else fullWidth },
                    animationSpec = tween(400, easing = EaseInOutCubic)
                ) + fadeOut(
                    animationSpec = tween(400)
                ))
            },
            label = "screen_transition"
        ) { targetTab ->
            when (targetTab) {
                0 -> EmergencyScreen(viewModel, Modifier.padding(padding))
                1 -> ContactsScreen(viewModel, Modifier.padding(padding))
                2 -> ThreatAnalysisScreen(viewModel, Modifier.padding(padding))
                3 -> SettingsScreen(viewModel, Modifier.padding(padding))
            }
        }
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