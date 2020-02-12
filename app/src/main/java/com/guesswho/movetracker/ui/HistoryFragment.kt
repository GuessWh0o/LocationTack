package com.guesswho.movetracker.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.guesswho.movetracker.R
import com.guesswho.movetracker.database.LocationHistoryDatabase
import com.guesswho.movetracker.ui.view.DetailView
import com.guesswho.movetracker.util.*
import kotlinx.android.synthetic.main.fragment_history.*


class HistoryFragment : Fragment(), OnMapReadyCallback {

    private lateinit var navController: NavController

    private lateinit var mContext: Context

    private lateinit var tvDistance: TextView

    private lateinit var dvSpeed: DetailView
    private lateinit var dvCalories: DetailView
    private lateinit var dvDuration: DetailView

    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private val sessionId by lazy {
        arguments?.let { HistoryFragmentArgs.fromBundle(it).sessionId }
    }

    private val db by lazy {
        LocationHistoryDatabase.invoke(mContext)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_history, container, false)
        tvDistance = rootView.findViewById(R.id.tv_distance)
        dvDuration = rootView.findViewById(R.id.dv_duration)
        dvSpeed = rootView.findViewById(R.id.dv_speed)
        dvCalories = rootView.findViewById(R.id.dv_calories)
        // Inflate the layout for this fragment
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        Log.d("Session  id", sessionId)
        mapView = mapHistory
        mapView.onCreate(savedInstanceState)
        mapView.onResume()
        mapView.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap?) {
        map?.let { gm ->
            googleMap = gm
            sessionId?.let { sessionId ->
                Coroutines.ioThenMain({ DataManager.getLocationHistoryForSession(sessionId, db) }, {
                    it?.let { locationHistoryList ->
                        locationHistoryList.toLatLongList().also { latLng ->
                            val polyline = googleMap.addPolyline(
                                PolylineOptions()
                                    .clickable(true)
                                    .color(R.color.polyline)
                                    .addAll(latLng)
                            )
                            moveToBounds(polyline, googleMap)
                        }
                        locationHistoryList.toHistoryDetails().also { hd ->
                            dvDuration.setTitle(hd.duration.toTimeString())
                            dvCalories.setTitle(hd.calories.toString())
                            dvSpeed.setTitle(hd.avgSpeed.toString())
                            tvDistance.text = getString(R.string.ui_km, hd.avgSpeed / 60 * hd.duration * 60 * 60)
                        }

                    }
                })

            }
        }
    }


    private fun moveToBounds(p: Polyline, map: GoogleMap) {
        val builder = LatLngBounds.Builder()
        for (i in p.points.indices) {
            builder.include(p.points[i])
        }
        val bounds = builder.build()
        val padding = 0 // offset from edges of the map in pixels
        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
        map.animateCamera(CameraUpdateFactory.zoomOut())
        map.animateCamera(cameraUpdate)

    }
}
