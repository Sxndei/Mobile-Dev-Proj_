package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Booking
import com.example.data.ChatConversation
import com.example.data.ChatMessage
import com.example.data.ServiceProvider
import com.example.data.ServiceRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AppNotification(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val message: String,
    val isForProvider: Boolean = false
)

class ServiceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ServiceRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ServiceRepository(database.serviceDao())
        
        // Seed initial provider data if empty
        viewModelScope.launch {
            repository.seedDatabaseIfEmpty()
        }
    }

    // Role state: "CUSTOMER" or "PROVIDER"
    private val _currentRole = MutableStateFlow("CUSTOMER")
    val currentRole: StateFlow<String> = _currentRole.asStateFlow()

    // Provider profile we are "logged in as" in PROVIDER view (Default is Alex Morgan, id=1)
    private val _activeProviderId = MutableStateFlow(1)
    val activeProviderId: StateFlow<Int> = _activeProviderId.asStateFlow()

    // Filter and search parameters
    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Active conversation provider ID
    private val _activeChatProviderId = MutableStateFlow<Int?>(null)
    val activeChatProviderId: StateFlow<Int?> = _activeChatProviderId.asStateFlow()

    // Notification updates
    private val _notification = MutableStateFlow<AppNotification?>(null)
    val notification: StateFlow<AppNotification?> = _notification.asStateFlow()

    // Observables from repo
    val allProviders: StateFlow<List<ServiceProvider>> = repository.allProviders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBookings: StateFlow<List<Booking>> = repository.allBookings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val conversations: StateFlow<List<ChatConversation>> = repository.allConversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Provider's bookings
    val providerBookings: StateFlow<List<Booking>> = combine(allBookings, _activeProviderId) { bookings, id ->
        bookings.filter { it.providerId == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Chat Message history
    val activeMessages: StateFlow<List<ChatMessage>> = _activeChatProviderId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.getMessagesForProvider(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered providers
    val filteredProviders: StateFlow<List<ServiceProvider>> = combine(
        allProviders,
        _selectedCategory,
        _searchQuery
    ) { list, cat, query ->
        list.filter { provider ->
            val matchCat = cat == "All" || provider.category.equals(cat, ignoreCase = true)
            val matchSearch = query.isEmpty() || provider.name.contains(query, ignoreCase = true) || 
                              provider.bio.contains(query, ignoreCase = true) || 
                              provider.category.contains(query, ignoreCase = true)
            matchCat && matchSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Functions
    fun setRole(role: String) {
        _currentRole.value = role
    }

    fun setActiveProviderId(id: Int) {
        _activeProviderId.value = id
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openChat(providerId: Int) {
        _activeChatProviderId.value = providerId
    }

    fun closeChat() {
        _activeChatProviderId.value = null
    }

    fun triggerNotification(title: String, message: String, isForProvider: Boolean = false) {
        _notification.value = AppNotification(title = title, message = message, isForProvider = isForProvider)
    }

    fun clearNotification() {
        _notification.value = null
    }

    // Book service
    fun bookSlot(provider: ServiceProvider, date: String, time: String, notes: String) {
        viewModelScope.launch {
            val booking = Booking(
                providerId = provider.id,
                providerName = provider.name,
                providerCategory = provider.category,
                providerAvatarUrl = provider.avatarUrl,
                customerName = "Jamie Vance (You)",
                bookingDate = date,
                bookingTime = time,
                status = "PENDING",
                totalPrice = provider.hourlyRate * 2, // assume 2 hours min
                notes = notes
            )
            repository.createBooking(booking)

            // Dynamic Toast push update simulate
            triggerNotification(
                title = "Booking Placed Successfully",
                message = "Your request with ${provider.name} for $date ($time) is pending approval."
            )

            // Simulate Provider notification
            triggerNotification(
                title = "New Booking Alert",
                message = "Jamie Vance requested a plumbing slot on $date at $time.",
                isForProvider = true
            )

            // Auto-initiated custom greeting in chat conversion
            repository.sendChatMessage(
                providerId = provider.id,
                senderRole = "CUSTOMER",
                messageText = "Hi ${provider.name}, I just requested a booking for $date at $time. Thanks!"
            )
        }
    }

    // Accept/Reject Booking (Provider action)
    fun acceptBooking(booking: Booking) {
        viewModelScope.launch {
            repository.updateBookingStatus(booking.id, "ACCEPTED")
            // Send simulate push update to Customer
            triggerNotification(
                title = "Booking Approved! 🎉",
                message = "${booking.providerName} has accepted your booking request for ${booking.bookingDate}!"
            )
            // Send automatic visual acceptance message in chat
            repository.sendChatMessage(
                providerId = booking.providerId,
                senderRole = "PROVIDER",
                messageText = "Hi Jamie, I'd be happy to take this job! I have accepted your booking request for ${booking.bookingDate} at ${booking.bookingTime}."
            )
        }
    }

    fun rejectBooking(booking: Booking) {
        viewModelScope.launch {
            repository.updateBookingStatus(booking.id, "REJECTED")
            // Send simulate push update
            triggerNotification(
                title = "Booking Schedule Alert",
                message = "${booking.providerName} declined the booking slot. Tap to reschedule."
            )
            repository.sendChatMessage(
                providerId = booking.providerId,
                senderRole = "PROVIDER",
                messageText = "Hi Jamie, unfortunately, I am unavailable at ${booking.bookingTime} on ${booking.bookingDate}. Feel free to propose a different slot!"
            )
        }
    }

    fun completeBooking(booking: Booking) {
        viewModelScope.launch {
            repository.updateBookingStatus(booking.id, "COMPLETED")
            triggerNotification(
                title = "Job Completed ✅",
                message = "Billing approved. Please drop a review rating for ${booking.providerName}."
            )
        }
    }

    // Give rating (Customer action)
    fun addRating(booking: Booking, rating: Int, comment: String) {
        viewModelScope.launch {
            repository.rateBooking(booking.id, booking.providerId, rating, comment)
            triggerNotification(
                title = "Review Submitted!",
                message = "Thank you for sharing your feedback on ${booking.providerName}."
            )
        }
    }

    // Send chat (Unified action)
    fun sendMessage(providerId: Int, senderRole: String, text: String) {
        viewModelScope.launch {
            repository.sendChatMessage(providerId, senderRole, text)

            // Auto response simulation for realism
            if (senderRole == "CUSTOMER") {
                simulateProviderAutoResponse(providerId)
            }
        }
    }

    private fun simulateProviderAutoResponse(providerId: Int) {
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000) // Delay to feel like a real chat peer
            val messages = repository.getMessagesForProvider(providerId).first()
            if (messages.isNotEmpty() && messages.last().senderRole == "CUSTOMER") {
                val response = when (providerId) {
                    1 -> "Thanks for reaching out! I'm on a pipe repair job right now, but I will check my schedule soon. Let me know if you placed a booking!"
                    2 -> "Hi there! Yes, I can certainly assist with those calculus concepts. Homework problems can be sent ahead of our session!"
                    3 -> "Got it! I bring my own vacuum and eco-friendly scrubbing sprays, if that works for you."
                    4 -> "Hey! I'm fully stocked on light fixtures and dimmer controls. Happy to look over your smart home panels."
                    else -> "Thanks for messaging me! I will review your requirements and respond as soon as I can."
                }
                repository.sendChatMessage(providerId, "PROVIDER", response)
                triggerNotification(
                    title = "New Message received",
                    message = response
                )
            }
        }
    }
}
