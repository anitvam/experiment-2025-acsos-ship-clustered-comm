package it.unibo.alchemist.boundary.swingui.effect.impl

import io.data2viz.geojson.JacksonGeoJsonObject
import it.unibo.alchemist.boundary.ui.api.Wormhole2D
import it.unibo.alchemist.model.GeoPosition
import it.unibo.util.geojson.DrawPoligonVisitor
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D

object DrawGeoJSONInformation {
    fun draw(
        layersToRepresent: List<JacksonGeoJsonObject>,
        color: Color,
        graphics2D: Graphics2D,
        wormhole: Wormhole2D<GeoPosition>
    ) {
        graphics2D.color = color
        graphics2D.stroke = BasicStroke(2f)
        layersToRepresent.forEach { it.accept(DrawPoligonVisitor(graphics2D, wormhole)) }
    }
}