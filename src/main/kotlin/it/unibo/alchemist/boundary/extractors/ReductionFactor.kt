package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.boundary.exportfilters.CommonFilters
import it.unibo.alchemist.model.Actionable
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Time
import it.unibo.alchemist.model.molecules.SimpleMolecule
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
        environment.nodes.associateWith {
            when (val leaderId = it.getConcentration(leader).toInt()) {
                it.id -> 0.0
                else -> {
                    val interCluster = environment.getNodeByID(leaderId).getConcentration(interClusterDR).toDouble()
                    it.getConcentration(intraClusterDR).toDouble() / interCluster
                }
            }
        }
//            it.contents[intraClusterDR].toDouble() / it.contents[interClusterDR].toDouble() }

    companion object {
        val leader = SimpleMolecule("myLeader")
        val intraClusterDR = SimpleMolecule("export-intra-cluster-relay-data-rate-not-leader")
        val interClusterDR = SimpleMolecule("leader-to-relay-data-rate")
    }
}