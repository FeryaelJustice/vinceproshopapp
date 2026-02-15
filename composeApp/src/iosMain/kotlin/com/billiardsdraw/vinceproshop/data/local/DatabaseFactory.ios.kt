package com.billiardsdraw.vinceproshop.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
actual fun platformDatabaseBuilder(databaseFileName: String): RoomDatabase.Builder<VinceProShopDatabase> {
    val directory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    val path = requireNotNull(directory?.path) { "Cannot resolve documents directory" }
    return Room.databaseBuilder<VinceProShopDatabase>(
        name = "$path/$databaseFileName",
    )
}
