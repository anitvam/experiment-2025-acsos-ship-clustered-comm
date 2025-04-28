package it.unibo.util

import it.unibo.clustered.seaborn.comm.DataRate
import it.unibo.clustered.seaborn.comm.Distance

fun Any?.toDouble(): Double = when (this) {
    is Double -> this
    is Number -> this.toDouble()
    is String -> this.toDouble()
    is DataRate -> kiloBitsPerSecond
    is Distance -> meters
    else -> Double.NaN
}