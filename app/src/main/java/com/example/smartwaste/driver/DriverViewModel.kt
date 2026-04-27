package com.example.smartwaste.driver

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartwaste.auth.FirebaseAuthService
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class PickupTask(
    val id: Int,
    val customerName: String,
    val address: String,
    val wasteType: String,
    val phoneNumber: String = "0700000000",
    val eta: String = "9:00 AM",
    val binFullness: String = "60% Full",
    val notes: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isCompleted: Boolean = false,
    val isMissed: Boolean = false,
    val missedReason: String = ""
)

data class DriverStats(
    val totalPickups: Int = 0,
    val completedPickups: Int = 0,
    val missedPickups: Int = 0,
    val pendingPickups: Int = 0
)

data class NotificationItem(
    val id: Int,
    val title: String,
    val message: String,
    val timestamp: String,
    val isRead: Boolean = false,
    val type: String // "new_assignment", "complaint", "route_change"
)

data class DriverReport(
    val issueType: String, // "breakdown", "blocked_road", "missed_pickup"
    val taskId: Int,
    val reason: String,
    val timestamp: String
)

class DriverViewModel : ViewModel() {

    private val _tasks = MutableStateFlow<List<PickupTask>>(emptyList())
    val tasks: StateFlow<List<PickupTask>> = _tasks.asStateFlow()

    private val _stats = MutableStateFlow(DriverStats())
    val stats: StateFlow<DriverStats> = _stats.asStateFlow()

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _showNotificationBadge = MutableStateFlow(false)
    val showNotificationBadge: StateFlow<Boolean> = _showNotificationBadge.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _driverName = MutableStateFlow("Driver")
    val driverName: StateFlow<String> = _driverName.asStateFlow()

    private val db = FirebaseDatabase.getInstance().reference
    private val authService = FirebaseAuthService()

    init {
        loadTodayPickups()
        loadNotifications()
        loadDriverProfile()
    }

    private fun loadDriverProfile() {
        viewModelScope.launch {
            authService.getCurrentUser()?.let { user ->
                val profile = authService.getUserProfile(user.uid)
                _driverName.value = profile?.get("fullName") as? String ?: "Driver"
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                authService.login(email, password)
            } catch (e: Exception) {
                Log.e("DriverViewModel", "Login failed", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadTodayPickups() {
        viewModelScope.launch {
            _isLoading.value = true
            delay(500)

            val dummyPickups = listOf(
                PickupTask(1, "Tukahebwa Ritah", "123 Kampala Rd", "Recycling", "0788123456", "8:15 AM", "85% Full", "Gate: 1234", 0.3136, 32.5811),
                PickupTask(2, "Moola Joseph", "45 Jinja Rd", "Compost", "0772654321", "8:30 AM", "60% Full", "", 0.3200, 32.5900),
                PickupTask(3, "Niwamanya Joel", "789 Ggaba Rd", "General Waste", "0755987654", "9:00 AM", "40% Full", "", 0.3000, 32.6000),
                PickupTask(4, "Diana Prince", "250 Entebbe Rd", "Recycling", "0700112233", "9:20 AM", "Bin Not Out", "Behind house", 0.2900, 32.5700),
                PickupTask(5, "Eve Wilson", "101 Bombo Rd", "Plastic", "0711445566", "10:00 AM", "70% Full", "", 0.3300, 32.5800)
            )
            _tasks.value = dummyPickups
            updateStats()
            _isLoading.value = false
        }
    }

    fun loadNotifications() {
        viewModelScope.launch {
            val dummyNotifications = listOf(
                NotificationItem(1, "New Assignment", "You have 2 new pickups added to your route", "8:00 AM", false, "new_assignment"),
                NotificationItem(2, "Complaint Alert", "Customer at 123 Kampala Rd reported missed pickup", "9:15 AM", false, "complaint"),
                NotificationItem(3, "Route Change", "Your route has been optimized. Check new order.", "7:30 AM", true, "route_change")
            )
            _notifications.value = dummyNotifications
            _showNotificationBadge.value = dummyNotifications.any { !it.isRead }
        }
    }

    fun markNotificationRead(notificationId: Int) {
        val notification = _notifications.value.find { it.id == notificationId }
        val updated = _notifications.value.map { n ->
            if (n.id == notificationId) n.copy(isRead = true) else n
        }
        _notifications.value = updated
        _showNotificationBadge.value = updated.any { !it.isRead }

        // If the notification was about new assignments, simulate adding them to the task list
        if (notification?.type == "new_assignment" && !notification.isRead) {
            handleNewAssignment()
        }
    }

    private fun handleNewAssignment() {
        val currentMaxId = _tasks.value.maxOfOrNull { it.id } ?: 0
        val newTasks = listOf(
            PickupTask(
                id = currentMaxId + 1,
                customerName = "Frank Miller",
                address = "50 Kyadondo Rd",
                wasteType = "Electronic Waste",
                phoneNumber = "0788001122",
                eta = "11:30 AM",
                binFullness = "90% Full",
                latitude = 0.3180,
                longitude = 32.5850
            ),
            PickupTask(
                id = currentMaxId + 2,
                customerName = "Grace Hopper",
                address = "12 Nakasero Hill",
                wasteType = "Paper",
                phoneNumber = "0777554433",
                eta = "12:15 PM",
                binFullness = "50% Full",
                latitude = 0.3250,
                longitude = 32.5820
            )
        )
        _tasks.value = _tasks.value + newTasks
        updateStats()
    }

    fun clearAllNotifications() {
        val updated = _notifications.value.map { it.copy(isRead = true) }
        _notifications.value = updated
        _showNotificationBadge.value = false
    }

    fun addDriverNote(taskId: Int, note: String) {
        val updatedTasks = _tasks.value.map { task ->
            if (task.id == taskId) {
                task.copy(notes = if (task.notes.isNotEmpty()) "${task.notes}\n📝 $note" else "📝 $note")
            } else {
                task
            }
        }
        _tasks.value = updatedTasks
    }

    fun reportIssue(issueType: String, taskId: Int, reason: String) {
        viewModelScope.launch {
            try {
                // Update local state
                if (issueType == "missed_pickup") {
                    val updatedTasks = _tasks.value.map { task ->
                        if (task.id == taskId) task.copy(isMissed = true, missedReason = reason) else task
                    }
                    _tasks.value = updatedTasks
                }
                updateStats()

                // Send to Realtime Database for Admin to see
                val task = _tasks.value.find { it.id == taskId }
                val report = hashMapOf(
                    "userEmail" to "Driver: ${_driverName.value}",
                    "issueType" to issueType.replace("_", " ").replaceFirstChar { it.uppercase() },
                    "location" to (task?.address ?: "Unknown"),
                    "description" to "Task #$taskId: $reason",
                    "timestamp" to System.currentTimeMillis(),
                    "status" to "Pending"
                )
                
                db.child("reports").push().setValue(report).await()
                Log.d("DriverViewModel", "Report sent to Realtime Database: $issueType")
            } catch (e: Exception) {
                Log.e("DriverViewModel", "Error reporting issue", e)
            }
        }
    }

    fun markTaskCompleted(taskId: Int) {
        val updatedTasks = _tasks.value.map { task ->
            if (task.id == taskId) task.copy(isCompleted = true) else task
        }
        _tasks.value = updatedTasks
        updateStats()
    }

    fun refreshData() {
        loadTodayPickups()
        loadNotifications()
    }

    private fun updateStats() {
        val total = _tasks.value.size
        val completed = _tasks.value.count { it.isCompleted }
        val missed = _tasks.value.count { it.isMissed }
        val pending = total - completed - missed

        _stats.value = DriverStats(
            totalPickups = total,
            completedPickups = completed,
            missedPickups = missed,
            pendingPickups = pending
        )
    }

    fun logout() {
        authService.logout()
    }

    fun getDailyEarnings(): Double {
        // Dummy logic to return current daily earnings
        return stats.value.completedPickups * 15.50
    }

    fun getWeeklyEarnings(): Double {
        // Dummy logic to return current weekly earnings
        return (stats.value.completedPickups * 15.50) + 450.0
    }
}
