package com.example.expensetrackerapp

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.expensetrackerapp.ui.theme.ExpenseTrackerAppTheme
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AddExpenseActivity : ComponentActivity() {

    private val db = FirebaseFirestore.getInstance()  // Firebase Firestore instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ExpenseTrackerAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AddExpenseScreen(modifier = Modifier.padding(innerPadding)) { expenseName, amount, category, date ->
                        saveExpenseToFirestore(expenseName, amount, category, date)
                    }
                }
            }
        }
    }

    private fun saveExpenseToFirestore(expenseName: String, amount: String, category: String, date: String) {
        if (expenseName.isEmpty() || amount.isEmpty() || category.isEmpty() || date.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val expense = hashMapOf(
            "name" to expenseName,
            "amount" to amount,
            "category" to category,
            "date" to date,  // Storing selected date
            "timestamp" to System.currentTimeMillis()  // Storing current timestamp
        )

        db.collection("expenses")
            .add(expense)
            .addOnSuccessListener {
                Toast.makeText(this, "Expense added successfully!", Toast.LENGTH_SHORT).show()
                finish()  // Close AddExpenseActivity
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to add expense!", Toast.LENGTH_SHORT).show()
            }
    }
}

@Composable
fun AddExpenseScreen(modifier: Modifier = Modifier, onSaveExpense: (String, String, String, String) -> Unit) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    var expenseName by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("Select Category") }

    // Format date to "yyyy-MM-dd"
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    var selectedDate by remember { mutableStateOf(dateFormatter.format(calendar.time)) }  // Default to today’s date

    val categories = listOf("Food", "Grocery", "Shopping", "Rent", "Entertainment", "Laundry", "Others")

    Column(
        modifier = modifier.padding(16.dp)
    ) {
        Text(text = "Add Expense", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // Expense Name Input Field
        OutlinedTextField(
            value = expenseName,
            onValueChange = { expenseName = it },
            label = { Text("Expense Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Amount Input Field
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Date Picker Button
        OutlinedButton(
            onClick = {
                DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        calendar.set(year, month, dayOfMonth)
                        selectedDate = dateFormatter.format(calendar.time)  // Update selected date
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Date: $selectedDate")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dropdown Menu for Category Selection
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(selectedCategory)
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category) },
                        onClick = {
                            selectedCategory = category  // Update selected category
                            expanded = false  // Close dropdown
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Save Button
        Button(
            onClick = {
                if (expenseName.isNotEmpty() && amount.isNotEmpty() && selectedCategory != "Select Category") {
                    onSaveExpense(expenseName, amount, selectedCategory, selectedDate)  // Pass all values to Firebase
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Expense")
        }
    }
}
