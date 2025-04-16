package it.unibo.clustered.seaborn.comm

import it.unibo.alchemist.collektive.device.CollektiveDevice
import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.aggregate.api.neighboring
import it.unibo.collektive.field.Field
import it.unibo.collektive.field.operations.any
import it.unibo.collektive.field.operations.anyWithSelf
import it.unibo.collektive.field.operations.max
import it.unibo.collektive.field.operations.minWithId
import it.unibo.collektive.stdlib.consensus.Candidacy
import it.unibo.collektive.stdlib.consensus.boundedElection
import it.unibo.collektive.stdlib.doubles.FieldedDoubles.plus
import it.unibo.collektive.stdlib.spreading.distanceTo
import kotlin.Double.Companion.POSITIVE_INFINITY
import kotlin.math.exp
import kotlin.math.ln


private typealias RelayInfo<ID> = Pair<ID, Double>
private fun <ID> RelayInfo<ID>.relayId(): ID = first
private val RelayInfo<*>.distanceToLeader: Double get() = second

/**
 * The entrypoint of the simulation running a gradient.
 */
fun Aggregate<Int>.entrypoint(
    environment: CollektiveDevice<*>
): Any? {
    fun <T> T.inject(name: String) = also { environment[name] = this }
    val groundStation: Boolean = environment.isDefined("station")
    val distances: Field<Int, Distance> = with(environment) { distances() }.map { it.meters }
        .inject("distance")
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
    // Once data rates have been established, 5g towers are cut off the computation,
    // they just relay the communication
    if (!thisIsA5gAntenna) { //
        val has5gAntenna = evolve(
            when (val probability = environment.get<Any?>("5gProbability")) {
                is Number -> environment.randomGenerator.nextDouble() < probability.toDouble()
                else -> false
            }
        ) { it }.inject("has5gAntenna")
        val antennasAround = neighboring(has5gAntenna).anyWithSelf { it }
        val dataRates = distances.mapWithId { id, distance ->
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
        }.inject("data rates")
        dataRates.max(base = disconnected).inject("max data rate")
        val timeToTransmit = dataRates.map { it.timeToTransmitOneMb }.inject("metric")
        val streamingBitRate = 3.megaBitsPerSecond
        val timeToStation: Double = distanceTo(
            groundStation,
            metric = timeToTransmit,
            isRiemannianManifold = false,
        ).inject("timeToStation")
        val myLeader: Int = boundedElection(
            strength = -timeToStation,
            bound = streamingBitRate.timeToTransmitOneMb,
            metric = timeToTransmit,
            selectBest = { c1, c2 ->
                maxOf(c1, c2, compareBy<Candidacy<Int, Double, Double>>{ it.strength }.thenBy { it.candidate })
            }
        ).inject("myLeader")
        val imLeader = myLeader == localId
        imLeader.inject("imLeader")
        val distanceToLeader = distanceTo(
            imLeader,
            metric = timeToTransmit,
            isRiemannianManifold = false,
        ).inject("distanceToLeader")
        val timesToStationAround = neighboring(timeToStation).inject("timesToStationAround")
        val localTimeToStation = timesToStationAround.localValue
        val potentialRelays = neighboring(myLeader)
            .alignedMap<Double, Boolean>(timesToStationAround) { leader, distance ->
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
        environment["upstream-data-rate"] = when {
            iHaveARelay -> upstreamToRelay
            else -> 0.kiloBitsPerSecond
        }
        return timeToTransmit[myRelay]
    }
    return Unit
}

@JvmInline
value class Distance(val meters: Double)  : Comparable<Distance> {
    val kilometers: Double get() = meters / 1000.0

    override fun toString(): String = when {
        meters > 1000 -> "${kilometers.readable}km"
        else -> "${meters.readable}m"
    }

    override fun compareTo(other: Distance): Int = meters.compareTo(other.meters)
}
val Double.meters get() = Distance(this)
val Double.kilometers get() = Distance(this * 1e3)
val Int.meters get() = toDouble().meters
val Int.kilometers get() = toDouble().kilometers
val Double.readable get() = String.format("%.3f", this)

@JvmInline
value class DataRate(val kiloBitsPerSecond: Double) : Comparable<DataRate> {
    val megaBitsPerSecond: Double get() = kiloBitsPerSecond / 1000.0
    val gigaBitsPerSecond: Double get() = megaBitsPerSecond / 1000.0

    val timeToTransmitOneKb: Double get() = 1.0 / kiloBitsPerSecond

    val timeToTransmitOneMb get() = timeToTransmitOneKb * 1e3

    override fun toString(): String = when {
        kiloBitsPerSecond.isInfinite() -> "loopback"
        gigaBitsPerSecond > 1 -> "${gigaBitsPerSecond.readable}Gbps"
        megaBitsPerSecond > 1 -> "${megaBitsPerSecond.readable}Mbps"
        else -> "${kiloBitsPerSecond.readable}Kbps"
    }

    override fun compareTo(other: DataRate) = kiloBitsPerSecond.compareTo(other.kiloBitsPerSecond)
}

val disconnected = DataRate(0.0)
val loopBack = DataRate(POSITIVE_INFINITY)
val Double.kiloBitsPerSecond get() = DataRate(this)
val Double.megaBitsPerSecond get() = DataRate(this * 1e3)
val Double.gigaBitsPerSecond get() = DataRate(this * 1e6)
val Int.bitsPerSecond get() = DataRate(this * 1e-3)
val Int.kiloBitsPerSecond get() = toDouble().kiloBitsPerSecond
val Int.megaBitsPerSecond get() = toDouble().megaBitsPerSecond
val Int.gigaBitsPerSecond get() = toDouble().gigaBitsPerSecond

val lora = ConnectionTechnology.byInterpolation(
    mapOf(10.meters to 50.kiloBitsPerSecond, 15.kilometers to 300.bitsPerSecond, 16.kilometers to 1.bitsPerSecond)
)

val wifi = ConnectionTechnology.byInterpolation(
    mapOf(
        10.meters to 750.megaBitsPerSecond,
        20.meters to 600.megaBitsPerSecond,
        30.meters to 450.megaBitsPerSecond,
        40.meters to 350.megaBitsPerSecond,
        50.meters to 250.megaBitsPerSecond,
        60.meters to 180.megaBitsPerSecond,
        70.meters to 120.megaBitsPerSecond,
        80.meters to 80.megaBitsPerSecond,
        90.meters to 40.megaBitsPerSecond,
        120.meters to 1.bitsPerSecond,
    )
)

val midband5G = ConnectionTechnology.byInterpolation(
    mapOf(
        50.meters to 1.gigaBitsPerSecond,
        500.meters to 800.megaBitsPerSecond,
        1.kilometers to 600.megaBitsPerSecond,
        1.5.kilometers to 400.megaBitsPerSecond,
        2.kilometers to 250.megaBitsPerSecond,
        2.5.kilometers to 200.megaBitsPerSecond,
        3.kilometers to 150.megaBitsPerSecond,
        3.5.kilometers to 110.megaBitsPerSecond,
        4.kilometers to 50.megaBitsPerSecond,
        5.kilometers to 5.megaBitsPerSecond,
    )
)

val aprs = ConnectionTechnology.byInterpolation(
    mapOf(
        1.kilometers to 9600.bitsPerSecond,
        5.kilometers to 9000.bitsPerSecond,
        10.kilometers to 8500.bitsPerSecond,
        20.kilometers to 7000.bitsPerSecond,
        30.kilometers to 4000.bitsPerSecond,
        40.kilometers to 2000.bitsPerSecond,
        50.kilometers to 1000.bitsPerSecond,
    )
)

fun interface ConnectionTechnology {
    operator fun invoke (distance: Distance): DataRate

    companion object {
        fun byInterpolation(
            dataRates: Map<Distance, DataRate>,
            maxRange: Distance = dataRates.keys.max(),
        ): ConnectionTechnology = object : ConnectionTechnology {
            val interpolated = interpolateLogLinear(dataRates)
            override fun invoke(distance: Distance): DataRate = when {
                distance > maxRange -> disconnected
                else -> interpolated(distance)
            }
        }
    }
}

// Fit exponential function y = a * exp(-b * x)
fun fitExponential(dataRates: Map<Distance, DataRate>): (Distance) -> DataRate {
    // Solve:
    // y = a * exp(-b * x)
    // ln(y) = ln(a) - b * x  -> linear regression
    require(dataRates.values.all { it.kiloBitsPerSecond > 0 }) {
        "All data rates must be strictly positive (found: ${dataRates.filterValues { it.kiloBitsPerSecond <= 0 }})"
    }
    val distances = dataRates.keys
    val n = dataRates.size
    val lnY = dataRates.mapValues { (_, dataRate) -> ln(dataRate.kiloBitsPerSecond) }
    val sumX = distances.sumOf { it.meters }
    check(sumX.isFinite()) { "Sum of distances must be finite" }
    val sumLnY = lnY.values.sum()
    check(sumLnY.isFinite()) { "Sum of lnY must be finite" }
    val sumX2 = distances.sumOf { it.meters * it.meters }
    val sumXlnY = lnY.toList().sumOf { it.first.meters * it.second }
    val denominator = n * sumX2 - sumX * sumX
    check(denominator > 0) { "Denominator must be positive" }
    val b = (n * sumXlnY - sumX * sumLnY) / denominator
    check(b.isFinite()) { "B must be finite" }
    val lnA = (sumLnY - b * sumX) / n
    check(lnA.isFinite()) { "lnA must be finite" }
    val a = exp(lnA)
    check(a > 0) { "a must be positive" }
    return { d: Distance -> (a * exp(-b * d.meters)).kiloBitsPerSecond }
}

fun interpolateLogLinear(data: Map<Distance, DataRate>): (Distance) -> DataRate {
    val sorted = data.entries.sortedBy { it.key.meters }
    return { d: Distance ->
        val x = d.meters
        val (low, high) = sorted
            .zipWithNext()
            .firstOrNull { x in it.first.key.meters..it.second.key.meters }
            ?: if (x < sorted.first().key.meters) sorted.first() to sorted.first()
               else sorted.last() to sorted.last()

        val x0 = low.key.meters
        val x1 = high.key.meters
        val y0 = ln(low.value.kiloBitsPerSecond)
        val y1 = ln(high.value.kiloBitsPerSecond)

        val proportion = if (x1 != x0) (x - x0) / (x1 - x0) else 0.0
        val lnInterpolated = y0 + proportion * (y1 - y0)
        exp(lnInterpolated).kiloBitsPerSecond
    }
}

fun main() {
    (0..50).map { it.kilometers to "APRS: ${aprs(it.kilometers)}, LoRa: ${lora(it.kilometers)}" }
        .forEach(::println)
}