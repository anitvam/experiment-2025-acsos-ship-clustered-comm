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

fun Any?.toInt(): Int = when (this) {
    is Number -> this.toInt()
    is String -> this.toInt()
    is DataRate -> kiloBitsPerSecond.toInt()
    is Distance -> meters.toInt()
    else -> 0
}

fun Any?.isNan(): Boolean = when (this) {
    is Double.NaN -> true
    else -> false
}

fun Any?.toBoolean(): Boolean = when (this) {
    is Boolean -> this
    is Number -> this.toInt() != 0
    is String -> this.toBooleanStrict()
    is DataRate -> toInt().toBoolean()
    is Distance -> toInt().toBoolean()
    else -> false
}

