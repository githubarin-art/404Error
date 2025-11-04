package com.runanywhere.startup_hackathon20.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Centralized permission management for the safety app
 */
object PermissionManager {

    /**
     * Core permissions needed for emergency features
     */
    val CORE_PERMISSIONS = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.SEND_SMS,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    /**
     * Additional permissions for Android 13+
     */
    val ANDROID_13_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        emptyList()
    }

    /**
     * Background location (Android 10+)
     */
    val BACKGROUND_LOCATION = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        listOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    } else {
        emptyList()
    }

    /**
     * Optional permissions for enhanced features
     */
    val OPTIONAL_PERMISSIONS = listOf(
        Manifest.permission.READ_CONTACTS
    )

    /**
     * All permissions combined
     */
    val ALL_PERMISSIONS = CORE_PERMISSIONS + ANDROID_13_PERMISSIONS + OPTIONAL_PERMISSIONS

    /**
     * Critical permissions that must be granted for app to function
     */
    val CRITICAL_PERMISSIONS = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.SEND_SMS,
        Manifest.permission.CALL_PHONE
    )

    /**
     * Check if a specific permission is granted
     */
    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if all core permissions are granted
     */
    fun areAllCorePermissionsGranted(context: Context): Boolean {
        return CORE_PERMISSIONS.all { isPermissionGranted(context, it) }
    }

    /**
     * Check if critical permissions are granted
     */
    fun areCriticalPermissionsGranted(context: Context): Boolean {
        return CRITICAL_PERMISSIONS.all { isPermissionGranted(context, it) }
    }

    /**
     * Get list of permissions that are not granted
     */
    fun getMissingPermissions(context: Context, permissions: List<String>): List<String> {
        return permissions.filter { !isPermissionGranted(context, it) }
    }

    /**
     * Get human-readable permission name
     */
    fun getPermissionName(permission: String): String {
        return when (permission) {
            Manifest.permission.ACCESS_FINE_LOCATION -> "Precise Location"
            Manifest.permission.ACCESS_COARSE_LOCATION -> "Approximate Location"
            Manifest.permission.ACCESS_BACKGROUND_LOCATION -> "Background Location"
            Manifest.permission.SEND_SMS -> "Send SMS"
            Manifest.permission.CALL_PHONE -> "Make Phone Calls"
            Manifest.permission.CAMERA -> "Camera"
            Manifest.permission.RECORD_AUDIO -> "Microphone"
            Manifest.permission.READ_CONTACTS -> "Read Contacts"
            Manifest.permission.POST_NOTIFICATIONS -> "Notifications"
            else -> permission.substringAfterLast(".")
        }
    }

    /**
     * Get permission explanation for user
     */
    fun getPermissionExplanation(permission: String): String {
        return when (permission) {
            Manifest.permission.ACCESS_FINE_LOCATION ->
                "Required to share your exact location with emergency contacts"

            Manifest.permission.ACCESS_COARSE_LOCATION ->
                "Required to share your approximate location with emergency contacts"

            Manifest.permission.ACCESS_BACKGROUND_LOCATION ->
                "Required to track your location even when the app is in the background"

            Manifest.permission.SEND_SMS ->
                "Required to send emergency SMS messages to your contacts"

            Manifest.permission.CALL_PHONE ->
                "Required to call emergency services and your contacts"

            Manifest.permission.CAMERA ->
                "Required to record video evidence during emergencies"

            Manifest.permission.RECORD_AUDIO ->
                "Required to record audio evidence during emergencies"

            Manifest.permission.READ_CONTACTS ->
                "Optional: Makes it easier to add emergency contacts from your phone"

            Manifest.permission.POST_NOTIFICATIONS ->
                "Required to show emergency alerts and status updates"

            else -> "Required for app functionality"
        }
    }
}
