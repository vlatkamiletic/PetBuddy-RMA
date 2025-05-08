package com.example.petbuddy

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material.icons.filled.ArrowDropDown



@Composable
fun PetDetailsScreen(navController: NavController, petId: String, petName: String) {
    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }
    val options = listOf("Vaccination", "Check-up", "Grooming", "Training", "Other")
    val dateFormatter = remember { SimpleDateFormat("dd.MM.yyyy. HH:mm", Locale.getDefault()) }

    var appointments by remember { mutableStateOf(listOf<Appointment>()) }
    var isLoading by remember { mutableStateOf(true) }
    var sortOption by remember { mutableStateOf("Najranije prvo") }

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedAppointment by remember { mutableStateOf<Appointment?>(null) }

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
                    if (sortOption == "Najranije prvo") list.sortedBy { it.date.toDate() }
                    else list.sortedByDescending { it.date.toDate() }
                }
                isLoading = false
            }
            .addOnFailureListener {
                Log.e("PetDetailsScreen", "Error loading appointments", it)
                isLoading = false
            }
    }

    LaunchedEffect(sortOption) {
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
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            var expanded by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Sort by:")
                Box {
                    Button(onClick = { expanded = true }) {
                        Text(sortOption)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("Najranije prvo") }, onClick = {
                            sortOption = "Najranije prvo"
                            expanded = false
                        })
                        DropdownMenuItem(text = { Text("Najkasnije prvo") }, onClick = {
                            sortOption = "Najkasnije prvo"
                            expanded = false
                        })
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (appointments.isEmpty()) {
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
                                        selectedAppointment = appointment
                                        showEditDialog = true
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                                    }
                                    IconButton(onClick = {
                                        db.collection("appointments").document(appointment.id).delete().addOnSuccessListener {
                                            appointments = appointments.filterNot { it.id == appointment.id }
                                        }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete")
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
        AppointmentDialog(
            onDismiss = { showAddDialog = false },
            onSave = { type, customType, notes, date ->
                val finalType = if (type == "Other") customType else type
                db.collection("appointments")
                    .add(
                        mapOf(
                            "petId" to petId,
                            "userId" to userId,
                            "type" to finalType,
                            "date" to Timestamp(date),
                            "notes" to notes
                        )
                    )
                    .addOnSuccessListener {
                        showAddDialog = false
                        loadAppointments()
                    }
            }
        )
    }

    if (showEditDialog && selectedAppointment != null) {
        val appointment = selectedAppointment!!
        AppointmentDialog(
            initialType = if (appointment.type in options) appointment.type else "Other",
            initialCustomType = if (appointment.type !in options) appointment.type else "",
            initialNotes = appointment.notes,
            initialDate = appointment.date.toDate(),
            onDismiss = {
                showEditDialog = false
                selectedAppointment = null
            },
            onSave = { type, customType, notes, date ->
                val finalType = if (type == "Other") customType else type
                db.collection("appointments").document(appointment.id)
                    .update(
                        mapOf(
                            "type" to finalType,
                            "notes" to notes,
                            "date" to Timestamp(date)
                        )
                    )
                    .addOnSuccessListener {
                        showEditDialog = false
                        selectedAppointment = null
                        loadAppointments()
                    }
            }
        )
    }
}



@Composable
fun AppointmentDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, Date) -> Unit,
    initialType: String = "Vaccination",
    initialCustomType: String = "",
    initialNotes: String = "",
    initialDate: Date? = null
) {
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }
    val dateFormatter = remember { SimpleDateFormat("dd.MM.yyyy. HH:mm", Locale.getDefault()) }
    val typeOptions = listOf("Vaccination", "Check-up", "Grooming", "Training", "Other", )

    var type by remember { mutableStateOf(initialType) }
    var customType by remember { mutableStateOf(initialCustomType) }
    var notes by remember { mutableStateOf(initialNotes) }
    var selectedDate by remember { mutableStateOf(initialDate) }
    var typeExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                if (selectedDate != null) {
                    onSave(type, customType, notes, selectedDate!!)
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Appointment") },
        text = {
            Column {
                Text("Type")
                Spacer(modifier = Modifier.height(4.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = type,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown",
                                modifier = Modifier.clickable { typeExpanded = true }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { typeExpanded = true }
                    )

                    DropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        typeOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    type = option
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                if (type == "Other") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customType,
                        onValueChange = { customType = it },
                        label = { Text("Custom Type") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

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
                                    selectedDate = calendar.time
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

                selectedDate?.let {
                    Text("Selected: ${dateFormatter.format(it)}")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}

