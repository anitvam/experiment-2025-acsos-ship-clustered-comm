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
class ReductionFactor @JvmOverloads constructor(
    aggregatorNames: List<String> = listOf("mean", "median"),
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
    ): Map<Node<T>, Double> {
        /*
         * Marti: raggruppa i nodi per leaader, così trovi i cluster
         * scarta i cluster per cui il leader è sorgente (valgono 1, trasmettono tutto)
         * Somma tutti gli intraClusterDR del cluster (intra totale), mettendo il massimo a 3mbit per ciascuno (se ho 100mb comunque ne mando 3)
         * Ottieni così quanto trasmetteresti se il leader del cluster fosse una stazione
         * poi prendi il nodo leader, e prendi il suo intracluster.
         * fai la divisione fra la somma di prima (sum(min(3mbit, intradatarate)) diviso intracluster
         * fallo per tutti i cluster
         * per ogni nodo, esponi il valore del cluster
         */
        // find clusters by grouping nodes by leader
        val clusters = environment.nodes.groupBy {
            it.getConcentration(leader).toInt()
        }.mapKeys {
            environment.getNodeByID(it.key)
        }
        return clusters.flatMap { (leader: Node<T>, others: List<Node<T>>) ->
            // Compute the ratio for each cluster
            val clusterRatio: Double = when {
                leader.contains(station) -> 1.0 // If the leader is a station, the reduction factor is 1
                else -> {
                    val optimalTransmission = others.asSequence()
                        .map { it.getConcentration(intraClusterDR) }
                        .map { it.toDouble() } // kbps
                        .map { it.coerceAtMost(3000.0) } // 3mbit
                        .sum()
                    val maxInterCluster = leader.getConcentration(intraClusterDR).toDouble()
                    minOf(maxInterCluster / optimalTransmission, 1.0)
                }
            }
            // Assign each member of the cluster the cluster ratio
            val clusterMembers: Sequence<Node<T>> = others.asSequence() + sequenceOf(leader)
            clusterMembers.map { it to clusterRatio }
        }.toMap()
    }

    companion object {
        val station = SimpleMolecule("station")
        val leader = SimpleMolecule("myLeader")
        val intraClusterDR = SimpleMolecule("export-intra-cluster-relay-data-rate-not-leader")
        val interClusterDR = SimpleMolecule("leader-to-relay-data-rate")
    }
}