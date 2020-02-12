package com.guesswho.movetracker.util

import android.location.Location
import android.util.Log
import androidx.annotation.WorkerThread
import com.google.android.gms.maps.model.LatLng
import com.guesswho.movetracker.database.LocationHistory
import com.guesswho.movetracker.database.LocationHistoryDatabase

object DataManager {

    @WorkerThread
    fun saveLocation(db: LocationHistoryDatabase, location: Location, sessionId: String) {
        location.let {

            val locationHistory =
                LocationHistory(
                    it.latitude,
                    it.longitude,
                    it.speed,
                    it.accuracy,
                    location.time,
                    sessionId
                )
            db.locationHistoryDoa().insert(locationHistory)
            Log.d("Size in database", "" + getLocationHistory(db).size)
        }
    }

    @WorkerThread
    fun getLocationHistory(db: LocationHistoryDatabase): List<LocationHistory> {
        return db.locationHistoryDoa().getAll()
    }

    @WorkerThread
    private fun clearLocationHistory(db: LocationHistoryDatabase) {
        return db.locationHistoryDoa().deleteAll()
    }

    @WorkerThread
    fun latLongListForSession(
        sessionId: String,
        db: LocationHistoryDatabase
    ): MutableList<LatLng> {
        val lzocations = db.locationHistoryDoa().getLocationForSession(sessionId)
        val latlongs = mutableListOf<LatLng>()
        lzocations.forEach { latlongs.add(LatLng(it.lat, it.longitude)) }
        return latlongs
    }

    @WorkerThread
    fun getLocationHistoryForSession(
        sessionId: String,
        db: LocationHistoryDatabase
    ): List<LocationHistory> = db.locationHistoryDoa().getLocationForSession(sessionId)

}