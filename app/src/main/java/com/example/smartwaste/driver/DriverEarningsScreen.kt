package com.example.smartwaste.driver

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DriverEarningsScreen(viewModel: DriverViewModel) {
    val daily = viewModel.getDailyEarnings()
    val weekly = viewModel.getWeeklyEarnings()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Driver Earnings Summary",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Daily Earnings Card
        EarningsCard(
            title = "Daily Earnings",
            amount = "$${"%.2f".format(daily)}"
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Weekly Earnings Card
        EarningsCard(
            title = "Weekly Earnings",
            amount = "$${"%.2f".format(weekly)}"
        )
    }
}

@Composable
fun EarningsCard(title: String, amount: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = amount,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}