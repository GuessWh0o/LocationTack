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
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.guesswho.movetracker.BackgroundLocationTrackingService
import com.guesswho.movetracker.R
import com.guesswho.movetracker.R.dimen.size_form_full_height
import com.guesswho.movetracker.R.dimen.size_form_peek_height
import com.guesswho.movetracker.location.livedata.LocationData
import com.guesswho.movetracker.location.livedata.LocationLiveData
import com.guesswho.movetracker.location.map.GoogleMapController
import com.guesswho.movetracker.util.Coroutines
import com.guesswho.movetracker.util.PermissionUtils
import kotlinx.android.synthetic.main.fragment_map.*
import java.util.*


class Mapfragment : Fragment(), OnMapReadyCallback {

    private val googleMapController by lazy { GoogleMapController() }

    private lateinit var locationLiveData: LocationLiveData

    private val mapViewModel: MapViewModel by viewModels()


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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        sharedPreferences = context.getSharedPreferences("map_prefs", Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_map, container, false)

    @SuppressLint("CheckResult")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapViewDashboard.onCreate(savedInstanceState)
        mapViewDashboard.onResume()
        mapViewDashboard.getMapAsync(this)

        navController = Navigation.findNavController(view)

        googleMapController.addIdleListener(GoogleMap.OnCameraIdleListener {
            googleMapController.getMap()?.let { map ->
                val markerPoint = locationMarkerView.getMarkerPoint()
                val markerLatLong = map.projection.fromScreenLocation(markerPoint)
                mapViewModel.updateLatLong(markerLatLong)
            }
        })


        btnTrackLocationServiceController.setOnClickListener {
            if (isServiceRunning) {
                btnTrackLocationServiceController.setImageResource(R.drawable.ic_start)
                activity?.stopService(backgroundLocationTrackingServiceIntent)
                showSyncLocationDialog()
            } else {
                btnTrackLocationServiceController.setImageResource(R.drawable.ic_stop)
                val uuid = UUID.randomUUID().toString()
                val bundle = Bundle()
                bundle.putString(BackgroundLocationTrackingService.SESSION_ID, uuid)
                backgroundLocationTrackingServiceIntent.putExtras(bundle)
                activity?.startService(backgroundLocationTrackingServiceIntent)
            }
            isServiceRunning = !isServiceRunning
        }

        btn_history.setOnClickListener {
            navController.navigate(MapfragmentDirections.actionEasyMapsActivityToHistoryViewerActivty())
        }

        googleMapController.addIdleListener(locationMarkerView)
        googleMapController.addMoveStartListener(locationMarkerView)

        activity?.let { activity ->
            locationLiveData = LocationLiveData(activity)
            locationLiveData.observe(viewLifecycleOwner, Observer {
                when (it?.status) {
                    LocationData.Status.PERMISSION_REQUIRED -> askLocationPermission(it.permissionList)
                    LocationData.Status.ENABLE_SETTINGS -> enableLocationSettings(it.resolvableApiException)
                    LocationData.Status.LOCATION_SUCCESS -> {
                        it.location?.let { location ->
                            updateUserLocation(location.latitude, location.longitude)
                        }
                    }
                    else -> {
                    }
                }
            })
            locationLiveData.start()
            locationMarkerView.initialize(
                mapFragmentView = this.view,
                bottomSheetExpandedHeight = size_form_full_height,
                bottomSheetCollapsedHeight = size_form_peek_height
            )
        }
    }


    override fun onResume() {
        super.onResume()
        btnTrackLocationServiceController.setImageResource(if (isServiceRunning) R.drawable.ic_stop else R.drawable.ic_start)
    }

    private fun showSyncLocationDialog() {
        val builder = AlertDialog.Builder(activity)
        builder.let {
            it.setMessage("Do you want to sync location to server?")
            it.setPositiveButton("YES") { dialog, _ ->
                mockSyncLocation()
                dialog.dismiss()
            }
            it.setNegativeButton("Cancel") { dialog, _ ->
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
            mapViewModel.syncLocation()
        }) {
            Log.d("updated values", "")
            Toast.makeText(activity, "Syncing Completed updated $it", Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_LOCATION_SETTINGS && resultCode == Activity.RESULT_OK) {
            locationLiveData.start()
        }
    }

    private fun enableLocationSettings(exception: ResolvableApiException?) {
        exception?.startResolutionForResult(activity, REQUEST_CODE_LOCATION_SETTINGS)
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

    private fun askLocationPermission(permissionList: Array<String?>) {
        activity?.let {
            ActivityCompat.requestPermissions(
                it,
                permissionList,
                REQUEST_CODE_LOCATION_PERMISSION
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateUserLocation(latitude: Double, longitude: Double) {
        googleMapController.getMap()?.let { map ->
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(
                        latitude,
                        longitude
                    ), 14.0f
                )
            )
            if (!map.isMyLocationEnabled)
                map.isMyLocationEnabled = true
            mapViewModel.updateLatLong(LatLng(latitude, longitude))
        }
    }

    companion object {
        const val REQUEST_CODE_LOCATION_PERMISSION = 12
        const val REQUEST_CODE_LOCATION_SETTINGS = 13
    }

    override fun onMapReady(map: GoogleMap?) {
        map?.let {
            googleMapController.setGoogleMap(it)
            map.uiSettings.isMyLocationButtonEnabled = true
        }
    }
}