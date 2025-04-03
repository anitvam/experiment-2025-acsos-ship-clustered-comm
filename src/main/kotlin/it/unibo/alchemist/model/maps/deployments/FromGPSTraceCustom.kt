/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.model.maps.deployments

import it.unibo.alchemist.boundary.gps.loaders.TraceLoader
import it.unibo.alchemist.model.Deployment
import it.unibo.alchemist.model.GeoPosition
import it.unibo.alchemist.model.maps.GPSTrace
import java.io.File
import java.util.stream.Stream
import java.util.stream.StreamSupport

/**
 * Distributes nodes in the first positions of [GPSTrace].
 */
class FromGPSTraceCustom(
    nodeCount: Int,
    path: String,
    cycle: Boolean,
    normalizer: String,
    vararg args: Any
) : Deployment<GeoPosition> {
    private val traces = TraceLoader(path, cycle, normalizer, *args)
    private val numNode: Int

    /**
     * @param nodeCount
     * number of node request
     * @param path
     * path with the gps tracks
     * @param cycle
     * true if, in case there are more nodes to deploy than available GPS traces,
     * the traces should be reused cyclically. E.g., if 10 nodes must be deployed
     * but only 9 GPS traces are available, the first one is reused for the 10th
     * node.
     * @param normalizer
     * class to use to normalize time
     * @param args
     * args to use to create GPSTimeNormalizer
     * @throws IOException if there are errors accessing the file system
     */
    init {
        require(!traces.size().map { size: Int -> size < nodeCount }.orElse(false)) {
            nodeCount.toString() + "traces required, " + traces.size().orElse(-1) + " traces available"
        }
        this.numNode = nodeCount
    }

    constructor(path: String, normalizer: String, vararg normalizerArgs: Any): this(
        File(
            object {}.javaClass.classLoader.getResource(path)?.toURI() ?: throw IllegalArgumentException("File not found: $path")
        ).listFiles()?.size ?: throw IllegalArgumentException("Folder is empty: $path"),
        path,
        false,
        normalizer,
        normalizerArgs
    )

    override fun stream(): Stream<GeoPosition> {
        return StreamSupport.stream(traces.spliterator(), false)
            .limit(numNode.toLong())
            .map { obj: GPSTrace -> obj.initialPosition }
    }
}