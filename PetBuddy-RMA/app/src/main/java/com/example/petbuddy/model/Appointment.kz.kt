package com.example.petbuddy.model

import com.google.firebase.Timestamp

data class Appointment(
    val id: String = "",
    val petId: String = "",
    val userId: String = "",
    val type: String = "",
    val date: Timestamp = Timestamp.now(),
    val notes: String = ""
)
