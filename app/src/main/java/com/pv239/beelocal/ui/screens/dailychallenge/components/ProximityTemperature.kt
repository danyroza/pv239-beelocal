package com.pv239.beelocal.ui.screens.dailychallenge.components

import androidx.compose.ui.graphics.Color

enum class ProximityTemperature(
    val label: String,
    val emoji: String,
    val description: String,
    val color: Color,
    val maxMeters: Int,
) {
    BOILING("Boiling!", "🌋", "< 10 m", Color(0xFFFF3D00), 10), HOT(
        "Hot!",
        "🔥",
        "< 50 m",
        Color(0xFFFF6D00),
        50
    ),
    WARM("Warm", "☀️", "< 150 m", Color(0xFFFFAB00), 150), LUKEWARM(
        "Lukewarm",
        "🌤️",
        "< 300 m",
        Color(0xFFFFD740),
        300
    ),
    COOL("Cool", "💨", "< 500 m", Color(0xFF80DEEA), 500), COLD(
        "Cold",
        "🥶",
        "< 1 000 m",
        Color(0xFF42A5F5),
        1000
    ),
    FREEZING("Freezing!", "🧊", "> 1 000 m", Color(0xFF90CAF9), Int.MAX_VALUE);

    companion object {
        fun fromDistance(meters: Int): ProximityTemperature =
            entries.first { meters <= it.maxMeters }
    }
}