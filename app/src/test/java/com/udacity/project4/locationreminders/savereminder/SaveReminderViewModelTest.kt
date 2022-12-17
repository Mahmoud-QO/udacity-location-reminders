package com.udacity.project4.locationreminders.savereminder

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.Result.Success
import com.udacity.project4.locationreminders.data.dto.Result.Error
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

// Testing the SaveReminderView and its live data objects

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest
{
	private lateinit var viewModel: SaveReminderViewModel
	private lateinit var fakeDataSource: FakeDataSource

	@get:Rule var instantExecutorRule = InstantTaskExecutorRule()
	@get: Rule var mainCoroutineRule = MainCoroutineRule()

	@Before
	fun setup() {
		fakeDataSource = FakeDataSource()
		viewModel = SaveReminderViewModel(
			ApplicationProvider.getApplicationContext(), fakeDataSource)
	}

	@After
	fun cleanup() = runBlockingTest {
		fakeDataSource.deleteAllReminders()
		stopKoin()
	}

	@Test
	fun validateEnteredDataWhenNoTitle_showSnackBar() = runBlockingTest {
		val reminder = ReminderDataItem(null, "", "GooglePlex", 0.0, 0.0, "1")

		assertThat(viewModel.validateEnteredData(reminder), `is`(false))
		assertThat(viewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_enter_title))
	}

	@Test
	fun validateEnteredDataWhenNoLocation_showSnackBar() = runBlockingTest {
		val reminder = ReminderDataItem("reminder", "", null, 0.0, 0.0, "1")

		assertThat(viewModel.validateEnteredData(reminder), `is`(false))
		assertThat(viewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_select_location))
	}

	@Test
	fun validateEnteredDataWhenValid_returnsTrue() = runBlockingTest {
		val reminder = ReminderDataItem("reminder", "", "GooglePlex", 0.0, 0.0, "1")

		assertThat(viewModel.validateEnteredData(reminder), `is`(true))
	}

	@Test
	fun saveReminder_Error() = runBlockingTest {
		fakeDataSource.setShouldReturnError(true)

		val reminder = ReminderDataItem("reminder", "", "GooglePlex", 0.0, 0.0, "1")
		viewModel.saveReminder(reminder)

		val result = fakeDataSource.getReminder(reminder.id) as Error
		assertThat(result, `is`(Error("FakeDataSource Error")))
	}

	@Test
	fun saveReminder_Success() = runBlockingTest {
		val reminder = ReminderDataItem("reminder", "", "GooglePlex", 0.0, 0.0, "1")

		mainCoroutineRule.pauseDispatcher()

		viewModel.saveReminder(reminder)
		assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(true))

		mainCoroutineRule.resumeDispatcher()

		assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(false))
		assertThat(viewModel.showToast.getOrAwaitValue(),
			`is`(ApplicationProvider.getApplicationContext<Context>()
					.getString(R.string.reminder_saved))
		)

		val result = fakeDataSource.getReminder(reminder.id) as Success
		assertThat(result.data.id, `is`(reminder.id))
		assertThat(result.data.title, `is`(reminder.title))
		assertThat(result.data.location, `is`(reminder.location))
	}

	@Test
	fun onClear_nulls() = runBlockingTest {
		viewModel.onClear()

		assertThat(viewModel.reminderTitle.getOrAwaitValue(), `is`(nullValue()))
		assertThat(viewModel.reminderDescription.getOrAwaitValue(), `is`(nullValue()))
		assertThat(viewModel.reminderSelectedLocationStr.getOrAwaitValue(), `is`(nullValue()))
		assertThat(viewModel.latitude.getOrAwaitValue(), `is`(nullValue()))
		assertThat(viewModel.longitude.getOrAwaitValue(), `is`(nullValue()))
	}

}