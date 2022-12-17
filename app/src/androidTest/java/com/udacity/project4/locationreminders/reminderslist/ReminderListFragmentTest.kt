package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
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
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

/*
 ## IMPORTANT!!! ##
  Before running this test, make sure to have the following:
	1) Device Animations are off (in Developer Options)
	2) Autostart is enabled for this app
	3) Pop-up Windows is enabled for this app
 */

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@MediumTest
class ReminderListFragmentTest : AutoCloseKoinTest()
{
	private val module = module {
		viewModel { RemindersListViewModel(appContext, get() as ReminderDataSource) }
		single { SaveReminderViewModel(appContext, get() as ReminderDataSource) }
		single<ReminderDataSource> { RemindersLocalRepository(get()) }
		single { LocalDB.createRemindersDao(appContext) }
	}

	private lateinit var repository: ReminderDataSource
	private lateinit var appContext: Application

	@get: Rule
	var instantExecutorRule = InstantTaskExecutorRule()

	@Before
	fun setup() {
		stopKoin()
		appContext = ApplicationProvider.getApplicationContext()

		startKoin { androidContext(appContext); modules(listOf(module)) }
		repository = get()
		runBlocking { repository.deleteAllReminders() }
	}

	@After
	fun cleanup() {
		stopKoin()
	}

	@Test
	fun noReminders_noDataDisplayedInUI(): Unit = runBlocking {
		launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

		onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
		onView(withText(appContext.getString(R.string.no_data))).check(matches(isDisplayed()))
	}

	@Test
	fun remindersList_displayedInUI(): Unit = runBlocking {
		val reminder1 = ReminderDTO("reminder1", "", "GooglePlex", 1.0, 1.0)
		val reminder2 = ReminderDTO("reminder2", "", "GooglePlex", 2.0, 2.0)

		runBlocking {
			repository.saveReminder(reminder1)
			repository.saveReminder(reminder2)
		}

		launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
		onView(withId(R.id.reminderssRecyclerView)).check(matches(isDisplayed()))

		onView(withId(R.id.reminderssRecyclerView))
			.check(matches(hasDescendant(withText(reminder1.title))))
			.check(matches(hasDescendant(withText(reminder1.description))))
			.check(matches(hasDescendant(withText(reminder1.location))))
			.check(matches(hasDescendant(withText(reminder2.title))))
			.check(matches(hasDescendant(withText(reminder2.description))))
			.check(matches(hasDescendant(withText(reminder2.location))))
	}

	@Test
	fun clickFAB_navigateToSaveReminderFragment() {
		val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
		val navController = mock(NavController::class.java)

		scenario.onFragment { Navigation.setViewNavController(it.requireView(), navController) }

		onView(withId(R.id.addReminderFAB)).perform(click())
		verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder())
	}

}