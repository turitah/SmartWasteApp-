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
                    
                    // Trigger notifications for high fill levels
                    if (initialLoadComplete) {
                        binList.forEach { bin ->
                            if (bin.fillLevel > 90) {
                                // In a real app, this would be a server-side trigger
                                // For this demo, we can show a local notification or UI alert
                                // For now, we'll use the existing report-style flow or similar
                            }
                        }
                    }
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
                
                // Detect new reports for notification
                if (initialLoadComplete) {
                    val currentIds = _reports.value.map { it.id }.toSet()
                    sortedList.forEach { report ->
                        if (report.id !in currentIds && report.status == "Pending") {
                            // This is a new incoming report
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

    fun updateReportNotes(reportId: String, notes: String) {
        db.child("reports").child(reportId).child("adminNotes").setValue(notes)
    }

    fun deleteReport(reportId: String) {
        db.child("reports").child(reportId).removeValue()
    }

    fun markReportAsViewed(reportId: String) {
        viewedReportIds = viewedReportIds + reportId
        prefs.edit().putStringSet("viewed_reports", viewedReportIds).apply()
    }

    fun clearNotifications() {
        val pendingIds = _reports.value.filter { it.status == "Pending" }.map { it.id }.toSet()
        viewedReportIds = viewedReportIds + pendingIds
        prefs.edit().putStringSet("viewed_reports", viewedReportIds).apply()
    }

    fun addDriver(driver: Driver) {
        drivers = drivers + driver
    }

    fun updateDriver(oldName: String, updated: Driver) {
        drivers = drivers.map { if (it.name == oldName) updated else it }
    }

    fun addBin(bin: BinData) {
        db.child("bins").child(bin.id).setValue(bin)
    }

    fun updateBin(oldId: String, updated: BinData) {
        if (oldId != updated.id) {
            db.child("bins").child(oldId).removeValue()
        }
        db.child("bins").child(updated.id).setValue(updated)
        
        // Automatically update driver assignments if the bin ID changed
        if (oldId != updated.id) {
            drivers = drivers.map { driver ->
                val updatedAssignments = driver.assignedBins.split(", ")
                    .map { if (it == oldId) updated.id else it }
                    .joinToString(", ")
                driver.copy(assignedBins = updatedAssignments)
            }
        }
    }

    fun deleteBin(binId: String) {
        db.child("bins").child(binId).removeValue()
    }

    fun addSchedule(schedule: ScheduleData) {
        schedules = schedules + schedule
    }

    fun updateSchedule(oldDate: String, updated: ScheduleData) {
        schedules = schedules.map { if (it.date == oldDate) updated else it }
    }

    fun deleteSchedule(schedule: ScheduleData) {
        schedules = schedules.filter { it != schedule }
    }
}
