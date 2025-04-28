package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.model.Actionable
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.GeoPosition
import it.unibo.alchemist.model.Time
import it.unibo.alchemist.model.molecules.SimpleMolecule

class NumberOfClusters(
    override val columnNames: List<String> = listOf("n_clusters"),
) : AbstractDoubleExporter() {
    override fun <T> extractData(
        environment: Environment<T, *>,
        reaction: Actionable<T>?,
        time: Time,
        step: Long
    ): Map<String, Double> = mapOf(
        columnNames.first() to environment.nodes.map { it.contents[myLeader] }.toSet().count().toDouble()
    )

    companion object {
        val myLeader = SimpleMolecule("myLeader")
    }
}