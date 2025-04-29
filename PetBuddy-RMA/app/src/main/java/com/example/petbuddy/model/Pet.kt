package com.example.petbuddy.model

data class Pet(
    val id: String = "",
    val name: String = "",
    val type: String = "",
    val imageUrl: String = "" // mora biti imageUrl, ne imageUri
)

