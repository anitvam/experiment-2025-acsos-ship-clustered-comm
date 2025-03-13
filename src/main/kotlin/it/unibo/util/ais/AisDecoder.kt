package it.unibo.util.ais

import dk.dma.ais.message.AisMessage
import java.io.File
import java.time.Instant

object AisDecoder {
    fun parseRaw(payload: String, date: String):  Map<Instant, AisMessage> {
        val aisMessageBuilder = AisCustomMessageParser()
        val payloadDecoded: MutableMap<Instant, AisMessage> = mutableMapOf()
        payload.lines().forEach {
            if (it.startsWith("!DATE-TIME")) {
                // Timestamp - NOT STANDARD AIS NMEA 0183
                val time = it.substringAfter("!DATE-TIME,").trim()
                //println("${date}T${time}Z")
                val currentTimestamp = Instant.parse("${date}T${time}Z")
                if (aisMessageBuilder.isComplete()) {
                    val aisMessage = aisMessageBuilder.build()
                    if (aisMessage != null) payloadDecoded[currentTimestamp] = aisMessage
                }
            } else if (it != "") {
                // Payload
                aisMessageBuilder.parseLine(it)
                //println(it)
            }
        }
        return payloadDecoded
    }

    fun parseFromPath(path: String): Map<Instant, AisMessage> {
        val file = File(path)
        val dateLong = path.substringAfterLast("/").substringBefore("-")
        val year = dateLong.take(4)
        val month = dateLong.drop(4).take(2)
        val day = dateLong.takeLast(2)
        val date = "${year}-${month}-${day}"
        return parseRaw(file.readText(Charsets.UTF_8), date)
    }
}
