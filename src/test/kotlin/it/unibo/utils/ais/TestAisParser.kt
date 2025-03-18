package it.unibo.utils.ais

import dk.dma.ais.message.AisMessage
import it.unibo.util.ais.AisCustomMessageParser
import it.unibo.util.ais.AisDecoder
import it.unibo.util.ais.AisPayload
import it.unibo.util.gpx.GpxFormatter
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.assertDoesNotThrow
import java.io.File
import java.io.FileFilter
import java.io.FilenameFilter
import java.time.Instant
import java.util.regex.Pattern
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TestAisParser {

    @Test
    fun testParseFile() {
        val aisMessage = AisDecoder.parseFile(dataToParse.first())
        println(aisMessage)
    }

    @Test
    fun testConversionToAisPayload() {
        val aisMessage = AisDecoder.parseFile(dataToParse.first())
        println(aisMessage)
        val aisPayloads = AisPayload.from(aisMessage)
        println(aisPayloads)
    }

    @Test
    fun testDeriveNumberOfBoats(){
        val aisMessage = AisDecoder.parseFile(dataToParse.first())
        val aisPayloads = AisPayload.from(aisMessage)
        val groupedByBoat = aisPayloads.groupBy { it.boatId }
        println(groupedByBoat.count())
        println(groupedByBoat)
    }

    @Test
    fun testGpxGenerationFromAisData() {
        val aisMessage = AisDecoder.parseFile(dataToParse.first())
        val xmlPath = outputXmlPath
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
    fun parseDayData() {
        val aisMessages: MutableMap<Instant, AisMessage> = mutableMapOf()
        dataToParse.forEach {
            aisMessages.putAll(AisDecoder.parseFile(it))
        }
        val aisPayloads = AisPayload.from(aisMessages)
        GpxFormatter.createGpxFileFromAisData(aisPayloads, outputXmlPath)
    }

    companion object {

        val dataToParse: List<File> = File("/home/anitvam/work/experiments/202208").listFiles { file ->
            Pattern.compile("20220818.+").matcher(file.name).matches()
        }.ifEmpty { throw IllegalStateException("No suitable file found") }.toList()

        val outputXmlPath = File("/home/anitvam/work/experiments/aggreg_colav/src/main/resources/navigation-routes")

        @JvmStatic
        @BeforeAll
        fun checkFilesExist(): Unit {
            assertDoesNotThrow { dataToParse }
            assertNotNull(dataToParse)
        }
    }
}
