package com.udacity.project4.locationreminders.savereminder

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment()
{
    //// Class Members ///////////////////////////////////////////////////////////////////////////

    companion object {
        @Suppress("unused")
        private val TAG = SaveReminderFragment::class.java.simpleName
    }

    //// Object Members //////////////////////////////////////////////////////////////////////////

    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()

    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var geofencePendingIntent: PendingIntent
    private lateinit var reminder: ReminderDataItem

    //// Override ////////////////////////////////////////////////////////////////////////////////

    @SuppressLint("UnspecifiedImmutableFlag")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil
            .inflate(inflater, R.layout.fragment_save_reminder, container, false)

        binding.viewModel = _viewModel
        setDisplayHomeAsUpEnabled(true)

        geofencingClient = LocationServices.getGeofencingClient(requireContext())

        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        intent.action = GEOFENCE_EVENT_ACTION
        geofencePendingIntent = PendingIntent.getBroadcast(
            requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this

        binding.selectLocation.setOnClickListener {
            // Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections
                    .actionSaveReminderFragmentToSelectLocationFragment())
        }

        // Use the user entered reminder details to:
        //  1) save the reminder to the local db
        //  2) add a geofencing request
        binding.saveReminder.setOnClickListener {
            reminder = ReminderDataItem(
                title = _viewModel.reminderTitle.value,
                description = _viewModel.reminderDescription.value,
                location = _viewModel.reminderSelectedLocationStr.value,
                latitude = _viewModel.latitude.value,
                longitude = _viewModel.longitude.value
            )

            if (_viewModel.validateAndSaveReminder(reminder)) {
                it.isEnabled = false
                checkBackgroundLocationPermission(
                    { checkDeviceLocationSettings({ addGeofenceAndExit() }, null) },
                    { requestBackgroundLocationPermission() }
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            DEVICE_LOCATION_REQUEST_CODE -> wrapEspressoIdlingResource {
                CoroutineScope(Dispatchers.Main).launch {
                    // wait a second for the device to get the current location, then recheck
                    delay(1000)
                    checkDeviceLocationSettings(
                        { addGeofenceAndExit() },
                        { _viewModel.navigationCommand.value = NavigationCommand.Back }
                    )
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE ||
            requestCode == FOREGROUND_LOCATION_PERMISSION_REQUEST_CODE
        ) {
            checkBackgroundLocationPermission(
                { checkDeviceLocationSettings({ addGeofenceAndExit() }, null) },
                { _viewModel.navigationCommand.value = NavigationCommand.Back }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _viewModel.onClear()
    }

    //// Functions ///////////////////////////////////////////////////////////////////////////////

    private fun addGeofenceAndExit() {
        geofencingClient.addReminderGeofence(reminder, geofencePendingIntent)
        _viewModel.navigationCommand.value = NavigationCommand.Back
    }

}
