package com.livefleet.studio.model

/**
 * Strongly-typed brand classification tagged at discovery time so mixed/auto-detect
 * studio sessions route every machine directly to its native console without cross-talk.
 */
enum class BrandType(val canonicalBrandName: String) {
    KEISER("Keiser"),
    WATTBIKE("Wattbike"),
    CONCEPT2("Concept2"),
    STAGES("Stages"),
    SCHWINN("Schwinn"),
    FTMS("FTMS"),
    POWER_METER("PowerMeter");

    companion object {
        fun fromBrandString(brand: String): BrandType {
            return when (brand.trim().lowercase()) {
                "keiser" -> KEISER
                "wattbike" -> WATTBIKE
                "concept2" -> CONCEPT2
                "stages" -> STAGES
                "schwinn" -> SCHWINN
                "ftms", "lifefitness", "technogym", "matrix", "bodybike" -> FTMS
                "powermeter", "power" -> POWER_METER
                else -> FTMS
            }
        }
    }
}
