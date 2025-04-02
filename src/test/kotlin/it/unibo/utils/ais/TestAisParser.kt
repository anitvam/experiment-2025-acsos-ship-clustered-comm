package it.unibo.utils.ais

import it.unibo.util.ais.AisCustomMessageParser
import it.unibo.util.ais.AisDecoder
import it.unibo.util.ais.AisPayload
import it.unibo.util.gpx.GpxFormatter
import org.junit.jupiter.api.assertDoesNotThrow
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TestAisParser {

    val dataToParse = File(object {}.javaClass.classLoader.getResource("ais-sample/20250506-130000.nmea.txt")?.toURI()
        ?: throw IllegalArgumentException("Missing sample file in project resources"))

    @Test
    fun testParseFile() {
        val aisMessage = AisDecoder.parseFile(dataToParse)
        assertTrue(aisMessage.isNotEmpty())
        println(aisMessage)
    }

    @Test
    fun testConversionToAisPayload() {
        val aisMessage = AisDecoder.parseFile(dataToParse)
        assertTrue(aisMessage.isNotEmpty())
        println(aisMessage)
        val aisPayloads = AisPayload.from(aisMessage)
        println(aisPayloads)
    }

    @Test
    fun testDeriveNumberOfBoats(){
        val aisMessage = AisDecoder.parseFile(dataToParse)
        assertTrue(aisMessage.isNotEmpty())
        val aisPayloads = AisPayload.from(aisMessage)
        val groupedByBoat = aisPayloads.groupBy { it.boatId }
        println(groupedByBoat.count())
        println(groupedByBoat)
    }

    @Test
    fun testGpxGenerationFromAisData() {
        val aisMessage = AisDecoder.parseFile(dataToParse)
        assertTrue(aisMessage.isNotEmpty())
        val xmlPath = File(
            (object {}.javaClass.classLoader.getResource("ais-sample/gpx-generated")?.toURI())
                ?: throw IllegalArgumentException("Missing sample directory in project resources")
        )
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
}
