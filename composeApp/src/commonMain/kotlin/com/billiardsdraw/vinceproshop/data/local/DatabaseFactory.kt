package com.billiardsdraw.vinceproshop.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

private const val DATABASE_FILE_NAME = "vinceproshop.db"

expect fun platformDatabaseBuilder(databaseFileName: String): RoomDatabase.Builder<VinceProShopDatabase>

fun createRoomDatabase(): VinceProShopDatabase {
    return platformDatabaseBuilder(DATABASE_FILE_NAME)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .fallbackToDestructiveMigration(true)
        .build()
}
