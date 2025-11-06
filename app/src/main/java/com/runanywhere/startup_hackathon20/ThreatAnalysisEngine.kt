package com.runanywhere.startup_hackathon20

import android.content.Context
import android.location.Location
import android.util.Log
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * Regional Threat Level Analysis Engine
 * 
 * Privacy-First, Real-Time Threat Assessment System
 * - Aggregates multiple data sources
 * - Computes probabilistic threat scores
 * - Operates with <500ms latency
 * - Includes robust fallbacks
 * - Respects user privacy
 */
class ThreatAnalysisEngine(private val context: Context) {
    
    companion object {
        private const val TAG = "ThreatAnalysisEngine"
        private const val MAX_COMPUTATION_TIME_MS = 500L
    }
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var config = ThreatAnalysisConfig()
    
    // Cached data for offline/fallback mode
    private var cachedResult: ThreatAnalysisResult? = null
    private val analysisHistory = mutableListOf<ThreatAnalysisResult>()
    
    /**
     * Main entry point: Analyze current threat level
     * Returns result within 500ms or uses cached/fallback
     */
    suspend fun analyzeThreatLevel(
        location: Location?,
        forceRefresh: Boolean = false
    ): ThreatAnalysisResult = withContext(Dispatchers.Default) {
        
        val startTime = System.currentTimeMillis()
        
        try {
            // Check cache if not forcing refresh
            if (!forceRefresh && cachedResult != null) {
                val cacheAge = System.currentTimeMillis() - cachedResult!!.timestamp
                if (cacheAge < config.updateIntervalMs) {
                    Log.d(TAG, "Returning cached result (age: ${cacheAge}ms)")
                    return@withContext cachedResult!!
                }
            }
            
            Log.i(TAG, "Starting threat analysis...")
            
            // Fetch all data sources in parallel with timeout
            val dataFetchJob = async {
                withTimeoutOrNull(MAX_COMPUTATION_TIME_MS - 100) {
                    fetchAllDataSources(location)
                }
            }
            
            val allData = dataFetchJob.await() ?: run {
                Log.w(TAG, "Data fetch timeout, using fallback")
                return@withContext generateFallbackResult(location)
            }
            
            // Compute threat score
            val result = computeThreatScore(allData, location)
            
            // Cache result
            cachedResult = result
            analysisHistory.add(result)
            if (analysisHistory.size > 100) {
                analysisHistory.removeAt(0)
            }
            
            val elapsed = System.currentTimeMillis() - startTime
            Log.i(TAG, "Threat analysis complete in ${elapsed}ms: ${result.threatLevel} (score: ${result.threatScore})")
            
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in threat analysis", e)
            generateFallbackResult(location)
        }
    }
    
    /**
     * Fetch all data sources in parallel
     */
    private suspend fun fetchAllDataSources(location: Location?): AllDataSources {
        return coroutineScope {
            val crimeJob = async { if (config.enableCrimeData) fetchCrimeData(location) else null }
            val timeJob = async { if (config.enableTimeAnalysis) fetchTimeData() else null }
            val locationJob = async { if (config.enableLocationSafety) fetchLocationSafety(location) else null }
            val envJob = async { if (config.enableEnvironmental) fetchEnvironmentalData(location) else null }
            val networkJob = async { if (config.enableNetwork) fetchNetworkQuality() else null }
            val behaviorJob = async { if (config.enableBehavioral) fetchBehaviorData() else null }
            
            AllDataSources(
                crime = crimeJob.await(),
                time = timeJob.await(),
                locationSafety = locationJob.await(),
                environmental = envJob.await(),
                network = networkJob.await(),
                behavior = behaviorJob.await()
            )
        }
    }
    
    /**
     * Compute final threat score from all factors
     */
    private fun computeThreatScore(
        data: AllDataSources,
        location: Location?
    ): ThreatAnalysisResult {
        
        val factors = mutableListOf<ThreatFactor>()
        var totalWeight = 0f
        var weightedScore = 0f
        var availableSources = 0
        val totalSources = 6
        
        // Crime Factor
        data.crime?.let { crime ->
            val crimeScore = computeCrimeScore(crime)
            val weight = config.customWeights[ThreatCategory.CRIME] 
                ?: ThreatCategory.CRIME.defaultWeight
            val contribution = crimeScore * weight
            
            factors.add(ThreatFactor(
                category = ThreatCategory.CRIME,
                name = "Crime Statistics",
                value = crimeScore,
                weight = weight,
                contribution = contribution,
                description = "${crime.recentIncidents} incidents (${crime.severity * 100}% severity)",
                isAvailable = true
            ))
            
            weightedScore += contribution
            totalWeight += weight
            availableSources++
        }
        
        // Time Factor
        data.time?.let { time ->
            val timeScore = computeTimeScore(time)
            val weight = config.customWeights[ThreatCategory.TIME] 
                ?: ThreatCategory.TIME.defaultWeight
            val contribution = timeScore * weight
            
            factors.add(ThreatFactor(
                category = ThreatCategory.TIME,
                name = "Time-Based Risk",
                value = timeScore,
                weight = weight,
                contribution = contribution,
                description = if (time.isNightTime) "Night time (${time.currentHour}:00)" else "Day time",
                isAvailable = true
            ))
            
            weightedScore += contribution
            totalWeight += weight
            availableSources++
        }
        
        // Location Safety Factor
        data.locationSafety?.let { locSafety ->
            val locScore = computeLocationScore(locSafety)
            val weight = config.customWeights[ThreatCategory.LOCATION] 
                ?: ThreatCategory.LOCATION.defaultWeight
            val contribution = locScore * weight
            
            val distanceDesc = locSafety.distanceToPoliceStation?.let {
                "Police: ${(it/1000).toInt()}km away"
            } ?: "Unknown proximity to safety services"
            
            factors.add(ThreatFactor(
                category = ThreatCategory.LOCATION,
                name = "Location Safety",
                value = locScore,
                weight = weight,
                contribution = contribution,
                description = distanceDesc,
                isAvailable = true
            ))
            
            weightedScore += contribution
            totalWeight += weight
            availableSources++
        }
        
        // Environmental Factor
        data.environmental?.let { env ->
            val envScore = computeEnvironmentalScore(env)
            val weight = config.customWeights[ThreatCategory.ENVIRONMENTAL] 
                ?: ThreatCategory.ENVIRONMENTAL.defaultWeight
            val contribution = envScore * weight
            
            factors.add(ThreatFactor(
                category = ThreatCategory.ENVIRONMENTAL,
                name = "Environmental Conditions",
                value = envScore,
                weight = weight,
                contribution = contribution,
                description = "${env.weatherCondition}, visibility: ${(env.visibility * 100).toInt()}%",
                isAvailable = true
            ))
            
            weightedScore += contribution
            totalWeight += weight
            availableSources++
        }
        
        // Network Quality Factor
        data.network?.let { net ->
            val netScore = computeNetworkScore(net)
            val weight = config.customWeights[ThreatCategory.NETWORK] 
                ?: ThreatCategory.NETWORK.defaultWeight
            val contribution = netScore * weight
            
            factors.add(ThreatFactor(
                category = ThreatCategory.NETWORK,
                name = "Network Quality",
                value = netScore,
                weight = weight,
                contribution = contribution,
                description = "${net.connectionType}, signal: ${(net.signalStrength * 100).toInt()}%",
                isAvailable = true
            ))
            
            weightedScore += contribution
            totalWeight += weight
            availableSources++
        }
        
        // Behavior Factor
        data.behavior?.let { behavior ->
            val behaviorScore = computeBehaviorScore(behavior)
            val weight = config.customWeights[ThreatCategory.BEHAVIORAL] 
                ?: ThreatCategory.BEHAVIORAL.defaultWeight
            val contribution = behaviorScore * weight
            
            factors.add(ThreatFactor(
                category = ThreatCategory.BEHAVIORAL,
                name = "User Behavior",
                value = behaviorScore,
                weight = weight,
                contribution = contribution,
                description = behavior.movementPattern,
                isAvailable = true
            ))
            
            weightedScore += contribution
            totalWeight += weight
            availableSources++
        }
        
        // Normalize score
        val normalizedScore = if (totalWeight > 0) {
            (weightedScore / totalWeight) * config.userSensitivity
        } else {
            0.5f // Default medium risk if no data
        }
        
        val finalScore = normalizedScore.coerceIn(0f, 1f)
        val threatLevel = RegionalThreatLevel.fromScore(finalScore)
        
        // Generate primary reasons
        val primaryReasons = factors
            .sortedByDescending { it.contribution }
            .take(3)
            .map { "${it.category.displayName}: ${it.description}" }
        
        // Calculate confidence based on data availability
        val confidence = (availableSources.toFloat() / totalSources.toFloat())
        
        return ThreatAnalysisResult(
            timestamp = System.currentTimeMillis(),
            threatScore = finalScore,
            threatLevel = threatLevel,
            location = location,
            factors = factors,
            primaryReasons = primaryReasons,
            confidence = confidence,
            dataSourcesAvailable = availableSources,
            dataSourcesTotal = totalSources
        )
    }
    
    /**
     * Individual scoring functions
     */
    
    private fun computeCrimeScore(crime: CrimeData): Float {
        // Higher incidents = higher score
        val incidentScore = min(crime.recentIncidents / 10f, 1f)
        // Severity directly contributes
        val severityScore = crime.severity
        // Recency matters - older data is less relevant
        val ageMs = System.currentTimeMillis() - crime.lastUpdated
        val recencyFactor = if (ageMs < 86400000) 1f else 0.7f // 24 hours
        
        return ((incidentScore * 0.6f + severityScore * 0.4f) * recencyFactor).coerceIn(0f, 1f)
    }
    
    private fun computeTimeScore(time: TimeRiskData): Float {
        var score = 0f
        
        // Night time increases risk
        if (time.isNightTime) score += 0.4f
        
        // Late night/early morning is highest risk
        if (time.currentHour in 22..23 || time.currentHour in 0..4) score += 0.3f
        
        // Weekend nights slightly higher
        if (time.isWeekend && time.isNightTime) score += 0.1f
        
        // Event days can increase risk
        if (time.isEventDay) score += 0.2f
        
        return (score * time.riskMultiplier).coerceIn(0f, 1f)
    }
    
    private fun computeLocationScore(locSafety: LocationSafetyData): Float {
        var score = 0.5f // Start neutral
        
        // Known high-risk area
        if (locSafety.isHighRiskArea) score += 0.4f
        
        // Known safe zone
        if (locSafety.isKnownSafeZone) score -= 0.3f
        
        // Distance to police (closer = safer)
        locSafety.distanceToPoliceStation?.let { distance ->
            val distanceScore = (distance / 5000f).coerceIn(0f, 0.3f) // Max 5km
            score += distanceScore
        }
        
        // Poor lighting increases risk
        score += (1f - locSafety.lightingQuality) * 0.2f
        
        // Low CCTV density increases risk
        score += (1f - locSafety.cctvDensity) * 0.1f
        
        // Low population density can increase risk
        if (locSafety.populationDensity < 0.3f) score += 0.2f
        
        return score.coerceIn(0f, 1f)
    }
    
    private fun computeEnvironmentalScore(env: EnvironmentalData): Float {
        var score = 0f
        
        // Poor visibility increases risk
        score += (1f - env.visibility) * 0.5f
        
        // Dark conditions increase risk
        if (env.isDark) score += 0.3f
        
        // Adverse weather slightly increases risk
        if (env.isRaining) score += 0.1f
        if (env.isFoggy) score += 0.1f
        
        return score.coerceIn(0f, 1f)
    }
    
    private fun computeNetworkScore(net: NetworkQualityData): Float {
        var score = 0f
        
        // Poor signal increases risk (can't call for help)
        score += (1f - net.signalStrength) * 0.6f
        
        // No connection is significant risk
        if (!net.isConnected) score += 0.4f
        
        return score.coerceIn(0f, 1f)
    }
    
    private fun computeBehaviorScore(behavior: BehaviorData): Float {
        var score = 0f
        
        // Deviation from routine
        score += behavior.routineDeviation * 0.4f
        
        // Being alone increases risk
        if (behavior.isAlone) score += 0.3f
        
        // Rapid movement might indicate distress
        if (behavior.movementPattern == "Rapid") score += 0.3f
        
        return score.coerceIn(0f, 1f)
    }
    
    /**
     * Data fetching functions (with simulated data for demo)
     */
    
    private suspend fun fetchCrimeData(location: Location?): CrimeData {
        delay(50) // Simulate API call
        
        // TODO: Integrate with real crime API (e.g., police data, crimemapping.com)
        // For now, simulate based on time and location
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isNight = hour < 6 || hour > 21
        
        return CrimeData(
            recentIncidents = if (isNight) (3..8).random() else (1..4).random(),
            severity = if (isNight) (0.4f..0.8f).random() else (0.2f..0.5f).random(),
            types = listOf("Theft", "Assault", "Vandalism").shuffled().take((1..2).random()),
            withinRadius = 1000f,
            lastUpdated = System.currentTimeMillis() - (1000 * 60 * 30), // 30 min ago
            source = "Local Crime Database"
        )
    }
    
    private suspend fun fetchTimeData(): TimeRiskData {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        return TimeRiskData(
            currentHour = hour,
            isNightTime = hour < 6 || hour > 21,
            isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY,
            isHoliday = false, // TODO: Check holiday calendar
            isEventDay = false, // TODO: Check local events
            riskMultiplier = 1.0f
        )
    }
    
    private suspend fun fetchLocationSafety(location: Location?): LocationSafetyData {
        delay(50) // Simulate API call
        
        // TODO: Integrate with Google Places API for actual safety data
        return LocationSafetyData(
            distanceToPoliceStation = (500f..5000f).random(),
            distanceToHospital = (800f..6000f).random(),
            cctvDensity = (0.3f..0.8f).random(),
            lightingQuality = if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) in 6..18) 0.9f else (0.3f..0.7f).random(),
            populationDensity = (0.4f..0.9f).random(),
            isKnownSafeZone = false,
            isHighRiskArea = false
        )
    }
    
    private suspend fun fetchEnvironmentalData(location: Location?): EnvironmentalData {
        delay(30) // Simulate API call
        
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isDark = hour < 6 || hour > 20
        
        // TODO: Integrate with weather API
        return EnvironmentalData(
            weatherCondition = listOf("Clear", "Cloudy", "Rainy", "Foggy").random(),
            visibility = if (isDark) (0.5f..0.8f).random() else (0.8f..1.0f).random(),
            temperature = (15f..30f).random(),
            isDark = isDark,
            isRaining = (0..10).random() < 2, // 20% chance
            isFoggy = (0..10).random() < 1 // 10% chance
        )
    }
    
    private suspend fun fetchNetworkQuality(): NetworkQualityData {
        // TODO: Actually check network quality
        return NetworkQualityData(
            signalStrength = (0.5f..1.0f).random(),
            connectionType = listOf("WiFi", "4G", "5G", "3G").random(),
            isConnected = true,
            latency = (20..200).random()
        )
    }
    
    private suspend fun fetchBehaviorData(): BehaviorData {
        // TODO: Analyze actual user movement patterns
        return BehaviorData(
            movementPattern = "Normal",
            routineDeviation = (0f..0.3f).random(),
            isAlone = true, // TODO: Detect from contacts/calendar
            activityType = "Walking"
        )
    }
    
    /**
     * Fallback result when data is unavailable
     */
    private fun generateFallbackResult(location: Location?): ThreatAnalysisResult {
        Log.w(TAG, "Generating fallback threat result")
        
        // Use cached result if available
        cachedResult?.let { return it }
        
        // Otherwise return medium risk as safe default
        return ThreatAnalysisResult(
            timestamp = System.currentTimeMillis(),
            threatScore = 0.5f,
            threatLevel = RegionalThreatLevel.MEDIUM,
            location = location,
            factors = emptyList(),
            primaryReasons = listOf("Limited data available - using safe defaults"),
            confidence = 0.3f,
            dataSourcesAvailable = 0,
            dataSourcesTotal = 6
        )
    }
    
    /**
     * Generate emergency alert messages
     */
    fun generateEmergencyAlert(
        result: ThreatAnalysisResult,
        userName: String,
        contactName: String
    ): EmergencyAlertTemplate {
        val locationStr = result.location?.let {
            "${it.latitude}, ${it.longitude}"
        } ?: "Location unavailable"
        
        val timestamp = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
            .format(Date(result.timestamp))
        
        return EmergencyAlertTemplate(
            threatLevel = result.threatLevel,
            userName = userName,
            location = locationStr,
            primaryReasons = result.primaryReasons,
            timestamp = timestamp,
            contactName = contactName
        )
    }
    
    /**
     * Get statistics from analysis history
     */
    fun getStatistics(): ThreatStatistics {
        if (analysisHistory.isEmpty()) {
            return ThreatStatistics(
                averageThreatScore = 0f,
                highThreatCount = 0,
                mediumThreatCount = 0,
                lowThreatCount = 0,
                totalAnalyses = 0,
                lastHighThreatTime = null,
                mostCommonFactors = emptyList()
            )
        }
        
        val avgScore = analysisHistory.map { it.threatScore }.average().toFloat()
        val highCount = analysisHistory.count { it.threatLevel == RegionalThreatLevel.HIGH }
        val mediumCount = analysisHistory.count { it.threatLevel == RegionalThreatLevel.MEDIUM }
        val lowCount = analysisHistory.count { it.threatLevel == RegionalThreatLevel.LOW }
        
        val lastHighThreat = analysisHistory
            .filter { it.threatLevel == RegionalThreatLevel.HIGH }
            .maxByOrNull { it.timestamp }
            ?.timestamp
        
        val allFactors = analysisHistory.flatMap { it.factors.map { f -> f.category.displayName } }
        val mostCommon = allFactors
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }
        
        return ThreatStatistics(
            averageThreatScore = avgScore,
            highThreatCount = highCount,
            mediumThreatCount = mediumCount,
            lowThreatCount = lowCount,
            totalAnalyses = analysisHistory.size,
            lastHighThreatTime = lastHighThreat,
            mostCommonFactors = mostCommon
        )
    }
    
    /**
     * Update configuration
     */
    fun updateConfig(newConfig: ThreatAnalysisConfig) {
        config = newConfig
        Log.i(TAG, "Configuration updated")
    }
    
    fun cleanup() {
        scope.cancel()
    }
}

/**
 * Container for all data sources
 */
private data class AllDataSources(
    val crime: CrimeData?,
    val time: TimeRiskData?,
    val locationSafety: LocationSafetyData?,
    val environmental: EnvironmentalData?,
    val network: NetworkQualityData?,
    val behavior: BehaviorData?
)

/**
 * Extension function to generate random float in range
 */
private fun ClosedFloatingPointRange<Float>.random(): Float {
    return Random.nextFloat() * (endInclusive - start) + start
}
