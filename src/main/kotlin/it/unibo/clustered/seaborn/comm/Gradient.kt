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
import it.unibo.util.toDouble

private typealias RelayInfo<ID> = Pair<ID, Double>
private fun <ID> RelayInfo<ID>.relayId(): ID = first
private val RelayInfo<*>.distanceToLeader: Double get() = second

fun <T> T.inject(
    environment: CollektiveDevice<*>,
    name: String
) = also {
    environment[name] = this
    environment["export-$name"] = this.toDouble()
}

fun Aggregate<Int>.entrypoint(
    environment: CollektiveDevice<*>
): Any? {
    // Streaming data rate
    val payloadSize = environment.get<Double>("payloadSize")
    val streamingBitRate = payloadSize.megaBitsPerSecond

    val groundStation: Boolean = environment.isDefined("station")
    val distances: Field<Int, Distance> = with(environment) { distances() }.map { it.meters }
        .inject(environment, "distance")
    val is5gAntenna: Field<Int, Boolean> = neighboring(environment.isDefined("5gAntenna"))
    val thisIsA5gAntenna = is5gAntenna.localValue
    val dataRates: Field<Int, DataRate> = computeDataRates(environment, distances)
    // Once data rates have been established, 5g towers are cut off the computation,
    // they just relay the communication
    if (!thisIsA5gAntenna) { //
        dataRates.max(base = disconnected).inject(environment, "max-data-rate")
        val timeToTransmit = dataRates.map { it.timeToTransmitOneMb }.inject(environment, "metric")

        // Baseline 1: direct communication with the station only
        val stationsNearby: Field<Int, Boolean> = neighboring(groundStation)
        val baseline1 = stationsNearby
            .alignedMap(dataRates) { isStation, dataRate -> dataRate.takeIf { isStation } ?: disconnected }
        val baseline1MaxData = baseline1.max(base = disconnected).takeIf { it >= 3.megaBitsPerSecond } ?: disconnected
        baseline1MaxData.max3Mbit().inject(environment, "baseline1-data-rate")

        // Baseline 2: min distance data rate
        val baseline2DistanceToStation = distanceTo(
            groundStation,
            bottom = 0.meters,
            top = POSITIVE_INFINITY.meters,
            metric = distances,
            accumulateDistance = Distance::plus
        ).inject(environment, "baseline2-distanceToStation")
        val neighborDistances = neighboring(baseline2DistanceToStation)
        val distanceThroughRelay = distances.alignedMap(neighborDistances, Distance::plus)
        val baseline2Parent = distanceThroughRelay.foldWithId(localId to POSITIVE_INFINITY.meters) { current, id, distance ->
            when {
                current.second > distance -> id to distance
                else -> current
            }
        }.first.inject(environment, "baseline2-parent")
        dataRates[baseline2Parent].inject(environment, "baseline2-parent-data-rate")
        computeNonCooperativeDataRate("baseline2", streamingBitRate, environment, baseline2Parent, stationsNearby, dataRates)

        // Baseline 3: min time to station
        val baseline3TimeToStation = distanceTo(
            groundStation,
            metric = timeToTransmit,
            isRiemannianManifold = false,
        ).inject(environment, "baseline3-timeToStation")

        val baseline3neighborTimes = neighboring(baseline3TimeToStation)
        val baseline3TimeToStationThroughRelays = timeToTransmit.alignedMap(baseline3neighborTimes, Double::plus)
        val baseline3Parent = baseline3TimeToStationThroughRelays.foldWithId(localId to POSITIVE_INFINITY) { current, id, time ->
            when {
                current.second > time -> id to time
                else -> current
            }
        }.first.inject(environment, "baseline3-parent")
        dataRates[baseline3Parent].inject(environment, "baseline3-parent-data-rate")
        computeNonCooperativeDataRate("baseline3", streamingBitRate, environment, baseline3Parent, stationsNearby, dataRates)

        // Clustered
        val clusteredTimeToStation: Double = distanceTo(
            groundStation,
            metric = timeToTransmit,
            isRiemannianManifold = false,
        ).inject(environment, "clustered-timeToStation")

        // Clustered
        val myLeader: Int = boundedElection(
            strength = -clusteredTimeToStation,
            bound = streamingBitRate.timeToTransmitOneMb,
            metric = timeToTransmit,
            selectBest = { c1, c2 ->
                maxOf(c1, c2, compareBy<Candidacy<Int, Double, Double>>{ it.strength }.thenBy { it.candidate })
            }
        ).inject(environment, "myLeader")
        val imLeader = myLeader == localId
        imLeader.inject(environment, "imLeader")
//        val distanceToLeader = distanceTo(
//            imLeader,
//            metric = timeToTransmit,
//            isRiemannianManifold = false,
//        ).inject(environment, "distanceToLeader")
        val distanceToLeader = alignedOn(myLeader) {
            distanceTo(
                imLeader,
                metric = timeToTransmit,
                isRiemannianManifold = false,
            ).inject(environment, "distanceToLeader")
        }
        val idOfIntraClusterRelay = neighboring(distanceToLeader)
            .alignedMap(timeToTransmit, Double::plus)
            .alignedMap(neighboring(myLeader)) { distance, leader ->
                when (leader) {
                    myLeader -> distance
                    else -> POSITIVE_INFINITY
                }
            }
            .mapWithId { id, time -> id to time }
            .minBy(localId to POSITIVE_INFINITY) { it.second}
            .first
            .inject(environment, "intra-cluster-relay")
        val intraClusterDataRate = dataRates[idOfIntraClusterRelay].inject(environment, "intra-cluster-relay-data-rate")
        val intraClusterDataRateNoLeaders = (intraClusterDataRate.takeUnless { imLeader } ?: Double.NaN).inject(environment, "intra-cluster-relay-data-rate-not-leader")


        val timesToStationAround = neighboring(clusteredTimeToStation).inject(environment, "timesToStationAround")
        val localTimeToStation = timesToStationAround.localValue
        val potentialRelays = neighboring(myLeader)
            .alignedMap(timesToStationAround) { leader, distance ->
                leader != myLeader && distance < localTimeToStation
            }.inject(environment, "potentialRelays")
        val myRelay = potentialRelays.alignedMap(timesToStationAround + timeToTransmit) { canRelay, distance ->
            when {
                canRelay -> distance
                else -> POSITIVE_INFINITY
            }
        }.minWithId(localId to POSITIVE_INFINITY, compareBy { it.distanceToLeader }).relayId().inject(environment, "myRelay")
        val imRelay = neighboring(myRelay).map { it == localId }.any(false).inject(environment, "imRelay")
        val upstreamToRelay = dataRates[myRelay]
        val iHaveARelay = imLeader && myRelay != localId
        environment["leader-to-relay-data-rate"] = when {
            iHaveARelay -> upstreamToRelay.kiloBitsPerSecond
            else -> Double.NaN
        }
        // Exported info
        // val myLeaderMolecule = SimpleMolecule("myLeader")
        // environment.environment.nodes.count { it.contents[myLeaderMolecule] == myLeader }.inject(environment, "cluster-size")


        return timeToTransmit[myRelay]
    }

    return Unit
}

fun Aggregate<Int>.computeNonCooperativeDataRate(
    experimentName: String,
    streamingBitRate: DataRate,
    environment: CollektiveDevice<*>,
    parent: Int,
    stationsNearby: Field<Int, Boolean>,
    dataRates: Field<Int, DataRate>,
) {
    val children = neighboring(parent).foldWithId(mutableSetOf<Int>()) { accumulator, id, neighborParentId ->
        accumulator.also { if (neighborParentId == localId) it += id }
    }.inject(environment, "$experimentName-children")
    val childrenCount = children.size.inject(environment, "$experimentName-children-count")
    val parentDataRate: DataRate = when {
        parent == localId -> 0.bitsPerSecond
        else -> dataRates[parent]
    }.inject(environment, "$experimentName-parent-clean-data-rate")
    exchange(parentDataRate) { xcDataRates ->
        val actualUpload: DataRate = dataRates.alignedMapWithId(xcDataRates) { id, idealDR, actualDR ->
            when {
                stationsNearby[id] -> idealDR
                else -> actualDR
            }
        }[parent]
        val sharedUpload: DataRate = (actualUpload - streamingBitRate) / childrenCount.toDouble()
        mapNeighborhood { id ->
            when (id) {
                localId -> actualUpload
                in children -> sharedUpload
                else -> 0.bitsPerSecond
            }
        }
    }.localValue.inject(environment, "$experimentName-data-rate")
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

fun Double.max3Mbit(): Double = coerceAtMost(3000.0)

fun DataRate.max3Mbit(): Double = kiloBitsPerSecond.coerceAtMost(3000.0)

fun main() {
    (0..50).map { it.kilometers to "APRS: ${aprs(it.kilometers)}, LoRa: ${lora(it.kilometers)}" }
        .forEach(::println)
}
