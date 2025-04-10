package it.unibo.collektive.examples.gradient

import it.unibo.alchemist.collektive.device.CollektiveDevice
import it.unibo.alchemist.model.GeoPosition
import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.aggregate.api.neighboring
import it.unibo.collektive.field.operations.minWithId
import it.unibo.collektive.stdlib.consensus.boundedElection
import it.unibo.collektive.stdlib.spreading.distanceTo
import kotlin.Double.Companion.POSITIVE_INFINITY


private typealias RelayInfo<ID> = Pair<ID, Double>
private fun <ID> RelayInfo<ID>.relayId(): ID = first
private val RelayInfo<*>.distanceToLeader: Double get() = second


/**
 * The entrypoint of the simulation running a gradient.
 */
fun Aggregate<Int>.gradientEntrypoint(
    environment: CollektiveDevice<GeoPosition>
): Double {
    fun <T> T.inject(name: String) = also { environment[name] = this }
    val groundStation: Boolean = environment["source"]
    val metric = with(environment) { distances() } // TODO: convert this to data rate
    val distanceToShore: Double = distanceTo(groundStation, metric = metric).inject("distanceToShore")
    val myLeader = boundedElection(
        strength = -distanceToShore,
        bound = 5000.0,
        metric = metric,
    ).inject("myLeader")
    val distanceToLeader = distanceTo(localId == myLeader, metric = metric).inject("distanceToLeader")
    val potentialRelays = neighboring(myLeader).map { it != myLeader }.inject("potentialRelays")
    val myRelay = potentialRelays.alignedMap(neighboring(distanceToLeader)) { canRelay, distance ->
        when {
            canRelay -> distance
            else -> POSITIVE_INFINITY
        }
    }.minWithId(localId to POSITIVE_INFINITY, compareBy { it.distanceToLeader }).relayId().inject("myRelay")
    return metric[myRelay]
}
