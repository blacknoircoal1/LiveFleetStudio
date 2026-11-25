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
    val companyOrServiceUuid: String,
    val setupTip: String
) {
    AUTO_DETECT(
        id = "auto",
        displayName = "Auto-Detect All Brands",
        subtitle = "Automatically discovers all supported bikes & ergs and renders each in its native console",
        broadcastType = "Passive BLE (Keiser) + BLE GATT (1–6) + USB ANT+ (30+)",
        supportedModels = "Keiser, Stages, Schwinn, Concept2, Wattbike, FTMS, Power Pedals",
        companyOrServiceUuid = "Multi-Transport Engine",
        setupTip = "Start pedaling or rowing on any supported machine. Plug in a USB-C ANT+ dongle for 10–30+ non-Keiser studio bikes simultaneously."
    ),
    KEISER(
        id = "keiser",
        displayName = "Keiser M3i / M3i Studio",
        subtitle = "Authentic passive Keiser broadcast with resistance gear, cadence, watts & calories",
        broadcastType = "Passive BLE Broadcast (Unlimited bikes, no pairing)",
        supportedModels = "Keiser M3i, M3i Studio, M3i Total Body Trainer",
        companyOrServiceUuid = "Company 0x0102 (Keiser Corporation)",
        setupTip = "No pairing required — simply pedal the bike to wake its computer and live data appears automatically for 30+ bikes."
    ),
    STAGES(
        id = "stages",
        displayName = "Stages SC3 / Stages Studio",
        subtitle = "BLE GATT Cycling Power (0x1818) for 1–6 bikes, or USB ANT+ Bike Power for 30+ studio bikes",
        broadcastType = "BLE GATT 0x1818 (1–6 bikes) • USB ANT+ Power (30+ bikes)",
        supportedModels = "Stages SC3, SC2 with Power Meter, Stages SB20",
        companyOrServiceUuid = "UUID 0x1818 (0x2A63) / ANT+ 0x0B",
        setupTip = "Spin the cranks to wake the power meter or SIC2 console. Use a USB-C ANT+ dongle when running more than 6 Stages bikes in a class."
    ),
    SCHWINN_4IIII(
        id = "schwinn_4iiii",
        displayName = "Schwinn Carbon Blue / 4iiii",
        subtitle = "BLE GATT 0x1818 (4iiii cranks) + USB ANT+ (MPower Echelon2 & 4iiii group classes)",
        broadcastType = "BLE GATT 0x1818 (4iiii) • USB ANT+ Power/FE-C (Echelon2 & 30+)",
        supportedModels = "Schwinn AC Performance Plus, Echelon2, 4iiii Precision",
        companyOrServiceUuid = "UUID 0x1818 / ANT+ 0x0B & 0x11",
        setupTip = "4iiii power cranks connect directly over Bluetooth or ANT+. Standard MPower Echelon2 consoles are ANT+ only and require a USB-C ANT+ dongle."
    ),
    CONCEPT2(
        id = "concept2",
        displayName = "Concept2 BikeErg / PM5",
        subtitle = "BLE GATT PM5 (1–6 ergs) + USB ANT+ FE-C Group Classes (10–30+ ergs with auto-priority)",
        broadcastType = "BLE GATT CE060030 (1–6 ergs) • USB ANT+ FE-C (30+ Group Classes)",
        supportedModels = "Concept2 BikeErg, RowErg, SkiErg with PM5 monitor",
        companyOrServiceUuid = "UUID CE060030 / ANT+ 0x11",
        setupTip = "For 1–6 ergs over Bluetooth, open Connect on the PM5 until it shows 'Ready for App Connection'. For 10–30+ ergs, use PM5 Group Classes (ANT+) with a USB-C ANT+ dongle."
    ),
    FTMS_UNIVERSAL(
        id = "ftms",
        displayName = "Wattbike & FTMS Smart Bikes",
        subtitle = "BLE GATT FTMS Indoor Bike Data (0x1826 -> 0x2AD2) + USB ANT+ FE-C/Power",
        broadcastType = "BLE GATT FTMS 0x1826 (1–6 bikes) • USB ANT+ FE-C/Power (30+)",
        supportedModels = "Wattbike Air-Pro/Atom, Life Fitness IC5–IC8, Technogym, Matrix, Body Bike",
        companyOrServiceUuid = "UUID 0x1826 (0x2AD2) / ANT+ 0x11 & 0x0B",
        setupTip = "Start pedaling to wake the console. Tiles show the last 5 digits of the bike's serial (e.g. AIR-PRO 02379) for quick identification."
    ),
    POWER_METERS(
        id = "power_meters",
        displayName = "Standard Power Pedals & Cranks",
        subtitle = "BLE GATT Cycling Power (0x1818 -> 0x2A63) + USB ANT+ Bicycle Power (0x0B)",
        broadcastType = "BLE GATT 0x1818 (1–6 meters) • USB ANT+ Power (30+ meters)",
        supportedModels = "Favero Assioma, Garmin Rally, Quarq, SRM, PowerTap",
        companyOrServiceUuid = "UUID 0x1818 (0x2A63) / ANT+ 0x0B",
        setupTip = "Spin the pedals or cranks a few revolutions to wake the power meter. Connects automatically via Bluetooth (1–6) or USB ANT+ (30+)."
    );

    companion object {
        fun fromId(id: String): BikeProtocol {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: KEISER
        }
    }
}
