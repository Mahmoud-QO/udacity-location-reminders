package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result.Error
import com.udacity.project4.locationreminders.data.dto.Result.Success
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Testing implementation to the RemindersLocalRepository.kt

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@MediumTest
class RemindersLocalRepositoryTest
{
	private lateinit var database: RemindersDatabase
	private lateinit var repository: RemindersLocalRepository

	@get:Rule
	val instantTaskExecutorRule = InstantTaskExecutorRule()

	@Before
	fun setup() {
		database = Room.inMemoryDatabaseBuilder(
			ApplicationProvider.getApplicationContext(),
			RemindersDatabase::class.java
		).allowMainThreadQueries().build()

		repository = RemindersLocalRepository(database.reminderDao(), Dispatchers.Main)
	}

	@After
	fun cleanup() {
		database.close()
	}

	@Test
	fun getReminderByIdWhenNotFound() = runBlocking {
		val reminder = repository.getReminder("5") as Error
		assertThat(reminder.message, notNullValue())
		assertThat(reminder.message, `is`("Reminder not found!"))
	}

	@Test
	fun getRemindersWhenNotFound() = runBlocking {
		val outputReminders = repository.getReminders() as Success<List<ReminderDTO>>
		assertThat(outputReminders.data, notNullValue())
		assertThat(outputReminders.data.size, `is`(0))
	}

	@Test
	fun saveReminderAndGetById() = runBlocking {
		val inputReminder = ReminderDTO("reminder", "", "GooglePlex", 0.0, 0.0)

		repository.saveReminder(inputReminder)

		val outputReminder = repository.getReminder(inputReminder.id) as Success<ReminderDTO>
		assertThat(outputReminder.data, notNullValue())

		assertThat(outputReminder.data.id, `is`(inputReminder.id))
		assertThat(outputReminder.data.title, `is`(inputReminder.title))
		assertThat(outputReminder.data.description, `is`(inputReminder.description))
		assertThat(outputReminder.data.location, `is`(inputReminder.location))
		assertThat(outputReminder.data.latitude, `is`(inputReminder.latitude))
		assertThat(outputReminder.data.longitude, `is`(inputReminder.longitude))
	}

	@Test
	fun saveRemindersAndGetReminders() = runBlocking {
		val inputReminder1 = ReminderDTO("reminder1", "", "GooglePlex", 1.0, 1.0)
		val inputReminder2 = ReminderDTO("reminder2", "", "GooglePlex", 2.0, 2.0)
		val inputReminder3 = ReminderDTO("reminder3", "", "GooglePlex", 3.0, 3.0)

		repository.saveReminder(inputReminder1)
		repository.saveReminder(inputReminder2)
		repository.saveReminder(inputReminder3)

		val outputReminders = repository.getReminders() as Success<List<ReminderDTO>>
		assertThat(outputReminders, notNullValue())
		assertThat(outputReminders.data.size, `is`(3))
	}

	@Test
	fun deleteAllRemindersAndGetReminders() = runBlocking {
		val inputReminder1 = ReminderDTO("reminder1", "", "GooglePlex", 1.0, 1.0)
		val inputReminder2 = ReminderDTO("reminder2", "", "GooglePlex", 2.0, 2.0)
		repository.saveReminder(inputReminder1)
		repository.saveReminder(inputReminder2)

		repository.deleteAllReminders()

		val outputReminders = repository.getReminders() as Success<List<ReminderDTO>>
		assertThat(outputReminders.data, notNullValue())
		assertThat(outputReminders.data.size, `is`(0))
	}

}
