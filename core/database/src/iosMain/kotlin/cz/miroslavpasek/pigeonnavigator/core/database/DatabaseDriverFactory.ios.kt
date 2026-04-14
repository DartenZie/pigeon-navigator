package cz.miroslavpasek.pigeonnavigator.core.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver

/**
 * iOS [DatabaseDriverFactory] implementation backed by [NativeSqliteDriver].
 */
actual class DatabaseDriverFactory {
    actual fun createDriver(
        schema: SqlSchema<QueryResult.Value<Unit>>,
        databaseName: String
    ): SqlDriver = NativeSqliteDriver(schema = schema, name = databaseName)
}
