package com.guesswho.movetracker.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.guesswho.movetracker.R
import com.guesswho.movetracker.database.LocationHistoryDatabase
import com.guesswho.movetracker.location.map.GoogleMapController
import com.guesswho.movetracker.util.*
import kotlinx.android.synthetic.main.fragment_history.*


class HistoryFragment : Fragment(), OnMapReadyCallback {

    private lateinit var navController: NavController

    private lateinit var mContext: Context

    private val googleMapController by lazy { GoogleMapController() }

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
    ): View? = inflater.inflate(R.layout.fragment_history, container, false)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

        mapHistory.onCreate(savedInstanceState)
        mapHistory.onResume()
        mapHistory.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap?) {
        map?.let { googleMap ->
            googleMapController.setGoogleMap(googleMap)
            googleMapController.disableGestures()
            sessionId?.let { sessionId ->
                Coroutines.ioThenMain({ DataManager.getLocationHistoryForSession(db, sessionId) }, {
                    it?.let { locationHistoryList ->
                        locationHistoryList.toLatLongList().also { latLng ->
                            googleMapController.addPolyline(latLng)
                        }
                        locationHistoryList.toHistoryDetails().also { hd ->
                            textviewDuration.text = hd.duration.toTimeString()
                            textviewCalories.text = hd.calories.toString()
                            textviewSpeed.text = hd.avgSpeed.toString()
                            textviewDistance.text =
                                getString(R.string.ui_km, hd.avgSpeed / 60 * hd.duration * 60 * 60)
                        }

                    }
                })

            }
        }
    }


}
