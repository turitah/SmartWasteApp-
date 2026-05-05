package com.example.smartwaste.admin

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class AdminViewModel(application: Application) : AndroidViewModel(application) {
    private val db = FirebaseDatabase.getInstance().reference
    private val prefs = application.getSharedPreferences("admin_prefs", Context.MODE_PRIVATE)

    private val _reports = MutableStateFlow<List<ReportItem>>(emptyList())
    val reports: StateFlow<List<ReportItem>> = _reports

    private val _newReportEvent = MutableSharedFlow<ReportItem>()
    val newReportEvent: SharedFlow<ReportItem> = _newReportEvent.asSharedFlow()

    private var initialLoadComplete = false

    var drivers by mutableStateOf<List<Driver>>(emptyList())
    var bins by mutableStateOf<List<BinData>>(emptyList())
    var schedules by mutableStateOf<List<ScheduleData>>(emptyList())

    var adminProfileImage by mutableStateOf<String?>(null)
    var adminNameState by mutableStateOf("Admin")
    var adminEmailState by mutableStateOf("admin@smartwaste.com")

    init {
        listenToReports()
        listenToBins()
        listenToDrivers()
    }

    fun setAdminEmail(email: String) {
        if (adminEmailState == email && adminNameState != "Admin") return
        adminEmailState = email
        prefs.edit().putString("admin_email", email).apply()
        loadAdminProfile()
    }

    private fun loadAdminProfile() {
        db.child("users").orderByChild("email").equalTo(adminEmailState).limitToFirst(1)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userDoc = snapshot.children.firstOrNull()
                    if (userDoc != null) {
                        adminNameState = userDoc.child("fullName").getValue(String::class.java) ?: "Admin"
                        adminProfileImage = userDoc.child("profileImage").getValue(String::class.java)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    fun updateAdminProfileImage(uri: String) {
        adminProfileImage = uri
        db.child("users").orderByChild("email").equalTo(adminEmailState).limitToFirst(1)
            .get().addOnSuccessListener { snapshot ->
                snapshot.children.firstOrNull()?.ref?.child("profileImage")?.setValue(uri)
            }
    }

    private fun listenToDrivers() {
        db.child("users").orderByChild("role").equalTo("driver")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val driverList = mutableListOf<Driver>()
                    for (doc in snapshot.children) {
                        val name = doc.child("fullName").getValue(String::class.java) ?: "Unknown Driver"
                        val profileImage = doc.child("profileImage").getValue(String::class.java)
                        driverList.add(
                            Driver(
                                id = doc.key ?: "",
                                name = name,
                                truck = doc.child("truck").getValue(String::class.java) ?: "TBD",
                                status = "Online",
                                statusColor = Color(0xFF4CAF50),
                                profileImage = profileImage ?: "https://ui-avatars.com/api/?name=${name.replace(" ", "+")}&background=random",
                                assignedBins = "None"
                            )
                        )
                    }
                    drivers = driverList
                    syncDriverAssignments()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun syncDriverAssignments() {
        drivers = drivers.map { driver ->
            val assigned = bins.filter { it.assignedDriver == driver.name }.map { it.id }
            driver.copy(assignedBins = if (assigned.isEmpty()) "None" else assigned.joinToString(", "))
        }
    }

    private fun listenToBins() {
        db.child("bins").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val binList = mutableListOf<BinData>()
                for (doc in snapshot.children) {
                    binList.add(
                        BinData(
                            id = doc.child("id").getValue(String::class.java) ?: "",
                            location = doc.child("location").getValue(String::class.java) ?: "",
                            fillLevel = doc.child("fillLevel").getValue(Int::class.java) ?: 0,
                            assignedDriver = doc.child("assignedDriver").getValue(String::class.java) ?: "Unassigned",
                            latitude = doc.child("latitude").getValue(Double::class.java) ?: 0.3136,
                            longitude = doc.child("longitude").getValue(Double::class.java) ?: 32.5811
                        )
                    )
                }
                bins = binList
                syncDriverAssignments()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun listenToReports() {
        db.child("reports").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val reportList = mutableListOf<ReportItem>()
                for (doc in snapshot.children) {
                    reportList.add(
                        ReportItem(
                            id = doc.key ?: "",
                            userName = doc.child("userEmail").getValue(String::class.java) ?: "Unknown",
                            location = doc.child("location").getValue(String::class.java) ?: "Unknown",
                            description = doc.child("description").getValue(String::class.java) ?: "",
                            status = doc.child("status").getValue(String::class.java) ?: "Pending",
                            issueType = doc.child("issueType").getValue(String::class.java) ?: "General",
                            timestamp = doc.child("timestamp").getValue(Long::class.java) ?: 0L,
                            adminNotes = doc.child("adminNotes").getValue(String::class.java) ?: ""
                        )
                    )
                }
                
                val sortedList = reportList.sortedByDescending { it.timestamp }
                
                if (initialLoadComplete) {
                    val currentIds = _reports.value.map { it.id }.toSet()
                    sortedList.forEach { report ->
                        if (report.id !in currentIds && report.status == "Pending") {
                            _newReportEvent.tryEmit(report)
                        }
                    }
                } else {
                    initialLoadComplete = true
                }
                
                _reports.value = sortedList
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun markAsResolved(reportId: String, driverName: String, notes: String = "") {
        val report = _reports.value.find { it.id == reportId } ?: return
        
        val updates = mutableMapOf<String, Any>(
            "status" to "Resolved",
            "assignedDriver" to driverName
        )
        if (notes.isNotEmpty()) {
            updates["adminNotes"] = notes
        }
        db.child("reports").child(reportId).updateChildren(updates)

        val taskData = hashMapOf(
            "driverName" to driverName,
            "reportId" to reportId,
            "customerName" to report.userName,
            "address" to report.location,
            "wasteType" to report.issueType,
            "shift" to "Report Resolution",
            "isCompleted" to false,
            "timestamp" to System.currentTimeMillis()
        )
        db.child("assigned_tasks").push().setValue(taskData)

        val notificationData = hashMapOf(
            "title" to "New Report Assigned",
            "message" to "You have been assigned to handle a report: ${report.issueType} at ${report.location}.",
            "timestamp" to System.currentTimeMillis().toString(),
            "isRead" to false,
            "type" to "complaint"
        )
        db.child("notifications").child(driverName).push().setValue(notificationData)
    }

    fun deleteReport(reportId: String) {
        db.child("reports").child(reportId).removeValue()
    }

    fun addDriver(name: String, truck: String, imageUri: String?) {
        // In a real app, this would involve creating a Firebase Auth user
        // and adding a document to 'users' with role 'driver'.
        val driverData = hashMapOf(
            "fullName" to name,
            "truck" to truck,
            "role" to "driver",
            "profileImage" to (imageUri ?: ""),
            "email" to "${name.lowercase().replace(" ", ".")}@smartwaste.com"
        )
        db.child("users").push().setValue(driverData)
    }

    fun updateDriver(driverId: String, updated: Driver) {
        if (driverId.isNotEmpty()) {
            db.child("users").child(driverId).updateChildren(mapOf(
                "fullName" to updated.name,
                "truck" to updated.truck,
                "profileImage" to updated.profileImage
            ))
        }
    }

    fun deleteDriver(driver: Driver) {
        if (driver.id.isNotEmpty()) {
            db.child("users").child(driver.id).removeValue()
        } else {
            // Fallback to name search if ID is missing for some reason
            db.child("users").orderByChild("fullName").equalTo(driver.name).limitToFirst(1)
                .get().addOnSuccessListener { snapshot ->
                    snapshot.children.firstOrNull()?.ref?.removeValue()
                }
        }
    }

    fun addBin(bin: BinData) {
        db.child("bins").child(bin.id).setValue(bin)
    }

    fun updateBin(oldId: String, updated: BinData) {
        if (oldId != updated.id) {
            db.child("bins").child(oldId).removeValue()
        }
        db.child("bins").child(updated.id).setValue(updated)
    }

    fun deleteBin(binId: String) {
        db.child("bins").child(binId).removeValue()
    }

    fun assignDriverToBin(binId: String, driverName: String) {
        val bin = bins.find { it.id == binId } ?: return
        db.child("bins").child(binId).child("assignedDriver").setValue(driverName)

        val taskData = hashMapOf(
            "driverName" to driverName,
            "binId" to binId,
            "customerName" to "Waste Bin $binId",
            "address" to bin.location,
            "wasteType" to "General Waste",
            "shift" to "Immediate Pickup",
            "isCompleted" to false,
            "latitude" to bin.latitude,
            "longitude" to bin.longitude,
            "timestamp" to System.currentTimeMillis()
        )
        db.child("assigned_tasks").push().setValue(taskData)

        val notificationData = hashMapOf(
            "title" to "New Task Assigned",
            "message" to "You have been assigned to collect bin $binId at ${bin.location}.",
            "timestamp" to System.currentTimeMillis().toString(),
            "isRead" to false,
            "type" to "new_assignment"
        )
        db.child("notifications").child(driverName).push().setValue(notificationData)
    }

    fun getDayEvaluation(): String {
        val totalTasks = bins.size + _reports.value.size
        val completedBins = bins.count { it.fillLevel < 20 && it.assignedDriver != "Unassigned" }
        val resolvedReports = _reports.value.count { it.status == "Resolved" }
        
        val totalCompleted = completedBins + resolvedReports
        val percentage = if (totalTasks > 0) (totalCompleted * 100) / totalTasks else 0
        
        return when {
            percentage >= 80 -> "Excellent! Most tasks are being handled efficiently today."
            percentage >= 50 -> "Good progress. Half of the scheduled tasks are completed."
            else -> "Busy day ahead! Many tasks are still pending resolution."
        }
    }
}
