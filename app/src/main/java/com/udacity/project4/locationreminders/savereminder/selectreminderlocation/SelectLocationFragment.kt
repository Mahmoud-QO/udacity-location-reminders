package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.*
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback
{
	//// Class Members ///////////////////////////////////////////////////////////////////////////

	companion object { private val TAG = SelectLocationFragment::class.java.simpleName }

	//// Object Members //////////////////////////////////////////////////////////////////////////

	// Use Koin to get the view model of the SaveReminder
	override val _viewModel: SaveReminderViewModel by inject()

	private lateinit var binding: FragmentSelectLocationBinding
	private lateinit var mapView: MapView
	private lateinit var map: GoogleMap

	// A marker that holds the data of the selected location
	private var marker: Marker? = null

	// A Boolean that tells if the user just returned from the App Info in the device settings
	private var returnedFromAppInfo = false

	//// Override ////////////////////////////////////////////////////////////////////////////////

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		binding = DataBindingUtil.inflate(
			inflater, R.layout.fragment_select_location, container, false)

		binding.viewModel = _viewModel
		binding.lifecycleOwner = this

		setHasOptionsMenu(true)
		setDisplayHomeAsUpEnabled(true)

		// Add the map setup implementation
		mapView = binding.mapView
		mapView.onCreate(savedInstanceState)
		mapView.getMapAsync(this)

		// Call this function after the user confirms on the selected location
		binding.btnSelect.setOnClickListener { onLocationSelected() }

		return binding.root
	}

	override fun onStart() { super.onStart(); mapView.onStart()
		if(returnedFromAppInfo) {
			returnedFromAppInfo = false
			checkForegroundLocationPermission(
				{ onForegroundLocationPermissionGranted() },
				{ showAppInfoSnackBar(R.string.snack_ask_foreground_location_permission,
					requireView().findViewById(R.id.coordinatorLayout_select_location)) }
			)
		}
	}

	override fun onMapReady(googleMap: GoogleMap) {
		map = googleMap

		checkForegroundLocationPermission(
			onSuccess = { onForegroundLocationPermissionGranted() },
			onFailure = { requestForegroundLocationPermission() }
		)

		// Put a marker to the location that the user selected
		map.setOnMapLongClickListener { latLng -> onMapLongClicked(latLng) }
		map.setOnPoiClickListener { pio -> onPioClicked(pio) }
		map.setOnMyLocationButtonClickListener {
			checkDeviceLocationSettings({ map.showCurrentLocation() }, null)
			true // returning false will essentially call the super method
		}
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)

		when (requestCode) {
			DEVICE_LOCATION_REQUEST_CODE -> wrapEspressoIdlingResource {
				CoroutineScope(Dispatchers.Main).launch {
					// wait a second for the device to get the current location, then recheck
					delay(1000)
					checkDeviceLocationSettings({ map.showCurrentLocation() }, {})
				}
			}
			APP_INFO_REQUEST_CODE -> returnedFromAppInfo = true

		}
	}

	override fun onRequestPermissionsResult(
		requestCode: Int, permissions: Array<out String>, grantResults: IntArray
	) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)

		when (requestCode) {
			FOREGROUND_LOCATION_PERMISSION_REQUEST_CODE ->
				checkForegroundLocationPermission(
					{ onForegroundLocationPermissionGranted() },
					{ showAppInfoSnackBar(R.string.snack_ask_foreground_location_permission,
						requireView().findViewById(R.id.coordinatorLayout_select_location)) }
				)
		}
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		inflater.inflate(R.menu.map_options, menu)
	}

	// Change the map type based on the user's selection.
	override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
		R.id.normal_map -> { map.mapType = GoogleMap.MAP_TYPE_NORMAL; true }
		R.id.hybrid_map -> { map.mapType = GoogleMap.MAP_TYPE_HYBRID; true }
		R.id.satellite_map -> { map.mapType = GoogleMap.MAP_TYPE_SATELLITE; true }
		R.id.terrain_map -> { map.mapType = GoogleMap.MAP_TYPE_TERRAIN; true }
		R.id.retro_map -> { map.setRawResourceStyle(requireContext(), R.raw.map_style_retro); true }
		else -> super.onOptionsItemSelected(item)
	}

	override fun onResume() { super.onResume(); mapView.onResume() }
	override fun onPause() { super.onPause(); mapView.onPause() }
	override fun onStop() { super.onStop(); mapView.onStop() }
	override fun onDestroy() { super.onDestroy(); mapView.onDestroy() }
	override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		mapView.onSaveInstanceState(outState)
	}

	//// Functions ///////////////////////////////////////////////////////////////////////////////

	// When the user confirms on the selected location,
	//  send back the selected location details to the view model
	//  and navigate back to the previous fragment to save the reminder and add the geofence
	private fun onLocationSelected() {
		marker?.apply {
			_viewModel.latitude.value = position.latitude
			_viewModel.longitude.value = position.longitude
			_viewModel.reminderSelectedLocationStr.value = title
		}

		_viewModel.navigationCommand.value = NavigationCommand.Back
	}

	private fun onMapLongClicked(latLng: LatLng) {
		map.clear()
		marker = map.addMarker(MarkerOptions()
			.position(latLng)
			.title(this.getString(R.string.dropped_pin))
		).apply { showInfoWindow() }
	}

	private fun onPioClicked(pio: PointOfInterest) {
		map.clear()
		marker = map.addMarker(MarkerOptions()
			.position(pio.latLng)
			.title(pio.name)
		).apply { showInfoWindow() }
	}

	@SuppressLint("MissingPermission")
	private fun onForegroundLocationPermissionGranted() {
		map.isMyLocationEnabled = true
		checkDeviceLocationSettings({ map.showCurrentLocation() }, null)
	}

	//// Utils ///////////////////////////////////////////////////////////////////////////////////

	// Zoom to the user location after taking his permission
	@SuppressLint("MissingPermission")
	private fun GoogleMap.showCurrentLocation() {
		Log.d(TAG, "#### showCurrentLocation ####")
		println("#### showCurrentLocation ####")
		LocationServices.getFusedLocationProviderClient(requireContext())
			.lastLocation.addOnSuccessListener { location -> location?.let {
				val llCurrentLocation = LatLng(it.latitude, it.longitude)
				animateCamera(CameraUpdateFactory.newLatLngZoom(llCurrentLocation, 16f))
			}
		}
	}

	// Add style to the map
	private fun GoogleMap.setRawResourceStyle(context: Context, style: Int) {
		try {
			val success = setMapStyle(MapStyleOptions.loadRawResourceStyle(context, style))
			if (success) { Log.e(TAG, "Style parsing succeeded.") }
			else { Log.e(TAG, "Style parsing failed.") }
		} catch (e: Resources.NotFoundException) {
			Log.e(TAG, "Cannot find style. Error: ", e)
		}
	}

}
