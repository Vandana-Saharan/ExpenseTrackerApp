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

class UserProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var imageViewProfile: ImageView
    private lateinit var textViewEmail: TextView
    private lateinit var textViewTotalExpense: TextView
    private lateinit var textViewAccountCreated: TextView
    private lateinit var buttonLogout: Button

    // 👇 New UI elements
    private lateinit var editTextName: EditText
    private lateinit var buttonEditName: Button
    private lateinit var buttonSaveName: Button

    private val db = FirebaseFirestore.getInstance()

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

//    private suspend fun fetchTotalExpensesForUser(userId: String): Double {
//        return try {
//            val snapshot = db.collection("expenses")
//                .whereEqualTo("userId", userId)
//                .get()
//                .await()
//
//            // Optional debug toast (only if you're in Main thread)
//            withContext(Dispatchers.Main) {
//                Toast.makeText(this@UserProfileActivity, "Fetched: ${snapshot.size()} expenses", Toast.LENGTH_SHORT).show()
//            }
//
//            // Log each expense for debugging
//            snapshot.documents.forEach {
//                val rawAmount = it.get("amount")
//                val userInDoc = it.get("userId")
//                Log.d("MyLog", "DocID: ${it.id} | Amount: $rawAmount | userId: $userInDoc")
//            }
//
//            // Sum up all valid amounts
//            val totalExpense =  snapshot.documents.mapNotNull {
//                it.getDouble("amount") ?: it.getString("amount")?.toDoubleOrNull()
//            }.sum()
//            Log.i("MyLog", totalExpense.toString());
//            return totalExpense;
//
//        } catch (e: Exception) {
//            Log.e("DEBUG", "Failed to fetch total expenses: ${e.message}")
//            0.0
//        }
//    }
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

}