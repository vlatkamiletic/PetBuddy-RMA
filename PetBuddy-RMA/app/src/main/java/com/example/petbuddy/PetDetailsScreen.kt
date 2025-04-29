package com.example.petbuddy

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    var appointments by remember { mutableStateOf(listOf<Appointment>()) }
    var isLoading by remember { mutableStateOf(true) }

    val dateFormatter = remember {
        SimpleDateFormat("dd.MM.yyyy. HH:mm", Locale.getDefault())
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var newType by remember { mutableStateOf("") }
    var newNotes by remember { mutableStateOf("") }
    val calendar = remember { Calendar.getInstance() }
    var selectedDateTime by remember { mutableStateOf<Date?>(null) }

    fun loadAppointments() {
        isLoading = true
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

    LaunchedEffect(Unit) {
        loadAppointments()
    }

    Scaffold(
        topBar = {
            Text(
                text = "Appointments for $petName",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Text("+")
            }
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

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                confirmButton = {
                    TextButton(onClick = {
                        if (selectedDateTime != null) {
                            val timestamp = Timestamp(selectedDateTime!!)
                            val appointment = hashMapOf(
                                "petId" to petId,
                                "userId" to userId,
                                "type" to newType,
                                "date" to timestamp,
                                "notes" to newNotes
                            )

                            db.collection("appointments")
                                .add(appointment)
                                .addOnSuccessListener {
                                    showAddDialog = false
                                    newType = ""
                                    newNotes = ""
                                    selectedDateTime = null
                                    loadAppointments()
                                }
                        }
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Cancel")
                    }
                },
                title = { Text("New Appointment") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newType,
                            onValueChange = { newType = it },
                            label = { Text("Type (e.g. Vaccination)") },
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Button(onClick = {
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    calendar.set(Calendar.YEAR, year)
                                    calendar.set(Calendar.MONTH, month)
                                    calendar.set(Calendar.DAY_OF_MONTH, day)

                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            calendar.set(Calendar.HOUR_OF_DAY, hour)
                                            calendar.set(Calendar.MINUTE, minute)
                                            selectedDateTime = calendar.time
                                        },
                                        calendar.get(Calendar.HOUR_OF_DAY),
                                        calendar.get(Calendar.MINUTE),
                                        true
                                    ).show()
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }) {
                            Text("Pick Date & Time")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        selectedDateTime?.let {
                            Text("Selected: ${dateFormatter.format(it)}")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newNotes,
                            onValueChange = { newNotes = it },
                            label = { Text("Notes") },
                            maxLines = 3
                        )
                    }
                }
            )
        }
    }
}