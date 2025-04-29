package com.example.petbuddy;

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.petbuddy.model.Appointment
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PetDetailsScreen(
    navController: NavController,
    petId: String,
    petName: String
) {
    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    var appointments by remember { mutableStateOf(listOf<Appointment>()) }
    var isLoading by remember { mutableStateOf(true) }

    val dateFormatter = remember {
        SimpleDateFormat("dd.MM.yyyy. HH:mm", Locale.getDefault())
    }

    LaunchedEffect(Unit) {
        db.collection("appointments")
            .whereEqualTo("petId", petId)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                appointments = result.map { doc ->
                    Appointment(
                        id = doc.id,
                        petId = doc.getString("petId") ?: "",
                        userId = doc.getString("userId") ?: "",
                        type = doc.getString("type") ?: "",
                        date = doc.getTimestamp("date") ?: Timestamp.now(),
                        notes = doc.getString("notes") ?: ""
                    )
                }
                isLoading = false
            }
            .addOnFailureListener { e ->
                Log.e("PetDetailsScreen", "Error loading appointments", e)
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            Text(
                text = "Appointments for $petName",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp)
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            if (appointments.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No appointments found.")
                }
            } else {
                LazyColumn(modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)) {
                    items(appointments) { appointment ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Type: ${appointment.type}", style = MaterialTheme.typography.titleMedium)
                                Text("Date: ${dateFormatter.format(appointment.date.toDate())}")
                                if (appointment.notes.isNotEmpty()) {
                                    Text("Notes: ${appointment.notes}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

