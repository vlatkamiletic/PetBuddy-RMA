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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit


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

    // var koja se koristi za brisanje termina
    var appointmentToDelete by remember { mutableStateOf<Appointment?>(null) }

    // var koje se koriste za uredivanje termina
    var appointmentToEdit by remember { mutableStateOf<Appointment?>(null) }
    var editType by remember { mutableStateOf("") }
    var editNotes by remember { mutableStateOf("") }
    var editDateTime by remember { mutableStateOf<Date?>(null) }

    var sortOption by remember { mutableStateOf("Najranije prvo") }


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
                }.let { list ->
                    if (sortOption == "Najranije prvo") {
                        list.sortedBy { it.date.toDate() }
                    } else {
                        list.sortedByDescending { it.date.toDate() }
                    }
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
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Sortiraj po:", style = MaterialTheme.typography.labelLarge)

                var expanded by remember { mutableStateOf(false) }

                Box {
                    Button(onClick = { expanded = true }) {
                        Text(sortOption)
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Najranije prvo") },
                            onClick = {
                                sortOption = "Najranije prvo"
                                loadAppointments()
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Najkasnije prvo") },
                            onClick = {
                                sortOption = "Najkasnije prvo"
                                loadAppointments()
                                expanded = false
                            }
                        )
                    }
                }
            }


            Spacer(modifier = Modifier.height(8.dp))

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
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
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
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        IconButton(onClick = {
                                            appointmentToEdit = appointment
                                            editType = appointment.type
                                            editNotes = appointment.notes
                                            editDateTime = appointment.date.toDate()
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Appointment")
                                        }
                                        IconButton(onClick = {
                                            appointmentToDelete = appointment
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Appointment")
                                        }
                                    }
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
        // Brisanje termina
        appointmentToDelete?.let { appointment ->
            AlertDialog(
                onDismissRequest = { appointmentToDelete = null },
                confirmButton = {
                    TextButton(onClick = {
                        db.collection("appointments").document(appointment.id)
                            .delete()
                            .addOnSuccessListener {
                                appointments = appointments.filterNot { it.id == appointment.id }
                                appointmentToDelete = null
                            }
                    }) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { appointmentToDelete = null }) {
                        Text("Cancel")
                    }
                },
                title = { Text("Delete Appointment") },
                text = { Text("Are you sure you want to delete this appointment?") }
            )
        }

        //uredivanje termina
        appointmentToEdit?.let { appointment ->
            AlertDialog(
                onDismissRequest = { appointmentToEdit = null },
                confirmButton = {
                    TextButton(onClick = {
                        val updated = mapOf(
                            "type" to editType,
                            "notes" to editNotes,
                            "date" to Timestamp(editDateTime ?: Date())
                        )
                        db.collection("appointments").document(appointment.id)
                            .update(updated)
                            .addOnSuccessListener {
                                loadAppointments()
                                appointmentToEdit = null
                            }
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { appointmentToEdit = null }) {
                        Text("Cancel")
                    }
                },
                title = { Text("Edit Appointment") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = editType,
                            onValueChange = { editType = it },
                            label = { Text("Type") },
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
                                            editDateTime = calendar.time
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
                            Text("Pick New Date & Time")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        editDateTime?.let {
                            Text("Selected: ${dateFormatter.format(it)}")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = editNotes,
                            onValueChange = { editNotes = it },
                            label = { Text("Notes") },
                            maxLines = 3
                        )
                    }
                }
            )
        }
    }
}