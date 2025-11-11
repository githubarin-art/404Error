package com.runanywhere.startup_hackathon20.data

data class SafePlace(
    val placeId: String,
    val name: String,
    val types: List<String>,
    val latitude: Double,
    val longitude: Double,
    val distance: Float,
    val isOpen24h: Boolean,
    val phoneNumber: String?
)