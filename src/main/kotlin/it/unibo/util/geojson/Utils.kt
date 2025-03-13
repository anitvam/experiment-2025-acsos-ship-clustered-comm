package it.unibo.util.geojson

import io.data2viz.geojson.jackson.LngLatAlt
import io.data2viz.geojson.jackson.MultiPolygon
import io.data2viz.geojson.jackson.Polygon
import it.unibo.alchemist.model.maps.positions.LatLongPosition
import java.awt.geom.Point2D
import kotlin.math.cos
import kotlin.math.sin

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

fun LatLongPosition.toLngLatAlt() = LngLatAlt(longitude, latitude)

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
