package com.example.expensetrackerapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SignupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth = FirebaseAuth.getInstance()

        val emailEditText: EditText = findViewById(R.id.editTextEmail)
        val passwordEditText: EditText = findViewById(R.id.editTextPassword)
        val confirmPasswordEditText: EditText = findViewById(R.id.editTextConfirmPassword)
        val signupButton: Button = findViewById(R.id.buttonSignup)
        val loginTextView: TextView = findViewById(R.id.textViewLogin)   // Add this ID in XML if not present

        signupButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            if (email.isEmpty()) {
                emailEditText.error = "Email is required"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                passwordEditText.error = "Password is required"
                return@setOnClickListener
            }
            if (password.length < 6) {
                passwordEditText.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                confirmPasswordEditText.error = "Passwords do not match"
                return@setOnClickListener
            }

            registerUser(email, password)
        }

        // ✅ Add Login navigation
        loginTextView.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun registerUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Signup Successful!", Toast.LENGTH_SHORT).show()
                    startDashboardActivity()
                } else {
                    Toast.makeText(this, "Signup Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun startDashboardActivity() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}