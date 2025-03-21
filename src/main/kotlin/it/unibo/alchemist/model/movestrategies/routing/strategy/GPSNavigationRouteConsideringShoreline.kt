package it.unibo.alchemist.model.movestrategies.routing.strategy

import it.unibo.alchemist.model.GeoPosition
import it.unibo.alchemist.model.Route
import it.unibo.alchemist.model.RoutingService
import it.unibo.alchemist.model.RoutingServiceOptions
import it.unibo.alchemist.model.maps.MapEnvironment
import it.unibo.alchemist.model.maps.maps.environments.NavigationEnvironment
import it.unibo.alchemist.model.movestrategies.RoutingStrategy
import it.unibo.alchemist.model.routes.PolygonalChain

class GPSNavigationRouteConsideringShoreline<T, O : RoutingServiceOptions<O>, S : RoutingService<GeoPosition, O>>(
    environment: MapEnvironment<T, O, S>
): RoutingStrategy<T, GeoPosition> {
    init {
        require(environment is NavigationEnvironment<T>)
        println("Using custom implementation for GPSNavigationRouteConsideringShoreline")
    }
    override fun computeRoute(currentPos: GeoPosition, finalPos: GeoPosition): Route<GeoPosition> {
        return PolygonalChain(currentPos, finalPos)
    }
}