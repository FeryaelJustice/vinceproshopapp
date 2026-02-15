package com.billiardsdraw.vinceproshop.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(
    entities = [
        ProductEntity::class,
        CategoryEntity::class,
        FeaturedSlideEntity::class,
        CartItemEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(VinceProShopDatabaseConstructor::class)
abstract class VinceProShopDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun categoryDao(): CategoryDao
    abstract fun featuredDao(): FeaturedDao
    abstract fun cartDao(): CartDao
}

@Suppress("KotlinNoActualForExpect")
expect object VinceProShopDatabaseConstructor : RoomDatabaseConstructor<VinceProShopDatabase> {
    override fun initialize(): VinceProShopDatabase
}
