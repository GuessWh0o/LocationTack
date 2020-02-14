package com.guesswho.movetracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.google.android.gms.maps.model.LatLng
import com.guesswho.movetracker.database.LocationHistoryDatabase
import com.guesswho.movetracker.location.geocoder.GeocoderController
import com.guesswho.movetracker.util.DataManager

class MapViewModel(val app: Application) : AndroidViewModel(app) {
    private val db by lazy { LocationHistoryDatabase.invoke(app.baseContext) }

    private val geocoderController = GeocoderController(app)


    fun updateLatLong(latLong: LatLng) {
        geocoderController.updateAddress(latLong)
    }


    override fun onCleared() {
        super.onCleared()
        geocoderController.destroy()
    }

    fun syncLocation(): Int = DataManager.updatePendingSynced(db)
}