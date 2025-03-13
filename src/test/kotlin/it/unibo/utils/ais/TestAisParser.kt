package it.unibo.utils.ais

import dk.dma.ais.message.AisMessage
import it.unibo.collektive.stdlib.iterables.FieldedCollectionsExtensions.groupBy
import it.unibo.util.ais.AisCustomMessageParser
import it.unibo.util.ais.AisDecoder
import it.unibo.util.ais.AisPayload
import it.unibo.util.gpx.GpxFormatter
import java.io.File
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TestAisParser {

    fun gatherPath(): String {
        val aisPath = System.getenv("AIS_DATA_SAMPLE")
        println("AIS path: $aisPath")
        assertNotNull(aisPath, "AIS path not set, consider setting the environment variable AIS_DATA_SAMPLE")
        return aisPath
    }

    @Test
    fun testParseFile() {
        val aisMessage = AisDecoder.parseFromPath(gatherPath())
        println(aisMessage)
    }

    @Test
    fun testConversionToAisPayload() {
        val aisMessage = AisDecoder.parseFromPath(gatherPath())
        val aisPayloads = AisPayload.from(aisMessage)
        println(aisPayloads)
    }

    @Test
    fun testDeriveNumberOfBoats(){
        val aisMessage = AisDecoder.parseFromPath(gatherPath())
        val aisPayloads = AisPayload.from(aisMessage)
        val groupedByBoat = aisPayloads.groupBy { it.boatId }
        println(groupedByBoat.count())
        println(groupedByBoat)
    }

    @Test
    fun testGpxGenerationFromAisData() {
        val aisMessage = AisDecoder.parseFromPath(gatherPath())
        val xmlPath = System.getenv("OUTPUT_XML_TEST")
        val aisPayloads = AisPayload.from(aisMessage)
        val groupedByBoat = aisPayloads.groupBy { it.boatId }
        println(groupedByBoat.count())
        GpxFormatter.createGpxFileFromAisData(groupedByBoat.entries.first().value, xmlPath)
    }

    @Test
    fun testSingleLineParse() {
        val customMessageParser = AisCustomMessageParser()
        val example = "!AIVDM,1,1,,A,14eGrSPP00ncMJTO5C6aBwvP2D0?,0*7A"
        customMessageParser.parseLine(example)
        val output = customMessageParser.build()
        println(output)
        assertEquals(
            "[msgId=1, repeat=0, userId=316013198, cog=2379, navStatus=0, pos=(32592666,190245714) = (32592666,-78189742), posAcc=1, raim=1, specialManIndicator=0, rot=128, sog=0, spare=0, syncState=0, trueHeading=511, utcSec=16, slotTimeout=5, subMessage=15]",
            output.toString()
        )
    }

    @Test
    fun testParseFolder() {
        val aisMessagesFolder = System.getenv("AIS_DATA_FOLDER")
        val xmlPath = System.getenv("OUTPUT_XML_TEST")

        val aisMessages: MutableMap<Instant, AisMessage> = mutableMapOf()
        println(aisMessagesFolder)
        File(aisMessagesFolder).also{
            println(it)}.listFiles()?.forEach {
            aisMessages.putAll(AisDecoder.parseFromPath(it.path))
        }

        val aisPayloads = AisPayload.from(aisMessages)
        val groupedByBoat = aisPayloads.groupBy { it.boatId }

        println(groupedByBoat.count())
        val boat = groupedByBoat.keys.random()
        groupedByBoat[boat]?.let { GpxFormatter.createGpxFileFromAisData(it, xmlPath) }
    }
}
