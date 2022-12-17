package com.udacity.project4

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get

// END TO END test to black box test the app
/*
 ## IMPORTANT!!! ##
  Before running this test, make sure to have the following:
	1) Device Animations are off (in Developer Options)
	2) Autostart is enabled for this app
	3) Pop-up Windows is enabled for this app
	4) Device Location is turned on to avoid showing a dialog asking to turn it on
 */

@RunWith(AndroidJUnit4::class)
@LargeTest
class RemindersActivityTest : AutoCloseKoinTest()
{
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    private val module = module {
        viewModel { RemindersListViewModel(appContext, get() as ReminderDataSource) }
        single { SaveReminderViewModel(appContext, get() as ReminderDataSource) }
        single<ReminderDataSource> { RemindersLocalRepository(get()) }
        single { LocalDB.createRemindersDao(appContext) }
    }

    private lateinit var appContext: Application
    private lateinit var activity: RemindersActivity
    private lateinit var repository: ReminderDataSource

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val activityTestRule = ActivityTestRule(RemindersActivity::class.java)

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun setup() {
        stopKoin()

        appContext = getApplicationContext()

        startKoin { androidContext(appContext); modules(listOf(module)) }

        activity = activityTestRule.activity
        repository = get()

        runBlocking { repository.deleteAllReminders() }

        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    @After
    fun cleanup() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
        stopKoin()
    }

    @Test
    fun testRemindersActivity() = runBlocking {
        val scenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(scenario)

        delay(2000)

        // At first "No Data" message is displayed
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
        onView(withId(R.id.noDataTextView)).check(matches(withText("No Data")))
        
        // Click the FAB to add a new reminder
        onView(withId(R.id.addReminderFAB)).check(matches(isDisplayed()))
        onView(withId(R.id.addReminderFAB)).perform(click())

        // Set the reminder title
        onView(withId(R.id.reminderTitle)).check(matches(isDisplayed()))
        onView(withId(R.id.reminderTitle)).perform(typeText("Test Reminder"))
        Espresso.closeSoftKeyboard()

        // Also set a description
        onView(withId(R.id.reminderDescription)).check(matches(isDisplayed()))
        onView(withId(R.id.reminderDescription)).perform(typeText("This is my end-to-end test reminder :)"))
        Espresso.closeSoftKeyboard()

        // Try to save the reminder by clicking the saveReminder button
        onView(withId(R.id.saveReminder)).check(matches(isDisplayed()))
        onView(withId(R.id.saveReminder)).perform(click())

        // A snackbar should be displayed asking us to select a location first
        val selectLocationSnackbarMessage = appContext.getString(R.string.select_location)
        onView(withText(selectLocationSnackbarMessage)).check(matches(isDisplayed()))

        delay(2000)

        // Ok, click the selectLocation button
        onView(withId(R.id.selectLocation)).check(matches(isDisplayed()))
        onView(withId(R.id.selectLocation)).perform(click())
        
        delay(2000)

        // Long click on the mapView to select a location
        onView(withId(R.id.mapView)).check(matches(isDisplayed()))
        onView(withId(R.id.mapView)).perform(longClick())

        // Click save to confirm your selection
        onView(withId(R.id.btn_select)).check(matches(isDisplayed()))
        onView(withId(R.id.btn_select)).perform(click())
        
        delay(2000)

        // Now clicking the saveReminder button to save the reminder
        onView(withId(R.id.saveReminder)).check(matches(isDisplayed()))
        onView(withId(R.id.saveReminder)).perform(click())

        // A Toast should be displayed conforming that the reminder was saved successfully
        val reminderSavedToastMessage = appContext.getString(R.string.reminder_saved)
        onView(withText(reminderSavedToastMessage))
            .inRoot(withDecorView(not(activity.window.decorView)))
            .check(matches(isDisplayed()))

        // The "No Data" message should be gone now
        onView(withId(R.id.noDataTextView)).check(matches(withEffectiveVisibility(Visibility.GONE)))

        // And the reminder should be displayed
        onView(withText("Test Reminder")).check(matches(isDisplayed()))
        onView(withText("This is my end-to-end test reminder :)")).check(matches(isDisplayed()))

        delay(2000)

        scenario.close()
    }

}
