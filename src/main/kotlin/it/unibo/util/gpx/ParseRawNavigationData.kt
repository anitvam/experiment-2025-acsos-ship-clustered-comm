package it.unibo.util.gpx

import dk.dma.ais.message.AisMessage
import it.unibo.util.ais.AisDecoder
import it.unibo.util.ais.AisPayload
import java.io.File
import java.time.Instant
import java.util.regex.Pattern

object ParseRawNavigationData {
    fun dataToParse(inputFolder: String, day: String): List<File> = File(inputFolder).listFiles { file ->
        Pattern.compile("$day.+").matcher(file.name).matches()
    }.ifEmpty {
        throw IllegalStateException("No suitable file found")
    }.toList()

    fun parse(inputFolder: String, outputFolder: String, day: String) {
        // Boat 235818393 to delete
        val aisMessages: MutableMap<Instant, AisMessage> = mutableMapOf()
        dataToParse(inputFolder, day).forEach {
            aisMessages.putAll(AisDecoder.parseFile(it))
        }
        val aisPayloads = AisPayload.from(aisMessages)
            .filterNot { it.boatId == 235818393 } // This boat is located in Russia somehow. Deleting from dataset.
        println("Writing to: $outputFolder")
        GpxFormatter.createGpxFileFromAisData(aisPayloads, File(outputFolder))
    }

    @JvmStatic
    fun main(vararg args: String) {
        val inputFolder = args[0]
        val outputFolder = args[1]
        val day = args[2]
        println("Parsing raw data from: $inputFolder")
        ParseRawNavigationData.parse(inputFolder, outputFolder, day)
    }
}

