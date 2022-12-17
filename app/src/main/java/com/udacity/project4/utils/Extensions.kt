package com.udacity.project4.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseRecyclerViewAdapter
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem


/**
 * Extension function to setup the RecyclerView
 */
fun <T> RecyclerView.setup(adapter: BaseRecyclerViewAdapter<T>) {
    this.apply {
        layoutManager = LinearLayoutManager(this.context)
        this.adapter = adapter
    }
}

fun Fragment.setTitle(title: String) {
    if (activity is AppCompatActivity) {
        (activity as AppCompatActivity).supportActionBar?.title = title
    }
}

fun Fragment.setDisplayHomeAsUpEnabled(bool: Boolean) {
    if (activity is AppCompatActivity) {
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(bool)
    }
}

fun Fragment.showAppInfoSnackBar(
    msg: Int, view: View = requireActivity().findViewById(android.R.id.content),
    onClick: (() -> Unit) = {}
) {
    Snackbar.make(
        view, msg, Snackbar.LENGTH_INDEFINITE
    ).setAction(R.string.settings) {
        onClick()
        startActivityForResult( Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }, APP_INFO_REQUEST_CODE)
    }.show()
}

fun Fragment.showDeviceLocationSnackBar(
    msg: Int, view: View = requireActivity().findViewById(android.R.id.content),
    onSuccess: (() -> Unit)? = null, onFailure: (() -> Unit)? = null, onClick: (() -> Unit) = {}
) {
    Snackbar.make(
        view, msg, Snackbar.LENGTH_INDEFINITE
    ).setAction(R.string.turn_on) {
        onClick()
        checkDeviceLocationSettings(onSuccess, onFailure)
    }.show()
}

@SuppressLint("MissingPermission")
fun GeofencingClient.addReminderGeofence(reminder: ReminderDataItem, pendingIntent: PendingIntent) {
    val geofence = Geofence.Builder()
        .setRequestId(reminder.id)
        .setCircularRegion(reminder.latitude!!, reminder.longitude!!, GEOFENCE_RADIUS_IN_METERS)
        .setExpirationDuration(Geofence.NEVER_EXPIRE)
        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
        .build()

    val geofencingRequest = GeofencingRequest.Builder()
        .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
        .addGeofence(geofence)
        .build()

    this.addGeofences(geofencingRequest, pendingIntent)?.run {
        addOnSuccessListener { Log.e(javaClass.name, "Add Geofence ${geofence.requestId}") }
        addOnFailureListener { if (it.message != null) Log.w(javaClass.name, it.message!!) }
    }
}

//animate changing the view visibility
fun View.fadeIn() {
    this.visibility = View.VISIBLE
    this.alpha = 0f
    this.animate().alpha(1f).setListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
            this@fadeIn.alpha = 1f
        }
    })
}

//animate changing the view visibility
fun View.fadeOut() {
    this.animate().alpha(0f).setListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
            this@fadeOut.alpha = 1f
            this@fadeOut.visibility = View.GONE
        }
    })
}
