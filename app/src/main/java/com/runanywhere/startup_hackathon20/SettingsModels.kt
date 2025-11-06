package com.runanywhere.startup_hackathon20

import org.json.JSONArray
import org.json.JSONObject

/**
 * Complete Settings Configuration
 */
data class AppSettings(
    val emergencyContacts: EmergencyContactsSettings = EmergencyContactsSettings(),
    val sosActivation: SOSActivationSettings = SOSActivationSettings(),
    val threatProtocol: ThreatProtocolSettings = ThreatProtocolSettings(),
    val notifications: NotificationSettings = NotificationSettings(),
    val location: LocationSettings = LocationSettings(),
    val privacy: PrivacySettings = PrivacySettings()
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("emergencyContacts", emergencyContacts.toJson())
        json.put("sosActivation", sosActivation.toJson())
        json.put("threatProtocol", threatProtocol.toJson())
        json.put("notifications", notifications.toJson())
        json.put("location", location.toJson())
        json.put("privacy", privacy.toJson())
        return json.toString()
    }

    companion object {
        fun fromJson(jsonString: String): AppSettings {
            val json = JSONObject(jsonString)
            return AppSettings(
                emergencyContacts = if (json.has("emergencyContacts"))
                    EmergencyContactsSettings.fromJson(json.getJSONObject("emergencyContacts"))
                else EmergencyContactsSettings(),
                sosActivation = if (json.has("sosActivation"))
                    SOSActivationSettings.fromJson(json.getJSONObject("sosActivation"))
                else SOSActivationSettings(),
                threatProtocol = if (json.has("threatProtocol"))
                    ThreatProtocolSettings.fromJson(json.getJSONObject("threatProtocol"))
                else ThreatProtocolSettings(),
                notifications = if (json.has("notifications"))
                    NotificationSettings.fromJson(json.getJSONObject("notifications"))
                else NotificationSettings(),
                location = if (json.has("location"))
                    LocationSettings.fromJson(json.getJSONObject("location"))
                else LocationSettings(),
                privacy = if (json.has("privacy"))
                    PrivacySettings.fromJson(json.getJSONObject("privacy"))
                else PrivacySettings()
            )
        }
    }
}

/**
 * Emergency Contacts Settings
 */
data class EmergencyContactsSettings(
    val sortByPriority: Boolean = true,
    val allowDuplicates: Boolean = false,
    val requireValidation: Boolean = true
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("sortByPriority", sortByPriority)
        json.put("allowDuplicates", allowDuplicates)
        json.put("requireValidation", requireValidation)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): EmergencyContactsSettings {
            return EmergencyContactsSettings(
                sortByPriority = json.optBoolean("sortByPriority", true),
                allowDuplicates = json.optBoolean("allowDuplicates", false),
                requireValidation = json.optBoolean("requireValidation", true)
            )
        }
    }
}

/**
 * SOS Activation Settings
 */
data class SOSActivationSettings(
    val triggerMethods: Set<SOSTriggerMethod> = setOf(
        SOSTriggerMethod.HIDDEN_BUTTON,
        SOSTriggerMethod.LONG_PRESS
    ),
    val confirmationMethod: SOSConfirmation = SOSConfirmation.TRIPLE_TAP,
    val silentAlarmMode: Boolean = false,
    val workWhenLocked: Boolean = true,
    val hapticFeedback: Boolean = true
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        val methodsArray = JSONArray()
        triggerMethods.forEach { methodsArray.put(it.name) }
        json.put("triggerMethods", methodsArray)
        json.put("confirmationMethod", confirmationMethod.name)
        json.put("silentAlarmMode", silentAlarmMode)
        json.put("workWhenLocked", workWhenLocked)
        json.put("hapticFeedback", hapticFeedback)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): SOSActivationSettings {
            val methods = mutableSetOf<SOSTriggerMethod>()
            val methodsArray = json.optJSONArray("triggerMethods")
            if (methodsArray != null) {
                for (i in 0 until methodsArray.length()) {
                    try {
                        methods.add(SOSTriggerMethod.valueOf(methodsArray.getString(i)))
                    } catch (e: Exception) {
                    }
                }
            }

            return SOSActivationSettings(
                triggerMethods = methods.ifEmpty { setOf(SOSTriggerMethod.HIDDEN_BUTTON) },
                confirmationMethod = try {
                    SOSConfirmation.valueOf(json.optString("confirmationMethod", "TRIPLE_TAP"))
                } catch (e: Exception) {
                    SOSConfirmation.TRIPLE_TAP
                },
                silentAlarmMode = json.optBoolean("silentAlarmMode", false),
                workWhenLocked = json.optBoolean("workWhenLocked", true),
                hapticFeedback = json.optBoolean("hapticFeedback", true)
            )
        }
    }
}

enum class SOSTriggerMethod(val displayName: String) {
    HIDDEN_BUTTON("Hidden Button"),
    LONG_PRESS("Long Press"),
    TRIPLE_TAP("Triple Tap"),
    SHAKE_GESTURE("Shake Gesture"),
    VOLUME_BUTTONS("Volume Buttons (5x)")
}

enum class SOSConfirmation(val displayName: String) {
    NONE("None (Instant)"),
    DOUBLE_TAP("Double Tap"),
    TRIPLE_TAP("Triple Tap"),
    LONG_PRESS("Long Press (2s)"),
    SLIDE_CONFIRM("Slide to Confirm")
}

/**
 * Threat Protocol Settings
 */
data class ThreatProtocolSettings(
    val threatSensitivity: ThreatSensitivity = ThreatSensitivity.BALANCED,
    val biometricFallback: Boolean = true,
    val securityQuestion: String? = null,
    val securityAnswer: String? = null,
    val autoEscalation: Boolean = true,
    val escalationDelaySeconds: Int = 60
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("threatSensitivity", threatSensitivity.name)
        json.put("biometricFallback", biometricFallback)
        json.put("securityQuestion", securityQuestion ?: "")
        json.put("securityAnswer", securityAnswer ?: "")
        json.put("autoEscalation", autoEscalation)
        json.put("escalationDelaySeconds", escalationDelaySeconds)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): ThreatProtocolSettings {
            return ThreatProtocolSettings(
                threatSensitivity = try {
                    ThreatSensitivity.valueOf(json.optString("threatSensitivity", "BALANCED"))
                } catch (e: Exception) {
                    ThreatSensitivity.BALANCED
                },
                biometricFallback = json.optBoolean("biometricFallback", true),
                securityQuestion = json.optString("securityQuestion").takeIf { it.isNotEmpty() },
                securityAnswer = json.optString("securityAnswer").takeIf { it.isNotEmpty() },
                autoEscalation = json.optBoolean("autoEscalation", true),
                escalationDelaySeconds = json.optInt("escalationDelaySeconds", 60)
            )
        }
    }
}

enum class ThreatSensitivity(
    val displayName: String,
    val multiplier: Float,
    val description: String
) {
    CONSERVATIVE("Conservative", 0.7f, "Less sensitive - fewer false alarms"),
    BALANCED("Balanced", 1.0f, "Recommended - balanced sensitivity"),
    AGGRESSIVE("Aggressive", 1.3f, "Highly sensitive - triggers faster")
}

/**
 * Notification Settings
 */
data class NotificationSettings(
    val alertChannels: Set<AlertChannel> = setOf(
        AlertChannel.SMS,
        AlertChannel.IN_APP
    ),
    val messageTone: MessageTone = MessageTone.URGENT,
    val escalationEnabled: Boolean = true,
    val escalationDelayMinutes: Int = 2,
    val repeatUntilAcknowledged: Boolean = true,
    val callAfterSMS: Boolean = false,
    val callDelayMinutes: Int = 3
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        val channelsArray = JSONArray()
        alertChannels.forEach { channelsArray.put(it.name) }
        json.put("alertChannels", channelsArray)
        json.put("messageTone", messageTone.name)
        json.put("escalationEnabled", escalationEnabled)
        json.put("escalationDelayMinutes", escalationDelayMinutes)
        json.put("repeatUntilAcknowledged", repeatUntilAcknowledged)
        json.put("callAfterSMS", callAfterSMS)
        json.put("callDelayMinutes", callDelayMinutes)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): NotificationSettings {
            val channels = mutableSetOf<AlertChannel>()
            val channelsArray = json.optJSONArray("alertChannels")
            if (channelsArray != null) {
                for (i in 0 until channelsArray.length()) {
                    try {
                        channels.add(AlertChannel.valueOf(channelsArray.getString(i)))
                    } catch (e: Exception) {
                    }
                }
            }

            return NotificationSettings(
                alertChannels = channels.ifEmpty { setOf(AlertChannel.SMS) },
                messageTone = try {
                    MessageTone.valueOf(json.optString("messageTone", "URGENT"))
                } catch (e: Exception) {
                    MessageTone.URGENT
                },
                escalationEnabled = json.optBoolean("escalationEnabled", true),
                escalationDelayMinutes = json.optInt("escalationDelayMinutes", 2),
                repeatUntilAcknowledged = json.optBoolean("repeatUntilAcknowledged", true),
                callAfterSMS = json.optBoolean("callAfterSMS", false),
                callDelayMinutes = json.optInt("callDelayMinutes", 3)
            )
        }
    }
}

enum class AlertChannel(val displayName: String) {
    SMS("SMS Message"),
    PHONE_CALL("Phone Call"),
    IN_APP("In-App Notification"),
    EMAIL("Email")
}

enum class MessageTone(val displayName: String) {
    FORMAL("Formal"),
    URGENT("Urgent"),
    DISCREET("Discreet")
}

/**
 * Location Settings
 */
data class LocationSettings(
    val updateInterval: LocationUpdateInterval = LocationUpdateInterval.ADAPTIVE,
    val locationMode: LocationMode = LocationMode.LIVE_SHARING,
    val safeZones: List<SafeZone> = emptyList(),
    val geofenceEnabled: Boolean = false,
    val notifyOnExitSafeZone: Boolean = true,
    val lowBatteryFallback: Boolean = true
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("updateInterval", updateInterval.name)
        json.put("locationMode", locationMode.name)
        val zonesArray = JSONArray()
        safeZones.forEach { zonesArray.put(it.toJson()) }
        json.put("safeZones", zonesArray)
        json.put("geofenceEnabled", geofenceEnabled)
        json.put("notifyOnExitSafeZone", notifyOnExitSafeZone)
        json.put("lowBatteryFallback", lowBatteryFallback)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): LocationSettings {
            val zones = mutableListOf<SafeZone>()
            val zonesArray = json.optJSONArray("safeZones")
            if (zonesArray != null) {
                for (i in 0 until zonesArray.length()) {
                    try {
                        zones.add(SafeZone.fromJson(zonesArray.getJSONObject(i)))
                    } catch (e: Exception) {
                    }
                }
            }

            return LocationSettings(
                updateInterval = try {
                    LocationUpdateInterval.valueOf(json.optString("updateInterval", "ADAPTIVE"))
                } catch (e: Exception) {
                    LocationUpdateInterval.ADAPTIVE
                },
                locationMode = try {
                    LocationMode.valueOf(json.optString("locationMode", "LIVE_SHARING"))
                } catch (e: Exception) {
                    LocationMode.LIVE_SHARING
                },
                safeZones = zones,
                geofenceEnabled = json.optBoolean("geofenceEnabled", false),
                notifyOnExitSafeZone = json.optBoolean("notifyOnExitSafeZone", true),
                lowBatteryFallback = json.optBoolean("lowBatteryFallback", true)
            )
        }
    }
}

enum class LocationUpdateInterval(val displayName: String, val seconds: Int) {
    REALTIME("Real-time (5s)", 5),
    FAST("Fast (15s)", 15),
    NORMAL("Normal (30s)", 30),
    ADAPTIVE("Adaptive (Smart)", -1)
}

enum class LocationMode(val displayName: String) {
    LIVE_SHARING("Live Sharing"),
    LAST_KNOWN("Last Known Location"),
    ON_DEMAND("On Demand Only")
}

data class SafeZone(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float = 100f
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("name", name)
        json.put("latitude", latitude)
        json.put("longitude", longitude)
        json.put("radiusMeters", radiusMeters)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): SafeZone {
            return SafeZone(
                id = json.optString("id", java.util.UUID.randomUUID().toString()),
                name = json.getString("name"),
                latitude = json.getDouble("latitude"),
                longitude = json.getDouble("longitude"),
                radiusMeters = json.optDouble("radiusMeters", 100.0).toFloat()
            )
        }
    }
}

/**
 * Privacy Settings
 */
data class PrivacySettings(
    val dataCollectionConsent: Boolean = false,
    val locationSharingConsent: Boolean = false,
    val sensorAccessConsent: Boolean = false,
    val analyticsConsent: Boolean = false,
    val encryptLocalData: Boolean = true,
    val autoDeleteAfterDays: Int = 30,
    val anonymizeData: Boolean = true
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("dataCollectionConsent", dataCollectionConsent)
        json.put("locationSharingConsent", locationSharingConsent)
        json.put("sensorAccessConsent", sensorAccessConsent)
        json.put("analyticsConsent", analyticsConsent)
        json.put("encryptLocalData", encryptLocalData)
        json.put("autoDeleteAfterDays", autoDeleteAfterDays)
        json.put("anonymizeData", anonymizeData)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): PrivacySettings {
            return PrivacySettings(
                dataCollectionConsent = json.optBoolean("dataCollectionConsent", false),
                locationSharingConsent = json.optBoolean("locationSharingConsent", false),
                sensorAccessConsent = json.optBoolean("sensorAccessConsent", false),
                analyticsConsent = json.optBoolean("analyticsConsent", false),
                encryptLocalData = json.optBoolean("encryptLocalData", true),
                autoDeleteAfterDays = json.optInt("autoDeleteAfterDays", 30),
                anonymizeData = json.optBoolean("anonymizeData", true)
            )
        }
    }
}

/**
 * Settings Section for UI organization
 */
enum class SettingsSection(val displayName: String, val icon: String) {
    EMERGENCY_CONTACTS("Emergency Contacts", "person"),
    SOS_ACTIVATION("SOS Activation", "emergency"),
    THREAT_PROTOCOL("Threat Protocol", "shield"),
    NOTIFICATIONS("Notifications", "notifications"),
    LOCATION("Location & Tracking", "location_on"),
    PRIVACY("Privacy & Data", "lock"),
    ABOUT("About & Help", "info")
}
