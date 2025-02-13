package it.unibo.alchemist.model.movestrategies.routing

import it.unibo.alchemist.model.GeoPosition
import it.unibo.alchemist.model.Route
import it.unibo.alchemist.model.maps.maps.environments.NavigationEnvironment
import it.unibo.alchemist.model.maps.positions.LatLongPosition
import it.unibo.alchemist.model.movestrategies.RoutingStrategy
import it.unibo.alchemist.model.routes.PolygonalChain
import org.apache.commons.math3.random.RandomGenerator
import kotlin.math.*

class NavigationWithinBoundaries<T>(
    private val environment : NavigationEnvironment<T>,
    private val randomGenerator: RandomGenerator,
): RoutingStrategy<T, GeoPosition> {

    override fun computeRoute(currentPos: GeoPosition, finalPos: GeoPosition): Route<GeoPosition> {
        var destination = finalPos
        while (!environment.isPositionNavigable(destination)) {
            destination = LatLongPosition(
                (currentPos.latitude + finalPos.latitude) / 2,
                (currentPos.longitude + finalPos.longitude) / 2,
            )
        }
        return PolygonalChain(currentPos, destination)
    }

    override fun equals(other: Any?): Boolean = other is NavigationWithinBoundaries<*>

    override fun hashCode() = 1
}