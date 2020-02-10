package com.guesswho.movetracker.location.map

import com.google.android.gms.maps.GoogleMap

open class SimpleCancellableCallback : GoogleMap.CancelableCallback{

    override fun onFinish() {
    }

    override fun onCancel() {
    }
}