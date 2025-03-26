package it.unibo.alchemist.model.maps.actions

import it.unibo.alchemist.model.Action
import it.unibo.alchemist.model.Context
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Reaction
import it.unibo.alchemist.model.actions.AbstractAction
import it.unibo.alchemist.model.maps.maps.environments.NavigationEnvironment
import it.unibo.alchemist.model.molecules.SimpleMolecule
import java.io.File

/**
 * @param <T> Concentration type
 */
class LoadIdFromTrace<T>(
    val environment: NavigationEnvironment<T>,
    node: Node<T>,
    val path: String
) : AbstractAction<T>(
    node
) {
    override fun cloneAction(p0: Node<T>?, p1: Reaction<T>?): Action<T> {
        return LoadIdFromTrace(environment, p0!!, path)
    }

    override fun execute() {
        val resources = javaClass.classLoader.getResource(path)?.let { File(it.toURI()) }
        // get file name without extension of all files
        val names = resources?.listFiles()?.map { it.nameWithoutExtension }?.sorted()
        val nodes = environment.nodes
        // get the index of the current node in the list of nodes
        val index = nodes.indexOf(node)
        // get the id of the current node
        val id = names?.get(index)
        // set the id of the current node
        node.setConcentration(SimpleMolecule("boat"), id as T)
    }

    override fun getContext(): Context = Context.LOCAL

}