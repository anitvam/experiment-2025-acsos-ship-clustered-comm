/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.model.maps.actions

import it.unibo.alchemist.model.*
import it.unibo.alchemist.model.maps.MapEnvironment
import it.unibo.alchemist.model.maps.movestrategies.routing.IgnoreStreets
import it.unibo.alchemist.model.maps.movestrategies.speed.StraightLineTraceDependantSpeed
import it.unibo.alchemist.model.maps.movestrategies.target.FollowTrace
import it.unibo.alchemist.model.movestrategies.routing.strategy.GPSNavigationRouteConsideringShoreline
import it.unibo.alchemist.model.movestrategies.speed.ConstantSpeed

/**
 * @param <T> Concentration type
 * @param <O> [RoutingServiceOptions] type
 * @param <S> [RoutingService] type
</S></O></T> */
class ReproduceGPSTrace<T, O : RoutingServiceOptions<O>, S : RoutingService<GeoPosition, O>>
    : MoveOnMapWithGPS<T, O, S> {
    /**
     * @param environment
     * the environment
     * @param node
     * the node
     * @param reaction
     * the reaction. Will be used to compute the distance to walk in
     * every step, relying on [Reaction]'s getRate() method.
     * @param path
     * resource(file, directory, ...) with GPS trace
     * @param cycle
     * true if the traces have to be distributed cyclically
     * @param normalizer
     * name of the class that implement the strategy to normalize the
     * time
     * @param normalizerArgs
     * Args to build normalize
     */
    constructor(
        environment: MapEnvironment<T?, O, S>,
        node: Node<T?>,
        reaction: Reaction<T?>,
        path: String,
        cycle: Boolean,
        normalizer: String?,
        vararg normalizerArgs: Any?
    ) : super(
        environment, node,
        IgnoreStreets<T, GeoPosition>(),
        StraightLineTraceDependantSpeed<T?, O, S>(environment, node, reaction),
        FollowTrace<T>(reaction),
        path, cycle, normalizer, *normalizerArgs
    )

    /**
     * @param environment
     * the environment
     * @param node
     * the node
     * @param reaction
     * the reaction. Will be used to compute the distance to walk in
     * every step, relying on [Reaction]'s getRate() method.
     * @param speed
     * the average speed
     * @param path
     * resource(file, directory, ...) with GPS trace
     * @param cycle
     * true if the traces have to be distributed cyclically
     * @param normalizer
     * name of the class that implement the strategy to normalize the
     * time
     * @param normalizerArgs
     * Args to build normalize
     */
    constructor(
        environment: MapEnvironment<T, O, S>,
        node: Node<T>,
        reaction: Reaction<T?>?,
        speed: Double,
        path: String,
        cycle: Boolean,
        normalizer: String?,
        vararg normalizerArgs: Any?
    ) : super(
        environment,
        node,
        //IgnoreStreets<T, GeoPosition>(),
        GPSNavigationRouteConsideringShoreline(environment),
        ConstantSpeed<T, GeoPosition>(reaction, speed),
        FollowTrace<T>(reaction),
        path,
        cycle,
        normalizer,
        *normalizerArgs
    )
}