package it.unibo.alchemist.boundary.swingui.effect.impl

import it.unibo.alchemist.boundary.ui.api.Wormhole2D
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.GeoPosition
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Position2D
import it.unibo.alchemist.model.maps.maps.environments.NavigationEnvironment
import it.unibo.util.geojson.DrawPoligonVisitor
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D


@Suppress("DEPRECATION")
class DrawShoreline:  AbstractDrawOnce() {

    override fun getColorSummary(): Color = Color.BLUE

    override fun <T : Any?, P : Position2D<P>> draw(
        graphics2D: Graphics2D,
        node: Node<T>,
        environment: Environment<T, P>,
        wormhole: Wormhole2D<P>,
    ) {
        val geoJsonObject = (environment as NavigationEnvironment).getGeoJsonObject()
        graphics2D.color = colorSummary
        graphics2D.stroke = BasicStroke(2f)
        geoJsonObject.accept(DrawPoligonVisitor(graphics2D, wormhole as Wormhole2D<GeoPosition>))
    }

}