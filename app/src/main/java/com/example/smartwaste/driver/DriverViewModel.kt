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
    val dbKey: String = "",
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

    private val _reportStatus = MutableStateFlow<String?>(null)
    val reportStatus: StateFlow<String?> = _reportStatus.asStateFlow()

    private val _loginStatus = MutableStateFlow<Result<Unit>?>(null)
    val loginStatus: StateFlow<Result<Unit>?> = _loginStatus.asStateFlow()

    init {
        loadDriverProfile()
        listenForAssignedTasks()
        loadNotifications()
    }

    private fun loadDriverProfile() {
        viewModelScope.launch {
            authService.getCurrentUser()?.let { user ->
                val profile = authService.getUserProfile(user.uid)
                val name = profile?.get("fullName") as? String ?: "Driver"
                _driverName.value = name
                // Re-listen for tasks once we have the name
                listenForAssignedTasks()
            }
        }
    }

    private fun listenForAssignedTasks() {
        val name = _driverName.value
        if (name == "Driver") return

        db.child("assigned_tasks").orderByChild("driverName").equalTo(name)
            .addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val taskList = mutableListOf<PickupTask>()
                    for (doc in snapshot.children) {
                        taskList.add(
                            PickupTask(
                                id = doc.key.hashCode(),
                                dbKey = doc.key ?: "",
                                customerName = doc.child("customerName").getValue(String::class.java) ?: "Pickup",
                                address = doc.child("address").getValue(String::class.java) ?: "No Address",
                                wasteType = doc.child("wasteType").getValue(String::class.java) ?: "General",
                                eta = doc.child("shift").getValue(String::class.java) ?: "TBD",
                                isCompleted = doc.child("isCompleted").getValue(Boolean::class.java) ?: false
                            )
                        )
                    }
                    _tasks.value = taskList
                    updateStats()
                }
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    Log.e("DriverViewModel", "Task listener cancelled", error.toException())
                }
            })
    }

    fun reportIssue(issueType: String, taskId: Int, reason: String) {
        viewModelScope.launch {
            try {
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
                _reportStatus.value = "Report submitted successfully! Admin has been notified."
                Log.d("DriverViewModel", "Report sent to Realtime Database: $issueType")
            } catch (e: Exception) {
                Log.e("DriverViewModel", "Error reporting issue", e)
                _reportStatus.value = "Failed to submit report. Please try again."
            }
        }
    }

    fun clearReportStatus() {
        _reportStatus.value = null
    }

    fun loadNotifications() {
        viewModelScope.launch {
            val dummyNotifications = listOf(
                NotificationItem(1, "New Assignment", "You have new pickups added to your route", "8:00 AM", false, "new_assignment"),
                NotificationItem(2, "System Update", "Welcome to the new SmartWaste Driver portal", "7:30 AM", true, "route_change")
            )
            _notifications.value = dummyNotifications
            _showNotificationBadge.value = dummyNotifications.any { !it.isRead }
        }
    }

    fun markNotificationRead(notificationId: Int) {
        val updated = _notifications.value.map { n ->
            if (n.id == notificationId) n.copy(isRead = true) else n
        }
        _notifications.value = updated
        _showNotificationBadge.value = updated.any { !it.isRead }
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

    fun markTaskCompleted(taskId: Int) {
        viewModelScope.launch {
            val task = _tasks.value.find { it.id == taskId }
            if (task != null && task.dbKey.isNotEmpty()) {
                try {
                    db.child("assigned_tasks").child(task.dbKey).child("isCompleted").setValue(true).await()
                    Log.d("DriverViewModel", "Task ${task.dbKey} marked as completed in DB")
                    
                    // The ValueEventListener will automatically update the local _tasks list
                    // but we can also update it immediately for better UX
                    val updatedTasks = _tasks.value.map {
                        if (it.id == taskId) it.copy(isCompleted = true) else it
                    }
                    _tasks.value = updatedTasks
                    updateStats()
                } catch (e: Exception) {
                    Log.e("DriverViewModel", "Error marking task completed", e)
                }
            }
        }
    }

    fun refreshData() {
        listenForAssignedTasks()
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

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = authService.login(email, password)
            if (result.isSuccess) {
                loadDriverProfile()
                _loginStatus.value = Result.success(Unit)
            } else {
                _loginStatus.value = Result.failure(result.exceptionOrNull() ?: Exception("Login failed"))
            }
            _isLoading.value = false
        }
    }

    fun clearLoginStatus() {
        _loginStatus.value = null
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
