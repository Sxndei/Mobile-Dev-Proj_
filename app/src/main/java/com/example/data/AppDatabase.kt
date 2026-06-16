package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceDao {
    // Providers
    @Query("SELECT * FROM service_providers")
    fun getAllProviders(): Flow<List<ServiceProvider>>

    @Query("SELECT * FROM service_providers WHERE id = :id LIMIT 1")
    fun getProviderById(id: Int): Flow<ServiceProvider?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProviders(providers: List<ServiceProvider>)

    // Bookings
    @Query("SELECT * FROM bookings ORDER BY timestamp DESC")
    fun getAllBookings(): Flow<List<Booking>>

    @Query("SELECT * FROM bookings WHERE providerId = :providerId ORDER BY timestamp DESC")
    fun getBookingsForProvider(providerId: Int): Flow<List<Booking>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: Booking): Long

    @Query("UPDATE bookings SET status = :status WHERE id = :bookingId")
    suspend fun updateBookingStatus(bookingId: Int, status: String)

    @Query("UPDATE bookings SET ratingGiven = :rating, reviewText = :reviewText WHERE id = :bookingId")
    suspend fun updateBookingRating(bookingId: Int, rating: Int, reviewText: String)

    @Query("UPDATE service_providers SET rating = (rating * reviewCount + :rating) / (reviewCount + 1), reviewCount = reviewCount + 1 WHERE id = :providerId")
    suspend fun updateProviderRating(providerId: Int, rating: Int)

    // Chats
    @Query("SELECT * FROM chat_messages WHERE providerId = :providerId ORDER BY timestamp ASC")
    fun getMessagesForProvider(providerId: Int): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("SELECT * FROM chat_conversations ORDER BY lastMessageTimestamp DESC")
    fun getAllConversations(): Flow<List<ChatConversation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ChatConversation)

    @Query("UPDATE chat_conversations SET lastMessageText = :text, lastMessageTimestamp = :ts WHERE providerId = :providerId")
    suspend fun updateConversationLastMessage(providerId: Int, text: String, ts: Long)

    // Users
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserAccount?

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserAccount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserAccount)
}

@Database(
    entities = [
        ServiceProvider::class,
        Booking::class,
        ChatMessage::class,
        ChatConversation::class,
        UserAccount::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun serviceDao(): ServiceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "local_services_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
