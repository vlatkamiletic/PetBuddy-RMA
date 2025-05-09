package com.example.petbuddy


import androidx.compose.ui.graphics.Color
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.petbuddy.model.Pet
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun HomeScreen(
    navController: NavController,
    onLogout: () -> Unit
) {

    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid

    val isDarkTheme = isSystemInDarkTheme()
    val textColor = if (isDarkTheme) Color.White else Color.DarkGray

    var pets by remember { mutableStateOf(listOf<Pet>()) }
    var isLoading by remember { mutableStateOf(true) }

    // Za brisanje i uređivanje
    var petToDelete by remember { mutableStateOf<Pet?>(null) }
    var petToEdit by remember { mutableStateOf<Pet?>(null) }
    var editName by remember { mutableStateOf("") }
    var editType by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        db.collection("pets")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                pets = result.map { document ->
                    Pet(
                        id = document.id,
                        name = document.getString("name") ?: "",
                        type = document.getString("type") ?: "",
                        imageUrl = document.getString("imageUrl") ?: ""
                    )
                }
                isLoading = false
            }
            .addOnFailureListener { e ->
                Log.w("HomeScreen", "Error fetching pets", e)
                isLoading = false
            }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Your Pets",
            style = MaterialTheme.typography.headlineMedium,
            color = textColor
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(pets) { pet ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable {
                            navController.navigate("pet_details/${pet.id}/${pet.name}")
                        },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        if (pet.imageUrl.isNotEmpty()) {
                            Image(
                                painter = rememberAsyncImagePainter(pet.imageUrl),
                                contentDescription = "Pet Image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Name: ${pet.name}", style = MaterialTheme.typography.bodyLarge)
                        Text("Type: ${pet.type}", style = MaterialTheme.typography.bodyMedium)

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(onClick = {
                                petToEdit = pet
                                editName = pet.name
                                editType = pet.type
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = { petToDelete = pet }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { navController.navigate("add_pet") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add New Pet")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Logout")
        }
    }

    // Alert za brisanje
    petToDelete?.let { pet ->
        AlertDialog(
            onDismissRequest = { petToDelete = null },
            confirmButton = {
                TextButton(onClick = {
                    db.collection("pets").document(pet.id)
                        .delete()
                        .addOnSuccessListener {
                            pets = pets.filterNot { it.id == pet.id }
                            petToDelete = null
                        }
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { petToDelete = null }) {
                    Text("Cancel")
                }
            },
            title = { Text("Delete Pet") },
            text = { Text("Are you sure you want to delete '${pet.name}'?") }
        )
    }

    // Alert za uređivanje
    petToEdit?.let { pet ->
        AlertDialog(
            onDismissRequest = { petToEdit = null },
            confirmButton = {
                TextButton(onClick = {
                    val updated = mapOf(
                        "name" to editName,
                        "type" to editType
                    )
                    db.collection("pets").document(pet.id)
                        .update(updated)
                        .addOnSuccessListener {
                            pets = pets.map {
                                if (it.id == pet.id) it.copy(name = editName, type = editType) else it
                            }
                            petToEdit = null
                        }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { petToEdit = null }) {
                    Text("Cancel")
                }
            },
            title = { Text("Edit Pet") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Name") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editType,
                        onValueChange = { editType = it },
                        label = { Text("Type") },
                        singleLine = true
                    )
                }
            }
        )
    }
}
