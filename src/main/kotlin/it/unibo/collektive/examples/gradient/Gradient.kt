package it.unibo.collektive.examples.gradient

import it.unibo.alchemist.collektive.device.CollektiveDevice
import it.unibo.alchemist.model.GeoPosition
import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.aggregate.api.neighboring
import it.unibo.collektive.field.Field
import it.unibo.collektive.field.operations.any
import it.unibo.collektive.field.operations.minWithId
import it.unibo.collektive.stdlib.accumulation.convergeCast
import it.unibo.collektive.stdlib.accumulation.findParent
import it.unibo.collektive.stdlib.consensus.boundedElection
import it.unibo.collektive.stdlib.spreading.distanceTo
import kotlin.Double.Companion.POSITIVE_INFINITY
import kotlin.time.measureTime


private typealias RelayInfo<ID> = Pair<ID, Double>
private fun <ID> RelayInfo<ID>.relayId(): ID = first
private val RelayInfo<*>.distanceToLeader: Double get() = second


/**
 * The entrypoint of the simulation running a gradient.
 */
fun Aggregate<Int>.gradientEntrypoint(
    environment: CollektiveDevice<GeoPosition>
): Any? {
    fun <T> T.inject(name: String) = also { environment[name] = this }
    val groundStation: Boolean = environment["source"]
    val metric: Field<Int, Double> = with(environment) { distances() } // TODO: convert this to data rate
        .inject("metric")
//    val distance = distanceTo(groundStation, metric = metric).inject("toSource")
//    return convergeCast(
//        local = listOf(localId),
//        potential = distance,
//        accumulateData = { x, y -> x + y }
//    ).inject("ids").size
    val distanceToShore: Double = distanceTo(groundStation, metric = metric).inject("distanceToShore")
    val myLeader = boundedElection(
        strength = -distanceToShore, // TODO
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
    val imRelay = neighboring(myRelay).map { it == localId }.any(false).inject("imRelay")
    return metric[myRelay]
}

//fun main() {
//    repeat(1000) { i ->
//        val size = 1000 - i
//        val origin = generateSequence(0) { it + 1 }.take(size)
//        val list = origin.toList()
//        val set = origin.toSet()
//        val queries = generateSequence(0) { (it + 1) % (size * 2) }.take(10_000).shuffled()
//        fun Collection<*>.queries() = queries.forEach { it in this }
//        fun warmup() {
//            list.queries()
//            set.queries()
//        }
//        val listTime = measureTime { queries.forEach { it in list } }
//        val setTime = measureTime { queries.forEach { it in set } }
//        println("With $size elements, ${"List".takeIf { listTime < setTime } ?: "Set"} is preferable: List time: $listTime, Set time: $setTime")
//    }
//}