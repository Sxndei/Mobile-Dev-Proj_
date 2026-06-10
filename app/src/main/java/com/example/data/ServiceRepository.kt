package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull

class ServiceRepository(private val serviceDao: ServiceDao) {

    val allProviders: Flow<List<ServiceProvider>> = serviceDao.getAllProviders()
    val allBookings: Flow<List<Booking>> = serviceDao.getAllBookings()
    val allConversations: Flow<List<ChatConversation>> = serviceDao.getAllConversations()

    fun getProviderById(id: Int): Flow<ServiceProvider?> = serviceDao.getProviderById(id)

    fun getBookingsForProvider(providerId: Int): Flow<List<Booking>> = serviceDao.getBookingsForProvider(providerId)

    fun getMessagesForProvider(providerId: Int): Flow<List<ChatMessage>> = serviceDao.getMessagesForProvider(providerId)

    suspend fun createBooking(booking: Booking): Long {
        return serviceDao.insertBooking(booking)
    }

    suspend fun updateBookingStatus(bookingId: Int, status: String) {
        serviceDao.updateBookingStatus(bookingId, status)
    }

    suspend fun rateBooking(bookingId: Int, providerId: Int, rating: Int, reviewText: String) {
        serviceDao.updateBookingRating(bookingId, rating, reviewText)
        serviceDao.updateProviderRating(providerId, rating)
    }

    suspend fun sendChatMessage(providerId: Int, senderRole: String, messageText: String) {
        // Insert message
        val message = ChatMessage(
            providerId = providerId,
            senderRole = senderRole,
            messageText = messageText
        )
        serviceDao.insertMessage(message)

        // Find or build conversation
        val providersList = serviceDao.getAllProviders().first()
        val provider = providersList.find { it.id == providerId }
        val name = provider?.name ?: "Provider #$providerId"
        val category = provider?.category ?: "Service"
        val avatar = provider?.avatarUrl ?: ""

        val conversation = ChatConversation(
            providerId = providerId,
            providerName = name,
            providerCategory = category,
            providerAvatarUrl = avatar,
            lastMessageText = messageText,
            lastMessageTimestamp = System.currentTimeMillis(),
            unreadCount = if (senderRole == "PROVIDER") 1 else 0
        )
        serviceDao.insertConversation(conversation)
    }

    suspend fun seedDatabaseIfEmpty() {
        // Let's check if providers are empty
        val current = serviceDao.getAllProviders().first()
        if (current.isEmpty()) {
            val seedList = listOf(
                ServiceProvider(
                    id = 1,
                    name = "Alex Morgan",
                    category = "Plumbing",
                    rating = 4.8,
                    reviewCount = 24,
                    experience = "8 years",
                    hourlyRate = 45.0,
                    bio = "Professional plumbing services, leak fixes, pipe repairs, and emergency plumbing. 100% customer satisfaction guaranteed.",
                    avatarUrl = "https://images.unsplash.com/photo-1540569014015-19a7be504e3a?w=150",
                    availabilitySlots = "09:00 AM,11:00 AM,02:00 PM,04:00 PM",
                    isFeatured = true
                ),
                ServiceProvider(
                    id = 2,
                    name = "Maya Patel",
                    category = "Tutoring",
                    rating = 4.9,
                    reviewCount = 42,
                    experience = "5 years",
                    hourlyRate = 35.0,
                    bio = "Experienced Calculus, Algebra, and SAT tutor. Certified math teacher with a passion for student success across all grade levels.",
                    avatarUrl = "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=150",
                    availabilitySlots = "10:00 AM,01:00 PM,03:00 PM,05:00 PM",
                    isFeatured = true
                ),
                ServiceProvider(
                    id = 3,
                    name = "Clara Reynolds",
                    category = "Cleaning",
                    rating = 4.7,
                    reviewCount = 18,
                    experience = "3 years",
                    hourlyRate = 25.0,
                    bio = "Meticulous deep cleaning and regular housekeeping. I use eco-friendly products to give your apartment a spotless, healthy shine.",
                    avatarUrl = "https://images.unsplash.com/photo-1567532939604-b6b5b0db2604?w=150",
                    availabilitySlots = "08:00 AM,11:00 AM,01:00 PM,04:00 PM",
                    isFeatured = false
                ),
                ServiceProvider(
                    id = 4,
                    name = "James Carter",
                    category = "Electrical",
                    rating = 4.9,
                    reviewCount = 31,
                    experience = "10 years",
                    hourlyRate = 50.0,
                    bio = "Licensed master electrician for residential and light commercial needs. Smart home setups, fixture installs, and diagnostic testing.",
                    avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150",
                    availabilitySlots = "09:00 AM,11:30 AM,02:30 PM,04:30 PM",
                    isFeatured = true
                ),
                ServiceProvider(
                    id = 5,
                    name = "Liam O'Connor",
                    category = "Fitness",
                    rating = 4.6,
                    reviewCount = 12,
                    experience = "4 years",
                    hourlyRate = 40.0,
                    bio = "Certified NASM personal trainer. Holistic health guidance, strength conditioning, weight-loss programming, and custom workout strategies.",
                    avatarUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150",
                    availabilitySlots = "10:00 AM,12:00 PM,03:00 PM,06:00 PM",
                    isFeatured = false
                )
            )
            serviceDao.insertProviders(seedList)
        }
    }
}
