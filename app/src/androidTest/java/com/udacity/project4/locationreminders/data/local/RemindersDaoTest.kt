package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Test

// Testing implementation to the RemindersDao.kt

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@SmallTest
class RemindersDaoTest
{
	private lateinit var database: RemindersDatabase

	@get:Rule
	val instantTaskExecutorRule = InstantTaskExecutorRule()

	@Before
	fun initDatabase() {
		database = Room.inMemoryDatabaseBuilder(
			ApplicationProvider.getApplicationContext(),
			RemindersDatabase::class.java
		).build()
	}

	@After
	fun closeDatabase() {
		database.close()
	}

	@Test
	fun insertReminderAndGetById() = runBlockingTest {
		val inputReminder = ReminderDTO("reminder", "", "GooglePlex", 0.0, 0.0)

		database.reminderDao().saveReminder(inputReminder)

		val outputReminder = database.reminderDao().getReminderById(inputReminder.id)
		assertThat(outputReminder as ReminderDTO, notNullValue())

		assertThat(outputReminder.id, `is`(inputReminder.id))
		assertThat(outputReminder.title, `is`(inputReminder.title))
		assertThat(outputReminder.description, `is`(inputReminder.description))
		assertThat(outputReminder.location, `is`(inputReminder.location))
		assertThat(outputReminder.latitude, `is`(inputReminder.latitude))
		assertThat(outputReminder.longitude, `is`(inputReminder.longitude))
	}

	@Test
	fun insertRemindersAndGetReminders() = runBlockingTest {
		val inputReminder1 = ReminderDTO("reminder1", "", "GooglePlex", 1.0, 1.0)
		val inputReminder2 = ReminderDTO("reminder2", "", "GooglePlex", 2.0, 2.0)
		val inputReminder3 = ReminderDTO("reminder3", "", "GooglePlex", 3.0, 3.0)

		database.reminderDao().saveReminder(inputReminder1)
		database.reminderDao().saveReminder(inputReminder2)
		database.reminderDao().saveReminder(inputReminder3)

		val outputReminders = database.reminderDao().getReminders()
		assertThat(outputReminders, notNullValue())
		assertThat(outputReminders.size, `is`(3))

		assertThat(outputReminders, hasItem(inputReminder1))
		assertThat(outputReminders, hasItem(inputReminder2))
		assertThat(outputReminders, hasItem(inputReminder3))
	}

	@Test
	fun deleteAllRemindersAndGetNoReminders() = runBlockingTest {
		val inputReminder1 = ReminderDTO("reminder1", "", "GooglePlex", 1.0, 1.0)
		val inputReminder2 = ReminderDTO("reminder2", "", "GooglePlex", 2.0, 2.0)
		database.reminderDao().saveReminder(inputReminder1)
		database.reminderDao().saveReminder(inputReminder2)

		database.reminderDao().deleteAllReminders()

		val outputReminders = database.reminderDao().getReminders()
		assertThat(outputReminders, notNullValue())
		assertThat(outputReminders.size, `is`(0))
	}

}