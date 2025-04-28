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
    aggregatorNames: List<String> = listOf("mean", "median", "StandardDeviation"),
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
        }.toMap().also {
            val mapped = it.mapKeys { it.key.id }
            println(mapped)
        }
    }

    companion object {
        val station = SimpleMolecule("station")
        val leader = SimpleMolecule("myLeader")
        val intraClusterDR = SimpleMolecule("export-intra-cluster-relay-data-rate-not-leader")
        val interClusterDR = SimpleMolecule("leader-to-relay-data-rate")
    }
}