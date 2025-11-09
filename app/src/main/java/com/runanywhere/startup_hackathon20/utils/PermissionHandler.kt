package com.runanywhere.startup_hackathon20.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Comprehensive Permission Handler
 * 
 * Handles all permission requests with:
 * - Fallback suggestions when denied
 * - User-friendly explanations
 * - Settings navigation for permanent denials
 * - Alternative suggestions
 */
object PermissionHandler {
    private const val TAG = "PermissionHandler"

    /**
     * Permission result with status and fallback suggestions
     */
    data class PermissionResult(
        val granted: Boolean,
        val permanentlyDenied: Boolean = false,
        val fallbackSuggestions: List<FallbackSuggestion> = emptyList()
    )

    /**
     * Fallback suggestion when permission is denied
     */
    data class FallbackSuggestion(
        val title: String,
        val description: String,
        val action: FallbackAction,
        val priority: Priority = Priority.MEDIUM
    ) {
        enum class Priority {
            CRITICAL,  // Must have for app to work
            HIGH,      // Important but has workarounds
            MEDIUM,    // Helpful but not essential
            LOW        // Nice to have
        }
    }

    /**
     * Actions that can be taken as fallbacks
     */
    sealed class FallbackAction {
        object OpenSettings : FallbackAction()
        object ManualLocationShare : FallbackAction()
        object ManualCall : FallbackAction()
        object ManualSMS : FallbackAction()
        object UseAlternativeFeature : FallbackAction()
        data class OpenExternalApp(val packageName: String) : FallbackAction()
    }

    /**
     * Check if a permission is granted
     */
    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check permission with detailed result including fallbacks
     */
    fun checkPermissionWithFallbacks(
        context: Context,
        permission: String,
        rationale: String? = null
    ): PermissionResult {
        val granted = isPermissionGranted(context, permission)
        
        if (granted) {
            return PermissionResult(granted = true)
        }

        // Permission denied - provide fallbacks
        val fallbacks = getFallbacksForPermission(permission)
        
        return PermissionResult(
            granted = false,
            permanentlyDenied = false, // This would need Activity context to check
            fallbackSuggestions = fallbacks
        )
    }

    /**
     * Get fallback suggestions for a specific permission
     */
    private fun getFallbacksForPermission(permission: String): List<FallbackSuggestion> {
        return when (permission) {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION -> {
                listOf(
                    FallbackSuggestion(
                        title = "📍 Grant Location Permission",
                        description = "Go to Settings and enable location access. This is critical for emergency responders to find you.",
                        action = FallbackAction.OpenSettings,
                        priority = FallbackSuggestion.Priority.CRITICAL
                    ),
                    FallbackSuggestion(
                        title = "📌 Share Location Manually",
                        description = "You can manually share your location when triggering an emergency (less accurate).",
                        action = FallbackAction.ManualLocationShare,
                        priority = FallbackSuggestion.Priority.HIGH
                    ),
                    FallbackSuggestion(
                        title = "🗺️ Use Google Maps",
                        description = "Send your location via Google Maps to contacts manually.",
                        action = FallbackAction.OpenExternalApp("com.google.android.apps.maps"),
                        priority = FallbackSuggestion.Priority.MEDIUM
                    )
                )
            }

            Manifest.permission.SEND_SMS -> {
                listOf(
                    FallbackSuggestion(
                        title = "💬 Enable SMS Permission",
                        description = "Go to Settings to allow SMS. Emergency alerts won't be sent automatically without this.",
                        action = FallbackAction.OpenSettings,
                        priority = FallbackSuggestion.Priority.HIGH
                    ),
                    FallbackSuggestion(
                        title = "📱 Send SMS Manually",
                        description = "You'll need to manually send SMS to your emergency contacts.",
                        action = FallbackAction.ManualSMS,
                        priority = FallbackSuggestion.Priority.HIGH
                    ),
                    FallbackSuggestion(
                        title = "📞 Use Phone Calls Instead",
                        description = "Call your emergency contacts directly instead of SMS.",
                        action = FallbackAction.ManualCall,
                        priority = FallbackSuggestion.Priority.MEDIUM
                    )
                )
            }

            Manifest.permission.CALL_PHONE -> {
                listOf(
                    FallbackSuggestion(
                        title = "📞 Enable Call Permission",
                        description = "Go to Settings to allow calling. Emergency calls won't be automated without this.",
                        action = FallbackAction.OpenSettings,
                        priority = FallbackSuggestion.Priority.HIGH
                    ),
                    FallbackSuggestion(
                        title = "☎️ Dial Manually",
                        description = "Emergency contacts will be shown, but you'll need to dial manually.",
                        action = FallbackAction.ManualCall,
                        priority = FallbackSuggestion.Priority.HIGH
                    )
                )
            }

            Manifest.permission.RECORD_AUDIO -> {
                listOf(
                    FallbackSuggestion(
                        title = "🎙️ Enable Microphone",
                        description = "Go to Settings to allow audio recording for evidence collection.",
                        action = FallbackAction.OpenSettings,
                        priority = FallbackSuggestion.Priority.MEDIUM
                    ),
                    FallbackSuggestion(
                        title = "📸 Use Camera Instead",
                        description = "You can use video recording as an alternative to audio.",
                        action = FallbackAction.UseAlternativeFeature,
                        priority = FallbackSuggestion.Priority.MEDIUM
                    )
                )
            }

            Manifest.permission.CAMERA -> {
                listOf(
                    FallbackSuggestion(
                        title = "📸 Enable Camera",
                        description = "Go to Settings to allow camera access for video evidence.",
                        action = FallbackAction.OpenSettings,
                        priority = FallbackSuggestion.Priority.MEDIUM
                    ),
                    FallbackSuggestion(
                        title = "🎙️ Use Audio Recording",
                        description = "You can use audio recording instead of video.",
                        action = FallbackAction.UseAlternativeFeature,
                        priority = FallbackSuggestion.Priority.MEDIUM
                    )
                )
            }

            Manifest.permission.POST_NOTIFICATIONS -> {
                listOf(
                    FallbackSuggestion(
                        title = "🔔 Enable Notifications",
                        description = "Go to Settings to allow notifications for emergency alerts.",
                        action = FallbackAction.OpenSettings,
                        priority = FallbackSuggestion.Priority.HIGH
                    )
                )
            }

            else -> {
                listOf(
                    FallbackSuggestion(
                        title = "⚙️ Check Settings",
                        description = "Go to app settings to review permissions.",
                        action = FallbackAction.OpenSettings,
                        priority = FallbackSuggestion.Priority.MEDIUM
                    )
                )
            }
        }
    }

    /**
     * Get color for permission priority
     */
    fun getPriorityColor(priority: FallbackSuggestion.Priority): androidx.compose.ui.graphics.Color {
        return when (priority) {
            FallbackSuggestion.Priority.CRITICAL -> androidx.compose.ui.graphics.Color(0xFFD32F2F) // Red
            FallbackSuggestion.Priority.HIGH -> androidx.compose.ui.graphics.Color(0xFFF57C00) // Orange
            FallbackSuggestion.Priority.MEDIUM -> androidx.compose.ui.graphics.Color(0xFFFBC02D) // Yellow
            FallbackSuggestion.Priority.LOW -> androidx.compose.ui.graphics.Color(0xFF388E3C) // Green
        }
    }

    /**
     * Open app settings
     */
    fun openAppSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Log.i(TAG, "Opening app settings")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open app settings", e)
            // Fallback: open general settings
            try {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to open settings", e2)
            }
        }
    }

    /**
     * Get user-friendly error message for permission denial
     */
    fun getPermissionDenialMessage(permission: String): String {
        return when (permission) {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION -> 
                "⚠️ Location access denied. Emergency contacts won't receive your location. This significantly reduces your safety."

            Manifest.permission.SEND_SMS -> 
                "⚠️ SMS access denied. Emergency alerts won't be sent automatically. You'll need to manually contact your emergency contacts."

            Manifest.permission.CALL_PHONE -> 
                "⚠️ Call access denied. Emergency calls won't be automated. You'll need to manually dial your emergency contacts."

            Manifest.permission.RECORD_AUDIO -> 
                "⚠️ Microphone access denied. Audio evidence recording is disabled."

            Manifest.permission.CAMERA -> 
                "⚠️ Camera access denied. Video evidence recording is disabled."

            Manifest.permission.POST_NOTIFICATIONS -> 
                "⚠️ Notification access denied. You may miss important emergency alerts."

            else -> 
                "⚠️ Permission denied. Some features may not work properly."
        }
    }

    /**
     * Get success message for permission grant
     */
    fun getPermissionGrantedMessage(permission: String): String {
        return when (permission) {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION -> 
                "✅ Location access granted. Your location will be shared in emergencies."

            Manifest.permission.SEND_SMS -> 
                "✅ SMS access granted. Emergency alerts will be sent automatically."

            Manifest.permission.CALL_PHONE -> 
                "✅ Call access granted. Emergency calls will be automated."

            Manifest.permission.RECORD_AUDIO -> 
                "✅ Microphone access granted. Audio evidence can be recorded."

            Manifest.permission.CAMERA -> 
                "✅ Camera access granted. Video evidence can be recorded."

            Manifest.permission.POST_NOTIFICATIONS -> 
                "✅ Notification access granted. You'll receive emergency alerts."

            else -> 
                "✅ Permission granted."
        }
    }

    /**
     * Check if all critical permissions are granted
     */
    fun areCriticalPermissionsGranted(context: Context): Boolean {
        return PermissionManager.CRITICAL_PERMISSIONS.all { 
            isPermissionGranted(context, it) 
        }
    }

    /**
     * Get list of missing critical permissions
     */
    fun getMissingCriticalPermissions(context: Context): List<String> {
        return PermissionManager.CRITICAL_PERMISSIONS.filter { 
            !isPermissionGranted(context, it) 
        }
    }

    /**
     * Get detailed permission status
     */
    data class PermissionStatus(
        val permission: String,
        val name: String,
        val granted: Boolean,
        val critical: Boolean,
        val explanation: String
    )

    /**
     * Get status for all app permissions
     */
    fun getAllPermissionsStatus(context: Context): List<PermissionStatus> {
        return PermissionManager.ALL_PERMISSIONS.map { permission ->
            PermissionStatus(
                permission = permission,
                name = PermissionManager.getPermissionName(permission),
                granted = isPermissionGranted(context, permission),
                critical = PermissionManager.CRITICAL_PERMISSIONS.contains(permission),
                explanation = PermissionManager.getPermissionExplanation(permission)
            )
        }
    }

    /**
     * Execute a fallback action
     */
    fun executeFallbackAction(context: Context, action: FallbackAction) {
        when (action) {
            is FallbackAction.OpenSettings -> openAppSettings(context)
            
            is FallbackAction.ManualLocationShare -> {
                try {
                    // Open Google Maps for location sharing
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("geo:0,0?q=My+Location")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to open maps", e)
                }
            }
            
            is FallbackAction.ManualCall -> {
                // Will be handled by UI to show contact list
                Log.i(TAG, "Manual call action - show contact list")
            }
            
            is FallbackAction.ManualSMS -> {
                // Will be handled by UI to show SMS composer
                Log.i(TAG, "Manual SMS action - show SMS composer")
            }
            
            is FallbackAction.UseAlternativeFeature -> {
                // Will be handled by UI to show alternative
                Log.i(TAG, "Alternative feature action")
            }
            
            is FallbackAction.OpenExternalApp -> {
                try {
                    val intent = context.packageManager.getLaunchIntentForPackage(action.packageName)
                    if (intent != null) {
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                    } else {
                        // App not installed, open Play Store
                        val playStoreIntent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("market://details?id=${action.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(playStoreIntent)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to open external app", e)
                }
            }
        }
    }
}
