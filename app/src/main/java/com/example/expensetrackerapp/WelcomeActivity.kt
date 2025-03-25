package com.example.expensetrackerapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val buttonContinue = findViewById<Button>(R.id.buttonContinue)

        // Automatically navigate after 2.5 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            navigateNext()
        }, 2500)

        // Navigate on button click
        buttonContinue.setOnClickListener {
            navigateNext()
        }
    }

    private fun navigateNext() {
        val auth = FirebaseAuth.getInstance()
        val nextActivity = if (auth.currentUser != null) {
            DashboardActivity::class.java
        } else {
            LoginActivity::class.java
        }
        val intent = Intent(this, nextActivity)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}