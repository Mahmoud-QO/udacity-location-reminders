package com.udacity.project4.utils

import java.util.concurrent.TimeUnit

val GEOFENCE_EXPIRATION_IN_MILLISECONDS = TimeUnit.HOURS.toMillis(1)
const val GEOFENCE_RADIUS_IN_METERS = 100f
const val GEOFENCE_EVENT_ACTION = "SaveReminderFragment.action.ACTION_GEOFENCE_EVENT"

//todo search about request codes
const val FOREGROUND_LOCATION_PERMISSION_REQUEST_CODE = 34
const val BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE = 35
const val DEVICE_LOCATION_REQUEST_CODE = 29
const val APP_INFO_REQUEST_CODE = 28