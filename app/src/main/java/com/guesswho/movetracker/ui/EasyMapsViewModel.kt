package com.guesswho.movetracker.ui

import android.app.Application
import android.location.Address
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.guesswho.movetracker.data.SelectedAddressInfo
import com.guesswho.movetracker.location.geocoder.GeocoderController
import com.guesswho.movetracker.location.places.PlacesController
import com.guesswho.movetracker.location.places.SearchResultResource
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.*

class EasyMapsViewModel(val app: Application) : AndroidViewModel(app) {

    private val selectedAddressDisposable: Disposable

    private val searchAddressResultDisposable: Disposable

    private var addressDetailDisposable: Disposable? = null

    private val geocoderController = GeocoderController(app)

    private val placesController = PlacesController(app)

    private val selectedAddressViewStateLiveData = MutableLiveData<SelectedAddressViewState>()

    private val searchQueryResultLiveData = MutableLiveData<SearchResultResource>()

    private var selectedAddressInfo = SelectedAddressInfo.empty()

    private var isInitializedWithAddress = false

    private var validateFields = true

    init {
        selectedAddressViewStateLiveData.value = SelectedAddressViewState(selectedAddressInfo)

        selectedAddressDisposable = geocoderController
            .getAddressObservable()
            .onErrorResumeNext { t: Throwable -> Observable.just(Address(Locale.getDefault())) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { updateAddress(it, false) },
                { Log.v(EasyMapsViewModel::class.java.name, it.message) })

        searchAddressResultDisposable = placesController
            .getSearchResultObservable()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { searchQueryResultLiveData.value = it },
                { Log.v(EasyMapsViewModel::class.java.name, it.message) })
    }

    fun initializeWithAddress(selectedAddressInfo: SelectedAddressInfo) {
        isInitializedWithAddress = true
        this.selectedAddressInfo = selectedAddressInfo
        selectedAddressViewStateLiveData.value =
            SelectedAddressViewState(
                selectedAddress = this.selectedAddressInfo,
                moveCameraToLatLong = true
            )
    }

    fun validateFields(validateFields: Boolean) {
        this.validateFields = validateFields
    }

    fun isValidateNeed(): Boolean = validateFields

    fun isInitializedWithAddress() = isInitializedWithAddress

    fun updateLatLong(latLong: LatLng) {
        geocoderController.updateAddress(latLong)
    }

    fun updateAutoCompletePrediction(autocompletePrediction: AutocompletePrediction) {
        addressDetailDisposable =
            placesController.getAddressDetailObservable(autocompletePrediction)
                .flatMap { geocoderController.getAddress(it.latLong!!) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { address -> updateAddress(address, true) },
                    { })
    }

    fun updateAddress(address: Address, moveCamera: Boolean) {
        val addressLine: String = if (address.getAddressLine(0) != null) {
            address.getAddressLine(0)
        } else {
            ""
        }

        selectedAddressInfo = this.selectedAddressInfo.copy(
            address = address,
            fullAddress = addressLine
        )
        selectedAddressViewStateLiveData.value = SelectedAddressViewState(
            selectedAddress = selectedAddressInfo,
            moveCameraToLatLong = moveCamera
        )
    }

    fun getSelectedAddressViewStateLiveData(): LiveData<SelectedAddressViewState> =
        selectedAddressViewStateLiveData

    fun getSearchQueryResultLiveData(): LiveData<SearchResultResource> = searchQueryResultLiveData

    override fun onCleared() {
        super.onCleared()
        if (selectedAddressDisposable.isDisposed.not()) {
            selectedAddressDisposable.dispose()
        }

        if (searchAddressResultDisposable.isDisposed.not()) {
            searchAddressResultDisposable.dispose()
        }

        addressDetailDisposable?.let {
            if (it.isDisposed.not()) {
                it.dispose()
            }
        }

        placesController.destroy()
        geocoderController.destroy()
    }

}