package com.example.expensetrackerapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
import android.content.Context

class UserProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var imageViewProfile: ImageView
    private lateinit var textViewEmail: TextView
    private lateinit var textViewTotalExpense: TextView
    private lateinit var textViewAccountCreated: TextView
    private lateinit var buttonLogout: Button

    // User profile UI elements
    private lateinit var editTextName: EditText
    private lateinit var buttonEditName: Button
    private lateinit var buttonSaveName: Button
    
    // Budget management UI elements
    private lateinit var editTextBudgetAmount: EditText
    private lateinit var editTextLimitAmount: EditText
    private lateinit var textViewRemainingAmount: TextView
    private lateinit var buttonSaveBudget: Button
    private lateinit var buttonSaveLimit: Button

    private val db = FirebaseFirestore.getInstance()
    private val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        auth = FirebaseAuth.getInstance()

        imageViewProfile = findViewById(R.id.imageViewProfile)
        editTextName = findViewById(R.id.editTextName)
        textViewEmail = findViewById(R.id.textViewEmail)
        textViewTotalExpense = findViewById(R.id.textViewTotalExpense)
        textViewAccountCreated = findViewById(R.id.textViewAccountCreated)
        buttonLogout = findViewById(R.id.buttonLogout)
        buttonEditName = findViewById(R.id.buttonEditName)
        buttonSaveName = findViewById(R.id.buttonSaveName)
        
        // Initialize budget management UI elements
        editTextBudgetAmount = findViewById(R.id.editTextBudgetAmount)
        editTextLimitAmount = findViewById(R.id.editTextLimitAmount)
        textViewRemainingAmount = findViewById(R.id.textViewRemainingAmount)
        buttonSaveBudget = findViewById(R.id.buttonSaveBudget)
        buttonSaveLimit = findViewById(R.id.buttonSaveLimit)

        val user = auth.currentUser
        ensureUserDocumentExists()
        if (user == null) {
            Toast.makeText(this, "You are not logged in.", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, WelcomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            return
        }

        user.photoUrl?.let {
            Glide.with(this).load(it).into(imageViewProfile)
        }

        editTextName.setText(user.displayName ?: "Name not available")
        editTextName.isEnabled = false
        textViewEmail.text = user.email ?: "Email not available"

        val creationDate = user.metadata?.creationTimestamp?.let {
            val date = Date(it)
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
        } ?: "--"
        textViewAccountCreated.text = "Account Created: $creationDate"

        CoroutineScope(Dispatchers.Main).launch {
            val totalExpense = fetchTotalExpensesForUser(user.uid)
            textViewTotalExpense.text = "Total Spent: ₹$totalExpense"
            
            // Load budget data and update UI
            loadBudgetData(user.uid)
        }

        // Setup budget management buttons
        buttonSaveBudget.setOnClickListener {
            saveBudget(user.uid)
        }
        
        buttonSaveLimit.setOnClickListener {
            saveLimit()
        }
        
        buttonLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Logged out successfully!", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, WelcomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        // 👇 Edit button click
        buttonEditName.setOnClickListener {
            editTextName.isEnabled = true
            buttonSaveName.visibility = View.VISIBLE
            buttonEditName.visibility = View.GONE
        }

        // 👇 Save button click
        buttonSaveName.setOnClickListener {
            val newName = editTextName.text.toString().trim()
            if (newName.isEmpty()) {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updates = hashMapOf<String, Any>("name" to newName)

            user.uid.let { uid ->
                db.collection("users").document(uid).update(updates)
                    .addOnSuccessListener {
                        // Optionally update Firebase Auth display name
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(newName)
                            .build()
                        user.updateProfile(profileUpdates)

                        Toast.makeText(this, "Name updated!", Toast.LENGTH_SHORT).show()
                        editTextName.isEnabled = false
                        buttonSaveName.visibility = View.GONE
                        buttonEditName.visibility = View.VISIBLE
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to update name!", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private suspend fun fetchTotalExpensesForUser(userId: String): Double {
        return try {
            val snapshot = db.collection("users")
                .document(userId)
                .collection("expenses")
                .get()
                .await()

            withContext(Dispatchers.Main) {
                Toast.makeText(this@UserProfileActivity, "Fetched: ${snapshot.size()} expenses", Toast.LENGTH_SHORT).show()
            }

            snapshot.documents.forEach {
                val rawAmount = it.get("amount")
                Log.d("MyLog", "DocID: ${it.id} | Amount: $rawAmount")
            }

            val totalExpense = snapshot.documents.mapNotNull {
                it.getString("amount")?.toDoubleOrNull()
            }.sum()

            Log.i("MyLog", "Total: $totalExpense")
            totalExpense

        } catch (e: Exception) {
            Log.e("MyLog", "Error: ${e.message}")
            0.0
        }
    }
    
    private suspend fun fetchMonthlyExpenses(userId: String): Double {
        return try {
            val calendar = Calendar.getInstance()
            
            // Set start of month
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfMonth = calendar.time.time

            // Set end of month
            calendar.add(Calendar.MONTH, 1)
            calendar.add(Calendar.MILLISECOND, -1)
            val endOfMonth = calendar.time.time

            val snapshot = db.collection("users")
                .document(userId)
                .collection("expenses")
                .whereGreaterThanOrEqualTo("timestamp", startOfMonth)
                .whereLessThanOrEqualTo("timestamp", endOfMonth)
                .get()
                .await()

            val monthlyExpense = snapshot.documents.mapNotNull {
                it.getString("amount")?.toDoubleOrNull()
            }.sum()
            
            Log.d("BudgetManagement", "Monthly expenses: $monthlyExpense")
            monthlyExpense
            
        } catch (e: Exception) {
            Log.e("BudgetManagement", "Error fetching monthly expenses: ${e.message}")
            0.0
        }
    }
    
    private fun ensureUserDocumentExists() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val uid = user.uid
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(uid)

        userRef.get().addOnSuccessListener { document ->
            if (!document.exists()) {
                val newUser = hashMapOf(
                    "name" to (user.displayName ?: "User"),
                    "email" to (user.email ?: "")
                )
                userRef.set(newUser)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Firestore profile created!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to create profile!", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }
    
    private fun loadBudgetData(userId: String) {
        // Load saved budget and limit from SharedPreferences
        val sharedPrefs = getSharedPreferences("BudgetPrefs", Context.MODE_PRIVATE)
        val savedBudget = sharedPrefs.getFloat("${currentMonth}_budget", -1f)
        val savedLimit = sharedPrefs.getFloat("monthly_limit", -1f)
        
        if (savedBudget >= 0) {
            editTextBudgetAmount.setText(savedBudget.toString())
        }
        
        if (savedLimit >= 0) {
            editTextLimitAmount.setText(savedLimit.toString())
        }
        
        // Update remaining budget
        updateRemainingBudget(userId)
    }
    
    private fun saveBudget(userId: String) {
        val budgetText = editTextBudgetAmount.text.toString()
        if (budgetText.isEmpty()) {
            Toast.makeText(this, "Please enter a budget amount", Toast.LENGTH_SHORT).show()
            return
        }
        
        val budget = budgetText.toFloatOrNull()
        if (budget == null || budget <= 0) {
            Toast.makeText(this, "Please enter a valid budget amount", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Save to SharedPreferences
        val sharedPrefs = getSharedPreferences("BudgetPrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putFloat("${currentMonth}_budget", budget).apply()
        
        Toast.makeText(this, "Budget saved successfully!", Toast.LENGTH_SHORT).show()
        
        // Update the remaining budget display
        updateRemainingBudget(userId)
    }
    
    private fun saveLimit() {
        val limitText = editTextLimitAmount.text.toString()
        if (limitText.isEmpty()) {
            Toast.makeText(this, "Please enter a limit amount", Toast.LENGTH_SHORT).show()
            return
        }
        
        val limit = limitText.toFloatOrNull()
        if (limit == null || limit <= 0) {
            Toast.makeText(this, "Please enter a valid limit amount", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Check if budget is set and limit is not greater than budget
        val sharedPrefs = getSharedPreferences("BudgetPrefs", Context.MODE_PRIVATE)
        val currentBudget = sharedPrefs.getFloat("${currentMonth}_budget", -1f)
        
        if (currentBudget < 0) {
            Toast.makeText(this, "Please set a budget first", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (limit > currentBudget) {
            Toast.makeText(this, "Alert limit cannot be greater than budget", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Save to SharedPreferences
        sharedPrefs.edit().putFloat("monthly_limit", limit).apply()
        
        Toast.makeText(this, "Alert limit saved successfully!", Toast.LENGTH_SHORT).show()
    }
    
    private fun updateRemainingBudget(userId: String) {
        CoroutineScope(Dispatchers.Main).launch {
            // Get the monthly budget from SharedPreferences
            val sharedPrefs = getSharedPreferences("BudgetPrefs", Context.MODE_PRIVATE)
            val budget = sharedPrefs.getFloat("${currentMonth}_budget", -1f)
            val limit = sharedPrefs.getFloat("monthly_limit", -1f)
            
            if (budget < 0) {
                textViewRemainingAmount.text = "Set a budget to see remaining amount"
                return@launch
            }
            
            // Get the monthly expenses
            val monthlyExpense = fetchMonthlyExpenses(userId)
            val remaining = budget - monthlyExpense.toFloat()
            
            // Update the UI
            textViewRemainingAmount.text = "Remaining Budget: ₹${String.format("%.2f", remaining)}"
            
            // Change text color based on remaining amount and limit
            if (limit >= 0 && remaining <= limit) {
                textViewRemainingAmount.setTextColor(getColor(android.R.color.holo_red_dark))
                
                // Show warning if remaining amount is below limit
                if (remaining <= limit) {
                    Toast.makeText(
                        this@UserProfileActivity,
                        "Warning: Remaining amount is below your set limit!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                textViewRemainingAmount.setTextColor(getColor(android.R.color.black))
            }
        }
    }

}