package it.unibo.util.gpx

import it.unibo.util.ais.AisPayload
import java.io.File

object GpxFormatter {
    fun createGpxFileFromAisData(aisData: List<AisPayload>, outputFolder: File) {

        val gpxHeader = """
        <?xml version="1.0" encoding="UTF-8"?>
        <gpx version="1.1" creator="AIS-to-GPX Converter" xmlns="http://www.topografix.com/GPX/1/1">
    """.trimIndent()

        val gpxFooter = "</gpx>"

        // Group AIS data by boatId
        val groupedData = aisData.groupBy { it.boatId }

        groupedData.entries.take(10).forEach { (boatId, points) ->
            val tracks = """
            <trk>
                <name>Boat $boatId</name>
                <trkseg>
                    ${points.joinToString("\n") { point ->
                    """
                        <trkpt lat="${point.latitude}" lon="${point.longitude}">
                            <ele>0</ele>
                            <time>${point.timestamp}</time>
                        </trkpt>
                        """.trimIndent()
                }}
                </trkseg>
            </trk>
            """.trimIndent()

            // Construct output file gpx
            val outputGpx =  "$gpxHeader\n$tracks\n$gpxFooter"

            // Create file for boatID
            val file = File("${outputFolder.path}/$boatId.gpx")
            if (!file.exists()) {
                file.createNewFile()
            }
            file.writeText(outputGpx)
        }
    }
}
