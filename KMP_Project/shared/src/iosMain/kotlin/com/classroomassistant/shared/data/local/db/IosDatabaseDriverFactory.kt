package com.classroomassistant.shared.data.local.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.classroomassistant.shared.db.AppDatabase

class IosDatabaseDriverFactory : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver =
        NativeSqliteDriver(
            schema = AppDatabase.Schema,
            name = AppDatabaseFactory.DATABASE_NAME,
        )
}
