package com.example.petbuddy

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetDetailsScreen(navController: NavController, petId: String, petName: String) {
    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    val dateFormatter = remember { SimpleDateFormat("dd.MM.yyyy. HH:mm", Locale.getDefault()) }

    var appointments by remember { mutableStateOf(listOf<Appointment>()) }
    var isLoading by remember { mutableStateOf(true) }
    var sortOption by remember { mutableStateOf("Earliest first") }
    var searchQuery by remember { mutableStateOf("") }

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedAppointment by remember { mutableStateOf<Appointment?>(null) }

    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var appointmentToDelete by remember { mutableStateOf<Appointment?>(null) }


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
                        endDate = doc.getTimestamp("endDate"),
                        notes = doc.getString("notes") ?: ""
                    )
                }.let { list ->
                    if (sortOption == "Earliest first") list.sortedBy { it.date.toDate() }
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
            TopAppBar(
                title = {
                    Text("Appointments for $petName")
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
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
                        DropdownMenuItem(text = { Text("Earliest first") }, onClick = {
                            sortOption = "Earliest first"
                            expanded = false
                        })
                        DropdownMenuItem(text = { Text("Latest first") }, onClick = {
                            sortOption = "Latest first"
                            expanded = false
                        })
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search appointments") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val filteredAppointments = appointments.filter {
                    it.type.contains(searchQuery, ignoreCase = true) ||
                            it.notes.contains(searchQuery, ignoreCase = true) ||
                            dateFormatter.format(it.date.toDate()).contains(searchQuery)
                }

                if (filteredAppointments.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No appointments found.")
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredAppointments) { appointment ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Type: ${appointment.type}", style = MaterialTheme.typography.titleMedium)
                                    val dateText = appointment.endDate?.let {
                                        "${dateFormatter.format(appointment.date.toDate())} – ${dateFormatter.format(it.toDate())}"
                                    } ?: dateFormatter.format(appointment.date.toDate())
                                    Text("Date: $dateText")
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
                                            appointmentToDelete = appointment
                                            showDeleteConfirmation = true
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
    }

    if (showAddDialog) {
        AppointmentDialog(
            onDismiss = { showAddDialog = false },
            onSave = { type, customType, notes, date, endDate ->
                val finalType = if (type == "Other") customType else type
                db.collection("appointments")
                    .add(
                        mapOf(
                            "petId" to petId,
                            "userId" to userId,
                            "type" to finalType,
                            "date" to Timestamp(date),
                            "endDate" to endDate?.let { Timestamp(it) },
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
            initialType = appointment.type,
            initialCustomType = if (appointment.type !in listOf("Vaccination", "Check-up", "Grooming", "Training", "Medication")) appointment.type else "",
            initialNotes = appointment.notes,
            initialDate = appointment.date.toDate(),
            initialEndDate = appointment.endDate?.toDate(),
            onDismiss = {
                showEditDialog = false
                selectedAppointment = null
            },
            onSave = { type, customType, notes, date, endDate ->
                val finalType = if (type == "Other") customType else type
                db.collection("appointments").document(appointment.id)
                    .update(
                        mapOf(
                            "type" to finalType,
                            "notes" to notes,
                            "date" to Timestamp(date),
                            "endDate" to endDate?.let { Timestamp(it) }
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

    // Alert za brisanje termina
    if (showDeleteConfirmation && appointmentToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmation = false
                appointmentToDelete = null
            },
            confirmButton = {
                TextButton(onClick = {
                    db.collection("appointments").document(appointmentToDelete!!.id)
                        .delete()
                        .addOnSuccessListener {
                            appointments = appointments.filterNot { it.id == appointmentToDelete!!.id }
                            showDeleteConfirmation = false
                            appointmentToDelete = null
                        }
                        .addOnFailureListener {
                            Log.e("PetDetailsScreen", "Failed to delete appointment", it)
                            showDeleteConfirmation = false
                            appointmentToDelete = null
                        }
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    appointmentToDelete = null
                }) {
                    Text("Cancel")
                }
            },
            title = { Text("Delete Appointment") },
            text = { Text("Are you sure you want to delete the appointment '${appointmentToDelete!!.type}'?") }
        )
    }
}



@Composable
fun AppointmentDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, Date, Date?) -> Unit,
    initialType: String = "Vaccination",
    initialCustomType: String = "",
    initialNotes: String = "",
    initialDate: Date? = null,
    initialEndDate: Date? = null
) {
    val context = LocalContext.current
    val now = remember { Calendar.getInstance() }
    val calendar = remember { Calendar.getInstance() }
    val dateFormatter = remember { SimpleDateFormat("dd.MM.yyyy. HH:mm", Locale.getDefault()) }
    val typeOptions = listOf("Vaccination", "Check-up", "Grooming", "Training", "Medication", "Other")

    var type by remember { mutableStateOf(initialType) }
    var customType by remember { mutableStateOf(initialCustomType) }
    var notes by remember { mutableStateOf(initialNotes) }
    var selectedDate by remember { mutableStateOf(initialDate) }
    var endDate by remember { mutableStateOf(initialEndDate) }
    var typeExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    fun isValidDates(): Boolean {
        return selectedDate != null && selectedDate!!.after(now.time)
    }

    val isSaveEnabled = isValidDates()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    if (!isValidDates()) {
                        errorMessage = if (type == "Medication") "Start date cannot be in the past." else "Date cannot be in the past."
                        return@TextButton
                    }
                    errorMessage = ""
                    onSave(type, customType, notes, selectedDate!!, endDate)
                },
                enabled = isSaveEnabled
            ) {
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

                if (type == "Medication") {
                    Text("Start Date & Time")
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
                                        val pickedDate = calendar.time
                                        if (pickedDate.after(now.time)) {
                                            selectedDate = pickedDate
                                            errorMessage = ""
                                        } else {
                                            errorMessage = "Start date cannot be in the past."
                                        }
                                    },
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    true
                                ).show()
                            },
                            now.get(Calendar.YEAR),
                            now.get(Calendar.MONTH),
                            now.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }) {
                        Text("Pick Start Date & Time")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    selectedDate?.let {
                        Text("Start: ${dateFormatter.format(it)}")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("End Date & Time")
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
                                        val pickedEnd = calendar.time
                                        if (pickedEnd.after(now.time)) {
                                            endDate = pickedEnd
                                            errorMessage = ""
                                        } else {
                                            errorMessage = "End date cannot be in the past."
                                        }
                                    },
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    true
                                ).show()
                            },
                            now.get(Calendar.YEAR),
                            now.get(Calendar.MONTH),
                            now.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }) {
                        Text("Pick End Date & Time")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    endDate?.let {
                        Text("End: ${dateFormatter.format(it)}")
                    }
                } else {
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
                                        val pickedDate = calendar.time
                                        if (pickedDate.after(now.time)) {
                                            selectedDate = pickedDate
                                            errorMessage = ""
                                        } else {
                                            errorMessage = "Date cannot be in the past."
                                        }
                                    },
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    true
                                ).show()
                            },
                            now.get(Calendar.YEAR),
                            now.get(Calendar.MONTH),
                            now.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }) {
                        Text("Pick Date & Time")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    selectedDate?.let {
                        Text("Selected: ${dateFormatter.format(it)}")
                    }
                }

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
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
