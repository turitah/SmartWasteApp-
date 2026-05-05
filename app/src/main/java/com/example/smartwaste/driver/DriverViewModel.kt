package com.example.smartwaste.driver

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartwaste.auth.FirebaseAuthService
import com.google.android.gms.maps.model.LatLng
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
    val dbKey: String = "",
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

    private val _driverLocation = MutableStateFlow<LatLng?>(null)
    val driverLocation: StateFlow<LatLng?> = _driverLocation.asStateFlow()

    private val _activeTask = MutableStateFlow<PickupTask?>(null)
    val activeTask: StateFlow<PickupTask?> = _activeTask.asStateFlow()

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
                                phoneNumber = doc.child("phoneNumber").getValue(String::class.java) ?: "0700000000",
                                latitude = doc.child("latitude").getValue(Double::class.java) ?: 0.0,
                                longitude = doc.child("longitude").getValue(Double::class.java) ?: 0.0,
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
        val name = _driverName.value
        if (name == "Driver") return

        db.child("notifications").child(name).addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val notificationList = mutableListOf<NotificationItem>()
                for (doc in snapshot.children) {
                    notificationList.add(
                        NotificationItem(
                            id = doc.key.hashCode(),
                            dbKey = doc.key ?: "",
                            title = doc.child("title").getValue(String::class.java) ?: "Notification",
                            message = doc.child("message").getValue(String::class.java) ?: "",
                            timestamp = doc.child("timestamp").getValue(String::class.java) ?: "",
                            isRead = doc.child("isRead").getValue(Boolean::class.java) ?: false,
                            type = doc.child("type").getValue(String::class.java) ?: "info"
                        )
                    )
                }
                _notifications.value = notificationList.reversed()
                _showNotificationBadge.value = notificationList.any { !it.isRead }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })
    }

    fun clearNotification(notificationId: Int) {
        val name = _driverName.value
        if (name == "Driver") return
        
        val notification = _notifications.value.find { it.id == notificationId } ?: return
        if (notification.dbKey.isEmpty()) return

        viewModelScope.launch {
            try {
                db.child("notifications").child(name).child(notification.dbKey).removeValue().await()
            } catch (e: Exception) {
                Log.e("DriverViewModel", "Error clearing notification", e)
            }
        }
    }

    fun markNotificationRead(notificationId: Int) {
        val name = _driverName.value
        if (name == "Driver") return

        val notification = _notifications.value.find { it.id == notificationId } ?: return
        if (notification.dbKey.isEmpty()) return

        viewModelScope.launch {
            try {
                db.child("notifications").child(name).child(notification.dbKey).child("isRead").setValue(true).await()
            } catch (e: Exception) {
                Log.e("DriverViewModel", "Error marking read", e)
            }
        }
    }

    fun clearAllNotifications() {
        val name = _driverName.value
        if (name == "Driver") return
        
        viewModelScope.launch {
            try {
                db.child("notifications").child(name).removeValue().await()
                _notifications.value = emptyList()
                _showNotificationBadge.value = false
            } catch (e: Exception) {
                Log.e("DriverViewModel", "Error clearing notifications", e)
            }
        }
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

    fun updateDriverLocation(latLng: LatLng) {
        _driverLocation.value = latLng
        // Update driver's location in Firebase for Admin to track
        val name = _driverName.value
        if (name != "Driver") {
            viewModelScope.launch {
                try {
                    db.child("drivers").child(name).child("lat").setValue(latLng.latitude)
                    db.child("drivers").child(name).child("lng").setValue(latLng.longitude)
                } catch (e: Exception) {
                    Log.e("DriverViewModel", "Error updating location in DB", e)
                }
            }
        }
    }

    fun startTaskNavigation(task: PickupTask) {
        _activeTask.value = task
        viewModelScope.launch {
            try {
                // Update task status in DB to "En Route"
                if (task.dbKey.isNotEmpty()) {
                    db.child("assigned_tasks").child(task.dbKey).child("status").setValue("En Route")
                }
                
                // Also update driver status in Firebase
                val name = _driverName.value
                if (name != "Driver") {
                    db.child("drivers").child(name).child("status").setValue("En Route")
                    db.child("drivers").child(name).child("activeTaskId").setValue(task.dbKey)
                }
            } catch (e: Exception) {
                Log.e("DriverViewModel", "Error starting task navigation", e)
            }
        }
    }

    fun markTaskCompleted(taskId: Int) {
        viewModelScope.launch {
            val task = _tasks.value.find { it.id == taskId }
            if (task != null && task.dbKey.isNotEmpty()) {
                try {
                    db.child("assigned_tasks").child(task.dbKey).child("isCompleted").setValue(true).await()
                    db.child("assigned_tasks").child(task.dbKey).child("status").setValue("Completed")
                    
                    // Update driver status back to Active
                    val name = _driverName.value
                    if (name != "Driver") {
                        db.child("drivers").child(name).child("status").setValue("Active")
                        db.child("drivers").child(name).child("activeTaskId").setValue("")
                    }
                    
                    if (_activeTask.value?.id == taskId) {
                        _activeTask.value = null
                    }

                    Log.d("DriverViewModel", "Task ${task.dbKey} marked as completed in DB")
                    
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

    fun resetPassword(email: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = authService.sendPasswordResetEmail(email)
            onResult(result)
        }
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
