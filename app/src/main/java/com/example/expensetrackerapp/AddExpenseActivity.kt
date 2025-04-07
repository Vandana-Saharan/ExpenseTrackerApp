package com.example.expensetrackerapp

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AddExpenseActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var expenseNameEditText: EditText
    private lateinit var expenseAmountEditText: EditText
    private lateinit var selectDateButton: Button
    private lateinit var expenseCategoryButton: Button
    private lateinit var addExpenseButton: Button

    private var selectedDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    private var selectedCategory: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expense)

        // Bind UI components
        expenseNameEditText = findViewById(R.id.expenseName)
        expenseAmountEditText = findViewById(R.id.expenseAmount)
        selectDateButton = findViewById(R.id.selectDateButton)
        expenseCategoryButton = findViewById(R.id.expenseCategory)
        addExpenseButton = findViewById(R.id.addExpenseButton)

        selectDateButton.text = "Select Date: $selectedDate"

        // Date picker
        selectDateButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                    selectDateButton.text = "Date: $selectedDate"
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Category selection (Simple pop-up)
        expenseCategoryButton.setOnClickListener {
            val categories = arrayOf("Food", "Grocery", "Shopping", "Rent", "Entertainment", "Laundry", "Others")
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setTitle("Select Category")
            builder.setItems(categories) { _, which ->
                selectedCategory = categories[which]
                expenseCategoryButton.text = selectedCategory
            }
            builder.show()
        }

        // Save expense button
        addExpenseButton.setOnClickListener {
            val expenseName = expenseNameEditText.text.toString().trim()
            val amount = expenseAmountEditText.text.toString().trim()

            if (expenseName.isEmpty() || amount.isEmpty() || selectedCategory.isEmpty() || selectedDate.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else {
                saveExpenseToFirestore(expenseName, amount, selectedCategory, selectedDate)
            }
        }
    }

    private fun saveExpenseToFirestore(expenseName: String, amount: String, category: String, date: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to add expenses", Toast.LENGTH_SHORT).show()
            return
        }

        val expense = hashMapOf(
            "name" to expenseName,
            "amount" to amount,
            "category" to category,
            "date" to date,
            "timestamp" to System.currentTimeMillis(),
            "userId" to FirebaseAuth.getInstance().currentUser?.uid
        )

        db.collection("users")
            .document(currentUser.uid)
            .collection("expenses")
            .add(expense)
            .addOnSuccessListener {
                Toast.makeText(this, "Expense added successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to add expense!", Toast.LENGTH_SHORT).show()
            }
    }
}