package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.boundary.exportfilters.CommonFilters
import it.unibo.alchemist.model.Actionable
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Time
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.util.toDouble
import kotlin.String
import kotlin.collections.List

/**
 * Reduction factor := sum(intra-cluster-data-rate)/sum(inter-cluster-data-rate)
 *
 */
class ReductionFactor @JvmOverloads constructor(
    aggregatorNames: List<String> = listOf("mean"),
    precision: Int? = null,
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
    ): Map<Node<T>, Double> =
        environment.nodes.associateWith { it.contents[intraClusterDR].toDouble() / it.contents[interClusterDR].toDouble() }

    companion object {
        val intraClusterDR = SimpleMolecule("intra-cluster-relay-data-rate-not-leader")
        val interClusterDR = SimpleMolecule("leader-to-relay-data-rate")
    }
}