package com.keiser.studio.model

/**
 * Universal Bike Protocols supported by the studio leaderboard.
 */
enum class BikeProtocol(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val broadcastType: String,
    val supportedModels: String,
    val companyOrServiceUuid: String
) {
    AUTO_DETECT(
        id = "auto",
        displayName = "Auto-Detect All Brands",
        subtitle = "Automatically discovers and parses any supported bike broadcasting in range",
        broadcastType = "Universal BLE Scanner (Passive)",
        supportedModels = "Keiser, Stages, Schwinn/4iiii, Concept2, FTMS Smart Bikes",
        companyOrServiceUuid = "Multi-Protocol Engine"
    ),
    KEISER(
        id = "keiser",
        displayName = "Keiser M3i / M3i Studio",
        subtitle = "Authentic Keiser broadcast with resistance gear, cadence, watts & calories",
        broadcastType = "Passive BLE Broadcast (Company 0x0102)",
        supportedModels = "Keiser M3i, M3i Studio, M3i Total Body Trainer",
        companyOrServiceUuid = "0x0102 (Keiser Corporation)"
    ),
    STAGES(
        id = "stages",
        displayName = "Stages SC3 / Stages Studio",
        subtitle = "Commercial studio broadcast with dual-sided power and cadence",
        broadcastType = "Stages BLE Broadcast (Company 0x0151)",
        supportedModels = "Stages SC3, SC2 with Power Meter, Stages SB20",
        companyOrServiceUuid = "0x0151 (Stages Cycling)"
    ),
    SCHWINN_4IIII(
        id = "schwinn_4iiii",
        displayName = "Schwinn Carbon Blue / 4iiii",
        subtitle = "Schwinn AC Performance Plus with 4iiii precision power cranks",
        broadcastType = "4iiii BLE Broadcast (Company 0x0182)",
        supportedModels = "Schwinn AC Performance Plus, AC Sport, 4iiii Podiiiium",
        companyOrServiceUuid = "0x0182 (4iiii Innovations)"
    ),
    CONCEPT2(
        id = "concept2",
        displayName = "Concept2 BikeErg / PM5",
        subtitle = "Concept2 PM5 console broadcast with cadence, watts, pace and distance",
        broadcastType = "Concept2 Broadcast Beacon (0x00CE / 0xCE06)",
        supportedModels = "Concept2 BikeErg, RowErg, SkiErg with PM5 monitor",
        companyOrServiceUuid = "0x00CE (Concept2, Inc.)"
    ),
    FTMS_UNIVERSAL(
        id = "ftms",
        displayName = "Standard Bluetooth FTMS / Smart Bikes",
        subtitle = "Bluetooth SIG international standard for commercial indoor fitness bikes",
        broadcastType = "Fitness Machine Service (UUID 0x1826)",
        supportedModels = "Wattbike Atom/Pro, Life Fitness IC5/IC6/IC7, Technogym, Matrix",
        companyOrServiceUuid = "UUID 0x1826 (Indoor Bike Data)"
    ),
    POWER_METERS(
        id = "power_meters",
        displayName = "Standard Power Pedals & Cranks",
        subtitle = "Standard third-party power meters installed on any spin bike",
        broadcastType = "Cycling Power Profile (UUID 0x1818)",
        supportedModels = "Favero Assioma, Garmin Rally, Quarq, SRM, PowerTap",
        companyOrServiceUuid = "UUID 0x1818 (Cycling Power)"
    );

    companion object {
        fun fromId(id: String): BikeProtocol {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: KEISER
        }
    }
}
