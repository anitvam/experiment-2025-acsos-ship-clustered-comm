package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.boundary.exportfilters.CommonFilters
import it.unibo.alchemist.model.Actionable
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Time
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.clustered.seaborn.comm.Metric.megaBitsPerSecond
import it.unibo.util.toDouble
import it.unibo.util.toInt
import kotlin.String
import kotlin.collections.List
import kotlin.collections.get

/**
 * Reduction factor := sum(intra-cluster-data-rate)/sum(inter-cluster-data-rate)
 *
 */
class ClusterSize @JvmOverloads constructor(
    aggregatorNames: List<String> = listOf("mean", "median", "StandardDeviation"),
    precision: Int? = null,
) : AbstractAggregatingDoubleExporter(
    CommonFilters.ONLYFINITE.filteringPolicy,
    aggregatorNames,
    precision,
) {

    override val columnName: String = "cluster-size"

    init {
        check(aggregatorNames.isNotEmpty())
    }

    override fun <T> getData(
        environment: Environment<T, *>,
        reaction: Actionable<T>?,
        time: Time,
        step: Long
    ): Map<Node<T>, Double> {
        val clusters = environment.nodes
            .groupBy { it.getConcentration(leader) }
            .filterKeys { it is Number } // filter out nodes non participating in the clustering (5g towers)
            .mapKeys { environment.getNodeByID(it.key.toInt()) }
        return clusters.mapValues { it.value.size.toDouble() }
    }

    companion object {
        val station = SimpleMolecule("station")
        val leader = SimpleMolecule("myLeader")
        val intraClusterDR = SimpleMolecule("export-intra-cluster-relay-data-rate-not-leader")
        val interClusterDR = SimpleMolecule("leader-to-relay-data-rate")
    }
}