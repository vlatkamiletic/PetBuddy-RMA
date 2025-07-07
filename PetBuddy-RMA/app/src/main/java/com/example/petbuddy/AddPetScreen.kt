package com.example.petbuddy

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*
import com.example.petbuddy.utils.showNotification

@Composable
fun AddPetScreen(
    onPetAdded: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val storageRef = FirebaseStorage.getInstance().reference

    val isDarkTheme = isSystemInDarkTheme()
    val textColor = if (isDarkTheme) Color.White else Color.DarkGray

    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    fun uploadImageAndSavePet() {
        val fileName = UUID.randomUUID().toString() + ".jpg"
        val imageRef = storageRef.child("pet_images/$fileName")

        selectedImageUri?.let { uri ->
            isUploading = true
            imageRef.putFile(uri)
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->

                        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

                        val pet = hashMapOf(
                            "name" to name,
                            "type" to type,
                            "imageUrl" to downloadUrl.toString(),
                            "userId" to userId
                        )

                        db.collection("pets")
                            .add(pet)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Pet added successfully!", Toast.LENGTH_SHORT).show()
                                isUploading = false
                                onPetAdded()
                                showNotification(context, "PetBuddy", "Added new pet: $name")
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Failed to add pet: ${e.message}", Toast.LENGTH_SHORT).show()
                                isUploading = false
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Image upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    isUploading = false
                }
        }
    }

    if (isUploading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Add new pet",
            style = MaterialTheme.typography.headlineMedium,
            color = textColor
        )


        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Pet name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = type,
            onValueChange = { type = it },
            label = { Text("Pet type") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { imagePickerLauncher.launch("image/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Choose image")
        }

        Spacer(modifier = Modifier.height(16.dp))

        selectedImageUri?.let { uri ->
            Image(
                painter = rememberAsyncImagePainter(uri),
                contentDescription = "Selected image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (name.isNotBlank() && type.isNotBlank() && selectedImageUri != null) {
                    uploadImageAndSavePet()
                } else {
                    Toast.makeText(context, "Please fill all fields and choose an image.", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add pet")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel")
        }
    }
}
