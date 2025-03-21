package it.unibo.alchemist.model.maps.actions

import it.unibo.alchemist.model.*
import it.unibo.alchemist.model.maps.MapEnvironment
import it.unibo.alchemist.model.maps.maps.environments.NavigationEnvironment
import it.unibo.alchemist.model.maps.movestrategies.routing.IgnoreStreets
import it.unibo.alchemist.model.maps.movestrategies.target.FollowTrace
import it.unibo.alchemist.model.movestrategies.speed.ConstantSpeed

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
class ReproduceGPSTraceHarmonized<T, O, S>(
    val environment: NavigationEnvironment<T>,
    node: Node<T>,
    reaction: Reaction<T>,
    speed: Double,
    path: String,
    cycle: Boolean,
    normalizer: String,
    vararg normalizerArgs: Any,
) : MoveOnMapWithGPS<T, O, S> (
    environment as MapEnvironment<T, O, S>,
    node,
    //GPSNavigationRouteConsideringShoreline(environment),
    IgnoreStreets(),
    ConstantSpeed<T, GeoPosition>(reaction, speed),
    FollowTrace(reaction),
    path,
    cycle,
    normalizer,
    normalizerArgs,
) where O: RoutingServiceOptions<O>, S: RoutingService<GeoPosition, O>