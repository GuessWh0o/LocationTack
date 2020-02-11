package com.guesswho.movetracker.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.guesswho.movetracker.util.Coroutines
import com.guesswho.movetracker.util.DataManager
import kotlinx.android.synthetic.main.fragment_history.*


class HistoryFragment : Fragment(), OnMapReadyCallback {

    private lateinit var navController: NavController

    private lateinit var mContext: Context

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
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        Log.d("Sessiuon  id", sessionId)
        mapView = mapHistory
        mapView.onCreate(savedInstanceState)
        mapView.onResume()
        mapView.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap?) {
        map?.let {
            googleMap = it
            sessionId?.let { sessionId ->
                Coroutines.ioThenMain({ DataManager.latLongListfroSession(sessionId, db) }, {
                    it?.let {
                        val polyline = googleMap.addPolyline(
                            PolylineOptions()
                                .clickable(true)
                                .addAll(it)
                        )
                        moveToBounds(polyline, map)
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
        val cu = CameraUpdateFactory.newLatLngBounds(bounds, padding)
        map.animateCamera(cu)
    }

}
