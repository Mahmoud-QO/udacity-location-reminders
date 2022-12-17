package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.dto.Result.Success
import com.udacity.project4.locationreminders.data.dto.Result.Error

// Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(private val reminders: MutableList<ReminderDTO> = mutableListOf())
    : ReminderDataSource
{
    private var shouldReturnError = false
    fun setShouldReturnError(value: Boolean) { shouldReturnError = value }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        return when {
            shouldReturnError -> Error("FakeDataSource Error")
            else -> Success(reminders)
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        val reminderDto = reminders.find { it.id == id }
        return when {
            shouldReturnError -> Error("FakeDataSource Error")
            reminderDto == null -> Error("Reminder not found")
            else -> Success(reminderDto)
        }
    }

    override suspend fun deleteAllReminders() {
        reminders.clear()
    }

}