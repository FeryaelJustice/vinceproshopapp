package com.billiardsdraw.vinceproshop.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import com.billiardsdraw.vinceproshop.AndroidAppContext

actual fun platformDatabaseBuilder(databaseFileName: String): RoomDatabase.Builder<VinceProShopDatabase> {
    val context = AndroidAppContext.requireContext()
    val dbFile = context.getDatabasePath(databaseFileName).absolutePath
    return Room.databaseBuilder<VinceProShopDatabase>(
        context = context,
        name = dbFile,
    )
}
