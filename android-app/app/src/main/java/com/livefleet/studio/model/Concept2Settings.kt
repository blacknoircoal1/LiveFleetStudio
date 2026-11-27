package com.livefleet.studio.model

/**
 * Hardware-matched Units display mode shared across Concept2 PM5 and Wattbike monitors:
 *  - [METRES]: Split Pace (/500m or /1km) + Distance (m)
 *  - [WATTS]: Live Watts + Average Watts
 *  - [CALORIES]: Cal/hr (kcal/hour) + Total Calories
 */
enum class Concept2MetricMode(val id: String, val label: String) {
    METRES("metres", "Pace & Metres"),
    WATTS("watts", "Watts & Avg"),
    CALORIES("calories", "Cal/hr & Cal");

    companion object {
        fun fromId(id: String?): Concept2MetricMode {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: METRES
        }
    }
}

/**
 * Subtype filter when operating Concept2 PM5 fleets.
 */
enum class Concept2ErgFilter(val id: String, val label: String) {
    ALL("all", "All Ergs"),
    BIKEERG_ONLY("bikeerg", "BikeErg"),
    ROWERG_ONLY("rowerg", "RowErg"),
    SKIERG_ONLY("skierg", "SkiErg");

    companion object {
        fun fromId(id: String?): Concept2ErgFilter {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: ALL
        }
    }
}
