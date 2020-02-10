package com.guesswho.movetracker.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.guesswho.movetracker.BackgroundLocationTrackingService
import com.guesswho.movetracker.HistoryViewerActivty
import com.guesswho.movetracker.R
import com.guesswho.movetracker.data.SelectedAddressInfo
import com.guesswho.movetracker.database.LocationHistoryDatabase
import com.guesswho.movetracker.location.livedata.LocationData
import com.guesswho.movetracker.location.livedata.LocationLiveData
import com.guesswho.movetracker.location.map.GoogleMapController
import com.guesswho.movetracker.ui.view.LocationMarkerView
import com.guesswho.movetracker.util.Coroutines
import com.guesswho.movetracker.util.PermissionUtils
import kotlinx.android.synthetic.main.activity_easy_maps.*
import java.util.*


class EasyMapsActivity : AppCompatActivity() {

    private lateinit var googleMapController: GoogleMapController

    private lateinit var locationLiveData: LocationLiveData

    private lateinit var easyMapsViewModel: EasyMapsViewModel

    private var isMapsInitialized = false

    private var bottomSheetState = STATE_COLLAPSED

    private val db by lazy { LocationHistoryDatabase(this) }

    private val backgroundLocationTrackingServiceIntent by lazy {
        Intent(
            this,
            BackgroundLocationTrackingService::class.java
        )
    }

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_easy_maps)

        easyMapsViewModel = ViewModelProviders.of(this).get(EasyMapsViewModel::class.java)

        googleMapController = GoogleMapController()


        googleMapController.addIdleListener(GoogleMap.OnCameraIdleListener {
            googleMapController.getMap()?.let { map ->
                val markerPoint = locationMarkerView.getMarkerPoint()
                val markerLatLong = map.projection.fromScreenLocation(markerPoint)
                easyMapsViewModel.updateLatLong(markerLatLong)
            }
        })


        buttonBackgroundTracking.setOnClickListener {
            val uuid = UUID.randomUUID().toString()
            val bundle = Bundle()
            bundle.putString(BackgroundLocationTrackingService.SESSION_ID, uuid)

            backgroundLocationTrackingServiceIntent.putExtras(bundle)
            startService(backgroundLocationTrackingServiceIntent)
        }

        btnStopTracking.setOnClickListener {
            stopService(backgroundLocationTrackingServiceIntent)
            showSyncLocationDialog()

        }
        btn_history.setOnClickListener {
            HistoryViewerActivty.start(this)
        }

        googleMapController.addIdleListener(locationMarkerView)
        googleMapController.addMoveStartListener(locationMarkerView)

        locationLiveData = LocationLiveData(this@EasyMapsActivity)

        locationLiveData.observe(this, Observer {
            when (it?.status) {
                LocationData.Status.PERMISSION_REQUIRED -> askLocationPermission(it.permissionList)
                LocationData.Status.ENABLE_SETTINGS -> enableLocationSettings(it.resolvableApiException)
                LocationData.Status.LOCATION_SUCCESS -> {
                    it.location?.let { location ->
                        updateUserLocation(location.latitude, location.longitude)
                    }
                }
            }
        })

        with(supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment) {
            findViewById<LocationMarkerView>(R.id.locationMarkerView)
                .initialize(
                    mapFragmentView = this.view,
                    bottomSheetExpandedHeight = R.dimen.size_form_full_height,
                    bottomSheetCollapsedHeight = R.dimen.size_form_peek_height
                )

            getMapAsync { map ->
                googleMapController.setGoogleMap(map)
                locationLiveData.start()
            }
        }
    }

    private fun showSyncLocationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.let {
            title = "Done With Run"
            it.setMessage("Do you want to sync location to server?")
            it.setPositiveButton("YES") { dialog, which ->
                mockSyncLocation()
                dialog.dismiss()
            }
            it.setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }
            val dialog: AlertDialog = it.create()
            dialog.show()
        }
    }

    private fun mockSyncLocation() {
        Toast.makeText(this, "Syncing Location to server", Toast.LENGTH_SHORT)
            .show()
        Coroutines.ioThenMain({
            val locations =
                db.locationHistoryDoa().getPendingSyncedLocation()
            locations.forEach { it.isUpdated = true }
            db.locationHistoryDoa().update(locations)
            locations.size
        }) {
            Toast.makeText(this, "Syncing Completed updated $it", Toast.LENGTH_SHORT)
                .show()
        }

        Coroutines.io {
            val locationSource = db.locationHistoryDoa().getAll().distinctBy { it.session }
            locationSource.forEach { Log.d("Session", it.session) }
        }
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
        if (requestCode == REQUEST_CODE_LOCATION_SETTINGS && resultCode == Activity.RESULT_OK) {
            locationLiveData.start()
        }
    }

    private fun askLocationPermission(permissionList: Array<String?>) {
        ActivityCompat.requestPermissions(
            this,
            permissionList,
            REQUEST_CODE_LOCATION_PERMISSION
        )
    }

    private fun enableLocationSettings(exception: ResolvableApiException?) {
        exception?.startResolutionForResult(this, REQUEST_CODE_LOCATION_SETTINGS)
    }

    @SuppressLint("MissingPermission")
    private fun updateUserLocation(latitude: Double?, longitude: Double?) {
        googleMapController.getMap()?.let { map ->
            if (isMapsInitialized.not()) {
                isMapsInitialized = true
                map.isMyLocationEnabled = true
                map.uiSettings.isMyLocationButtonEnabled = true
                map.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(
                            latitude ?: 0.0,
                            longitude ?: 0.0
                        ), 14.0f
                    )
                )
            }

            easyMapsViewModel.updateLatLong(LatLng(latitude ?: 0.0, longitude ?: 0.0))
        }
    }

    companion object {

        const val KEY_SELECTED_ADDRESS = "KEY_SELECTED_ADDRESS"
        const val KEY_VALIDATE_FIELDS = "KEY_VALIDATE_FIELDS"

        const val REQUEST_CODE_LOCATION_PERMISSION = 12
        const val REQUEST_CODE_LOCATION_SETTINGS = 13

        @JvmStatic
        fun newIntent(
            context: Context,
            selectedAddressInfo: SelectedAddressInfo? = null,
            validateFields: Boolean
        ): Intent {
            return Intent(context, EasyMapsActivity::class.java)
                .apply {
                    putExtra(KEY_SELECTED_ADDRESS, selectedAddressInfo)
                    putExtra(KEY_VALIDATE_FIELDS, validateFields)
                }
        }
    }
}