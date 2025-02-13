package it.unibo.alchemist.model.maps.actions;

import it.unibo.alchemist.model.*
import it.unibo.alchemist.model.actions.AbstractConfigurableMoveNode
import it.unibo.alchemist.model.maps.maps.environments.NavigationEnvironment
import it.unibo.alchemist.model.movestrategies.RoutingStrategy
import it.unibo.alchemist.model.movestrategies.SpeedSelectionStrategy
import it.unibo.alchemist.model.movestrategies.TargetSelectionStrategy
import it.unibo.alchemist.model.movestrategies.speed.GloballyConstantSpeed
import it.unibo.alchemist.utils.Maps


/**
 * @param <T> Concentration type
 */
open class NavigateOnMap<T>(
    val environment: NavigationEnvironment<T>,
    node: Node<T>,
    routingStrategy: RoutingStrategy<T, GeoPosition>,
    speedSelectionStrategy: SpeedSelectionStrategy<T, GeoPosition>,
    targetSelectionStrategy: TargetSelectionStrategy<T, GeoPosition>,
) : AbstractConfigurableMoveNode<T, GeoPosition>(
    environment,
    node,
    routingStrategy,
    targetSelectionStrategy,
    speedSelectionStrategy,
    true,
) {

    constructor(
        environment: NavigationEnvironment<T>,
        node: Node<T>,
        reaction: Reaction<T>,
        routingStrategy: RoutingStrategy<T, GeoPosition>,
        speed: Double,
        targetSelectionStrategy: TargetSelectionStrategy<T, GeoPosition>,
    ) : this(
        environment,
        node,
        routingStrategy,
        GloballyConstantSpeed(reaction, speed),
        targetSelectionStrategy,
    )

    /**
     * Fails, can't be cloned.
     */
    override fun cloneAction(node: Node<T>, reaction: Reaction<T>): NavigateOnMap<T> =
        /*
         * Routing strategies can not be cloned at the moment.
         */
        NavigateOnMap(
            environment,
            node,
            routingStrategy.cloneIfNeeded(node, reaction),
            speedSelectionStrategy.cloneIfNeeded(node, reaction),
            targetSelectionStrategy.cloneIfNeeded(node, reaction)
        )

    override fun interpolatePositions(current: GeoPosition?, target: GeoPosition?, maxWalk: Double): GeoPosition {
        return Maps.getDestinationLocation(current, target, maxWalk)
    }
}