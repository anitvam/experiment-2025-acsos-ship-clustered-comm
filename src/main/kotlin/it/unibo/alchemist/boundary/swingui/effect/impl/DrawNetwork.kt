package it.unibo.alchemist.boundary.swingui.effect.impl

import it.unibo.alchemist.boundary.swingui.effect.api.Effect
import it.unibo.alchemist.boundary.ui.api.Wormhole2D
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Position2D
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.util.*
import java.awt.*
import java.awt.geom.AffineTransform
import kotlin.math.atan2
import kotlin.math.roundToInt


@Suppress("DEPRECATION")
class DrawNetwork: Effect {
    override fun getColorSummary(): Color = Color.RED

    override fun <T : Any?, P : Position2D<P>> apply(
        graphics: Graphics2D,
        node: Node<T>,
        environment: Environment<T, P>,
        wormhole: Wormhole2D<P>
    ) {
        super.apply(graphics, node, environment, wormhole)
        val is5GAntenna = node.getConcentration(SimpleMolecule("5gAntenna")).toBoolean()
        val isStation = node.getConcentration(SimpleMolecule("station")).toBoolean()
        if (!isStation && !is5GAntenna) {
            val imLeader = node.getConcentration(iAmLeader).toBoolean()
            val imRelay = node.getConcentration(iAmRelay).toBoolean()
            val myLeader = node.getConcentration(leader).toInt()
            val myRelay = node.getConcentration(relay).toInt()
            val intraClusterRelay = node.getConcentration(intraClusterRelay).toInt()
            val myId = node.id
            val myPosition = wormhole.getViewPoint(environment.getPosition(node))
Double.NaN
            // Line configuration
            graphics.color = colorSummary
            graphics.stroke = BasicStroke(1.0f)

            if (myLeader != myId) {
                // I am not the leader of the cluster.
                if (intraClusterRelay != myLeader) {
                    // I have an intermediate to reach the leader.
                    val intermediate = environment.getNodeByID(intraClusterRelay)
                    val intermediatePosition = wormhole.getViewPoint(environment.getPosition(intermediate))
                    graphics.drawLine(myPosition.x, myPosition.y, intermediatePosition.x, intermediatePosition.y)
                    Arrow().drawArrowHead(graphics, myPosition, intermediatePosition)

                } else {
                    // I directly communicate with the leader.
                    val leader = environment.getNodeByID(myLeader)
                    val leaderPosition = wormhole.getViewPoint(environment.getPosition(leader))
                    graphics.drawLine(myPosition.x, myPosition.y, leaderPosition.x, leaderPosition.y)
                    Arrow().drawArrowHead(graphics, myPosition, leaderPosition)

                }
            } else if (imLeader && myRelay != myId) {
                val relayCandidate = environment.getNodeByID(myRelay)
                if (relayCandidate.getConcentration(SimpleMolecule("imRelay")).toBoolean()) {
                    val relayPosition = wormhole.getViewPoint(environment.getPosition(relayCandidate))
                    graphics.drawLine(myPosition.x, myPosition.y, relayPosition.x, relayPosition.y)
                    Arrow().drawArrowHead(graphics,myPosition, relayPosition)
                }
            }
        }
    }
}
class Arrow(size: Int = 5) {
    private val arrowHead = Polygon()

    init {
        // create a triangle centered on (0,0) and pointing right
        arrowHead.addPoint(size, 0)
        arrowHead.addPoint(-size, -size)
        arrowHead.addPoint(-size, size)
        //arrowHead.addPoint (0, 0); // Another style
    }

    fun drawArrowHead(g: Graphics2D, from: Point, to: Point) {
        val midpoint = midpoint(from, to)
        val tx = AffineTransform.getTranslateInstance(midpoint.x.toDouble(), midpoint.y.toDouble())
        tx.rotate(atan2((to.y - from.y).toDouble(), (to.x - from.x).toDouble()))
        g.fill(tx.createTransformedShape(arrowHead))
    }
    private fun midpoint(p1: Point, p2: Point): Point = Point(
        ((p1.x + p2.x) / 2.0).roundToInt(),
        ((p1.y + p2.y) / 2.0).roundToInt(),
    )
}