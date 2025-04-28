package it.unibo.alchemist.boundary.swingui.effect.impl

import it.unibo.alchemist.boundary.swingui.effect.api.Effect
import it.unibo.alchemist.boundary.ui.api.Wormhole2D
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Position2D
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.util.toBoolean
import it.unibo.util.toInt
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D

@Suppress("DEPRECATION")
class DrawNetwork: Effect {
    override fun getColorSummary(): Color = Color.ORANGE

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
            val imLeader = node.getConcentration(SimpleMolecule("imLeader")).toBoolean()
            val imRelay = node.getConcentration(SimpleMolecule("imRelay")).toBoolean()
            val myLeader = node.getConcentration(SimpleMolecule("myLeader")).toInt()
            val myRelay = node.getConcentration(SimpleMolecule("myRelay")).toInt()
            val myId = node.id
            val myPosition = wormhole.getViewPoint(environment.getPosition(node))
            if (myLeader != myId) {
                // I have a leader.
                graphics.color = colorSummary
                graphics.stroke = BasicStroke(2.0f)
                val leader = environment.getNodeByID(myLeader)
                val leaderPosition = wormhole.getViewPoint(environment.getPosition(leader))
                graphics.drawLine(myPosition.x, myPosition.y, leaderPosition.x, leaderPosition.y)
                // Warning: the communication can be not direct towards leader.
            } else if (imLeader && myRelay != myId) {
                val relayCandidate = environment.getNodeByID(myRelay)
                if (relayCandidate.getConcentration(SimpleMolecule("imRelay")).toBoolean()) {
                    val relayPosition = wormhole.getViewPoint(environment.getPosition(relayCandidate))
                    graphics.color = colorSummary
                    graphics.stroke = BasicStroke(2.0f)
                    graphics.drawLine(myPosition.x, myPosition.y, relayPosition.x, relayPosition.y)
                }
            }
        }
    }
}