package com.guesswho.movetracker.util

import android.location.Location
import androidx.annotation.WorkerThread
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
        }
    }

    @WorkerThread
    fun getLocationHistory(db: LocationHistoryDatabase): List<LocationHistory> =
        db.locationHistoryDoa().getAll().distinctBy { it.session }

    @WorkerThread
    fun getPendingSyncLocationHistory(db: LocationHistoryDatabase): List<LocationHistory> =
        db.locationHistoryDoa().getPendingSyncedLocation()

    @WorkerThread
    fun updatePendingSynced(db: LocationHistoryDatabase): Int {
        val locations = getPendingSyncLocationHistory(db)
        locations.forEach { it.isUpdated = true }
        db.locationHistoryDoa().update(locations)
        return locations.size
    }


    @WorkerThread
    fun getLocationHistoryForSession(
        db: LocationHistoryDatabase, sessionId: String
    ): List<LocationHistory> = db.locationHistoryDoa().getLocationForSession(sessionId)


}