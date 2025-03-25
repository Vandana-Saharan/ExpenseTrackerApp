package com.example.expensetrackerapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class DashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        auth = FirebaseAuth.getInstance()

        val welcomeTextView = findViewById<TextView>(R.id.dashboardWelcomeText)
        val buttonAddExpense = findViewById<Button>(R.id.buttonAddExpense)
        val buttonViewExpenses = findViewById<Button>(R.id.buttonViewExpenses)
        val buttonUserProfile = findViewById<Button>(R.id.buttonUserProfile)
        //val buttonLogout = findViewById<Button>(R.id.buttonLogout) // Add this button in your XML if not already

        buttonAddExpense.setOnClickListener {
            checkLoginAndProceed {
                startActivity(Intent(this, AddExpenseActivity::class.java))
            }
        }

        buttonViewExpenses.setOnClickListener {
            checkLoginAndProceed {
                startActivity(Intent(this, ViewExpensesActivity::class.java))
            }
        }

        buttonUserProfile.setOnClickListener {
            startActivity(Intent(this, UserProfileActivity::class.java))
        }

       // buttonLogout.setOnClickListener {
          //  logoutUser()
       // }
    }

    private fun checkLoginAndProceed(action: () -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showLoginRequiredDialog()
        } else {
            action()
        }
    }

    private fun showLoginRequiredDialog() {
        AlertDialog.Builder(this)
            .setTitle("Login Required")
            .setMessage("Please login or sign up to perform this action.")
            .setPositiveButton("Login / Signup") { _, _ ->
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun logoutUser() {
        auth.signOut()
        Toast.makeText(this, "Logged out successfully!", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, WelcomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}