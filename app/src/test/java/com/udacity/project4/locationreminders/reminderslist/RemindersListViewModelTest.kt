package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest
{
	private lateinit var viewModel: RemindersListViewModel
	private lateinit var fakeDataSource: FakeDataSource

	@get:Rule var instantExecutorRule = InstantTaskExecutorRule()
	@get: Rule var mainCoroutineRule = MainCoroutineRule()

	@Before
	fun setup() {
		fakeDataSource = FakeDataSource()
		viewModel = RemindersListViewModel(
			ApplicationProvider.getApplicationContext(), fakeDataSource)
	}

	@After
	fun cleanup() = runBlockingTest {
		fakeDataSource.deleteAllReminders()
		stopKoin()
	}

	@Test
	fun loadReminders_showSnackBar() = runBlockingTest {
		fakeDataSource.setShouldReturnError(true)

		viewModel.loadReminders()

		assertThat(viewModel.showSnackBar.getOrAwaitValue(), `is`("FakeDataSource Error"))
	}

	@Test
	fun loadReminders_showLoading() = runBlockingTest {
		fakeDataSource.saveReminder(ReminderDTO("reminder1", "", "GooglePlex", 1.0, 1.0))
		fakeDataSource.saveReminder(ReminderDTO("reminder2", "", "GooglePlex", 2.0, 2.0))

		mainCoroutineRule.pauseDispatcher()

		viewModel.loadReminders()
		assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(true))

		mainCoroutineRule.resumeDispatcher()

		assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(false))
	}

	@Test
	fun loadReminders_showNoData() = runBlockingTest {
		fakeDataSource.deleteAllReminders()

		viewModel.loadReminders()

		assertThat(viewModel.showNoData.getOrAwaitValue(), `is`(true))
	}

	@Test
	fun loadReminders_remindersList() = runBlockingTest {
		fakeDataSource.saveReminder(ReminderDTO("reminder1", "", "GooglePlex", 1.0, 1.0))
		fakeDataSource.saveReminder(ReminderDTO("reminder2", "", "GooglePlex", 2.0, 2.0))
		fakeDataSource.saveReminder(ReminderDTO("reminder3", "", "GooglePlex", 3.0, 3.0))

		viewModel.loadReminders()

		assertThat(viewModel.remindersList.getOrAwaitValue().size, `is`(3))
		assertThat(viewModel.remindersList.getOrAwaitValue().first().location, `is`("GooglePlex"))
	}

}
