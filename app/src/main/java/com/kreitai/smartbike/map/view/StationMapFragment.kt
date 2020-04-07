/*
 * MIT License
 *
 * Copyright (c) 2020 Kreitai OÜ
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.kreitai.smartbike.map.view

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import com.kreitai.smartbike.core.utils.PermissionUtils
import com.kreitai.smartbike.core.view.BaseFragment
import com.kreitai.smartbike.databinding.FragmentStationMapBinding
import com.kreitai.smartbike.map.ext.toBitmapDescriptor
import com.kreitai.smartbike.map.presentation.StationItem
import com.kreitai.smartbike.map.presentation.StationMapVm
import kotlinx.android.synthetic.main.fragment_station_map.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf


class StationMapFragment :
    BaseFragment<StationMapVm, FragmentStationMapBinding>(),
    OnMapReadyCallback {

    companion object {
        private const val INITIAL_ZOOM = 17.0f
        private const val MAPVIEW_BUNDLE_KEY = "MapViewBundleKey"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 999
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var permissionDenied: Boolean = false
    private var googleMap: GoogleMap? = null

    override val toolbarResId: Int
        get() = com.kreitai.smartbike.R.id.stations_toolbar
    override val toolbarTitle: CharSequence
        get() = getString(com.kreitai.smartbike.R.string.stations_title)
    override val layoutResource: Int
        get() = com.kreitai.smartbike.R.layout.fragment_station_map

    private val stationMapVm by viewModel<StationMapVm> { parametersOf(resources) }

    override fun bindViewModel(
        viewBinding: FragmentStationMapBinding,
        viewModel: StationMapVm
    ) {
        viewBinding.view = this
        viewBinding.vm = viewModel
    }

    private lateinit var mapView: MapView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices
            .getFusedLocationProviderClient(requireActivity())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var mapViewBundle: Bundle? = null
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY)
        }
        mapView = map
        mapView.onCreate(mapViewBundle)
        mapView.getMapAsync(this)
        getViewModel().getRenderedState()
            .observe(viewLifecycleOwner, Observer { stationsViewState ->

                stationsViewState.stations?.let { stationItems ->
                    for (station in stationItems) {
                        val bikeIcon = when (station.state) {
                            StationItem.StationState.MANY -> com.kreitai.smartbike.R.drawable.ic_bike
                            StationItem.StationState.LOW -> com.kreitai.smartbike.R.drawable.ic_bike_low
                            StationItem.StationState.EMPTY -> com.kreitai.smartbike.R.drawable.ic_bike_empty
                        }
                        googleMap?.addMarker(
                            MarkerOptions().position(
                                LatLng(
                                    station.latitude,
                                    station.longitude
                                )
                            ).title(station.name).icon(
                                context?.let {
                                    bikeIcon.toBitmapDescriptor(
                                        it
                                    )
                                }
                            ).snippet(station.availableBikes)
                        )
                    }
                }
            })
    }

    override fun getViewModel(): StationMapVm {
        return stationMapVm
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        this.googleMap = googleMap
        enableMyLocation()
        getViewModel().fetchStations()
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap?.isMyLocationEnabled = true
            val locationResult: Task<Location?> = fusedLocationClient.lastLocation
            locationResult.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Set the map's camera position to the current location of the device.
                    val location = task.result
                    val currentLatLng = LatLng(
                        location?.latitude ?: 25.033,
                        location?.longitude ?: 121.52
                    )
                    googleMap?.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            currentLatLng,
                            INITIAL_ZOOM
                        )
                    )
                }
            }

        } else {
            // Permission to access the location is missing. Show rationale and request permission
            PermissionUtils.requestPermission(
                requireActivity() as AppCompatActivity?, LOCATION_PERMISSION_REQUEST_CODE,
                Manifest.permission.ACCESS_FINE_LOCATION, true
            )
            val taipei = LatLng(25.033, 121.52)
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(taipei, INITIAL_ZOOM))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        var mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY)
        if (mapViewBundle == null) {
            mapViewBundle = Bundle()
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle)
        }

        mapView.onSaveInstanceState(mapViewBundle)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        if (permissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError()
            permissionDenied = false
        }
    }

    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
    private fun showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
            .newInstance(true).show(parentFragmentManager, "dialog")
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        mapView.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return
        }
        if (PermissionUtils.isPermissionGranted(
                permissions,
                grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation()
        } else {
            // Permission was denied. Display an error message
            // Display the missing permission error dialog when the fragments resume.
            permissionDenied = true
        }
    }

}
