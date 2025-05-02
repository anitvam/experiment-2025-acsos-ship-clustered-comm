package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.boundary.exportfilters.CommonFilters
import it.unibo.alchemist.model.Actionable
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Time
import it.unibo.util.interClusterDR
import it.unibo.util.intraClusterDR
import it.unibo.util.leader
import it.unibo.util.station
import it.unibo.util.toDouble
import it.unibo.util.toInt
import kotlin.String
import kotlin.collections.List
import kotlin.collections.component1
import kotlin.collections.component2

/**
 * Reduction factor := sum(intra-cluster-data-rate)/sum(inter-cluster-data-rate)
 *
 */
class ReductionFactor @JvmOverloads constructor(
    aggregatorNames: List<String> = listOf("mean", "median", "StandardDeviation"),
    precision: Int? = 3,
) : AbstractAggregatingDoubleExporter(
    CommonFilters.ONLYFINITE.filteringPolicy,
    aggregatorNames,
    precision,
) {

    override val columnName: String = "reduction-factor"

    override fun <T> getData(
        environment: Environment<T, *>,
        reaction: Actionable<T>?,
        time: Time,
        step: Long
    ): Map<Node<T>, Double> = clusterRatios(environment)

    companion object {
    }
}

fun <T> clusterRatios(environment: Environment<T, *>): Map<Node<T>, Double> {
    // find clusters by grouping nodes by leader
    val clusters = environment.nodes
        .groupBy { it.getConcentration(leader) }
        .filterKeys { it is Number } // filter out nodes non participating in the clustering (5g towers)
        .mapKeys { environment.getNodeByID(it.key.toInt()) }
    return clusters.flatMap { (leader: Node<T>, members: List<Node<T>>) ->
        // Compute the ratio for each cluster
        val clusterRatio: Double = when {
            members.size == 1 -> Double.NaN
            leader.contains(station) -> 1.0 // If the leader is a station, the reduction factor is 1
            else -> {
                val optimalTransmission = members.asSequence()
                    .filter { it != leader }
                    .map { it.getConcentration(intraClusterDR) }
                    .map { it.toDouble() } // kbps
                    .filter { it.isFinite() }
                    .map { it.coerceAtMost(3000.0) } // 3mbit
                    .sum()
                val maxInterCluster = leader.getConcentration(interClusterDR).toDouble()
                minOf(maxInterCluster / optimalTransmission, 1.0)
            }
        }
        // Assign each member of the cluster the cluster ratio
        members.asSequence().map { it to clusterRatio }
    }.toMap()
}
