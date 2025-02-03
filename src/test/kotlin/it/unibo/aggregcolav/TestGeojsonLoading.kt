package it.unibo.aggregcolav

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.data2viz.geojson.JacksonGeoJsonObject
import io.data2viz.geojson.jackson.*
import java.awt.geom.Path2D
import java.awt.geom.Point2D
import java.io.File
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

const val EARTH_RADIUS = 6371000L

fun LngLatAlt.toCartesian() = Point2D.Double(
    EARTH_RADIUS * cos(this.latitude) * cos(longitude),
    EARTH_RADIUS * cos(this.latitude) * sin(longitude),
)

fun MultiPolygon.asListOfPoligon(): List<Polygon> {
    if(this.coordinates.size > 1)
        throw IllegalStateException("More than one element in this array has no sense for this experiment")
    return this.coordinates.first().map {
        Polygon(it)
    }
}

fun LngLatAlt.insideOf(polygon: Polygon): Boolean {
    if(polygon.coordinates.size > 1)
        throw IllegalStateException("More than one element in this array has no sense for this experiment")
    val polCoords = polygon.coordinates.first()

    if (polCoords.contains(this)) // Point of Polygon consider to be inside
        return true

    var i = 0
    var j = polCoords.size - 1
    var result = false

    while (i < polCoords.size) {
        if (polCoords[i].latitude > this.latitude != polCoords[j].latitude > this.latitude &&
            this.longitude < (polCoords[j].longitude - polCoords[i].longitude) * (this.latitude - polCoords[i].latitude) /
            (polCoords[j].latitude - polCoords[i].latitude) + polCoords[i].longitude
        ) result = !result
        j = i++
    }
    return result
}

class IsNavigableFor(private val point: LngLatAlt) : GeoJsonObjectVisitor<Boolean> {

    override fun visit(geoJsonObject: Feature): Boolean {
        return if (geoJsonObject.geometry != null) {
            geoJsonObject.geometry!!.accept(this)
        } else false
    }

    override fun visit(geoJsonObject: FeatureCollection): Boolean = geoJsonObject.getFeatures().map {
        it.accept(this)
    }.all { it }

    override fun visit(geoJsonObject: GeometryCollection): Boolean = TODO("Not yet implemented")

    override fun visit(geoJsonObject: LineString): Boolean = TODO("Not yet implemented")

    override fun visit(geoJsonObject: MultiLineString): Boolean = TODO("Not yet implemented")

    override fun visit(geoJsonObject: MultiPoint): Boolean = TODO("Not yet implemented")

    override fun visit(geoJsonObject: MultiPolygon): Boolean = geoJsonObject.asListOfPoligon().map {
       it.accept(this)
   }.all { it }

    override fun visit(geoJsonObject: Point): Boolean = TODO("Not yet implemented")

    override fun visit(geoJsonObject: Polygon): Boolean = !point.insideOf(geoJsonObject)

}

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
                geojsonObject.accept(IsNavigableFor(it))
            }
        }
        navigablePoints.forEach {
            assertTrue {
                geojsonObject.accept(IsNavigableFor(it))
            }
        }

        // Known limit of the representation: outside the polygon in the geojson the point is regognised as navigable,
        // even if it's not.
        // For the simulation purpose this should be ok, but consider this in general.
        // This should be FALSE:
        assertTrue { geojsonObject.accept(IsNavigableFor(LngLatAlt(9.956511, 54.346068))) }

    }
}
