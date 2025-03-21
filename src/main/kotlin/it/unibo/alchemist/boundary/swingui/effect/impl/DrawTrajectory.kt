package it.unibo.alchemist.boundary.swingui.effect.impl

import it.unibo.alchemist.boundary.swingui.effect.api.Effect
import it.unibo.alchemist.boundary.ui.api.Wormhole2D
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.GeoPosition
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Position2D
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.util.geojson.toLatLongPosition
import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.geom.Line2D
import kotlin.math.max
import kotlin.math.min

@Suppress("DEPRECATION")
class DrawTrajectory: Effect {

    @Transient
    private var positionsMemory: Map<Int, List<Point>> = mapOf()

    @Transient
    private var lastDrawMemory: Map<Int, Int> = emptyMap()

    override fun getColorSummary(): Color = Color.BLACK

    override fun <T : Any?, P : Position2D<P>> apply(
        g: Graphics2D,
        node: Node<T>,
        environment: Environment<T, P>,
        wormhole: Wormhole2D<P>,
    ) {
        // Actual position
        val nodePosition: P = environment.getPosition(node)
        val viewPoint: Point = wormhole.getViewPoint(nodePosition)

        drawTrajectory(g, node)

        updateTrajectory(node, environment, viewPoint)
    }

    private fun <P : Position2D<P>> drawTrajectory(
        graphics2D: Graphics2D,
        node: Node<*>,
    ) {
        val positions = positionsMemory[node.id].orEmpty()
        val color = computeColorOrBlack(node)
        positions.takeLast(DEFAULT_SNAPSHOT_LENGTH).forEach {
            graphics2D.color = color
            graphics2D.fillOval(it.x - 2, it.y - 2, 4, 4)
        }
    }

    private fun <P : Position2D<P>, T> updateTrajectory(
        node: Node<T>,
        environment: Environment<T, P>,
        actualPosition: Point,
    ) {
        val positions = positionsMemory[node.id].orEmpty()
        val lastDraw = lastDrawMemory[node.id] ?: 0
        val roundedTime =
            environment.simulation.time
                .toDouble()
                .toInt()
        if (roundedTime >= lastDraw) {
            lastDrawMemory = lastDrawMemory + (node.id to lastDraw + DEFAULT_TIMESPAN)
            val updatedPositions =
                (positions + actualPosition)
            positionsMemory = positionsMemory +
                (node.id to updatedPositions)
        }
    }


    private fun computeColorOrBlack(
        node: Node<*>,
    ): Color =
        node
            .id
            .toFloat()
            .let {
                val hue = (it % MAX_COLOR) * 360f / MAX_COLOR  // Convert to hue (0-360 degrees)
                return Color.getHSBColor(hue.toFloat(), 1f, 1f) // Full saturation and brightness
            }

    companion object {
        private const val MAX_COLOR: Double = 255.0
        private const val DEFAULT_SNAPSHOT_LENGTH: Int = 140
        private const val ADJUST_ALPHA_FACTOR: Int = 4
        private const val DEFAULT_NODE_SIZE: Double = 1.0
        private const val DEFAULT_TIMESPAN: Int = 100


    }
}