package it.unibo.alchemist.model.maps.actions;

import it.unibo.alchemist.model.*
import it.unibo.alchemist.model.actions.AbstractConfigurableMoveNode;
import it.unibo.alchemist.model.maps.MapEnvironment;
import it.unibo.alchemist.model.movestrategies.RoutingStrategy;
import it.unibo.alchemist.model.movestrategies.SpeedSelectionStrategy;
import it.unibo.alchemist.model.movestrategies.TargetSelectionStrategy;
import it.unibo.alchemist.model.movestrategies.speed.GloballyConstantSpeed
import it.unibo.alchemist.utils.Maps;


/**
 * @param <T> Concentration type
 * @param <O> {@link RoutingServiceOptions} type
 * @param <S> {@link RoutingService} type
 */
open class MoveOnMapK<T, O : RoutingServiceOptions<O>, S : RoutingService<GeoPosition, O>>(
    val environment: MapEnvironment<T, O, S>,
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

    /**
     * @param environment
     *            the environment
     * @param node
     *            the node
     * @param routingStrategy the {@link RoutingStrategy}
     * @param speedSelectionStrategy
     *            the {@link SpeedSelectionStrategy}
     * @param targetSelectionStrategy
     *            {@link TargetSelectionStrategy}
     */
    constructor(
        environment: MapEnvironment<T, O, S>,
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
    override fun cloneAction(node: Node<T>, reaction: Reaction<T>): MoveOnMap<T, O, S> =
        /*
         * Routing strategies can not be cloned at the moment.
         */
        MoveOnMap(
        environment,
        node,
        routingStrategy.cloneIfNeeded(node, reaction),
        speedSelectionStrategy.cloneIfNeeded(node, reaction),
        targetSelectionStrategy.cloneIfNeeded(node, reaction)
    )

    override fun interpolatePositions(current: GeoPosition?, target: GeoPosition?, maxWalk: Double): GeoPosition =
        Maps.getDestinationLocation(current, target, maxWalk)

}