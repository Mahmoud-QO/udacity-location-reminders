package com.udacity.project4.utils

import android.Manifest
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest

fun Context.isForegroundLocationPermissionGranted(): Boolean {
	return (
		PackageManager.PERMISSION_GRANTED ==
		ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
	)
}

fun Context.isBackgroundLocationPermissionGranted(): Boolean {
	return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
		PackageManager.PERMISSION_GRANTED ==
		ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
	} else {
		isForegroundLocationPermissionGranted()
	}
}

fun Fragment.requestForegroundLocationPermission() {
	requestPermissions(
		arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
		FOREGROUND_LOCATION_PERMISSION_REQUEST_CODE
	)
}

fun Fragment.requestBackgroundLocationPermission() {
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
		requestPermissions(
			arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
			BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE
		)
	} else {
		requestForegroundLocationPermission()
	}
}

/**
 * Checks if the FOREGROUND_LOCATION_PERMISSION is granted:
 * If so, execute onSuccess()
 * If not, execute onFailure()
 */
fun Fragment.checkForegroundLocationPermission(onSuccess: (() -> Unit)?, onFailure: (() -> Unit)?) {
	if (requireContext().isForegroundLocationPermissionGranted()) {
		if (onSuccess != null) { onSuccess() }
	} else {
		if (onFailure != null) { onFailure() }
	}
}

/**
 * Checks if the BACKGROUND_LOCATION_PERMISSION is granted:
 * If so, execute onSuccess()
 * If not, execute onFailure()
 */
fun Fragment.checkBackgroundLocationPermission(onSuccess: (() -> Unit)?, onFailure: (() -> Unit)?) {
	if (requireContext().isBackgroundLocationPermissionGranted()) {
		if (onSuccess != null) { onSuccess() }
	} else {
		if (onFailure != null) { onFailure() }
	}
}

/**
 * Checks if the Device Location is turned on:
 * If so, execute onSuccess()
 * If not, execute onFailure(), but if onFailure is null,
 * then show a dialog that asks the user to turn the device location on.
 */
fun Fragment.checkDeviceLocationSettings(onSuccess: (() -> Unit)?, onFailure: (() -> Unit)?) {
	val locationRequest = LocationRequest.create().apply {
		priority = LocationRequest.PRIORITY_LOW_POWER
	}
	val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

	val settingsClient = LocationServices.getSettingsClient(requireActivity())
	val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())

	locationSettingsResponseTask.addOnSuccessListener { if (onSuccess != null) { onSuccess() } }

	locationSettingsResponseTask.addOnFailureListener { exception ->
		if (onFailure != null) { onFailure() }
		else if (exception is ResolvableApiException) {
			// Location settings are not satisfied, but this can be fixed by showing the user a dialog.
			try {
				// Show the dialog by calling startResolutionForResult(), and check the result in onActivityResult().
				startIntentSenderForResult(
					exception.resolution.intentSender, DEVICE_LOCATION_REQUEST_CODE,
					null, 0, 0, 0, null
				)
			} catch (sendEx: IntentSender.SendIntentException) {
				Log.d(javaClass.name, "Error getting location settings resolution: " + sendEx.message)
			}
		}
	}
}
