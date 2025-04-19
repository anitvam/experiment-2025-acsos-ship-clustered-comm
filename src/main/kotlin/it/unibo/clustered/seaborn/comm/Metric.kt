package it.unibo.clustered.seaborn.comm

import it.unibo.clustered.seaborn.comm.Metric.disconnected
import java.io.File
import kotlin.Double.Companion.POSITIVE_INFINITY

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
