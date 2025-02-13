package it.unibo.aggregcolav.geojson

import io.data2viz.geojson.jackson.*

class IsNavigableVisitor(private val targetPosition: LngLatAlt) : GeoJsonObjectVisitor<Boolean> {

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

    override fun visit(geoJsonObject: Polygon): Boolean = !targetPosition.insideOf(geoJsonObject)
}
