package com.example.expensetrackerapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class UserProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var imageViewProfile: ImageView
    private lateinit var textViewName: TextView
    private lateinit var textViewEmail: TextView
    private lateinit var textViewTotalExpense: TextView
    private lateinit var textViewAccountCreated: TextView
    private lateinit var buttonLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        auth = FirebaseAuth.getInstance()

        // Link XML views
        imageViewProfile = findViewById(R.id.imageViewProfile)
        textViewName = findViewById(R.id.textViewName)
        textViewEmail = findViewById(R.id.textViewEmail)
        textViewTotalExpense = findViewById(R.id.textViewTotalExpense)
        textViewAccountCreated = findViewById(R.id.textViewAccountCreated)
        buttonLogout = findViewById(R.id.buttonLogout)

        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(this, "You are not logged in.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Load profile picture if available
        user.photoUrl?.let {
            Glide.with(this).load(it).into(imageViewProfile)
        }

        textViewName.text = user.displayName ?: "Name not available"
        textViewEmail.text = user.email ?: "Email not available"

        val creationDate = user.metadata?.creationTimestamp?.let {
            val date = Date(it)
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
        } ?: "--"
        textViewAccountCreated.text = "Account Created: $creationDate"

        // Fetch and display total expenses
        CoroutineScope(Dispatchers.Main).launch {
            val totalExpense = fetchTotalExpensesForUser(user.uid)
            textViewTotalExpense.text = "Total Spent: ₹$totalExpense"
        }

        buttonLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Logged out successfully!", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }
    }

    private suspend fun fetchTotalExpensesForUser(userId: String): Double {
        val db = FirebaseFirestore.getInstance()
        return try {
            val snapshot = db.collection("users").document(userId).collection("expenses").get().await()
            snapshot.documents.mapNotNull {
                it.getDouble("amount") ?: it.getString("amount")?.toDoubleOrNull()
            }.sum()
        } catch (e: Exception) {
            0.0
        }
    }
}