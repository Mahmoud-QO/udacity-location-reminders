package com.udacity.project4.locationreminders.reminderslist

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.udacity.project4.R
import com.udacity.project4.authentication.AuthenticationActivity
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentRemindersBinding
import com.udacity.project4.locationreminders.ReminderDescriptionActivity
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class ReminderListFragment : BaseFragment()
{
    //// Class Members ///////////////////////////////////////////////////////////////////////////

    companion object {
		@Suppress("unused")
		private val TAG = ReminderListFragment::class.java.simpleName
	}

    //// Object Members //////////////////////////////////////////////////////////////////////////

    //use Koin to retrieve the ViewModel instance
    override val _viewModel: RemindersListViewModel by inject()

    private lateinit var binding: FragmentRemindersBinding
	private lateinit var geofencingClient: GeofencingClient
	private lateinit var geofencePendingIntent: PendingIntent

	/**
	 * Allow re-registering geofences ONLY if the user tried to
	 * enable background location permission or turn on device location throw snackbars
	 * BECAUSE we don't want to re-register each time onResume is called
	 * */
	private var reRegisteringGeofencesAllowed = false

	//// Override ////////////////////////////////////////////////////////////////////////////////

	@SuppressLint("UnspecifiedImmutableFlag")
	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		binding = DataBindingUtil.inflate(
			inflater, R.layout.fragment_reminders, container, false)
		binding.viewModel = _viewModel

		setHasOptionsMenu(true)
		setDisplayHomeAsUpEnabled(false)
		setTitle(getString(R.string.app_name))

		reRegisteringGeofencesAllowed = false
		geofencingClient = LocationServices.getGeofencingClient(requireContext())

		val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
		intent.action = GEOFENCE_EVENT_ACTION
		geofencePendingIntent = PendingIntent.getBroadcast(
			requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
		)

		binding.refreshLayout.setOnRefreshListener { _viewModel.loadReminders() }

		return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        setupRecyclerView()
        binding.addReminderFAB.setOnClickListener {
            navigateToAddReminder()
        }
    }

    override fun onResume() { super.onResume()
        // Load the reminders list on the ui
		_viewModel.loadReminders()

		// Remove snackbars
		binding.coordinatorLayoutReminders.removeAllViews()

		checkBackgroundLocationPermission(
			{ onBackgroundLocationPermissionGranted() }, { onBackgroundLocationPermissionDenied() }
		)
    }

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        // display logout as menu item
        inflater.inflate(R.menu.main_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.logout -> {
                // Add the logout implementation
                AuthUI.getInstance().signOut(requireContext()).addOnSuccessListener {
                    val intent = Intent(activity, AuthenticationActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                    requireActivity().finish()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    //// Functions ///////////////////////////////////////////////////////////////////////////////

    private fun setupRecyclerView() {
        val adapter = RemindersListAdapter { reminder ->
			requireActivity().startActivity(
				ReminderDescriptionActivity.newIntent(requireContext().applicationContext, reminder)
			)
		}

        // setup the recycler view using the extension function
        binding.reminderssRecyclerView.setup(adapter)
    }

    private fun navigateToAddReminder() {
        //use the navigationCommand live data to navigate between the fragments
        _viewModel.navigationCommand.postValue(
            NavigationCommand.To(ReminderListFragmentDirections.toSaveReminder())
        )
    }

	private fun onBackgroundLocationPermissionGranted() = wrapEspressoIdlingResource {
		CoroutineScope(Dispatchers.Main).launch {
			// wait a second for the device to get the current location, then recheck
			// no need to wait if it's the first call of onResume
			if(reRegisteringGeofencesAllowed) delay(1000)

			checkDeviceLocationSettings(
				{ onDeviceLocationTurnedOn() }, { onDeviceLocationTurnedOff() }
			)
		}
	}

	private fun onDeviceLocationTurnedOn() = wrapEspressoIdlingResource {
		CoroutineScope(Dispatchers.Main).launch {
			if (reRegisteringGeofencesAllowed) { reRegisteringGeofencesAllowed = false

				//binding.coordinatorLayoutReminders.removeAllViews()

				_viewModel.remindersList.value?.apply { if (isNotEmpty()) {
					Toast.makeText(
						requireContext(), R.string.re_registering_geofences, Toast.LENGTH_LONG
					).show()

					// Re-register geofences
					geofencingClient.removeGeofences(geofencePendingIntent)?.run {
						addOnCompleteListener {
							forEach {
								geofencingClient.addReminderGeofence(it, geofencePendingIntent)
							}
						}
					}
				}}
			}
		}
	}

	private fun onBackgroundLocationPermissionDenied() = showAppInfoSnackBar(
		R.string.snack_ask_background_location_permission, binding.coordinatorLayoutReminders
	) { reRegisteringGeofencesAllowed = true }

	private fun onDeviceLocationTurnedOff() = showDeviceLocationSnackBar(
		R.string.snack_ask_device_location_settings, binding.coordinatorLayoutReminders
	) { reRegisteringGeofencesAllowed = true }

}
