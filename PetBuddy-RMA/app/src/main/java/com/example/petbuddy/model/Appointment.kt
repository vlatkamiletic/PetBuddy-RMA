package com.example.petbuddy.model

import com.google.firebase.Timestamp

data class Appointment(
    val id: String = "",
    val petId: String = "",
    val userId: String = "",
    val type: String = "",
    val date: Timestamp = Timestamp.now(),
    val notes: String = "",

    // Novo za ponavljajuće termine
    val isRecurring: Boolean = false,           // Označava ponavljanje
    val recurrenceType: String = "",            // Npr. "daily"
    val endDate: Timestamp? = null              // Do kada se ponavlja
)
