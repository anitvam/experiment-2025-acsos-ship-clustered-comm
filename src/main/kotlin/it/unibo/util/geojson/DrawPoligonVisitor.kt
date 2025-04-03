package it.unibo.util.geojson

import io.data2viz.geojson.jackson.*
import it.unibo.alchemist.boundary.ui.api.Wormhole2D
import it.unibo.alchemist.model.GeoPosition
import java.awt.Graphics2D

class DrawPoligonVisitor(
    val graphics2D: Graphics2D,
    val wormhole2D: Wormhole2D<GeoPosition>,
): GeoJsonObjectVisitor<Unit> {

    override fun visit(geoJsonObject: Point) {
        val viewP = wormhole2D.getViewPoint(geoJsonObject.coordinates.toLatLongPosition());
        graphics2D.fillOval(viewP.x - 2, viewP.y - 2, 4, 4)
    }

    override fun visit(geoJsonObject: LineString) {
        val coordinates = geoJsonObject.coordinates
        for (i in 0 until coordinates.size - 1) {
            val viewP1 = wormhole2D.getViewPoint(coordinates[i].toLatLongPosition());
            val viewP2 = wormhole2D.getViewPoint(coordinates[i+1].toLatLongPosition());

            graphics2D.drawLine(viewP1.x, viewP1.y, viewP2.x, viewP2.y)
        }
    }

    override fun visit(geoJsonObject: Polygon) {
        val coordinates = geoJsonObject.coordinates.first()
        val n = coordinates.size
        val xPoints = IntArray(n)
        val yPoints = IntArray(n)

        for (i in 0 until n) {
            val viewP = wormhole2D.getViewPoint(coordinates[i].toLatLongPosition());
            xPoints[i] = viewP.x
            yPoints[i] = viewP.y
        }

        graphics2D.drawPolygon(xPoints, yPoints, n)
    }

    override fun visit(geoJsonObject: MultiPoint) {
        geoJsonObject.accept(this)
    }

    override fun visit(geoJsonObject: MultiLineString) {
        geoJsonObject.accept(this)
    }

    override fun visit(geoJsonObject: MultiPolygon) {
        geoJsonObject.asListOfPoligon().map {
            it.accept(this)
        }
    }

    override fun visit(geoJsonObject: GeometryCollection) {
        geoJsonObject.getGeometries().map {
            it.accept(this)
        }
    }

    override fun visit(geoJsonObject: FeatureCollection) {
        geoJsonObject.getFeatures().map {
            it.accept(this)
        }
    }

    override fun visit(geoJsonObject: Feature) {
        if (geoJsonObject.geometry != null) {
            geoJsonObject.geometry!!.accept(this)
        }
    }
}