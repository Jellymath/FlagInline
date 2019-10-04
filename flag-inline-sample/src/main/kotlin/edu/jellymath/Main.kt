package edu.jellymath

import edu.jellymath.InlineStatusSensor.Companion.GPS
import edu.jellymath.InlineStatusSensor.Companion.DIFFERENTIAL_PRESSURE
import edu.jellymath.InlineStatusSensor.Companion.THREE_DIMENSIONAL_ACCEL
import edu.jellymath.InlineStatusSensor.Companion.THREE_DIMENSIONAL_MAG

@Flags
enum class StatusSensor {
    THREE_DIMENSIONAL_GYRO,
    THREE_DIMENSIONAL_ACCEL,
    THREE_DIMENSIONAL_MAG,
    ABSOLUTE_PRESSURE,
    DIFFERENTIAL_PRESSURE,
    GPS,
    OPTICAL_FLOW
}

fun main() {
    val sensors = DIFFERENTIAL_PRESSURE or GPS or THREE_DIMENSIONAL_ACCEL
    println(sensors.toString())
    println(GPS in sensors)
    println(THREE_DIMENSIONAL_MAG in sensors)
}