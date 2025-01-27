
package it.unibo.alchemist.model.maps.movestrategies.routing;

import it.unibo.alchemist.model.routes.PolygonalChain;
import it.unibo.alchemist.model.Position;
import it.unibo.alchemist.model.movestrategies.RoutingStrategy;
import it.unibo.alchemist.model.Route;

/**
 * This strategy ignores any information about the map, and connects the
 * starting and ending point with a straight line using
 * {@link PolygonalChain}.
 *
 * @param <T> Concentration type
 * @param <P> position type
 */
open class IgnoreStreetsK<T, P : Position<P>> : RoutingStrategy<T, P> {
    @Override
    override fun computeRoute(currentPos: P, finalPos: P): Route<P> =
        PolygonalChain(currentPos, finalPos)

    override fun toString(): String = "IgnoreStreets"

    override fun equals(other: Any?): Boolean = other is IgnoreStreetsK<*, *>

    override fun hashCode() = 1
}
