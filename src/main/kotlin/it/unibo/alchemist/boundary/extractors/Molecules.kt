package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.model.molecules.SimpleMolecule

val fiveG = SimpleMolecule("5gAntenna")
val station = SimpleMolecule("station")
val leader = SimpleMolecule("myLeader")
val iAmLeader = SimpleMolecule("imLeader")
val relay = SimpleMolecule("myRelay")
val intraClusterDR = SimpleMolecule("export-intra-cluster-relay-data-rate-not-leader")
val interClusterDR = SimpleMolecule("leader-to-relay-data-rate")
val baseline1DR = SimpleMolecule("baseline1-data-rate")
val baseline2Parent = SimpleMolecule("baseline2-parent")
val baseline2DataRates = SimpleMolecule("baseline2-data-rate")
val baseline3Parent = SimpleMolecule("baseline3-parent")
val baseline3DataRates = SimpleMolecule("baseline3-data-rate")
