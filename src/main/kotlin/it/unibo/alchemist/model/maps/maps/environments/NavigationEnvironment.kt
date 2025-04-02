package it.unibo.alchemist.model.maps.maps.environments

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.data2viz.geojson.GeoJsonObject
import io.data2viz.geojson.JacksonGeoJsonObject
import it.unibo.util.geojson.IsNavigableVisitor
import it.unibo.util.geojson.toLngLatAlt
import it.unibo.alchemist.model.GeoPosition
import it.unibo.alchemist.model.Incarnation
import it.unibo.alchemist.model.environments.Abstract2DEnvironment
import it.unibo.alchemist.model.maps.MapEnvironment
import it.unibo.alchemist.model.maps.environments.OSMEnvironment
import it.unibo.alchemist.model.maps.positions.LatLongPosition
import it.unibo.alchemist.model.maps.routingservices.GraphHopperOptions
import it.unibo.alchemist.model.maps.routingservices.GraphHopperRoutingService
import java.io.File

class NavigationEnvironment<T>(
     incarnation: Incarnation<T, GeoPosition>,
     shorelineFiles: List<String>,
     routesFiles: List<String>,
) : MapEnvironment<T, GraphHopperOptions, GraphHopperRoutingService> by OSMEnvironment(incarnation){

    constructor(incarnation: Incarnation<T, GeoPosition>, shorelineFiles: List<String>):
        this(incarnation, shorelineFiles, emptyList())

    // Loading
    private val geoJsonObjectsForShoreline: List<JacksonGeoJsonObject> = shorelineFiles.map { deserializeGeoJSON(it) }
    private val geoJsonObjectsForRoutes: List<JacksonGeoJsonObject> = routesFiles.map { deserializeGeoJSON(it) }

    private fun deserializeGeoJSON(path: String): JacksonGeoJsonObject {
        val file = this::class.java.classLoader.getResource(path)?.toURI()?.let { File(it) }
        if (file == null) {
            throw IllegalArgumentException("Nor resource $path exist")
        }

        val fileExtension = file.path.split(".")[1]
        if (fileExtension != "geojson")
            error("$fileExtension is not recognized format for this environment: 'geojson' is needed.")

        // Deserialize
        val customMapper = ObjectMapper()
            .registerKotlinModule()
            .findAndRegisterModules()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return customMapper.readValue(file, JacksonGeoJsonObject::class.java)
    }

    fun isPositionNavigable(position: GeoPosition): Boolean = when(position) {
        is LatLongPosition -> isPositionNavigable(position)
        else -> error("Not yet implemented!")
    }

    fun getGeoJsonObjectsForRoutes(): List<JacksonGeoJsonObject> = geoJsonObjectsForRoutes
    fun getGeoJsonObjectsForShoreline(): List<JacksonGeoJsonObject> = geoJsonObjectsForShoreline

    fun isPositionNavigable(position: LatLongPosition): Boolean =
        geoJsonObjectsForShoreline.all{ it.accept(IsNavigableVisitor(position.toLngLatAlt())) }

    override fun makePosition(vararg coordinates: Number): GeoPosition {
        require(coordinates.size == 2) { javaClass.simpleName + " only supports bi-dimensional coordinates (latitude, longitude)" }
        return LatLongPosition(coordinates[0].toDouble(), coordinates[1].toDouble())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as NavigationEnvironment<*>
        if (geoJsonObjectsForShoreline != other.geoJsonObjectsForShoreline) return false
        if (geoJsonObjectsForRoutes != other.geoJsonObjectsForRoutes) return false
        return true
    }

    override fun hashCode(): Int =  geoJsonObjectsForShoreline.hashCode() * geoJsonObjectsForRoutes.hashCode()

}