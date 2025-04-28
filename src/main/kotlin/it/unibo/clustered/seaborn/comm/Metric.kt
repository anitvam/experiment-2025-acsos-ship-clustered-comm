package it.unibo.clustered.seaborn.comm

import it.unibo.clustered.seaborn.comm.Metric.disconnected
import it.unibo.clustered.seaborn.comm.Metric.kiloBitsPerSecond
import java.io.File
import kotlin.Double.Companion.POSITIVE_INFINITY
import kotlin.math.exp
import kotlin.math.ln

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

//fun interpolateLogLinear(data: Map<Distance, DataRate>): (Distance) -> DataRate {
//    val sorted = data.entries.sortedBy { it.key.meters }
//    return { d: Distance ->
//        val x = d.meters
//        val (low, high) = sorted
//            .zipWithNext()
//            .firstOrNull { x in it.first.key.meters..it.second.key.meters }
//            ?: if (x < sorted.first().key.meters) sorted.first() to sorted.first()
//            else sorted.last() to sorted.last()
//
//        val x0 = low.key.meters
//        val x1 = high.key.meters
//        val y0 = ln(low.value.kiloBitsPerSecond)
//        val y1 = ln(high.value.kiloBitsPerSecond)
//
//        val proportion = if (x1 != x0) (x - x0) / (x1 - x0) else 0.0
//        val lnInterpolated = y0 + proportion * (y1 - y0)
//        exp(lnInterpolated).kiloBitsPerSecond
//    }
//}

fun interpolateLogLinear(data: Map<Distance, DataRate>): (Distance) -> DataRate {
    val sorted = data.entries.sortedBy { it.key.meters }
    val xs = sorted.map { it.key.meters }.toDoubleArray()
    val ysLog = sorted.map { ln(it.value.kiloBitsPerSecond) }.toDoubleArray()
    return { d: Distance ->
        val x = d.meters
        val idx = xs.binarySearch(x).let { if (it >= 0) it else -(it + 1) }
        val i0 = when {
            idx == 0 -> 0
            idx >= xs.size -> xs.lastIndex
            else -> idx - 1
        }
        val i1 = when {
            idx == 0 -> 0
            idx >= xs.size -> xs.lastIndex
            else -> idx
        }
        val x0 = xs[i0]
        val x1 = xs[i1]
        val y0 = ysLog[i0]
        val y1 = ysLog[i1]
        val proportion = if (x1 != x0) (x - x0) / (x1 - x0) else 0.0
        val lnInterpolated = y0 + proportion * (y1 - y0)
        exp(lnInterpolated).kiloBitsPerSecond
    }
}

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

@JvmInline
value class Distance(val meters: Double)  : Comparable<Distance> {
    val kilometers: Double get() = meters / 1000.0

    override fun toString(): String = when {
        meters > 1000 -> "${kilometers.readable}km"
        else -> "${meters.readable}m"
    }

    operator fun plus(other: Distance): Distance = Distance(meters + other.meters)

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

    operator fun plus(other: DataRate): DataRate = DataRate(kiloBitsPerSecond + other.kiloBitsPerSecond)

    operator fun minus(other: DataRate): DataRate = DataRate((kiloBitsPerSecond - other.kiloBitsPerSecond)
        .coerceAtLeast(0.0))

    operator fun times(other: Double): DataRate = DataRate(kiloBitsPerSecond * other)

    operator fun div(other: Double): DataRate = DataRate(kiloBitsPerSecond / other)

    override fun compareTo(other: DataRate) = kiloBitsPerSecond.compareTo(other.kiloBitsPerSecond)
}

object Metric {
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

    fun exportMetricInCsv(fileName: String = "data/metric_data.csv") {
        val file = File(fileName)
        file.printWriter().use { out ->
            out.println("x,y_wifi,y_aprs,y_lora,y_midband5g")
            for (i in 1 until 60000) {
                out.println(
                    listOf(
                        i.meters.meters,
                        Metric.wifi.invoke(i.meters).megaBitsPerSecond,
                        Metric.aprs.invoke(i.meters).megaBitsPerSecond,
                        Metric.lora.invoke(i.meters).megaBitsPerSecond,
                        Metric.midband5G.invoke(i.meters).megaBitsPerSecond,
                    ).joinToString(",")
                )
            }
            println("CSV exported successfully.")
        }
    }
    @JvmStatic
    fun main(vararg args: String) {
        Metric.exportMetricInCsv(args.first())
    }
}
