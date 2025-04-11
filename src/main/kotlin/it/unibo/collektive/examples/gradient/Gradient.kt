package it.unibo.collektive.examples.gradient

import it.unibo.alchemist.collektive.device.CollektiveDevice
import it.unibo.alchemist.model.GeoPosition
import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.aggregate.api.neighboring
import it.unibo.collektive.field.Field
import it.unibo.collektive.field.operations.any
import it.unibo.collektive.field.operations.minWithId
import it.unibo.collektive.stdlib.consensus.boundedElection
import it.unibo.collektive.stdlib.spreading.distanceTo
import javax.xml.crypto.Data
import kotlin.Double.Companion.POSITIVE_INFINITY
import kotlin.math.exp
import kotlin.math.ln


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

@JvmInline
value class Distance(val meters: Double) {
    val kilometers: Double get() = meters / 1000.0

    override fun toString(): String = when {
        meters > 1000 -> "${kilometers}km"
        else -> "${meters}m"
    }
}
val Double.meters get() = Distance(this)
val Double.kilometers get() = Distance(this * 1e3)
val Int.meters get() = toDouble().meters
val Int.kilometers get() = toDouble().kilometers


@JvmInline
value class DataRate(val kiloBitsPerSecond: Double) {
    val megaBitsPerSecond: Double get() = kiloBitsPerSecond / 1000.0
    val gigaBitsPerSecond: Double get() = megaBitsPerSecond / 1000.0

    val timeToTransmitOneKb: Double
        get() = 1.0 / kiloBitsPerSecond

    override fun toString(): String = when {
        gigaBitsPerSecond > 1 -> "${gigaBitsPerSecond}Gbps"
        megaBitsPerSecond > 1 -> "${megaBitsPerSecond}Mbps"
        else -> "${kiloBitsPerSecond}Kbps"
    }
}

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
        10.meters to 10.megaBitsPerSecond,
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
        5.5.kilometers to 1.bitsPerSecond,
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
        60.kilometers to 1.bitsPerSecond,
    )
)

fun interface ConnectionTechnology {
    operator fun invoke (distance: Distance): DataRate

    companion object {
        fun byInterpolation(
            dataRates: Map<Distance, DataRate>,
        ): ConnectionTechnology = object : ConnectionTechnology {
            val interpolated = interpolateLogLinear(dataRates)
            override fun invoke(distance: Distance): DataRate = interpolated(distance)
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