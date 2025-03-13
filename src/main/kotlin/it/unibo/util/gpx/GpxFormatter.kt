package it.unibo.util.gpx

import it.unibo.util.ais.AisPayload
import java.io.File
import java.time.Instant
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.w3c.dom.Document

object GpxFormatter {
    fun createGpxFileFromAisData(aisDataList: List<AisPayload>, outputFilePath: String) {
        val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val document: Document = documentBuilder.newDocument()

        // Root element <gpx> creation
        val gpxElement = document.createElement("gpx")
        gpxElement.setAttribute("version", "1.1")
        gpxElement.setAttribute("creator", "AIS-to-GPX Converter")
        document.appendChild(gpxElement)

        // Creation of <trk> trace
        val trkElement = document.createElement("trk")
        gpxElement.appendChild(trkElement)

        // Create track segment <trkseg>
        val trkSegElement = document.createElement("trkseg")
        trkElement.appendChild(trkSegElement)

        // Add positions <trkpt>
        for (data in aisDataList) {
            val trkPtElement = document.createElement("trkpt")
            trkPtElement.setAttribute("lat", data.latitude.toString())
            trkPtElement.setAttribute("lon", data.longitude.toString())

            val timeElement = document.createElement("time")
            timeElement.textContent = data.timestamp.toString()
            trkPtElement.appendChild(timeElement)

            trkSegElement.appendChild(trkPtElement)
        }

        // Save
        val transformerFactory = TransformerFactory.newInstance()
        val transformer = transformerFactory.newTransformer()
        val source = DOMSource(document)
        val result = StreamResult(File(outputFilePath))
        transformer.transform(source, result)
    }
}
