package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.boundary.exportfilters.CommonFilters
import it.unibo.alchemist.model.Actionable
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Molecule
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Time
import it.unibo.util.toBoolean
import it.unibo.util.toDouble
import it.unibo.util.toInt

class DataRates(
    val mode: Mode,
): AbstractAggregatingDoubleExporter(
    CommonFilters.ONLYFINITE.filteringPolicy,
    listOf("mean"),
    3,
) {

    private var lastRound = -1L
    var previous = emptyMap<Any, Double>()

    fun <T> getRatios(environment: Environment<T, *>, round: Long): Map<Node<T>, Double> = if (lastRound == round) {
        @Suppress("UNCHECKED_CAST")
        previous as Map<Node<T>, Double>
    } else {
        clusterRatios(environment).also {
            lastRound = round
            @Suppress("UNCHECKED_CAST")
            previous = it as Map<Any, Double>
        }
    }

    constructor(mode: String) : this(Mode.fromString(mode))

    override val columnName: String = mode.toString()

    override fun <T> getData(
        environment: Environment<T, *>,
        reaction: Actionable<T>?,
        time: Time,
        step: Long
    ): Map<Node<T>, Double> = environment.nodes.associateWith { subject ->
        if (subject.getConcentration(station).toBoolean() || subject.getConcentration(fiveG).toBoolean()) {
            return@associateWith Double.NaN
        }
        when (mode) {
            Mode.b1dr -> subject.getConcentration(baseline1DR).toDouble()
            Mode.b2dr -> nonAggregatingDataRate(environment, subject, baseline2Parent, baseline2DataRates)
            Mode.b3dr -> nonAggregatingDataRate(environment, subject, baseline3Parent, baseline3DataRates)
            Mode.bcdr -> {
                val reductionFactors = getRatios(environment, step)
                val leader = environment.getNodeByID(subject.getConcentration(leader).toInt())
                clusteringDataRate(environment, reductionFactors, leader, 3000.0)
            }
        }
    }
}

tailrec fun <T> nonAggregatingDataRate(
    environment: Environment<T,*>, subject: Node<T>, getLeader: Molecule, dataRate: Molecule, visited: Set<Int> = emptySet()
): Double {
    return when {
        subject.getConcentration(station).toBoolean() -> 3000.0
        subject.getConcentration(dataRate).toDouble() >= 3000 -> {
            val next = subject.getConcentration(leader).toInt()
            when {
                next in visited -> 0.0
                else -> nonAggregatingDataRate(
                    environment,
                    environment.getNodeByID(next),
                    getLeader,
                    dataRate,
                    visited + next,
                )
            }
        }
        else -> 0.0
    }
}

fun <T> clusteringDataRate(environment: Environment<T,*>, reductionFactors: Map<Node<T>, Double>, subject: Node<T>, dataRate: Double, visited: Set<Int> = emptySet()): Double {
    return when{
        subject.getConcentration(station).toBoolean() -> dataRate
        subject.id in visited -> 0.0
        subject.getConcentration(iAmLeader).toBoolean() -> {
            val reducedDataRate = dataRate * reductionFactors.getValue(subject)
            val relay = environment.getNodeByID(subject.getConcentration(relay).toInt())
            when {
                reducedDataRate == 0.0 -> 0.0
                relay == subject -> 0.0
                else -> clusteringDataRate<T>(
                    environment,
                    reductionFactors,
                    relay,
                    reducedDataRate,
                    visited + relay.id
                )
            }
        }
        else -> {
            val myLeader = subject.getConcentration(leader).toInt()
            when {
                myLeader == subject.id -> 0.0
                else -> clusteringDataRate(
                    environment,
                    reductionFactors,
                    environment.getNodeByID(myLeader),
                    dataRate,
                    visited + myLeader,
                )
            }
        }
    }
}

enum class Mode {
    b1dr,
    b2dr,
    b3dr,
    bcdr;

    companion object {
        fun fromString(value: String): Mode {
            return when (value) {
                "b1-dr" -> b1dr
                "b2-dr" -> b2dr
                "b3-dr" -> b3dr
                "bc-dr" -> bcdr
                else -> throw IllegalArgumentException("Unknown mode: $value")
            }
        }
    }
}