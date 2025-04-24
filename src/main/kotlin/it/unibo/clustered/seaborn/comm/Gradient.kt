package it.unibo.clustered.seaborn.comm

import it.unibo.alchemist.collektive.device.CollektiveDevice
import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.aggregate.api.neighboring
import it.unibo.collektive.field.Field
import it.unibo.collektive.field.operations.anyWithSelf
import it.unibo.collektive.field.operations.minWithId
import it.unibo.collektive.stdlib.consensus.Candidacy
import it.unibo.collektive.stdlib.consensus.boundedElection
import it.unibo.collektive.stdlib.spreading.distanceTo
import kotlin.Double.Companion.POSITIVE_INFINITY
import it.unibo.clustered.seaborn.comm.Metric.aprs
import it.unibo.clustered.seaborn.comm.Metric.bitsPerSecond
import it.unibo.clustered.seaborn.comm.Metric.disconnected
import it.unibo.clustered.seaborn.comm.Metric.gigaBitsPerSecond
import it.unibo.clustered.seaborn.comm.Metric.loopBack
import it.unibo.clustered.seaborn.comm.Metric.lora
import it.unibo.clustered.seaborn.comm.Metric.megaBitsPerSecond
import it.unibo.clustered.seaborn.comm.Metric.midband5G
import it.unibo.clustered.seaborn.comm.Metric.wifi
import it.unibo.collektive.aggregate.api.exchange
import it.unibo.collektive.aggregate.api.mapNeighborhood
import it.unibo.collektive.field.Field.Companion.foldWithId
import it.unibo.collektive.field.operations.any
import it.unibo.collektive.field.operations.max
import it.unibo.collektive.field.operations.minBy
import it.unibo.collektive.stdlib.doubles.FieldedDoubles.plus
import it.unibo.collektive.stdlib.spreading.bellmanFordGradientCast

private typealias RelayInfo<ID> = Pair<ID, Double>
private fun <ID> RelayInfo<ID>.relayId(): ID = first
private val RelayInfo<*>.distanceToLeader: Double get() = second

/**
 * Baseline 3: Bellman Ford shortest path using the lowest data rate as metric
 */
fun Aggregate<Int>.dataRateBellmanFord(
    environment: CollektiveDevice<*>
): Any? {
    val groundStation: Boolean = environment.isDefined("station")
    return bellmanFordGradientCast(
        groundStation,
        local = 0.0,
        accumulateData = { source: Double, dest: Double, value: Double ->
            // sommerei il tempo richiesto a far transitare il payload da source a dest
            0.0
        },
        metric = computeDataRates(
            environment,
            with(environment) { distances() }.map { it.meters }
        ).map {
            it.timeToTransmitOneKb
        }
    )

}

/**
 * Baseline 2: Bellman Ford shortest path using the distance between each node as a metric.
 * Maybe here we can ignore the 5g probability.
 */
//fun Aggregate<Int>.baseline2Dist(
//    environment: CollektiveDevice<*>,
//    groundStation: Boolean,
//    dataRates: Field<Int, DataRate>,
//): DataRate {
//    val groundStation: Boolean = environment.isDefined("station")
//    val dataRateDirectToStation = neighboring(groundStation).alignedMap(dataRates) { station, dataRate ->
//        when {
//            station -> dataRate
//            else -> disconnected
//        }
//    }.max(disconnected)
//    val myDistance = bellmanFordGradientCast<Int, DataRate, Distance>(
//        groundStation,
//        local = if (groundStation) loopBack else disconnected,
//        bottom = 0.meters,
//        top = POSITIVE_INFINITY.meters,
//        accumulateData = { source: Distance, dest: Distance, value: DataRate ->
//            when {
//                dataRateDirectToStation > disconnected -> dataRateDirectToStation
//
//            }
//        },
//        accumulateDistance = Distance::plus,
//        metric = with(environment) { distances() }.map { it.meters }
//    )
//    return neighboring(myDistance).alignedMap( dataRates) { distance, dataRate ->
//        distance to dataRate
//    }.minBy(POSITIVE_INFINITY.meters to 0.bitsPerSecond ) { it.first }.second
//}

/**
 * The entrypoint of the simulation running a gradient.
 */
fun Aggregate<Int>.clusteredBellmanFord(
    environment: CollektiveDevice<*>
): Any? {
    // Streaming data rate
    val payloadSize = environment.get<Double>("payloadSize")
    val streamingBitRate = payloadSize.megaBitsPerSecond

    fun <T> T.inject(name: String) = also { environment[name] = this }
    val groundStation: Boolean = environment.isDefined("station")
    val distances: Field<Int, Distance> = with(environment) { distances() }.map { it.meters }
        .inject("distance")
    val is5gAntenna: Field<Int, Boolean> = neighboring(environment.isDefined("5gAntenna"))
    val thisIsA5gAntenna = is5gAntenna.localValue
    // Once data rates have been established, 5g towers are cut off the computation,
    // they just relay the communication
    if (!thisIsA5gAntenna) { //
        val dataRates = computeDataRates(environment, distances)
        dataRates.max(base = disconnected).inject("max-data-rate")
        val timeToTransmit = dataRates.map { it.timeToTransmitOneMb }.inject("metric")

        val timeToStation: Double = distanceTo(

        // Baseline 1: direct communication with the station only
        val stationsNearby = neighboring(groundStation)
        val baseline1 = stationsNearby
            .alignedMap(dataRates) { isStation, dataRate -> dataRate.takeIf { isStation } ?: disconnected }
        val baseline1MaxData = baseline1.max(base = disconnected).inject("baseline1-data-rate")

        // Baseline 2: min distance data rate
        val distanceToStation = distanceTo(groundStation, bottom = 0.meters, top = POSITIVE_INFINITY.meters, metric = distances, accumulateDistance = Distance::plus)
        val neighborDistances = neighboring(distanceToStation)
        val distanceThroughRelay = distances.alignedMap(neighborDistances, Distance::plus)
        val baseline2parent = distanceThroughRelay.foldWithId(localId to POSITIVE_INFINITY.meters) { current, id, distance ->
            when {
                current.second > distance -> id to distance
                else -> current
            }
        }.first
        val children = neighboring(baseline2parent).foldWithId(mutableSetOf<Int>()) { accumulator, id, neighborParentId ->
            accumulator.also { if (neighborParentId == localId) it += id }
        }.inject("baseline2-children")
        val childrenCount = children.size.inject("baseline2-children-count")
        val baseline2parentDataRate: DataRate = when {
            baseline2parent == localId -> 0.bitsPerSecond
            else -> dataRates[baseline2parent]
        }.inject("baseline2-parent-clean-data-rate")
        exchange(baseline2parentDataRate) { xcDataRates ->
            val actualUpload = dataRates.alignedMapWithId(xcDataRates) { id, idealDR, actualDR ->
                when {
                    stationsNearby[id] -> idealDR
                    else -> actualDR
                }
            }[baseline2parent]
            val sharedUpload = (actualUpload - streamingBitRate) / childrenCount.toDouble()
            mapNeighborhood { id ->
                when {
                    id == localId -> actualUpload
                    id in children -> sharedUpload
                    else -> 0.bitsPerSecond
                }
            }
        }.inject("baseline2-data-rate")

            groundStation,
            metric = timeToTransmit,
            isRiemannianManifold = false,
        ).inject("timeToStation")

        // Baseline 3: min time to station
        val distanceToStationDataRate3 = distanceTo(
            groundStation,
            metric = timeToTransmit,
            isRiemannianManifold = false,
        ).inject("distanceToStationDataRate3")

        // Clustered
        val myLeader: Int = boundedElection(
            strength = -timeToStation,
            bound = streamingBitRate.timeToTransmitOneMb,
            metric = timeToTransmit,
            selectBest = { c1, c2 ->
                maxOf(c1, c2, compareBy<Candidacy<Int, Double, Double>>{ it.strength }.thenBy { it.candidate })
            }
        ).inject("myLeader")
        //dataRates[myLeader].inject("data-rate-towards-leader") // Why myLeader has a key which does not exist in map?
        val imLeader = myLeader == localId
        imLeader.inject("imLeader")
        val distanceToLeader = distanceTo(
            imLeader,
            metric = timeToTransmit,
            isRiemannianManifold = false,
        ).inject("distanceToLeader")
        val idOfIntraClusterRelay = neighboring(distanceToLeader)
            .alignedMap(timeToTransmit, Double::plus)
            .mapWithId { id, time -> id to time }
            .minBy(localId to POSITIVE_INFINITY) { it.second}
            .first
        val intraClusterDataRate = dataRates[idOfIntraClusterRelay].inject("intra-cluster-relay-data-rate")
        (intraClusterDataRate.takeUnless { imLeader } ?: Double.NaN).inject("intra-cluster-relay-data-rate-not-leader")
        val timesToStationAround = neighboring(timeToStation).inject("timesToStationAround")
        val localTimeToStation = timesToStationAround.localValue
        val potentialRelays = neighboring(myLeader)
            .alignedMap(timesToStationAround) { leader, distance ->
                leader != myLeader && distance < localTimeToStation
            }.inject("potentialRelays")
        val myRelay = potentialRelays.alignedMap(timesToStationAround + timeToTransmit) { canRelay, distance ->
            when {
                canRelay -> distance
                else -> POSITIVE_INFINITY
            }
        }.minWithId(localId to POSITIVE_INFINITY, compareBy { it.distanceToLeader }).relayId().inject("myRelay")
        val imRelay = neighboring(myRelay).map { it == localId }.any(false).inject("imRelay")
        val upstreamToRelay = dataRates[myRelay]
        val iHaveARelay = imLeader && myRelay != localId
        environment["leader-to-relay-data-rate"] = when {
            iHaveARelay -> upstreamToRelay.kiloBitsPerSecond
            else -> Double.NaN
        }
        return timeToTransmit[myRelay]
    }
    return Unit
}


fun Aggregate<Int>.computeDataRates(
    environment: CollektiveDevice<*>,
    distances: Field<Int, Distance>,
): Field<Int, DataRate> {
    fun <T> T.inject(name: String) = also { environment[name] = this }
    val is5gAntenna: Field<Int, Boolean> = neighboring(environment.isDefined("5gAntenna"))
    val thisIsA5gAntenna = is5gAntenna.localValue
    val dataRate5g = distances.alignedMapWithId(is5gAntenna) { id, distance, neighborIs5g ->
        when {
            id == localId -> loopBack
            neighborIs5g && thisIsA5gAntenna -> 10.gigaBitsPerSecond // 5g towers with fiber backhaul
            neighborIs5g || thisIsA5gAntenna -> midband5G(distance)
            else -> disconnected
        }
    }.inject("5g data rates").max(base = disconnected).inject("5g data rate")

    val has5gAntenna = evolve(
        when (val probability = environment.get<Any?>("5gProbability")) {
            is Number -> environment.randomGenerator.nextDouble() < probability.toDouble()
            else -> false
        }
    ) { it }.inject("has5gAntenna")
    val antennasAround = neighboring(has5gAntenna).anyWithSelf { it }
    return distances.mapWithId { id, distance ->
        when {
            id == localId -> loopBack
            else -> {
                val classicDataRates = maxOf(lora(distance), wifi(distance), aprs(distance), dataRate5g)
                when {
                    antennasAround -> {
                        val shipToShip5G = maxOf(dataRate5g, midband5G(distance))
                        maxOf(shipToShip5G, classicDataRates)
                    }
                    else -> classicDataRates
                }
            }
        }
    }.inject("dataRates")
}

fun main() {
    (0..50).map { it.kilometers to "APRS: ${aprs(it.kilometers)}, LoRa: ${lora(it.kilometers)}" }
        .forEach(::println)
}