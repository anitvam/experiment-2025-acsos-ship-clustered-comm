package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.model.Actionable
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Time
import it.unibo.alchemist.model.molecules.SimpleMolecule

/**
 * Reduction factor := sum(intra-cluster-data-rate)/sum(inter-cluster-data-rate)
 *
 */
class ReductionFactor(
    override val columnNames: List<String> = listOf("reduction-factor")
) : AbstractDoubleExporter() {
    override fun <T> extractData(
        environment: Environment<T, *>,
        reaction: Actionable<T>?,
        time: Time,
        step: Long
    ): Map<String, Double> {
        val intraDR = environment.nodes.map { it.contents[intraClusterDR] }.map { it.toDouble() }.fold(0.0, Double::plus)
        val interDR = environment.nodes.map { it.contents[interClusterDR] }.map { it.toDouble() }.fold(0.0, Double::plus)
        return mapOf(columnNames.first() to (intraDR / interDR))
    }

    companion object {
        val intraClusterDR = SimpleMolecule("intra-cluster-relay-data-rate-not-leader")
        val interClusterDR = SimpleMolecule("leader-to-relay-data-rate")
        private fun Any?.toDouble(): Double = when (this) {
            is Double -> this
            is Number -> this.toDouble()
            null -> error("Unexpected value: $this")
            Unit -> error("Unexpected value: $this")
            else -> error("Unexpected value: $this")
        }
    }
}