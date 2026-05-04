package com.example.smartwaste.admin

import androidx.compose.ui.graphics.Color

data class Driver(
    val name: String,
    val truck: String,
    val status: String,
    val statusColor: Color,
    val profileImage: String,
    val assignedBins: String = "None"
)

data class BinData(
    val id: String,
    val location: String,
    val fillLevel: Int,
    val assignedDriver: String = "Unassigned",
    val latitude: Double = 0.3136,
    val longitude: Double = 32.5811
)

data class ScheduleData(
    val date: String,
    val shift: String,
    val driverName: String,
    val vehicle: String
)

data class ReportItem(
    val id: String = "",
    val userName: String,
    val location: String,
    val description: String,
    val status: String = "Pending",
    val issueType: String = "General",
    val timestamp: Long = 0L,
    val adminNotes: String = ""
)
