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

    var drivers by mutableStateOf(
        listOf(
            Driver(name = "Niwamanya Joel", truck = "Truck #001", status = "Active", statusColor = Color(0xFF4CAF50), profileImage = "https://randomuser.me/api/portraits/men/32.jpg", assignedBins = "Bin-001, Bin-004"),
            Driver(name = "Moola Joseph", truck = "Truck #005", status = "On Break", statusColor = Color(0xFFFF9800), profileImage = "https://randomuser.me/api/portraits/women/44.jpg", assignedBins = "Bin-005"),
            Driver(name = "Okello Peter", truck = "Truck #003", status = "En Route", statusColor = Color(0xFF2196F3), profileImage = "https://randomuser.me/api/portraits/men/46.jpg", assignedBins = "Bin-003"),
            Driver(name = "Musa Juma", truck = "Truck #008", status = "Offline", statusColor = Color(0xFF9E9E9E), profileImage = "https://randomuser.me/api/portraits/men/86.jpg", assignedBins = "None")
        )
    )

    var bins by mutableStateOf(
        listOf(
            BinData("Bin-001", "Acacia Ave", 85, "John Kato", 0.3340, 32.5930),
            BinData("Bin-002", "Bugolobi Village", 40, "Sarah Namuli", 0.3180, 32.6180),
            BinData("Bin-003", "Kiwatule Road", 15, "Peter Okello", 0.3650, 32.6240),
            BinData("Bin-004", "Ntinda St", 92, "Niwamanya Joel", 0.3540, 32.6120),
            BinData("Bin-005", "Makerere Main", 60, "Moola Joseph", 0.3320, 32.5700)
        )
    )

    var schedules by mutableStateOf(
        listOf(
            ScheduleData("Mon, Jan 22", "Morning", "John Kato", "Truck #001"),
            ScheduleData("Tue, Jan 23", "Afternoon", "Sarah Namuli", "Truck #005"),
            ScheduleData("Wed, Jan 24", "Morning", "Peter Okello", "Truck #003")
        )
    )

    var viewedReportIds by mutableStateOf(prefs.getStringSet("viewed_reports", emptySet()) ?: emptySet())

    init {
        listenToReports()
        listenToBins()
        listenToDrivers()
    }

    private fun listenToDrivers() {
        db.child("users").orderByChild("role").equalTo("driver")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val driverList = mutableListOf<Driver>()
                    for (doc in snapshot.children) {
                        val name = doc.child("fullName").getValue(String::class.java) ?: "Unknown Driver"
                        driverList.add(
                            Driver(
                                name = name,
                                truck = "Truck #00${(1..9).random()}", // In a real app, this would be in the profile
                                status = "Online",
                                statusColor = Color(0xFF4CAF50),
                                profileImage = "https://randomuser.me/api/portraits/men/${(1..99).random()}.jpg",
                                assignedBins = "None" // This will be calculated below
                            )
                        )
                    }
                    if (driverList.isNotEmpty()) {
                        drivers = driverList
                        syncDriverAssignments()
                    }
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
                if (snapshot.childrenCount == 0L) {
                    // Initialize with default bins if Firebase is empty
                    val defaultBins = listOf(
                        BinData("Bin-001", "Acacia Ave", 85, "John Kato", 0.3340, 32.5930),
                        BinData("Bin-002", "Bugolobi Village", 40, "Sarah Namuli", 0.3180, 32.6180),
                        BinData("Bin-003", "Kiwatule Road", 15, "Peter Okello", 0.3650, 32.6240),
                        BinData("Bin-004", "Ntinda St", 92, "Niwamanya Joel", 0.3540, 32.6120),
                        BinData("Bin-005", "Makerere Main", 60, "Moola Joseph", 0.3320, 32.5700)
                    )
                    defaultBins.forEach { addBin(it) }
                } else {
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
                }
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

    fun markAsResolved(reportId: String, notes: String = "") {
        val updates = mutableMapOf<String, Any>(
            "status" to "Resolved"
        )
        if (notes.isNotEmpty()) {
            updates["adminNotes"] = notes
        }
        db.child("reports").child(reportId).updateChildren(updates)
    }

    fun deleteReport(reportId: String) {
        db.child("reports").child(reportId).removeValue()
    }

    fun addDriver(driver: Driver) {
        drivers = drivers + driver
    }

    fun updateDriver(oldName: String, updated: Driver) {
        drivers = drivers.map { if (it.name == oldName) updated else it }
    }

    fun deleteDriver(driver: Driver) {
        drivers = drivers.filter { it.name != driver.name }
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

    fun addSchedule(schedule: ScheduleData) {
        schedules = schedules + schedule
    }

    fun assignDriverToBin(binId: String, driverName: String) {
        // 1. Update bin in Firebase
        val bin = bins.find { it.id == binId } ?: return
        val updatedBin = bin.copy(assignedDriver = driverName)
        updateBin(binId, updatedBin)

        // 2. Create a task in "assigned_tasks" for the Driver to see
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

        // 3. Send a notification to the driver
        val notificationData = hashMapOf(
            "title" to "New Task Assigned",
            "message" to "You have been assigned to collect bin $binId at ${bin.location}.",
            "timestamp" to System.currentTimeMillis().toString(),
            "isRead" to false,
            "type" to "new_assignment"
        )
        db.child("notifications").child(driverName).push().setValue(notificationData)

        // 4. Update local drivers state for immediate UI feedback
        syncDriverAssignments()
    }
}
