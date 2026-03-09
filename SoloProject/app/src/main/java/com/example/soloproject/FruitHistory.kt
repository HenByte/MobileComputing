package com.example.soloproject

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

//This defines a fruit.
@Entity(tableName = "fruit_history")
data class FruitHistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fruitName: String,
    val fruitIcon: String,
    val carbs: Int?,
    val timestamp: Long = System.currentTimeMillis()
)
//This defines how get fruits.
@Dao
interface FruitHistoryDao {
    @Insert
    suspend fun insert(entry: FruitHistoryEntry)

    @Query("SELECT * FROM fruit_history ORDER BY timestamp DESC")
    fun getAll(): Flow<List<FruitHistoryEntry>>
}
//This defines what is the database.
@Database(entities = [FruitHistoryEntry::class], version = 1, exportSchema = false)
abstract class FruitDatabase : RoomDatabase() {
    abstract fun fruitHistoryDao(): FruitHistoryDao

    companion object {
        @Volatile private var instance: FruitDatabase? = null

        fun getInstance(context: Context): FruitDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FruitDatabase::class.java,
                    "fruit_db"
                ).build().also { instance = it }
            }
    }
}
