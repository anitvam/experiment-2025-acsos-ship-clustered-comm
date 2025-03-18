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
     val path: String,
):
MapEnvironment<T, GraphHopperOptions, GraphHopperRoutingService> by OSMEnvironment(incarnation){

    private lateinit var geoJsonObject: JacksonGeoJsonObject

    init {
        // Loading
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
        geoJsonObject = customMapper.readValue(file, JacksonGeoJsonObject::class.java)
    }

    fun isPositionNavigable(position: GeoPosition): Boolean = when(position) {
        is LatLongPosition -> isPositionNavigable(position)
        else -> error("Not yet implemented!")
    }

    fun getGeoJsonObject(): JacksonGeoJsonObject = geoJsonObject

    fun isPositionNavigable(position: LatLongPosition): Boolean =
        geoJsonObject.accept(IsNavigableVisitor(position.toLngLatAlt()))

    override fun makePosition(vararg coordinates: Number): GeoPosition {
        require(coordinates.size == 2) { javaClass.simpleName + " only supports bi-dimensional coordinates (latitude, longitude)" }
        return LatLongPosition(coordinates[0].toDouble(), coordinates[1].toDouble())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as NavigationEnvironment<*>
        return path == other.path
    }

    override fun hashCode(): Int = path.hashCode()
}