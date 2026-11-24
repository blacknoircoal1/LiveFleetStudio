package com.keiser.studio.parser

import android.bluetooth.le.ScanResult
import com.keiser.studio.model.BikeData
import com.keiser.studio.model.BikeProtocol

/**
 * Universal interface for indoor bike packet decoders.
 */
interface BikePacketParser {
    val protocol: BikeProtocol

    /**
     * Attempts to parse a Bluetooth LE scan result into standardized BikeData.
     * Returns null if the packet does not match this brand's format.
     */
    fun parse(result: ScanResult): BikeData?
}
