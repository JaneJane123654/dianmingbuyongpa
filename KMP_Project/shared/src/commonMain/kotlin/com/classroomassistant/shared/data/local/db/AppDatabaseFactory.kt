package com.classroomassistant.shared.data.local.db

import com.classroomassistant.shared.db.AppDatabase

class AppDatabaseFactory(
    private val driverFactory: DatabaseDriverFactory,
) {
    fun create(): AppDatabase = AppDatabase(driverFactory.createDriver())

    companion object {
        const val DATABASE_NAME: String = "classroom_assistant.db"
    }
}
