package com.guesswho.movetracker

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.guesswho.movetracker.database.LocationHistoryDatabase
import com.guesswho.movetracker.ui.EasyMapsActivity
import com.guesswho.movetracker.util.Coroutines
import com.guesswho.movetracker.util.DataManager
import com.guesswho.movetracker.util.PermissionUtils


class BackgroundLocationTrackingService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private val CHANNEL_ID = "ForegroundServiceChannel"

    private lateinit var sessionId: String

    private val db by lazy { LocationHistoryDatabase.invoke(this) }


    /**
     * Location Request and Client
     */
    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private val locationRequest by lazy { createLocationRequest() }

    /**
     * Location Settings
     */
    private val locationSettingsBuilder by lazy {
        LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
    }

    private val settingsClient by lazy { LocationServices.getSettingsClient(this) }

    /**
     * Last Location
     */
    private var lastLocation: Location? = null

    /**
     * Callback
     */
    private val fineLocationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult?) {
            super.onLocationResult(result)
            result?.let {
                calculateDistance(it.lastLocation)
            }
        }
    }

    private fun calculateDistance(location: Location) {
        /*  var distanceInMeters = -1f
          lastLocation?.let {
              distanceInMeters = it.distanceTo(location)
          }
          if (distanceInMeters < 0 || distanceInMeters > 1) {*/
        lastLocation = location
        saveLocation(location)
        // }
    }

    private fun saveLocation(location: Location) {
        Coroutines.io {
            DataManager.saveLocation(
                db,
                location, sessionId
            )
        }
    }

    /**
     * Creates a LocationRequest model
     */
    private fun createLocationRequest(): LocationRequest = LocationRequest().apply {
        interval = 10000
        fastestInterval = 5000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    /**
     * Checks runtime permission first.
     * Then check if GPS settings is enabled by user
     * If all good, then start listening user location
     * and update livedata
     */
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (PermissionUtils.isLocationPermissionsGranted(this).not())
            return

        val settingsTask = settingsClient.checkLocationSettings(locationSettingsBuilder.build())
        settingsTask.addOnSuccessListener {
            fusedLocationClient.lastLocation.addOnSuccessListener {
                it?.let { calculateDistance(it) }
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    fineLocationCallback,
                    null
                )
            }

        }
    }

    /**
     * Removes listener onInactive
     */
    private fun stopLocationUpdates() =
        fusedLocationClient.removeLocationUpdates(fineLocationCallback)


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        intent?.extras?.let {
            if (it.containsKey(SESSION_ID)) {
                sessionId = it.getString(SESSION_ID, "")
            }
        }
        val notificationIntent = Intent(this, EasyMapsActivity::class.java)
        createNotificationChannel()
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, 0
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Tracking Your Location")
            .setContentText("Storing it")
            .setSmallIcon(R.drawable.ic_icon_marker)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(1, notification)
        startLocationUpdates()

        return START_REDELIVER_INTENT
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }

    companion object {
        const val SESSION_ID = "session_id"
    }
}
