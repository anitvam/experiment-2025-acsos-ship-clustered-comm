package it.unibo.util.ais

import dk.dma.ais.message.AisMessage
import dk.dma.ais.message.IPositionMessage
import it.unibo.util.gpx.ParsingUtils
import java.time.Instant

data class AisPayload(
    val boatId: Int,
    val timestamp: Instant,
    val longitude: Double,
    val latitude: Double,
) {
    companion object {

        fun from(boatId: Int, timestamp: Instant, aisMessage: AisMessage): AisPayload? {
            return if (
                aisMessage is IPositionMessage &&
                ParsingUtils.validateLongitude(aisMessage.pos.longitudeDouble) &&
                ParsingUtils.validateLatitude(aisMessage.pos.latitudeDouble)
            ) {
                AisPayload(boatId, timestamp, aisMessage.pos.longitudeDouble, aisMessage.pos.latitudeDouble)
            } else null
        }
        fun from(map: Map<Instant, AisMessage>): List<AisPayload> =
            map.map { from(it.value.userId, it.key, it.value) }.filterNotNull()
    }
}

