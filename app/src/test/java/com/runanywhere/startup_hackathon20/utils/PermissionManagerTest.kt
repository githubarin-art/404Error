package com.runanywhere.startup_hackathon20.utils

import android.Manifest
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for PermissionManager
 *
 * Tests permission-related utility functions
 * Note: Some tests require Android context (use androidTest for those)
 */
class PermissionManagerTest {

    @Test
    fun `CORE_PERMISSIONS contains required permissions`() {
        assertTrue(PermissionManager.CORE_PERMISSIONS.contains(Manifest.permission.ACCESS_FINE_LOCATION))
        assertTrue(PermissionManager.CORE_PERMISSIONS.contains(Manifest.permission.SEND_SMS))
        assertTrue(PermissionManager.CORE_PERMISSIONS.contains(Manifest.permission.CALL_PHONE))
        assertTrue(PermissionManager.CORE_PERMISSIONS.contains(Manifest.permission.RECORD_AUDIO))
        assertTrue(PermissionManager.CORE_PERMISSIONS.contains(Manifest.permission.CAMERA))
    }

    @Test
    fun `CRITICAL_PERMISSIONS are subset of CORE_PERMISSIONS`() {
        PermissionManager.CRITICAL_PERMISSIONS.forEach { permission ->
            assertTrue(
                "Critical permission $permission should be in CORE_PERMISSIONS",
                PermissionManager.CORE_PERMISSIONS.contains(permission)
            )
        }
    }

    @Test
    fun `getPermissionName returns human readable names`() {
        assertEquals(
            "Precise Location",
            PermissionManager.getPermissionName(Manifest.permission.ACCESS_FINE_LOCATION)
        )
        assertEquals(
            "Send SMS",
            PermissionManager.getPermissionName(Manifest.permission.SEND_SMS)
        )
        assertEquals(
            "Make Phone Calls",
            PermissionManager.getPermissionName(Manifest.permission.CALL_PHONE)
        )
        assertEquals(
            "Microphone",
            PermissionManager.getPermissionName(Manifest.permission.RECORD_AUDIO)
        )
        assertEquals(
            "Camera",
            PermissionManager.getPermissionName(Manifest.permission.CAMERA)
        )
    }

    @Test
    fun `getPermissionExplanation returns meaningful explanations`() {
        val explanation = PermissionManager.getPermissionExplanation(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        assertTrue(explanation.contains("location"))
        assertTrue(explanation.length > 20) // Should be meaningful

        val smsExplanation = PermissionManager.getPermissionExplanation(
            Manifest.permission.SEND_SMS
        )
        assertTrue(smsExplanation.contains("SMS") || smsExplanation.contains("message"))
    }

    @Test
    fun `ALL_PERMISSIONS includes core and optional permissions`() {
        // Should contain all core permissions
        PermissionManager.CORE_PERMISSIONS.forEach { permission ->
            assertTrue(
                "ALL_PERMISSIONS should contain $permission",
                PermissionManager.ALL_PERMISSIONS.contains(permission)
            )
        }

        // Should contain optional permissions
        PermissionManager.OPTIONAL_PERMISSIONS.forEach { permission ->
            assertTrue(
                "ALL_PERMISSIONS should contain optional permission $permission",
                PermissionManager.ALL_PERMISSIONS.contains(permission)
            )
        }
    }

    @Test
    fun `permissions are not duplicated`() {
        val allPerms = PermissionManager.ALL_PERMISSIONS
        val uniquePerms = allPerms.toSet()

        assertEquals(
            "ALL_PERMISSIONS should not contain duplicates",
            uniquePerms.size,
            allPerms.size
        )
    }

    @Test
    fun `unknown permission gets generic name`() {
        val unknownPermission = "android.permission.UNKNOWN_TEST"
        val name = PermissionManager.getPermissionName(unknownPermission)

        // Should at least return something (fallback behavior)
        assertNotNull(name)
        assertTrue(name.isNotEmpty())
    }
}
