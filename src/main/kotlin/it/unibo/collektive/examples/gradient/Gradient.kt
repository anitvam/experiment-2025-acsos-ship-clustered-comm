package it.unibo.collektive.examples.gradient

import com.fasterxml.jackson.databind.module.SimpleModule
import it.unibo.alchemist.collektive.device.CollektiveDevice
import it.unibo.alchemist.collektive.device.DistanceSensor
import it.unibo.alchemist.model.GeoPosition
import it.unibo.alchemist.model.maps.positions.LatLongPosition
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables
import it.unibo.collektive.stdlib.consensus.boundedElection
import it.unibo.collektive.stdlib.spreading.distanceTo
import kotlin.Double.Companion.POSITIVE_INFINITY

/**
 * The entrypoint of the simulation running a gradient.
 */
fun Aggregate<Int>.gradientEntrypoint(
    environment: CollektiveDevice<GeoPosition>,
    distanceSensor: DistanceSensor,
): Double {
    val groundStation: Boolean = environment["station"]
    val metric = with(environment) { distances() }
    val myLeader = boundedElection(
        strength = if (groundStation) POSITIVE_INFINITY else 0.0,
        bound = 5000.0,
        metric = metric,
    )
    return myLeader.toDouble()
}
