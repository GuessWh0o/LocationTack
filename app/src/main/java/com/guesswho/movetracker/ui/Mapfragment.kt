package com.guesswho.movetracker.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.guesswho.movetracker.BackgroundLocationTrackingService
import com.guesswho.movetracker.R
import com.guesswho.movetracker.database.LocationHistoryDatabase
import com.guesswho.movetracker.location.livedata.LocationData
import com.guesswho.movetracker.location.livedata.LocationLiveData
import com.guesswho.movetracker.location.map.GoogleMapController
import com.guesswho.movetracker.util.Coroutines
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.activity_easy_maps.*
import java.util.*


class Mapfragment : Fragment(), OnMapReadyCallback {

    private lateinit var googleMapController: GoogleMapController

    private lateinit var locationLiveData: LocationLiveData

    private val easyMapsViewModel: EasyMapsViewModel by viewModels()

    private var isMapsInitialized = false

    private val db by lazy { LocationHistoryDatabase(mContext) }

    private val backgroundLocationTrackingServiceIntent by lazy {
        Intent(
            activity,
            BackgroundLocationTrackingService::class.java
        )
    }

    private var isServiceRunning: Boolean
        get() = sharedPreferences.getBoolean("ServiceRunning", false)
        set(value) {
            sharedPreferences.edit().putBoolean("ServiceRunning", value).apply()
        }

    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var navController: NavController

    private lateinit var mContext: Context

    private lateinit var mapView: MapView
    override fun onAttach(context: Context) {
        super.onAttach(context)
        sharedPreferences = context.getSharedPreferences("map_prefs", Context.MODE_PRIVATE)
        mContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_easy_maps, container, false)
    }

    @SuppressLint("CheckResult")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        googleMapController = GoogleMapController()
        mapView = map
        mapView.onCreate(savedInstanceState)
        mapView.onResume()
        mapView.getMapAsync(this)

        navController = Navigation.findNavController(view)

        googleMapController.addIdleListener(GoogleMap.OnCameraIdleListener {
            googleMapController.getMap()?.let { map ->
                val markerPoint = locationMarkerView.getMarkerPoint()
                val markerLatLong = map.projection.fromScreenLocation(markerPoint)
                easyMapsViewModel.updateLatLong(markerLatLong)
            }
        })


        btn_start_stop.setOnClickListener {
            if (isServiceRunning) {
                btn_start_stop.setImageResource(R.drawable.ic_start)
                activity?.stopService(backgroundLocationTrackingServiceIntent)
                showSyncLocationDialog()
            } else {
                btn_start_stop.setImageResource(R.drawable.ic_stop)
                val uuid = UUID.randomUUID().toString()
                val bundle = Bundle()
                bundle.putString(BackgroundLocationTrackingService.SESSION_ID, uuid)
                backgroundLocationTrackingServiceIntent.putExtras(bundle)
                activity?.startService(backgroundLocationTrackingServiceIntent)
            }
            isServiceRunning = !isServiceRunning
        }

        btn_history.setOnClickListener {
            context?.let { it1 -> navController.navigate(MapfragmentDirections.actionEasyMapsActivityToHistoryViewerActivty()) }
        }

        googleMapController.addIdleListener(locationMarkerView)
        googleMapController.addMoveStartListener(locationMarkerView)

        activity?.let {
            locationLiveData = LocationLiveData(it)
            locationLiveData.observe(viewLifecycleOwner, Observer {
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
            locationLiveData.start()
            locationMarkerView.initialize(
                mapFragmentView = this.view,
                bottomSheetExpandedHeight = R.dimen.size_form_full_height,
                bottomSheetCollapsedHeight = R.dimen.size_form_peek_height
            )
        }
    }


    override fun onResume() {
        super.onResume()
        btn_start_stop.setImageResource(if (isServiceRunning) R.drawable.ic_stop else R.drawable.ic_start)
    }

    private fun showSyncLocationDialog() {
        val builder = AlertDialog.Builder(activity)
        builder.let {
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
        Toast.makeText(activity, "Syncing Location to server", Toast.LENGTH_SHORT)
            .show()
        Coroutines.ioThenMain({

            val locations =
                db.locationHistoryDoa().getPendingSyncedLocation()
            locations.forEach { it.isUpdated = true }
            db.locationHistoryDoa().update(locations)
            locations.size
        }) {
            Toast.makeText(activity, "Syncing Completed updated $it", Toast.LENGTH_SHORT)
                .show()
        }

        Coroutines.io {
            val locationSource = db.locationHistoryDoa().getAll().distinctBy { it.session }
            locationSource.forEach { Log.d("Session", it.session) }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_LOCATION_SETTINGS && resultCode == Activity.RESULT_OK) {
            locationLiveData.start()
        }
    }

    private fun askLocationPermission(permissionList: List<String>) {
        Dexter.withActivity(activity)
            .withPermissions(permissionList)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let {
                        locationLiveData.start()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                }

            }).check()
    }

    private fun enableLocationSettings(exception: ResolvableApiException?) {
        exception?.startResolutionForResult(activity, REQUEST_CODE_LOCATION_SETTINGS)
    }

    @SuppressLint("MissingPermission")
    private fun updateUserLocation(latitude: Double?, longitude: Double?) {
        googleMapController.getMap()?.let { map ->
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(
                        latitude ?: 0.0,
                        longitude ?: 0.0
                    ), 14.0f
                )
            )
            easyMapsViewModel.updateLatLong(LatLng(latitude ?: 0.0, longitude ?: 0.0))
        }
    }

    companion object {
        const val REQUEST_CODE_LOCATION_PERMISSION = 12
        const val REQUEST_CODE_LOCATION_SETTINGS = 13
    }

    override fun onMapReady(map: GoogleMap?) {
        map?.let {
            googleMapController.setGoogleMap(it)
            map.isMyLocationEnabled = true
            map.uiSettings.isMyLocationButtonEnabled = true
        }
    }
}