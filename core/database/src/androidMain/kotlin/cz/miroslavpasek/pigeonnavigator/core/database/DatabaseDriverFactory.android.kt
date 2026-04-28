package cz.miroslavpasek.pigeonnavigator.core.database

import android.content.Context
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import org.koin.mp.KoinPlatform

/**
 * Android [DatabaseDriverFactory] implementation backed by [AndroidSqliteDriver].
 */
actual class DatabaseDriverFactory {
    private val context: Context = KoinPlatform.getKoin().get()

    actual fun createDriver(
        schema: SqlSchema<QueryResult.Value<Unit>>,
        databaseName: String
    ): SqlDriver = AndroidSqliteDriver(schema = schema, context = context, name = databaseName)
}
