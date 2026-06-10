package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "service_providers")
data class ServiceProvider(
    @PrimaryKey val id: Int,
    val name: String,
    val category: String, // Plumber, Tutor, Cleaner, Electrician, etc.
    val rating: Double,
    val reviewCount: Int,
    val experience: String,
    val hourlyRate: Double,
    val bio: String,
    val avatarUrl: String,
    val availabilitySlots: String, // Separated by comma, e.g. "9:00 AM,11:00 AM,2:00 PM,4:00 PM"
    val isFeatured: Boolean = false
)

@Entity(tableName = "bookings")
data class Booking(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val providerId: Int,
    val providerName: String,
    val providerCategory: String,
    val providerAvatarUrl: String,
    val customerName: String,
    val bookingDate: String, // e.g. "June 12, 2026"
    val bookingTime: String, // e.g. "10:00 AM"
    val status: String, // PENDING, ACCEPTED, REJECTED, COMPLETED
    val totalPrice: Double,
    val notes: String = "",
    val ratingGiven: Int = 0, // 0 means not rated yet
    val reviewText: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val providerId: Int,
    val senderRole: String, // "CUSTOMER" or "PROVIDER"
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Entity(tableName = "chat_conversations")
data class ChatConversation(
    @PrimaryKey val providerId: Int,
    val providerName: String,
    val providerCategory: String,
    val providerAvatarUrl: String,
    val lastMessageText: String,
    val lastMessageTimestamp: Long,
    val unreadCount: Int = 0
)
