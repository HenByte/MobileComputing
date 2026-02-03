package com.example.mobilecomputing

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

//This defines a user.
@Entity(tableName = "users")
data class User(
        @PrimaryKey(autoGenerate = true)
        val id: Long = 0,
        val name: String,
        val imageUri: String?
)

@Dao
interface UserDao  {

    @Query("SELECT * FROM users LIMIT 1")
    fun getCurrent(): Flow<User?>

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getCurrentOnce(): User?


    @Insert
    suspend fun  insert(user: User): Long

    @Delete
    suspend fun delete(user: User)
}

//This ties the message to a user.
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]

)
//This is a message from user.
data class Message(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val text: String
)
@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE userId = :userId ORDER BY id ASC")
    fun getMessagesByUser(userId: Long): Flow<List<Message>>

    @Insert
    suspend fun insert(message: Message): Long

    @Query("DELETE FROM messages WHERE userId = :userId")
    suspend fun deleteMessagesByUser(userId: Long)
}
//This defines what is the database
@Database(
    entities = [User::class, Message::class],
    version = 1,
    exportSchema = false
)
abstract class HippoDatabase: RoomDatabase(){

    // used to handle user related data
    abstract fun userDao(): UserDao

    // used to handle message related data
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: HippoDatabase? = null

        fun getDatabase(context: Context): HippoDatabase {
            return INSTANCE ?: synchronized(this) {
                // create database
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HippoDatabase::class.java,
                    "hippo_database"
                ).build()

                // set isntance
                INSTANCE = instance

                // return database
                return instance
            }
        }
    }

}