package com.guesswho.movetracker.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import com.google.android.gms.common.api.ResolvableApiException
import com.guesswho.movetracker.R
import com.guesswho.movetracker.location.livedata.LocationData
import com.guesswho.movetracker.location.livedata.LocationLiveData
import com.guesswho.movetracker.util.PermissionUtils

class MainActivity : AppCompatActivity() {
    lateinit var locationLiveData: LocationLiveData
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        locationLiveData = LocationLiveData(activity = this)
        locationLiveData.start()
        locationLiveData.observe(this, Observer {
            when (it?.status) {
                LocationData.Status.PERMISSION_REQUIRED -> askLocationPermission(it.permissionList)
                LocationData.Status.ENABLE_SETTINGS -> enableLocationSettings(it.resolvableApiException)
                LocationData.Status.LOCATION_SUCCESS -> {
                }
            }
        })
    }

    private fun askLocationPermission(permissionList: Array<String?>) {
        ActivityCompat.requestPermissions(
            this,
            permissionList,
            Mapfragment.REQUEST_CODE_LOCATION_PERMISSION
        )

    }

    private fun enableLocationSettings(exception: ResolvableApiException?) {
        exception?.startResolutionForResult(this, Mapfragment.REQUEST_CODE_LOCATION_SETTINGS)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (PermissionUtils.isPermissionResultsGranted(grantResults)) {
            locationLiveData.start()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Mapfragment.REQUEST_CODE_LOCATION_SETTINGS && resultCode == Activity.RESULT_OK) {
            locationLiveData.start()
        }
    }
}
