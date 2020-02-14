package com.guesswho.movetracker.location.map

import android.annotation.SuppressLint
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.guesswho.movetracker.R

class GoogleMapController {

    private var googleMap: GoogleMap? = null

    private val cameraMoveStartListeners = arrayListOf<GoogleMap.OnCameraMoveStartedListener>()

    private val cameraIdleListeners = arrayListOf<GoogleMap.OnCameraIdleListener>()

    private val mapClickListeners = arrayListOf<GoogleMap.OnMapClickListener>()

    fun setGoogleMap(googleMap: GoogleMap) {
        this.googleMap = googleMap
        this.googleMap?.apply {
            setOnCameraIdleListener { notifyCameraIdle() }
            setOnCameraMoveStartedListener { notifyCameraMoveStart(it) }
            setOnMapClickListener { notifyMapClicked(it) }
        }

    }

    fun getMap(): GoogleMap? = googleMap


    fun addPolyline(latlangs: List<LatLng>) {
        googleMap?.apply {
            val polyline = addPolyline(
                PolylineOptions()
                    .clickable(true)
                    .color(R.color.polyline)
                    .addAll(latlangs)
            )
            moveToBounds(polyline)
        }
    }

    private fun moveToBounds(p: Polyline) {
        val builder = LatLngBounds.Builder()
        for (i in p.points.indices) {
            builder.include(p.points[i])
        }
        val bounds = builder.build()
        val padding = 30 // offset from edges of the map in pixels
        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
        googleMap?.apply {
            animateCamera(CameraUpdateFactory.zoomOut())
            animateCamera(cameraUpdate)
        }

    }

    @SuppressLint("MissingPermission")
    fun disableGestures() {
        googleMap?.let {
            it.uiSettings.setAllGesturesEnabled(false)
            it.isMyLocationEnabled = false
        }
    }

    @SuppressLint("MissingPermission")
    fun enableGestures() {
        googleMap?.let {
            it.uiSettings.setAllGesturesEnabled(true)
            it.isMyLocationEnabled = true
        }
    }

    fun addMoveStartListener(moveStartListener: GoogleMap.OnCameraMoveStartedListener) {
        if (cameraMoveStartListeners.contains(moveStartListener).not()) {
            cameraMoveStartListeners.add(moveStartListener)
        }
    }

    fun addIdleListener(idleListener: GoogleMap.OnCameraIdleListener) {
        if (cameraIdleListeners.contains(idleListener).not()) {
            cameraIdleListeners.add(idleListener)
        }
    }

    private fun notifyCameraMoveStart(reason: Int) {
        cameraMoveStartListeners.forEach { it.onCameraMoveStarted(reason) }
    }

    private fun notifyCameraIdle() {
        cameraIdleListeners.forEach { it.onCameraIdle() }
    }

    private fun notifyMapClicked(latLng: LatLng) {
        mapClickListeners.forEach { it.onMapClick(latLng) }
    }

}