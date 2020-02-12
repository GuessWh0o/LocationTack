package com.guesswho.movetracker.util

import com.google.android.gms.maps.model.LatLng
import com.guesswho.movetracker.data.HistoryDetails
import com.guesswho.movetracker.database.LocationHistory
import java.text.SimpleDateFormat
import java.util.*


/**
 *
 * @author Maxim on 2020-02-12
 */

fun Long.toTimeString(): String {
    val date = Date(this)
    val formatter = SimpleDateFormat("HH:mm", Locale.GERMANY)
    formatter.timeZone = TimeZone.getTimeZone("UTC")
    return formatter.format(date)
}

/**
 * The calories are estimated by the formula provided by
 * "The Compendium of Physical Activities 2011"
 * And the formula is MET x 3.5 x (body weight kg) / 200 = cals/min
 */
fun List<LocationHistory>.toHistoryDetails(): HistoryDetails {
    var avgSpeed = 0.0
    var time = 0L
    this.sortedBy { it.time }.let { list ->
        avgSpeed = list.sumByDouble { it.speed?.toDouble() ?: 0.0 } / list.size

        val initTime = this.first().time ?: 0
        val finalTime = this.last().time ?: 0
        time = finalTime - initTime
    }

    val met = 8 //Estimated for bike
    val estimatedCalories = (met * 3.5 * 80 / 200).toInt()
    return HistoryDetails(
        duration = time,
        avgSpeed = avgSpeed,
        calories = estimatedCalories
    )
}

fun List<LocationHistory>.toLatLongList(): List<LatLng> {
    val latLongs = mutableListOf<LatLng>()
    this.forEach { latLongs.add(LatLng(it.lat, it.longitude)) }
    return latLongs
}