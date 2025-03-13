package it.unibo.utils

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.data2viz.geojson.JacksonGeoJsonObject
import io.data2viz.geojson.jackson.*
import it.unibo.util.geojson.IsNavigableVisitor
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestGeojsonLoading {

    val navigablePoints = listOf(
        LngLatAlt( 10.164589, 54.331647),
        LngLatAlt(10.234147, 54.427487),
        LngLatAlt(10.152995, 54.333769),
    )

    val nonNavigablePoints = listOf(
        LngLatAlt(10.168856, 54.326575),
        LngLatAlt(10.129859, 54.343357),
        LngLatAlt(10.162890, 54.422513),
    )

    @Test
    fun loadGeojson() {
        val geojsonFile = File("/home/anitvam/work/experiments/aggreg_colav/src/main/resources/maps/coast_only.geojson")
        //println(geojsonFile.bufferedReader().readLines())
        val customMapper = ObjectMapper()
            .registerKotlinModule()
            .findAndRegisterModules()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        val geojsonObject = customMapper.readValue(geojsonFile, JacksonGeoJsonObject::class.java)

        nonNavigablePoints.forEach {
            assertFalse {
                geojsonObject.accept(IsNavigableVisitor(it))
            }
        }
        navigablePoints.forEach {
            assertTrue {
                geojsonObject.accept(IsNavigableVisitor(it))
            }
        }

        // Known limit of the representation: outside the polygon in the geojson the point is regognised as navigable,
        // even if it's not.
        // For the simulation purpose this should be ok, but consider this in general.
        // This should be FALSE:
        assertTrue { geojsonObject.accept(IsNavigableVisitor(LngLatAlt(9.956511, 54.346068))) }

    }
}
