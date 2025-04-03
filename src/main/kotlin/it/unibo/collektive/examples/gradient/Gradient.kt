package it.unibo.collektive.examples.gradient

import it.unibo.alchemist.collektive.device.DistanceSensor
import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables
import it.unibo.collektive.stdlib.spreading.distanceTo

/**
 * The entrypoint of the simulation running a gradient.
 */
fun Aggregate<Int>.gradientEntrypoint(
    environment: EnvironmentVariables,
    distanceSensor: DistanceSensor,
): Double = distanceTo(environment["source"]) {
    val dist = with(distanceSensor) { distances() }
    dist
}
