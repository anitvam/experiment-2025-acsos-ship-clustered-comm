package it.unibo.util.gpx

object ParsingUtils {
    fun validateLatitude(latitude: Double): Boolean =
        !(latitude.isNaN() || latitude < -90.0 || latitude > 90.0)

    fun validateLongitude(longitude: Double): Boolean =
        !(longitude.isNaN() || longitude < -180.0 || longitude > 180.0)
}